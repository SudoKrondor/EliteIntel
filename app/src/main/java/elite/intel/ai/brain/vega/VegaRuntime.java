package elite.intel.ai.brain.vega;

import elite.intel.ai.brain.vega.execution.ExecutionGateway;
import elite.intel.ai.brain.vega.llm.LlmGateway;
import elite.intel.ai.brain.vega.memory.MemoryGateway;
import elite.intel.ai.brain.vega.mind.VegaState;
import elite.intel.ai.brain.vega.prompt.VegaActionReducer;
import elite.intel.ai.brain.vega.speech.SpeechGateway;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic access point to the currently running VEGA subsystem. Every service, the narrator, and shared
 * state are published as one immutable {@link VegaRuntimeGraph}; readers therefore observe one complete
 * generation or no runtime, never a mixture assembled through independently assigned fields.
 * <p>
 * The lifecycle owner installs a fully started graph with {@link #installGraph(VegaRuntimeGraph)} and
 * removes that exact instance with {@link #uninstallGraph(VegaRuntimeGraph)} before closing it. Identity-
 * based removal prevents a delayed stop from clearing a newer restart generation.
 */
public final class VegaRuntime {

    private static final AtomicReference<VegaRuntimeGraph> ACTIVE_GRAPH = new AtomicReference<>();
    private static final ThreadLocal<Long> EXECUTION_GENERATION = new ThreadLocal<>();

    private VegaRuntime() {
    }

    /**
     * Atomically publishes one fully assembled and started runtime graph.
     */
    public static void installGraph(VegaRuntimeGraph runtimeGraph) {
        Objects.requireNonNull(runtimeGraph, "runtimeGraph");
        if (!runtimeGraph.isStarted()) {
            throw new IllegalStateException("Cannot install a VEGA runtime graph before startup completes");
        }
        if (runtimeGraph.isClosed() || !runtimeGraph.runtimeGeneration().isActive()) {
            throw new IllegalStateException("Cannot install a closed VEGA runtime graph");
        }
        if (!ACTIVE_GRAPH.compareAndSet(null, runtimeGraph)) {
            throw new IllegalStateException("A VEGA runtime graph is already installed");
        }
    }

    /**
     * Removes {@code expectedGraph} only when it is still current. A stale generation cannot clear a newer one.
     *
     * @return {@code true} when the expected graph was installed and has been removed
     */
    public static boolean uninstallGraph(VegaRuntimeGraph expectedGraph) {
        return expectedGraph != null && ACTIVE_GRAPH.compareAndSet(expectedGraph, null);
    }

    public static LlmGateway llm() {
        return requireActiveGraph().llmGateway();
    }

    public static SpeechGateway speech() {
        return requireActiveGraph().speechGateway();
    }

    public static ExecutionGateway execution() {
        return requireActiveGraph().executionGateway();
    }

    public static MemoryGateway memory() {
        return requireActiveGraph().memoryGateway();
    }

    public static VegaActionReducer reducer() {
        return requireActiveGraph().actionReducer();
    }

    public static VegaState state() {
        return requireActiveGraph().vegaState();
    }

    /**
     * The single door gameplay subscribers use to voice reactions. Returns {@link VegaNarrator#NO_OP} while
     * the subsystem is stopped, so subscribers may call it unconditionally.
     */
    public static VegaNarrator narrator() {
        VegaRuntimeGraph runtimeGraph = ACTIVE_GRAPH.get();
        return runtimeGraph != null
                && runtimeGraph.runtimeGeneration().isActive()
                && matchesExecutionGeneration(runtimeGraph)
                ? runtimeGraph.narrator()
                : VegaNarrator.NO_OP;
    }

    /**
     * Binds synchronous handler code to the runtime generation that submitted it. If an old action finishes after
     * restart, static runtime access from that handler cannot accidentally target the newly installed graph.
     */
    public static <T> T callWithinGeneration(long generationId, Callable<T> operation) throws Exception {
        Objects.requireNonNull(operation, "operation");
        if (generationId <= 0L) {
            return operation.call();
        }
        Long previousGenerationId = EXECUTION_GENERATION.get();
        EXECUTION_GENERATION.set(generationId);
        try {
            return operation.call();
        } finally {
            if (previousGenerationId == null) {
                EXECUTION_GENERATION.remove();
            } else {
                EXECUTION_GENERATION.set(previousGenerationId);
            }
        }
    }

    /**
     * Returns the active graph as one consistent snapshot, primarily for lifecycle diagnostics and tests.
     */
    static VegaRuntimeGraph activeGraph() {
        return ACTIVE_GRAPH.get();
    }

    private static VegaRuntimeGraph requireActiveGraph() {
        VegaRuntimeGraph runtimeGraph = ACTIVE_GRAPH.get();
        if (runtimeGraph == null
                || !runtimeGraph.runtimeGeneration().isActive()
                || !matchesExecutionGeneration(runtimeGraph)) {
            throw new IllegalStateException("VEGA runtime is not installed (subsystem not running)");
        }
        return runtimeGraph;
    }

    private static boolean matchesExecutionGeneration(VegaRuntimeGraph runtimeGraph) {
        Long expectedGenerationId = EXECUTION_GENERATION.get();
        return expectedGenerationId == null
                || expectedGenerationId == runtimeGraph.runtimeGeneration().generationId();
    }
}
