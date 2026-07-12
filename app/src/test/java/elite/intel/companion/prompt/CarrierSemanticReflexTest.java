package elite.intel.companion.prompt;

import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import elite.intel.ai.embed.SemanticSearchProvider;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The invariant the carrier-query collapse exists to buy: carrier questions reach the semantic reflex, so they
 * are answered from data without ever consulting the LLM.
 * <p>
 * They could not before. {@link SemanticReflexResolver} only fires when the top match clears the runner-up by its
 * gap, and the old tools came in fleet/squadron pairs whose trigger phrases were a phrase and its own superstring
 * ("carrier status" / "squadron carrier status"). Those embed too closely to separate, so every carrier question
 * fell through to a small local model that then picked a sibling at random - the misroute this change removes.
 * Now one tool answers each question and {@code CarrierOwnership} reads the owner from the words.
 * <p>
 * Opt-in ({@code embedding-manual}); loads the real 118 MB model. Run with {@code ./gradlew embeddingTest}.
 */
@Tag("embedding-manual")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CarrierSemanticReflexTest {

    @BeforeAll
    void boot() {
        Cypher.initializeKey();
        Database.init().close();
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        SystemSession.getInstance().setLanguage(Language.EN);
        Assumptions.assumeTrue(SemanticSearchProvider.matcher() != null,
                "embedding model unavailable; semantic reflex degrades to the LLM path by design");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource({
            "carrier status,                            query_carrier_status",
            "squadron carrier status,                   query_carrier_status",
            "carrier tritium,                           query_carrier_status",
            "squadron carrier fuel level,               query_carrier_status",

            "squadron carrier route,                    query_carrier_voyage",
            "where is carrier going,                    query_carrier_voyage",

            "carrier ETA,                               query_carrier_departure_eta",
            "squadron carrier ETA,                      query_carrier_departure_eta",
            "when does squadron carrier arrive,         query_carrier_departure_eta",
    })
    void carrierPhrasesFireTheSemanticReflex(String utterance, String expectedActionId) {
        Optional<String> resolved = new SemanticReflexResolver().resolve(utterance.strip());
        assertEquals(Optional.of(expectedActionId), resolved,
                "\"" + utterance.strip() + "\" must dispatch without the LLM; empty means a close runner-up "
                        + "pushed it back onto the model");
    }

    /**
     * Characterises the reflex's remaining blind spot, so a change in it is noticed rather than discovered.
     * <p>
     * These two phrases DO win their match outright - {@code query_carrier_voyage} scores 1.0000 on both - but a
     * carrier COMMAND lands inside the reflex's 0.05 runner-up gap and pushes them onto the LLM: {@code carrier
     * route} is shadowed by {@code calculate_fleet_carrier_route} (0.9636, via its bare "carrier jump route"
     * alias) and {@code where is squadron carrier going} by {@code navigate_to_squadron_carrier} (0.9525).
     * <p>
     * Neither is the fleet-vs-squadron ambiguity this change removed: no carrier QUERY is the runner-up any more.
     * This is a query-vs-command tie, and the LLM now chooses between three carrier queries instead of ten.
     */
    @ParameterizedTest(name = "[{index}] \"{0}\" still needs the LLM")
    @CsvSource({"carrier route", "where is squadron carrier going"})
    void aCarrierCommandStillShadowsTheseVoyagePhrases(String utterance) {
        assertEquals(Optional.empty(), new SemanticReflexResolver().resolve(utterance.strip()),
                "\"" + utterance.strip() + "\" now reflexes: the shadowing command's alias must have changed. "
                        + "Move it into carrierPhrasesFireTheSemanticReflex - this is an improvement.");
    }
}
