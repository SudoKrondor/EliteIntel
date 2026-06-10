package elite.intel.ai.hands;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static elite.intel.ai.hands.KeyProcessor.NATIVE_BASE;

/**
 * Windows implementation of NativeKeyInput using user32.dll SendInput.
 *
 * Uses KEYEVENTF_SCANCODE with PS/2 Set-1 hardware scan codes for all keys.
 * This is required because DirectInput (used by Elite Dangerous) identifies keys
 * by hardware scan code, not VK code. Robot-based VK events are ignored or
 * misidentified by DirectInput, especially for numpad and extended keys.
 *
 * Extended keys (right-side modifiers, navigation cluster, numpad /, Win, Apps,
 * PrintScreen) use KEYEVENTF_EXTENDEDKEY alongside KEYEVENTF_SCANCODE to produce
 * the correct E0-prefixed scan code sequence.
 *
 * Keys not present in SCAN_MAP (e.g. VK_PAUSE, VK_CLEAR) are not handled here;
 * KeyProcessor falls back to Robot for those.
 */
class WindowsNativeKeyInput implements NativeKeyInput {
    private static final Logger log = LogManager.getLogger(WindowsNativeKeyInput.class);

    private static final int KEYEVENTF_KEYUP = 0x0002;
    private static final int KEYEVENTF_EXTENDEDKEY = 0x0001;
    private static final int KEYEVENTF_SCANCODE = 0x0008;
    private static final int KEYEVENTF_UNICODE = 0x0004;
    private static final int MAPVK_VK_TO_VSC = 0;

    // PS/2 Set-1 scan codes keyed by AWT VK code or KeyProcessor NATIVE_BASE+N code.
    private static final Map<Integer, Short> SCAN_MAP = new HashMap<>();

    // Keys that require the E0-prefix (KEYEVENTF_EXTENDEDKEY).
    private static final Set<Integer> NEEDS_EXTENDED = Set.of(
            // Right-side modifiers and super/menu
            NATIVE_BASE + 2,            // KEY_RIGHTCONTROL  → E0 1D
            NATIVE_BASE + 6,            // KEY_RIGHTALT      → E0 38
            NATIVE_BASE + 7,            // KEY_LEFTSUPER     → E0 5B
            NATIVE_BASE + 8,            // KEY_RIGHTSUPER    → E0 5C
            NATIVE_BASE + 9,            // KEY_MENU/APPS     → E0 5D
            // Navigation cluster (shares scan codes with numpad but extended)
            KeyEvent.VK_UP,             // E0 48
            KeyEvent.VK_DOWN,           // E0 50
            KeyEvent.VK_LEFT,           // E0 4B
            KeyEvent.VK_RIGHT,          // E0 4D
            KeyEvent.VK_INSERT,         // E0 52
            KeyEvent.VK_DELETE,         // E0 53
            KeyEvent.VK_HOME,           // E0 47
            KeyEvent.VK_END,            // E0 4F
            KeyEvent.VK_PAGE_UP,        // E0 49
            KeyEvent.VK_PAGE_DOWN,      // E0 51
            // Numpad extended keys
            KeyEvent.VK_DIVIDE,         // E0 35  (numpad /)
            NATIVE_BASE + 11,           // KEY_NUMENTER → E0 1C
            // PrintScreen
            KeyEvent.VK_PRINTSCREEN     // E0 37
    );

