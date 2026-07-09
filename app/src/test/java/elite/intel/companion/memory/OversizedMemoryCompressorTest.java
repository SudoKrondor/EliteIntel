package elite.intel.companion.memory;

import elite.intel.companion.CompanionConfig;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.memory.ToolLink;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void preservesToolLinkSoACompressedResultStaysPairedWithItsCall() {
        llm.scripted = "res sites and a conflict zone";
        MemoryEntry longResult = new MemoryEntry(Instant.now(), ConversationTopic.EXPLORATION,
                MemorySource.TOOL_RESULT, "a very long system briefing that would bloat the prompt timeline",
                MemoryImportance.NORMAL, null, null, ToolLink.result("call-7"));

        compressor.onOversized(longResult);

        assertEquals(1, memory.writes.size());
        MemoryEntry gist = memory.writes.get(0);
        assertEquals("res sites and a conflict zone", gist.content());
        assertNotNull(gist.toolLink(), "the gist keeps the call linkage so it replays as the call's RESULT");
        assertTrue(gist.toolLink().isResult());
        assertEquals("call-7", gist.toolLink().toolCallId());
    }

    @Test
    void keepsTheTruncatedGistWhenTheModelsSummaryStaysOverTheCap() {
        // A small local model may echo an over-cap "gist". Its truncated form - the purpose-built summary, not the
        // raw head of the original - is kept, still paired with the call.
        llm.scripted = "z".repeat(CompanionConfig.memoryEntryMaxChars() + 1);
        MemoryEntry longResult = new MemoryEntry(Instant.now(), ConversationTopic.EXPLORATION,
                MemorySource.TOOL_RESULT, "a".repeat(CompanionConfig.memoryEntryMaxChars() + 500),
                MemoryImportance.NORMAL, null, null, ToolLink.result("call-9"));

        compressor.onOversized(longResult);

        assertEquals(1, memory.writes.size(), "the linked result is kept, not dropped");
        MemoryEntry stored = memory.writes.get(0);
        assertTrue(stored.content().length() <= CompanionConfig.memoryEntryMaxChars(), "fits the cap");
        assertTrue(stored.content().startsWith("z"),
                "the model's summary attempt is kept (truncated), not the raw head of the original");
        assertNotNull(stored.toolLink(), "stays paired with its call");
        assertEquals("call-9", stored.toolLink().toolCallId());
    }

    @Test
    void fallsBackToTheOriginalHeadOnlyWhenCompressionReturnsNothing() {
        // The model produced nothing usable: keep a truncated copy of the original so the pair still survives.
        llm.scripted = null;
        MemoryEntry longResult = new MemoryEntry(Instant.now(), ConversationTopic.EXPLORATION,
                MemorySource.TOOL_RESULT, "a".repeat(CompanionConfig.memoryEntryMaxChars() + 500),
                MemoryImportance.NORMAL, null, null, ToolLink.result("call-10"));

        compressor.onOversized(longResult);

        assertEquals(1, memory.writes.size());
        MemoryEntry stored = memory.writes.get(0);
        assertTrue(stored.content().startsWith("a"), "with no gist, the truncated original head is kept");
        assertNotNull(stored.toolLink(), "stays paired with its call");
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
        @Override public MemorySnapshot snapshot() { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> readShortTermTimeline() { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) { throw new UnsupportedOperationException(); }
        @Override public List<String> recallMatching(String query, int limit) { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> recallCandidates(String query, int limit) { throw new UnsupportedOperationException(); }
        @Override public String longTermSummary() { throw new UnsupportedOperationException(); }
        @Override public void replaceLongTermSummary(String summary) { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> longTermPinnedFacts() { throw new UnsupportedOperationException(); }
        @Override public void addLongTermPinned(MemoryEntry fact) { throw new UnsupportedOperationException(); }
    }
}
