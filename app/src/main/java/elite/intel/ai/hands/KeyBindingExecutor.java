package elite.intel.ai.hands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.*;

/**
 * The KeyBindingExecutor class provides functionality to manage and execute key bindings
 * by interfacing with a KeyProcessor. It handles mapping keys from external naming conventions
 * to internal representations, processes modifiers, and manages key press sequences
 * including press and hold actions.
 */
public class KeyBindingExecutor {
    private static final Logger log = LogManager.getLogger(KeyBindingExecutor.class);
    private final KeyProcessor keyProcessor;
    private static final Map<String, Integer> ELITE_TO_KEYPROCESSOR_MAP = new HashMap<>();

    private static KeyBindingExecutor instance;

    private KeyBindingExecutor() {
        this.keyProcessor = KeyProcessor.getInstance();
    }

    public static synchronized KeyBindingExecutor getInstance() {
        if (instance == null) {
            instance = new KeyBindingExecutor();
        }
        return instance;
    }

    static {
        try {
            for (Field field : KeyProcessor.class.getDeclaredFields()) {
                if (field.getName().startsWith("KEY_") && field.getType() == int.class) {
                    String eliteKeyName = convertToEliteKeyName(field.getName());
                    ELITE_TO_KEYPROCESSOR_MAP.put(eliteKeyName.toUpperCase(), field.getInt(null));
                }
            }
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_APPS", KeyProcessor.KEY_MENU);
            // Elite uses "Key_Grave" for the backtick/grave key; KeyProcessor defines it as KEY_GRAVEACCENT
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_GRAVE", KeyProcessor.KEY_GRAVEACCENT);
            // On UK (ISO) keyboards the # key is a dedicated physical key at PS/2 scan 0x2B
            // the same hardware position as the US \| key. Elite records it as "Key_Hash", but
            // VK_BACK_SLASH is the Java/Windows VK code that maps to scan 0x2B, so we override
            // the auto-reflected KEY_HASH→VK_3 (which is the US "Shift+3 = #" mapping and wrong).
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_HASH", KeyProcessor.KEY_BACKSLASH);

            // German QWERTZ / EU keyboard keys. Elite uses the actual Unicode character in the key name.
            // Each maps to a dedicated NATIVE_BASE code so both Windows (scan code) and Linux (keysym) work correctly.
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_Ä", KeyProcessor.KEY_ADIAERESIS); // ä → scan 0x28 / XK_adiaeresis
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_Ö", KeyProcessor.KEY_ODIAERESIS); // ö → scan 0x27 / XK_odiaeresis
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_Ü", KeyProcessor.KEY_UDIAERESIS); // ü → scan 0x1A / XK_udiaeresis
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_SS", KeyProcessor.KEY_SSHARP);     // ß → scan 0x0C / XK_ssharp (Java toUpperCase → "SS")
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_ẞ", KeyProcessor.KEY_SSHARP);     // ẞ → scan 0x0C / XK_ssharp (capital sharp S fallback)
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_ACUTE", KeyProcessor.KEY_DEAD_ACUTE); // ´ → scan 0x0D / XK_dead_acute
            // "Key_LessThan" is the ISO 102nd key (<> on DE/EU keyboards). Auto-reflection maps it
            // to KEY_LESSTHAN via field name, but "KEY_LESS" would not match. Added explicitly for clarity.
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_LESSTHAN", KeyProcessor.KEY_LESSTHAN);
            // Elite uses "Key_Numpad_Enter"; our field is KEY_NUMENTER → auto-reflection produces "KEY_NUMENTER"
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_NUMPAD_ENTER", KeyProcessor.KEY_NUMENTER);
            // French AZERTY accented keys. Elite serialises as e.g. "Key_é" (lowercase Unicode).
            // toUpperCase() on lookup produces "KEY_É" which the reflection loop cannot auto-map
            // (field name is ASCII "KEY_EACUTE"), so explicit entries are required.
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_É", KeyProcessor.KEY_EACUTE);   // é → NATIVE_BASE+17
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_È", KeyProcessor.KEY_EGRAVE);   // è → NATIVE_BASE+18
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_À", KeyProcessor.KEY_AGRAVE);   // à → NATIVE_BASE+19
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_Ù", KeyProcessor.KEY_UGRAVE);   // ù → NATIVE_BASE+20
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_Ç", KeyProcessor.KEY_CCEDILLA); // ç → NATIVE_BASE+21
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_Ñ", KeyProcessor.KEY_NTILDE);   // ñ → NATIVE_BASE+22
            // Dump the full map so we can verify key names at startup
            log.debug("[key-map] ELITE_TO_KEYPROCESSOR_MAP: {} entries", ELITE_TO_KEYPROCESSOR_MAP.size());
            ELITE_TO_KEYPROCESSOR_MAP.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .forEach(e -> log.debug("[key-map]   '{}' → 0x{}", e.getKey(), Integer.toHexString(e.getValue())));

        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to initialize key mappings", e);
        }
    }

