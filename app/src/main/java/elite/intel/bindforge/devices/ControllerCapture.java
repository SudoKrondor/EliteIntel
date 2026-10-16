package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.ButtonMapLabels;
import elite.intel.bindforge.io.SlotAssignment;
import elite.intel.bindforge.model.BindingInput;
import elite.intel.devices.events.DeviceHatEvent;

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

    private final Set<BindingInput> held = new LinkedHashSet<>();
    private final Map<String, Integer> hatDirections = new HashMap<>();

    /**
     * @param device   the controller's {@code .binds} name
     * @param sdlIndex SDL's button index, from 0
     * @return what to assign when this was a press; empty on a release
     */
    public Optional<Captured> button(String device, int sdlIndex, boolean pressed) {
        BindingInput input = new BindingInput(device, ButtonMapLabels.buttonToken(sdlIndex + 1));
        if (!pressed) {
            held.remove(input);
            return Optional.empty();
        }
        return Optional.of(press(input));
    }

    /**
     * @param sdlIndex   SDL's hat index, from 0
     * @param directions the directions pushed now, as SDL's bits
     * @return what to assign when a direction was newly pushed; empty when one was only let go
     */
    public Optional<Captured> hat(String device, int sdlIndex, int directions) {
        String key = device + "#" + sdlIndex;
        int before = hatDirections.getOrDefault(key, 0);
        hatDirections.put(key, directions);
        List<String> tokens = DeviceEditorInputs.hatTokens(sdlIndex + 1);
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
     * Forgets what one controller holds - it was unplugged, so no release will come.
     */
    public void release(String device) {
        held.removeIf(input -> input.device().equals(device));
        hatDirections.keySet().removeIf(key -> key.startsWith(device + "#"));
    }

    private Captured press(BindingInput input) {
        List<BindingInput> modifiers = new ArrayList<>();
        for (BindingInput other : held) {
            if (modifiers.size() == SlotAssignment.MAX_MODIFIERS) break;
            if (!other.sameInputAs(input)) modifiers.add(other);
        }
        held.add(input);
        return new Captured(input, modifiers);
    }
}
