package elite.intel.util;

import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.win32.User32;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class for mapping key events to Elite game-specific keyboard tokens.
 * This class helps to convert {@link KeyEvent} codes into unique
 * string identifiers (Elite tokens) that represent physical key positions,
 * regardless of the active keyboard layout.
 *
 * <p>This is achieved using two mappings:
 * <ul>
 * - {@code SCAN_TO_TOKEN}: Maps PS/2 scan codes to Elite tokens. Primarily used on
 *   Windows for layout-independent keys.
 * - {@code VK_TO_TOKEN}: Maps Virtual-Key (VK) codes to Elite tokens, assuming a
 *   QWERTY physical layout. Used as a fallback on Windows and on Linux.
 *
 * <p>The mappings cover various key groups such as letters, numbers, function keys,
 * punctuation, navigation keys, numpad keys, and modifiers.
 *
 * <p>This class is designed to be stateless, and cannot be instantiated.
 */
public final class KeyCaptureMapper {

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");

    private static final boolean IS_LINUX =
            !IS_WINDOWS && System.getProperty("os.name", "").toLowerCase().contains("linux");

    // PS/2 Set-1 scan code → Elite token.
    // Mirrors the letter/punctuation/number-row entries in WindowsNativeKeyInput.SCAN_MAP,
    // with the key and value swapped. Used on the capture (input) side on Windows.
    private static final Map<Integer, String> SCAN_TO_TOKEN = new HashMap<>();

    // VK code → Elite token, assuming QWERTY physical positions.
    // Used on Linux, and as a fallback on Windows for keys not covered by SCAN_TO_TOKEN.
    private static final Map<Integer, String> VK_TO_TOKEN = new HashMap<>();

