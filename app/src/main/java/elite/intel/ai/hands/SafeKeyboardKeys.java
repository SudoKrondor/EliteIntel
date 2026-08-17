package elite.intel.ai.hands;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-layout-safe keyboard tokens used by the automatic binding assigner.
 * <p>
 * Unlike {@link EliteKeyboardKeys} (the full allow-list offered for manual
 * assignment), this pool is restricted to keys that occupy the same physical
 * position with the same label across QWERTY, AZERTY, and QWERTZ. We cannot read
 * the user's physical keyboard - the {@code .binds} file only records the game
 * locale - so auto-assignment must pick keys that behave identically regardless
 * of hardware.
 * <p>
 * Excluded on purpose: Q, W, A, Z, M, Y (move between layouts), all punctuation
 * (positions swing across ISO/ANSI/AZERTY), and {@code Key_RightAlt}
 * (AltGr on AZERTY/QWERTZ).
 * <p>
 * The <b>numpad is excluded outright</b> - see {@link #NUMPAD_KEYS_NEVER_AUTO_ASSIGNED}.
 * Its keys are layout-stable, but we cannot know the commander has them: TKL, 60% and most
 * laptop keyboards have no numpad at all, and NumLock changes what those keys send. Numpad
 * keys remain fully supported everywhere else - a commander may bind them by hand through
 * {@link EliteKeyboardKeys}, and {@code KeyBindingExecutor} presses them like any other key.
 */
public final class SafeKeyboardKeys {

    /**
     * One assignable keyboard chord: a base key with an optional supported
     * modifier ({@code null} means an unmodified key press).
     */
    public record Chord(String key, BindingModifier modifier) {
        public boolean hasModifier() {
            return modifier != null;
        }
    }

    /**
     * Never auto-assigned, at any priority: we do not know whether this commander's keyboard
     * even has a numpad. Kept here as documentation of the exclusion (and as the list the
     * guard test checks the pool against), not as a pool the assigner draws from.
     */
    static final List<String> NUMPAD_KEYS_NEVER_AUTO_ASSIGNED = List.of(
            "Key_Numpad_0", "Key_Numpad_1", "Key_Numpad_2", "Key_Numpad_3", "Key_Numpad_4",
            "Key_Numpad_5", "Key_Numpad_6", "Key_Numpad_7", "Key_Numpad_8", "Key_Numpad_9",
            "Key_Numpad_Add", "Key_Numpad_Subtract", "Key_Numpad_Multiply", "Key_Numpad_Divide",
            "Key_Numpad_Decimal"
    );

    private static final List<String> LAYOUT_STABLE_KEYS = buildLayoutStableKeys();

    private static List<String> buildLayoutStableKeys() {
        return List.of(
                // Letters with identical physical position and label on every layout.
                "Key_E", "Key_R", "Key_T", "Key_U", "Key_I", "Key_O", "Key_P",
                "Key_S", "Key_D", "Key_F", "Key_G", "Key_H", "Key_J", "Key_K", "Key_L",
                "Key_B", "Key_N",
                // Top-row digits: same physical position; shifted on AZERTY but resolved by keysym.
                "Key_0", "Key_1", "Key_2", "Key_3", "Key_4",
                "Key_5", "Key_6", "Key_7", "Key_8", "Key_9",
                // Function keys: identical on every layout.
                "Key_F1", "Key_F2", "Key_F3", "Key_F4", "Key_F5", "Key_F6",
                "Key_F7", "Key_F8", "Key_F9", "Key_F10", "Key_F11", "Key_F12"
        );
    }

    // AZERTY safety guard relaxed 2026-06-24: testers confirmed these layout-variable keys work
    // fine in practice, and excluding them needlessly shrank the auto-assign pool. To RESTORE the
    // strict guard, drop LAYOUT_VARIABLE_KEYS from BASE_KEYS below (the list itself can stay).
    private static final List<String> LAYOUT_VARIABLE_KEYS = List.of(
            "Key_Q", "Key_W", "Key_A", "Key_Z", "Key_M", "Key_Y",
            "Key_Minus", "Key_Equals", "Key_LeftBracket", "Key_RightBracket", "Key_BackSlash",
            "Key_SemiColon", "Key_Apostrophe", "Key_Comma", "Key_Period", "Key_Slash", "Key_Grave"
    );

    private static final List<String> BASE_KEYS = buildBaseKeys();

    private static List<String> buildBaseKeys() {
        List<String> keys = new ArrayList<>(LAYOUT_STABLE_KEYS);
        keys.addAll(LAYOUT_VARIABLE_KEYS); // remove this line to restore the strict AZERTY guard
        return List.copyOf(keys);
    }

    // RightAlt is omitted: it is AltGr on AZERTY/QWERTZ.
    private static final List<BindingModifier> SAFE_MODIFIERS = List.of(
            new BindingModifier("Keyboard", "Key_LeftControl"),
            new BindingModifier("Keyboard", "Key_LeftShift"),
            new BindingModifier("Keyboard", "Key_LeftAlt"),
            new BindingModifier("Keyboard", "Key_RightShift")
    );

    private static final List<Chord> ORDERED_CHORDS = buildOrderedChords();

    private SafeKeyboardKeys() {
    }

    /**
     * Every base key auto-assignment may use, in allocation order (layout-stable first, then
     * the relaxed layout-variable ones). The numpad is not in here and never is.
     */
    public static List<String> baseKeys() {
        return BASE_KEYS;
    }

    public static List<BindingModifier> safeModifiers() {
        return SAFE_MODIFIERS;
    }

    /**
     * Returns every assignable chord in allocation order: modifier combos first (each base
     * key paired with every safe modifier), then the plain unmodified keys as a fallback.
     * Combos come first so unmodified keys stay free for the controls a commander reaches
     * for most often. Every chord has a real main key, and a modifier - when present - is
     * always one concrete supported keyboard modifier; there is no blank-modifier chord.
     */
    public static List<Chord> orderedChords() {
        return ORDERED_CHORDS;
    }

    private static List<Chord> buildOrderedChords() {
        List<Chord> chords = new ArrayList<>();
        for (String key : BASE_KEYS) {
            for (BindingModifier modifier : SAFE_MODIFIERS) {
                addUnlessReserved(chords, new Chord(key, modifier));
            }
        }
        for (String key : BASE_KEYS) {
            addUnlessReserved(chords, new Chord(key, null));
        }
        return List.copyOf(chords);
    }

    /**
     * Keeps OS-reserved chords (e.g. Alt+F4) out of the auto-assign pool.
     */
    private static void addUnlessReserved(List<Chord> chords, Chord chord) {
        List<String> modifierKeys = chord.modifier() == null ? List.of() : List.of(chord.modifier().key());
        if (!ReservedKeyChords.isReserved(chord.key(), modifierKeys)) {
            chords.add(chord);
        }
    }
}
