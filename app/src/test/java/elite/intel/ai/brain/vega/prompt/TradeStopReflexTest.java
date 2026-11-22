package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.vega.model.GameStateSnapshot;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards "plot route to trade port", which did nothing while "plot route to <em>next</em> trade port" worked.
 *
 * <p>Every alias the command owned carried "next" - "navigate to next trade stop", "go to next trade stop",
 * "next trade stop" - and none carried the commander's actual vocabulary: the verb "plot" and the noun
 * "port". So the phrase matched no alias verbatim, {@link ReflexResolver} declined, and the turn fell to the
 * local model with eight candidates inside the margin. There is only ever one stop to fly to, so "next" is
 * describing the route rather than selecting between stops: it cannot be load-bearing in what the commander
 * has to say.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TradeStopReflexTest {

    private static final String COMMAND = "navigate_to_next_trade_stop";

    private ReflexResolver resolver;

    @BeforeAll
    void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        SystemSession.getInstance().setLanguage(Language.EN);
        resolver = new ReflexResolver();
    }

    /**
     * The reported utterance, verbatim.
     */
    @Test
    void plottingToTheTradePortDoesNotRequireTheWordNext() {
        Optional<ReflexResolver.Reflex> reflex = resolve("plot route to trade port");
        assertTrue(reflex.isPresent(), "\"plot route to trade port\" must not depend on the local model");
        assertEquals(COMMAND, reflex.get().actionId());
    }

    /**
     * The phrasing that already worked must keep working.
     */
    @Test
    void theOlderPhrasingStillResolves() {
        assertEquals(COMMAND, resolve("navigate to next trade stop").orElseThrow().actionId());
        assertEquals(COMMAND, resolve("next trade stop").orElseThrow().actionId());
    }

    /**
     * The station is called a port, a station and a stop by different commanders; all three are the same
     * order. Adding one wording and leaving the siblings behind is how this class of miss recurs.
     */
    @Test
    void portStationAndStopAreTheSameOrder() {
        assertEquals(COMMAND, resolve("plot route to trade station").orElseThrow().actionId());
        assertEquals(COMMAND, resolve("plot a course to the trade stop").orElseThrow().actionId());
        assertEquals(COMMAND, resolve("take me to the trade port").orElseThrow().actionId());
    }

    /**
     * Every locale must be able to say it without its own word for "next" - the miss was in the alias data,
     * so it would have been in all nine.
     */
    @ParameterizedTest(name = "{0} can order the trade stop without saying \"next\"")
    @EnumSource(Language.class)
    void everyLocaleHasAPhraseWithoutItsWordForNext(Language language) {
        SystemSession.getInstance().setLanguage(language);
        try {
            List<String> nextless = AiActionLocalizations.phrasesForAction(COMMAND).stream()
                    .filter(phrase -> !containsWordForNext(language, phrase))
                    .toList();
            assertTrue(!nextless.isEmpty(),
                    language + " has no phrase for " + COMMAND + " that omits its word for \"next\"");
        } finally {
            SystemSession.getInstance().setLanguage(Language.EN);
        }
    }

    /**
     * The stem is enough: these are all inflected, and the aliases are lower-cased already.
     */
    private static boolean containsWordForNext(Language language, String phrase) {
        String lower = phrase.toLowerCase(java.util.Locale.ROOT);
        for (String stem : wordsForNext(language)) {
            if (lower.contains(stem)) return true;
        }
        return false;
    }

    private static String[] wordsForNext(Language language) {
        return switch (language) {
            case DE -> new String[]{"nächst"};
            case ES -> new String[]{"siguiente", "próxim"};
            case FR -> new String[]{"prochain"};
            case IT -> new String[]{"prossim", "prossimo"};
            case PT, PTBZ -> new String[]{"próxim"};
            case RU -> new String[]{"следующ"};
            case UK -> new String[]{"наступн"};
            default -> new String[]{"next"};
        };
    }

    private Optional<ReflexResolver.Reflex> resolve(String utterance) {
        return resolver.resolve(utterance, GameStateSnapshot.capture(
                Status.detached(PlayerSituation.IN_SHIP_DEEP_SPACE)));
    }
}
