package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemoryRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MidTermMemoryTest {

    @Test
    void dialogueAndEventHaveIndependentLimits() {
        MidTermMemory memory = new MidTermMemory();
        for (int i = 0; i <= CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.DIALOGUE); i++) {
            memory.add(MemoryRecord.dialogue(Instant.ofEpochSecond(i), "order " + i, "reply " + i));
        }
        MemoryRecord event = MemoryRecord.event(Instant.ofEpochSecond(1000), "arrived");
        memory.add(event);

        List<MemoryRecord> evicted = memory.stageOverflow();

        assertEquals(1, evicted.size());
        assertEquals(Instant.EPOCH, evicted.get(0).timestamp());
        assertEquals(CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.DIALOGUE),
                memory.retainedSnapshot().get(MemoryKind.DIALOGUE).size());
        assertEquals(List.of(evicted.get(0)), memory.pendingSnapshot().get(MemoryKind.DIALOGUE));
        assertEquals(CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.DIALOGUE) + 1,
                memory.records(MemoryKind.DIALOGUE).size());
        assertEquals(List.of(event), memory.records(MemoryKind.EVENT));
    }

    @Test
    void rejectsKindsWithoutRetainedStorage() {
        MidTermMemory memory = new MidTermMemory();

        assertThrows(IllegalArgumentException.class,
                () -> memory.add(MemoryRecord.savedText(Instant.EPOCH, "remember this")));
    }
}
