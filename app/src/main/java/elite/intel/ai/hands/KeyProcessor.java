package elite.intel.ai.hands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.ThreadLocalRandom;

public class KeyProcessor {
    private static final Logger log = LogManager.getLogger(KeyProcessor.class);

    /**
     * Synthetic code base for left/right modifier keys that require platform-native
     * handling (above AWT's VK range of 0–0xFFFF).  Any keyCode >= NATIVE_BASE is
     * routed through NativeKeyInput instead of Robot.
     */
    public static final int NATIVE_BASE = 0x10000;
    private static final int NATIVE_CHARACTER_BASE = 0x20000;
    private static final int NATIVE_CHARACTER_LIMIT = NATIVE_CHARACTER_BASE + Character.MAX_VALUE;

    private final Robot robot;
    private final NativeKeyInput nativeKeyInput;

    static int nativeCharacterCode(char character) {
        return NATIVE_CHARACTER_BASE + character;
    }

    static boolean isNativeCharacterCode(int keyCode) {
        return keyCode >= NATIVE_CHARACTER_BASE && keyCode <= NATIVE_CHARACTER_LIMIT;
    }

    static char nativeCharacter(int keyCode) {
        if (!isNativeCharacterCode(keyCode)) {
            throw new IllegalArgumentException("Not a native character code: 0x" + Integer.toHexString(keyCode));
        }
        return (char) (keyCode - NATIVE_CHARACTER_BASE);
    }

