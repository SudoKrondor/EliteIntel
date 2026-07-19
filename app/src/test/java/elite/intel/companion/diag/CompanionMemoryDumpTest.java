package elite.intel.companion.diag;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemoryRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionMemoryDumpTest {

    private static final Instant TIME = Instant.parse("2026-07-05T10:00:00Z");

    private static MemorySnapshot sampleSnapshot() {
        MemoryRecord recent = MemoryRecord.event(TIME, "docked at Abraham Lincoln");
        recent = recent.withEntries(List.of(
                recent.entries().get(0).withEmbedding(new float[]{0.1f, 0.2f})));
        MemoryRecord retained = MemoryRecord.dialogue(TIME, "plot route to Sol", "route plotted");
        MemoryRecord savedText = MemoryRecord.savedText(TIME, "my name is Jameson");
        return new MemorySnapshot(
                List.of(recent),
                Map.of(MemoryKind.DIALOGUE, List.of(retained)),
                Map.of(),
                Map.of(MemoryKind.EVENT, "old jumps were summarized here"),
                List.of(savedText));
    }

    @Test
    void dumpContainsEveryRecordBasedAreaAndCounts() {
        JsonObject dump = JsonParser.parseString(CompanionMemoryDump.toJson(sampleSnapshot())).getAsJsonObject();

        assertTrue(dump.has("dumpedAt"));
        assertTrue(dump.getAsJsonObject("limits").has("recentMaxRecords"));
        assertTrue(dump.getAsJsonObject("limits").has("recentTokenBudget"));
        assertTrue(dump.has("recent"));
        assertTrue(dump.has("retained"));
        assertTrue(dump.has("summaries"));
        assertTrue(dump.has("savedTexts"));
        assertEquals(1, dump.getAsJsonObject("counts").get("recentRecords").getAsInt());
        assertEquals(1, dump.getAsJsonObject("counts").get("recentEntries").getAsInt());
        assertEquals(1, dump.getAsJsonObject("counts").get("savedTextRecords").getAsInt());
        assertEquals(1, dump.getAsJsonObject("counts")
                .getAsJsonObject("retainedRecordsByKind").get("DIALOGUE").getAsInt());
    }

    @Test
    void recordKeepsKindTimestampAndEntriesWithoutDumpingVectors() {
        JsonObject record = JsonParser.parseString(CompanionMemoryDump.toJson(sampleSnapshot()))
                .getAsJsonObject().getAsJsonArray("recent").get(0).getAsJsonObject();

        assertEquals("2026-07-05T10:00:00Z", record.get("timestamp").getAsString());
        assertEquals("EVENT", record.get("kind").getAsString());
        JsonObject entry = record.getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals("EVENT", entry.get("source").getAsString());
        assertEquals("docked at Abraham Lincoln", entry.get("content").getAsString());
        assertFalse(entry.has("embedding"));
        assertTrue(entry.get("hasEmbedding").getAsBoolean());
    }

    @Test
    void dumpedAtUsesWholeSecondJournalForm() {
        JsonObject dump = JsonParser.parseString(CompanionMemoryDump.toJson(sampleSnapshot())).getAsJsonObject();

        assertTrue(dump.get("dumpedAt").getAsString()
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }
}