    static {
        // --- Left/right modifier keys (NATIVE_BASE + N synthetic codes) ---
        SCAN_MAP.put(NATIVE_BASE + 1, (short) 0x1D);  // KEY_LEFTCONTROL
        SCAN_MAP.put(NATIVE_BASE + 2, (short) 0x1D);  // KEY_RIGHTCONTROL  → E0 1D
        SCAN_MAP.put(NATIVE_BASE + 3, (short) 0x2A);  // KEY_LEFTSHIFT
        SCAN_MAP.put(NATIVE_BASE + 4, (short) 0x36);  // KEY_RIGHTSHIFT
        SCAN_MAP.put(NATIVE_BASE + 5, (short) 0x38);  // KEY_LEFTALT
        SCAN_MAP.put(NATIVE_BASE + 6, (short) 0x38);  // KEY_RIGHTALT      → E0 38
        SCAN_MAP.put(NATIVE_BASE + 7, (short) 0x5B);  // KEY_LEFTSUPER     → E0 5B
        SCAN_MAP.put(NATIVE_BASE + 8, (short) 0x5C);  // KEY_RIGHTSUPER    → E0 5C
        SCAN_MAP.put(NATIVE_BASE + 9, (short) 0x5D);  // KEY_MENU/APPS     → E0 5D
        SCAN_MAP.put(NATIVE_BASE + 10, (short) 0x56);  // KEY_LESSTHAN      → ISO 102nd key (DE/EU <> key)
        SCAN_MAP.put(NATIVE_BASE + 11, (short) 0x1C);  // KEY_NUMENTER      → E0 1C (Numpad Enter, extended)
        SCAN_MAP.put(NATIVE_BASE + 12, (short) 0x28);  // KEY_ADIAERESIS    → ä (DE/EU, US ' position)
        SCAN_MAP.put(NATIVE_BASE + 13, (short) 0x27);  // KEY_ODIAERESIS    → ö (DE/EU, US ; position)
        SCAN_MAP.put(NATIVE_BASE + 14, (short) 0x1A);  // KEY_UDIAERESIS    → ü (DE/EU, US [ position)
        SCAN_MAP.put(NATIVE_BASE + 15, (short) 0x0C);  // KEY_SSHARP        → ß (DE,    US - position)
        SCAN_MAP.put(NATIVE_BASE + 16, (short) 0x0D);  // KEY_DEAD_ACUTE    → ´ (DE,    US = position)
        SCAN_MAP.put(NATIVE_BASE + 17, (short) 0x03); // KEY_EACUTE   → é (FR, PS/2 position 2)
        SCAN_MAP.put(NATIVE_BASE + 18, (short) 0x08); // KEY_EGRAVE   → è (FR, PS/2 position 7)
        SCAN_MAP.put(NATIVE_BASE + 19, (short) 0x0B); // KEY_AGRAVE   → à (FR, PS/2 position 0)
        SCAN_MAP.put(NATIVE_BASE + 20, (short) 0x27); // KEY_UGRAVE   → ù (FR, PS/2 ';' position)
        SCAN_MAP.put(NATIVE_BASE + 21, (short) 0x0A); // KEY_CCEDILLA → ç (FR, PS/2 position 9)
        SCAN_MAP.put(NATIVE_BASE + 22, (short) 0x27); // KEY_NTILDE   → ñ (ES, PS/2 ';' position)

        // --- Control / editing keys ---
        SCAN_MAP.put(KeyEvent.VK_ESCAPE, (short) 0x01);
        SCAN_MAP.put(KeyEvent.VK_BACK_SPACE, (short) 0x0E);
        SCAN_MAP.put(KeyEvent.VK_TAB, (short) 0x0F);
        SCAN_MAP.put(KeyEvent.VK_ENTER, (short) 0x1C);  // main Enter; numpad Enter shares this VK
        SCAN_MAP.put(KeyEvent.VK_SPACE, (short) 0x39);
        SCAN_MAP.put(KeyEvent.VK_CAPS_LOCK, (short) 0x3A);
        SCAN_MAP.put(KeyEvent.VK_NUM_LOCK, (short) 0x45);
        SCAN_MAP.put(KeyEvent.VK_SCROLL_LOCK, (short) 0x46);
        SCAN_MAP.put(KeyEvent.VK_PRINTSCREEN, (short) 0x37);  // E0 37

        // --- Navigation cluster (all extended) ---
        SCAN_MAP.put(KeyEvent.VK_INSERT, (short) 0x52);  // E0 52
        SCAN_MAP.put(KeyEvent.VK_DELETE, (short) 0x53);  // E0 53
        SCAN_MAP.put(KeyEvent.VK_HOME, (short) 0x47);  // E0 47
        SCAN_MAP.put(KeyEvent.VK_END, (short) 0x4F);  // E0 4F
        SCAN_MAP.put(KeyEvent.VK_PAGE_UP, (short) 0x49);  // E0 49
        SCAN_MAP.put(KeyEvent.VK_PAGE_DOWN, (short) 0x51);  // E0 51
        SCAN_MAP.put(KeyEvent.VK_UP, (short) 0x48);  // E0 48
        SCAN_MAP.put(KeyEvent.VK_DOWN, (short) 0x50);  // E0 50
        SCAN_MAP.put(KeyEvent.VK_LEFT, (short) 0x4B);  // E0 4B
        SCAN_MAP.put(KeyEvent.VK_RIGHT, (short) 0x4D);  // E0 4D

        // --- Function keys ---
        SCAN_MAP.put(KeyEvent.VK_F1, (short) 0x3B);
        SCAN_MAP.put(KeyEvent.VK_F2, (short) 0x3C);
        SCAN_MAP.put(KeyEvent.VK_F3, (short) 0x3D);
        SCAN_MAP.put(KeyEvent.VK_F4, (short) 0x3E);
        SCAN_MAP.put(KeyEvent.VK_F5, (short) 0x3F);
        SCAN_MAP.put(KeyEvent.VK_F6, (short) 0x40);
        SCAN_MAP.put(KeyEvent.VK_F7, (short) 0x41);
        SCAN_MAP.put(KeyEvent.VK_F8, (short) 0x42);
        SCAN_MAP.put(KeyEvent.VK_F9, (short) 0x43);
        SCAN_MAP.put(KeyEvent.VK_F10, (short) 0x44);
        SCAN_MAP.put(KeyEvent.VK_F11, (short) 0x57);
        SCAN_MAP.put(KeyEvent.VK_F12, (short) 0x58);

        // --- Number row ---
        SCAN_MAP.put(KeyEvent.VK_1, (short) 0x02);
        SCAN_MAP.put(KeyEvent.VK_2, (short) 0x03);
        SCAN_MAP.put(KeyEvent.VK_3, (short) 0x04);
        SCAN_MAP.put(KeyEvent.VK_4, (short) 0x05);
        SCAN_MAP.put(KeyEvent.VK_5, (short) 0x06);
        SCAN_MAP.put(KeyEvent.VK_6, (short) 0x07);
        SCAN_MAP.put(KeyEvent.VK_7, (short) 0x08);
        SCAN_MAP.put(KeyEvent.VK_8, (short) 0x09);
        SCAN_MAP.put(KeyEvent.VK_9, (short) 0x0A);
        SCAN_MAP.put(KeyEvent.VK_0, (short) 0x0B);

        // --- Number-row punctuation ---
        SCAN_MAP.put(KeyEvent.VK_MINUS, (short) 0x0C);  // - / _
        SCAN_MAP.put(KeyEvent.VK_EQUALS, (short) 0x0D);  // = / +

        // --- Letters (QWERTY layout) ---
        SCAN_MAP.put(KeyEvent.VK_Q, (short) 0x10);
        SCAN_MAP.put(KeyEvent.VK_W, (short) 0x11);
        SCAN_MAP.put(KeyEvent.VK_E, (short) 0x12);
        SCAN_MAP.put(KeyEvent.VK_R, (short) 0x13);
        SCAN_MAP.put(KeyEvent.VK_T, (short) 0x14);
        SCAN_MAP.put(KeyEvent.VK_Y, (short) 0x15);
        SCAN_MAP.put(KeyEvent.VK_U, (short) 0x16);
        SCAN_MAP.put(KeyEvent.VK_I, (short) 0x17);
        SCAN_MAP.put(KeyEvent.VK_O, (short) 0x18);
        SCAN_MAP.put(KeyEvent.VK_P, (short) 0x19);
        SCAN_MAP.put(KeyEvent.VK_OPEN_BRACKET, (short) 0x1A);  // [ / {
        SCAN_MAP.put(KeyEvent.VK_CLOSE_BRACKET, (short) 0x1B);  // ] / }
        SCAN_MAP.put(KeyEvent.VK_A, (short) 0x1E);
        SCAN_MAP.put(KeyEvent.VK_S, (short) 0x1F);
        SCAN_MAP.put(KeyEvent.VK_D, (short) 0x20);
        SCAN_MAP.put(KeyEvent.VK_F, (short) 0x21);
        SCAN_MAP.put(KeyEvent.VK_G, (short) 0x22);
        SCAN_MAP.put(KeyEvent.VK_H, (short) 0x23);
        SCAN_MAP.put(KeyEvent.VK_J, (short) 0x24);
        SCAN_MAP.put(KeyEvent.VK_K, (short) 0x25);
        SCAN_MAP.put(KeyEvent.VK_L, (short) 0x26);
        SCAN_MAP.put(KeyEvent.VK_SEMICOLON, (short) 0x27);  // ; / :
        SCAN_MAP.put(KeyEvent.VK_QUOTE, (short) 0x28);  // ' / "
        SCAN_MAP.put(KeyEvent.VK_BACK_QUOTE, (short) 0x29);  // ` / ~
        SCAN_MAP.put(KeyEvent.VK_BACK_SLASH, (short) 0x2B);  // \ / | (also UK # key)
        SCAN_MAP.put(KeyEvent.VK_Z, (short) 0x2C);
        SCAN_MAP.put(KeyEvent.VK_X, (short) 0x2D);
        SCAN_MAP.put(KeyEvent.VK_C, (short) 0x2E);
        SCAN_MAP.put(KeyEvent.VK_V, (short) 0x2F);
        SCAN_MAP.put(KeyEvent.VK_B, (short) 0x30);
        SCAN_MAP.put(KeyEvent.VK_N, (short) 0x31);
        SCAN_MAP.put(KeyEvent.VK_M, (short) 0x32);
        SCAN_MAP.put(KeyEvent.VK_COMMA, (short) 0x33);  // , / <
        SCAN_MAP.put(KeyEvent.VK_PERIOD, (short) 0x34);  // . / >
        SCAN_MAP.put(KeyEvent.VK_SLASH, (short) 0x35);  // / / ?  (main keyboard)

        // --- Numpad ---
        SCAN_MAP.put(KeyEvent.VK_MULTIPLY, (short) 0x37);  // *
        SCAN_MAP.put(KeyEvent.VK_ADD, (short) 0x4E);  // +
        SCAN_MAP.put(KeyEvent.VK_SUBTRACT, (short) 0x4A);  // -
        SCAN_MAP.put(KeyEvent.VK_DECIMAL, (short) 0x53);  // .
        SCAN_MAP.put(KeyEvent.VK_DIVIDE, (short) 0x35);  // /  (E0 35)
        SCAN_MAP.put(KeyEvent.VK_NUMPAD0, (short) 0x52);
        SCAN_MAP.put(KeyEvent.VK_NUMPAD1, (short) 0x4F);
        SCAN_MAP.put(KeyEvent.VK_NUMPAD2, (short) 0x50);
        SCAN_MAP.put(KeyEvent.VK_NUMPAD3, (short) 0x51);
        SCAN_MAP.put(KeyEvent.VK_NUMPAD4, (short) 0x4B);
        SCAN_MAP.put(KeyEvent.VK_NUMPAD5, (short) 0x4C);
        SCAN_MAP.put(KeyEvent.VK_NUMPAD6, (short) 0x4D);
        SCAN_MAP.put(KeyEvent.VK_NUMPAD7, (short) 0x47);
        SCAN_MAP.put(KeyEvent.VK_NUMPAD8, (short) 0x48);
        SCAN_MAP.put(KeyEvent.VK_NUMPAD9, (short) 0x49);
    }

