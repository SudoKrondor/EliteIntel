package elite.intel.bindforge.rules;

import elite.intel.bindforge.model.BindingModifier;
import elite.intel.bindforge.model.EliteKeyboardKeys;
import elite.intel.bindforge.model.GameKeyboardLayout;

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
 * The pool depends on the layout the binds file declares ({@link GameKeyboardLayout}). On an AZERTY
 * file it is strict: the layout-stable letters and the function keys only - no top-row digits, no
 * Q/W/A/Z/M/Y, no punctuation. Other layouts also get the digits and the layout-variable keys
 * (relaxed 2026-06-24). {@code Key_RightAlt} (AltGr on AZERTY/QWERTZ) is never a modifier.
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
    public static final List<String> NUMPAD_KEYS_NEVER_AUTO_ASSIGNED = List.of(
            "Key_Numpad_0", "Key_Numpad_1", "Key_Numpad_2", "Key_Numpad_3", "Key_Numpad_4",
            "Key_Numpad_5", "Key_Numpad_6", "Key_Numpad_7", "Key_Numpad_8", "Key_Numpad_9",
            "Key_Numpad_Add", "Key_Numpad_Subtract", "Key_Numpad_Multiply", "Key_Numpad_Divide",
            "Key_Numpad_Decimal"
    );

    /**
     * Letters with an identical physical position and label on every layout.
     */
    private static final List<String> LAYOUT_STABLE_LETTERS = List.of(
            "Key_E", "Key_R", "Key_T", "Key_U", "Key_I", "Key_O", "Key_P",
            "Key_S", "Key_D", "Key_F", "Key_G", "Key_H", "Key_J", "Key_K", "Key_L",
            "Key_B", "Key_N"
    );

    /**
     * Top-row digits. Same physical position everywhere, but on AZERTY the digit is the <i>shifted</i>
     * character, and the game names that key by its unshifted symbol ({@code Key_é}, {@code Key_ç}...) -
     * a {@code Key_9} in an AZERTY file is not a key the game can tell apart from {@code Key_1}.
     */
    private static final List<String> TOP_ROW_DIGITS = List.of(
            "Key_0", "Key_1", "Key_2", "Key_3", "Key_4",
            "Key_5", "Key_6", "Key_7", "Key_8", "Key_9"
    );

    /**
     * Function keys: identical on every layout.
     */
    private static final List<String> FUNCTION_KEYS = List.of(
            "Key_F1", "Key_F2", "Key_F3", "Key_F4", "Key_F5", "Key_F6",
            "Key_F7", "Key_F8", "Key_F9", "Key_F10", "Key_F11", "Key_F12"
    );

    /**
     * Keys whose position or meaning moves between layouts. Relaxed into the pool on 2026-06-24 for
     * non-AZERTY files (testers confirmed they work there); never handed out on an AZERTY file, where
     * several of them ({@code Key_Period}, {@code Key_Slash}, the brackets...) need Shift or AltGr and so
     * have no name the game recognises.
     */
    private static final List<String> LAYOUT_VARIABLE_KEYS = List.of(
            "Key_Q", "Key_W", "Key_A", "Key_Z", "Key_M", "Key_Y",
            "Key_Minus", "Key_Equals", "Key_LeftBracket", "Key_RightBracket", "Key_BackSlash",
            "Key_SemiColon", "Key_Apostrophe", "Key_Comma", "Key_Period", "Key_Slash", "Key_Grave"
    );

    /**
     * The strict AZERTY pool: only keys that exist under the same name on an AZERTY file.
     */
    private static final List<String> AZERTY_BASE_KEYS = concat(LAYOUT_STABLE_LETTERS, FUNCTION_KEYS);

    private static final List<String> RELAXED_BASE_KEYS =
            concat(LAYOUT_STABLE_LETTERS, TOP_ROW_DIGITS, FUNCTION_KEYS, LAYOUT_VARIABLE_KEYS);

    // RightAlt is omitted: it is AltGr on AZERTY/QWERTZ.
    private static final List<BindingModifier> SAFE_MODIFIERS = List.of(
            new BindingModifier("Keyboard", "Key_LeftControl"),
            new BindingModifier("Keyboard", "Key_LeftShift"),
            new BindingModifier("Keyboard", "Key_LeftAlt"),
            new BindingModifier("Keyboard", "Key_RightShift")
    );

    private static final List<Chord> AZERTY_CHORDS = buildOrderedChords(AZERTY_BASE_KEYS);
    private static final List<Chord> RELAXED_CHORDS = buildOrderedChords(RELAXED_BASE_KEYS);

    private SafeKeyboardKeys() {
    }

    /**
     * Every base key auto-assignment may use on a file of this layout, in allocation order. The numpad is
     * not in here and never is.
     */
    public static List<String> baseKeys(GameKeyboardLayout layout) {
        return layout == GameKeyboardLayout.AZERTY ? AZERTY_BASE_KEYS : RELAXED_BASE_KEYS;
    }

    public static List<BindingModifier> safeModifiers() {
        return SAFE_MODIFIERS;
    }

    /**
     * Returns every assignable chord for a file of this layout, in allocation order: modifier combos first
     * (each base key paired with every safe modifier), then the plain unmodified keys as a fallback.
     * Combos come first so unmodified keys stay free for the controls a commander reaches for most often.
     * Every chord has a real main key, and a modifier - when present - is always one concrete supported
     * keyboard modifier; there is no blank-modifier chord.
     */
    public static List<Chord> orderedChords(GameKeyboardLayout layout) {
        return layout == GameKeyboardLayout.AZERTY ? AZERTY_CHORDS : RELAXED_CHORDS;
    }

    @SafeVarargs
    private static List<String> concat(List<String>... groups) {
        List<String> keys = new ArrayList<>();
        for (List<String> group : groups) {
            keys.addAll(group);
        }
        return List.copyOf(keys);
    }

    private static List<Chord> buildOrderedChords(List<String> baseKeys) {
        List<Chord> chords = new ArrayList<>();
        for (String key : baseKeys) {
            for (BindingModifier modifier : SAFE_MODIFIERS) {
                addUnlessReserved(chords, new Chord(key, modifier));
            }
        }
        for (String key : baseKeys) {
            addUnlessReserved(chords, new Chord(key, null));
        }
        return List.copyOf(chords);
    }

    /**
     * Keeps OS-reserved chords (e.g. Alt+F4) out of the auto-assign pool.
     * <p>
     * Only the OS half of {@link ReservedKeyChords} can be applied here: this pool is a constant, and
     * the other half - the key the commander has on the game menu - is read out of their {@code .binds}
     * file. {@link MissingBindingAutoAssigner} filters the pool for that as it draws from it.
     */
    private static void addUnlessReserved(List<Chord> chords, Chord chord) {
        List<String> modifierKeys = chord.modifier() == null ? List.of() : List.of(chord.modifier().key());
        if (!ReservedKeyChords.isOsReserved(chord.key(), modifierKeys)) {
            chords.add(chord);
        }
    }
}
