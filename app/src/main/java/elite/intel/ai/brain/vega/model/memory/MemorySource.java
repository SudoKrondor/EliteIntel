package elite.intel.ai.brain.vega.model.memory;

/**
 * Source marker on an entry within a completed {@link MemoryRecord}. It preserves the role or protocol origin of
 * the text without deciding the record's retention policy.
 */
public enum MemorySource {
    COMMANDER,
    /**
     * VEGA's own reply in a completed dialogue or query record.
     */
    VEGA;

    /**
     * Uses the configured AI name for its lines and stable enum names for every other source.
     */
    public String displayLabel(String vegaName) {
        return this == VEGA && vegaName != null && !vegaName.isBlank()
                ? vegaName.trim()
                : name();
    }
}
