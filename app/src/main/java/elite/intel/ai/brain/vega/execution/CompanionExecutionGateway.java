package elite.intel.ai.brain.vega.execution;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.brain.actions.handlers.QueryHandlerFactory;
import elite.intel.ai.brain.actions.handlers.queries.IntelQuery;
import elite.intel.ai.brain.vega.CompanionConfig;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.ai.brain.vega.model.execution.ExecutionRequest;
import elite.intel.ai.brain.vega.tools.SystemFunction;
import elite.intel.ai.brain.vega.tools.SystemFunctionRegistry;
import elite.intel.ai.brain.vega.tools.SystemFunctionResultFields;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link ExecutionGateway} that runs tool-calls by reusing the single execution contract every tool shares:
 * {@code IntelAction#handle}. Commands, queries, macros and system functions are all {@code IntelAction}s,
 * so the gateway resolves {@code toolName} to one and calls {@code handle}, agnostic to the kind. Handlers
 * are called directly (not via {@code ResponseRouter}), so none of the legacy auto-speech fires - the owning
 * {@code Thought} decides what is spoken (via the {@code speak} system function), and the raw result flows
 * back as a tool result.
 * <p>
 * Threading: commands/macros run on one serialized action lane, so multi-step game/session side effects retain
 * submission order. Read-only queries run on a bounded parallel pool, so a slow lookup does not block unrelated
 * queries or the next commander's cognitive turn. Small companion system functions execute on the caller's
 * cognitive thread; they dispatch metadata/speech and are not remote game handlers.
 * <p>
 * Result is dispatch/execution status, not a game fact: a {@code handle} that returns a payload (queries,
 * data-returning system functions) yields it as-is; a side-effect {@code handle} that returns null yields a
 * {@code completed_by_executor} status. The gateway never writes to memory.
 */
public final class CompanionExecutionGateway implements ExecutionGateway {

    /** Dispatch-status result for a side-effect tool; the gateway-only "tool" field names which one ran. */
    private static final String TOOL = "tool";
    private static final String STATUS_COMPLETED = "completed_by_executor";
    private static final long EXECUTOR_SHUTDOWN_WAIT_MILLIS = 1000;

    private final Map<String, IntelAction> commandHandlers;
    private final Map<String, IntelQuery> queryHandlers;
    private final Map<String, SystemFunction> systemFunctions;
    private final Executor actionLane;
    private final Executor queryLane;
    private final Map<CompletableFuture<JsonObject>, AtomicBoolean> submittedOperations = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Production: handler maps + system-function registry, a serialized action lane, and a bounded query pool.
     */
    public CompanionExecutionGateway() {
        this(CommandHandlerFactory.getInstance().registerCommandHandlers(),
                QueryHandlerFactory.getInstance().registerQueryHandlers(),
                loadedSystemFunctions(),
                Executors.newSingleThreadExecutor(daemon("companion-action")),
                Executors.newFixedThreadPool(CompanionConfig.maxParallelQueryExecutions(),
                        daemon("companion-query")));
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
        if (closed.get()) {
            return CompletableFuture.failedFuture(
                    new RejectedExecutionException("Companion execution gateway is closed"));
        }
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
            return run(Runnable::run, systemFunction, request);
        }
        return CompletableFuture.failedFuture(
                new IllegalArgumentException("Unknown companion tool: " + toolName));
    }

    /**
     * Submits work to a lane; a future cancelled before the task starts skips execution entirely.
     */
    private CompletableFuture<JsonObject> run(Executor lane, IntelAction tool, ExecutionRequest request) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        AtomicBoolean started = new AtomicBoolean();
        submittedOperations.put(future, started);
        future.whenComplete((ignored, failure) -> submittedOperations.remove(future));
        try {
            lane.execute(() -> {
                if (future.isDone() || closed.get()) {
                    future.cancel(false); // cancelled/closed while queued: never enter the handler
                    return;
                }
                started.set(true);
                if (future.isDone()) {
                    return; // close raced with the start marker and cancelled this still-unentered operation
                }
                try {
                    future.complete(execute(tool, request));
                } catch (Throwable failure) {
                    future.completeExceptionally(failure);
                }
            });
        } catch (RuntimeException rejected) {
            future.completeExceptionally(rejected);
        }
        return future;
    }

    /**
     * Rejects new work, cancels operations that have not entered a handler, and gracefully shuts down owned
     * lanes. Started actions are allowed to return naturally and are never force-interrupted.
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        submittedOperations.forEach((future, started) -> {
            if (!started.get()) {
                future.cancel(false);
            }
        });

        long shutdownDeadline = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(EXECUTOR_SHUTDOWN_WAIT_MILLIS);
        shutdownExecutor(actionLane);
        if (queryLane != actionLane) {
            shutdownExecutor(queryLane);
        }
        awaitExecutor(actionLane, shutdownDeadline);
        if (queryLane != actionLane) {
            awaitExecutor(queryLane, shutdownDeadline);
        }
    }

    /** Single execution path: a non-null handle result is the payload; null means a side-effect dispatch. */
    private JsonObject execute(IntelAction tool, ExecutionRequest request) throws Exception {
        // Pass the commander's raw utterance as originalUserInput so handlers that match a spoken name
        // (e.g. AnalyzeStellarObjectsQuery resolving "is B 1 landable") receive it instead of "".
        JsonObject result = CompanionRuntime.callWithinGeneration(request.runtimeGenerationId(),
                () -> tool.handle(request.toolName(), request.arguments(),
                        tool.executionInput(request.commanderInput(), request.matchInput())));
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

    private static void shutdownExecutor(Executor executor) {
        if (executor instanceof ExecutorService executorService) {
            executorService.shutdown();
        }
    }

    private static void awaitExecutor(Executor executor, long shutdownDeadline) {
        if (!(executor instanceof ExecutorService executorService)) {
            return;
        }
        long remainingNanos = shutdownDeadline - System.nanoTime();
        if (remainingNanos <= 0) {
            return;
        }
        try {
            executorService.awaitTermination(remainingNanos, TimeUnit.NANOSECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
