package elite.intel.ai.hands;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.ptr.IntByReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import static elite.intel.ai.hands.KeyProcessor.NATIVE_BASE;


/**
 * The {@code LinuxX11NativeKeyInput} class provides a native implementation of the {@code NativeKeyInput} interface
 * using the X11 library and its XTest extension on Linux. This implementation directly interacts with the X11
 * server to inject key events and perform low-level key handling. The class is designed for advanced keyboard
 * control, including support for modifier keys (like Shift or Control) through X11's native APIs.
 * <p>
 * It sends <em>every</em> key, the plain AWT ones included, over its one display connection, and never leaves
 * any to {@link java.awt.Robot}. Robot injects over AWT's own connection, and a compositor that gates XTEST per
 * X11 client (KWin, on an Xwayland built with libei) asks the commander's permission once for each connection:
 * splitting a chord across two cost a second prompt, and the modifier was lost while it was open.
 */
class LinuxX11NativeKeyInput implements NativeKeyInput {
    private static final Logger log = LogManager.getLogger(LinuxX11NativeKeyInput.class);

    /**
     * Minimal JNA wrapper for XGetKeyboardMapping (libX11).
     * Used to determine the modifier level (plain vs Shift) for a given keysym,
     * so typeChar() can inject the correct modifier before the key event.
     */
    interface X11Keyboard extends Library {
        X11Keyboard INSTANCE = Native.load("X11", X11Keyboard.class);

        // Returns a pointer to (keycode_count * symsPerKeycode) KeySym values; caller must XFree it.
        Pointer XGetKeyboardMapping(X11.Display display, byte first_keycode, int keycode_count,
                                    IntByReference symsPerKeycode_return);

        int XFree(Pointer data);
    }

    /**
     * Minimal JNA interface for the XTest extension in libXtst.
     * XTestFakeKeyEvent injects a key event directly into the X input stream.
     */
    interface XTst extends Library {
        XTst INSTANCE = Native.load("Xtst", XTst.class);

        /**
         * @param display  the X11 display connection
         * @param keycode  hardware keycode (NOT keysym; use XKeysymToKeycode to convert)
         * @param is_press 1 for press, 0 for release
         * @param delay    event delay in milliseconds (0 = immediate)
         */
        int XTestFakeKeyEvent(X11.Display display, int keycode, int is_press, NativeLong delay);
    }

    // X11 keysyms for left/right modifiers
    // Reference: /usr/include/X11/keysymdef.h
    private static final Map<Integer, Long> KEYSYM_MAP = new HashMap<>();

