package elite.intel.companion.memory.facts;

import elite.intel.ai.embed.AngleEmbedder;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticQuery;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
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
    void mergesMemoryCoreFirstThenPluginFacts() {
        MemoryGateway memory = new FakeMemory(List.of(commander("mem fact")));
        List<Fact> plugins = List.of(new Fact("ship a", "ship"), new Fact("ship b", "ship"));

        assertEquals(
                List.of(new Fact("mem fact", "commander"),
                        new Fact("ship a", "ship"),
                        new Fact("ship b", "ship")),
                MergedFactCandidates.forInput(memory, ctx("q"), plugins));
    }

    @Test
    void capsEachSourceAtTwo() {
        MemoryGateway memory = new FakeMemory(List.of());
        List<Fact> plugins = List.of(
                new Fact("a", "s"), new Fact("b", "s"), new Fact("c", "s"),
                new Fact("d", "s"), new Fact("e", "s"));

        assertEquals(List.of(new Fact("a", "s"), new Fact("b", "s")),
                MergedFactCandidates.forInput(memory, ctx(""), plugins));
    }

    @Test
    void capsTotalAtSixAcrossMemoryAndPlugins() {
        MemoryGateway memory = new FakeMemory(List.of(commander("m1"), commander("m2"), commander("m3")));
        List<Fact> plugins = List.of(
                new Fact("a1", "a"), new Fact("a2", "a"),
                new Fact("b1", "b"), new Fact("b2", "b"));

        List<Fact> result = MergedFactCandidates.forInput(memory, ctx("q"), plugins);

        // memory (3) + source a (2) fills 5, leaving room for only the first fact of source b.
        assertEquals(6, result.size());
        assertEquals(new Fact("b1", "b"), result.get(5));
        assertTrue(result.stream().noneMatch(f -> f.text().equals("b2")));
    }

    @Test
    void dropsAPluginFactAlreadyPresentInMemoryCaseInsensitive() {
        MemoryGateway memory = new FakeMemory(List.of(commander("field is bedlam")));
        List<Fact> plugins = List.of(new Fact("Field Is Bedlam", "p"));

        assertEquals(List.of(new Fact("field is bedlam", "commander")),
                MergedFactCandidates.forInput(memory, ctx("q"), plugins));
    }

    @Test
    void emptyWhenNoMemoryAndNoPluginFacts() {
        assertTrue(MergedFactCandidates.forInput(new FakeMemory(List.of()), ctx(""), List.of()).isEmpty());
    }

    @Test
    void sendsPreparedSemanticQueryOnlyToTheMemoryCore() {
        FakeMemory memory = new FakeMemory(List.of(commander("mem fact")));
        SemanticQuery prepared = new SemanticPhraseMatcher(new AngleEmbedder(Map.of("q", 0.0)))
                .embedQueryContext("q");

        MergedFactCandidates.forInput(memory, ctx("q"), prepared);

        assertSame(prepared, memory.semanticQuery);
    }

    private static MemoryFactContext ctx(String query) {
        return MemoryFactContext.forQuery(query);
    }

    /** A stated commander fact (survives the tier-2 filter of the memory core). */
    private static MemoryEntry commander(String content) {
        return new MemoryEntry(Instant.now(), ConversationTopic.SOCIAL, MemorySource.COMMANDER,
                content, MemoryImportance.HIGH, null, content);
    }

    /** Minimal gateway returning a fixed candidate list; the other operations are unused by these tests. */
    private static final class FakeMemory implements MemoryGateway {
        private final List<MemoryEntry> candidates;
        private SemanticQuery semanticQuery;

        private FakeMemory(List<MemoryEntry> candidates) {
            this.candidates = candidates;
        }

        @Override public MemorySnapshot snapshot() { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> recallCandidates(String query, int limit) { return candidates; }
        @Override public List<MemoryEntry> recallCandidates(String query, int limit, SemanticQuery semanticQuery) {
            this.semanticQuery = semanticQuery;
            return candidates;
        }
        @Override public void write(MemoryEntry entry) { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> readShortTermTimeline() { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) { throw new UnsupportedOperationException(); }
        @Override public List<String> recallMatching(String query, int limit) { throw new UnsupportedOperationException(); }
        @Override public String longTermSummary() { throw new UnsupportedOperationException(); }
        @Override public void replaceLongTermSummary(String summary) { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> longTermPinnedFacts() { throw new UnsupportedOperationException(); }
        @Override public void addLongTermPinned(MemoryEntry fact) { throw new UnsupportedOperationException(); }
    }
}
