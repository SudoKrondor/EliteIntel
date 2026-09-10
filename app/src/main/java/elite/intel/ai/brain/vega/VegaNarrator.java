package elite.intel.ai.brain.vega;

/**
 * The single door a gameplay subscriber uses to give VEGA something to voice. It replaces the old
 * per-kind bridges ({@code CompanionSensorDataBridge}, {@code CompanionAnnouncementBridge}) and their intake
 * events: a subscriber that reacts to a game event calls one of these directly instead of publishing an event
 * for a bridge to forward. Reached statically via {@link VegaRuntime#narrator()}.
 * <p>
 * Three branches, one per stage of a subscriber's reaction (see docs/VEGA_ARCHITECTURE.md, "Something happened in the game"):
 * <ul>
 *   <li>{@link #filler} - a throwaway line at the <em>start</em> of processing (an ack while work runs); voiced
 *       and immediately forgotten (never enters memory, no LLM);</li>
 *   <li>{@link #narrate} - event data plus phrasing instructions; the data exists only for that LLM round, while
 *       the successful narration is voiced and then forgotten (a gameplay narration is never replayed
 *       into the commander prompt, so nothing stores it);</li>
 *   <li>{@link #announce} - a finished phrase voiced verbatim, and likewise not stored.</li>
 * </ul>
 * When VEGA subsystem is not running, {@link VegaRuntime#narrator()} returns {@link #NO_OP}, so a
 * subscriber can call unconditionally without guarding on VEGA.
 */
public interface VegaNarrator {

    /**
     * Voices a start-of-processing throwaway line; never remembered, no LLM. {@code urgent} preempts current speech.
     */
    void filler(String text, boolean urgent);

    /**
     * Voices event data phrased by one LLM round and remembers only the successful final narration.
     *
     * @param data         event data used only in the current LLM request
     * @param instructions how to phrase it this turn
     */
    void narrate(String data, String instructions);

    /**
     * Voices a finished phrase verbatim. Nothing is stored.
     *
     * @param phrase the finished line to voice and remember
     * @param urgent whether the line preempts current speech
     */
    void announce(String phrase, boolean urgent);

    /**
     * No-op narrator returned when VEGA subsystem is not running, so callers never need a mode guard.
     */
    VegaNarrator NO_OP = new VegaNarrator() {
        @Override
        public void filler(String text, boolean urgent) {
        }

        @Override
        public void narrate(String data, String instructions) {
        }

        @Override
        public void announce(String phrase, boolean urgent) {
        }
    };
}