    static {
        KEYSYM_MAP.put(NATIVE_BASE + 1, 0xFFE3L);  // KEY_LEFTCONTROL  → XK_Control_L
        KEYSYM_MAP.put(NATIVE_BASE + 2, 0xFFE4L);  // KEY_RIGHTCONTROL → XK_Control_R
        KEYSYM_MAP.put(NATIVE_BASE + 3, 0xFFE1L);  // KEY_LEFTSHIFT    → XK_Shift_L
        KEYSYM_MAP.put(NATIVE_BASE + 4, 0xFFE2L);  // KEY_RIGHTSHIFT   → XK_Shift_R
        KEYSYM_MAP.put(NATIVE_BASE + 5, 0xFFE9L);  // KEY_LEFTALT      → XK_Alt_L
        KEYSYM_MAP.put(NATIVE_BASE + 6, 0xFFEAL);  // KEY_RIGHTALT     → XK_Alt_R
        KEYSYM_MAP.put(NATIVE_BASE + 7, 0xFFEBL);  // KEY_LEFTSUPER    → XK_Super_L
        KEYSYM_MAP.put(NATIVE_BASE + 8, 0xFFECL);  // KEY_RIGHTSUPER   → XK_Super_R
        KEYSYM_MAP.put(NATIVE_BASE + 9, 0xFF67L);  // KEY_MENU         → XK_Menu
        KEYSYM_MAP.put(NATIVE_BASE + 10, 0x003CL);  // KEY_LESSTHAN     → XK_less (ISO 102nd key)
        KEYSYM_MAP.put(NATIVE_BASE + 11, 0xFF8DL);  // KEY_NUMENTER     → XK_KP_Enter
        KEYSYM_MAP.put(NATIVE_BASE + 12, 0x00E4L);  // KEY_ADIAERESIS   → XK_adiaeresis (ä)
        KEYSYM_MAP.put(NATIVE_BASE + 13, 0x00F6L);  // KEY_ODIAERESIS   → XK_odiaeresis (ö)
        KEYSYM_MAP.put(NATIVE_BASE + 14, 0x00FCL);  // KEY_UDIAERESIS   → XK_udiaeresis (ü)
        KEYSYM_MAP.put(NATIVE_BASE + 15, 0x00DFL);  // KEY_SSHARP       → XK_ssharp (ß)
        KEYSYM_MAP.put(NATIVE_BASE + 16, 0xFE51L);  // KEY_DEAD_ACUTE   → XK_dead_acute (´)
        KEYSYM_MAP.put(NATIVE_BASE + 17, 0x00E9L);  // KEY_EACUTE   → XK_eacute (é)
        KEYSYM_MAP.put(NATIVE_BASE + 18, 0x00E8L);  // KEY_EGRAVE   → XK_egrave (è)
        KEYSYM_MAP.put(NATIVE_BASE + 19, 0x00E0L);  // KEY_AGRAVE   → XK_agrave (à)
        KEYSYM_MAP.put(NATIVE_BASE + 20, 0x00F9L);  // KEY_UGRAVE   → XK_ugrave (ù)
        KEYSYM_MAP.put(NATIVE_BASE + 21, 0x00E7L);  // KEY_CCEDILLA → XK_ccedilla (ç)
        KEYSYM_MAP.put(NATIVE_BASE + 22, 0x00F1L);  // KEY_NTILDE   → XK_ntilde (ñ)
        KEYSYM_MAP.put(KeyProcessor.NATIVE_NUMPAD_DIVIDE, 0xFFAFL);   // XK_KP_Divide
        KEYSYM_MAP.put(KeyProcessor.NATIVE_NUMPAD_MULTIPLY, 0xFFAAL); // XK_KP_Multiply
        KEYSYM_MAP.put(KeyProcessor.NATIVE_NUMPAD_DECIMAL, 0xFFAEL);  // XK_KP_Decimal
        KEYSYM_MAP.put(KeyProcessor.NATIVE_NUMPAD_ADD, 0xFFABL);      // XK_KP_Add
        KEYSYM_MAP.put(KeyProcessor.NATIVE_NUMPAD_SUBTRACT, 0xFFADL); // XK_KP_Subtract
    }

    /**
     * AWT virtual keys → the X11 keysym {@link java.awt.Robot} presses for each, so a key sent here reaches the
     * same physical key it reached through Robot, on any layout. Every {@code KEY_*} in {@link KeyProcessor}
     * below {@link KeyProcessor#NATIVE_BASE} needs an entry, or it falls back to Robot's connection.
     */
    private static final Map<Integer, Long> VK_KEYSYM_MAP = new HashMap<>();

