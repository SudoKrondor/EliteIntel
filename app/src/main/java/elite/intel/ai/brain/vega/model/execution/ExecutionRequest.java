package elite.intel.ai.brain.vega.model.execution;

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
 * @param commanderInput the commander's raw utterance that led to this call
 * @param matchInput canonical text used for tool selection and shown to the model
 * @param runtimeGenerationId process-local companion generation that owns this operation; zero for callers that
 *                   execute outside an installed companion runtime (primarily isolated gateway tests)
 */
public record ExecutionRequest(
        String requestId,
        String toolName,
        JsonObject arguments,
        String commanderInput,
        String matchInput,
        long runtimeGenerationId
) {
    public ExecutionRequest {
        if (commanderInput == null) commanderInput = "";
        if (matchInput == null) matchInput = commanderInput;
    }

    /** Convenience constructor for a request with no originating commander utterance. */
    public ExecutionRequest(String requestId, String toolName, JsonObject arguments) {
        this(requestId, toolName, arguments, "", "", 0L);
    }

    /** Backward-compatible constructor for a request outside an explicitly owned runtime generation. */
    public ExecutionRequest(String requestId, String toolName, JsonObject arguments, String commanderInput) {
        this(requestId, toolName, arguments, commanderInput, commanderInput, 0L);
    }

    /** Backward-compatible constructor when raw and canonical inputs are identical. */
    public ExecutionRequest(
            String requestId,
            String toolName,
            JsonObject arguments,
            String commanderInput,
            long runtimeGenerationId
    ) {
        this(requestId, toolName, arguments, commanderInput, commanderInput, runtimeGenerationId);
    }
}
