package elite.intel.companion.model.memory;

import elite.intel.companion.model.ConversationTopic;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryEntryTest {

    @Test
    void embeddingTextPrefersCanonicalWhenPresent() {
        MemoryEntry entry = new MemoryEntry(Instant.now(), ConversationTopic.TRADE, MemorySource.COMMANDER,
                "и запиши: покупатель утиля — халлоран", MemoryImportance.HIGH, null, "покупатель утиля — халлоран");

        assertEquals("покупатель утиля — халлоран", entry.embeddingText());
    }

    @Test
    void embeddingTextFallsBackToContentWhenCanonicalIsNull() {
        MemoryEntry entry = new MemoryEntry(Instant.now(), ConversationTopic.MINING, MemorySource.COMMANDER,
                "поле зовётся бедлам", MemoryImportance.NORMAL);

        assertEquals("поле зовётся бедлам", entry.embeddingText());
    }

    @Test
    void embeddingTextFallsBackToContentWhenCanonicalIsBlank() {
        MemoryEntry entry = new MemoryEntry(Instant.now(), ConversationTopic.MINING, MemorySource.COMMANDER,
                "поле зовётся бедлам", MemoryImportance.NORMAL, null, "   ");

        assertEquals("поле зовётся бедлам", entry.embeddingText());
    }
}
