package elite.intel.companion.model.memory;

/**
 * Lifecycle outcome recorded on a {@link MemoryEntry}, so the timeline carries honest provenance of
 * how each input was handled.
 * <p>
 * Kept minimal: only the states the current implementation actually writes. Further states (interrupt
 * outcomes) are added when their code paths land.
 */
public enum MemoryProcessingState {
    /** Handled normally to completion. */
    PROCESSED,
    /** Not carried to a result (e.g. invalid/failed LLM response). */
    UNRESOLVED,
    /** A dangerous tool-call set is frozen, waiting for the commander's confirmation (§2.13). */
    AWAITING_CONFIRMATION,
    /** The commander confirmed; the frozen set was executed. */
    CONFIRMED,
    /** The commander declined, or no confirmation slot was free; the frozen set was discarded. */
    CANCELLED,
    /** No confirmation arrived within the timeout; the frozen set was discarded. */
    TIMED_OUT,
    /** The thought was interrupted (safe-flush) before it could resolve to a normal outcome (§2.7). */
    INTERRUPTED
}
