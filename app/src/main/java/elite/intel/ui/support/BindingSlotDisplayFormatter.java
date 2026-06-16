package elite.intel.ui.support;


import elite.intel.ai.hands.BindingModifier;
import elite.intel.ai.hands.KeyBindingsParser;

import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Display-only formatter for Elite Dangerous binding slots.
 * <p>
 * It deliberately keeps raw action/device identifiers intact; this class only
 * decides how diagnostic slot values are presented in the Bindings tab.
 */
public class BindingSlotDisplayFormatter {

    public String formatSlot(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        if (isEmptySlot(slot)) {
            return getText("bindings.status.notDefined");
        }
        return formatDevice(slot) + " | " + formatBinding(slot);
    }

    public String formatBindingToken(String token) {
        if (token.startsWith("Key_") && token.length() > "Key_".length()) {
            return formatKeyboardToken(token);
        }
        if (token.startsWith("Joy_") && token.length() > "Joy_".length()) {
            return "Joystick " + token.substring("Joy_".length());
        }
        return token;
    }

    private boolean isEmptySlot(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        return isEmptyDevice(slot)
                && (slot == null || slot.key() == null || slot.key().isBlank() || "Key_".equals(slot.key()));
    }

    private String formatDevice(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        if (isEmptyDevice(slot)) {
            return "\u2014";
        }
        return isRawDeviceId(slot.device()) ? "Device " + slot.device() : slot.device();
    }

    private boolean isEmptyDevice(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        return slot == null || slot.device() == null || slot.device().isBlank() || "{NoDevice}".equals(slot.device());
    }

    private boolean isRawDeviceId(String device) {
        return device.matches("(?i)[0-9a-f]{8}");
    }

    private String formatBinding(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        if (slot == null || slot.key() == null || slot.key().isBlank() || "Key_".equals(slot.key())) {
            return getText("bindings.status.notDefined");
        }

        List<String> modifiers = slot.bindingModifiers().stream()
                .filter(modifier -> modifier.key() != null && !modifier.key().isBlank() && !"Key_".equals(modifier.key()))
                .map(this::formatModifier)
                .toList();
        String key = formatBindingToken(slot.key());
        if (modifiers.isEmpty()) {
            return key;
        }

        return String.join(" + ", modifiers) + " + " + key;
    }

    private String formatModifier(BindingModifier modifier) {
        if ("Keyboard".equals(modifier.device())) {
            return formatBindingToken(modifier.key());
        }
        String device = modifier.device() == null || modifier.device().isBlank()
                ? getText("bindings.assign.modifier.unknownDevice")
                : modifier.device();
        return device + " " + formatBindingToken(modifier.key());
    }

    private String formatKeyboardToken(String token) {
        return switch (token) {
            case "Key_LeftControl" -> "Left Ctrl";
            case "Key_RightControl" -> "Right Ctrl";
            case "Key_LeftShift" -> "Left Shift";
            case "Key_RightShift" -> "Right Shift";
            case "Key_LeftAlt" -> "Left Alt";
            case "Key_RightAlt" -> "Right Alt";
            default -> token.substring("Key_".length());
        };
    }

    /**
     * Formats a RAW_KEY step's key and optional modifier for display.
     * Accepts uppercase Elite key names (e.g. {@code "KEY_LEFTCONTROL"}, {@code "KEY_G"})
     * and returns a human-readable string (e.g. {@code "Left Ctrl + G"}).
     */
    public String formatRawKeyStep(String rawKey, String rawKeyModifier) {
        String keyLabel = rawKey != null ? formatBindingToken(toEliteKeyFormat(rawKey)) : "";
        if (rawKeyModifier != null && !rawKeyModifier.isBlank()) {
            String modLabel = formatBindingToken(toEliteKeyFormat(rawKeyModifier));
            return modLabel + " + " + keyLabel;
        }
        return keyLabel;
    }

    /**
     * Converts an uppercase key name (e.g. {@code "KEY_LEFTCONTROL"}) to the original Elite
     * {@code Key_XXX} format accepted by {@link #formatBindingToken(String)}.
     */
    public static String toEliteKeyFormat(String upperCaseName) {
        return switch (upperCaseName) {
            case "KEY_LEFTCONTROL"  -> "Key_LeftControl";
            case "KEY_RIGHTCONTROL" -> "Key_RightControl";
            case "KEY_LEFTSHIFT"    -> "Key_LeftShift";
            case "KEY_RIGHTSHIFT"   -> "Key_RightShift";
            case "KEY_LEFTALT"      -> "Key_LeftAlt";
            case "KEY_RIGHTALT"     -> "Key_RightAlt";
            default -> {
                String raw = upperCaseName.startsWith("KEY_") ? upperCaseName.substring(4) : upperCaseName;
                StringBuilder sb = new StringBuilder("Key_");
                for (String part : raw.split("_")) {
                    if (!part.isEmpty()) {
                        sb.append(Character.toUpperCase(part.charAt(0)));
                        sb.append(part.substring(1).toLowerCase());
                    }
                }
                yield sb.toString();
            }
        };
    }
}
