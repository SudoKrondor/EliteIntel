package elite.intel.ai.hands;

import java.util.List;
import java.util.Set;

/**
 * Raw Elite Dangerous modifier assignment from a binding slot plus the V1
 * keyboard modifier allow-list.
 */
public record BindingModifier(String device, String key) {
    private static final List<BindingModifier> SUPPORTED_KEYBOARD_MODIFIERS = List.of(
            new BindingModifier("Keyboard", "Key_LeftControl"),
            new BindingModifier("Keyboard", "Key_RightControl"),
            new BindingModifier("Keyboard", "Key_LeftShift"),
            new BindingModifier("Keyboard", "Key_RightShift"),
            new BindingModifier("Keyboard", "Key_LeftAlt"),
            new BindingModifier("Keyboard", "Key_RightAlt")
    );
    private static final Set<String> SUPPORTED_KEYBOARD_MODIFIER_KEYS = Set.of(
            "Key_LeftControl",
            "Key_RightControl",
            "Key_LeftShift",
            "Key_RightShift",
            "Key_LeftAlt",
            "Key_RightAlt"
    );

    public boolean isSupportedKeyboardModifier() {
        return isSupportedKeyboardModifier(device, key);
    }

    public static boolean isSupportedKeyboardModifier(String device, String key) {
        return "Keyboard".equals(device) && SUPPORTED_KEYBOARD_MODIFIER_KEYS.contains(key);
    }

    /**
     * Returns V1-supported keyboard modifiers in stable GUI display order.
     */
    public static List<BindingModifier> supportedKeyboardModifiers() {
        return SUPPORTED_KEYBOARD_MODIFIERS;
    }
}
