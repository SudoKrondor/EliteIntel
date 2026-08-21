package elite.intel.ai.ears;

import org.junit.jupiter.api.Test;

import static elite.intel.ai.ears.PushToTalkCaptureWindow.Frame.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The bug this guards: push-to-talk used to be a filter on the finished transcript while the noise gate still
 * ran underneath it, so anything said before the press was already sitting in the buffer and went to the
 * speech recognizer the moment the button went down. The window must collect nothing at all with the button
 * up, and everything between press and release.
 */
class PushToTalkCaptureWindowTest {

    private static final int FRAME = 3200; // 100ms of 16 kHz 16-bit mono
    private static final int MAX = FRAME * 5;

    @Test
    void framesWithTheButtonUpAreDiscarded() {
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX);
        for (int i = 0; i < 10; i++) {
            assertEquals(DISCARD, window.onFrame(false, FRAME));
        }
        assertFalse(window.isOpen());
        assertEquals(0, window.collectedBytes());
    }

    @Test
    void everythingBetweenPressAndReleaseIsCollected() {
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX);
        window.onFrame(false, FRAME);
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertTrue(window.isOpen());
        // The release frame is collected too - the last word is in it - and closes the capture.
        assertEquals(CLOSE_ON_RELEASE, window.onFrame(false, FRAME));
        assertFalse(window.isOpen());
        assertEquals(FRAME * 3, window.collectedBytes());
    }

    @Test
    void whatWasSaidBeforeThePressIsNotInTheCapture() {
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX);
        for (int i = 0; i < 20; i++) window.onFrame(false, FRAME);
        window.onFrame(true, FRAME);
        window.onFrame(false, FRAME);
        assertEquals(FRAME * 2, window.collectedBytes());
    }

    @Test
    void aQuietFrameInsideTheWindowIsStillCollected() {
        // The frame carries no level: a pause mid-order, or speech too quiet to trip the noise gate, cannot
        // end a capture the commander is still holding the button for.
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX);
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertTrue(window.isOpen());
    }

    @Test
    void aTapShorterThanOneFrameStillCaptures() {
        // The caller treats a press seen since the last frame as held, so the window opens and closes on the
        // same frame rather than the tap being lost between two reads.
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX);
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(CLOSE_ON_RELEASE, window.onFrame(false, FRAME));
    }

    @Test
    void aFullCaptureIsRetiredAndTheNextFrameStartsAnother() {
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX);
        for (int i = 0; i < 4; i++) assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(CLOSE_ON_MAX_LENGTH, window.onFrame(true, FRAME));
        assertFalse(window.isOpen());
        // Still held: the next frame opens a fresh capture rather than the button going deaf.
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(FRAME, window.collectedBytes());
    }

    @Test
    void resetAbandonsAnOpenCapture() {
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX);
        window.onFrame(true, FRAME);
        window.reset();
        assertFalse(window.isOpen());
        // A window abandoned mid-capture is not continued by the next press: it starts from zero.
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(FRAME, window.collectedBytes());
    }
}
