package elite.intel.companion.model.execution;

import com.google.gson.JsonObject;

/**
 * Unit of work handed to {@code ExecutionGateway}. The gateway never sees a {@code Thought}.
 * <p>
 * The execution lane is derived by resolving {@code toolName} against the tool registries: the
 * resolved tool's type (command / query / macro / system function) selects the lane.
 *
 * @param requestId  unique id for correlation/diagnostics
 * @param toolName   name of the tool/command/query to run
 * @param arguments  parsed JSON arguments
 * @param toolCallId correlation id of the model tool-call this settles, so a command handler's own narration
 *                   is recorded as this call's {@code tool} result (see {@code ActiveToolCall}); {@code null}
 *                   for calls that need no tool-result pairing (system functions, reflexes with no id)
 * @param commanderInput the commander's raw utterance that led to this call, passed to the handler as its
 *                   {@code originalUserInput} so handlers that match a spoken name (e.g. "is B 1 landable")
 *                   can resolve it. Never null (normalized to ""); empty when there is no originating utterance.
 */
public record ExecutionRequest(
        String requestId,
        String toolName,
        JsonObject arguments,
        String toolCallId,
        String commanderInput
) {
    public ExecutionRequest {
        if (commanderInput == null) commanderInput = "";
    }

    /** Convenience constructor for a request that needs no tool-call pairing ({@code toolCallId} is null). */
    public ExecutionRequest(String requestId, String toolName, JsonObject arguments) {
        this(requestId, toolName, arguments, null, "");
    }

    /**
     * Convenience constructor for a request with a tool-call id but no originating commander utterance.
     */
    public ExecutionRequest(String requestId, String toolName, JsonObject arguments, String toolCallId) {
        this(requestId, toolName, arguments, toolCallId, "");
    }
}
