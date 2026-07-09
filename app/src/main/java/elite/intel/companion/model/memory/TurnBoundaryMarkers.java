package elite.intel.companion.model.memory;

/**
 * Single owner of the turn-boundary marker literals. A boundary marker is a self-closing tag recorded as a
 * {@code SYSTEM} short-term entry when a commander turn produced no reply ({@link #NO_ANSWER}) or was
 * interrupted before it could reply ({@link #INTERRUPTED}). It keeps a distinct boundary between two commander
 * turns so they are not coalesced into one blurred {@code user} message.
 * <p>
 * The literals live here, not per call site: {@code CommanderThought} writes them, {@code PromptComposer}
 * detects and replays them, and {@code CommanderPrompt} explains them to the model. All three must use the
 * same strings, so they read them from this one holder (the prose in {@code CommanderPrompt} mirrors these by
 * documentation, as a Java text block cannot reference a constant).
 */
public final class TurnBoundaryMarkers {

    private TurnBoundaryMarkers() {
    }

    /** Recorded when a commander turn drew no reply (the model chose not to answer). */
    public static final String NO_ANSWER = "<no_reply/>";

    /** Recorded when a commander turn was interrupted before it could reply. */
    public static final String INTERRUPTED = "<cut_off/>";

    /** Whether {@code content} (trimmed) is one of the boundary markers. Null-safe. */
    public static boolean isBoundary(String content) {
        if (content == null) {
            return false;
        }
        String trimmed = content.strip();
        return NO_ANSWER.equals(trimmed) || INTERRUPTED.equals(trimmed);
    }
}
