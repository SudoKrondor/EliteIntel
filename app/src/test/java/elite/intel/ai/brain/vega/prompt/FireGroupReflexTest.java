package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.i18n.AliasPhrase;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards "group bravo", which stopped switching fire groups.
 *
 * <p>The utterance reached the reducer and scored 0.847 - the best of all 162 candidates, but under the
 * absolute {@code SEM_FLOOR} of 0.85, so the turn was offered no game tools at all and the companion
 * answered conversationally ("Group Bravo, this is Vega. Go ahead."). The margin was 0.003, because a
 * {@code string} parameter block is deleted from the embedding text: the aliases embedded as "fire group",
 * which shares no word with what the commander actually said.
 *
 * <p>The fix pins each group as a literal alias, so these phrases resolve in the reflex gate and never
 * depend on the floor, the model, or the argument-extraction hint. Substituting the parameter's example
 * value instead was measured and rejected - it overfits to the substituted word and pushed "group bravo"
 * down to 0.832.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FireGroupReflexTest {

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

    @ParameterizedTest(name = "\"{0}\" reflexes to key={1}")
    @CsvSource({
            "group alpha, alpha", "group bravo, bravo", "group charlie, charlie", "group delta, delta",
            "group echo, echo", "group foxtrot, foxtrot", "group golf, golf", "group hotel, hotel",
            "fire group alpha, alpha", "fire group bravo, bravo", "fire group hotel, hotel",
            "Group Bravo, bravo", "group bravo., bravo",
    })
    void aSpokenGroupResolvesInTheReflexGate(String utterance, String expectedKey) {
        Optional<ReflexResolver.Reflex> reflex = resolve(utterance);
        assertTrue(reflex.isPresent(), () -> "\"" + utterance + "\" must not depend on the LLM or the floor");
        assertEquals("select_fire_group_by_nato", reflex.get().actionId());
        assertEquals(expectedKey, reflex.get().arguments().get("key"));
    }

    /**
     * Every locale carried the same three parameterized phrasings and so the same gap. The spoken word is
     * localized while the pinned argument stays the English NATO word, because {@code FireGroups} maps those.
     */
    @ParameterizedTest(name = "{0} \"{1}\" reflexes to key={2}")
    @CsvSource({
            "DE, gruppe bravo, bravo", "DE, feuergruppe bravo, bravo", "DE, gruppe foxtrott, foxtrot",
            "DE, gruppe alfa, alpha", "DE, feuergruppe hotel, hotel",
            "ES, grupo bravo, bravo", "ES, grupo de fuego bravo, bravo", "ES, grupo alfa, alpha",
            "ES, grupo eco, echo", "ES, grupo de fuego charlie, charlie",
            "FR, groupe bravo, bravo", "FR, groupe de tir bravo, bravo", "FR, groupe écho, echo",
            "FR, groupe hôtel, hotel", "FR, groupe de tir delta, delta",
            "RU, группа браво, bravo", "RU, огневая группа браво, bravo", "RU, группа альфа, alpha",
            "RU, группа эхо, echo", "RU, группа отель, hotel", "RU, огневая группа чарли, charlie",
            "UK, група браво, bravo", "UK, вогнева група браво, bravo", "UK, група альфа, alpha",
            "UK, група ехо, echo", "UK, група готель, hotel", "UK, вогнева група чарлі, charlie",
            "PT, grupo bravo, bravo", "PT, grupo de fogo bravo, bravo", "PT, grupo alfa, alpha",
            "PT, grupo golfe, golf",
            "PTBZ, grupo bravo, bravo", "PTBZ, grupo de fogo alfa, alpha", "PTBZ, grupo eco, echo",
    })
    void everyLocaleResolvesItsOwnSpokenGroup(Language language, String utterance, String expectedKey) {
        SystemSession.getInstance().setLanguage(language);
        try {
            Optional<ReflexResolver.Reflex> reflex = resolve(utterance);
            assertTrue(reflex.isPresent(),
                    () -> language + " \"" + utterance + "\" must resolve without the LLM");
            assertEquals("select_fire_group_by_nato", reflex.get().actionId());
            assertEquals(expectedKey, reflex.get().arguments().get("key"),
                    () -> "the pinned argument must stay the English NATO word FireGroups maps");
        } finally {
            SystemSession.getInstance().setLanguage(Language.EN);
        }
    }

    /**
     * All eight groups must be reachable in every locale, not just the ones spot-checked above.
     */
    @ParameterizedTest(name = "{0} reaches all eight groups")
    @EnumSource(Language.class)
    void everyLocaleReachesAllEightGroups(Language language) {
        SystemSession.getInstance().setLanguage(language);
        try {
            Set<String> reachable = new java.util.TreeSet<>();
            for (String phrase : AiActionLocalizations.phrasesForAction("select_fire_group_by_nato")) {
                AliasPhrase alias = AliasPhrase.parse(phrase);
                if (!alias.hasVariableArgument()) {
                    reachable.addAll(alias.literalArguments().values());
                }
            }
            assertEquals(Set.of("alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf", "hotel"),
                    reachable, () -> language + " does not pin all eight fire groups");
        } finally {
            SystemSession.getInstance().setLanguage(Language.EN);
        }
    }

    /**
     * The bare parameterized aliases still need the LLM to read the value out of the wording.
     */
    @Test
    void anUnpinnedPhrasingStillTakesTheLlmPath() {
        assertTrue(resolve("switch to fire group").isEmpty());
    }

    /**
     * Fire groups do not exist on foot, so the reflex must not fire there.
     */
    @Test
    void theReflexRespectsVisibility() {
        assertTrue(resolver.resolve("group bravo",
                elite.intel.ai.brain.vega.model.GameStateSnapshot.capture(
                        Status.detached(PlayerSituation.ON_FOOT_PLANET))).isEmpty());
    }

    private Optional<ReflexResolver.Reflex> resolve(String utterance) {
        return resolver.resolve(utterance, elite.intel.ai.brain.vega.model.GameStateSnapshot.capture(
                Status.detached(PlayerSituation.IN_SHIP_SUPERCRUISE)));
    }
}
