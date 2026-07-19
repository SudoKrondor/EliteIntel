package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryRecord;

/** Receives records that remain searchable while waiting for long-term consolidation. */
@FunctionalInterface
public interface PendingConsolidationListener {

    /** Signals that one record entered the pending-consolidation area. */
    void onPending(MemoryRecord record);
}
