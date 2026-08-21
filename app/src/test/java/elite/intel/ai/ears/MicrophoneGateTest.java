package elite.intel.ai.ears;

import org.junit.jupiter.api.Test;

import static elite.intel.ai.ears.MicrophoneGate.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which gate is in force for one finished utterance. The rule that matters is the precedence: push-to-talk
 * supersedes Sleep/Wake, so a commander who deliberately held the button is never refused by a sleep flag
 * left over from a hands-free session.
 */
class MicrophoneGateTest {

    @Test
    void aHeldButtonIsAlwaysAnOrder() {
        assertEquals(OPEN_PUSH_TO_TALK, decide(true, true, false));
        assertEquals(OPEN_PUSH_TO_TALK, decide(true, true, true));
    }

    @Test
    void pushToTalkArmedAndButtonReleasedDiscards() {
        assertEquals(CLOSED_PUSH_TO_TALK, decide(false, true, false));
    }

    @Test
    void sleepIsNotConsultedUnderPushToTalk() {
        // Sleeping and push-to-talk armed: still the push-to-talk verdict, not the sleep one, because the
        // mapped button already gates the microphone and asking twice could only refuse a real order.
        assertEquals(CLOSED_PUSH_TO_TALK, decide(false, true, true));
    }

    @Test
    void handsFreeAndAsleepDefersToTheWakeBypass() {
        assertEquals(CLOSED_ASLEEP, decide(false, false, true));
    }

    @Test
    void handsFreeAndAwakeRoutesNormally() {
        assertEquals(OPEN_HANDS_FREE, decide(false, false, false));
    }
}
