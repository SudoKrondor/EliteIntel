package elite.intel.junit.util;

import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "Address Me" off means no form of address at all - not a different one, and not a comma left standing
 * where one used to be.
 */
class AddressMeTest {

    private final PlayerSession session = PlayerSession.getInstance();

    @AfterEach
    void restoreDefault() {
        session.setAddressMeOn(true);
    }

    @Test
    void offDrawsNoFormOfAddress() {
        session.setAddressMeOn(false);
        assertEquals("", session.getVariablePlayerName());
    }

    @Test
    void onStillDrawsOne() {
        session.setAddressMeOn(true);
        assertFalse(session.getVariablePlayerName().isBlank(),
                "with addressing on there is always a form to use, down to a bare Commander");
    }

    /**
     * The generic address the LLM and the game use is removed with the comma that carried it, rather than
     * swapped for a personal form.
     */
    @Test
    void offRemovesTheGenericAddress() {
        session.setAddressMeOn(false);
        assertEquals("Docking request granted.", StringUtls.sanitizeTts("Docking request granted, Commander."));
        assertEquals("Welcome aboard.", StringUtls.sanitizeTts("Welcome aboard, pilot."));
        assertEquals("Fuel is low.", StringUtls.sanitizeTts("Commander, fuel is low."));
    }

    /**
     * The sentence templates carry the comma themselves, so an absent address must not leave one behind.
     */
    @Test
    void offClosesTheGapTheAddressLeftInASentence() {
        session.setAddressMeOn(false);
        assertEquals("Welcome back aboard.", StringUtls.sanitizeTts("Welcome back aboard, ."));
        assertEquals("Krondor is ready. All systems online.",
                StringUtls.sanitizeTts(", Krondor is ready. All systems online."));
        assertEquals("Good morning.", StringUtls.sanitizeTts("Good morning, !"));
    }

    /**
     * With addressing on the generic form is still replaced by one of the commander's own, which is the
     * behaviour this setting guards rather than changes.
     */
    @Test
    void onStillPersonalizesTheGenericAddress() {
        session.setAddressMeOn(true);
        String spoken = StringUtls.sanitizeTts("Docking request granted, Commander.");
        assertTrue(spoken.startsWith("Docking request granted"), spoken);
        assertFalse(spoken.equals("Docking request granted."), "an address should have been drawn: " + spoken);
    }

    /**
     * Text that never had an address is untouched - the tidy-up runs on every spoken line.
     */
    @Test
    void ordinaryPunctuationSurvives() {
        session.setAddressMeOn(true);
        assertEquals("Fuel low, shields up, weapons hot.",
                StringUtls.sanitizeTts("Fuel low, shields up, weapons hot."));
    }
}
