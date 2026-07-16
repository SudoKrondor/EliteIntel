package elite.intel.companion;

import com.google.gson.JsonObject;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.memory.MemorySearchResult;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemorySearchMatch;
import elite.intel.companion.model.memory.MemoryRecord;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.speech.SpeechGateway;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Complete-graph installer for tests that exercise static companion system-function boundaries. */
public final class CompanionRuntimeTestSupport {

    private CompanionRuntimeTestSupport() {
    }

    public static CompanionRuntimeGraph install(
            LlmGateway llmGateway,
            SpeechGateway speechGateway,
            ExecutionGateway executionGateway,
            MemoryGateway memoryGateway,
            CompanionActionReducer actionReducer,
            CompanionState companionState
    ) {
        return install(llmGateway, speechGateway, executionGateway, memoryGateway,
                actionReducer, companionState, CompanionNarrator.NO_OP);
    }

    public static CompanionRuntimeGraph install(
            LlmGateway llmGateway,
            SpeechGateway speechGateway,
            ExecutionGateway executionGateway,
            MemoryGateway memoryGateway,
            CompanionActionReducer actionReducer,
            CompanionState companionState,
            CompanionNarrator narrator
    ) {
        CompanionRuntimeGraph runtimeGraph = new CompanionRuntimeGraph(
                new CompanionRuntimeGeneration(),
                llmGateway != null ? llmGateway : NoOpLlmGateway.INSTANCE,
                speechGateway != null ? speechGateway : request -> CompletableFuture.completedFuture(null),
                executionGateway != null ? executionGateway : request -> CompletableFuture.completedFuture(new JsonObject()),
                memoryGateway != null ? memoryGateway : NoOpMemoryGateway.INSTANCE,
                actionReducer != null ? actionReducer : (categories, input) -> List.of(),
                companionState != null ? companionState : new CompanionState(),
                narrator != null ? narrator : CompanionNarrator.NO_OP);
        CompanionRuntime.installGraph(runtimeGraph);
        return runtimeGraph;
    }

    public static CompanionRuntimeGraph installNarrator(CompanionNarrator narrator) {
        return install(null, null, null, null, null, null, narrator);
    }

    public static void uninstall(CompanionRuntimeGraph runtimeGraph) {
        if (runtimeGraph != null) {
            CompanionRuntime.uninstallGraph(runtimeGraph);
            runtimeGraph.close();
        }
    }

    public static void clearInstalledGraph() {
        CompanionRuntimeGraph runtimeGraph = CompanionRuntime.activeGraph();
        uninstall(runtimeGraph);
    }

    private enum NoOpLlmGateway implements LlmGateway {
        INSTANCE;

        @Override
        public CompletableFuture<LlmResult> submit(LlmRequest request) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("LLM is not used by this test"));
        }

        @Override
        public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("LLM is not used by this test"));
        }
    }

    private enum NoOpMemoryGateway implements MemoryGateway {
        INSTANCE;

        @Override public void write(MemoryRecord record) { }
        @Override public MemorySnapshot snapshot() { throw unused(); }
        @Override public List<MemoryRecord> readRecentHistory() { return List.of(); }
        @Override public MemorySearchResult recallMatching(String query, int limit) {
            return MemorySearchResult.empty();
        }
        @Override public List<MemorySearchMatch> recallFactCandidates(String query, int limit) { return List.of(); }
        @Override public Map<MemoryKind, String> longTermSummaries() { return Map.of(); }
        @Override public void commitConsolidation(
                MemoryKind kind, List<MemoryRecord> batch, String summary
        ) { }
        @Override public List<MemoryRecord> savedTextRecords() { return List.of(); }

        private static UnsupportedOperationException unused() {
            return new UnsupportedOperationException("Memory snapshot is not used by this test");
        }
    }
}
