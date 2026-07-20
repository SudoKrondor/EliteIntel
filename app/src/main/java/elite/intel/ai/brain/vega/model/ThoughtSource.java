package elite.intel.ai.brain.vega.model;

/**
 * Source of a {@code Thought}: which input stream gave birth to it.
 * <p>
 * Fixed at thought creation and drives tool access (see {@code IntelActionAccessPolicy} for game tools and
 * {@code SystemFunction.availableFor} for system functions): COMMANDER thoughts may run actions/macros, while
 * EVENT thoughts (a subscriber's reaction to a game event) get no game tools - the subscriber already
 * calculated and filtered the data, so they only phrase it via {@code speak}.
 */
public enum ThoughtSource {
    /** Thought born from a commander voice/text input. */
    COMMANDER,
    /** Thought born from a gameplay subscriber's reaction to a game event (phrasing/voicing only, no game tools). */
    EVENT
}
