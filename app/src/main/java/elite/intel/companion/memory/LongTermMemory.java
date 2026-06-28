package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryEntry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Long-term memory area, two parts always added to the prompt: a single session-wide compact summary of old,
 * evicted mid-term memory (replaced atomically by {@code MidTermToLongTermConsolidator}), and a list of
 * pinned {@code MAX}-importance facts carried verbatim - never summarized, never dropped (the commander
 * explicitly asked to remember them). Package-private internal of {@link SessionMemoryGateway}.
 */
class LongTermMemory {

    // Empty until the consolidator fills it; replaced atomically as one reference write.
    private volatile String summary = "";
    // MAX-importance facts, kept verbatim and never compressed; appended as they reach long-term.
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

    /** Pins a MAX-importance fact verbatim; a null is ignored. */
    void pin(MemoryEntry fact) {
        if (fact != null) {
            pinnedFacts.add(fact);
        }
    }
}
