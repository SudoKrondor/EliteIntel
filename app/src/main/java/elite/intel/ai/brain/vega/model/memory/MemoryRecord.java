package elite.intel.ai.brain.vega.model.memory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * One completed, indivisible unit of companion memory. Storage and eviction always operate on the whole record,
 * so a completed pair or event fact can never be observed or retained only in part.
 *
 * @param timestamp completion time used for ordering and recency ranking
 * @param kind      the record's retention contract
 * @param entries   protocol-ordered entries; copied and validated at construction
 */
public record MemoryRecord(Instant timestamp, MemoryKind kind, List<MemoryEntry> entries) {

    public MemoryRecord {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(kind, "kind");
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        validateShape(kind, entries);
    }

    /** Creates a completed commander/companion dialogue pair. */
    public static MemoryRecord dialogue(Instant timestamp, String commander, String companion) {
        return new MemoryRecord(timestamp, MemoryKind.DIALOGUE, List.of(
                new MemoryEntry(MemorySource.COMMANDER, commander),
                new MemoryEntry(MemorySource.COMPANION, companion)));
    }

    /** Creates a completed commander query and its spoken answer without retaining execution details. */
    public static MemoryRecord query(Instant timestamp, String commander, String companion) {
        return new MemoryRecord(timestamp, MemoryKind.QUERY, List.of(
                new MemoryEntry(MemorySource.COMMANDER, commander),
                new MemoryEntry(MemorySource.COMPANION, companion)));
    }

    /** Returns a copy with transformed entries while preserving completion time and kind. */
    public MemoryRecord withEntries(List<MemoryEntry> entries) {
        return new MemoryRecord(timestamp, kind, entries);
    }

    /** Total number of role/protocol entries carried by this record. */
    public int entryCount() {
        return entries.size();
    }

    /** Returns the commander side of a DIALOGUE or QUERY pair. */
    public String commanderText() {
        requireKind(MemoryKind.DIALOGUE, MemoryKind.QUERY);
        return entries.get(0).content();
    }

    /** Returns the companion side of a DIALOGUE or QUERY pair. */
    public String companionText() {
        requireKind(MemoryKind.DIALOGUE, MemoryKind.QUERY);
        return entries.get(1).content();
    }

    private static void validateShape(MemoryKind kind, List<MemoryEntry> entries) {
        switch (kind) {
            case DIALOGUE, QUERY -> requireSources(kind, entries, MemorySource.COMMANDER, MemorySource.COMPANION);
        }
    }

    private static void requireSources(MemoryKind kind, List<MemoryEntry> entries, MemorySource... sources) {
        if (entries.size() != sources.length) {
            throw new IllegalArgumentException(kind + " requires " + sources.length + " entries");
        }
        for (int i = 0; i < sources.length; i++) {
            MemoryEntry entry = Objects.requireNonNull(entries.get(i), "entries[" + i + "]");
            if (entry.source() != sources[i]) {
                throw new IllegalArgumentException(kind + " entry " + i + " must come from " + sources[i]);
            }
        }
    }

    private void requireKind(MemoryKind... allowed) {
        for (MemoryKind candidate : allowed) {
            if (kind == candidate) {
                return;
            }
        }
        throw new IllegalStateException("Text accessor does not apply to " + kind);
    }
}