    public static final int KEY_SPACE = KeyEvent.VK_SPACE;
    public static final int KEY_ENTER = KeyEvent.VK_ENTER;
    public static final int KEY_BACKSPACE = KeyEvent.VK_BACK_SPACE;
    public static final int KEY_TAB = KeyEvent.VK_TAB;
    public static final int KEY_ESCAPE = KeyEvent.VK_ESCAPE;
    public static final int KEY_UPARROW = KeyEvent.VK_UP;
    public static final int KEY_DOWNARROW = KeyEvent.VK_DOWN;
    public static final int KEY_LEFTARROW = KeyEvent.VK_LEFT;
    public static final int KEY_RIGHTARROW = KeyEvent.VK_RIGHT;
    public static final int KEY_DELETE = KeyEvent.VK_DELETE;
    public static final int KEY_F1 = KeyEvent.VK_F1;
    public static final int KEY_F2 = KeyEvent.VK_F2;
    public static final int KEY_F3 = KeyEvent.VK_F3;
    public static final int KEY_F4 = KeyEvent.VK_F4;
    public static final int KEY_F5 = KeyEvent.VK_F5;
    public static final int KEY_F6 = KeyEvent.VK_F6;
    public static final int KEY_F7 = KeyEvent.VK_F7;
    public static final int KEY_F8 = KeyEvent.VK_F8;
    public static final int KEY_F9 = KeyEvent.VK_F9;
    public static final int KEY_F10 = KeyEvent.VK_F10;
    public static final int KEY_F11 = KeyEvent.VK_F11;
    public static final int KEY_F12 = KeyEvent.VK_F12;
    public static final int KEY_PRINTSCREEN = KeyEvent.VK_PRINTSCREEN;
    public static final int KEY_SCROLLLOCK = KeyEvent.VK_SCROLL_LOCK;
    public static final int KEY_PAUSE = KeyEvent.VK_PAUSE;
    public static final int KEY_INSERT = KeyEvent.VK_INSERT;
    public static final int KEY_HOME = KeyEvent.VK_HOME;
    public static final int KEY_PAGEUP = KeyEvent.VK_PAGE_UP;
    public static final int KEY_PAGEDOWN = KeyEvent.VK_PAGE_DOWN;
    public static final int KEY_END = KeyEvent.VK_END;
    public static final int KEY_CAPSLOCK = KeyEvent.VK_CAPS_LOCK;
    public static final int KEY_NUMLOCK = KeyEvent.VK_NUM_LOCK;
    public static final int KEY_NUMSLASH = KeyEvent.VK_DIVIDE;
    public static final int KEY_NUMASTERISK = KeyEvent.VK_MULTIPLY;
    public static final int KEY_NUMPAD_SUBTRACT = KeyEvent.VK_SUBTRACT;
    public static final int KEY_NUMPAD_ADD = KeyEvent.VK_ADD;
    public static final int KEY_NUMPAD_1 = KeyEvent.VK_NUMPAD1;
    public static final int KEY_NUMPAD_2 = KeyEvent.VK_NUMPAD2;
    public static final int KEY_NUMPAD_3 = KeyEvent.VK_NUMPAD3;
    public static final int KEY_NUMPAD_4 = KeyEvent.VK_NUMPAD4;
    public static final int KEY_NUMPAD_5 = KeyEvent.VK_NUMPAD5;
    public static final int KEY_NUMPAD_6 = KeyEvent.VK_NUMPAD6;
    public static final int KEY_NUMPAD_7 = KeyEvent.VK_NUMPAD7;
    public static final int KEY_NUMPAD_8 = KeyEvent.VK_NUMPAD8;
    public static final int KEY_NUMPAD_9 = KeyEvent.VK_NUMPAD9;
    public static final int KEY_NUMPAD_0 = KeyEvent.VK_NUMPAD0;
    public static final int KEY_NUMPERIOD = KeyEvent.VK_DECIMAL;
    public static final int KEY_NUMENTER = NATIVE_BASE + 11;  // Numpad Enter (E0 1C, distinct from main Enter)
    public static final int KEY_NUMEQUALS = KeyEvent.VK_EQUALS;
    public static final int KEY_NUMBACKSPACE = KeyEvent.VK_BACK_SPACE;
    public static final int KEY_NUMTAB = KeyEvent.VK_TAB;
    public static final int KEY_NUMCLEAR = KeyEvent.VK_CLEAR;
    public static final int KEY_NUMSLASHPERIOD = KeyEvent.VK_DIVIDE;
    public static final int KEY_LESSTHAN = NATIVE_BASE + 10; // ISO 102nd key (<> on DE/EU keyboards, scan 0x56)
    // KEY_NUMENTER already defined above as NATIVE_BASE + 11
    public static final int KEY_ADIAERESIS = NATIVE_BASE + 12; // ä (DE/EU) → scan 0x28
    public static final int KEY_ODIAERESIS = NATIVE_BASE + 13; // ö (DE/EU) → scan 0x27
    public static final int KEY_UDIAERESIS = NATIVE_BASE + 14; // ü (DE/EU) → scan 0x1A
    public static final int KEY_SSHARP = NATIVE_BASE + 15; // ß (DE)    → scan 0x0C
    public static final int KEY_DEAD_ACUTE = NATIVE_BASE + 16; // ´ (DE)    → scan 0x0D
    public static final int KEY_EACUTE = NATIVE_BASE + 17; // é (FR AZERTY, number-row position 2)
    public static final int KEY_EGRAVE = NATIVE_BASE + 18; // è (FR AZERTY, number-row position 7)
    public static final int KEY_AGRAVE = NATIVE_BASE + 19; // à (FR AZERTY, number-row position 0)
    public static final int KEY_UGRAVE = NATIVE_BASE + 20; // ù (FR AZERTY, ù key position)
    public static final int KEY_CCEDILLA = NATIVE_BASE + 21; // ç (FR AZERTY, number-row position 9)
    public static final int KEY_NTILDE = NATIVE_BASE + 22;   // ñ (ES Spanish, home-row position after L)
    static final int NATIVE_NUMPAD_DIVIDE = NATIVE_BASE + 23;
    static final int NATIVE_NUMPAD_MULTIPLY = NATIVE_BASE + 24;
    static final int NATIVE_NUMPAD_DECIMAL = NATIVE_BASE + 25;
    static final int NATIVE_NUMPAD_ADD = NATIVE_BASE + 26;
    static final int NATIVE_NUMPAD_SUBTRACT = NATIVE_BASE + 27;
    public static final int KEY_LEFTCONTROL = NATIVE_BASE + 1;
    public static final int KEY_RIGHTCONTROL = NATIVE_BASE + 2;
    public static final int KEY_LEFTSHIFT = NATIVE_BASE + 3;
    public static final int KEY_RIGHTSHIFT = NATIVE_BASE + 4;
    public static final int KEY_LEFTALT = NATIVE_BASE + 5;
    public static final int KEY_RIGHTALT = NATIVE_BASE + 6;
    public static final int KEY_LEFTSUPER = NATIVE_BASE + 7;
    public static final int KEY_RIGHTSUPER = NATIVE_BASE + 8;
    public static final int KEY_MENU = NATIVE_BASE + 9;
    public static final int KEY_LEFTBRACKET = KeyEvent.VK_OPEN_BRACKET;
    public static final int KEY_BACKSLASH = KeyEvent.VK_BACK_SLASH;
    public static final int KEY_RIGHTBRACKET = KeyEvent.VK_CLOSE_BRACKET;
    public static final int KEY_GRAVEACCENT = KeyEvent.VK_BACK_QUOTE;
    // Letters A-Z
    public static final int KEY_A = KeyEvent.VK_A;
    public static final int KEY_B = KeyEvent.VK_B;
    public static final int KEY_C = KeyEvent.VK_C;
    public static final int KEY_D = KeyEvent.VK_D;
    public static final int KEY_E = KeyEvent.VK_E;
    public static final int KEY_F = KeyEvent.VK_F;
    public static final int KEY_G = KeyEvent.VK_G;
    public static final int KEY_H = KeyEvent.VK_H;
    public static final int KEY_I = KeyEvent.VK_I;
    public static final int KEY_J = KeyEvent.VK_J;
    public static final int KEY_K = KeyEvent.VK_K;
    public static final int KEY_L = KeyEvent.VK_L;
    public static final int KEY_M = KeyEvent.VK_M;
    public static final int KEY_N = KeyEvent.VK_N;
    public static final int KEY_O = KeyEvent.VK_O;
    public static final int KEY_P = KeyEvent.VK_P;
    public static final int KEY_Q = KeyEvent.VK_Q;
    public static final int KEY_R = KeyEvent.VK_R;
    public static final int KEY_S = KeyEvent.VK_S;
    public static final int KEY_T = KeyEvent.VK_T;
    public static final int KEY_U = KeyEvent.VK_U;
    public static final int KEY_V = KeyEvent.VK_V;
    public static final int KEY_W = KeyEvent.VK_W;
    public static final int KEY_X = KeyEvent.VK_X;
    public static final int KEY_Y = KeyEvent.VK_Y;
    public static final int KEY_Z = KeyEvent.VK_Z;

