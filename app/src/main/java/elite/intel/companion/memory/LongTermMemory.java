package elite.intel.companion.memory;

/**
 * Long-term memory area: a single session-wide compact summary of old, evicted mid-term memory,
 * always added to the prompt. Holds only the summary text; replaced atomically by
 * {@code MidTermToLongTermConsolidator}. Package-private internal of {@link SessionMemoryGateway}.
 */
class LongTermMemory {

    /** Current summary text. */
    String get() {
        throw new UnsupportedOperationException("TODO: Phase 4");
    }

    /** Atomically replaces the summary. */
    void replace(String summary) {
        throw new UnsupportedOperationException("TODO: Phase 4");
    }
}
