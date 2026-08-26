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
 * The release does not cut the recording where it lands. Commanders let go of the button as they say the last
 * word rather than after it, and a capture severed mid-syllable reaches the recogniser as the first phoneme
 * and nothing else - "FTL" came back as "f" and was dropped as too short to be a phrase. So a release opens a
 * <i>tail</i> instead: recording continues for {@code tailBytes} more audio and only then does the capture
 * retire. The tail costs that much added delay before transcription starts, which is the price of not losing
 * the word; a re-press inside the tail is the same utterance continuing and reopens the window rather than
 * starting a second one.
 * <p>
 * The tail is deliberately blind to what the audio contains. Ending it early on quiet would put the
 * voice-activity detector back in charge of when a push-to-talk capture ends, which is the thing this class
 * exists to prevent.
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
         * The button came up and the tail that followed it has run out: add the frame and transcribe.
         */
        CLOSE_ON_RELEASE,
        /**
         * The capture is full with the button still held: add it and transcribe; the next frame starts a new one.
         */
        CLOSE_ON_MAX_LENGTH
    }

    private final int maxUtteranceBytes;
    private final int tailBytes;
    private boolean open;
    private int collectedBytes;
    private int tailBytesRemaining;

    /**
     * @param maxUtteranceBytes the size at which a capture retires on its own, button or no button
     * @param tailBytes         how much audio to keep recording after the release, so a word the commander was
     *                          still saying is not cut off; zero closes the capture on the release frame
     */
    public PushToTalkCaptureWindow(int maxUtteranceBytes, int tailBytes) {
        this.maxUtteranceBytes = maxUtteranceBytes;
        this.tailBytes = tailBytes;
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
        if (buttonHeld) {
            // Held again: whatever tail was running belonged to a release the commander has taken back.
            tailBytesRemaining = tailBytes;
        }
        collectedBytes += frameBytes;

        if (!buttonHeld) {
            tailBytesRemaining -= frameBytes;
            // A tail cut short by the size cap still ends a capture the button has already released.
            if (tailBytesRemaining <= 0 || collectedBytes >= maxUtteranceBytes) {
                open = false;
                return Frame.CLOSE_ON_RELEASE;
            }
            return Frame.COLLECT;
        }
        if (collectedBytes >= maxUtteranceBytes) {
            open = false;
            return Frame.CLOSE_ON_MAX_LENGTH;
        }
        return Frame.COLLECT;
    }

    /**
     * Forgets an open window, so a capture abandoned by a mode change or a reopened audio line cannot be
     * continued by a later press - nor finished by a tail that outlived it.
     */
    public void reset() {
        open = false;
        collectedBytes = 0;
        tailBytesRemaining = 0;
    }

    /**
     * Whether audio is still being collected - true through the tail, which is part of the capture.
     */
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
