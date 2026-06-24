package elite.intel.ai.hands;

import java.util.List;
import java.util.Set;

/**
 * Curated allow-list of Elite Dangerous keyboard tokens that the MVP can assign.
 * <p>
 * The list is deliberately static instead of being derived from the active
 * {@code .binds} file: a free key may not appear in the file yet, but still must
 * be available in the UI dropdown.
 */
public final class EliteKeyboardKeys {
    private static final List<String> ASSIGNABLE_KEYS = List.of(
            "Key_A", "Key_B", "Key_C", "Key_D", "Key_E", "Key_F", "Key_G", "Key_H", "Key_I", "Key_J",
            "Key_K", "Key_L", "Key_M", "Key_N", "Key_O", "Key_P", "Key_Q", "Key_R", "Key_S", "Key_T",
            "Key_U", "Key_V", "Key_W", "Key_X", "Key_Y", "Key_Z",
            "Key_0", "Key_1", "Key_2", "Key_3", "Key_4", "Key_5", "Key_6", "Key_7", "Key_8", "Key_9",
            "Key_F1", "Key_F2", "Key_F3", "Key_F4", "Key_F5", "Key_F6", "Key_F7", "Key_F8", "Key_F9",
            "Key_F10", "Key_F11", "Key_F12",
            "Key_Escape", "Key_Tab", "Key_CapsLock", "Key_LeftShift", "Key_RightShift", "Key_LeftControl",
            "Key_RightControl", "Key_LeftAlt", "Key_RightAlt", "Key_Space", "Key_Return", "Key_Backspace",
            "Key_Insert", "Key_Delete", "Key_Home", "Key_End", "Key_PageUp", "Key_PageDown",
            "Key_UpArrow", "Key_DownArrow", "Key_LeftArrow", "Key_RightArrow",
            "Key_Grave", "Key_Minus", "Key_Equals", "Key_LeftBracket", "Key_RightBracket",
            "Key_BackSlash", "Key_SemiColon", "Key_Apostrophe", "Key_Comma", "Key_Period", "Key_Slash",
            "Key_Hash",
            "Key_Numpad_0", "Key_Numpad_1", "Key_Numpad_2", "Key_Numpad_3", "Key_Numpad_4",
            "Key_Numpad_5", "Key_Numpad_6", "Key_Numpad_7", "Key_Numpad_8", "Key_Numpad_9",
            "Key_Numpad_Add", "Key_Numpad_Subtract", "Key_Numpad_Multiply", "Key_Numpad_Divide",
            "Key_Numpad_Decimal", "Key_NumLock", "Key_ScrollLock", "Key_Pause", "Key_Apps"
    );

    private static final Set<String> ASSIGNABLE_KEY_SET = Set.copyOf(ASSIGNABLE_KEYS);

    private EliteKeyboardKeys() {
    }

    /**
     * Returns supported keyboard tokens in stable display order.
     */
    public static List<String> assignableKeys() {
        return ASSIGNABLE_KEYS;
    }

    /**
     * Validates a token before any save attempt.
     */
    public static boolean isAssignable(String key) {
        return ASSIGNABLE_KEY_SET.contains(key);
    }
}