    // Diagnostics run from the constructor (not the static block) so a crash here
    // is caught by NativeKeyInputFactory and triggers RobotFallback instead of
    // killing the whole class with ExceptionInInitializerError.
    WindowsNativeKeyInput() {
        logStartupDiagnostics();
    }

    // -------------------------------------------------------------------------
    // Startup diagnostics
    // -------------------------------------------------------------------------

    private static void logStartupDiagnostics() {
        log.info("=== WindowsNativeKeyInput startup diagnostics ===");
        log.info("[diag] OS      : {} {} ({})",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));
        log.info("[diag] Java    : {} ({})",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
        log.info("[diag] User    : {}", System.getProperty("user.name"));
        log.info("[diag] PID     : {}", ProcessHandle.current().pid());
        log.info("[diag] SCAN_MAP: {} entries, NEEDS_EXTENDED: {} entries",
                SCAN_MAP.size(), NEEDS_EXTENDED.size());

        try {
            checkJnaConnectivity();
        } catch (Throwable t) {
            log.error("[diag] checkJnaConnectivity failed", t);
        }
        try {
            checkProcessElevation();
        } catch (Throwable t) {
            log.error("[diag] checkProcessElevation failed", t);
        }
        try {
            checkBlockingInput();
        } catch (Throwable t) {
            log.error("[diag] checkBlockingInput failed", t);
        }

        log.info("[diag] *** UIPI reminder: if Elite Dangerous is launched as Administrator ***");
        log.info("[diag] *** (UAC shield on the launcher icon), Windows will silently drop   ***");
        log.info("[diag] *** ALL SendInput calls from this standard-user process.            ***");
        log.info("[diag] *** Fix: ED launcher → right-click → Properties → Compatibility    ***");
        log.info("[diag] *** tab → un-check 'Run this program as an administrator'.          ***");
        log.info("=================================================");
    }

