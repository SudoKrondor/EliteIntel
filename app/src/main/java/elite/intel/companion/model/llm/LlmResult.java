package elite.intel.companion.model.llm;

import java.util.List;

/**
 * Result of an {@code LlmGateway} consciousness call. In the consciousness loop a valid response is
 * always one or more tool-calls; anything else (plain text, empty, malformed, unknown tool, invalid
 * schema) yields {@link Status#INVALID_RESPONSE} after the gateway's single repair/retry attempt.
 *
 * @param status     OK or INVALID_RESPONSE
 * @param toolInvocations  tool invocations in LLM response order (empty when INVALID_RESPONSE)
 */
public record LlmResult(
        Status status,
        List<LlmToolInvocation> toolInvocations
) {
    public enum Status {
        OK,
        INVALID_RESPONSE
    }

    public boolean isValid() {
        return status == Status.OK;
    }
}
