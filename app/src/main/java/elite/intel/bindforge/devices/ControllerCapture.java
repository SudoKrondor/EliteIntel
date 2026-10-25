package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.ButtonMapLabels;
import elite.intel.bindforge.io.SlotAssignment;
import elite.intel.bindforge.model.BindingInput;
import elite.intel.devices.events.DeviceHatEvent;
import elite.intel.devices.model.GameAxis;

import java.util.*;

/**
 * Turns controller presses into what the capture dialog assigns - an input and the modifiers held for it.
 * <p>
 * <strong>Capture is a single motion, as in the game</strong> (bind-editor.md, <em>Modifiers, Hold, Inverted,
 * Deadzone</em>): whatever is held when the completing input arrives becomes the modifier set, up to three, and the
 * completing input is the slot's value. Held inputs count on any controller, in the order they were pressed.
 * <p>
 * A hat direction is an input like a button. A diagonal on an 8-way hat pushes two directions in one report:
 * the first in the spec's order (up, right, down, left) is captured, and the other counts as held.
 * <p>
 * Pure and not thread-safe: the dialog feeds it on the EDT.
 */
public final class ControllerCapture {

    /**
     * An input to assign and the modifiers held for it, in the {@code .binds} vocabulary.
     */
    public record Captured(BindingInput input, List<BindingInput> modifiers) {
        public Captured {
            modifiers = List.copyOf(modifiers);
        }
    }

    private static final int[] HAT_BITS = {DeviceHatEvent.UP, DeviceHatEvent.RIGHT, DeviceHatEvent.DOWN, DeviceHatEvent.LEFT};
    private static final float AXIS_ACTIVATION_THRESHOLD = 0.25f;

    /** Elite's tokens for the GamePad's triggers; SDL reports them as the Z and RZ axes. */
    public static final String GAMEPAD_LEFT_TRIGGER = "Pos_GamePad_LTrigger";
    public static final String GAMEPAD_RIGHT_TRIGGER = "Pos_GamePad_RTrigger";
    private static final Map<GameAxis, String> GAMEPAD_TRIGGERS =
            Map.of(GameAxis.Z, GAMEPAD_LEFT_TRIGGER, GameAxis.RZ, GAMEPAD_RIGHT_TRIGGER);

    /** In the order of {@link #HAT_BITS}. */
    private static final List<String> GAMEPAD_DPAD_TOKENS =
            List.of("GamePad_DPadUp", "GamePad_DPadRight", "GamePad_DPadDown", "GamePad_DPadLeft");

    private final Set<BindingInput> held = new LinkedHashSet<>();
    private final Map<String, Integer> hatDirections = new HashMap<>();
    private final Map<String, Integer> axisDirections = new HashMap<>();

    /**
     * @param device  the controller's {@code .binds} name
     * @param token   the button's token, from {@link #buttonToken}
     * @return what to assign when this was a press; empty on a release
     */
    public Optional<Captured> button(String device, String token, boolean pressed) {
        BindingInput input = new BindingInput(device, token);
        if (!pressed) {
            held.remove(input);
            return Optional.empty();
        }
        return Optional.of(press(input));
    }

    /**
     * @param sdlIndex SDL's button index, from 0
     * @return the token the game writes for this button; empty for a gamepad's Start and Guide, which the game
     * does not let a binding use
     */
    public static Optional<String> buttonToken(boolean gamepad, int sdlIndex) {
        return gamepad ? gamepadButtonToken(sdlIndex) : Optional.of(ButtonMapLabels.buttonToken(sdlIndex + 1));
    }

    /**
     * SDL's button order for an Xbox-style pad, written with Elite's GamePad tokens.
     */
    static Optional<String> gamepadButtonToken(int sdlIndex) {
        return switch (sdlIndex) {
            case 0 -> Optional.of("GamePad_FaceDown");
            case 1 -> Optional.of("GamePad_FaceRight");
            case 2 -> Optional.of("GamePad_FaceLeft");
            case 3 -> Optional.of("GamePad_FaceUp");
            case 4 -> Optional.of("GamePad_LBumper");
            case 5 -> Optional.of("GamePad_RBumper");
            case 6 -> Optional.of("GamePad_Back");
            case 7, 10 -> Optional.empty();
            case 8 -> Optional.of("GamePad_LThumb");
            case 9 -> Optional.of("GamePad_RThumb");
            // WHY: a pad's extra buttons (paddles, share) have no GamePad token in Elite, so they bind as plain buttons.
            default -> Optional.of(ButtonMapLabels.buttonToken(sdlIndex + 1));
        };
    }

    /**
     * @param sdlIndex   SDL's hat index, from 0
     * @param directions the directions pushed now, as SDL's bits
     * @return what to assign when a direction was newly pushed; empty when one was only let go
     */
    public Optional<Captured> hat(String device, int sdlIndex, int directions) {
        return hat(device, sdlIndex, directions, DeviceEditorInputs.hatTokens(sdlIndex + 1));
    }

