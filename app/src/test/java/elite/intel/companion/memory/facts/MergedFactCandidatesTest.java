package elite.intel.companion.memory.facts;

import elite.intel.ai.embed.AngleEmbedder;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticQuery;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.memory.MemorySearchResult;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemorySearchMatch;
import elite.intel.companion.model.memory.MemoryRecord;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.prompt.Fact;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergedFactCandidatesTest {

    @Test
    void mergesMemoryFirstThenPluginFacts() {
        MemoryGateway memory = new FakeMemory(List.of(savedText("mem fact")));
        List<Fact> plugins = List.of(new Fact("ship a", "ship"), new Fact("ship b", "ship"));

        assertEquals(List.of(
                        new Fact("mem fact", "saved_text"),
                        new Fact("ship a", "ship"),
                        new Fact("ship b", "ship")),
                MergedFactCandidates.forInput(memory, ctx("q"), plugins));
    }

    @Test
    void capsEachPluginSourceAtTwo() {
        List<Fact> plugins = List.of(
                new Fact("a", "s"), new Fact("b", "s"), new Fact("c", "s"));

        assertEquals(List.of(new Fact("a", "s"), new Fact("b", "s")),
                MergedFactCandidates.forInput(new FakeMemory(List.of()), ctx(""), plugins));
    }

    @Test
    void capsTotalAtSixAndDeduplicatesCaseInsensitively() {
        MemoryGateway memory = new FakeMemory(List.of(
                savedText("m1"), savedText("m2"), savedText("Field is Bedlam")));
        List<Fact> plugins = List.of(
                new Fact("field IS bedlam", "a"),
                new Fact("a1", "a"), new Fact("a2", "a"),
                new Fact("b1", "b"), new Fact("b2", "b"));

        List<Fact> result = MergedFactCandidates.forInput(memory, ctx("q"), plugins);

        assertEquals(6, result.size());
        assertTrue(result.stream().noneMatch(fact -> fact.text().equals("field IS bedlam")));
        assertEquals(new Fact("b1", "b"), result.get(5));
    }

    @Test
    void forwardsPreparedSemanticQueryOnlyToMemory() {
        FakeMemory memory = new FakeMemory(List.of(savedText("mem fact")));
        SemanticQuery prepared = new SemanticPhraseMatcher(new AngleEmbedder(Map.of("q", 0.0)))
                .embedQueryContext("q");

        MergedFactCandidates.forInput(memory, ctx("q"), prepared);

        assertSame(prepared, memory.semanticQuery);
    }

    private static MemoryFactContext ctx(String query) {
        return MemoryFactContext.forQuery(query);
    }

    private static MemorySearchMatch savedText(String content) {
        return new MemorySearchMatch(MemoryKind.SAVED_TEXT, Instant.EPOCH,
                new MemoryEntry(MemorySource.COMMANDER, content));
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
