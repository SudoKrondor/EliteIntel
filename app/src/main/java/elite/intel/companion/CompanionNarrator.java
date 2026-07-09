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
 *   <li>{@link #narrate} - the <em>result</em> as raw-but-digested data plus phrasing instructions; the companion
 *       compresses it through one LLM round, voices it, and remembers the exchange as a {@code user -> assistant}
 *       pair;</li>
 *   <li>{@link #announce} - the <em>result</em> as a finished phrase; voiced verbatim (no LLM) and remembered as a
 *       {@code user -> assistant} pair, where the {@code user} turn is a short {@code sourceId} (never the raw
 *       data, which could be a huge list that would bloat the prompt).</li>
 * </ul>
 * When the companion subsystem is not running, {@link CompanionRuntime#narrator()} returns {@link #NO_OP}, so a
 * subscriber can call unconditionally without guarding on companion mode.
 */
public interface CompanionNarrator {

    /** Voices a start-of-processing throwaway line; never remembered, no LLM. {@code urgent} preempts current speech. */
    void filler(String text, boolean urgent);

    /**
     * Voices the digested result data phrased by one LLM round and remembers the exchange.
     *
     * @param data         digested (not raw) event data - recorded as the {@code user} turn and phrased by the LLM
     * @param instructions how to phrase it this turn (prompt-only, never remembered)
     * @param topic        a neutral topic tag (a {@code ConversationTopic} name; blank falls back to system)
     */
    void narrate(String data, String instructions, String topic);

    /** {@link #narrate(String, String, String)} under the default system topic. */
    default void narrate(String data, String instructions) {
        narrate(data, instructions, "");
    }

    /**
     * Voices a finished phrase verbatim (no LLM) and remembers it as the companion's reply.
     *
     * @param sourceId a short source/event id recorded as the {@code user} turn (never the raw data)
     * @param phrase   the finished line to voice and remember as the {@code assistant} turn
     * @param topic    a neutral topic tag (a {@code ConversationTopic} name; blank falls back to system)
     * @param urgent   whether the line preempts current speech (e.g. a fresh radar contact)
     */
    void announce(String sourceId, String phrase, String topic, boolean urgent);

    /** No-op narrator returned when the companion subsystem is not running, so callers never need a mode guard. */
    CompanionNarrator NO_OP = new CompanionNarrator() {
        @Override public void filler(String text, boolean urgent) { }
        @Override public void narrate(String data, String instructions, String topic) { }
        @Override public void announce(String sourceId, String phrase, String topic, boolean urgent) { }
    };
}
