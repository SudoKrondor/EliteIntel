package elite.intel.ai.brain.actions;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the outcome contract owned by {@link CommandOutcome}: builders write the shared
 * text-to-speech key and the criticality flag, and the readers tolerate null / foreign / malformed
 * payloads (a query payload is a valid outcome with no criticality).
 */
class CommandOutcomeTest {

    @Test
    void speakCarriesTextAndIsNotCritical() {
        JsonObject outcome = CommandOutcome.speak("Route plotted");

        assertEquals("Route plotted", outcome.get(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE).getAsString());
        assertEquals("Route plotted", CommandOutcome.spokenText(outcome));
        assertFalse(CommandOutcome.isCritical(outcome));
    }

    @Test
    void criticalCarriesTextAndIsCritical() {
        JsonObject outcome = CommandOutcome.critical("No trade route found");

        assertEquals("No trade route found", CommandOutcome.spokenText(outcome));
        assertTrue(CommandOutcome.isCritical(outcome));
    }

    @Test
    void nullTextBecomesEmptyAndStaysReadable() {
        JsonObject outcome = CommandOutcome.speak(null);

        assertEquals("", CommandOutcome.spokenText(outcome));
        assertFalse(CommandOutcome.isCritical(outcome));
    }

    @Test
    void readersTolerateNullOutcome() {
        assertEquals("", CommandOutcome.spokenText(null));
        assertFalse(CommandOutcome.isCritical(null));
    }

    @Test
    void foreignPayloadIsAValidSilentOutcome() {
        // A not-yet-migrated command (null) or a plain data payload speaks nothing and is not critical.
        JsonObject dataPayload = new JsonObject();
        dataPayload.addProperty("fuel", 0.42);

        assertEquals("", CommandOutcome.spokenText(dataPayload));
        assertFalse(CommandOutcome.isCritical(dataPayload));
    }

    @Test
    void queryPayloadWithSpeechIsReadAsOutcome() {
        // Queries already produce the text-to-speech key; the same reader works on their payload.
        JsonObject queryPayload = new JsonObject();
        queryPayload.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, "Fuel at 42 percent");

        assertEquals("Fuel at 42 percent", CommandOutcome.spokenText(queryPayload));
        assertFalse(CommandOutcome.isCritical(queryPayload));
    }
}
