package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryEntry;

import java.util.List;

/**
 * Hot chronological timeline of recent entries, inserted directly into the prompt as a context block.
 * Bounded by max entry count and a token budget; overflow is evicted into mid-term topic memory.
 * Package-private internal of {@link SessionMemoryGateway}.
 */
class ShortTermMemory {

    /** Appends a new entry. */
    void add(MemoryEntry entry) {
        throw new UnsupportedOperationException("TODO: Phase 2");
    }

    /** Current timeline, oldest-to-newest. */
    List<MemoryEntry> timeline() {
        throw new UnsupportedOperationException("TODO: Phase 2");
    }

    /** Evicts entries that exceed the count/token limits and returns them for mid-term storage. */
    List<MemoryEntry> evictOverflow() {
        throw new UnsupportedOperationException("TODO: Phase 2");
    }
}
