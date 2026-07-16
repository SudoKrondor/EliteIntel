package elite.intel.companion;

/**
 * The single door a gameplay subscriber uses to give the companion something to voice. It replaces the old
 * per-kind bridges ({@code CompanionSensorDataBridge}, {@code CompanionAnnouncementBridge}) and their intake
 * events: a subscriber that reacts to a game event calls one of these directly instead of publishing an event
 * for a bridge to forward. Reached statically via {@link CompanionRuntime#narrator()}.
 * <p>
 * Three branches, one per stage of a subscriber's reaction (see COMPANION_ARCHITECTURE.md):
 * <ul>
 *   <li>{@link #filler} - a throwaway line at the <em>start</em> of processing (an ack while work runs); voiced
 *       and immediately forgotten (never enters memory, no LLM);</li>
 *   <li>{@link #narrate} - event data plus phrasing instructions; the data exists only for that LLM round, while
 *       the successful narration is voiced and stored as the EVENT fact;</li>
 *   <li>{@link #announce} - a finished phrase voiced verbatim and stored as the EVENT fact.</li>
 * </ul>
 * When the companion subsystem is not running, {@link CompanionRuntime#narrator()} returns {@link #NO_OP}, so a
 * subscriber can call unconditionally without guarding on companion mode.
 */
public interface CompanionNarrator {

    /** Voices a start-of-processing throwaway line; never remembered, no LLM. {@code urgent} preempts current speech. */
    void filler(String text, boolean urgent);

    /**
     * Voices event data phrased by one LLM round and remembers only the successful final narration.
     *
     * @param data         event data used only in the current LLM request
     * @param instructions how to phrase it this turn
     */
    void narrate(String data, String instructions);

    /**
     * Voices a finished phrase verbatim and remembers it as the EVENT fact.
     *
     * @param phrase the finished line to voice and remember
     * @param urgent whether the line preempts current speech
     */
    void announce(String phrase, boolean urgent);

    /** No-op narrator returned when the companion subsystem is not running, so callers never need a mode guard. */
    CompanionNarrator NO_OP = new CompanionNarrator() {
        @Override public void filler(String text, boolean urgent) { }
        @Override public void narrate(String data, String instructions) { }
        @Override public void announce(String phrase, boolean urgent) { }
    };
}
