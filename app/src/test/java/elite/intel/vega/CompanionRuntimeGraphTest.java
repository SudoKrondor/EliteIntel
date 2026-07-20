package elite.intel.vega;

import com.google.gson.JsonObject;
import elite.intel.vega.execution.ExecutionGateway;
import elite.intel.vega.execution.GenerationBoundExecutionGateway;
import elite.intel.vega.llm.LlmGateway;
import elite.intel.vega.memory.MemoryGateway;
import elite.intel.vega.memory.MemorySearchResult;
import elite.intel.vega.memory.MemorySnapshot;
import elite.intel.vega.mind.CompanionState;
import elite.intel.vega.mind.DispatcherCompanionNarrator;
import elite.intel.vega.mind.ThoughtDispatcher;
import elite.intel.vega.model.Urgency;
import elite.intel.vega.model.execution.ExecutionRequest;
import elite.intel.vega.model.llm.LlmRequest;
import elite.intel.vega.model.llm.LlmResult;
import elite.intel.vega.model.memory.MemoryKind;
import elite.intel.vega.model.memory.MemoryRecord;
import elite.intel.vega.model.speech.SpeechRequest;
import elite.intel.vega.speech.GenerationBoundSpeechGateway;
import elite.intel.vega.speech.SpeechGateway;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionRuntimeGraphTest {

    @Test
    void closeFencesGenerationCancelsOwnedWorkAndClosesDependenciesOnce() {
        List<String> closeOrder = new ArrayList<>();
        CompanionRuntimeGeneration runtimeGeneration = new CompanionRuntimeGeneration();
        RecordingLlmGateway llmGateway = new RecordingLlmGateway(closeOrder);
        RecordingExecutionGateway rawExecutionGateway = new RecordingExecutionGateway(closeOrder);
        GenerationBoundExecutionGateway executionGateway = new GenerationBoundExecutionGateway(
                rawExecutionGateway, runtimeGeneration);
        RecordingSpeechGateway rawSpeechGateway = new RecordingSpeechGateway(closeOrder);
        GenerationBoundSpeechGateway speechGateway = new GenerationBoundSpeechGateway(
                rawSpeechGateway, runtimeGeneration);
        CompanionRuntimeGraph runtimeGraph = graph(
                runtimeGeneration, llmGateway, speechGateway, executionGateway, CompanionNarrator.NO_OP);

        CompletableFuture<Void> speechCompletion = speechGateway.submit(
                new SpeechRequest("speech-1", "Course plotted.", Urgency.NORMAL));
        CompletableFuture<JsonObject> executionResult = executionGateway.submit(
                new ExecutionRequest("execution-1", "plot_route", new JsonObject()));
        runtimeGraph.close();
        runtimeGraph.close();

        assertFalse(runtimeGeneration.isActive());
        assertTrue(speechCompletion.isCancelled());
        assertTrue(executionResult.isCancelled());
        assertEquals(List.of("speech", "execution", "llm"), closeOrder);
    }

    @Test
    void narratorCapturedBeforeShutdownCannotSpeakAfterGenerationExpires() {
        CompanionRuntimeGeneration runtimeGeneration = new CompanionRuntimeGeneration();
        AtomicInteger submissions = new AtomicInteger();
        SpeechGateway speechGateway = request -> {
            submissions.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        };
        CompanionNarrator narrator = new DispatcherCompanionNarrator(
                new ThoughtDispatcher(null), speechGateway, runtimeGeneration);
        CompanionRuntimeGraph runtimeGraph = graph(
                runtimeGeneration,
                new RecordingLlmGateway(new ArrayList<>()),
                speechGateway,
                request -> CompletableFuture.completedFuture(new JsonObject()),
                narrator);

        runtimeGraph.close();
        narrator.filler("This belongs to the old runtime.", false);

        assertEquals(0, submissions.get());
    }

    @Test
    void generationIdentifiersIncreaseMonotonically() {
        CompanionRuntimeGeneration first = new CompanionRuntimeGeneration();
        CompanionRuntimeGeneration second = new CompanionRuntimeGeneration();

        assertTrue(second.generationId() > first.generationId());
    }

    private static CompanionRuntimeGraph graph(
            CompanionRuntimeGeneration runtimeGeneration,
            LlmGateway llmGateway,
            SpeechGateway speechGateway,
            ExecutionGateway executionGateway,
            CompanionNarrator narrator
    ) {
        return new CompanionRuntimeGraph(
                runtimeGeneration,
                llmGateway,
                speechGateway,
                executionGateway,
                NoOpMemoryGateway.INSTANCE,
                (categories, input) -> List.of(),
                new CompanionState(),
                narrator);
    }

    private static final class RecordingLlmGateway implements LlmGateway {
        private final List<String> closeOrder;

        private RecordingLlmGateway(List<String> closeOrder) {
            this.closeOrder = closeOrder;
        }

        @Override
        public CompletableFuture<LlmResult> submit(LlmRequest request) {
            return new CompletableFuture<>();
        }

        @Override
        public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            return new CompletableFuture<>();
        }

        @Override
        public void close() {
            closeOrder.add("llm");
        }
    }

    private static final class RecordingExecutionGateway implements ExecutionGateway {
        private final List<String> closeOrder;
        private final CompletableFuture<JsonObject> result = new CompletableFuture<>();

        private RecordingExecutionGateway(List<String> closeOrder) {
            this.closeOrder = closeOrder;
        }

        @Override
        public CompletableFuture<JsonObject> submit(ExecutionRequest request) {
            return result;
        }

        @Override
        public void close() {
            closeOrder.add("execution");
        }
    }

    private static final class RecordingSpeechGateway implements SpeechGateway {
        private final List<String> closeOrder;
        private final CompletableFuture<Void> completion = new CompletableFuture<>();

        private RecordingSpeechGateway(List<String> closeOrder) {
            this.closeOrder = closeOrder;
            completion.whenComplete((ignored, failure) -> closeOrder.add("speech"));
        }

        @Override
        public CompletableFuture<Void> submit(SpeechRequest request) {
            return completion;
        }
    }

    private enum NoOpMemoryGateway implements MemoryGateway {
        INSTANCE;

        @Override
        public void write(MemoryRecord record) {
        }

        @Override
        public MemorySnapshot snapshot() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<MemoryRecord> readRecentHistory() {
            return List.of();
        }

        @Override
        public MemorySearchResult recallMatching(String query, int limit) {
            return MemorySearchResult.empty();
        }

        @Override
        public Map<MemoryKind, String> longTermSummaries() {
            return Map.of();
        }

        @Override
        public void commitConsolidation(
                MemoryKind kind, List<MemoryRecord> batch, String summary
        ) {
        }

        @Override
        public List<MemoryRecord> savedTextRecords() {
            return List.of();
        }
    }
}
