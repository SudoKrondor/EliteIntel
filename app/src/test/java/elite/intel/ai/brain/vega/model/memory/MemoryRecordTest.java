package elite.intel.ai.brain.vega.model.memory;

import elite.intel.ai.brain.vega.model.memory.MemoryEntry;
import elite.intel.ai.brain.vega.model.memory.MemoryKind;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;
import elite.intel.ai.brain.vega.model.memory.MemorySource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemoryRecordTest {

    @Test
    void factoriesCreateProtocolValidShapes() {
        assertEquals(List.of(MemorySource.COMMANDER, MemorySource.COMPANION),
                MemoryRecord.dialogue(Instant.EPOCH, "hello", "hello").entries().stream()
                        .map(MemoryEntry::source).toList());
        assertEquals(2, MemoryRecord.query(Instant.EPOCH, "cargo?", "empty").entryCount());
    }

    @Test
    void rejectsPartialDialogueAndQuery() {
        assertThrows(IllegalArgumentException.class, () -> new MemoryRecord(
                Instant.EPOCH, MemoryKind.DIALOGUE,
                List.of(new MemoryEntry(MemorySource.COMMANDER, "hello"))));
        assertThrows(IllegalArgumentException.class, () -> new MemoryRecord(
                Instant.EPOCH, MemoryKind.QUERY, List.of(
                new MemoryEntry(MemorySource.COMMANDER, "cargo?"),
                new MemoryEntry(MemorySource.COMMANDER, "empty"))));
    }

}