    // Number row 0-9 (main keyboard)
    public static final int KEY_0 = KeyEvent.VK_0;
    public static final int KEY_1 = KeyEvent.VK_1;
    public static final int KEY_2 = KeyEvent.VK_2;
    public static final int KEY_3 = KeyEvent.VK_3;
    public static final int KEY_4 = KeyEvent.VK_4;
    public static final int KEY_5 = KeyEvent.VK_5;
    public static final int KEY_6 = KeyEvent.VK_6;
    public static final int KEY_7 = KeyEvent.VK_7;
    public static final int KEY_8 = KeyEvent.VK_8;
    public static final int KEY_9 = KeyEvent.VK_9;

    // Punctuation keys on standard US QWERTY
    public static final int KEY_MINUS = KeyEvent.VK_MINUS;        // '-'
    public static final int KEY_EQUALS = KeyEvent.VK_EQUALS;       // '='
    public static final int KEY_SEMICOLON = KeyEvent.VK_SEMICOLON;    // ';'
    public static final int KEY_APOSTROPHE = KeyEvent.VK_QUOTE;   // '\''
    public static final int KEY_COMMA = KeyEvent.VK_COMMA;        // ','
    public static final int KEY_PERIOD = KeyEvent.VK_PERIOD;       // '.'
    public static final int KEY_SLASH = KeyEvent.VK_SLASH;        // '/'
    public static final int KEY_TILDE = KeyEvent.VK_BACK_QUOTE;        // '`' (same as GRAVE)

    // Symbols requiring Shift (for reference; same base codes as above)
    public static final int KEY_EXCLAMATION = KeyEvent.VK_1;   // '!' -> Shift+1
    public static final int KEY_AT = KeyEvent.VK_2;            // '@' -> Shift+2
    public static final int KEY_HASH = KeyEvent.VK_3;          // '#' -> Shift+3
    public static final int KEY_DOLLAR = KeyEvent.VK_4;        // '$' -> Shift+4
    public static final int KEY_PERCENT = KeyEvent.VK_5;       // '%' -> Shift+5
    public static final int KEY_CARET = KeyEvent.VK_6;         // '^' -> Shift+6
    public static final int KEY_AMPERSAND = KeyEvent.VK_7;     // '&' -> Shift+7
    public static final int KEY_ASTERISK = KeyEvent.VK_8;      // '*' -> Shift+8
    public static final int KEY_LEFTPAREN = KeyEvent.VK_9;    // '(' -> Shift+9
    public static final int KEY_RIGHTPAREN = KeyEvent.VK_0;   // ')' -> Shift+0
    public static final int KEY_UNDERSCORE = KeyEvent.VK_MINUS;    // '_' -> Shift+'-'
    public static final int KEY_PLUS = KeyEvent.VK_EQUALS;      // '+' -> Shift+'='
    public static final int KEY_COLON = KeyEvent.VK_SEMICOLON;  // ':' -> Shift+';'
    public static final int KEY_QUOTE = KeyEvent.VK_QUOTE;      // '"' -> Shift+'\''
    public static final int KEY_LESS = KeyEvent.VK_COMMA;       // '<' -> Shift+','
    public static final int KEY_GREATER = KeyEvent.VK_PERIOD;   // '>' -> Shift+'.'
    public static final int KEY_QUESTION = KeyEvent.VK_SLASH;   // '?' -> Shift+'/'
    public static final int KEY_TILDE_SHIFT = KeyEvent.VK_BACK_QUOTE;  // '~' -> Shift+'`'
    private static KeyProcessor instance;