    static {
        // --- SCAN_TO_TOKEN: PS/2 scan codes → Elite tokens ---
        // Number row
        SCAN_TO_TOKEN.put(0x02, "Key_1");
        SCAN_TO_TOKEN.put(0x03, "Key_2");
        SCAN_TO_TOKEN.put(0x04, "Key_3");
        SCAN_TO_TOKEN.put(0x05, "Key_4");
        SCAN_TO_TOKEN.put(0x06, "Key_5");
        SCAN_TO_TOKEN.put(0x07, "Key_6");
        SCAN_TO_TOKEN.put(0x08, "Key_7");
        SCAN_TO_TOKEN.put(0x09, "Key_8");
        SCAN_TO_TOKEN.put(0x0A, "Key_9");
        SCAN_TO_TOKEN.put(0x0B, "Key_0");
        SCAN_TO_TOKEN.put(0x0C, "Key_Minus");
        SCAN_TO_TOKEN.put(0x0D, "Key_Equals");
        // Top letter row
        SCAN_TO_TOKEN.put(0x10, "Key_Q");
        SCAN_TO_TOKEN.put(0x11, "Key_W");
        SCAN_TO_TOKEN.put(0x12, "Key_E");
        SCAN_TO_TOKEN.put(0x13, "Key_R");
        SCAN_TO_TOKEN.put(0x14, "Key_T");
        SCAN_TO_TOKEN.put(0x15, "Key_Y");
        SCAN_TO_TOKEN.put(0x16, "Key_U");
        SCAN_TO_TOKEN.put(0x17, "Key_I");
        SCAN_TO_TOKEN.put(0x18, "Key_O");
        SCAN_TO_TOKEN.put(0x19, "Key_P");
        SCAN_TO_TOKEN.put(0x1A, "Key_LeftBracket");
        SCAN_TO_TOKEN.put(0x1B, "Key_RightBracket");
        // Home letter row
        SCAN_TO_TOKEN.put(0x1E, "Key_A");
        SCAN_TO_TOKEN.put(0x1F, "Key_S");
        SCAN_TO_TOKEN.put(0x20, "Key_D");
        SCAN_TO_TOKEN.put(0x21, "Key_F");
        SCAN_TO_TOKEN.put(0x22, "Key_G");
        SCAN_TO_TOKEN.put(0x23, "Key_H");
        SCAN_TO_TOKEN.put(0x24, "Key_J");
        SCAN_TO_TOKEN.put(0x25, "Key_K");
        SCAN_TO_TOKEN.put(0x26, "Key_L");
        SCAN_TO_TOKEN.put(0x27, "Key_SemiColon");
        SCAN_TO_TOKEN.put(0x28, "Key_Apostrophe");
        SCAN_TO_TOKEN.put(0x29, "Key_Grave");
        SCAN_TO_TOKEN.put(0x2B, "Key_BackSlash");
        // Bottom letter row
        SCAN_TO_TOKEN.put(0x2C, "Key_Z");
        SCAN_TO_TOKEN.put(0x2D, "Key_X");
        SCAN_TO_TOKEN.put(0x2E, "Key_C");
        SCAN_TO_TOKEN.put(0x2F, "Key_V");
        SCAN_TO_TOKEN.put(0x30, "Key_B");
        SCAN_TO_TOKEN.put(0x31, "Key_N");
        SCAN_TO_TOKEN.put(0x32, "Key_M");
        SCAN_TO_TOKEN.put(0x33, "Key_Comma");
        SCAN_TO_TOKEN.put(0x34, "Key_Period");
        SCAN_TO_TOKEN.put(0x35, "Key_Slash");

        // --- VK_TO_TOKEN: QWERTY-assumption fallback ---
        // Letters A-Z
        for (int i = 0; i < 26; i++) {
            VK_TO_TOKEN.put(KeyEvent.VK_A + i, "Key_" + (char) ('A' + i));
        }
        // Number row 0-9
        for (int i = 0; i <= 9; i++) {
            VK_TO_TOKEN.put(KeyEvent.VK_0 + i, "Key_" + i);
        }
        // Function keys F1-F12
        for (int i = 1; i <= 12; i++) {
            VK_TO_TOKEN.put(KeyEvent.VK_F1 + (i - 1), "Key_F" + i);
        }
        // Numpad digits 0-9
        for (int i = 0; i <= 9; i++) {
            VK_TO_TOKEN.put(KeyEvent.VK_NUMPAD0 + i, "Key_Numpad_" + i);
        }
        // Navigation / editing
        VK_TO_TOKEN.put(KeyEvent.VK_ESCAPE, "Key_Escape");
        VK_TO_TOKEN.put(KeyEvent.VK_TAB, "Key_Tab");
        VK_TO_TOKEN.put(KeyEvent.VK_CAPS_LOCK, "Key_CapsLock");
        VK_TO_TOKEN.put(KeyEvent.VK_SPACE, "Key_Space");
        VK_TO_TOKEN.put(KeyEvent.VK_ENTER, "Key_Return");
        VK_TO_TOKEN.put(KeyEvent.VK_BACK_SPACE, "Key_Backspace");
        VK_TO_TOKEN.put(KeyEvent.VK_INSERT, "Key_Insert");
        VK_TO_TOKEN.put(KeyEvent.VK_DELETE, "Key_Delete");
        VK_TO_TOKEN.put(KeyEvent.VK_HOME, "Key_Home");
        VK_TO_TOKEN.put(KeyEvent.VK_END, "Key_End");
        VK_TO_TOKEN.put(KeyEvent.VK_PAGE_UP, "Key_PageUp");
        VK_TO_TOKEN.put(KeyEvent.VK_PAGE_DOWN, "Key_PageDown");
        // Arrow keys
        VK_TO_TOKEN.put(KeyEvent.VK_UP, "Key_UpArrow");
        VK_TO_TOKEN.put(KeyEvent.VK_DOWN, "Key_DownArrow");
        VK_TO_TOKEN.put(KeyEvent.VK_LEFT, "Key_LeftArrow");
        VK_TO_TOKEN.put(KeyEvent.VK_RIGHT, "Key_RightArrow");
        // Punctuation (QWERTY positions)
        VK_TO_TOKEN.put(KeyEvent.VK_BACK_QUOTE, "Key_Grave");
        VK_TO_TOKEN.put(KeyEvent.VK_MINUS, "Key_Minus");
        VK_TO_TOKEN.put(KeyEvent.VK_EQUALS, "Key_Equals");
        VK_TO_TOKEN.put(KeyEvent.VK_OPEN_BRACKET, "Key_LeftBracket");
        VK_TO_TOKEN.put(KeyEvent.VK_CLOSE_BRACKET, "Key_RightBracket");
        VK_TO_TOKEN.put(KeyEvent.VK_BACK_SLASH, "Key_BackSlash");
        VK_TO_TOKEN.put(KeyEvent.VK_SEMICOLON, "Key_SemiColon");
        VK_TO_TOKEN.put(KeyEvent.VK_QUOTE, "Key_Apostrophe");
        VK_TO_TOKEN.put(KeyEvent.VK_COMMA, "Key_Comma");
        VK_TO_TOKEN.put(KeyEvent.VK_PERIOD, "Key_Period");
        VK_TO_TOKEN.put(KeyEvent.VK_SLASH, "Key_Slash");
        // Dedicated # key (UK and some EU layouts only)
        VK_TO_TOKEN.put(KeyEvent.VK_NUMBER_SIGN, "Key_Hash");
        // Numpad operators
        VK_TO_TOKEN.put(KeyEvent.VK_ADD, "Key_Numpad_Add");
        VK_TO_TOKEN.put(KeyEvent.VK_SUBTRACT, "Key_Numpad_Subtract");
        VK_TO_TOKEN.put(KeyEvent.VK_MULTIPLY, "Key_Numpad_Multiply");
        VK_TO_TOKEN.put(KeyEvent.VK_DIVIDE, "Key_Numpad_Divide");
        VK_TO_TOKEN.put(KeyEvent.VK_DECIMAL, "Key_Numpad_Decimal");
        // Lock / system keys
        VK_TO_TOKEN.put(KeyEvent.VK_NUM_LOCK, "Key_NumLock");
        VK_TO_TOKEN.put(KeyEvent.VK_SCROLL_LOCK, "Key_ScrollLock");
        VK_TO_TOKEN.put(KeyEvent.VK_PAUSE, "Key_Pause");
        VK_TO_TOKEN.put(KeyEvent.VK_CONTEXT_MENU, "Key_Apps");
    }

