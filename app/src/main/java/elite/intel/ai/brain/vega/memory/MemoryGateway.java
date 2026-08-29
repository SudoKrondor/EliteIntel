package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.model.memory.MemoryRecord;

import java.util.List;

/**
 * The single door to the companion's session memory. Callers publish only completed {@link MemoryRecord records};
 * the gateway owns their storage and whole-record eviction. Everything it holds is replayed in the next prompt -
 * memory that nothing reads is not collected.
 */
public interface MemoryGateway {

    /**
     * Accepts one completed record. Normal records are stored atomically; an oversized record may be handed off
     * whole for asynchronous compression before its eventual atomic write.
     */
    void write(MemoryRecord record);

    /** Returns recent completed records, oldest-to-newest, for role-valid prompt replay. */
    List<MemoryRecord> readRecentHistory();

    /**
     * Returns an immutable snapshot of session memory for diagnostics/export.
     */
    MemorySnapshot snapshot();
}
