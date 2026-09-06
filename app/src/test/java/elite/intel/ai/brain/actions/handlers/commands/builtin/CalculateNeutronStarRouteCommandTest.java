package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static elite.intel.ai.brain.actions.handlers.commands.builtin.CalculateNeutronStarRouteCommand.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The parameter reading, which is all of this command that runs without a session, a database and a
 * minute of Spansh. Both arguments reach it as whatever the model wrote, so the shapes it accepts - and
 * the ones it refuses - are what decides which route gets plotted.
 */
class CalculateNeutronStarRouteCommandTest {

    @Test
    void aDecimalIsTheNumberItLooksLikeAndNotItsDigits() {
        // A model answering 6.0 means six. Reading it as sixty would plot a route nobody asked for.
        assertEquals(6, parseWholeNumber("6.0"));
        assertEquals(70, parseWholeNumber("70.0"));
        assertEquals(70, parseWholeNumber("70"));
        assertEquals(7, parseWholeNumber("6.5"));
    }

    @Test
    void wordierAnswersStillYieldTheirNumber() {
        assertEquals(70, parseWholeNumber("70 percent"));
        assertNull(parseWholeNumber("as high as possible"));
        assertNull(parseWholeNumber(null));
    }

    @Test
    void anUnstatedOrImpossibleEfficiencyReadsAsUnstated() {
        JsonObject params = new JsonObject();
        assertNull(readNumber(params, "efficiency", 1, 100), "absent");

        params.addProperty("efficiency", 70);
        assertEquals(70, readNumber(params, "efficiency", 1, 100));

        // Out of range counts as unstated rather than as an error: the commander gave an order, and a
        // misheard digit is no reason to hand it back to them.
        params.addProperty("efficiency", 700);
        assertNull(readNumber(params, "efficiency", 1, 100));
        params.addProperty("efficiency", 0);
        assertNull(readNumber(params, "efficiency", 1, 100));
    }

    @Test
    void anUnmentionedSuperchargeIsANo() {
        JsonObject params = new JsonObject();
        assertFalse(readFlag(params, "supercharge"), "never raised - plot the plain route, do not ask");

        params.addProperty("supercharge", true);
        assertTrue(readFlag(params, "supercharge"));
        params.addProperty("supercharge", false);
        assertFalse(readFlag(params, "supercharge"));

        // Models answer a boolean as a quoted string often enough to be worth pinning.
        params.addProperty("supercharge", "true");
        assertTrue(readFlag(params, "supercharge"));
    }
}
