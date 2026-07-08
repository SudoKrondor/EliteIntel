package elite.intel.companion.diag;

import elite.intel.companion.CompanionConfig;
import elite.intel.companion.memory.CompanionMemoryLimits;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.ToolLink;
import elite.intel.util.json.GsonFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single owner of the companion-memory diagnostic dump format: turns a {@link MemorySnapshot} into a
 * pretty-printed JSON document meant to be read by both a human and an assistant when investigating what the
 * companion remembers and why recall behaved as it did.
 * <p>
 * The document keeps every recorded field of a {@link MemoryEntry} (timestamp, topic, source, importance,
 * verbatim content, optional canonical fact and tool linkage) but omits the meaning-vector - hundreds of
 * floats that are noise to a reader - surfacing only whether one is present. A header records the moment of
 * the dump, the configured memory limits, and the current per-area counts, so eviction pressure
 * (counts vs limits) is visible at a glance.
 * <p>
 * Serialization is delegated to the shared {@link GsonFactory} Gson (pretty-printing, ISO-8601 {@code Instant}s,
 * null fields omitted), so the format stays consistent with the rest of the app and no field ordering or
 * escaping is hand-rolled here. Timestamps are truncated to whole seconds so they render as the
 * {@code yyyy-MM-ddTHH:mm:ssZ} journal form, matching the exported logs and the game journal for correlation.
 */
public final class CompanionMemoryDump {

    private CompanionMemoryDump() {
    }

    /** Serializes the snapshot into the pretty-printed JSON dump document. */
    public static String toJson(MemorySnapshot snapshot) {
        return GsonFactory.getGson().toJson(build(snapshot));
    }

    private static Dump build(MemorySnapshot snapshot) {
        Map<String, List<Entry>> midTerm = new LinkedHashMap<>();
        Map<String, Integer> midTermCounts = new LinkedHashMap<>();
        int midTermTotal = 0;
        for (Map.Entry<ConversationTopic, List<MemoryEntry>> byTopic : snapshot.midTermByTopic().entrySet()) {
            List<Entry> entries = mapEntries(byTopic.getValue());
            midTerm.put(byTopic.getKey().id(), entries);
            midTermCounts.put(byTopic.getKey().id(), entries.size());
            midTermTotal += entries.size();
        }

        Limits limits = new Limits(
                CompanionConfig.shortTermMemorySize(),
                CompanionMemoryLimits.SHORT_TERM_TOKEN_BUDGET,
                CompanionConfig.midTermMemorySizePerTopic(),
                CompanionConfig.memoryEntryMaxChars(),
                CompanionConfig.semanticDedupFloor());

        String summary = snapshot.longTermSummary() == null ? "" : snapshot.longTermSummary();
        Counts counts = new Counts(
                snapshot.shortTerm().size(),
                midTermCounts,
                midTermTotal,
                snapshot.longTermPinned().size(),
                summary.length());

        return new Dump(
                secondsUtc(Instant.now()),
                limits,
                counts,
                mapEntries(snapshot.shortTerm()),
                midTerm,
                summary,
                mapEntries(snapshot.longTermPinned()));
    }

    private static List<Entry> mapEntries(List<MemoryEntry> entries) {
        return entries.stream().map(CompanionMemoryDump::mapEntry).toList();
    }

    /**
     * Truncates a timestamp to whole seconds so the shared Gson {@code Instant} adapter renders it as
     * {@code yyyy-MM-ddTHH:mm:ssZ} (the journal/log form), without the sub-second precision those do not carry.
     */
    private static Instant secondsUtc(Instant timestamp) {
        return timestamp == null ? null : timestamp.truncatedTo(ChronoUnit.SECONDS);
    }

    private static Entry mapEntry(MemoryEntry e) {
        return new Entry(
                secondsUtc(e.timestamp()),
                e.topic() == null ? null : e.topic().id(),
                e.source() == null ? null : e.source().name(),
                e.importance() == null ? null : e.importance().name(),
                e.content(),
                e.canonicalFact(),
                e.embedding() != null,
                e.toolLink());
    }

    // --- serializable model (field names become JSON keys; nulls are omitted by the shared Gson) ---

    private record Dump(Instant dumpedAt, Limits limits, Counts counts,
                        List<Entry> shortTerm, Map<String, List<Entry>> midTerm,
                        String longTermSummary, List<Entry> longTermPinned) {
    }

    private record Limits(int shortTermMaxEntries, int shortTermTokenBudget, int midTermMaxPerTopic,
                          int memoryEntryMaxChars, double semanticDedupFloor) {
    }

    private record Counts(int shortTerm, Map<String, Integer> midTermByTopic, int midTermTotal,
                          int longTermPinned, int longTermSummaryChars) {
    }

    private record Entry(Instant timestamp, String topic, String source, String importance,
                         String content, String canonicalFact, boolean hasEmbedding, ToolLink toolLink) {
    }
}