    private static void checkJnaConnectivity() {
        try {
            int screenW = User32.INSTANCE.GetSystemMetrics(0); // SM_CXSCREEN
            int screenH = User32.INSTANCE.GetSystemMetrics(1); // SM_CYSCREEN
            log.info("[diag] JNA/User32 OK — screen {}x{}", screenW, screenH);
        } catch (Exception | UnsatisfiedLinkError e) {
            log.error("[diag] JNA/User32 FAILED: {} — SendInput will not work!", e.getMessage());
        }
    }

    /**
     * Custom JNA interface for the two Advapi32 calls we need.
     * Uses raw Pointer for GetTokenInformation so JNA never tries to reflect on a
     * Structure subclass — that reflection breaks under Java 25's module system.
     */
    private interface Advapi32Ptr extends StdCallLibrary {
        Advapi32Ptr INSTANCE = com.sun.jna.Native.load("advapi32", Advapi32Ptr.class,
                W32APIOptions.DEFAULT_OPTIONS);

        boolean OpenProcessToken(WinNT.HANDLE ProcessHandle, int DesiredAccess,
                                 WinNT.HANDLEByReference TokenHandle);

        boolean GetTokenInformation(WinNT.HANDLE tokenHandle, int tokenInformationClass,
                                    com.sun.jna.Pointer tokenInformation,
                                    int tokenInformationLength, IntByReference returnLength);
    }