    public static KeyProcessor getInstance() {
        if (instance == null) {
            try {
                instance = new KeyProcessor();
            } catch (AWTException e) {
                throw new RuntimeException("Failed to initialize KeyboardProcessor", e);
            }
        }
        return instance;
    }

    private KeyProcessor() throws AWTException {
        this.robot = new Robot();
        robot.setAutoDelay(20);
        this.nativeKeyInput = NativeKeyInputFactory.create(robot);
    }

    private boolean isNative(int keyCode) {
        // NATIVE_BASE codes always go native.
        // nativeKeyInput logs a warning if it has no mapping.
        return keyCode >= NATIVE_BASE || nativeKeyInput.handles(keyCode);
    }

    public void pressKey(int keyCode) {
        if (isNative(keyCode)) {
            log.debug("[proc] pressKey 0x{} → native (SendInput)", Integer.toHexString(keyCode));
            nativeKeyInput.keyDown(keyCode);
            robot.delay(jitter());
            nativeKeyInput.keyUp(keyCode);
        } else {
            log.debug("[proc] pressKey 0x{} → Robot.keyPress", Integer.toHexString(keyCode));
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
        }
    }

    public void pressAndHoldKey(int keyCode, int holdTime) {
        if (isNative(keyCode)) {
            log.debug("[proc] pressAndHoldKey 0x{} {}ms → native", Integer.toHexString(keyCode), holdTime);
            nativeKeyInput.keyDown(keyCode);
            robot.delay(holdTime);
            nativeKeyInput.keyUp(keyCode);
        } else {
            log.debug("[proc] pressAndHoldKey 0x{} {}ms → Robot", Integer.toHexString(keyCode), holdTime);
            robot.keyPress(keyCode);
            robot.delay(holdTime);
            robot.keyRelease(keyCode);
        }
    }

    public void holdKey(int keyCode) {
        if (isNative(keyCode)) {
            log.debug("[proc] holdKey 0x{} → native keyDown", Integer.toHexString(keyCode));
            nativeKeyInput.keyDown(keyCode);
            if (keyCode >= NATIVE_BASE) {
                // Modifier keys need a settling delay so the game's DirectInput poller sees
                // the modifier as held before the main key fires.
                robot.delay(jitter());
            }
        } else {
            log.debug("[proc] holdKey 0x{} → Robot.keyPress", Integer.toHexString(keyCode));
            robot.keyPress(keyCode);
        }
    }

    public void releaseKey(int keyCode) {
        if (isNative(keyCode)) {
            log.debug("[proc] releaseKey 0x{} → native keyUp", Integer.toHexString(keyCode));
            nativeKeyInput.keyUp(keyCode);
        } else {
            log.debug("[proc] releaseKey 0x{} → Robot.keyRelease", Integer.toHexString(keyCode));
            robot.keyRelease(keyCode);
        }
    }

    public void pressKeys(int... keyCodes) {
        for (int keyCode : keyCodes) {
            if (isNative(keyCode)) {
                nativeKeyInput.keyDown(keyCode);
                robot.delay(jitter());
            }
            else robot.keyPress(keyCode);
        }
        for (int i = keyCodes.length - 1; i >= 0; i--) {
            if (isNative(keyCodes[i])) nativeKeyInput.keyUp(keyCodes[i]);
            else robot.keyRelease(keyCodes[i]);
        }
    }

    public void pressKeyCombo(int... keyCodes) {
        try {
            for (int keyCode : keyCodes) {
                if (isNative(keyCode)) {
                    nativeKeyInput.keyDown(keyCode);
                    robot.delay(30);
                }
                else robot.keyPress(keyCode);
            }
            Thread.sleep(100);
            for (int i = keyCodes.length - 1; i >= 0; i--) {
                if (isNative(keyCodes[i])) nativeKeyInput.keyUp(keyCodes[i]);
                else robot.keyRelease(keyCodes[i]);
            }
        } catch (InterruptedException e) {
            for (int i = keyCodes.length - 1; i >= 0; i--) {
                if (isNative(keyCodes[i])) nativeKeyInput.keyUp(keyCodes[i]);
                else robot.keyRelease(keyCodes[i]);
            }
            Thread.currentThread().interrupt();
        }
    }

    private int jitter() {
        return ThreadLocalRandom.current().nextInt(43, 89);
    }

    public void enterText(String text) {
        for (char c : text.toCharArray()) {
            if (!nativeKeyInput.typeChar(c)) {
                robot.keyPress(KeyEvent.getExtendedKeyCodeForChar(c));
                robot.keyRelease(KeyEvent.getExtendedKeyCodeForChar(c));
            }
        }
    }
}
