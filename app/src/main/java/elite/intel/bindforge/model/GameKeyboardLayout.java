package elite.intel.bindforge.model;

import java.util.Locale;
import java.util.Set;

/**
 * The keyboard layout the game names its keys by, as a {@code .binds} file declares it in
 * {@code <KeyboardLayout>}.
 * <p>
 * It matters because Elite names a keyboard key by the character it types unshifted on that layout. On a
 * French AZERTY file the number row is {@code Key_Ampersand}, {@code Key_é}, {@code Key_DoubleQuote} ...
 * {@code Key_à}, and there is no {@code Key_1} to {@code Key_0}, no {@code Key_Period}: those characters
 * need Shift there. A chord written with one of those names is not a key the game can tell apart - a
 * commander whose auto-assigned Hyperspace sat on Shift+{@code Key_9} got the Friends menu, which sat on
 * Shift+{@code Key_1}.
 */
public enum GameKeyboardLayout {
    /**
     * French or Belgian AZERTY: the number row and most punctuation are named by their unshifted symbol.
     */
    AZERTY,
    /**
     * Any other layout the game declares (or none).
     */
    OTHER;

    private static final Set<String> AZERTY_TAGS = Set.of("fr-fr", "fr-be");

    /**
     * Key names an AZERTY file cannot hold for a key of its own: the characters need Shift or AltGr there, and
     * the game names the keys that type them by their unshifted symbol instead ({@code Key_ç}, {@code Key_SemiColon}...).
     * Only another layout's naming, or an older auto-assign, puts these in an AZERTY file.
     */
    private static final Set<String> NOT_NAMED_ON_AZERTY = Set.of(
            "Key_0", "Key_1", "Key_2", "Key_3", "Key_4", "Key_5", "Key_6", "Key_7", "Key_8", "Key_9",
            "Key_Period", "Key_Slash", "Key_LeftBracket", "Key_RightBracket", "Key_BackSlash", "Key_Grave"
    );

    /**
     * Reads the {@code <KeyboardLayout>} value of a binds file, e.g. {@code fr-FR}. A missing or unknown tag
     * is {@link #OTHER}: the file then names its keys the way every non-AZERTY file does.
     */
    public static GameKeyboardLayout fromBindsTag(String tag) {
        if (tag == null) {
            return OTHER;
        }
        return AZERTY_TAGS.contains(tag.trim().toLowerCase(Locale.ROOT)) ? AZERTY : OTHER;
    }

    /**
     * False when a file of this layout cannot name {@code key} as a key of its own, so a binding on it is
     * not one the game can tell apart from its neighbours and must not be pressed.
     */
    public boolean namesKey(String key) {
        return this != AZERTY || !NOT_NAMED_ON_AZERTY.contains(key);
    }
}
