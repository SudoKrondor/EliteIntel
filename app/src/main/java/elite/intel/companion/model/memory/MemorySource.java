package elite.intel.companion.model.memory;

/**
 * Source marker on a {@link MemoryEntry}. Memory is a single timeline; the source only marks where a
 * piece of information came from, it does not create separate memories.
 */
public enum MemorySource {
    COMMANDER,
    EVENT,
    TOOL_RESULT,
    SYSTEM
}