    private static String convertToEliteKeyName(String fieldName) {
        String keyName = fieldName.substring(4);
        if (keyName.startsWith("NUM_")) {
            keyName = "Numpad_" + keyName.substring(4);
        }
        StringBuilder eliteKey = new StringBuilder("KEY_");

        for (char c : keyName.toCharArray()) {
            if (c == '_') {
                eliteKey.append(c);
            } else {
                eliteKey.append(Character.toUpperCase(c));
            }
        }
        return eliteKey.toString().toUpperCase();
    }

    /**
     * Resolves an Elite key name (e.g. {@code "KEY_W"} or {@code "Key_LeftControl"}) to a
     * {@link KeyProcessor} key code. Returns {@code null} if the key name is unknown.
     * The lookup is case-insensitive.
     */
    public static Integer resolveKeyCode(String eliteKeyName) {
        if (eliteKeyName == null || eliteKeyName.isBlank()) {
            return null;
        }
        return ELITE_TO_KEYPROCESSOR_MAP.get(eliteKeyName.toUpperCase());
    }

    /**
     * Returns all known Elite key names (uppercase, e.g. {@code "KEY_W"}).
     * Useful for populating raw-key picker UIs.
     */
    public static Set<String> knownEliteKeyNames() {
        return Collections.unmodifiableSet(ELITE_TO_KEYPROCESSOR_MAP.keySet());
    }

    public void executeBinding(KeyBindingsParser.KeyBinding binding) {
        executeBindingWithHold(binding, 0); // Default: no hold
    }

    /**
     * Executes a binding as a guaranteed single tap regardless of the binding's
     * hold flag. Use for UI navigation where holding would cause key-repeat.
     */
    public void executeTap(KeyBindingsParser.KeyBinding binding) {
        try {
            log.debug("[exec] executeTap key='{}' modifiers={}", binding.key, java.util.Arrays.toString(binding.modifiers));
            Integer mainKeyCode = ELITE_TO_KEYPROCESSOR_MAP.get(binding.key.toUpperCase());
            if (mainKeyCode == null) {
                log.error("[exec] UNKNOWN KEY '{}' (uppercase='{}') — not in ELITE_TO_KEYPROCESSOR_MAP",
                        binding.key, binding.key.toUpperCase());
                log.error("[exec] Known keys: {}",
                        ELITE_TO_KEYPROCESSOR_MAP.keySet().stream().sorted().toList());
                return;
            }
            log.debug("[exec] key '{}' → keyCode=0x{}", binding.key.toUpperCase(), Integer.toHexString(mainKeyCode));
            int[] modifierCodes = new int[binding.modifiers.length];
            for (int i = 0; i < binding.modifiers.length; i++) {
                Integer modCode = ELITE_TO_KEYPROCESSOR_MAP.get(binding.modifiers[i].toUpperCase());
                if (modCode == null) {
                    log.error("[exec] UNKNOWN MODIFIER '{}' (uppercase='{}') — not in ELITE_TO_KEYPROCESSOR_MAP",
                            binding.modifiers[i], binding.modifiers[i].toUpperCase());
                    return;
                }
                modifierCodes[i] = modCode;
            }
            for (int modCode : modifierCodes) {
                keyProcessor.holdKey(modCode);
            }
            // Always pressKey - never hold, regardless of binding.hold flag
            keyProcessor.pressKey(mainKeyCode);
            log.debug("Executed tap binding: key={}, modifiers={}", binding.key, binding.modifiers);
            for (int i = modifierCodes.length - 1; i >= 0; i--) {
                keyProcessor.releaseKey(modifierCodes[i]);
            }
        } catch (Exception e) {
            log.error("Error executing tap binding: {}", e.getMessage());
        }
    }

