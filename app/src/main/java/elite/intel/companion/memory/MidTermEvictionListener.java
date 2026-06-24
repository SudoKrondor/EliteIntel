package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryEntry;

/**
 * Receives entries evicted from mid-term topic memory (per-topic overflow), so they can be consolidated
 * into the long-term summary. Lets the mechanical {@code MemoryGateway} hand off evicted entries without
 * itself calling the LLM (see COMPANION_ARCHITECTURE.md §3.1/§3.7); the
 * {@code MidTermToLongTermConsolidator} is the listener wired in at subsystem start.
 */
@FunctionalInterface
public interface MidTermEvictionListener {

    /** Called once per entry evicted from mid-term memory. */
    void onEvicted(MemoryEntry entry);
}
