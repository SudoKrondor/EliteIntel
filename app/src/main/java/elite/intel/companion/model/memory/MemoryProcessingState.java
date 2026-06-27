package elite.intel.companion.model.memory;

/**
 * Outcome of a dangerous-action confirmation wait (§2.13): the value {@code awaitConfirmationOutcome}
 * resolves to, used to decide whether the frozen tool-call set runs and to phrase the recorded result.
 * It is not stored on a memory entry; provenance lives in the entry's content text.
 */
public enum MemoryProcessingState {
    /** The commander confirmed; the frozen set was executed. */
    CONFIRMED,
    /** The commander declined, no confirmation slot was free, or the wait failed; the set was discarded. */
    CANCELLED,
    /** No confirmation arrived within the timeout; the frozen set was discarded. */
    TIMED_OUT,
    /** The wait was interrupted (safe-flush) before it could resolve (§2.7). */
    INTERRUPTED
}
