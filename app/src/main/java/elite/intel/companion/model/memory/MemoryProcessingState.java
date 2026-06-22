package elite.intel.companion.model.memory;

/**
 * Lifecycle outcome recorded on a {@link MemoryEntry}, so the timeline carries honest provenance of
 * how each input was handled.
 * <p>
 * Kept minimal: only the states the current implementation actually writes. Further states
 * (e.g. dangerous-confirmation and interrupt outcomes) are added when their code paths land.
 */
public enum MemoryProcessingState {
    /** Handled normally to completion. */
    PROCESSED,
    /** Not carried to a result (e.g. invalid/failed LLM response). */
    UNRESOLVED
}
