package elite.intel.companion.memory.facts;

import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.prompt.Fact;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergedFactCandidatesTest {

    @Test
    void mergesMemoryCoreFirstThenPluginFactsTaggedBySource() {
        MemoryGateway memory = new FakeMemory(List.of(commander("mem fact")));
        List<MemoryFactSource> sources = List.of(source("ship", "ship a", "ship b"));

        assertEquals(
                List.of(new Fact("mem fact", "commander"),
                        new Fact("ship a", "ship"),
                        new Fact("ship b", "ship")),
                MergedFactCandidates.forInput(memory, ctx("q"), sources));
    }

    @Test
    void capsEachSourceAtTwo() {
        MemoryGateway memory = new FakeMemory(List.of());
        List<MemoryFactSource> sources = List.of(source("s", "a", "b", "c", "d", "e"));

        assertEquals(
                List.of(new Fact("a", "s"), new Fact("b", "s")),
                MergedFactCandidates.forInput(memory, ctx(""), sources));
    }

    @Test
    void capsTotalAtSixAcrossMemoryAndPlugins() {
        MemoryGateway memory = new FakeMemory(List.of(commander("m1"), commander("m2"), commander("m3")));
        List<MemoryFactSource> sources = List.of(source("a", "a1", "a2"), source("b", "b1", "b2"));

        List<Fact> result = MergedFactCandidates.forInput(memory, ctx("q"), sources);

        // memory (3) + source a (2) fills 5, leaving room for only the first fact of source b.
        assertEquals(6, result.size());
        assertEquals(new Fact("b1", "b"), result.get(5));
        assertTrue(result.stream().noneMatch(f -> f.text().equals("b2")));
    }

    @Test
    void dropsAPluginFactAlreadyPresentInMemoryCaseInsensitive() {
        MemoryGateway memory = new FakeMemory(List.of(commander("field is bedlam")));
        List<MemoryFactSource> sources = List.of(source("p", "Field Is Bedlam"));

        assertEquals(
                List.of(new Fact("field is bedlam", "commander")),
                MergedFactCandidates.forInput(memory, ctx("q"), sources));
    }

    @Test
    void skipsAFailingSourceInsteadOfFailingTheTurn() {
        MemoryGateway memory = new FakeMemory(List.of(commander("mem fact")));
        MemoryFactSource boom = new MemoryFactSource() {
            @Override public String id() { return "boom"; }
            @Override public List<String> factsFor(MemoryFactContext context) { throw new IllegalStateException("boom"); }
        };
        List<MemoryFactSource> sources = List.of(boom, source("ok", "ok fact"));

        // The throwing source is skipped; memory and the healthy source still contribute.
        assertEquals(
                List.of(new Fact("mem fact", "commander"), new Fact("ok fact", "ok")),
                MergedFactCandidates.forInput(memory, ctx("q"), sources));
    }

    @Test
    void treatsANullReturningSourceAsNoFacts() {
        MemoryFactSource nully = new MemoryFactSource() {
            @Override public String id() { return "nully"; }
            @Override public List<String> factsFor(MemoryFactContext context) { return null; }
        };

        assertTrue(MergedFactCandidates.forInput(new FakeMemory(List.of()), ctx(""), List.of(nully)).isEmpty());
    }

    @Test
    void emptyWhenNoMemoryAndNoSources() {
        assertTrue(MergedFactCandidates.forInput(new FakeMemory(List.of()), ctx(""), List.of()).isEmpty());
    }

    private static MemoryFactContext ctx(String query) {
        return new MemoryFactContext(query, ThoughtSource.COMMANDER, Urgency.NORMAL);
    }

    /** A stated commander fact (survives the tier-2 filter of the memory core). */
    private static MemoryEntry commander(String content) {
        return new MemoryEntry(Instant.now(), ConversationTopic.SOCIAL, MemorySource.COMMANDER,
                content, MemoryImportance.HIGH, null, null);
    }

    /** A fact source returning fixed lines under a fixed id. */
    private static MemoryFactSource source(String id, String... facts) {
        return new MemoryFactSource() {
            @Override public String id() { return id; }
            @Override public List<String> factsFor(MemoryFactContext context) { return List.of(facts); }
        };
    }

    /** Minimal gateway returning a fixed candidate list; the other operations are unused by these tests. */
    private static final class FakeMemory implements MemoryGateway {
        private final List<MemoryEntry> candidates;

        private FakeMemory(List<MemoryEntry> candidates) {
            this.candidates = candidates;
        }

        @Override public MemorySnapshot snapshot() { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> recallCandidates(String query, int limit) { return candidates; }
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