    /**
     * Captures a gamepad D-pad using Elite's GamePad tokens instead of the generic POV vocabulary.
     */
    public Optional<Captured> gamepadHat(String device, int sdlIndex, int directions) {
        return hat(device, sdlIndex, directions, GAMEPAD_DPAD_TOKENS);
    }

    public static List<String> gamepadDPadTokens() {
        return GAMEPAD_DPAD_TOKENS;
    }

    private Optional<Captured> hat(String device, int sdlIndex, int directions, List<String> tokens) {
        String key = device + "#" + sdlIndex;
        int before = hatDirections.getOrDefault(key, 0);
        hatDirections.put(key, directions);
        Captured captured = null;
        for (int i = 0; i < HAT_BITS.length; i++) {
            BindingInput input = new BindingInput(device, tokens.get(i));
            boolean was = (before & HAT_BITS[i]) != 0;
            boolean is = (directions & HAT_BITS[i]) != 0;
            if (was && !is) held.remove(input);
            if (!was && is) {
                if (captured == null) captured = press(input);
                else held.add(input);
            }
        }
        return Optional.ofNullable(captured);
    }

    /**
     * @param axis the game's axis slot behind the SDL axis
     * @param value the current normalized value
     * @return what to assign when the axis moves far enough; empty while centred or held in the same direction
     */
    public Optional<Captured> axis(String device, GameAxis axis, float value) {
        Objects.requireNonNull(axis, "axis");
        String key = device + "#" + axis.token();
        int direction = Math.abs(value) < AXIS_ACTIVATION_THRESHOLD ? 0 : value < 0 ? -1 : 1;
        int before = axisDirections.getOrDefault(key, 0);
        axisDirections.put(key, direction);
        if (direction == before) return Optional.empty();
        BindingInput negative = new BindingInput(device, "Neg_" + axis.token());
        BindingInput positive = new BindingInput(device, "Pos_" + axis.token());
        forget(negative);
        forget(positive);
        if (direction == 0) return Optional.empty();
        return Optional.of(press(direction < 0 ? negative : positive));
    }

    /**
     * Like {@link #axis}, but the GamePad's triggers are captured under their own tokens, one direction each.
     */
    public Optional<Captured> gamepadAxis(String device, GameAxis axis, float value) {
        Optional<String> trigger = gamepadTriggerToken(axis);
        return trigger.isPresent() ? trigger(device, trigger.get(), value) : axis(device, axis, value);
    }

    /** @return Elite's token for the GamePad trigger that SDL reports as this axis; empty for any other axis */
    public static Optional<String> gamepadTriggerToken(GameAxis axis) {
        return Optional.ofNullable(GAMEPAD_TRIGGERS.get(axis));
    }

    /** @return the SDL axis behind a GamePad trigger token; empty for any other token */
    public static Optional<GameAxis> gamepadTriggerAxis(String token) {
        return GAMEPAD_TRIGGERS.entrySet().stream()
                .filter(entry -> entry.getValue().equals(token))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private Optional<Captured> trigger(String device, String token, float value) {
        String key = device + "#" + token;
        // WHY: a trigger rests at -1 and is pulled towards +1, so only the positive side counts as pressed.
        boolean pressed = value >= AXIS_ACTIVATION_THRESHOLD;
        boolean before = axisDirections.getOrDefault(key, 0) != 0;
        axisDirections.put(key, pressed ? 1 : 0);
        if (pressed == before) return Optional.empty();
        BindingInput input = new BindingInput(device, token);
        forget(input);
        return pressed ? Optional.of(press(input)) : Optional.empty();
    }

    private void forget(BindingInput target) {
        held.removeIf(input -> input.sameInputAs(target));
    }

    /**
     * Forgets what one controller holds - it was unplugged, so no release will come.
     */
    public void release(String device) {
        held.removeIf(input -> input.device().equals(device));
        hatDirections.keySet().removeIf(key -> key.startsWith(device + "#"));
        axisDirections.keySet().removeIf(key -> key.startsWith(device + "#"));
    }

    private Captured press(BindingInput input) {
        List<BindingInput> modifiers = new ArrayList<>();
        for (BindingInput other : held) {
            if (modifiers.size() == SlotAssignment.MAX_MODIFIERS) break;
            if (isAxisInput(other)) continue;
            if (!other.sameInputAs(input)) modifiers.add(other);
        }
        held.add(input);
        return new Captured(input, modifiers);
    }

    private static boolean isAxisInput(BindingInput input) {
        String key = input.key();
        return key.startsWith("Pos_Joy_") && key.endsWith("Axis")
                || key.startsWith("Neg_Joy_") && key.endsWith("Axis")
                || GAMEPAD_TRIGGERS.containsValue(key);
    }
}
