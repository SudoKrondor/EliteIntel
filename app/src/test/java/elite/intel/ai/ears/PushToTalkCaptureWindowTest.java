package elite.intel.ai.ears;

import org.junit.jupiter.api.Test;

import static elite.intel.ai.ears.PushToTalkCaptureWindow.Frame.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The bug this guards: push-to-talk used to be a filter on the finished transcript while the noise gate still
 * ran underneath it, so anything said before the press was already sitting in the buffer and went to the
 * speech recognizer the moment the button went down. The window must collect nothing at all with the button
 * up, and everything between press and release.
 * <p>
 * The second bug it guards: the release used to cut the recording where it landed, so a commander who let go
 * on the last word sent a severed syllable to the recogniser - "FTL" came back as "f" and was dropped as too
 * short to be a phrase. Recording must continue for the tail past the release.
 */
class PushToTalkCaptureWindowTest {

    private static final int FRAME = 3200; // 100ms of 16 kHz 16-bit mono
    private static final int MAX = FRAME * 5;
    private static final int TAIL = FRAME * 2;
    private static final int NO_TAIL = 0;

    @Test
    void framesWithTheButtonUpAreDiscarded() {
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX, TAIL);
        for (int i = 0; i < 10; i++) {
            assertEquals(DISCARD, window.onFrame(false, FRAME));
        }
        assertFalse(window.isOpen());
        assertEquals(0, window.collectedBytes());
    }

    @Test
    void everythingBetweenPressAndReleaseIsCollected() {
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX, NO_TAIL);
        window.onFrame(false, FRAME);
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertTrue(window.isOpen());
        // The release frame is collected too - the last word is in it - and with no tail configured it closes
        // the capture.
        assertEquals(CLOSE_ON_RELEASE, window.onFrame(false, FRAME));
        assertFalse(window.isOpen());
        assertEquals(FRAME * 3, window.collectedBytes());
    }

    @Test
    void recordingContinuesForTheTailAfterTheRelease() {
        // The word the commander was still saying when they let go lives in these frames.
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX, TAIL);
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(COLLECT, window.onFrame(false, FRAME));
        assertTrue(window.isOpen(), "the tail is part of the capture");
        assertEquals(CLOSE_ON_RELEASE, window.onFrame(false, FRAME));
        assertFalse(window.isOpen());
        assertEquals(FRAME * 3, window.collectedBytes());
    }

    @Test
    void theTailDoesNotOutliveTheCaptureItBelongsTo() {
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX, TAIL);
        window.onFrame(true, FRAME);
        window.onFrame(false, FRAME);
        assertEquals(CLOSE_ON_RELEASE, window.onFrame(false, FRAME));
        // The button is still up: the frames after the tail are room noise like any other.
        assertEquals(DISCARD, window.onFrame(false, FRAME));
        assertEquals(DISCARD, window.onFrame(false, FRAME));
    }

    @Test
    void aPressInsideTheTailContinuesTheSameCapture() {
        // Letting go and pressing again mid-order is one utterance, not two: the tail is taken back rather
        // than the capture being split at the gap.
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX, TAIL);
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(COLLECT, window.onFrame(false, FRAME));
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertTrue(window.isOpen());
        // ...and the tail it gets afterwards is a full one, not the remainder of the first.
        assertEquals(COLLECT, window.onFrame(false, FRAME));
        assertEquals(CLOSE_ON_RELEASE, window.onFrame(false, FRAME));
        assertEquals(FRAME * 5, window.collectedBytes());
    }

    @Test
    void whatWasSaidBeforeThePressIsNotInTheCapture() {
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX, NO_TAIL);
        for (int i = 0; i < 20; i++) window.onFrame(false, FRAME);
        window.onFrame(true, FRAME);
        window.onFrame(false, FRAME);
        assertEquals(FRAME * 2, window.collectedBytes());
    }

    @Test
    void aQuietFrameInsideTheWindowIsStillCollected() {
        // The frame carries no level: a pause mid-order, or speech too quiet to trip the noise gate, cannot
        // end a capture the commander is still holding the button for.
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX, TAIL);
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertTrue(window.isOpen());
    }

    @Test
    void aTapShorterThanOneFrameStillCaptures() {
        // The caller treats a press seen since the last frame as held, so the window opens and closes on the
        // same frame rather than the tap being lost between two reads - and the tail gives the commander who
        // started speaking on the tap somewhere to say it.
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX, TAIL);
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(COLLECT, window.onFrame(false, FRAME));
        assertEquals(CLOSE_ON_RELEASE, window.onFrame(false, FRAME));
    }

    @Test
    void aFullCaptureIsRetiredAndTheNextFrameStartsAnother() {
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX, TAIL);
        for (int i = 0; i < 4; i++) assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(CLOSE_ON_MAX_LENGTH, window.onFrame(true, FRAME));
        assertFalse(window.isOpen());
        // Still held: the next frame opens a fresh capture rather than the button going deaf.
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(FRAME, window.collectedBytes());
    }

    @Test
    void theSizeCapEndsATailThatWouldOverrunIt() {
        // A capture already at the cap cannot keep growing just because it is in its tail.
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX, TAIL);
        for (int i = 0; i < 4; i++) assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(CLOSE_ON_RELEASE, window.onFrame(false, FRAME));
        assertFalse(window.isOpen());
        assertEquals(MAX, window.collectedBytes());
    }

    @Test
    void resetAbandonsAnOpenCapture() {
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX, TAIL);
        window.onFrame(true, FRAME);
        window.reset();
        assertFalse(window.isOpen());
        // A window abandoned mid-capture is not continued by the next press: it starts from zero.
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(FRAME, window.collectedBytes());
    }

    @Test
    void resetAbandonsARunningTail() {
        PushToTalkCaptureWindow window = new PushToTalkCaptureWindow(MAX, TAIL);
        window.onFrame(true, FRAME);
        window.onFrame(false, FRAME);
        window.reset();
        assertFalse(window.isOpen());
        // The audio line was reopened under it: nothing from before the reset may be finished off now.
        assertEquals(DISCARD, window.onFrame(false, FRAME));
        assertEquals(COLLECT, window.onFrame(true, FRAME));
        assertEquals(FRAME, window.collectedBytes());
    }
}
