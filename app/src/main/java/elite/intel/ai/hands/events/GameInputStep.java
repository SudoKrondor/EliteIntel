package elite.intel.ai.hands.events;

import java.util.Objects;

/**
 * One semantic game input step inside a {@link GameInputSequenceEvent}.
 */
public final class GameInputStep {

    public enum Type {
        BINDING_TAP,
        BINDING_FORCED_TAP,
        BINDING_HOLD,
        BINDING_DOWN,
        BINDING_UP,
        RAW_KEY,
        TEXT,
        DELAY
    }

    private final Type type;
    private final String bindingId;
    private final int keyCode;
    /** KeyProcessor code of the modifier key held during RAW_KEY execution; 0 means no modifier. */
    private final int modifierKeyCode;
    private final String text;
    private final int durationMs;

    private GameInputStep(Type type, String bindingId, int keyCode, String text, int durationMs, int modifierKeyCode) {
        this.type = Objects.requireNonNull(type, "type");
        this.bindingId = bindingId;
        this.keyCode = keyCode;
        this.modifierKeyCode = modifierKeyCode;
        this.text = text;
        this.durationMs = durationMs;
    }

    /**
     * Presses an Elite Dangerous binding the way the commander's {@code .binds} file configures it: a short
     * tap normally, or a press-and-hold when that binding carries {@code <Hold Value="1"/>}.
     * <p>
     * This is the right step for game actions, because the game decides which of them need a long press.
     * Use {@link #bindingForcedTap(String)} only when the caller's own contract is a tap regardless of the
     * file, and {@link #bindingHold(String, int)} when the caller owns the duration.
     */
    public static GameInputStep bindingTap(String bindingId) {
        return new GameInputStep(Type.BINDING_TAP, requireBindingId(bindingId), 0, null, 0, 0);
    }

    /**
     * Taps an Elite Dangerous binding, ignoring any {@code <Hold Value="1"/>} flag on it.
     * <p>
     * For the custom-command editor's <em>Binding Tap</em> step, where the commander chose a tap over the
     * neighbouring <em>Binding Hold</em> step and must get one whatever their file says.
     */
    public static GameInputStep bindingForcedTap(String bindingId) {
        return new GameInputStep(Type.BINDING_FORCED_TAP, requireBindingId(bindingId), 0, null, 0, 0);
    }

    /**
     * Holds an Elite Dangerous binding for the requested duration in milliseconds.
     */
    public static GameInputStep bindingHold(String bindingId, int holdMs) {
        return new GameInputStep(Type.BINDING_HOLD, requireBindingId(bindingId), 0, null, requireNonNegative(holdMs, "holdMs"), 0);
    }

    /**
     * Presses an Elite Dangerous binding down and leaves it held. Must be paired with a later
     * {@link #bindingUp(String)} for the same binding, otherwise the key stays stuck down.
     * Use when the release moment is decided by an external signal rather than a fixed duration
     * (e.g. holding the discovery-scanner trigger until the scan completes).
     */
    public static GameInputStep bindingDown(String bindingId) {
        return new GameInputStep(Type.BINDING_DOWN, requireBindingId(bindingId), 0, null, 0, 0);
    }

    /**
     * Releases an Elite Dangerous binding previously held by {@link #bindingDown(String)}.
     */
    public static GameInputStep bindingUp(String bindingId) {
        return new GameInputStep(Type.BINDING_UP, requireBindingId(bindingId), 0, null, 0, 0);
    }


    /**
     * Presses a raw physical key with an optional modifier held and an optional hold duration.
     *
     * @param keyCode         KeyProcessor code of the main key
     * @param modifierKeyCode KeyProcessor code of the modifier to hold, or 0 for none
     * @param holdMs          how long to hold the main key in milliseconds, or 0 for a tap
     */
    public static GameInputStep rawKey(int keyCode, int modifierKeyCode, int holdMs) {
        return new GameInputStep(Type.RAW_KEY, null, keyCode, null, requireNonNegative(holdMs, "holdMs"), modifierKeyCode);
    }

    /**
     * Enters text through the low-level key processor.
     */
    public static GameInputStep text(String text) {
        return new GameInputStep(Type.TEXT, null, 0, Objects.requireNonNull(text, "text"), 0, 0);
    }

    /**
     * Adds an explicit sequence delay. The executor's default post-input delay is not applied to this step.
     */
    public static GameInputStep delay(int delayMs) {
        return new GameInputStep(Type.DELAY, null, 0, null, requireNonNegative(delayMs, "delayMs"), 0);
    }

    public Type getType() {
        return type;
    }

    public String getBindingId() {
        return bindingId;
    }

    public int getKeyCode() {
        return keyCode;
    }

    /** Returns the KeyProcessor code of the modifier key, or 0 if no modifier is set. */
    public int getModifierKeyCode() {
        return modifierKeyCode;
    }

    public String getText() {
        return text;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public boolean isInputProducing() {
        return type != Type.DELAY;
    }

    private static String requireBindingId(String bindingId) {
        if (bindingId == null || bindingId.isBlank()) {
            throw new IllegalArgumentException("bindingId must not be blank");
        }
        return bindingId;
    }

    private static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }
}
