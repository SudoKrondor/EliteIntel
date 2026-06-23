package elite.intel.companion.memory;

import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryProcessingState;
import elite.intel.companion.model.memory.MemorySource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage of the topic-keyed archive: entries group by their own topic, the index reports only
 * non-empty topics, and topics are returned in a stable enum order for the prompt block.
 */
class MidTermTopicMemoryTest {

    private static MemoryEntry entry(ConversationTopic topic, String content) {
        return new MemoryEntry(Instant.now(), topic, MemorySource.EVENT, content, MemoryProcessingState.PROCESSED);
    }

    @Test
    void emptyArchiveReportsNoTopics() {
        assertTrue(new MidTermTopicMemory().topicsWithMemory().isEmpty());
    }

    @Test
    void repeatedAddsToOneTopicFillItButReportItOnce() {
        MidTermTopicMemory memory = new MidTermTopicMemory();
        for (int i = 0; i < 5; i++) {
            memory.add(entry(ConversationTopic.COMBAT, "hit-" + i));
        }
        // The topic is filled by five entries but appears a single time in the index.
        assertEquals(List.of(ConversationTopic.COMBAT), memory.topicsWithMemory());
    }

    @Test
    void topicsAreReportedInEnumOrderRegardlessOfInsertionOrder() {
        MidTermTopicMemory memory = new MidTermTopicMemory();
        // Insert out of enum order: TRADE comes after COMBAT in the enum, NAVIGATION before both.
        memory.add(entry(ConversationTopic.TRADE, "t"));
        memory.add(entry(ConversationTopic.NAVIGATION, "n"));
        memory.add(entry(ConversationTopic.COMBAT, "c"));

        assertEquals(
                List.of(ConversationTopic.NAVIGATION, ConversationTopic.COMBAT, ConversationTopic.TRADE),
                memory.topicsWithMemory());
    }

    @Test
    void recallReturnsLatestEntriesChronologicallyCappedAtLimit() {
        MidTermTopicMemory memory = new MidTermTopicMemory();
        for (int i = 0; i < 5; i++) {
            memory.add(entry(ConversationTopic.NAVIGATION, "jump-" + i));
        }
        // Latest 2, returned oldest-to-newest.
        List<MemoryEntry> recalled = memory.recall(ConversationTopic.NAVIGATION, null, 2);
        assertEquals(List.of("jump-3", "jump-4"), recalled.stream().map(MemoryEntry::content).toList());
    }

    @Test
    void recallFiltersByQueryCaseInsensitively() {
        MidTermTopicMemory memory = new MidTermTopicMemory();
        memory.add(entry(ConversationTopic.NAVIGATION, "docked at Jameson"));
        memory.add(entry(ConversationTopic.NAVIGATION, "jumped to Sol"));
        memory.add(entry(ConversationTopic.NAVIGATION, "docked at Abraham"));

        List<MemoryEntry> recalled = memory.recall(ConversationTopic.NAVIGATION, "DOCKED", 10);
        assertEquals(List.of("docked at Jameson", "docked at Abraham"),
                recalled.stream().map(MemoryEntry::content).toList());
    }

    @Test
    void recallEmptyForUnknownTopicOrNonPositiveLimit() {
        MidTermTopicMemory memory = new MidTermTopicMemory();
        memory.add(entry(ConversationTopic.NAVIGATION, "n"));
        assertTrue(memory.recall(ConversationTopic.TRADE, null, 10).isEmpty());
        assertTrue(memory.recall(ConversationTopic.NAVIGATION, null, 0).isEmpty());
    }

    @Test
    void evictOverflowReturnsOldestBeyondPerTopicCap() {
        MidTermTopicMemory memory = new MidTermTopicMemory();
        int over = 3;
        for (int i = 0; i < CompanionMemoryLimits.MID_TERM_MAX_ENTRIES_PER_TOPIC + over; i++) {
            memory.add(entry(ConversationTopic.MINING, "rock-" + i));
        }

        List<MemoryEntry> evicted = memory.evictOverflow();

        // The three oldest overflow, in chronological order; the topic is left at the cap.
        assertEquals(List.of("rock-0", "rock-1", "rock-2"), evicted.stream().map(MemoryEntry::content).toList());
        assertEquals(CompanionMemoryLimits.MID_TERM_MAX_ENTRIES_PER_TOPIC,
                memory.recall(ConversationTopic.MINING, null, Integer.MAX_VALUE).size());
        assertTrue(memory.evictOverflow().isEmpty()); // nothing left to evict
    }
}
