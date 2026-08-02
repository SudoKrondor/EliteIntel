package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.i18n.AliasPhrase;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A spoken phrase must belong to exactly one action, in every locale.
 *
 * <p>{@link ReflexResolver} matches verbatim and then requires a single owner - two actions sharing a
 * phrase is not a tie it breaks, it is a reflex it abandons ({@code matches.size() != 1}). So a duplicated
 * phrase silently condemns that utterance to the local model forever, which is the exact failure that made
 * "hardpoints" answer with the ship's loadout (see {@link HardpointsReflexTest}).
 *
 * <p>Found this way and fixed: RU "питание на системы" sat on {@code transfer_power_to_shields} as well as
 * {@code transfer_power_to_ship_systems}; RU/UK "торговый маршрут" on the calculate command as well as the
 * query; FR/RU/UK "station services" on the query as well as the panel command; IT "destinazione fleet
 * carrier" on the destination-typing command as well as the voyage query; EN "outfitting" on both the
 * station query and the ship-loadout query.
 *
 * <p>The resolution follows the convention EN already encodes: an imperative belongs to the command and a
 * bare noun phrase to the query, except where the noun names an actual in-game panel ("station services").
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AliasPhraseCollisionTest {

    @BeforeAll
    void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
    }

    @ParameterizedTest(name = "{0} gives every phrase a single owner")
    @EnumSource(Language.class)
    void noPhraseIsOwnedByTwoActions(Language language) {
        SystemSession.getInstance().setLanguage(language);
        try {
            SortedMap<String, SortedSet<String>> owners = new TreeMap<>();
            for (String actionId : actionIds()) {
                for (String phrase : AiActionLocalizations.phrasesForAction(actionId)) {
                    String spoken = AliasPhrase.parse(phrase).spokenText().trim().toLowerCase(Locale.ROOT);
                    if (!spoken.isEmpty()) {
                        owners.computeIfAbsent(spoken, key -> new TreeSet<>()).add(actionId);
                    }
                }
            }

            List<String> clashes = owners.entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 1)
                    .map(entry -> "\"" + entry.getKey() + "\" -> " + entry.getValue())
                    .toList();

            assertTrue(clashes.isEmpty(),
                    () -> language + " has phrases the reflex gate can never resolve:\n  "
                            + String.join("\n  ", clashes));
        } finally {
            SystemSession.getInstance().setLanguage(Language.EN);
        }
    }

    /**
     * Commands and queries together - the reflex gate covers both, so a command and a query sharing a phrase
     * is as fatal as two commands sharing one.
     */
    private static List<String> actionIds() {
        return Stream.concat(
                        CommandRegistry.getInstance().byId().entrySet().stream().map(Map.Entry::getKey),
                        QueryRegistry.getInstance().byId().entrySet().stream().map(Map.Entry::getKey))
                .distinct()
                .toList();
    }
}
