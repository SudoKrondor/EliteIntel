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

    /**
     * How long to hold a binding whose .binds slot carries {@code <Hold Value="1"/>}.
     */
    private static final int CONFIGURED_HOLD_MS = 500;

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
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_NUMPAD_DIVIDE", KeyProcessor.NATIVE_NUMPAD_DIVIDE);
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_NUMPAD_MULTIPLY", KeyProcessor.NATIVE_NUMPAD_MULTIPLY);
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_NUMPAD_DECIMAL", KeyProcessor.NATIVE_NUMPAD_DECIMAL);
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_NUMPAD_ADD", KeyProcessor.NATIVE_NUMPAD_ADD);
            ELITE_TO_KEYPROCESSOR_MAP.put("KEY_NUMPAD_SUBTRACT", KeyProcessor.NATIVE_NUMPAD_SUBTRACT);
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
        return FrontierBindingKeyResolver.resolve(eliteKeyName, ELITE_TO_KEYPROCESSOR_MAP);
    }

    /**
     * Returns all known Elite key names (uppercase, e.g. {@code "KEY_W"}).
     * Useful for populating raw-key picker UIs.
     */
    public static Set<String> knownEliteKeyNames() {
        return Collections.unmodifiableSet(ELITE_TO_KEYPROCESSOR_MAP.keySet());
    }

    /**
     * Presses a binding the way its {@code .binds} entry configures it: a plain tap normally, or a
     * {@link #CONFIGURED_HOLD_MS} press-and-hold when the slot carries {@code <Hold Value="1"/>}.
     * <p>
     * This is the default for game actions: the game itself decides which of them need a long press, so
     * the file owns that decision. {@link #executeTap} is the opposite choice, for the callers that mean
     * a tap whatever the file says; a hold the file does not specify comes from
     * {@link #executeBindingWithHold} with an explicit duration.
     */
    public void executeBinding(KeyBindingsParser.KeyBinding binding) {
        executeBindingWithHold(binding, 0); // 0 = no caller override; the binding's own hold flag applies
    }

    /**
     * Executes a binding as a guaranteed single tap, ignoring the {@code <Hold Value="1"/>} flag in the
     * {@code .binds} file.
     * <p>
     * For callers whose own contract is "tap", not "do what the game does": the custom-command editor's
     * <em>Binding Tap</em> step, where the commander picked a tap over the neighbouring <em>Binding
     * Hold</em> step and must get one even if that binding is configured as a long press in their file.
     * <p>
     * // WHY: this used to back every binding press in the app, which silently defeated long-press
     * actions once {@link KeyBindingsParser} started reporting the hold flag correctly. It is kept
     * narrow on purpose: reach for {@link #executeBinding} unless the caller genuinely owns the tap
     * decision, or that bug comes back.
     */
    public void executeTap(KeyBindingsParser.KeyBinding binding) {
        NormalizedChord chord = normalizeChord(binding.key, binding.modifiers);
        if (!warnAndCheckExecutable(chord, binding)) {
            return;
        }
        try {
            log.debug("[exec] executeTap trigger='{}' modifiers={}", chord.triggerKey(), chord.modifierKeys());
            ResolvedChord resolved = resolve(chord);
            if (resolved == null) {
                return;
            }
            for (int modCode : resolved.modifierCodes()) {
                keyProcessor.holdKey(modCode);
            }
            keyProcessor.pressKey(resolved.triggerCode());
            releaseModifiers(resolved.modifierCodes());
            log.debug("Executed tap binding: trigger={}, modifiers={}", chord.triggerKey(), chord.modifierKeys());
        } catch (Exception e) {
            log.error("Error executing tap binding: {}", e.getMessage());
        }
    }

    public void executeBindingWithHold(KeyBindingsParser.KeyBinding binding, int holdTimeMs) {
        NormalizedChord chord = normalizeChord(binding.key, binding.modifiers);
        if (!warnAndCheckExecutable(chord, binding)) {
            return;
        }
        try {
            log.debug("[exec] executeBindingWithHold trigger='{}' modifiers={} hold={}ms",
                    chord.triggerKey(), chord.modifierKeys(), holdTimeMs);
            ResolvedChord resolved = resolve(chord);
            if (resolved == null) {
                return;
            }

            // Press modifiers
            for (int modCode : resolved.modifierCodes()) {
                keyProcessor.holdKey(modCode);
            }

            // Execute trigger key
            if (holdTimeMs > 0) {
                keyProcessor.pressAndHoldKey(resolved.triggerCode(), holdTimeMs);
                log.debug("Executed hold binding: trigger={}, modifiers={}, holdTimeMs={}", chord.triggerKey(), chord.modifierKeys(), holdTimeMs);
            } else if (binding.hold) {
                keyProcessor.holdKey(resolved.triggerCode());
                try {
                    Thread.sleep(CONFIGURED_HOLD_MS);
                } catch (InterruptedException interrupted) {
                    // WHY: shutdownNow() interrupts this worker mid-hold. Let the key up before leaving,
                    // then re-arm the flag so the executor's shutdown still sees it - the broad catch below
                    // would swallow both and leave the key stuck down.
                    keyProcessor.releaseKey(resolved.triggerCode());
                    releaseModifiers(resolved.modifierCodes());
                    Thread.currentThread().interrupt();
                    return;
                }
                keyProcessor.releaseKey(resolved.triggerCode());
                log.debug("Executed hold binding: trigger={}, modifiers={}", chord.triggerKey(), chord.modifierKeys());
            } else {
                keyProcessor.pressKey(resolved.triggerCode());
                log.debug("Executed press binding: trigger={}, modifiers={}", chord.triggerKey(), chord.modifierKeys());
            }

            // Release modifiers in reverse order
            releaseModifiers(resolved.modifierCodes());
        } catch (Exception e) {
            // Ensure all keys are released on error
            for (int modCode : ELITE_TO_KEYPROCESSOR_MAP.values()) {
                keyProcessor.releaseKey(modCode);
            }
            keyProcessor.releaseKey(Optional.ofNullable(resolveKeyCode(chord.triggerKey())).orElse(0));
            log.error("Error executing key binding: {}", e.getMessage());
        }
    }

    /**
     * Presses a binding's chord down (modifiers, then trigger) and leaves every key held.
     * The caller is responsible for a later {@link #releaseBinding} of the same binding,
     * otherwise the keys stay stuck down. Use for holds whose release moment is decided by an
     * external signal rather than a fixed duration.
     */
    public void holdBindingDown(KeyBindingsParser.KeyBinding binding) {
        NormalizedChord chord = normalizeChord(binding.key, binding.modifiers);
        if (!warnAndCheckExecutable(chord, binding)) {
            return;
        }
        ResolvedChord resolved = resolve(chord);
        if (resolved == null) {
            return;
        }
        try {
            for (int modCode : resolved.modifierCodes()) {
                keyProcessor.holdKey(modCode);
            }
            keyProcessor.holdKey(resolved.triggerCode());
            log.debug("[exec] holdBindingDown trigger='{}' modifiers={}", chord.triggerKey(), chord.modifierKeys());
        } catch (Exception e) {
            log.error("Error holding binding down: {}", e.getMessage());
        }
    }

    /**
     * Releases a binding's chord (trigger, then modifiers in reverse) previously held by
     * {@link #holdBindingDown}. The chord resolves deterministically from the binding, so no
     * held-key state needs to be carried between the two calls.
     */
    public void releaseBinding(KeyBindingsParser.KeyBinding binding) {
        NormalizedChord chord = normalizeChord(binding.key, binding.modifiers);
        if (chord.status() == NormalizedChord.Status.NO_TRIGGER) {
            return;
        }
        ResolvedChord resolved = resolve(chord);
        if (resolved == null) {
            return;
        }
        try {
            keyProcessor.releaseKey(resolved.triggerCode());
            releaseModifiers(resolved.modifierCodes());
            log.debug("[exec] releaseBinding trigger='{}' modifiers={}", chord.triggerKey(), chord.modifierKeys());
        } catch (Exception e) {
            log.error("Error releasing binding: {}", e.getMessage());
        }
    }

    /**
     * Re-classifies a binding's keys by their actual identity rather than by their
     * {@code <Primary>}/{@code <Modifier>} XML slot. Frontier's data model is positional:
     * the game evaluates a chord as an unordered <em>set</em> of held keys, and the
     * {@code .binds} format happily parks an action key (e.g. {@code Key_Y}) in a modifier
     * slot and a modifier (e.g. {@code Key_LeftControl}) in the primary slot. Honouring those
     * labels makes us hold the action key for the whole chord — firing its own bindings and
     * any long-press action — and only tap the modifier. We therefore pool every key, hold the
     * ones that are actual Ctrl/Shift/Alt modifiers, and tap the single non-modifier trigger last.
     *
     * <p>A chord of nothing but modifiers (e.g. {@code UI_Back} on Ctrl+Alt) is still legal and
     * still has a trigger edge — the last key down completes it — so there the pooling has nothing
     * to sort and the slot labels are taken at face value instead.
     *
     * <p>Package-private and pure so the classification can be unit-tested without AWT.
     *
     * @param primaryKey     the {@code <Primary>} key token (may be {@code null})
     * @param modifierTokens the {@code <Modifier>} key tokens (may be {@code null})
     */
    static NormalizedChord normalizeChord(String primaryKey, String[] modifierTokens) {
        // A set so a token listed in both a primary and a modifier slot is held/tapped once, not twice.
        Set<String> pool = new LinkedHashSet<>();
        if (primaryKey != null && !primaryKey.isBlank()) {
            pool.add(primaryKey);
        }
        if (modifierTokens != null) {
            for (String token : modifierTokens) {
                if (token != null && !token.isBlank()) {
                    pool.add(token);
                }
            }
        }

        List<String> heldModifiers = new ArrayList<>();
        List<String> triggers = new ArrayList<>();
        for (String token : pool) {
            if (isModifierKey(token)) {
                heldModifiers.add(token);
            } else {
                triggers.add(token);
            }
        }

        if (triggers.isEmpty()) {
            // Every key is a modifier - but that is still a real chord, not an unplayable one:
            // Ctrl+Alt fires when Alt goes down with Ctrl already held. Re-classification by
            // identity has nothing to sort here, so defer to the file's own labelling: <Primary>
            // is the action key, the <Modifier> slots are held. Only a chord with no primary key
            // at all has nothing to press, and KeyBindingsParser.toExecutableBinding already
            // refuses to build one of those.
            if (primaryKey == null || primaryKey.isBlank()) {
                return new NormalizedChord(null, heldModifiers, NormalizedChord.Status.NO_TRIGGER);
            }
            List<String> heldAroundModifierTrigger = new ArrayList<>(pool);
            heldAroundModifierTrigger.remove(primaryKey);
            return new NormalizedChord(primaryKey, heldAroundModifierTrigger, NormalizedChord.Status.MODIFIER_ONLY);
        }
        if (triggers.size() == 1) {
            return new NormalizedChord(triggers.get(0), heldModifiers, NormalizedChord.Status.OK);
        }

        // Two or more non-modifier keys: genuinely ambiguous which one is the trigger.
        // Prefer the slot-labelled primary if it is itself a non-modifier, else the first
        // non-modifier; hold everything else (including the other action key).
        String trigger = !isModifierKey(primaryKey) && primaryKey != null && !primaryKey.isBlank()
                ? primaryKey
                : triggers.get(0);
        List<String> held = new ArrayList<>(pool);
        held.remove(trigger);
        return new NormalizedChord(trigger, held, NormalizedChord.Status.AMBIGUOUS);
    }

    private static boolean isModifierKey(String token) {
        return token != null && BindingModifier.isSupportedKeyboardModifier("Keyboard", token);
    }

    /**
     * Logs any normalization caveat and reports whether the chord can be executed at all.
     * Only {@code NO_TRIGGER} chords - the ones with no key to press at all - are skipped.
     */
    private boolean warnAndCheckExecutable(NormalizedChord chord, KeyBindingsParser.KeyBinding binding) {
        switch (chord.status()) {
            case NO_TRIGGER -> {
                log.warn("[exec] Unexecutable chord (key='{}', modifiers={}): no key to press. Skipping.",
                        binding.key, java.util.Arrays.toString(binding.modifiers));
                return false;
            }
            case MODIFIER_ONLY -> log.debug("[exec] Modifier-only chord (key='{}', modifiers={}): " +
                            "tapping the labelled primary '{}' and holding the rest.",
                    binding.key, java.util.Arrays.toString(binding.modifiers), chord.triggerKey());
            case AMBIGUOUS -> log.warn("[exec] Ambiguous chord (key='{}', modifiers={}): multiple non-modifier keys. " +
                            "Tapping '{}' and holding the rest.",
                    binding.key, java.util.Arrays.toString(binding.modifiers), chord.triggerKey());
            case OK -> { /* normal chord, nothing to warn about */ }
        }
        return true;
    }

    private ResolvedChord resolve(NormalizedChord chord) {
        Integer triggerCode = resolveKeyCode(chord.triggerKey());
        if (triggerCode == null) {
            log.error("[exec] UNKNOWN KEY '{}' — not in ELITE_TO_KEYPROCESSOR_MAP", chord.triggerKey());
            log.error("[exec] Known keys: {}", ELITE_TO_KEYPROCESSOR_MAP.keySet().stream().sorted().toList());
            return null;
        }
        log.debug("[exec] trigger '{}' → keyCode=0x{}", chord.triggerKey(), Integer.toHexString(triggerCode));
        List<String> mods = chord.modifierKeys();
        int[] modifierCodes = new int[mods.size()];
        for (int i = 0; i < mods.size(); i++) {
            Integer modCode = resolveKeyCode(mods.get(i));
            if (modCode == null) {
                log.error("[exec] UNKNOWN MODIFIER '{}' — not in ELITE_TO_KEYPROCESSOR_MAP", mods.get(i));
                return null;
            }
            log.debug("[exec] modifier '{}' → keyCode=0x{}", mods.get(i), Integer.toHexString(modCode));
            modifierCodes[i] = modCode;
        }
        return new ResolvedChord(triggerCode, modifierCodes);
    }

    private void releaseModifiers(int[] modifierCodes) {
        for (int i = modifierCodes.length - 1; i >= 0; i--) {
            keyProcessor.releaseKey(modifierCodes[i]);
        }
    }

    /**
     * A binding's keys re-classified by identity: the keys to hold and the one trigger key to
     * tap last, plus a status describing whether (and how cleanly) the chord can be executed.
     */
    record NormalizedChord(String triggerKey, List<String> modifierKeys, Status status) {
        enum Status {
            /**
             * One non-modifier key, everything else a real modifier.
             */
            OK,
            /**
             * No key to press at all - nothing was in the primary slot.
             */
            NO_TRIGGER,
            /**
             * Every key is a modifier; the labelled primary supplies the trigger edge.
             */
            MODIFIER_ONLY,
            /**
             * Two or more non-modifier keys; one was chosen as the trigger.
             */
            AMBIGUOUS
        }

        NormalizedChord {
            modifierKeys = modifierKeys == null ? List.of() : List.copyOf(modifierKeys);
        }
    }

    /**
     * Resolved key codes for a {@link NormalizedChord}: the trigger and the modifiers to hold.
     */
    private record ResolvedChord(int triggerCode, int[] modifierCodes) {
    }
}
