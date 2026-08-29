package elite.intel.ai.brain.vega.diag;

import elite.intel.ai.brain.vega.memory.CompanionMemoryPolicy;
import elite.intel.ai.brain.vega.memory.MemorySnapshot;
import elite.intel.ai.brain.vega.model.memory.MemoryEntry;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;
import elite.intel.util.json.GsonFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Renders a companion session-memory snapshot as readable diagnostic JSON.
 */
public final class CompanionMemoryDump {

    private CompanionMemoryDump() {
    }

    /** Serializes the replayed conversation window with its current limits. */
    public static String toJson(MemorySnapshot snapshot) {
        return GsonFactory.getGson().toJson(build(snapshot));
    }

    private static Dump build(MemorySnapshot snapshot) {
        Limits limits = new Limits(
                CompanionMemoryPolicy.recentRecordLimit(),
                CompanionMemoryPolicy.recentTokenBudget(),
                CompanionMemoryPolicy.entryMaxChars());
        Counts counts = new Counts(snapshot.recent().size(), entryCount(snapshot.recent()));

        return new Dump(secondsUtc(Instant.now()), limits, counts, mapRecords(snapshot.recent()));
    }

    private static int entryCount(List<MemoryRecord> records) {
        return records.stream().mapToInt(MemoryRecord::entryCount).sum();
    }

    private static List<Record> mapRecords(List<MemoryRecord> records) {
        return records.stream().map(CompanionMemoryDump::mapRecord).toList();
    }

    private static Record mapRecord(MemoryRecord record) {
        return new Record(secondsUtc(record.timestamp()), record.kind().name(),
                record.entries().stream().map(CompanionMemoryDump::mapEntry).toList());
    }

    private static Entry mapEntry(MemoryEntry entry) {
        return new Entry(entry.source().name(), entry.content());
    }

    private static Instant secondsUtc(Instant timestamp) {
        return timestamp == null ? null : timestamp.truncatedTo(ChronoUnit.SECONDS);
    }

    private record Dump(Instant dumpedAt, Limits limits, Counts counts, List<Record> recent) {
    }

    private record Limits(int recentMaxRecords, int recentTokenBudget, int memoryEntryMaxChars) {
    }

    private record Counts(int recentRecords, int recentEntries) {
    }

    private record Record(Instant timestamp, String kind, List<Entry> entries) {
    }

    private record Entry(String source, String content) {
    }
}
