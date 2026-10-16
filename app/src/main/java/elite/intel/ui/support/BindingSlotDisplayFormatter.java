package elite.intel.ui.support;


import elite.intel.bindforge.devicefiles.SavedDeviceLabels;
import elite.intel.bindforge.io.KeyBindingsParser;
import elite.intel.bindforge.model.BindingModifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Display-only formatter for Elite Dangerous binding slots.
 * <p>
 * It deliberately keeps raw action/device identifiers intact; this class only
 * decides how diagnostic slot values are presented in the Bindings tab.
 * <p>
 * A controller input reads as the name the user gave it in the Device Editor ({@code 1st Hat Up}) when there is
 * one, and as its number ({@code Joystick 6}) otherwise - the way the game's own control screen shows it once
 * the labels are applied.
 */
public class BindingSlotDisplayFormatter {

    private static final Logger log = LogManager.getLogger(BindingSlotDisplayFormatter.class);

    private static final String KEYBOARD = "Keyboard";

    private final SavedDeviceLabels deviceLabels;

    /**
     * Shows controller inputs by number - for text that never names a controller input.
     */
    public BindingSlotDisplayFormatter() {
        this(SavedDeviceLabels.NONE);
    }

    public BindingSlotDisplayFormatter(SavedDeviceLabels deviceLabels) {
        this.deviceLabels = deviceLabels;
    }

    /**
     * Shows controller inputs by the names saved in the Device Editor, read now. A database that cannot be read
     * costs only the names: inputs fall back to their numbers rather than the screen failing to open.
     */
    public static BindingSlotDisplayFormatter withSavedLabels() {
        try {
            return new BindingSlotDisplayFormatter(SavedDeviceLabels.load());
        } catch (RuntimeException e) {
            log.warn("Could not read the saved controller button names; showing input numbers: {}", e.getMessage());
            return new BindingSlotDisplayFormatter();
        }
    }

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

    /**
     * The raw tokens behind a slot that shows a controller input by its given name - {@code VKB-Stick Joy_6} -
     * because support bundles, forum posts and the file itself all use the token. {@code null} when nothing on
     * the slot was renamed, so there is nothing the text hides.
     */
    public String rawTokens(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        if (isEmptySlot(slot)) {
            return null;
        }
        boolean renamed = labelOf(slot.device(), slot.key()).isPresent()
                || slot.bindingModifiers().stream().anyMatch(m -> labelOf(m.device(), m.key()).isPresent());
        if (!renamed) {
            return null;
        }
        StringBuilder raw = new StringBuilder();
        for (BindingModifier modifier : slot.bindingModifiers()) {
            if (modifier.key() == null || modifier.key().isBlank()) continue;
            raw.append(modifier.device()).append(' ').append(modifier.key()).append(" + ");
        }
        return raw.append(slot.device()).append(' ').append(slot.key()).toString();
    }

    /**
     * What the search box matches on a slot that involves a controller: the slot as shown - device, and button
     * name or number - and its raw tokens, so {@code 1st Hat Up}, {@code VKB-Stick} and {@code Joy_6} all find it.
     * Empty for a keyboard-only slot: a search for a control's name must not match every row bound to a key
     * spelled like it.
     */
    public String controllerSearchText(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        if (isEmptySlot(slot) || !involvesController(slot)) {
            return "";
        }
        String raw = rawTokens(slot);
        return formatSlot(slot) + (raw == null ? " " + slot.device() + " " + slot.key() : " " + raw);
    }

    private static boolean involvesController(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        return !KEYBOARD.equals(slot.device())
                || slot.bindingModifiers().stream().anyMatch(m -> m.device() != null && !KEYBOARD.equals(m.device()));
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

    /**
     * Formats a full keyboard chord (modifiers then main key) as a single label,
     * e.g. {@code "Left Ctrl + Left Shift + N"}. Shared by the slot readout and the
     * chord-capture dialog so both render the same way.
     */
    public String formatChord(List<BindingModifier> modifiers, String key) {
        return formatChord(KEYBOARD, modifiers, key);
    }

    /**
     * One input on a device and its modifiers as a slot reads in the grid - {@code VKB-Stick | 1st Hat Up} - for
     * a choice not yet written.
     */
    public String formatInputs(String device, List<BindingModifier> modifiers, String key) {
        return device + " | " + formatChord(device, modifiers, key);
    }

    private String formatChord(String device, List<BindingModifier> modifiers, String key) {
        List<String> modifierLabels = modifiers.stream()
                .filter(modifier -> modifier.key() != null && !modifier.key().isBlank() && !"Key_".equals(modifier.key()))
                .map(this::formatModifier)
                .toList();
        String keyLabel = formatInput(device, key);
        if (modifierLabels.isEmpty()) {
            return keyLabel;
        }
        return String.join(" + ", modifierLabels) + " + " + keyLabel;
    }

    private String formatBinding(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        if (slot == null || slot.key() == null || slot.key().isBlank() || "Key_".equals(slot.key())) {
            return getText("bindings.status.notDefined");
        }
        return formatChord(slot.device(), slot.bindingModifiers(), slot.key());
    }

    /**
     * A controller input by its given name when it has one; a key, or an unnamed input, as the token reads.
     */
    private String formatInput(String device, String token) {
        return labelOf(device, token).orElseGet(() -> formatBindingToken(token));
    }

    private Optional<String> labelOf(String device, String token) {
        if (device == null || KEYBOARD.equals(device)) {
            return java.util.Optional.empty();
        }
        return deviceLabels.label(device, token);
    }

    private String formatModifier(BindingModifier modifier) {
        if (KEYBOARD.equals(modifier.device())) {
            return formatBindingToken(modifier.key());
        }
        String device = modifier.device() == null || modifier.device().isBlank()
                ? getText("bindings.assign.modifier.unknownDevice")
                : modifier.device();
        return device + " " + formatInput(modifier.device(), modifier.key());
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