    private static void checkProcessElevation() {
        try {
            WinNT.HANDLEByReference hTokenRef = new WinNT.HANDLEByReference();
            if (!Advapi32Ptr.INSTANCE.OpenProcessToken(
                    Kernel32.INSTANCE.GetCurrentProcess(),
                    WinNT.TOKEN_QUERY,
                    hTokenRef)) {
                log.warn("[diag] OpenProcessToken failed: err=0x{}",
                        Integer.toHexString(Kernel32.INSTANCE.GetLastError()));
                return;
            }
            try {
                // TOKEN_ELEVATION (class 20) is a single DWORD — 4 bytes
                Memory pElev = new Memory(4);
                IntByReference pSize = new IntByReference(4);
                if (Advapi32Ptr.INSTANCE.GetTokenInformation(
                        hTokenRef.getValue(), 20, pElev, 4, pSize)) {
                    int elevated = pElev.getInt(0);
                    log.info("[diag] This process elevation: {} (TokenIsElevated={})",
                            elevated != 0 ? "ELEVATED/ADMIN" : "standard-user", elevated);
                    if (elevated == 0) {
                        log.warn("[diag] Running as standard user. If the game runs elevated, " +
                                "SendInput will be silently blocked (UIPI error 5).");
                    }
                } else {
                    log.warn("[diag] GetTokenInformation(TokenElevation) failed: err=0x{}",
                            Integer.toHexString(Kernel32.INSTANCE.GetLastError()));
                }
            } finally {
                Kernel32.INSTANCE.CloseHandle(hTokenRef.getValue());
            }
        } catch (Throwable e) {
            log.warn("[diag] Elevation check error: {} {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * Sends a benign test event (key-down + key-up for scan 0x00, unused scan code)
     * to verify SendInput reaches the OS at all. A return of 0 here with error 5 means
     * UIPI is already blocking us, even before the game is in focus.
     */
    private static void checkBlockingInput() {
        try {
            WinUser.INPUT input = new WinUser.INPUT();
            input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
            input.input.setType(WinUser.KEYBDINPUT.class);
            input.input.ki.wVk = new WinDef.WORD(0);
            input.input.ki.wScan = new WinDef.WORD(0xFF); // unused scan — no real key
            input.input.ki.dwFlags = new WinDef.DWORD(KEYEVENTF_SCANCODE | KEYEVENTF_KEYUP);
            input.input.ki.time = new WinDef.DWORD(0);

            WinDef.DWORD sent = User32.INSTANCE.SendInput(
                    new WinDef.DWORD(1),
                    (WinUser.INPUT[]) input.toArray(1),
                    input.size());

            int err = Kernel32.INSTANCE.GetLastError();
            if (sent.intValue() == 1) {
                log.info("[diag] SendInput smoke-test: OK (1 event accepted)");
            } else {
                log.error("[diag] SendInput smoke-test FAILED: sent={}, GetLastError=0x{} ({})",
                        sent.intValue(), Integer.toHexString(err), describeWin32Error(err));
            }
        } catch (Throwable e) {
            log.error("[diag] SendInput smoke-test exception: {} {}", e.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // NativeKeyInput implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean handles(int keyCode) {
        return SCAN_MAP.containsKey(keyCode);
    }

    @Override
    public void keyDown(int keyCode) {
        sendKeyEvent(keyCode, false);
    }

    @Override
    public void keyUp(int keyCode) {
        sendKeyEvent(keyCode, true);
    }

    private void sendKeyEvent(int keyCode, boolean isKeyUp) {
        short scan;
        String resolvedVia;

        if (keyCode >= NATIVE_BASE) {
            Short s = SCAN_MAP.get(keyCode);
            if (s == null) {
                log.warn("[key] No Windows scan code mapping for NATIVE code 0x{}", Integer.toHexString(keyCode));
                return;
            }
            scan = s;
            resolvedVia = "SCAN_MAP(native)";
        } else {
            boolean isLetter = keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z;
            boolean isNavCluster = keyCode >= KeyEvent.VK_PAGE_UP && keyCode <= KeyEvent.VK_DOWN;
            if (isLetter || isNavCluster) {
                int vsc = User32.INSTANCE.MapVirtualKeyEx(keyCode, MAPVK_VK_TO_VSC, null);
                if (vsc != 0) {
                    scan = (short) vsc;
                    resolvedVia = isLetter ? "MapVirtualKeyEx(letter)" : "MapVirtualKeyEx(nav)";
                } else {
                    log.warn("[key] MapVirtualKeyEx returned 0 for VK=0x{} ({}), falling back to SCAN_MAP",
                            Integer.toHexString(keyCode), keyName(keyCode));
                    Short s = SCAN_MAP.get(keyCode);
                    if (s == null) {
                        log.warn("[key] No scan code mapping for VK=0x{} ({})", Integer.toHexString(keyCode), keyName(keyCode));
                        return;
                    }
                    scan = s;
                    resolvedVia = "SCAN_MAP(MapVK-fallback)";
                }
            } else {
                Short s = SCAN_MAP.get(keyCode);
                if (s == null) {
                    log.warn("[key] No Windows scan code mapping for VK=0x{} ({})", Integer.toHexString(keyCode), keyName(keyCode));
                    return;
                }
                scan = s;
                resolvedVia = "SCAN_MAP(direct)";
            }
        }

        boolean needsExtended = NEEDS_EXTENDED.contains(keyCode);
        int flags = KEYEVENTF_SCANCODE;
        if (needsExtended) flags |= KEYEVENTF_EXTENDEDKEY;
        if (isKeyUp) flags |= KEYEVENTF_KEYUP;

        log.debug("[key] SendInput keyCode=0x{} ({}) scan=0x{} flags=0x{} ({}) via={} keyUp={}",
                Integer.toHexString(keyCode),
                keyName(keyCode),
                Integer.toHexString(scan & 0xFFFF),
                Integer.toHexString(flags),
                describeFlags(flags),
                resolvedVia,
                isKeyUp);

        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType(WinUser.KEYBDINPUT.class);
        input.input.ki.wVk = new WinDef.WORD(0);
        input.input.ki.wScan = new WinDef.WORD(scan);
        input.input.ki.dwFlags = new WinDef.DWORD(flags);
        input.input.ki.time = new WinDef.DWORD(0);

        WinDef.DWORD sent = User32.INSTANCE.SendInput(
                new WinDef.DWORD(1),
                (WinUser.INPUT[]) input.toArray(1),
                input.size()
        );

        int result = sent.intValue();
        if (result == 1) {
            log.debug("[key] SendInput OK — keyCode=0x{} ({}) scan=0x{} keyUp={}",
                    Integer.toHexString(keyCode), keyName(keyCode),
                    Integer.toHexString(scan & 0xFFFF), isKeyUp);
        } else {
            int err = Kernel32.INSTANCE.GetLastError();
            log.error("[key] SendInput FAILED — sent={} GetLastError=0x{} ({}) keyCode=0x{} ({}) scan=0x{} flags=0x{}",
                    result,
                    Integer.toHexString(err),
                    describeWin32Error(err),
                    Integer.toHexString(keyCode),
                    keyName(keyCode),
                    Integer.toHexString(scan & 0xFFFF),
                    Integer.toHexString(flags));
        }
    }

    @Override
    public boolean typeChar(char c) {
        WinUser.INPUT[] inputs = (WinUser.INPUT[]) new WinUser.INPUT().toArray(2);

        inputs[0].type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        inputs[0].input.setType(WinUser.KEYBDINPUT.class);
        inputs[0].input.ki.wVk = new WinDef.WORD(0);
        inputs[0].input.ki.wScan = new WinDef.WORD(c);
        inputs[0].input.ki.dwFlags = new WinDef.DWORD(KEYEVENTF_UNICODE);
        inputs[0].input.ki.time = new WinDef.DWORD(0);

        inputs[1].type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        inputs[1].input.setType(WinUser.KEYBDINPUT.class);
        inputs[1].input.ki.wVk = new WinDef.WORD(0);
        inputs[1].input.ki.wScan = new WinDef.WORD(c);
        inputs[1].input.ki.dwFlags = new WinDef.DWORD(KEYEVENTF_UNICODE | KEYEVENTF_KEYUP);
        inputs[1].input.ki.time = new WinDef.DWORD(0);

        log.debug("[key] typeChar '{}' (U+{}) via KEYEVENTF_UNICODE", c, Integer.toHexString(c));

        WinDef.DWORD sent = User32.INSTANCE.SendInput(
                new WinDef.DWORD(2),
                inputs,
                inputs[0].size()
        );
        int result = sent.intValue();
        if (result != 2) {
            int err = Kernel32.INSTANCE.GetLastError();
            log.error("[key] SendInput (unicode) FAILED — sent={} GetLastError=0x{} ({}) char='{}' U+{}",
                    result,
                    Integer.toHexString(err),
                    describeWin32Error(err),
                    c,
                    Integer.toHexString(c));
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String describeWin32Error(int err) {
        return switch (err) {
            case 0 -> "ERROR_SUCCESS (no error)";
            case 5 -> "ERROR_ACCESS_DENIED — UIPI blocked: target window runs at higher integrity than this process!";
            case 6 -> "ERROR_INVALID_HANDLE";
            case 87 -> "ERROR_INVALID_PARAMETER";
            case 1400 -> "ERROR_INVALID_WINDOW_HANDLE";
            case 1 -> "ERROR_INVALID_FUNCTION — SendInput blocked (e.g. UIPI or secure desktop)";
            default -> "unknown (0x" + Integer.toHexString(err) + ")";
        };
    }

    private static String describeFlags(int flags) {
        StringBuilder sb = new StringBuilder();
        if ((flags & KEYEVENTF_SCANCODE) != 0) sb.append("SCANCODE|");
        if ((flags & KEYEVENTF_EXTENDEDKEY) != 0) sb.append("EXTENDED|");
        if ((flags & KEYEVENTF_KEYUP) != 0) sb.append("KEYUP|");
        if ((flags & KEYEVENTF_UNICODE) != 0) sb.append("UNICODE|");
        if (sb.isEmpty()) sb.append("none");
        else sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private static String keyName(int keyCode) {
        if (keyCode >= NATIVE_BASE) return "NATIVE+" + (keyCode - NATIVE_BASE);
        String name = KeyEvent.getKeyText(keyCode);
        return (name == null || name.isBlank()) ? "?" : name;
    }
}
