package elite.intel.companion.model.memory;

/**
 * Source marker on an entry within a completed {@link MemoryRecord}. It preserves the role or protocol origin of
 * the text without deciding the record's retention policy.
 */
public enum MemorySource {
    COMMANDER,
    EVENT,
    SYSTEM,
    /** The companion's own reply in a completed dialogue or query record. */
    COMPANION;

    /** Uses the configured companion name for its lines and stable enum names for every other source. */
    public String displayLabel(String companionName) {
        return this == COMPANION && companionName != null && !companionName.isBlank()
                ? companionName.trim()
                : name();
    }
}
