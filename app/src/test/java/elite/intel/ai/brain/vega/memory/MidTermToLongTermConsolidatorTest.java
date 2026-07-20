package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.CompanionRuntimeGeneration;
import elite.intel.ai.brain.vega.llm.LlmGateway;
import elite.intel.ai.brain.vega.memory.*;
import elite.intel.ai.brain.vega.model.llm.LlmRequest;
import elite.intel.ai.brain.vega.model.llm.LlmResult;
import elite.intel.ai.brain.vega.model.memory.MemoryKind;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;
import elite.intel.ai.brain.vega.model.speech.SpeechRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MidTermToLongTermConsolidatorTest {

    private static final Executor SYNC = Runnable::run;

    private final RecordingMemory memory = new RecordingMemory();
    private final FakeLlm llm = new FakeLlm();
    private final List<SpeechRequest> notices = new ArrayList<>();
    private final MidTermToLongTermConsolidator consolidator =
            new MidTermToLongTermConsolidator(memory, llm, request -> {
                notices.add(request);
                return CompletableFuture.completedFuture(null);
            }, SYNC);

    private static MemoryRecord event(int index) {
        return MemoryRecord.event(Instant.ofEpochSecond(index), "event-" + index);
    }

    private void feedEvents(int count) {
        for (int i = 0; i < count; i++) {
            consolidator.onPending(event(i));
        }
    }

    @Test
    void compressesAndReplacesOnlyTheMatchingKindSummaryAtThreshold() {
        memory.summaries.put(MemoryKind.DIALOGUE, "dialogue remains");
        llm.scripted = "compact event summary";

        feedEvents(CompanionMemoryPolicy.consolidationBatchSize());

        assertEquals(1, llm.calls);
        assertEquals(MemoryKind.EVENT, llm.lastKind);
        assertEquals("compact event summary", memory.summaries.get(MemoryKind.EVENT));
        assertEquals("dialogue remains", memory.summaries.get(MemoryKind.DIALOGUE));
        assertTrue(notices.isEmpty());
    }

    @Test
    void kindsBufferIndependently() {
        llm.scripted = "summary";
        for (int i = 0; i < CompanionMemoryPolicy.consolidationBatchSize() - 1; i++) {
            consolidator.onPending(event(i));
            consolidator.onPending(MemoryRecord.dialogue(
                    Instant.ofEpochSecond(i), "order-" + i, "reply-" + i));
        }

        assertEquals(0, llm.calls);
        consolidator.onPending(event(100));
        assertEquals(1, llm.calls);
        assertEquals(MemoryKind.EVENT, llm.lastKind);
    }

    @Test
    void repeatedOversizedCompressionCommitsBoundedLocalFallback() {
        memory.summaries.put(MemoryKind.EVENT, "previous summary");
        llm.scripted = "x".repeat(CompanionMemoryPolicy.summaryMaxChars() + 1);

        feedEvents(CompanionMemoryPolicy.consolidationBatchSize());

        assertEquals(3, llm.calls);
        assertTrue(memory.summaries.get(MemoryKind.EVENT).contains("previous summary"));
        assertTrue(memory.summaries.get(MemoryKind.EVENT).contains("event-0"));
        assertTrue(memory.summaries.get(MemoryKind.EVENT).length()
                <= CompanionMemoryPolicy.summaryMaxChars());
        assertEquals(CompanionMemoryPolicy.consolidationBatchSize(), memory.committed.size());
        assertEquals(1, notices.size());
    }

    @Test
    void failedBatchRetriesWithoutWaitingForAnotherRecord() {
        llm.responses.add("x".repeat(CompanionMemoryPolicy.summaryMaxChars() + 1));
        llm.responses.add("recovered summary");

        feedEvents(CompanionMemoryPolicy.consolidationBatchSize());

        assertEquals(2, llm.calls);
        assertEquals("recovered summary", memory.summaries.get(MemoryKind.EVENT));
        assertEquals(CompanionMemoryPolicy.consolidationBatchSize(), memory.committed.size());
    }

    @Test
    void closeDiscardsCompletionThatArrivesAfterShutdown() throws Exception {
        RecordingMemory delayedMemory = new RecordingMemory();
        BlockingLlm delayedLlm = new BlockingLlm();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        MidTermToLongTermConsolidator delayed = new MidTermToLongTermConsolidator(
                delayedMemory, delayedLlm,
                request -> CompletableFuture.completedFuture(null),
                new CompanionRuntimeGeneration(), worker);
        for (int i = 0; i < CompanionMemoryPolicy.consolidationBatchSize(); i++) {
            delayed.onPending(event(i));
        }
        assertTrue(delayedLlm.started.await(1, TimeUnit.SECONDS));

        delayed.close();
        delayedLlm.result.complete("late summary");
        assertTrue(worker.awaitTermination(1, TimeUnit.SECONDS));

        assertTrue(delayedMemory.summaries.isEmpty());
    }

    private static final class FakeLlm implements LlmGateway {
        private String scripted;
        private final Deque<String> responses = new ArrayDeque<>();
        private int calls;
        private MemoryKind lastKind;

        @Override
        public CompletableFuture<LlmResult> submit(LlmRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            calls++;
            String content = request.messages().get(1).content();
            lastKind = content.contains("Memory kind: EVENT") ? MemoryKind.EVENT : MemoryKind.DIALOGUE;
            return CompletableFuture.completedFuture(responses.isEmpty() ? scripted : responses.removeFirst());
        }
    }

    private static final class BlockingLlm implements LlmGateway {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CompletableFuture<String> result = new CompletableFuture<>();

        @Override
        public CompletableFuture<LlmResult> submit(LlmRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            started.countDown();
            return result;
        }
    }

    private static final class RecordingMemory implements MemoryGateway {
        private final Map<MemoryKind, String> summaries = new EnumMap<>(MemoryKind.class);
        private List<MemoryRecord> committed = List.of();

        @Override
        public void write(MemoryRecord record) {
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
            return Map.copyOf(summaries);
        }

        @Override
        public void commitConsolidation(
                MemoryKind kind, List<MemoryRecord> batch, String summary
        ) {
            summaries.put(kind, summary);
            committed = List.copyOf(batch);
        }

        @Override
        public List<MemoryRecord> savedTextRecords() {
            return List.of();
        }

        @Override
        public MemorySnapshot snapshot() {
            throw new UnsupportedOperationException();
        }
    }
}
