package elite.intel.ai.ears;

/**
 * Published on the EventBus when the application starts or finishes speaking via TTS.
 * Subscribers (e.g. STT) use this to suppress voice recognition while the AI is speaking.
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
