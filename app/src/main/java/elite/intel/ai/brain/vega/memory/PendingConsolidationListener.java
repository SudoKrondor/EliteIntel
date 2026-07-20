package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.model.memory.MemoryRecord;

/** Receives records that remain searchable while waiting for long-term consolidation. */
@FunctionalInterface
public interface PendingConsolidationListener {

    /** Signals that one record entered the pending-consolidation area. */
    void onPending(MemoryRecord record);
}
