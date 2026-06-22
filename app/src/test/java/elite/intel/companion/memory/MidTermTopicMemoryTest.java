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
}