    private KeyCaptureMapper() {
    }

    /**
     * Converts the provided {@link KeyEvent} into an optional token string representation.
     * The token is derived based on the key event's code, location, and platform-specific mappings.
     *
     * @param e the {@code KeyEvent} to convert; must not be {@code null}.
     * @return an {@code Optional} containing the token string if mapping is successful,
     * or an empty {@code Optional} if the event could not be resolved to a token.
     */
    public static Optional<String> fromKeyEvent(KeyEvent e) {
        return Optional.ofNullable(resolveToken(e));
    }


    /**
     * Determines if the provided {@link KeyEvent} corresponds exclusively to a modifier key.
     * The method checks if the key code of the event matches {@code KeyEvent.VK_SHIFT},
     * {@code KeyEvent.VK_CONTROL}, or {@code KeyEvent.VK_ALT}.
     *
     * @param e the {@code KeyEvent} to examine; must not be {@code null}.
     * @return {@code true} if the key event represents only a modifier key,
     * {@code false} otherwise.
     */
    public static boolean isModifierOnly(KeyEvent e) {
        int vk = e.getKeyCode();
        return vk == KeyEvent.VK_SHIFT
                || vk == KeyEvent.VK_CONTROL
                || vk == KeyEvent.VK_ALT;
    }

    private static String resolveToken(KeyEvent e) {
        return switch (e.getKeyCode()) {
            case KeyEvent.VK_SHIFT ->
                    e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT ? "Key_RightShift" : "Key_LeftShift";
            case KeyEvent.VK_CONTROL ->
                    e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT ? "Key_RightControl" : "Key_LeftControl";
            case KeyEvent.VK_ALT -> e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT ? "Key_RightAlt" : "Key_LeftAlt";
            default -> resolveLayoutAware(e.getKeyCode());
        };
    }

