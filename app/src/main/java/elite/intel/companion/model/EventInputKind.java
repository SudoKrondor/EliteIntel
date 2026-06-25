package elite.intel.companion.model;

/**
 * EVENT-thought subtype. The source stays {@link ThoughtSource#EVENT}, while the kind controls small
 * lifecycle differences such as whether subscriber-prepared narration may use query tools or be silenced
 * by ambient verbosity.
 */
public enum EventInputKind {
    /** Raw or lightly formatted journal/status gameplay event. */
    GAME_EVENT,
    /** Pre-digested subscriber output that already decided something should be narrated. */
    SENSOR_NARRATION
}