    public void executeBindingWithHold(KeyBindingsParser.KeyBinding binding, int holdTimeMs) {
        try {
            log.debug("[exec] executeBindingWithHold key='{}' modifiers={} hold={}ms",
                    binding.key, java.util.Arrays.toString(binding.modifiers), holdTimeMs);
            Integer mainKeyCode = ELITE_TO_KEYPROCESSOR_MAP.get(binding.key.toUpperCase());
            if (mainKeyCode == null) {
                log.error("[exec] UNKNOWN KEY '{}' (uppercase='{}') — not in ELITE_TO_KEYPROCESSOR_MAP",
                        binding.key, binding.key.toUpperCase());
                log.error("[exec] Known keys: {}",
                        ELITE_TO_KEYPROCESSOR_MAP.keySet().stream().sorted().toList());
                return;
            }
            log.debug("[exec] key '{}' → keyCode=0x{}", binding.key.toUpperCase(), Integer.toHexString(mainKeyCode));
            int[] modifierCodes = new int[binding.modifiers.length];
            for (int i = 0; i < binding.modifiers.length; i++) {
                Integer modCode = ELITE_TO_KEYPROCESSOR_MAP.get(binding.modifiers[i].toUpperCase());
                if (modCode == null) {
                    log.error("[exec] UNKNOWN MODIFIER '{}' (uppercase='{}') — not in ELITE_TO_KEYPROCESSOR_MAP",
                            binding.modifiers[i], binding.modifiers[i].toUpperCase());
                    return;
                }
                log.debug("[exec] modifier '{}' → keyCode=0x{}", binding.modifiers[i].toUpperCase(), Integer.toHexString(modCode));
                modifierCodes[i] = modCode;
            }

            // Press modifiers
            for (int modCode : modifierCodes) {
                keyProcessor.holdKey(modCode);
            }

            // Execute main key
            if (holdTimeMs > 0) {
                keyProcessor.pressAndHoldKey(mainKeyCode, holdTimeMs);
                log.debug("Executed hold binding: key={}, modifiers={}, holdTimeMs={}", binding.key, binding.modifiers, holdTimeMs);
            } else if (binding.hold) {
                keyProcessor.holdKey(mainKeyCode);
                Thread.sleep(500);
                keyProcessor.releaseKey(mainKeyCode);
                log.debug("Executed hold binding: key={}, modifiers={}", binding.key, binding.modifiers);
            } else {
                keyProcessor.pressKey(mainKeyCode);
                log.debug("Executed press binding: key={}, modifiers={}", binding.key, binding.modifiers);
            }

            // Release modifiers in reverse order
            for (int i = modifierCodes.length - 1; i >= 0; i--) {
                keyProcessor.releaseKey(modifierCodes[i]);
            }
        } catch (Exception e) {
            // Ensure all keys are released on error
            for (int modCode : ELITE_TO_KEYPROCESSOR_MAP.values()) {
                keyProcessor.releaseKey(modCode);
            }
            keyProcessor.releaseKey(ELITE_TO_KEYPROCESSOR_MAP.getOrDefault(binding.key, 0));
            log.error("Error executing key binding: {}", e.getMessage());
        }
    }
}