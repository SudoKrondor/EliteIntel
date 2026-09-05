package elite.intel.ai.hands;

import elite.intel.ai.hands.KeyBindingsParser.ReadOnlyBindingSlot;
import elite.intel.ai.hands.KeyBindingsParser.ReadOnlyBindingSlots;
import elite.intel.util.OsDetector;

import java.util.*;

/**
 * Keyboard keys and chords that must never be assigned to a game control.
 * <p>
 * Two separate sources, one fixed and one read out of the commander's own file.
 * <p>
 * <b>Intercepted by the operating system / desktop environment</b> before the game ever sees them,
 * and triggering them mid-flight is destructive:
 * <ul>
 *   <li><b>Alt+F4</b> closes the focused window on every desktop OS - it would quit the game.</li>
 *   <li><b>Ctrl+Alt+F1..F12</b> on Linux switch virtual terminals and can drop the user to a TTY,
 *       yanking them out of the running game session.</li>
 * </ul>
 * These are matched on the full key-set (main key + modifiers), so key order and any extra modifiers
 * held alongside the reserved combination do not let one slip through.
 * <p>
 * <b>Claimed by Elite Dangerous itself: the game-menu key.</b> Elite's {@code Pause} control ("Game
 * Menu" on the options screen) opens the menu and pauses the game. It is matched on the <em>key
 * alone</em>: with {@code Pause} on {@code Key_P}, the menu comes up on P, on Shift+P, on Alt+P, on
 * anything ending in P. So a second control sharing that key can never be pressed without dropping
 * the commander out of the cockpit and into the options screen, whatever modifiers are stacked on it.
 * Which key that is comes from the file - it is whatever the commander put on {@code Pause} - so this
 * half of the rule is a parameter ({@code gameMenuKeys}) and not a constant.
 * <p>
 * <b>The game-menu control is worth nothing and costs a key.</b> {@code Esc} opens that same menu
 * whether or not {@code Pause} is bound to anything, so a key on {@code Pause} buys the commander
 * a second way in and takes a key off the board. That is why {@link MissingBindingAutoAssigner}
 * leaves {@code Pause} empty rather than filling it like any other unbound control.
 * <p>
 * How this was found: the auto-assigner handed out Alt+P on a file whose {@code Pause} was on
 * {@code Key_P}, and the control opened the options menu every time. The first reading blamed the
 * Alt+P chord - Elite documents none of this - but P was never special: the reserved key is
 * whatever sits on {@code Pause}, and on that file it happened to be P.
 * <p>
 * Used by the auto-assigner (to keep these out of the chord pool), the manual assign dialog (to
 * block the save and flag the key), the live keyboard map (to colour the keys as reserved), and
 * {@link #scan} (to report the ones already sitting in the commander's file).
 */
public final class ReservedKeyChords {

    /**
     * Elite's action name for the game-menu control, as it appears in the {@code .binds} file.
     */
    public static final String GAME_MENU_ACTION = "Pause";

    private static final boolean IS_LINUX = OsDetector.getOs() == OsDetector.OS.LINUX;

    private static final Set<String> ALT_KEYS = Set.of("Key_LeftAlt", "Key_RightAlt");
    private static final Set<String> CTRL_KEYS = Set.of("Key_LeftControl", "Key_RightControl");
    private static final Set<String> FUNCTION_KEYS = Set.of(
            "Key_F1", "Key_F2", "Key_F3", "Key_F4", "Key_F5", "Key_F6",
            "Key_F7", "Key_F8", "Key_F9", "Key_F10", "Key_F11", "Key_F12");

    /**
     * One control already bound to a reserved key or chord in the commander's file.
     *
     * @param action the Elite action name ({@code QuickCommsPanel}, ...)
     * @param chord  the full key-set, in Elite's raw tokens, for rendering with {@link BindingChordSpeech}
     * @param reason what that chord does instead of - or as well as - the control, as a sentence fragment
     *               completing "&lt;control&gt; is on a chord that ..."; carried on the record because only
     *               the scan knows which of the two rules matched, and they are not interchangeable
     */
    public record ReservedBinding(String action, Set<String> chord, String reason) {
    }

