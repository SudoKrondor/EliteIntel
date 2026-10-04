package elite.intel.ai.ears;

/**
 * Published by the active Mouth when the process transitions between zero and at least one accepted TTS request.
 * Subscribers use it to detect barge-in and observe playback lifecycle. While it is true a hands-free
 * capture admits only an interrupt phrase ({@link PlaybackEcho}); push-to-talk is unaffected.
 */
public class IsSpeakingEvent {
    boolean isSpeaking;

    public IsSpeakingEvent(boolean isSpeaking) {
        this.isSpeaking = isSpeaking;
    }
    public boolean isSpeaking() {
        return isSpeaking;
    }
}
