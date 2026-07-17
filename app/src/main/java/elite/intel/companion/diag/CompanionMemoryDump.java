package elite.intel.companion.diag;

import elite.intel.companion.memory.CompanionMemoryPolicy;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemoryRecord;
import elite.intel.util.json.GsonFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Renders a complete record-based companion-memory snapshot as readable diagnostic JSON. */
public final class CompanionMemoryDump {

    private CompanionMemoryDump() {
    }

    /** Serializes the snapshot with vectors omitted but their presence reported. */
    public static String toJson(MemorySnapshot snapshot) {
        return GsonFactory.getGson().toJson(build(snapshot));
    }

    private static Dump build(MemorySnapshot snapshot) {
        Map<String, List<Record>> retained = new LinkedHashMap<>();
        Map<String, Integer> retainedCounts = new LinkedHashMap<>();
        for (MemoryKind kind : MemoryKind.values()) {
            List<MemoryRecord> records = snapshot.retainedByKind().get(kind);
            if (records == null) {
                continue;
            }
            retained.put(kind.name(), mapRecords(records));
            retainedCounts.put(kind.name(), records.size());
        }
        Map<String, List<Record>> pending = new LinkedHashMap<>();
        Map<String, Integer> pendingCounts = new LinkedHashMap<>();
        snapshot.pendingByKind().forEach((kind, records) -> {
            pending.put(kind.name(), mapRecords(records));
            pendingCounts.put(kind.name(), records.size());
        });

        Map<String, String> summaries = new LinkedHashMap<>();
        snapshot.summaries().forEach((kind, summary) -> summaries.put(kind.name(), summary));

        Limits limits = new Limits(
                CompanionMemoryPolicy.recentRecordLimit(),
                CompanionMemoryPolicy.recentTokenBudget(),
                CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.DIALOGUE),
                CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.EVENT),
                CompanionMemoryPolicy.entryMaxChars(),
                CompanionMemoryPolicy.semanticDedupFloor());
        Counts counts = new Counts(
                snapshot.recent().size(),
                entryCount(snapshot.recent()),
                retainedCounts,
                pendingCounts,
                snapshot.savedTexts().size(),
                summaries.values().stream().mapToInt(String::length).sum());

        return new Dump(secondsUtc(Instant.now()), limits, counts,
                mapRecords(snapshot.recent()), retained, pending, summaries, mapRecords(snapshot.savedTexts()));
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
        return new Entry(entry.source().name(), entry.content(), entry.embedding() != null);
    }

    private static Instant secondsUtc(Instant timestamp) {
        return timestamp == null ? null : timestamp.truncatedTo(ChronoUnit.SECONDS);
    }

    private record Dump(
            Instant dumpedAt,
            Limits limits,
            Counts counts,
            List<Record> recent,
            Map<String, List<Record>> retained,
            Map<String, List<Record>> pending,
            Map<String, String> summaries,
            List<Record> savedTexts
    ) {
    }

    private record Limits(
            int recentMaxRecords,
            int recentTokenBudget,
            int retainedDialogueMaxRecords,
            int retainedEventMaxRecords,
            int memoryEntryMaxChars,
            double semanticDedupFloor
    ) {
    }

    private record Counts(
            int recentRecords,
            int recentEntries,
            Map<String, Integer> retainedRecordsByKind,
            Map<String, Integer> pendingRecordsByKind,
            int savedTextRecords,
            int summaryChars
    ) {
    }

    private record Record(Instant timestamp, String kind, List<Entry> entries) {
    }

    private record Entry(String source, String content, boolean hasEmbedding) {
    }
}
