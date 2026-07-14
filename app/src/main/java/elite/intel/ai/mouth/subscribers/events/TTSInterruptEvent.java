package elite.intel.ai.mouth.subscribers.events;

/** Interrupts all interruptible TTS work, or one request when {@link #requestId()} is non-null. */
public class TTSInterruptEvent {

    private final boolean hasAiReference;
    private final String requestId;

    public TTSInterruptEvent() {
        this(false, null);
    }
    public TTSInterruptEvent(boolean hasAiReference) {
        this(hasAiReference, null);
    }

    /** Targets cancellation to one speech request; a null id on the other constructors interrupts the queue. */
    public TTSInterruptEvent(String requestId) {
        this(false, requestId);
    }

    private TTSInterruptEvent(boolean hasAiReference, String requestId) {
        this.hasAiReference = hasAiReference;
        this.requestId = requestId;
    }

    public boolean hasAiReference() {
        return this.hasAiReference;
    }

    /** Correlation id for targeted cancellation; null means a global barge-in/urgent interrupt. */
    public String requestId() {
        return requestId;
    }
}