    static {
        for (int vk = KeyEvent.VK_A; vk <= KeyEvent.VK_Z; vk++) {
            VK_KEYSYM_MAP.put(vk, (long) Character.toLowerCase((char) vk));  // XK_a .. XK_z
        }
        for (int vk = KeyEvent.VK_0; vk <= KeyEvent.VK_9; vk++) {
            VK_KEYSYM_MAP.put(vk, (long) vk);                                 // XK_0 .. XK_9
        }
        for (int i = 0; i < 12; i++) {
            VK_KEYSYM_MAP.put(KeyEvent.VK_F1 + i, 0xFFBEL + i);               // XK_F1 .. XK_F12
        }
        for (int i = 0; i < 10; i++) {
            VK_KEYSYM_MAP.put(KeyEvent.VK_NUMPAD0 + i, 0xFFB0L + i);          // XK_KP_0 .. XK_KP_9
        }
        VK_KEYSYM_MAP.put(KeyEvent.VK_SPACE, 0x0020L);          // XK_space
        VK_KEYSYM_MAP.put(KeyEvent.VK_ENTER, 0xFF0DL);          // XK_Return
        VK_KEYSYM_MAP.put(KeyEvent.VK_BACK_SPACE, 0xFF08L);     // XK_BackSpace
        VK_KEYSYM_MAP.put(KeyEvent.VK_TAB, 0xFF09L);            // XK_Tab
        VK_KEYSYM_MAP.put(KeyEvent.VK_CLEAR, 0xFF0BL);          // XK_Clear
        VK_KEYSYM_MAP.put(KeyEvent.VK_ESCAPE, 0xFF1BL);         // XK_Escape
        VK_KEYSYM_MAP.put(KeyEvent.VK_PAUSE, 0xFF13L);          // XK_Pause
        VK_KEYSYM_MAP.put(KeyEvent.VK_SCROLL_LOCK, 0xFF14L);    // XK_Scroll_Lock
        VK_KEYSYM_MAP.put(KeyEvent.VK_PRINTSCREEN, 0xFF61L);    // XK_Print
        VK_KEYSYM_MAP.put(KeyEvent.VK_INSERT, 0xFF63L);         // XK_Insert
        VK_KEYSYM_MAP.put(KeyEvent.VK_DELETE, 0xFFFFL);         // XK_Delete
        VK_KEYSYM_MAP.put(KeyEvent.VK_HOME, 0xFF50L);           // XK_Home
        VK_KEYSYM_MAP.put(KeyEvent.VK_LEFT, 0xFF51L);           // XK_Left
        VK_KEYSYM_MAP.put(KeyEvent.VK_UP, 0xFF52L);             // XK_Up
        VK_KEYSYM_MAP.put(KeyEvent.VK_RIGHT, 0xFF53L);          // XK_Right
        VK_KEYSYM_MAP.put(KeyEvent.VK_DOWN, 0xFF54L);           // XK_Down
        VK_KEYSYM_MAP.put(KeyEvent.VK_PAGE_UP, 0xFF55L);        // XK_Page_Up
        VK_KEYSYM_MAP.put(KeyEvent.VK_PAGE_DOWN, 0xFF56L);      // XK_Page_Down
        VK_KEYSYM_MAP.put(KeyEvent.VK_END, 0xFF57L);            // XK_End
        VK_KEYSYM_MAP.put(KeyEvent.VK_NUM_LOCK, 0xFF7FL);       // XK_Num_Lock
        VK_KEYSYM_MAP.put(KeyEvent.VK_CAPS_LOCK, 0xFFE5L);      // XK_Caps_Lock
        VK_KEYSYM_MAP.put(KeyEvent.VK_MULTIPLY, 0xFFAAL);       // XK_KP_Multiply
        VK_KEYSYM_MAP.put(KeyEvent.VK_ADD, 0xFFABL);            // XK_KP_Add
        VK_KEYSYM_MAP.put(KeyEvent.VK_SUBTRACT, 0xFFADL);       // XK_KP_Subtract
        VK_KEYSYM_MAP.put(KeyEvent.VK_DECIMAL, 0xFFAEL);        // XK_KP_Decimal
        VK_KEYSYM_MAP.put(KeyEvent.VK_DIVIDE, 0xFFAFL);         // XK_KP_Divide
        VK_KEYSYM_MAP.put(KeyEvent.VK_MINUS, 0x002DL);          // XK_minus
        VK_KEYSYM_MAP.put(KeyEvent.VK_EQUALS, 0x003DL);         // XK_equal
        VK_KEYSYM_MAP.put(KeyEvent.VK_SEMICOLON, 0x003BL);      // XK_semicolon
        VK_KEYSYM_MAP.put(KeyEvent.VK_QUOTE, 0x0027L);          // XK_apostrophe
        VK_KEYSYM_MAP.put(KeyEvent.VK_COMMA, 0x002CL);          // XK_comma
        VK_KEYSYM_MAP.put(KeyEvent.VK_PERIOD, 0x002EL);         // XK_period
        VK_KEYSYM_MAP.put(KeyEvent.VK_SLASH, 0x002FL);          // XK_slash
        VK_KEYSYM_MAP.put(KeyEvent.VK_BACK_QUOTE, 0x0060L);     // XK_grave
        VK_KEYSYM_MAP.put(KeyEvent.VK_OPEN_BRACKET, 0x005BL);   // XK_bracketleft
        VK_KEYSYM_MAP.put(KeyEvent.VK_BACK_SLASH, 0x005CL);     // XK_backslash
        VK_KEYSYM_MAP.put(KeyEvent.VK_CLOSE_BRACKET, 0x005DL);  // XK_bracketright
    }

