package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
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
 * Guards "turn off the radio", which answered "I don't have a function for that, you gotta use the comms
 * panel directly" and only routed once the commander named "radio chatter" himself.
 *
 * <p>{@code toggle_radio} was reachable in principle but not by that wording. Its alias group carried
 * "toggle radio", "radio traffic" and "radio transmissions {state:true/false}", and none of those can ever
 * produce a reflex: a phrase with no parameter block cannot supply the {@code state} the action declares,
 * and {@code {state:true/false}} is a variable value the model still has to read out of the utterance - see
 * {@link elite.intel.ai.brain.i18n.AliasPhrase}. So every radio order in every locale reached the local
 * model, which read "radio" as the ship's comms rather than as this setting.
 *
 * <p>Pinning the state as a literal per phrase ({@code turn radio off {state:false}}) makes the on and off
 * orders fully resolved by the alias, so they dispatch with no model in the loop and the argument already
 * decided. Nothing else in the game or the app is called a radio, so the unqualified order is unambiguous.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RadioReflexTest {

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
    void theReportedUtteranceTurnsTheRadioOff() {
        Optional<ReflexResolver.Reflex> reflex = resolve("turn off radio");
        assertTrue(reflex.isPresent(), "\"turn off radio\" must never depend on the local model");
        assertEquals("toggle_radio", reflex.get().actionId());
        assertEquals("false", reflex.get().arguments().get("state"));
    }

    @ParameterizedTest(name = "EN \"{0}\" -> state={1}")
    @CsvSource({
            "turn off radio, false",
            "turn radio off, false",
            "radio off, false",
            "turn on radio, true",
            "turn radio on, true",
            "radio on, true",
    })
    void bothWordOrdersCarryTheirOwnState(String utterance, String expectedState) {
        Optional<ReflexResolver.Reflex> reflex = resolve(utterance);
        assertTrue(reflex.isPresent(), () -> "\"" + utterance + "\" must resolve without the LLM");
        assertEquals("toggle_radio", reflex.get().actionId());
        assertEquals(expectedState, reflex.get().arguments().get("state"));
    }

    @ParameterizedTest(name = "{0} \"{1}\" -> state={2}")
    @CsvSource({
            "DE, funk aus, false",
            "DE, funk an, true",
            "ES, apaga la radio, false",
            "ES, enciende la radio, true",
            "FR, éteins la radio, false",
            "FR, allume la radio, true",
            "IT, spegni la radio, false",
            "IT, accendi la radio, true",
            "PT, desliga o rádio, false",
            "PT, liga o rádio, true",
            "PTBZ, desliga o rádio, false",
            "PTBZ, liga o rádio, true",
            "RU, выключи радио, false",
            "RU, включи радио, true",
            "UK, вимкни радіо, false",
            "UK, увімкни радіо, true",
    })
    void everyLocaleCanSwitchTheRadioWithoutQualifyingIt(Language language, String utterance, String expectedState) {
        SystemSession.getInstance().setLanguage(language);
        try {
            Optional<ReflexResolver.Reflex> reflex = resolve(utterance);
            assertTrue(reflex.isPresent(),
                    () -> language + " \"" + utterance + "\" must resolve without the LLM");
            assertEquals("toggle_radio", reflex.get().actionId());
            assertEquals(expectedState, reflex.get().arguments().get("state"));
        } finally {
            SystemSession.getInstance().setLanguage(Language.EN);
        }
    }

    /**
     * "toggle radio" names no state, so it must keep taking the LLM path rather than being executed on a
     * guess - the alias cannot say whether the commander wants it on or off.
     */
    @Test
    void aBareToggleStillNeedsTheModel() {
        assertTrue(resolve("toggle radio").isEmpty(),
                "\"toggle radio\" pins no state and must not dispatch on a guess");
    }

    private Optional<ReflexResolver.Reflex> resolve(String utterance) {
        return resolver.resolve(utterance, GameStateSnapshot.capture(
                Status.detached(PlayerSituation.IN_SHIP_SUPERCRUISE)));
    }
}
