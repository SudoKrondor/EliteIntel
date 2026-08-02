package elite.intel.ai.brain.actions.handlers.commands.custom;

/**
 * One step in a user-defined customCommand sequence.
 * Gson populates fields directly; call {@link #validate(int)} after deserialization.
 * <p>
 * A custom command is a keystroke sequence and nothing more: it drives game bindings and raw keys the
 * way a VoiceAttack profile would, alongside the built-in actions rather than wrapping them. Anything
 * that needs to consult a data source or touch persistence is a built-in handler by definition, so no
 * step type delegates to one.
 */
public final class CustomCommandStep {

    public enum Type {
        /** Tap an Elite Dangerous game binding once. Requires {@code bindingId}. */
        BINDING_TAP,
        /** Hold an Elite Dangerous game binding for {@code durationMs} milliseconds. Requires {@code bindingId} and {@code durationMs}. */
        BINDING_HOLD,
        /** Pause execution for {@code durationMs} milliseconds. */
        DELAY,
        /** Speak {@code text} via TTS (publishes {@code AiVoxResponseEvent}). */
        SPEAK,
        /**
         * Press an arbitrary raw key with an optional modifier and optional hold duration.
         * Requires {@code rawKey} (uppercase Elite key name, e.g. {@code "KEY_W"}).
         * {@code rawKeyModifier} is optional (e.g. {@code "KEY_LEFTCONTROL"}); {@code durationMs} is 0 for a tap.
         */
        RAW_KEY
    }

    private final Type type;
    private final String bindingId;
    private final int durationMs;
    private final String text;
    private final String rawKey;
    private final String rawKeyModifier;

    /**
     * Creates one custom command step. Only the fields required by {@code type} are used at execution time.
     */
    public CustomCommandStep(Type type, String bindingId, int durationMs, String text) {
        this(type, bindingId, durationMs, text, null, null);
    }

    /**
     * Creates a RAW_KEY step or any step that needs {@code rawKey}/{@code rawKeyModifier}.
     * For all other types, pass {@code null} for the last two parameters.
     */
    public CustomCommandStep(Type type, String bindingId, int durationMs, String text,
                     String rawKey, String rawKeyModifier) {
        this.type = type;
        this.bindingId = bindingId;
        this.durationMs = durationMs;
        this.text = text;
        this.rawKey = rawKey;
        this.rawKeyModifier = rawKeyModifier;
    }

    @SuppressWarnings("unused")
    private CustomCommandStep() {
        type = null;
        bindingId = null;
        durationMs = 0;
        text = null;
        rawKey = null;
        rawKeyModifier = null;
    }

    /** Validates required fields for this step's type. */
    public void validate(int stepIndex) {
        if (type == null) {
            throw new IllegalArgumentException("CustomCommandStep[" + stepIndex + "]: type is null");
        }
        switch (type) {
            case BINDING_TAP ->
                require(bindingId != null && !bindingId.isBlank(), stepIndex, "bindingId");
            case BINDING_HOLD -> {
                require(bindingId != null && !bindingId.isBlank(), stepIndex, "bindingId");
                require(durationMs >= 0, stepIndex, "durationMs");
            }
            case DELAY ->
                require(durationMs >= 0, stepIndex, "durationMs");
            case SPEAK ->
                require(text != null && !text.isBlank(), stepIndex, "text");
            case RAW_KEY ->
                require(rawKey != null && !rawKey.isBlank(), stepIndex, "rawKey");
        }
    }

    private static void require(boolean ok, int idx, String field) {
        if (!ok) {
            throw new IllegalArgumentException("CustomCommandStep[" + idx + "]: " + field + " is missing or invalid");
        }
    }

    public Type getType() { return type; }
    public String getBindingId() { return bindingId; }
    public int getDurationMs() { return durationMs; }
    public String getText() { return text; }
    public String getRawKey() { return rawKey; }
    public String getRawKeyModifier() { return rawKeyModifier; }
}
