package elite.intel.companion.model.llm;

import java.util.List;

/**
 * Result of an {@code LlmGateway} consciousness call. In the consciousness loop a valid response is
 * always one or more tool-calls; anything else (plain text, empty, malformed, unknown tool, invalid
 * schema) yields {@link Status#INVALID_RESPONSE} after the gateway's single repair/retry attempt.
 *
 * @param status           OK or INVALID_RESPONSE
 * @param toolInvocations  tool invocations in LLM response order (empty when INVALID_RESPONSE)
 * @param finishReason     the provider's stop reason (OpenAI/LM Studio {@code finish_reason}, Anthropic
 *                         {@code stop_reason}, Gemini {@code finishReason}), or null - diagnostic only: it tells
 *                         a {@code stop} (model chose to end) apart from a {@code length} (truncated) response
 * @param droppedText      free-text content the model returned alongside the tool-calls (or as a text-only
 *                         response), which the consciousness turn does not use - diagnostic only: it reveals a
 *                         model that "answered" as plain text instead of a {@code speak} call. Null when none.
 */
public record LlmResult(
        Status status,
        List<LlmToolInvocation> toolInvocations,
        String finishReason,
        String droppedText
) {
    public enum Status {
        OK,
        INVALID_RESPONSE
    }

    /**
     * Back-compat constructor for call sites (and tests) that carry no provider diagnostics: leaves
     * {@link #finishReason} and {@link #droppedText} null.
     */
    public LlmResult(Status status, List<LlmToolInvocation> toolInvocations) {
        this(status, toolInvocations, null, null);
    }

    public boolean isValid() {
        return status == Status.OK;
    }
}
