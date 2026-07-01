package elite.intel.companion.memory;

import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.speech.SpeechRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the consolidator with synchronous fakes: it buffers until the threshold, then compresses and
 * atomically replaces the summary; a failing/oversized compression keeps the existing summary and notifies
 * the commander; below the threshold nothing happens.
 */
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

    private static MemoryEntry entry(String content) {
        return new MemoryEntry(Instant.now(), ConversationTopic.MINING, MemorySource.EVENT, content);
    }

    private static MemoryEntry entry(String content, MemoryImportance importance) {
        return new MemoryEntry(Instant.now(), ConversationTopic.MINING, MemorySource.EVENT, content, importance);
    }

    private void feed(int count) {
        for (int i = 0; i < count; i++) {
            consolidator.onEvicted(entry("rock-" + i));
        }
    }

    @Test
    void compressesAndReplacesSummaryAtThreshold() {
        llm.scripted = "compact mining summary";

        feed(CompanionMemoryLimits.CONSOLIDATION_BUFFER_THRESHOLD);

        assertEquals(1, llm.calls);
        assertEquals("compact mining summary", memory.summary);
        assertTrue(notices.isEmpty());
    }

    @Test
    void maxIsPinnedVerbatimImmediatelyAndLowIsDroppedWithoutBuffering() {
        llm.scripted = "summary";
        consolidator.onEvicted(entry("abort word granite", MemoryImportance.MAX));
        consolidator.onEvicted(entry("idle chatter", MemoryImportance.LOW));

        // MAX is pinned right away (verbatim); LOW is dropped; neither buffers, so no compression yet.
        assertEquals(List.of("abort word granite"), memory.pinned.stream().map(MemoryEntry::content).toList());
        assertEquals(0, llm.calls);

        // HIGH/NORMAL still buffer and summarize at the threshold - MAX/LOW did not count toward it.
        for (int i = 0; i < CompanionMemoryLimits.CONSOLIDATION_BUFFER_THRESHOLD; i++) {
            consolidator.onEvicted(entry("rock-" + i, MemoryImportance.NORMAL));
        }
        assertEquals(1, llm.calls);
        assertEquals("summary", memory.summary);
        assertEquals(1, memory.pinned.size());
    }

    @Test
    void belowThresholdDoesNothing() {
        feed(CompanionMemoryLimits.CONSOLIDATION_BUFFER_THRESHOLD - 1);

        assertEquals(0, llm.calls);
        assertEquals("", memory.summary);
    }

    @Test
    void failedCompressionKeepsSummaryAndNotifies() {
        memory.summary = "previous summary";
        llm.scripted = null; // compression failure

        feed(CompanionMemoryLimits.CONSOLIDATION_BUFFER_THRESHOLD);

        assertEquals("previous summary", memory.summary); // unchanged
        assertEquals(1, notices.size());
    }

    @Test
    void oversizedOutputIsTreatedAsFailure() {
        memory.summary = "previous summary";
        llm.scripted = "x".repeat(CompanionMemoryLimits.SUMMARY_MAX_CHARS + 1);

        feed(CompanionMemoryLimits.CONSOLIDATION_BUFFER_THRESHOLD);

        assertEquals("previous summary", memory.summary);
        assertEquals(1, notices.size());
    }

    @Test
    void bufferClearsSoFollowingEntriesAccumulateAgain() {
        llm.scripted = "s";
        feed(CompanionMemoryLimits.CONSOLIDATION_BUFFER_THRESHOLD); // one pass
        feed(CompanionMemoryLimits.CONSOLIDATION_BUFFER_THRESHOLD - 1); // not enough for a second

        assertEquals(1, llm.calls);
    }

    /** LlmGateway fake: scripted compression result; submit unused. */
    private static final class FakeLlm implements LlmGateway {
        volatile String scripted;
        volatile int calls;

        @Override
        public CompletableFuture<LlmResult> submit(LlmRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            calls++;
            return CompletableFuture.completedFuture(scripted);
        }
    }

    /** MemoryGateway fake recording the long-term summary and the pinned MAX facts. */
    private static final class RecordingMemory implements MemoryGateway {
        String summary = "";
        final List<MemoryEntry> pinned = new ArrayList<>();

        @Override public void write(MemoryEntry entry) { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> readShortTermTimeline() { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) { throw new UnsupportedOperationException(); }
        @Override public List<String> recallMatching(String query, int limit) { throw new UnsupportedOperationException(); }
        @Override public MemoryAvailabilitySnapshot indexes() { throw new UnsupportedOperationException(); }
        @Override public String longTermSummary() { return summary; }
        @Override public void replaceLongTermSummary(String summary) { this.summary = summary; }
        @Override public List<MemoryEntry> longTermPinnedFacts() { return List.copyOf(pinned); }
        @Override public void addLongTermPinned(MemoryEntry fact) { pinned.add(fact); }
    }
}
