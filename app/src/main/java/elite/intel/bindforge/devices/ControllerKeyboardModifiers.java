package elite.intel.bindforge.devices;

import elite.intel.bindforge.model.BindingInput;

import java.util.*;

/**
 * The keyboard keys a controller input may be held with: SHIFT, CTRL and ALT, and nothing else (Krondor,
 * 2026-11-10). Any other key held with a stick button could collide with what Vega presses in ways nobody can
 * predict, so the capture dialog offers these three as checkboxes rather than capturing a key.
 * <p>
 * The dialog ticks a key; the file names a side. A tick adds the left key, which is what the game writes when a
 * player holds one, and a key already in the slot keeps its side for as long as its box stays ticked, so opening a
 * slot bound to Right Shift and saving it changes nothing.
 * <p>
 * Pure: the dialog keeps which boxes are ticked.
 */
public final class ControllerKeyboardModifiers {

    /** One of the three keys, by the tokens of its two sides. */
    public enum Key {
        SHIFT("Key_LeftShift", "Key_RightShift"),
        CTRL("Key_LeftControl", "Key_RightControl"),
        ALT("Key_LeftAlt", "Key_RightAlt");

        private final String left;
        private final String right;

        Key(String left, String right) {
            this.left = left;
            this.right = right;
        }

        boolean names(BindingInput modifier) {
            return BindingInput.KEYBOARD.equals(modifier.device())
                    && (left.equalsIgnoreCase(modifier.key()) || right.equalsIgnoreCase(modifier.key()));
        }
    }

    private ControllerKeyboardModifiers() {
    }

    /** @return which of the three keys these modifiers hold, either side */
    public static Set<Key> heldIn(List<BindingInput> modifiers) {
        EnumSet<Key> held = EnumSet.noneOf(Key.class);
        for (BindingInput modifier : modifiers) {
            for (Key key : Key.values()) {
                if (key.names(modifier)) held.add(key);
            }
        }
        return held;
    }

    /**
     * The modifiers with exactly these keys held: the controller's own modifiers untouched and first, then each
     * ticked key - on the side it already had, or the left one.
     */
    public static List<BindingInput> withKeys(List<BindingInput> modifiers, Set<Key> keys) {
        List<BindingInput> result = new ArrayList<>();
        for (BindingInput modifier : modifiers) {
            if (Arrays.stream(Key.values()).noneMatch(key -> key.names(modifier))) result.add(modifier);
        }
        for (Key key : Key.values()) {
            if (!keys.contains(key)) continue;
            BindingInput kept = modifiers.stream().filter(key::names).findFirst()
                    .orElse(new BindingInput(BindingInput.KEYBOARD, key.left));
            result.add(kept);
        }
        return List.copyOf(result);
    }
}
