package elite.intel.companion.confirm;

/**
 * Signals that the commander has confirmed a frozen dangerous action, delivered on a dedicated
 * confirmation bus. It only has effect if there is a current thought in {@code awaiting_confirmation};
 * the LLM does not participate in confirmation (see COMPANION_ARCHITECTURE.md §2.13).
 */
public final class DangerousActionConfirmedEvent {

    /** Where the confirmation came from. */
    public enum Source {
        /** STT code word from settings. */
        VOICE_CODE_WORD,
        /** Input module key/button. */
        INPUT_BUTTON
    }

    private final Source source;

    public DangerousActionConfirmedEvent(Source source) {
        this.source = source;
    }

    public Source getSource() {
        return source;
    }
}
