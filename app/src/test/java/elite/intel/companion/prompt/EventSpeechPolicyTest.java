package elite.intel.companion.prompt;

import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.Verbosity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** EVENT speak gating: an urgent event always speaks; otherwise QUIET stays silent, NORMAL/CHATTY may comment. */
class EventSpeechPolicyTest {

    @Test
    void urgentEventMayAlwaysSpeak() {
        assertTrue(EventSpeechPolicy.mayComment(Urgency.URGENT, Verbosity.QUIET));
    }

    @Test
    void quietSilencesNonUrgentEvents() {
        assertFalse(EventSpeechPolicy.mayComment(Urgency.NORMAL, Verbosity.QUIET));
    }

    @Test
    void normalAndChattyAllowComment() {
        assertTrue(EventSpeechPolicy.mayComment(Urgency.NORMAL, Verbosity.NORMAL));
        assertTrue(EventSpeechPolicy.mayComment(Urgency.NORMAL, Verbosity.CHATTY));
    }
}