    private ReservedKeyChords() {
    }

    /**
     * The keyboard keys the commander has on the game-menu control, from the executable binding map.
     * <p>
     * Normally empty or a single key. Returned as a set because the answer is a property of the file,
     * not a constant, and because {@link #gameMenuKeysFromSlots} can legitimately find two.
     */
    public static Set<String> gameMenuKeys(Map<String, KeyBindingsParser.KeyBinding> bindings) {
        if (bindings == null) {
            return Set.of();
        }
        KeyBindingsParser.KeyBinding menu = bindings.get(GAME_MENU_ACTION);
        return menu == null ? Set.of() : mainKeySet(menu.key);
    }

    /**
     * The keyboard keys on the game-menu control, from the read-only slot map.
     * <p>
     * Both slots are read, unlike {@link #gameMenuKeys}: Elite gives every control a Primary and a
     * Secondary, and a commander with a key in each has two keys that open the menu. The executable
     * map keeps only one slot per action, which is fine for pressing a control and not fine for
     * deciding which keys are unusable.
     */
    public static Set<String> gameMenuKeysFromSlots(Map<String, ReadOnlyBindingSlots> slots) {
        if (slots == null) {
            return Set.of();
        }
        ReadOnlyBindingSlots menu = slots.get(GAME_MENU_ACTION);
        if (menu == null) {
            return Set.of();
        }
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(keyboardMainKey(menu.primary()));
        keys.addAll(keyboardMainKey(menu.secondary()));
        return Set.copyOf(keys);
    }

