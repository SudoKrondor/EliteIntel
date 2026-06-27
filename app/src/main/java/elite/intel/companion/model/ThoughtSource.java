package elite.intel.companion.model;

/**
 * Source of a {@code Thought}: which input stream gave birth to it.
 * <p>
 * Fixed at thought creation and drives tool access (see {@code IntelActionAccessPolicy} for game tools and
 * {@code SystemFunction.availableFor} for system functions): COMMANDER thoughts may run actions/macros,
 * EVENT thoughts are read-only (queries only), and NARRATION thoughts get no game tools at all - the
 * subscriber layer already calculated and filtered the data, so they only phrase it via {@code speak}.
 */
public enum ThoughtSource {
    /** Thought born from a commander voice/text input. */
    COMMANDER,
    /** Thought born from a filtered game event. */
    EVENT,
    /** Thought born from subscriber-prepared sensor narration: phrasing only, no game tools. */
    NARRATION
}
