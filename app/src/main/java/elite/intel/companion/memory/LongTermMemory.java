package elite.intel.companion.memory;

/**
 * Long-term memory area: a single session-wide compact summary of old, evicted mid-term memory,
 * always added to the prompt. Holds only the summary text; replaced atomically by
 * {@code MidTermToLongTermConsolidator}. Package-private internal of {@link SessionMemoryGateway}.
 */
class LongTermMemory {

    // Empty until the consolidator fills it; replaced atomically as one reference write.
    private volatile String summary = "";

    /** Current summary text (empty string when nothing has been consolidated yet). */
    String get() {
        return summary;
    }

    /** Atomically replaces the summary; null is normalized to empty. */
    void replace(String summary) {
        this.summary = summary == null ? "" : summary;
    }
}