    /**
     * Every control in the file that is already bound to a reserved key or chord.
     * <p>
     * Refusing one at assignment time only helps the commander who assigns it through EliteIntel.
     * Elite's own controls screen has no such rule, and a {@code .binds} written there - or carried
     * over from an older install - can hold Alt+F4, or a control sharing the game-menu key, on
     * anything. So the file is read as well as written: what cannot be assigned is also reported
     * when found.
     * <p>
     * Scanned across every action, not just the ones EliteIntel presses. A reserved chord is a
     * property of the chord, not of who presses it: Alt+F4 quits the game whoever reaches for it.
     * The game-menu control itself is never reported - it is the one control that key belongs to.
     *
     * @param bindings action name to parsed binding, as from {@code BindingsMonitor.getBindings()}
     * @return the offending bindings in action-name order; empty when the file is clean
     */
    public static List<ReservedBinding> scan(Map<String, KeyBindingsParser.KeyBinding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }
        Set<String> menuKeys = gameMenuKeys(bindings);
        List<ReservedBinding> found = new ArrayList<>();
        // Sorted so the spoken list and the log lines come out in the same order on every start.
        for (Map.Entry<String, KeyBindingsParser.KeyBinding> entry : new TreeMap<>(bindings).entrySet()) {
            if (GAME_MENU_ACTION.equals(entry.getKey())) {
                continue; // the game menu is allowed to sit on its own key
            }
            KeyBindingsParser.KeyBinding binding = entry.getValue();
            if (binding == null || binding.key == null || binding.key.isBlank() || "Key_".equals(binding.key)) {
                continue; // unbound, or bound only to a device we cannot press
            }
            List<String> modifiers = binding.modifiers == null ? List.of() : Arrays.asList(binding.modifiers);
            String reason = reason(binding.key, modifiers, menuKeys);
            if (reason == null) {
                continue;
            }
            Set<String> chord = new LinkedHashSet<>();
            chord.add(binding.key);
            chord.addAll(modifiers);
            found.add(new ReservedBinding(entry.getKey(), chord, reason));
        }
        return List.copyOf(found);
    }

    /**
     * True when this chord must not be assigned, given the keys the commander has on the game menu.
     * <p>
     * {@code mainKey} is kept apart from {@code modifierKeys} because the two rules read the chord
     * differently: the OS combinations match the whole key-set, while the game-menu key matches the
     * main key only. A key held as a modifier alongside something else does not open the menu, so
     * reserving it there would take keys off the board for nothing.
     *
     * @param gameMenuKeys from {@link #gameMenuKeys} or {@link #gameMenuKeysFromSlots}; empty when the
     *                     commander has left the game-menu control unbound, which is the recommended state
     */
    public static boolean isReserved(String mainKey, Collection<String> modifierKeys, Collection<String> gameMenuKeys) {
        return reason(mainKey, modifierKeys, gameMenuKeys) != null;
    }

    /**
     * True when the chord is reserved by the operating system alone, with no {@code .binds} file in
     * hand. This is the rule {@link SafeKeyboardKeys} can apply while building its static pool; every
     * caller that knows the commander's bindings uses {@link #isReserved} instead, so the game-menu
     * key is covered too.
     */
    public static boolean isOsReserved(String mainKey, Collection<String> modifierKeys) {
        return isReservedKeyset(keyset(mainKey, modifierKeys));
    }

    /**
     * What this chord does instead of - or as well as - the control bound to it, or {@code null} when
     * it is free to assign.
     * <p>
     * Named per rule rather than generically, because the consequences are not comparable: one quits
     * the game, one leaves the desktop session entirely, one pauses the game and opens the options
     * screen. The spoken warning stays generic (it has to cover all of them in nine languages); this
     * is the line that goes in the log and the diagnostics bundle, where the commander - or whoever
     * reads their bundle - needs the specifics.
     */
    static String reason(String mainKey, Collection<String> modifierKeys, Collection<String> gameMenuKeys) {
        return reason(mainKey, modifierKeys, gameMenuKeys, IS_LINUX);
    }

    /**
     * OS passed explicitly so the platform-specific rules are deterministically testable.
     */
    static String reason(String mainKey, Collection<String> modifierKeys, Collection<String> gameMenuKeys, boolean linux) {
        Set<String> keys = keyset(mainKey, modifierKeys);
        boolean hasAlt = keys.stream().anyMatch(ALT_KEYS::contains);
        if (hasAlt && keys.contains("Key_F4")) {
            return "closes the game window";
        }
        if (isReservedKeyset(keys, linux)) {
            return "switches to a virtual terminal and leaves the game session";
        }
        if (mainKey != null && gameMenuKeys != null && gameMenuKeys.contains(mainKey)) {
            // Elite matches the game-menu key on the key alone, so the modifiers on this chord change nothing.
            return "is the key the game menu is on, so pressing it pauses the game and opens the options screen";
        }
        return null;
    }

    private static Set<String> keyset(String mainKey, Collection<String> modifierKeys) {
        Set<String> keys = new HashSet<>();
        if (mainKey != null && !mainKey.isBlank() && !"Key_".equals(mainKey)) {
            keys.add(mainKey);
        }
        if (modifierKeys != null) {
            for (String modifier : modifierKeys) {
                if (modifier != null && !modifier.isBlank()) {
                    keys.add(modifier);
                }
            }
        }
        return keys;
    }

    private static Set<String> mainKeySet(String key) {
        return key == null || key.isBlank() || "Key_".equals(key) ? Set.of() : Set.of(key);
    }

    private static Set<String> keyboardMainKey(ReadOnlyBindingSlot slot) {
        return slot == null || !"Keyboard".equals(slot.device()) ? Set.of() : mainKeySet(slot.key());
    }

    /**
     * Key-set based core of the OS rules, so they can be tested without constructing chords.
     */
    static boolean isReservedKeyset(Set<String> keys) {
        return isReservedKeyset(keys, IS_LINUX);
    }

    /**
     * OS passed explicitly so the platform-specific rules are deterministically testable.
     */
    static boolean isReservedKeyset(Set<String> keys, boolean linux) {
        boolean hasAlt = keys.stream().anyMatch(ALT_KEYS::contains);
        boolean hasCtrl = keys.stream().anyMatch(CTRL_KEYS::contains);
        boolean hasFunctionKey = keys.stream().anyMatch(FUNCTION_KEYS::contains);

        // Alt+F4 closes the window on every desktop OS.
        if (hasAlt && keys.contains("Key_F4")) {
            return true;
        }
        // Ctrl+Alt+F1..F12 switch virtual terminals on Linux.
        return linux && hasCtrl && hasAlt && hasFunctionKey;
    }
}
