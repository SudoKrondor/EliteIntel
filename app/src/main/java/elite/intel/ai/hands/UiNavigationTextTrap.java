package elite.intel.ai.hands;

import java.util.*;

/**
 * Detects UI navigation bound to a key that a focused Elite text field eats as text.
 * <p>
 * <b>The trap.</b> While a text field has focus - the galaxy map's system search box above all - Elite
 * treats any keystroke that produces a <em>printable character</em> as typing and never consults the
 * {@code UI_*} bindings at all. A commander with {@code UI_Down} on {@code S} or {@code Shift+S} who
 * types a system name and then presses their own "down" key does not move onto the first result: they
 * append an "s" to what they typed. Focus never leaves the box, and every keystroke after it is typed
 * into the box too.
 * <p>
 * <b>Why the modifier decides it, not the binding.</b> {@code Ctrl+S} and {@code Alt+S} produce no
 * character, so the keystroke falls through to the binding and navigation works (verified in game,
 * 2026-08-31, with UI navigation on {@code Ctrl+W/A/S/D}). {@code Shift+S} produces "S" and is
 * swallowed exactly like bare {@code S}. That is why this setup appears to work for some commanders
 * and not others, and why the test is "does this chord type a character", not "is this chord modified".
 * <p>
 * <b>What we can and cannot do about it.</b> {@code RoutePlotter} sends a raw Down arrow for the one
 * step that has to escape the search box, because the arrow produces no character on any layout. That
 * covers the commander whose arrows still carry UI navigation. It does nothing for a commander who
 * moved UI navigation off the arrows entirely - their own keys still cannot get out of the box, in our
 * hands or theirs. That is a bindings problem, so it is reported rather than worked around.
 * <p>
 * Pure and side-effect free, so it is unit-testable without the file watcher or database.
 */
public final class UiNavigationTextTrap {

    /**
     * One UI navigation action whose chord a focused text field would swallow.
     *
     * @param action the Elite action name ({@code UI_Down}, ...)
     * @param chord  the full key-set, in Elite's raw tokens, for rendering with {@link BindingChordSpeech}
     */
    public record TrappedBinding(String action, Set<String> chord) {
    }

    /**
     * The four directions the commander steers menus and the galaxy map result list with.
     * <p>
     * Deliberately NOT {@code UI_Select}: Frontier's default for it is bare {@code Space}, which types a
     * character and would therefore flag almost every commander alive. Select is also pressed only once
     * focus has already left the text field, where a printable key is harmless.
     */
    private static final List<String> UI_NAVIGATION_ACTIONS = List.of("UI_Up", "UI_Down", "UI_Left", "UI_Right");

    /**
     * Modifiers that suppress the character a key would otherwise produce, so the keystroke reaches the
     * binding instead of the text field. Shift is pointedly absent - it changes the character rather than
     * removing it.
     */
    private static final Set<String> CHARACTER_SUPPRESSING_MODIFIERS = Set.of(
            "KEY_LEFTCONTROL", "KEY_RIGHTCONTROL",
            "KEY_LEFTALT", "KEY_RIGHTALT",
            "KEY_LEFTSUPER", "KEY_RIGHTSUPER",
            "KEY_APPS", "KEY_MENU");

    /**
     * Keys that put a character in the box when pressed alone.
     * <p>
     * Listed positively rather than as "everything except the arrows": an Elite key token we do not
     * recognise then produces no warning, which is the right way to be wrong. A startup warning that
     * cries wolf about a working layout gets tuned out, and it is announced on every start.
     */
    private static final Set<String> CHARACTER_PRODUCING_KEYS = buildCharacterProducingKeys();

    private UiNavigationTextTrap() {
    }

    /**
     * Scans the four UI direction bindings for chords a focused text field would swallow.
     *
     * @param bindings action name to parsed binding, as from {@code BindingsMonitor.getBindings()}
     * @return the trapped bindings in {@link #UI_NAVIGATION_ACTIONS} order; empty when the layout is safe
     */
    public static List<TrappedBinding> scan(Map<String, KeyBindingsParser.KeyBinding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }
        List<TrappedBinding> trapped = new ArrayList<>();
        for (String action : UI_NAVIGATION_ACTIONS) {
            KeyBindingsParser.KeyBinding binding = bindings.get(action);
            if (binding == null || binding.key == null || binding.key.isBlank()) {
                continue; // unbound, or bound only to a device we cannot press - not this check's business
            }
            String[] modifiers = binding.modifiers == null ? new String[0] : binding.modifiers;
            if (!typesACharacter(binding.key, modifiers)) {
                continue;
            }
            Set<String> chord = new LinkedHashSet<>();
            chord.add(binding.key);
            chord.addAll(Arrays.asList(modifiers));
            trapped.add(new TrappedBinding(action, chord));
        }
        return List.copyOf(trapped);
    }

    /**
     * True when pressing this chord puts a character into a focused text field instead of navigating.
     * Package-private so the classification can be exercised directly.
     */
    static boolean typesACharacter(String key, String[] modifiers) {
        if (key == null || key.isBlank()) {
            return false;
        }
        for (String modifier : modifiers) {
            if (modifier != null && CHARACTER_SUPPRESSING_MODIFIERS.contains(modifier.toUpperCase(Locale.ROOT))) {
                return false;
            }
        }
        return CHARACTER_PRODUCING_KEYS.contains(key.toUpperCase(Locale.ROOT));
    }

    private static Set<String> buildCharacterProducingKeys() {
        Set<String> keys = new HashSet<>();
        for (char c = 'A'; c <= 'Z'; c++) {
            keys.add("KEY_" + c);
        }
        for (char c = '0'; c <= '9'; c++) {
            keys.add("KEY_" + c);
            keys.add("KEY_NUMPAD_" + c);
        }
        keys.addAll(List.of(
                // Space is a character like any other: it lands in the box as a space.
                "KEY_SPACE",
                // ASCII punctuation, in Elite's own token spellings.
                "KEY_MINUS", "KEY_EQUALS", "KEY_SEMICOLON", "KEY_APOSTROPHE", "KEY_COMMA", "KEY_PERIOD",
                "KEY_SLASH", "KEY_BACKSLASH", "KEY_LEFTBRACKET", "KEY_RIGHTBRACKET", "KEY_GRAVE",
                "KEY_GRAVEACCENT", "KEY_TILDE", "KEY_HASH", "KEY_LESSTHAN",
                // Numpad keys that type rather than navigate. Numpad Enter is excluded with main Enter.
                "KEY_NUMPAD_DECIMAL", "KEY_NUMPAD_ADD", "KEY_NUMPAD_SUBTRACT",
                "KEY_NUMPAD_DIVIDE", "KEY_NUMPAD_MULTIPLY",
                // Non-ASCII layouts: DE, FR AZERTY and ES all put letters on their own keys, and Elite
                // serialises them as the character itself (e.g. "Key_é"), which uppercases to these.
                "KEY_Ä", "KEY_Ö", "KEY_Ü", "KEY_SS", "KEY_ẞ", "KEY_ACUTE",
                "KEY_É", "KEY_È", "KEY_À", "KEY_Ù", "KEY_Ç", "KEY_Ñ"));
        return Set.copyOf(keys);
    }
}
