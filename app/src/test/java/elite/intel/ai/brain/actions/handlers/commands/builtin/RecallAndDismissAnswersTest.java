package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.util.StringUtls;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Recall and dismiss share one keystroke - Frontier binds both to a single toggle - and they used to share
 * one answer too, picked from the ship's landed flag. Every dismissal made while the ship was not already
 * sitting beside the commander was confirmed with "Coming back to get you".
 */
class RecallAndDismissAnswersTest {

    @Test
    void eachCommandConfirmsWhatWasAskedOfIt() {
        String recall = new ReturnToSurfaceCommand().execute(new JsonObject(), null);
        String dismiss = new DismissShipToOrbitCommand().execute(new JsonObject(), null);

        assertEquals(StringUtls.localizedResponse("speech.shipRecall"), recall);
        assertEquals(StringUtls.localizedResponse("speech.shipDismissed"), dismiss);
        assertNotEquals(recall, dismiss);
    }
}
