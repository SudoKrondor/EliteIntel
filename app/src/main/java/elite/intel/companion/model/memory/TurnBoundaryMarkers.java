package elite.intel.companion.model.memory;

/**
 * Single owner of the turn-boundary marker literals. A boundary marker is a self-closing tag standing in for a
 * turn's missing dialogue half, so a turn is never left as a bare {@code user} line adjacent to the next one.
 * <ul>
 *   <li>{@link #NO_ANSWER} / {@link #INTERRUPTED} / {@link #PROCESSING} - the companion's own (assistant-side)
 *       boundary, recorded as a {@code COMPANION} short-term entry when a turn drew no reply, was cut off, or
 *       detached a handler; they replay as plain {@code assistant} messages.</li>
 *   <li>{@link #CONFIRMED} - the commander-side (user) confirmation of a dangerous action, recorded as a
 *       {@code COMMANDER} entry so the executed outcome pairs with it as its own exchange.</li>
 * </ul>
 * The literals live here, not per call site: {@code CommanderThought} writes them, and {@code CommanderPrompt}
 * explains them to the model. Both must use the same strings, so they read them from this one holder (the prose
 * in {@code CommanderPrompt} mirrors these by documentation, as a Java text block cannot reference a constant).
 */
public final class TurnBoundaryMarkers {

    private TurnBoundaryMarkers() {
    }

    /** Recorded when a commander turn drew no reply (the model chose not to answer). */
    public static final String NO_ANSWER = "<no_reply/>";

    /** Recorded when a commander turn was interrupted before it could reply. */
    public static final String INTERRUPTED = "<cut_off/>";

    /** Recorded while a detached query or macro continues after its ordered cognitive turn has completed. */
    public static final String PROCESSING = "<processing/>";

    /** Recorded as the commander's user turn when a dangerous action was confirmed, so its outcome pairs with it. */
    public static final String CONFIRMED = "<confirmed/>";
}
