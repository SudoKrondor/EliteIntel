package elite.intel.companion.model;

/**
 * Source of a {@code Thought}: which input stream gave birth to it.
 * <p>
 * Fixed at thought creation and drives tool access: COMMANDER thoughts may run actions/macros,
 * EVENT thoughts are read-only (see {@code IntelActionAccessPolicy}).
 */
public enum ThoughtSource {
    /** Thought born from a commander voice/text input. */
    COMMANDER,
    /** Thought born from a filtered game event. */
    EVENT
}
