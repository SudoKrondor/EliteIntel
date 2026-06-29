package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryEntry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Long-term memory area in two parts: a single session-wide compact summary of old, evicted mid-term memory
 * (always added to the prompt; replaced atomically by {@code MidTermToLongTermConsolidator}), and an archive
 * of pinned {@code MAX}-importance facts carried verbatim - never summarized, never dropped (the commander
 * explicitly asked to remember them). The archive is surfaced through {@code search_in_memory}
 * (importance-ranked, capped), NOT force-fed into every prompt, so it can grow without bloating the context.
 * Package-private internal of {@link SessionMemoryGateway}.
 */
class LongTermMemory {

    // Empty until the consolidator fills it; replaced atomically as one reference write.
    private volatile String summary = "";
    // MAX-importance facts, kept verbatim and never compressed; an archive searched on demand, not always-on.
    private final List<MemoryEntry> pinnedFacts = new CopyOnWriteArrayList<>();

    /** Current summary text (empty string when nothing has been consolidated yet). */
    String get() {
        return summary;
    }

    /** Atomically replaces the summary; null is normalized to empty. */
    void replace(String summary) {
        this.summary = summary == null ? "" : summary;
    }

    /** The pinned MAX-importance facts, in the order they were pinned (oldest-to-newest). */
    List<MemoryEntry> pinnedFacts() {
        return List.copyOf(pinnedFacts);
    }

    /** Pins a MAX-importance fact verbatim; a null, or content already pinned, is ignored (no duplicates). */
    void pin(MemoryEntry fact) {
        if (fact == null || fact.content() == null) {
            return;
        }
        for (MemoryEntry existing : pinnedFacts) {
            if (fact.content().equals(existing.content())) {
                return; // the commander already had this exact fact pinned; do not archive it twice
            }
        }
        pinnedFacts.add(fact);
    }
}
