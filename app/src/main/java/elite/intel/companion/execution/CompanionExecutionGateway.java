package elite.intel.companion.execution;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.brain.actions.handlers.QueryHandlerFactory;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.tools.SystemFunction;
import elite.intel.companion.tools.SystemFunctionRegistry;
import elite.intel.companion.tools.SystemFunctionResultFields;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * {@link ExecutionGateway} that runs tool-calls by reusing the single execution contract every tool shares:
 * {@code IntelAction#handle}. Commands, queries, macros and system functions are all {@code IntelAction}s,
 * so the gateway resolves {@code toolName} to one and calls {@code handle}, agnostic to the kind. Handlers
 * are called directly (not via {@code ResponseRouter}), so none of the legacy auto-speech fires - the owning
 * {@code Thought} decides what is spoken (via the {@code speak} system function), and the raw result flows
 * back as a tool result.
 * <p>
 * Lanes (see COMPANION_ARCHITECTURE.md §1.9): commands and macros run on a single serialized action lane
 * (game input must be sequential); read-only queries and system functions run on the parallel lane (system
 * functions are not game input; the speech/memory gateways they call own their own ordering/threading).
 * <p>
 * Result is dispatch/execution status, not a game fact: a {@code handle} that returns a payload (queries,
 * data-returning system functions) yields it as-is; a side-effect {@code handle} that returns null yields a
 * {@code completed_by_executor} status. The gateway never writes to memory. The lifecycle-only terminator
 * {@code nothing_to_do} is intercepted by the {@code Thought} and normally never reaches the gateway.
 */
public final class CompanionExecutionGateway implements ExecutionGateway {

    /** Dispatch-status result for a side-effect tool; the gateway-only "tool" field names which one ran. */
    private static final String TOOL = "tool";
    private static final String STATUS_COMPLETED = "completed_by_executor";

    private final Map<String, IntelAction> commandHandlers;
    private final Map<String, IntelQuery> queryHandlers;
    private final Map<String, SystemFunction> systemFunctions;
    private final Executor actionLane;
    private final Executor queryLane;

    /** Production: handler maps + system-function registry; serialized action lane + parallel query lane. */
    public CompanionExecutionGateway() {
        this(CommandHandlerFactory.getInstance().registerCommandHandlers(),
                QueryHandlerFactory.getInstance().registerQueryHandlers(),
                loadedSystemFunctions(),
                Executors.newSingleThreadExecutor(daemon("companion-action")),
                Executors.newCachedThreadPool(daemon("companion-query")));
    }

    /** Test seam: inject handler/system-function maps and executors (e.g. synchronous). */
    CompanionExecutionGateway(Map<String, IntelAction> commandHandlers,
                              Map<String, IntelQuery> queryHandlers,
                              Map<String, SystemFunction> systemFunctions,
                              Executor actionLane,
                              Executor queryLane) {
        this.commandHandlers = commandHandlers;
        this.queryHandlers = queryHandlers;
        this.systemFunctions = systemFunctions;
        this.actionLane = actionLane;
        this.queryLane = queryLane;
    }

    @Override
    public CompletableFuture<JsonObject> submit(ExecutionRequest request) {
        String toolName = request.toolName();
        // Command precedence mirrors ResponseRouter; ids do not collide across registries.
        IntelAction command = commandHandlers.get(toolName);
        if (command != null) {
            return run(actionLane, command, request);
        }
        IntelAction query = queryHandlers.get(toolName);
        if (query != null) {
            return run(queryLane, query, request);
        }
        IntelAction systemFunction = systemFunctions.get(toolName);
        if (systemFunction != null) {
            return run(queryLane, systemFunction, request);
        }
        return CompletableFuture.failedFuture(
                new IllegalArgumentException("Unknown companion tool: " + toolName));
    }

    /** Submits work to a lane; a future cancelled before the task starts skips execution entirely. */
    private CompletableFuture<JsonObject> run(Executor lane, IntelAction tool, ExecutionRequest request) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        lane.execute(() -> {
            if (future.isCancelled()) {
                return; // cancelled while queued: skip (an already-started action/macro is never interrupted)
            }
            try {
                future.complete(execute(tool, request));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /** Single execution path: a non-null handle result is the payload; null means a side-effect dispatch. */
    private JsonObject execute(IntelAction tool, ExecutionRequest request) throws Exception {
        JsonObject result = tool.handle(request.toolName(), request.arguments(), "");
        if (result != null) {
            return result;
        }
        JsonObject status = new JsonObject();
        status.addProperty(SystemFunctionResultFields.STATUS, STATUS_COMPLETED);
        status.addProperty(TOOL, request.toolName());
        return status;
    }

    private static Map<String, SystemFunction> loadedSystemFunctions() {
        SystemFunctionRegistry registry = SystemFunctionRegistry.getInstance();
        if (registry.byId().isEmpty()) {
            registry.load();
        }
        return registry.byId();
    }

    private static java.util.concurrent.ThreadFactory daemon(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }
}