    /**
     * Resolves a layout-aware token string representation for a given virtual key code.
     * On Windows systems, this method attempts to map the virtual key to a scan code
     * and retrieve a corresponding token. If the scan code cannot be determined or no
     * token is found, the method falls back to a predefined virtual key (VK) to token mapping.
     * On non-Windows systems, the method directly uses the VK to token mapping.
     *
     * @param vk the virtual key code to resolve; must be a valid integer representing a key.
     * @return a string representing the token associated with the given virtual key code,
     * or {@code null} if no corresponding token could be resolved.
     */
    private static String resolveLayoutAware(int vk) {
        if (IS_WINDOWS) {
            try {
                int scan = WindowsScanResolver.getScanCode(vk);
                if (scan != 0) {
                    String token = SCAN_TO_TOKEN.get(scan);
                    if (token != null) return token;
                }
            } catch (Throwable ignored) {
                // JNA unavailable or MapVirtualKeyEx failed — fall through to VK map
            }
        } else if (IS_LINUX) {
            try {
                int scan = LinuxScanResolver.getScanCode(vk);
                if (scan != 0) {
                    String token = SCAN_TO_TOKEN.get(scan);
                    if (token != null) return token;
                }
            } catch (Throwable ignored) {
                // X11 unavailable (pure Wayland without XWayland) — fall through to VK map
            }
        }
        return VK_TO_TOKEN.get(vk);
    }

    // Linux X11: keycodes = PS/2 scan code + 8 (evdev driver offset)
    private static final class LinuxScanResolver {
        private static final X11 x11;
        private static final X11.Display display;

        static {
            X11 x11ref = null;
            X11.Display dispRef = null;
            try {
                x11ref = X11.INSTANCE;
                dispRef = x11ref.XOpenDisplay(null);
            } catch (Throwable ignored) {
            }
            x11 = x11ref;
            display = dispRef;
        }

        static int getScanCode(int vk) {
            if (x11 == null || display == null) return 0;
            long keysym = vkToKeysym(vk);
            if (keysym == 0) return 0;
            int keycode = x11.XKeysymToKeycode(display, new X11.KeySym(keysym)) & 0xFF;
            return keycode > 8 ? keycode - 8 : 0;
        }

        // Maps Java VK codes to X11 keysyms.
        // For letters: VK_A(65) = 'A'; XK_a(0x61) = 'a'; offset = +32.
        // For Latin-1 extended (é,è,à,ü,ö,ä,ß,...): Java VK = Unicode = XK keysym — default case.
        // For ASCII punctuation where Java kept the ASCII value: default case.
        // Explicit entries only for keys where Java renamed the VK constant away from the keysym value.
        private static long vkToKeysym(int vk) {
            if (vk >= KeyEvent.VK_A && vk <= KeyEvent.VK_Z) return vk + 32L;
            return switch (vk) {
                case KeyEvent.VK_QUOTE -> 0x27L;  // XK_apostrophe
                case KeyEvent.VK_BACK_QUOTE -> 0x60L;  // XK_grave
                case KeyEvent.VK_AMPERSAND -> 0x26L;  // XK_ampersand        (AZERTY 1)
                case KeyEvent.VK_QUOTEDBL -> 0x22L;  // XK_quotedbl         (AZERTY 3)
                case KeyEvent.VK_LEFT_PARENTHESIS -> 0x28L;  // XK_parenleft        (AZERTY 5)
                case KeyEvent.VK_RIGHT_PARENTHESIS -> 0x29L;  // XK_parenright       (AZERTY ))
                case KeyEvent.VK_UNDERSCORE -> 0x5FL;  // XK_underscore       (AZERTY 8)
                default -> vk;
            };
        }
    }

    /**
     * A utility class designed specifically for resolving Windows-specific scan codes
     * from virtual key codes (VK). This class is used in the context of
     * Windows systems to map a virtual key code to its corresponding
     * scan code utilizing the native User32 library.
     * <p>
     * This functionality is critical for correctly handling key events on Windows
     * platforms where scan codes are required for certain key mappings or
     * event processing workflows.
     * <p>
     * The class contains only static methods and constants, thereby making
     * its functionality accessible without requiring an instance of the class.
     * <p>
     * Key Features:
     * - Leverages the `MapVirtualKeyEx` function from the User32 library to perform
     * the VK-to-scan code mapping.
     * - Uses the `MAPVK_VK_TO_VSC` flag to facilitate the mapping from a virtual
     * key code to a scan code.
     */
    private static final class WindowsScanResolver {
        private static final int MAPVK_VK_TO_VSC = 0;

        static int getScanCode(int vk) {
            return User32.INSTANCE.MapVirtualKeyEx(vk, MAPVK_VK_TO_VSC, null);
        }
    }
}