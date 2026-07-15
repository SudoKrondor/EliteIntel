package elite.intel.companion;

import com.google.gson.JsonObject;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.execution.GenerationBoundExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.mind.DispatcherCompanionNarrator;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.speech.GenerationBoundSpeechGateway;
import elite.intel.companion.speech.SpeechGateway;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
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

        @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
            return new CompletableFuture<>();
        }
        @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            return new CompletableFuture<>();
        }
        @Override public void close() { closeOrder.add("llm"); }
    }

    private static final class RecordingExecutionGateway implements ExecutionGateway {
        private final List<String> closeOrder;
        private final CompletableFuture<JsonObject> result = new CompletableFuture<>();

        private RecordingExecutionGateway(List<String> closeOrder) {
            this.closeOrder = closeOrder;
        }

        @Override public CompletableFuture<JsonObject> submit(ExecutionRequest request) { return result; }
        @Override public void close() { closeOrder.add("execution"); }
    }

    private static final class RecordingSpeechGateway implements SpeechGateway {
        private final List<String> closeOrder;
        private final CompletableFuture<Void> completion = new CompletableFuture<>();

        private RecordingSpeechGateway(List<String> closeOrder) {
            this.closeOrder = closeOrder;
            completion.whenComplete((ignored, failure) -> closeOrder.add("speech"));
        }

        @Override public CompletableFuture<Void> submit(SpeechRequest request) { return completion; }
    }

    private enum NoOpMemoryGateway implements MemoryGateway {
        INSTANCE;

        @Override public void write(MemoryEntry entry) { }
        @Override public void writeBatch(List<MemoryEntry> entries) { }
        @Override public MemorySnapshot snapshot() { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> readShortTermTimeline() { return List.of(); }
        @Override public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) { return List.of(); }
        @Override public List<String> recallMatching(String query, int limit) { return List.of(); }
        @Override public List<MemoryEntry> recallCandidates(String query, int limit) { return List.of(); }
        @Override public String longTermSummary() { return ""; }
        @Override public void replaceLongTermSummary(String summary) { }
        @Override public List<MemoryEntry> longTermPinnedFacts() { return List.of(); }
        @Override public void addLongTermPinned(MemoryEntry fact) { }
    }
}
