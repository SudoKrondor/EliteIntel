package elite.intel.ai.brain.actions.customcommand;

import java.util.Map;

/**
 * One step in a user-defined customCommand sequence.
 * Gson populates fields directly; call {@link #validate(int)} after deserialization.
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
         * Delegate to an existing built-in command handler by {@code actionId}.
         * Must reference a Commands action ID, not another custom command ID.
         */
        RUN_COMMAND,
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
    private final String actionId;
    private final String rawKey;
    private final String rawKeyModifier;
    /**
     * Step-level parameter mapping for {@link Type#RUN_COMMAND} steps.
     * Maps handler param names to value templates, e.g. {@code {"lat": "${lat}", "lon": "${lon}"}}.
     * May be {@code null} for non-parameterized steps (backward-compatible).
     */
    private final Map<String, String> stepParams;

    /**
     * Creates one custom command step. Only the fields required by {@code type} are used at execution time.
     */
    public CustomCommandStep(Type type, String bindingId, int durationMs, String text, String actionId) {
        this(type, bindingId, durationMs, text, actionId, null, null, null);
    }

    /**
     * Creates a RAW_KEY step or any step that needs {@code rawKey}/{@code rawKeyModifier}.
     * For all other types, pass {@code null} for the last two parameters.
     */
    public CustomCommandStep(Type type, String bindingId, int durationMs, String text, String actionId,
                     String rawKey, String rawKeyModifier) {
        this(type, bindingId, durationMs, text, actionId, rawKey, rawKeyModifier, null);
    }

    private CustomCommandStep(Type type, String bindingId, int durationMs, String text, String actionId,
                      String rawKey, String rawKeyModifier, Map<String, String> stepParams) {
        this.type = type;
        this.bindingId = bindingId;
        this.durationMs = durationMs;
        this.text = text;
        this.actionId = actionId;
        this.rawKey = rawKey;
        this.rawKeyModifier = rawKeyModifier;
        this.stepParams = stepParams == null ? null : Map.copyOf(stepParams);
    }

    /**
     * Creates a {@link Type#RUN_COMMAND} step with a step-level param mapping for customCommand param substitution.
     *
     * @param actionId   the built-in command action ID to invoke
     * @param stepParams mapping of handler param names to value templates (e.g. {@code {"key": "${speed}"}})
     */
    public static CustomCommandStep runCommandWithParams(String actionId, Map<String, String> stepParams) {
        return new CustomCommandStep(Type.RUN_COMMAND, null, 0, null, actionId, null, null,
                stepParams == null ? null : Map.copyOf(stepParams));
    }

    @SuppressWarnings("unused")
    private CustomCommandStep() {
        type = null;
        bindingId = null;
        durationMs = 0;
        text = null;
        actionId = null;
        rawKey = null;
        rawKeyModifier = null;
        stepParams = null;
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
            case RUN_COMMAND ->
                require(actionId != null && !actionId.isBlank(), stepIndex, "actionId");
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
    public String getActionId() { return actionId; }
    public String getRawKey() { return rawKey; }
    public String getRawKeyModifier() { return rawKeyModifier; }
    /** Returns the step-level param mapping for RUN_COMMAND steps. Empty map if not set. */
    public Map<String, String> getStepParams() { return stepParams != null ? stepParams : Map.of(); }
}
