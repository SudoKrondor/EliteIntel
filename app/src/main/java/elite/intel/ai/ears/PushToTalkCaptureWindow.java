package elite.intel.ai.ears;

/**
 * The capture window while push-to-talk is armed: the mapped controller button, and nothing else, decides
 * what one frame of microphone audio is for.
 * <p>
 * This is a gate on the <i>recording</i>, not on the finished transcript. With the button up the frame is
 * thrown away where it was read, so a remark made before the press cannot sit in a buffer waiting to be swept
 * into the next capture, and room noise never costs an inference. The press opens the window, every frame
 * from then on belongs to the capture, and the release closes it and sends what was collected to be
 * transcribed. The voice-activity detector is not consulted at all: a pause mid-order cannot end an utterance
 * the commander is still holding the button for, and speech too quiet to trip the noise gate is still heard.
 * <p>
 * It owns the byte count so it can retire a capture at {@code maxUtteranceBytes} on its own; a button still
 * held simply opens the next one on the following frame.
 * <p>
 * It is deliberately ignorant of threads. The button level is sampled by the loop that owns the capture
 * window and handed in - the timing must belong to that thread, never to the one that reads the button.
 */
public final class PushToTalkCaptureWindow {

    /**
     * What the capture loop should do with the frame it just read.
     */
    public enum Frame {
        /**
         * The button is up: drop it.
         */
        DISCARD,
        /**
         * Inside the window: add it to the capture.
         */
        COLLECT,
        /**
         * The button came up: add it - the last word is in this frame - and transcribe the capture.
         */
        CLOSE_ON_RELEASE,
        /**
         * The capture is full with the button still held: add it and transcribe; the next frame starts a new one.
         */
        CLOSE_ON_MAX_LENGTH
    }

    private final int maxUtteranceBytes;
    private boolean open;
    private int collectedBytes;

    public PushToTalkCaptureWindow(int maxUtteranceBytes) {
        this.maxUtteranceBytes = maxUtteranceBytes;
    }

    /**
     * @param buttonHeld whether the mapped button is down for this frame - a press seen since the last frame
     *                   counts as held, so a tap shorter than one frame still opens the window
     * @param frameBytes the size of the frame being offered
     */
    public Frame onFrame(boolean buttonHeld, int frameBytes) {
        if (!open) {
            if (!buttonHeld) {
                return Frame.DISCARD;
            }
            open = true;
            collectedBytes = 0;
        }
        collectedBytes += frameBytes;
        if (!buttonHeld) {
            open = false;
            return Frame.CLOSE_ON_RELEASE;
        }
        if (collectedBytes >= maxUtteranceBytes) {
            open = false;
            return Frame.CLOSE_ON_MAX_LENGTH;
        }
        return Frame.COLLECT;
    }

    /**
     * Forgets an open window, so a capture abandoned by a mode change or a reopened audio line cannot be
     * continued by a later press.
     */
    public void reset() {
        open = false;
        collectedBytes = 0;
    }

    public boolean isOpen() {
        return open;
    }

    /**
     * How much of the current (or just-closed) capture has been collected, in bytes.
     */
    public int collectedBytes() {
        return collectedBytes;
    }
}
