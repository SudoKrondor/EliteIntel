package elite.intel.companion.prompt;

import elite.intel.ai.embed.SemanticQuery;
import elite.intel.ai.embed.AngleEmbedder;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.memory.MemorySearchResult;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemorySearchMatch;
import elite.intel.companion.model.memory.MemoryRecord;
import elite.intel.companion.model.memory.MemorySource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryFactCandidatesTest {

    @Test
    void mapsTrustedKindsToPromptSourceLabels() {
        MemoryGateway memory = new FakeMemory(List.of(
                match(MemoryKind.SAVED_TEXT, "our contact is Delgado"),
                match(MemoryKind.EVENT, "arrived in Wolf")));

        assertEquals(List.of(
                        new Fact("our contact is Delgado", "saved_text"),
                        new Fact("arrived in Wolf", "event")),
                MemoryFactCandidates.forInput(memory, "what do you remember"));
    }

    @Test
    void capsAtThreeCandidates() {
        MemoryGateway memory = new FakeMemory(List.of(
                match(MemoryKind.SAVED_TEXT, "one"),
                match(MemoryKind.EVENT, "two"),
                match(MemoryKind.SAVED_TEXT, "three"),
                match(MemoryKind.EVENT, "four")));

        assertEquals(3, MemoryFactCandidates.forInput(memory, "facts").size());
    }

    @Test
    void blankInputYieldsNoCandidates() {
        assertTrue(MemoryFactCandidates.forInput(new FakeMemory(List.of()), "   ").isEmpty());
    }

    @Test
    void forwardsPreparedSemanticQuery() {
        FakeMemory memory = new FakeMemory(List.of());
        SemanticQuery prepared = new SemanticPhraseMatcher(new AngleEmbedder(Map.of("query", 0.0)))
                .embedQueryContext("query");

        MemoryFactCandidates.forInput(memory, "query", prepared);

        assertSame(prepared, memory.semanticQuery);
    }

    private static MemorySearchMatch match(MemoryKind kind, String content) {
        return new MemorySearchMatch(kind, Instant.EPOCH, new MemoryEntry(
                kind == MemoryKind.EVENT ? MemorySource.EVENT : MemorySource.COMMANDER, content));
    }

    private static final class FakeMemory implements MemoryGateway {
        private final List<MemorySearchMatch> candidates;
        private SemanticQuery semanticQuery;

        private FakeMemory(List<MemorySearchMatch> candidates) {
            this.candidates = candidates;
        }

        @Override public List<MemorySearchMatch> recallFactCandidates(String query, int limit) { return candidates; }
        @Override public List<MemorySearchMatch> recallFactCandidates(
                String query, int limit, SemanticQuery semanticQuery) {
            this.semanticQuery = semanticQuery;
            return candidates;
        }
        @Override public void write(MemoryRecord record) { throw new UnsupportedOperationException(); }
        @Override public List<MemoryRecord> readRecentHistory() { return List.of(); }
        @Override public MemorySearchResult recallMatching(String query, int limit) {
            return MemorySearchResult.empty();
        }
        @Override public Map<MemoryKind, String> longTermSummaries() { return Map.of(); }
        @Override public void commitConsolidation(
                MemoryKind kind, List<MemoryRecord> batch, String summary
        ) { }
        @Override public List<MemoryRecord> savedTextRecords() { return List.of(); }
        @Override public MemorySnapshot snapshot() { throw new UnsupportedOperationException(); }
    }
}
