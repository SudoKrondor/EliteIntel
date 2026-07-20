package elite.intel.ai.brain.vega.model.speech;

import elite.intel.ai.brain.vega.model.Urgency;

/**
 * Unit of work handed to {@code SpeechGateway}. The gateway never sees a {@code Thought}.
 *
 * @param requestId  unique id for correlation/diagnostics
 * @param text       text to vocalize
 * @param urgency    urgent speech may interrupt current speech
 */
public record SpeechRequest(
        String requestId,
        String text,
        Urgency urgency
) {
    /** Rejects speech that cannot be correlated or vocalized before it reaches the asynchronous Mouth pipeline. */
    public SpeechRequest {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Speech request id must not be blank");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Speech text must not be blank");
        }
        if (urgency == null) {
            throw new IllegalArgumentException("Speech urgency must not be null");
        }
    }
}
