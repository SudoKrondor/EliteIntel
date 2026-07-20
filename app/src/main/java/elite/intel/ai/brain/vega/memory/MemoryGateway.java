package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.model.memory.MemoryKind;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;

import java.util.List;
import java.util.Map;

/**
 * The single door to the companion's session memory. Callers publish only completed {@link MemoryRecord records};
 * the gateway owns their storage, whole-record eviction, semantic indexing and recall.
 */
public interface MemoryGateway {

    /**
     * Accepts one completed record. Normal records are stored atomically; an oversized record may be handed off
     * whole for asynchronous compression before its eventual atomic write. SAVED_TEXT goes directly to its
     * verbatim long-term storage.
     */
    void write(MemoryRecord record);

    /** Returns recent completed records, oldest-to-newest, for role-valid prompt replay. */
    List<MemoryRecord> readRecentHistory();

    /** Searches every memory area and returns bounded record-level matches with honest count metadata. */
    MemorySearchResult recallMatching(String query, int limit);

    /** Returns the long-term summaries by retained kind. */
    Map<MemoryKind, String> longTermSummaries();

    /** Returns one retained kind's summary, or an empty string when none exists. */
    default String longTermSummary(MemoryKind kind) {
        return longTermSummaries().getOrDefault(kind, "");
    }

    /** Atomically stores a summary and removes exactly the pending records it covers. */
    void commitConsolidation(MemoryKind kind, List<MemoryRecord> batch, String summary);

    /** Returns explicitly saved commander text, oldest-to-newest. */
    List<MemoryRecord> savedTextRecords();

    /** Returns an immutable snapshot of every memory area for diagnostics/export. */
    MemorySnapshot snapshot();
}
