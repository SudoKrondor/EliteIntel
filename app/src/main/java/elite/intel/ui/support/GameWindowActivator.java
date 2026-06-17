package elite.intel.ui.support;


import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Optional;

/**
 * Best-effort foreground activation for the Elite Dangerous game window before GUI-triggered input dispatch.
 */
final class GameWindowActivator {

    private static final Logger log = LogManager.getLogger(GameWindowActivator.class);
    private static final String[] ELITE_WINDOW_TITLE_MARKERS = {
            "elite - dangerous",
            "elite dangerous"
    };

    private GameWindowActivator() {
    }

    /**
     * Attempts to restore and foreground the game window.
     *
     * @return {@code true} when an Elite Dangerous window was found and a foreground request was accepted
     */
    static boolean activateEliteDangerousWindow() {
        if (!Platform.isWindows()) {
            return false;
        }
        Optional<WinDef.HWND> gameWindow = findWindowsGameWindow();
        if (gameWindow.isEmpty()) {
            log.debug("Elite Dangerous window not found for GUI command dispatch");
            return false;
        }

        WinDef.HWND hwnd = gameWindow.get();
        User32.INSTANCE.ShowWindow(hwnd, WinUser.SW_RESTORE);
        User32.INSTANCE.BringWindowToTop(hwnd);
        boolean foregroundSet = User32.INSTANCE.SetForegroundWindow(hwnd);
        log.debug("Elite Dangerous foreground request accepted={}", foregroundSet);
        return foregroundSet;
    }

    private static Optional<WinDef.HWND> findWindowsGameWindow() {
        WindowSearch search = new WindowSearch();
        User32.INSTANCE.EnumWindows(search, null);
        return Optional.ofNullable(search.match);
    }

    private static boolean isEliteDangerousTitle(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }
        String normalized = title.toLowerCase(Locale.ROOT);
        for (String marker : ELITE_WINDOW_TITLE_MARKERS) {
            if (normalized.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private static final class WindowSearch implements WinUser.WNDENUMPROC {
        private WinDef.HWND match;

        @Override
        public boolean callback(WinDef.HWND hwnd, Pointer data) {
            if (!User32.INSTANCE.IsWindowVisible(hwnd)) {
                return true;
            }
            String title = windowTitle(hwnd);
            if (isEliteDangerousTitle(title)) {
                match = hwnd;
                return false;
            }
            return true;
        }

        private static String windowTitle(WinDef.HWND hwnd) {
            char[] buffer = new char[512];
            int length = User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
            return length <= 0 ? "" : new String(buffer, 0, length);
        }
    }
}
