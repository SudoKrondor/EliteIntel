package elite.intel.ai.brain.vega.diag;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.vega.memory.MemorySnapshot;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionMemoryDumpTest {

    private static final Instant TIME = Instant.parse("2026-07-05T10:00:00Z");

    private static MemorySnapshot sampleSnapshot() {
        return new MemorySnapshot(List.of(MemoryRecord.dialogue(TIME, "plot route to Sol", "route plotted")));
    }

    @Test
    void dumpContainsTheReplayedWindowAndItsCounts() {
        JsonObject dump = JsonParser.parseString(CompanionMemoryDump.toJson(sampleSnapshot())).getAsJsonObject();

        assertTrue(dump.has("dumpedAt"));
        assertTrue(dump.getAsJsonObject("limits").has("recentMaxRecords"));
        assertTrue(dump.getAsJsonObject("limits").has("recentTokenBudget"));
        assertTrue(dump.has("recent"));
        assertEquals(1, dump.getAsJsonObject("counts").get("recentRecords").getAsInt());
        assertEquals(2, dump.getAsJsonObject("counts").get("recentEntries").getAsInt());
    }

    @Test
    void recordKeepsKindTimestampAndEntries() {
        JsonObject record = JsonParser.parseString(CompanionMemoryDump.toJson(sampleSnapshot()))
                .getAsJsonObject().getAsJsonArray("recent").get(0).getAsJsonObject();

        assertEquals("2026-07-05T10:00:00Z", record.get("timestamp").getAsString());
        assertEquals("DIALOGUE", record.get("kind").getAsString());
        JsonObject entry = record.getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals("COMMANDER", entry.get("source").getAsString());
        assertEquals("plot route to Sol", entry.get("content").getAsString());
        assertFalse(entry.has("embedding"));
    }

    @Test
    void dumpedAtUsesWholeSecondJournalForm() {
        JsonObject dump = JsonParser.parseString(CompanionMemoryDump.toJson(sampleSnapshot())).getAsJsonObject();

        assertTrue(dump.get("dumpedAt").getAsString()
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }
}
