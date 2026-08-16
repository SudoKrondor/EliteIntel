package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.i18n.PhoneticInputNormalizer;
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
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards "transfer power of the systems", which was answered with a refusal - "power transfer is only
 * available to increase power to specific systems, not decrease it".
 *
 * <p>Nothing was wrong with the transcript. A blanket {@code of -> off} correction in the English normalizer
 * rewrote the commander's preposition, so the turn was matched and prompted as "transfer power OFF the
 * systems" - a request to shut power down, which no action offers, so the model quite reasonably declined.
 * The correction now names the phrases where "off" is genuinely what was said, and "power of/to the X"
 * collapses onto the authored "power to X" alias instead.
 *
 * <p>The second half of the fix is the alias family: the reducer had kept three candidates inside its margin
 * and handed the decision to a local model, when the phrasing is unambiguous to begin with. Each capacitor
 * now owns the long forms as well as the short one, so this resolves with no model in the loop.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PowerDistributionReflexTest {

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
     * The reported utterance, end to end: what STT produced, through the normalizer, into the reflex.
     */
    @Test
    void theReportedUtteranceDivertsPowerInsteadOfBeingRefused() {
        assertEquals("transfer_power_to_ship_systems", resolveSpoken("transfer power of the systems"));
    }

    /**
     * The commander reported the shields wording behaving identically, and in game it is the same capacitor.
     */
    @Test
    void theSameWordingForShieldsResolvesToo() {
        assertEquals("transfer_power_to_shields", resolveSpoken("transfer power of the shields"));
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({
            "power to systems, transfer_power_to_ship_systems",
            "power to the systems, transfer_power_to_ship_systems",
            "transfer power to systems, transfer_power_to_ship_systems",
            "divert power to systems, transfer_power_to_ship_systems",
            "pips to systems, transfer_power_to_ship_systems",
            "transfer power of the engines, transfer_power_to_engines",
            "divert power to the engines, transfer_power_to_engines",
            "pips to engines, transfer_power_to_engines",
            "transfer power of the weapons, transfer_power_to_weapons",
            "pips to weapons, transfer_power_to_weapons",
            "power to the shields, transfer_power_to_shields",
            "pips to shields, transfer_power_to_shields",
    })
    void everyCapacitorAnswersToTheSameFamilyOfPhrasings(String utterance, String expectedAction) {
        assertEquals(expectedAction, resolveSpoken(utterance));
    }

    /**
     * Balancing the distributor is a different command and must not be swallowed by the family above.
     */
    @Test
    void equalizingKeepsItsOwnCommand() {
        assertEquals("equalize_power", resolveSpoken("equalize power"));
    }

    /**
     * Pips are unavailable on foot, so the phrase must fall back to the normal path rather than firing a
     * binding the game will ignore.
     */
    @Test
    void theReflexRespectsBeingOnFoot() {
        String normalized = PhoneticInputNormalizer.normalize("transfer power of the systems");
        assertTrue(resolver.resolve(normalized, GameStateSnapshot.capture(
                Status.detached(PlayerSituation.ON_FOOT))).isEmpty());
    }

    /**
     * Resolves the way a live turn does: the normalizer first, then the reflex, in the situation the report
     * came from (in ship, orbital flight near a planet).
     */
    private String resolveSpoken(String utterance) {
        String normalized = PhoneticInputNormalizer.normalize(utterance);
        Optional<ReflexResolver.Reflex> reflex = resolver.resolve(normalized, GameStateSnapshot.capture(
                Status.detached(PlayerSituation.IN_SHIP_ORBIT)));
        assertTrue(reflex.isPresent(),
                () -> "\"" + utterance + "\" (normalized: \"" + normalized + "\") must not depend on the local model");
        return reflex.get().actionId();
    }
}