    /**
     * Seam for tests: whether an AWT virtual key is sent over this connection rather than Robot's.
     */
    static boolean mapsVirtualKey(int vk) {
        return VK_KEYSYM_MAP.containsKey(vk);
    }

    private final X11 x11;
    private final XTst xtst;
    private final X11Keyboard x11keyboard;
    private final X11.Display display;
    // Cache keysym → hardware keycode (XKeysymToKeycode is not free; returns byte)
    private final Map<Integer, Byte> keycodeCache = new HashMap<>();
    private final boolean available;
    private byte shiftKeycode;

    LinuxX11NativeKeyInput() {
        X11 x11ref = null;
        XTst xtstRef = null;
        X11Keyboard x11kbRef = null;
        X11.Display disp = null;
        boolean ok = false;
        byte shiftKc = 0;

        try {
            x11ref = X11.INSTANCE;
            xtstRef = XTst.INSTANCE;
            x11kbRef = X11Keyboard.INSTANCE;
            disp = x11ref.XOpenDisplay(null);
            if (disp == null) {
                log.warn("XOpenDisplay returned null - likely pure Wayland session. " +
                        "Left/right modifier distinction will fall back to generic Robot keys.");
            } else {
                // Pre-populate keycode cache for all known synthetic codes
                for (Map.Entry<Integer, Long> entry : KEYSYM_MAP.entrySet()) {
                    byte kc = x11ref.XKeysymToKeycode(disp, new X11.KeySym(entry.getValue()));
                    if (kc == 0) {
                        log.warn("XKeysymToKeycode returned 0 for keysym 0x{} - key may not be mapped in current X keymap",
                                Long.toHexString(entry.getValue()));
                    }
                    keycodeCache.put(entry.getKey(), kc);
                }
                shiftKc = x11ref.XKeysymToKeycode(disp, new X11.KeySym(0xFFE1L)); // XK_Shift_L
                ok = true;
                log.debug("LinuxX11NativeKeyInput initialised, {} keycodes mapped", keycodeCache.size());
            }
        } catch (UnsatisfiedLinkError e) {
            log.warn("libXtst not available: {}. Left/right modifier distinction unavailable.", e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to initialise X11 native key input: {}", e.getMessage());
        }

        this.x11 = x11ref;
        this.xtst = xtstRef;
        this.x11keyboard = x11kbRef;
        this.display = disp;
        this.available = ok;
        this.shiftKeycode = shiftKc;
    }

    boolean isAvailable() {
        return available;
    }

    @Override
    public boolean handles(int keyCode) {
        return KEYSYM_MAP.containsKey(keyCode) || VK_KEYSYM_MAP.containsKey(keyCode)
                || KeyProcessor.isNativeCharacterCode(keyCode);
    }

    @Override
    public void keyDown(int syntheticCode) {
        fakeKeyEvent(syntheticCode, true);
    }

    @Override
    public void keyUp(int syntheticCode) {
        fakeKeyEvent(syntheticCode, false);
    }

    // Xlib is not thread-safe on one display, and the action lane runs commands on their own threads.
    private synchronized void fakeKeyEvent(int syntheticCode, boolean press) {
        Long vkKeysym = VK_KEYSYM_MAP.get(syntheticCode);
        if (vkKeysym != null) {
            // Resolved per press, as Robot does, so a layout switched mid-session is followed.
            byte keycode = x11.XKeysymToKeycode(display, new X11.KeySym(vkKeysym));
            if (keycode == 0) {
                log.warn("No X11 keycode for virtual key 0x{} (keysym 0x{})",
                        Integer.toHexString(syntheticCode), Long.toHexString(vkKeysym));
                return;
            }
            xtst.XTestFakeKeyEvent(display, keycode & 0xFF, press ? 1 : 0, new NativeLong(0));
            x11.XFlush(display);
            return;
        }
        if (KeyProcessor.isNativeCharacterCode(syntheticCode)) {
            char character = KeyProcessor.nativeCharacter(syntheticCode);
            long keysym = characterKeysym(character);
            byte keycode = x11.XKeysymToKeycode(display, new X11.KeySym(keysym));
            if (keycode == 0) {
                long deadKeysym = deadKeysym(character);
                if (deadKeysym != 0) {
                    keysym = deadKeysym;
                    keycode = x11.XKeysymToKeycode(display, new X11.KeySym(keysym));
                }
            }
            if (keycode == 0) {
                log.warn("No X11 keycode for Frontier character '{}' (keysym 0x{})",
                        character, Long.toHexString(keysym));
                return;
            }
            xtst.XTestFakeKeyEvent(display, keycode & 0xFF, press ? 1 : 0, new NativeLong(0));
            x11.XFlush(display);
            return;
        }

        Byte keycode = keycodeCache.get(syntheticCode);
        if (keycode == null) {
            log.warn("No X11 keycode cached for synthetic code 0x{}", Integer.toHexString(syntheticCode));
            return;
        }
        if (keycode == 0) {
            log.warn("X11 keycode is 0 for synthetic code 0x{} - skipping", Integer.toHexString(syntheticCode));
            return;
        }
        xtst.XTestFakeKeyEvent(display, keycode & 0xFF, press ? 1 : 0, new NativeLong(0));
        x11.XFlush(display);
    }

    private static long characterKeysym(char character) {
        return character < 0x100 ? character : 0x01000000L | character;
    }

    private static long deadKeysym(char character) {
        return switch (character) {
            case '`' -> 0xFE50L; // XK_dead_grave
            case '´' -> 0xFE51L; // XK_dead_acute
            case '^' -> 0xFE52L; // XK_dead_circumflex
            case '~' -> 0xFE53L; // XK_dead_tilde
            case '¨' -> 0xFE57L; // XK_dead_diaeresis
            default -> 0;
        };
    }


    @Override
    public synchronized boolean typeChar(char c) {
        if (!available) return false;

        // X11 keysym: Latin-1 range (0x00–0xFF) maps directly; beyond that use the Unicode escape.
        long keysym = (c < 0x100) ? (long) c : (0x01000000L | c);

        byte keycode = x11.XKeysymToKeycode(display, new X11.KeySym(keysym));
        if (keycode == 0) {
            log.debug("XKeysymToKeycode returned 0 for char '{}' (keysym 0x{}) - falling back to Robot",
                    c, Long.toHexString(keysym));
            return false;
        }

        boolean needsShift = false;
        IntByReference symsPerKeycode = new IntByReference();
        Pointer keysymTable = x11keyboard.XGetKeyboardMapping(display, keycode, 1, symsPerKeycode);
        try {
            int n = symsPerKeycode.getValue();
            if (n >= 1) {
                // Each KeySym is a native long (4 bytes on 32-bit, 8 bytes on 64-bit)
                long level0 = readNativeLong(keysymTable, 0);
                if (level0 != keysym && n >= 2) {
                    long level1 = readNativeLong(keysymTable, 1);
                    if (level1 == keysym) {
                        needsShift = true;
                    } else {
                        // AltGr or other modifier required but not handled, fall back to Robot
                        return false;
                    }
                }
            }
        } finally {
            x11keyboard.XFree(keysymTable);
        }

        if (needsShift && shiftKeycode != 0) {
            xtst.XTestFakeKeyEvent(display, shiftKeycode & 0xFF, 1, new NativeLong(0));
        }
        xtst.XTestFakeKeyEvent(display, keycode & 0xFF, 1, new NativeLong(0));
        xtst.XTestFakeKeyEvent(display, keycode & 0xFF, 0, new NativeLong(0));
        if (needsShift && shiftKeycode != 0) {
            xtst.XTestFakeKeyEvent(display, shiftKeycode & 0xFF, 0, new NativeLong(0));
        }
        x11.XFlush(display);
        return true;
    }

    private static long readNativeLong(Pointer p, int index) {
        int offset = index * Native.LONG_SIZE;
        return (Native.LONG_SIZE == 8) ? p.getLong(offset) : (p.getInt(offset) & 0xFFFFFFFFL);
    }
}
