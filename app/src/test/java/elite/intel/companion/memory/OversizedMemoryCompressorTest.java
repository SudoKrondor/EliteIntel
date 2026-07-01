package elite.intel.companion.memory;

import elite.intel.companion.CompanionConfig;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemorySource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the over-long-entry compressor with synchronous fakes: a usable gist is re-written under the
 * entry's original provenance, and an empty, null, or still-oversized compression drops the entry. Runs off
 * the write path on a dedicated executor (here injected as synchronous), so it never blocks a write or a lane.
 */
class OversizedMemoryCompressorTest {

    private static final Executor SYNC = Runnable::run;

    private final RecordingMemory memory = new RecordingMemory();
    private final FakeLlm llm = new FakeLlm();
    private final OversizedMemoryCompressor compressor = new OversizedMemoryCompressor(memory, llm, SYNC);

    private static MemoryEntry longEntry(MemoryImportance importance) {
        return new MemoryEntry(Instant.now(), ConversationTopic.NAVIGATION, MemorySource.COMPANION,
                "a very long station-services ramble that would bloat the prompt", importance);
    }

    @Test
    void compressesAndReWritesTheGistUnderOriginalProvenance() {
        llm.scripted = "docked at jameson memorial";

        compressor.onOversized(longEntry(MemoryImportance.HIGH));

        assertEquals(1, llm.calls);
        assertEquals(1, memory.writes.size());
        MemoryEntry written = memory.writes.get(0);
        assertEquals("docked at jameson memorial", written.content());
        assertEquals(MemorySource.COMPANION, written.source(), "original source is preserved");
        assertEquals(ConversationTopic.NAVIGATION, written.topic(), "original topic is preserved");
        assertEquals(MemoryImportance.HIGH, written.importance(), "original importance is preserved");
    }

    @Test
    void dropsEmptyOrOversizedOutputWithoutWriting() {
        llm.scripted = null; // the model produced nothing
        compressor.onOversized(longEntry(MemoryImportance.NORMAL));
        assertTrue(memory.writes.isEmpty(), "nothing is written when compression yields no gist");

        llm.scripted = "z".repeat(CompanionConfig.memoryEntryMaxChars() + 1); // still over the cap
        compressor.onOversized(longEntry(MemoryImportance.NORMAL));
        assertTrue(memory.writes.isEmpty(), "nothing is written when the gist is still over the cap");
    }

    /** LlmGateway fake: scripted compression result; submit unused. */
    private static final class FakeLlm implements LlmGateway {
        volatile String scripted;
        volatile int calls;

        @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            calls++;
            return CompletableFuture.completedFuture(scripted);
        }
    }

    /** MemoryGateway fake recording re-writes; the rest is unused by the compressor. */
    private static final class RecordingMemory implements MemoryGateway {
        final List<MemoryEntry> writes = new ArrayList<>();

        @Override public void write(MemoryEntry entry) { writes.add(entry); }
        @Override public List<MemoryEntry> readShortTermTimeline() { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) { throw new UnsupportedOperationException(); }
        @Override public List<String> recallMatching(String query, int limit) { throw new UnsupportedOperationException(); }
        @Override public MemoryAvailabilitySnapshot indexes() { throw new UnsupportedOperationException(); }
        @Override public String longTermSummary() { throw new UnsupportedOperationException(); }
        @Override public void replaceLongTermSummary(String summary) { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> longTermPinnedFacts() { throw new UnsupportedOperationException(); }
        @Override public void addLongTermPinned(MemoryEntry fact) { throw new UnsupportedOperationException(); }
    }
}
