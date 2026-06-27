package elite.intel.companion.execution;

import com.google.gson.JsonObject;
import elite.intel.companion.model.execution.ExecutionRequest;

import java.util.concurrent.CompletableFuture;

/**
 * The single door for executing tool-calls in companion mode. Executes {@code ExecutionRequest}s
 * (never {@code Thought}s) on a serialized action lane (actions/macros) or a parallel query lane.
 * <p>
 * The gateway never writes to memory; real game changes arrive later via the journal/status path.
 */
public interface ExecutionGateway {

    /**
     * Submits a tool-call for execution on the lane selected by its operation type.
     *
     * @return a future completing with the operation result (query data; null/empty for action/macro
     *         side effects); cancel it to skip the operation if it has not started yet
     */
    CompletableFuture<JsonObject> submit(ExecutionRequest request);
}
