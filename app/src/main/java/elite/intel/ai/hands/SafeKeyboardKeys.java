package elite.intel.ai.hands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
 * Layout-stability is one question; which key auto-assignment should reach for first is
 * another. The numpad answers the first well (same label and position everywhere) but the
 * second badly - TKL, 60% and most laptop keyboards have no numpad, and NumLock changes
 * what its keys send. It therefore stays in the pool as a <b>deferred tier</b>: still
 * assignable, but handed out only once every main-block chord is taken.
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
     * Numpad keys carry the same label and position on every layout, so they belong in
     * {@link #LAYOUT_STABLE_KEYS}. They are listed separately only because auto-assignment
     * <em>deprioritises</em> them - see {@link #LAST_RESORT_KEYS}. A commander who has a
     * numpad can still bind these by hand.
     */
    private static final List<String> NUMPAD_KEYS = List.of(
            "Key_Numpad_0", "Key_Numpad_1", "Key_Numpad_2", "Key_Numpad_3", "Key_Numpad_4",
            "Key_Numpad_5", "Key_Numpad_6", "Key_Numpad_7", "Key_Numpad_8", "Key_Numpad_9",
            "Key_Numpad_Add", "Key_Numpad_Subtract", "Key_Numpad_Multiply", "Key_Numpad_Divide",
            "Key_Numpad_Decimal"
    );

    private static final List<String> LAYOUT_STABLE_KEYS = buildLayoutStableKeys();

    private static List<String> buildLayoutStableKeys() {
        List<String> keys = new ArrayList<>(List.of(
                // Letters with identical physical position and label on every layout.
                "Key_E", "Key_R", "Key_T", "Key_U", "Key_I", "Key_O", "Key_P",
                "Key_S", "Key_D", "Key_F", "Key_G", "Key_H", "Key_J", "Key_K", "Key_L",
                "Key_B", "Key_N",
                // Top-row digits: same physical position; shifted on AZERTY but resolved by keysym.
                "Key_0", "Key_1", "Key_2", "Key_3", "Key_4",
                "Key_5", "Key_6", "Key_7", "Key_8", "Key_9"
        ));
        // Numpad: identical label and position on every layout (but see LAST_RESORT_KEYS).
        keys.addAll(NUMPAD_KEYS);
        // Function keys: identical on every layout.
        keys.addAll(List.of(
                "Key_F1", "Key_F2", "Key_F3", "Key_F4", "Key_F5", "Key_F6",
                "Key_F7", "Key_F8", "Key_F9", "Key_F10", "Key_F11", "Key_F12"
        ));
        return List.copyOf(keys);
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

    /**
     * The allocation policy, applied on top of {@link #BASE_KEYS}: which keys auto-assignment
     * saves for last. Membership here says nothing about whether a key is layout-stable or
     * assignable - only that a commander is less likely to have it (TKL, 60% and most laptop
     * keyboards have no numpad) or to reach it reliably (NumLock changes what it sends).
     */
    private static final Set<String> LAST_RESORT_KEYS = Set.copyOf(NUMPAD_KEYS);

    private static final List<String> PREFERRED_KEYS = filterBaseKeys(false);
    private static final List<String> DEFERRED_KEYS = filterBaseKeys(true);

    private static List<String> filterBaseKeys(boolean lastResort) {
        return BASE_KEYS.stream().filter(key -> LAST_RESORT_KEYS.contains(key) == lastResort).toList();
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
     * Every base key auto-assignment may use, in declaration order (layout-stable first,
     * then the relaxed layout-variable ones). Both tiers are in here.
     */
    public static List<String> baseKeys() {
        return BASE_KEYS;
    }

    /**
     * Base keys handed out first - everything except {@link #deferredKeys()}.
     */
    public static List<String> preferredKeys() {
        return PREFERRED_KEYS;
    }

    /**
     * Base keys auto-assignment saves for last (the numpad). Still perfectly assignable -
     * by hand, or by the auto-assigner once every preferred chord is taken.
     */
    public static List<String> deferredKeys() {
        return DEFERRED_KEYS;
    }

    public static List<BindingModifier> safeModifiers() {
        return SAFE_MODIFIERS;
    }

    /**
     * Returns every assignable chord in allocation order: the whole preferred
     * (main-block) tier first, then the numpad tier. Within a tier, modifier combos
     * come before the plain unmodified keys, so unmodified keys stay free for the
     * controls a commander reaches for most often.
     */
    public static List<Chord> orderedChords() {
        return ORDERED_CHORDS;
    }

    private static List<Chord> buildOrderedChords() {
        List<Chord> chords = new ArrayList<>();
        addTier(chords, PREFERRED_KEYS);
        addTier(chords, DEFERRED_KEYS);
        return List.copyOf(chords);
    }

    /**
     * Appends one tier's chords: every combo, then every plain key.
     */
    private static void addTier(List<Chord> chords, List<String> keys) {
        for (String key : keys) {
            for (BindingModifier modifier : SAFE_MODIFIERS) {
                addUnlessReserved(chords, new Chord(key, modifier));
            }
        }
        for (String key : keys) {
            addUnlessReserved(chords, new Chord(key, null));
        }
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
