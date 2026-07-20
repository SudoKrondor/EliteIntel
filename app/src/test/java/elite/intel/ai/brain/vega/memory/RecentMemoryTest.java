package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.memory.CompanionMemoryPolicy;
import elite.intel.ai.brain.vega.memory.RecentMemory;
import elite.intel.ai.brain.vega.model.memory.MemoryKind;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;
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

    @Test
    void delayedCompressedRecordIsInsertedByItsOriginalTimestamp() {
        RecentMemory memory = new RecentMemory(text -> 0);
        MemoryRecord later = MemoryRecord.event(Instant.ofEpochSecond(2), "later");
        MemoryRecord delayed = MemoryRecord.event(Instant.ofEpochSecond(1), "compressed later");

        memory.add(later);
        memory.add(delayed);

        assertEquals(List.of(delayed, later), memory.records());
    }
}
