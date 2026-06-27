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
 */
public record ExecutionRequest(
        String requestId,
        String toolName,
        JsonObject arguments
) {}
