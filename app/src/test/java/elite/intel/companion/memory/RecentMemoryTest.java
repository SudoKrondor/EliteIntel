package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemoryRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecentMemoryTest {

    @Test
    void countOverflowEvictsOldestWholeRecord() {
        RecentMemory memory = new RecentMemory(text -> 0);
        for (int i = 0; i <= CompanionMemoryPolicy.recentRecordLimit(); i++) {
            memory.add(MemoryRecord.dialogue(Instant.ofEpochSecond(i), "order " + i, "reply " + i));
        }

        List<MemoryRecord> evicted = memory.evictOverflow();

        assertEquals(1, evicted.size());
        assertEquals(Instant.EPOCH, evicted.get(0).timestamp());
        assertEquals(CompanionMemoryPolicy.recentRecordLimit(), memory.records().size());
    }

    @Test
    void tokenOverflowNeverSplitsQueryPair() {
        RecentMemory memory = new RecentMemory(text -> text.length() * 100);
        MemoryRecord query = MemoryRecord.query(Instant.EPOCH, "cargo?", "eight tonnes");
        memory.add(query);
        memory.add(MemoryRecord.dialogue(Instant.ofEpochSecond(1), "hello", "hello commander"));

        List<MemoryRecord> evicted = memory.evictOverflow();

        assertEquals(List.of(query), evicted);
        assertEquals(MemoryKind.DIALOGUE, memory.records().get(0).kind());
    }
}
