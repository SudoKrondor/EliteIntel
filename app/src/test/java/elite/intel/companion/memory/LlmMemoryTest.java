package elite.intel.companion.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage of the cyclic llm_memory store: ordered storage, blank rejection, 50-char truncation,
 * case/whitespace-insensitive dedup, and 15-entry cyclic eviction of the oldest.
 */
class LlmMemoryTest {

    @Test
    void storesInInsertionOrder() {
        LlmMemory memory = new LlmMemory();
        memory.add("first");
        memory.add("second");
        assertEquals(java.util.List.of("first", "second"), memory.all());
        assertEquals(2, memory.size());
    }

    @Test
    void ignoresBlankAndTruncatesToMaxLength() {
        LlmMemory memory = new LlmMemory();
        memory.add("   ");
        memory.add(null);
        assertEquals(0, memory.size());

        String tooLong = "x".repeat(LlmMemory.MAX_CONTENT_LENGTH + 10);
        memory.add(tooLong);
        assertEquals(LlmMemory.MAX_CONTENT_LENGTH, memory.all().get(0).length());
    }

    @Test
    void dedupsIgnoringCaseAndWhitespace() {
        LlmMemory memory = new LlmMemory();
        memory.add("Owes me 5cr");
        memory.add("  owes   me   5cr ");
        assertEquals(1, memory.size());
    }

    @Test
    void evictsOldestPastCapacity() {
        LlmMemory memory = new LlmMemory();
        for (int i = 0; i < LlmMemory.MAX_ENTRIES + 3; i++) {
            memory.add("item-" + i);
        }
        assertEquals(LlmMemory.MAX_ENTRIES, memory.size());
        // The three oldest (item-0..2) were evicted; item-3 is now the oldest.
        assertEquals("item-3", memory.all().get(0));
        assertTrue(memory.all().contains("item-" + (LlmMemory.MAX_ENTRIES + 2)));
    }
}
