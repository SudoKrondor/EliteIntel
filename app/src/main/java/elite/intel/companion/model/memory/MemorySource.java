package elite.intel.companion.model.memory;

/**
 * Source marker on a {@link MemoryEntry}. Memory is a single timeline; the source only marks where a
 * piece of information came from, it does not create separate memories.
 */
public enum MemorySource {
    COMMANDER,
    EVENT,
    TOOL_RESULT,
    SYSTEM,
    /** The companion's own spoken words (a {@code speak}/clarification/confirmation it uttered). */
    COMPANION;

    /**
     * The speaker tag this source is rendered as in the prompt timeline and in {@code search_in_memory}
     * results. The single owner of the source-to-label mapping (used by every render site, so they stay in
     * sync). {@code COMPANION} is shown as the companion's own name, so its lines read as a named speaker
     * rather than an abstract role; every other source keeps its stable enum name. The name is passed in,
     * not read here, to keep this model enum free of a config dependency.
     *
     * @param companionName the configured companion name; falls back to {@link #name()} when null/blank
     * @return the label to print inside the {@code [..]} speaker tag
     */
    public String displayLabel(String companionName) {
        return this == COMPANION && companionName != null && !companionName.isBlank()
                ? companionName.trim()
                : name();
    }
}
