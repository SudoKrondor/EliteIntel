package elite.intel.companion.diag;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemorySource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the JSON shape produced by {@link CompanionMemoryDump}: every memory area is present, each entry keeps
 * its recorded fields, the meaning-vector is excluded (only a {@code hasEmbedding} marker), and the counts
 * header reflects the snapshot.
 */
class CompanionMemoryDumpTest {

    private static MemoryEntry entry(String content, MemorySource source, MemoryImportance importance,
                                     float[] embedding, String canonicalFact) {
        return new MemoryEntry(Instant.parse("2026-07-05T10:00:00Z"), ConversationTopic.NAVIGATION,
                source, content, importance, embedding, canonicalFact);
    }

    private static MemorySnapshot sampleSnapshot() {
        MemoryEntry shortA = entry("docked at abraham lincoln", MemorySource.EVENT, MemoryImportance.NORMAL,
                new float[]{0.1f, 0.2f}, null);
        MemoryEntry midA = entry("route plotted to sol", MemorySource.COMMANDER, MemoryImportance.HIGH,
                null, "commander wants to reach sol");
        MemoryEntry pinned = entry("commander name is jameson", MemorySource.COMMANDER, MemoryImportance.MAX,
                null, null);
        return new MemorySnapshot(
                List.of(shortA),
                Map.of(ConversationTopic.NAVIGATION, List.of(midA)),
                "old jumps were summarized here",
                List.of(pinned));
    }

    @Test
    void dumpContainsEveryAreaAndHeader() {
        JsonObject dump = JsonParser.parseString(CompanionMemoryDump.toJson(sampleSnapshot())).getAsJsonObject();

        assertTrue(dump.has("dumpedAt"));
        assertTrue(dump.has("limits"));
        // Both short-term eviction drivers are recorded so token-based ageing is diagnosable.
        assertTrue(dump.getAsJsonObject("limits").has("shortTermMaxEntries"));
        assertTrue(dump.getAsJsonObject("limits").has("shortTermTokenBudget"));
        assertTrue(dump.has("counts"));
        assertTrue(dump.has("shortTerm"));
        assertTrue(dump.has("midTerm"));
        assertEquals("old jumps were summarized here", dump.get("longTermSummary").getAsString());
        assertTrue(dump.has("longTermPinned"));

        JsonObject counts = dump.getAsJsonObject("counts");
        assertEquals(1, counts.get("shortTerm").getAsInt());
        assertEquals(1, counts.get("midTermTotal").getAsInt());
        assertEquals(1, counts.get("longTermPinned").getAsInt());
        assertEquals(1, counts.getAsJsonObject("midTermByTopic").get("navigation").getAsInt());
    }

    @Test
    void entryKeepsRecordedFieldsButNotTheVector() {
        JsonObject dump = JsonParser.parseString(CompanionMemoryDump.toJson(sampleSnapshot())).getAsJsonObject();
        JsonObject shortEntry = dump.getAsJsonArray("shortTerm").get(0).getAsJsonObject();

        assertEquals("navigation", shortEntry.get("topic").getAsString());
        assertEquals("EVENT", shortEntry.get("source").getAsString());
        assertEquals("NORMAL", shortEntry.get("importance").getAsString());
        assertEquals("docked at abraham lincoln", shortEntry.get("content").getAsString());
        // Timestamp is the whole-second UTC journal form, matching the exported logs and the game journal.
        assertEquals("2026-07-05T10:00:00Z", shortEntry.get("timestamp").getAsString());
        // The meaning-vector is never dumped; only its presence is flagged.
        assertFalse(shortEntry.has("embedding"));
        assertTrue(shortEntry.get("hasEmbedding").getAsBoolean());
    }

    @Test
    void dumpedAtUsesTheWholeSecondJournalForm() {
        JsonObject dump = JsonParser.parseString(CompanionMemoryDump.toJson(sampleSnapshot())).getAsJsonObject();
        // dumpedAt comes from Instant.now(); it must be truncated to whole seconds (no sub-second part) so it
        // reads exactly like the game journal / exported log timestamps.
        assertTrue(dump.get("dumpedAt").getAsString().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"),
                "dumpedAt must be the yyyy-MM-ddTHH:mm:ssZ journal form");
    }

    @Test
    void midTermEntryCarriesCanonicalFactAndPinnedIsNavigation() {
        JsonObject dump = JsonParser.parseString(CompanionMemoryDump.toJson(sampleSnapshot())).getAsJsonObject();

        JsonArray midNav = dump.getAsJsonObject("midTerm").getAsJsonArray("navigation");
        JsonObject midEntry = midNav.get(0).getAsJsonObject();
        assertEquals("commander wants to reach sol", midEntry.get("canonicalFact").getAsString());
        assertFalse(midEntry.get("hasEmbedding").getAsBoolean());

        JsonObject pinned = dump.getAsJsonArray("longTermPinned").get(0).getAsJsonObject();
        assertEquals("MAX", pinned.get("importance").getAsString());
    }
}
