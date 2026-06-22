package elite.intel.companion.model.speech;

import elite.intel.companion.model.Urgency;

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
) {}
