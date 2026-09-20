package elite.intel.ai.hands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.KeyEvent;

import static elite.intel.ai.hands.KeyProcessor.NATIVE_BASE;

/**
 * Creates the appropriate NativeKeyInput for the current platform.
 * <p>
 * Priority order:
 * Windows  → WindowsNativeKeyInput  (SendInput via user32.dll)
 * Linux    → LinuxX11NativeKeyInput (XTestFakeKeyEvent via libXtst)
 * if X11/XTest unavailable → RobotFallback (generic modifiers, no L/R distinction)
 * Other    → RobotFallback
 */
class NativeKeyInputFactory {
    private static final Logger log = LogManager.getLogger(NativeKeyInputFactory.class);

    static NativeKeyInput create(Robot robot) {
        String os = System.getProperty("os.name", "").toLowerCase();

        log.info("[factory] OS='{}' Java='{}' user='{}' robotMethod='{}'",
                os, System.getProperty("java.version"), System.getProperty("user.name"),
                System.getProperty("awt.robot.screenshotMethod", "jdk default"));

        if (os.contains("win")) {
            try {
                WindowsNativeKeyInput impl = new WindowsNativeKeyInput();
                log.info("[factory] Using WindowsNativeKeyInput (SendInput via user32.dll)");
                return impl;
            } catch (Throwable e) {
                log.warn("[factory] WindowsNativeKeyInput unavailable: {} {}. Falling back to Robot.",
                        e.getClass().getSimpleName(), e.getMessage(), e);
            }
        } else if (os.contains("linux") || os.contains("nix") || os.contains("nux")) {
            try {
                LinuxX11NativeKeyInput impl = new LinuxX11NativeKeyInput();
                if (impl.isAvailable()) {
                    log.info("Using Linux X11 native key input (XTestFakeKeyEvent)");
                    return impl;
                }
                log.warn("LinuxX11NativeKeyInput initialised but unavailable - falling back to Robot.");
            } catch (Exception | UnsatisfiedLinkError e) {
                log.warn("LinuxX11NativeKeyInput unavailable: {}. Falling back to Robot.", e.getMessage());
            }
        } else {
            log.warn("Unrecognised OS '{}' - falling back to Robot for modifier keys.", os);
        }

        log.warn("Native left/right modifier distinction is NOT active. " +
                "LeftAlt+X and RightAlt+X will send identical key events.");
        return new RobotFallback(robot);
    }

    /**
     * Fallback that maps synthetic modifier codes back to generic AWT VK codes.
     * Preserves the old behaviour when native support is unavailable.
     */
    private static class RobotFallback implements NativeKeyInput {
        private static final java.util.Map<Integer, Integer> FALLBACK_VK = new java.util.HashMap<>();

        static {
            // Left and right variants map to the same generic VK - no distinction possible
            FALLBACK_VK.put(NATIVE_BASE + 1, KeyEvent.VK_CONTROL);  // LEFTCONTROL
            FALLBACK_VK.put(NATIVE_BASE + 2, KeyEvent.VK_CONTROL);  // RIGHTCONTROL
            FALLBACK_VK.put(NATIVE_BASE + 3, KeyEvent.VK_SHIFT);    // LEFTSHIFT
            FALLBACK_VK.put(NATIVE_BASE + 4, KeyEvent.VK_SHIFT);    // RIGHTSHIFT
            FALLBACK_VK.put(NATIVE_BASE + 5, KeyEvent.VK_ALT);      // LEFTALT
            FALLBACK_VK.put(NATIVE_BASE + 6, KeyEvent.VK_ALT);      // RIGHTALT
            FALLBACK_VK.put(NATIVE_BASE + 7, KeyEvent.VK_WINDOWS);        // LEFTSUPER
            FALLBACK_VK.put(NATIVE_BASE + 8, KeyEvent.VK_WINDOWS);        // RIGHTSUPER
            FALLBACK_VK.put(NATIVE_BASE + 9, KeyEvent.VK_CONTEXT_MENU);   // MENU
        }

        private final Robot robot;

        RobotFallback(Robot robot) {
            this.robot = robot;
        }

        @Override
        public boolean handles(int keyCode) {
            return FALLBACK_VK.containsKey(keyCode);
        }

        @Override
        public void keyDown(int syntheticCode) {
            Integer vk = FALLBACK_VK.get(syntheticCode);
            if (vk != null) robot.keyPress(vk);
        }

        @Override
        public void keyUp(int syntheticCode) {
            Integer vk = FALLBACK_VK.get(syntheticCode);
            if (vk != null) robot.keyRelease(vk);
        }
    }
}
