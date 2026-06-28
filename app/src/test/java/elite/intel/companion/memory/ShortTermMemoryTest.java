package elite.intel.companion.memory;

import elite.intel.companion.CompanionConfig;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemorySource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct coverage of the short-term eviction movement: {@code evictOverflow()} returns exactly the
 * entries that leave the hot timeline for mid-term, so this asserts that payload, not just timeline size.
 */
class ShortTermMemoryTest {

    /** Constant per-entry token cost so count vs. budget eviction can be isolated in each test. */
    private static final class FixedTokenEstimator implements TokenEstimator {
        private final int perCall;

        FixedTokenEstimator(int perCall) {
            this.perCall = perCall;
        }

        @Override
        public int estimate(String text) {
            return perCall;
        }
    }

    private static MemoryEntry entry(String content) {
        return new MemoryEntry(Instant.now(), ConversationTopic.NAVIGATION, MemorySource.COMMANDER, content);
    }

    private static MemoryEntry entry(MemorySource source, String content) {
        return new MemoryEntry(Instant.now(), ConversationTopic.NAVIGATION, source, content);
    }

    private static List<String> contents(List<MemoryEntry> entries) {
        return entries.stream().map(MemoryEntry::content).collect(Collectors.toList());
    }

    @Test
    void evictOverflowReturnsNothingWhenWithinLimits() {
        ShortTermMemory memory = new ShortTermMemory(new FixedTokenEstimator(1));
        for (int i = 0; i < CompanionConfig.shortTermMemorySize(); i++) {
            memory.add(entry("e" + i));
        }
        assertTrue(memory.evictOverflow().isEmpty());
        assertEquals(CompanionConfig.shortTermMemorySize(), memory.timeline().size());
    }

    @Test
    void countOverflowEvictsExactOldestInOrder() {
        ShortTermMemory memory = new ShortTermMemory(new FixedTokenEstimator(1));
        int overflow = 3;
        for (int i = 0; i < CompanionConfig.shortTermMemorySize() + overflow; i++) {
            memory.add(entry("e" + i));
        }

        List<MemoryEntry> evicted = memory.evictOverflow();

        // Exactly the three oldest, oldest-first, are the ones handed to mid-term.
        assertEquals(List.of("e0", "e1", "e2"), contents(evicted));
        // The hot timeline retains the newest MAX_ENTRIES, oldest-to-newest.
        List<MemoryEntry> timeline = memory.timeline();
        assertEquals(CompanionConfig.shortTermMemorySize(), timeline.size());
        assertEquals("e3", timeline.get(0).content());
        assertEquals("e" + (CompanionConfig.shortTermMemorySize() + overflow - 1), timeline.get(timeline.size() - 1).content());
    }

    @Test
    void sustainedWritesEvictEveryEntryBeyondCapacityInOrder() {
        // Mirrors the gateway's add-then-evict loop over a long run; collects everything that moved out.
        ShortTermMemory memory = new ShortTermMemory(new FixedTokenEstimator(1));
        int total = CompanionConfig.shortTermMemorySize() * 3;
        List<String> evictedAll = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            memory.add(entry("e" + i));
            evictedAll.addAll(contents(memory.evictOverflow()));
        }

        // Everything except the last MAX_ENTRIES entries flowed to mid-term, in chronological order.
        List<String> expectedEvicted = new ArrayList<>();
        for (int i = 0; i < total - CompanionConfig.shortTermMemorySize(); i++) {
            expectedEvicted.add("e" + i);
        }
        assertEquals(expectedEvicted, evictedAll);
        assertEquals(CompanionConfig.shortTermMemorySize(), memory.timeline().size());
        assertEquals("e" + (total - 1), memory.timeline().get(memory.timeline().size() - 1).content());
    }

    @Test
    void tokenBudgetEvictsOldestButAlwaysKeepsNewest() {
        // Each entry costs the entire budget, so any second entry forces eviction down to the newest one.
        ShortTermMemory memory = new ShortTermMemory(new FixedTokenEstimator(CompanionMemoryLimits.SHORT_TERM_TOKEN_BUDGET));
        memory.add(entry("old"));
        memory.add(entry("new"));

        List<MemoryEntry> evicted = memory.evictOverflow();
        assertEquals(List.of("old"), contents(evicted));
        assertEquals(List.of("new"), contents(memory.timeline()));
    }

    @Test
    void repeatedCommandsCollapseKeepingTheLatestOccurrence() {
        // A subsystem-targeting cycle during combat: the same command recurs, interleaved with another.
        ShortTermMemory memory = new ShortTermMemory(new FixedTokenEstimator(1));
        for (String c : List.of("target drive", "target drive", "target power plant",
                "target drive", "target power plant", "target power plant")) {
            memory.add(entry(c));
        }

        // Each distinct command survives once, in order of its most recent use - so the current target is last.
        assertEquals(List.of("target drive", "target power plant"), contents(memory.timeline()));
    }

    @Test
    void dedupSubtractsTokensSoCollapsedDuplicatesDoNotMisfireEviction() {
        // Per-entry cost is half the budget, so two distinct entries sit exactly at the budget and nothing
        // evicts - but only if each collapsed duplicate's tokens were subtracted on removal. A broken
        // subtraction would inflate the estimate past the budget and wrongly evict the older "alpha".
        int perEntryEstimate = CompanionMemoryLimits.SHORT_TERM_TOKEN_BUDGET / 2
                - CompanionMemoryLimits.SHORT_TERM_ENTRY_FRAMING_OVERHEAD_TOKENS;
        ShortTermMemory memory = new ShortTermMemory(new FixedTokenEstimator(perEntryEstimate));
        memory.add(entry("alpha"));
        memory.add(entry("repeat"));
        memory.add(entry("repeat"));
        memory.add(entry("repeat"));

        assertTrue(memory.evictOverflow().isEmpty());
        assertEquals(List.of("alpha", "repeat"), contents(memory.timeline()));
    }

    @Test
    void sameContentFromDifferentSpeakersIsNotDeduped() {
        ShortTermMemory memory = new ShortTermMemory(new FixedTokenEstimator(1));
        memory.add(entry(MemorySource.COMMANDER, "shields"));
        memory.add(entry(MemorySource.COMPANION, "shields"));

        // Dedup only collapses one author's own repeat; the commander's word and the reply both stay.
        assertEquals(2, memory.timeline().size());
    }

    @Test
    void tokenBudgetNeverEvictsTheSoleEntry() {
        // A single entry over budget stays: the hot timeline must never go empty on the token rule.
        ShortTermMemory memory = new ShortTermMemory(new FixedTokenEstimator(CompanionMemoryLimits.SHORT_TERM_TOKEN_BUDGET + 1));
        memory.add(entry("only"));
        assertTrue(memory.evictOverflow().isEmpty());
        assertEquals(List.of("only"), contents(memory.timeline()));
    }
}
