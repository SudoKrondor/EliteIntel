package elite.intel.ai.brain.vega;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.vega.execution.ExecutionGateway;
import elite.intel.ai.brain.vega.llm.LlmGateway;
import elite.intel.ai.brain.vega.memory.MemoryGateway;
import elite.intel.ai.brain.vega.memory.MemorySnapshot;
import elite.intel.ai.brain.vega.mind.VegaState;
import elite.intel.ai.brain.vega.model.llm.LlmRequest;
import elite.intel.ai.brain.vega.model.llm.LlmResult;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;
import elite.intel.ai.brain.vega.prompt.VegaActionReducer;
import elite.intel.ai.brain.vega.speech.SpeechGateway;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Complete-graph installer for tests that exercise static VEGA system-function boundaries.
 */
public final class VegaRuntimeTestSupport {

    private VegaRuntimeTestSupport() {
    }

    public static VegaRuntimeGraph install(
            LlmGateway llmGateway,
            SpeechGateway speechGateway,
            ExecutionGateway executionGateway,
            MemoryGateway memoryGateway,
            VegaActionReducer actionReducer,
            VegaState vegaState
    ) {
        return install(llmGateway, speechGateway, executionGateway, memoryGateway,
                actionReducer, vegaState, VegaNarrator.NO_OP);
    }

    public static VegaRuntimeGraph install(
            LlmGateway llmGateway,
            SpeechGateway speechGateway,
            ExecutionGateway executionGateway,
            MemoryGateway memoryGateway,
            VegaActionReducer actionReducer,
            VegaState vegaState,
            VegaNarrator narrator
    ) {
        VegaRuntimeGraph runtimeGraph = new VegaRuntimeGraph(
                new VegaRuntimeGeneration(),
                llmGateway != null ? llmGateway : NoOpLlmGateway.INSTANCE,
                speechGateway != null ? speechGateway : request -> CompletableFuture.completedFuture(null),
                executionGateway != null ? executionGateway : request -> CompletableFuture.completedFuture(new JsonObject()),
                memoryGateway != null ? memoryGateway : NoOpMemoryGateway.INSTANCE,
                actionReducer != null ? actionReducer : (categories, input) -> List.of(),
                vegaState != null ? vegaState : new VegaState(),
                narrator != null ? narrator : VegaNarrator.NO_OP);
        VegaRuntime.installGraph(runtimeGraph);
        return runtimeGraph;
    }

    public static VegaRuntimeGraph installNarrator(VegaNarrator narrator) {
        return install(null, null, null, null, null, null, narrator);
    }

    public static void uninstall(VegaRuntimeGraph runtimeGraph) {
        if (runtimeGraph != null) {
            VegaRuntime.uninstallGraph(runtimeGraph);
            runtimeGraph.close();
        }
    }

    public static void clearInstalledGraph() {
        VegaRuntimeGraph runtimeGraph = VegaRuntime.activeGraph();
        uninstall(runtimeGraph);
    }

    private enum NoOpLlmGateway implements LlmGateway {
        INSTANCE;

        @Override
        public CompletableFuture<LlmResult> submit(LlmRequest request) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("LLM is not used by this test"));
        }

        @Override
        public CompletableFuture<String> completePlainText(LlmRequest request) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("LLM is not used by this test"));
        }
    }

    private enum NoOpMemoryGateway implements MemoryGateway {
        INSTANCE;

        @Override
        public void write(MemoryRecord record) {
        }

        @Override
        public MemorySnapshot snapshot() {
            throw unused();
        }

        @Override
        public List<MemoryRecord> readRecentHistory() {
            return List.of();
        }

        private static UnsupportedOperationException unused() {
            return new UnsupportedOperationException("Memory snapshot is not used by this test");
        }
    }
}
