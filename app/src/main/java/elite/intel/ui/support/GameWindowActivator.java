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
 * Windows integration for locating the Elite Dangerous window and applying reversible foreground or position changes.
 */
public final class GameWindowActivator {

    private static final Logger log = LogManager.getLogger(GameWindowActivator.class);
    private static final Object WINDOW_POSITION_LOCK = new Object();
    private static final String[] ELITE_WINDOW_TITLE_MARKERS = {
            "elite - dangerous",
            "elite dangerous"
    };
    private static SavedWindowPosition shiftedWindow;

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

    /**
     * Moves the game window upward just enough to place its non-client title area above its current monitor, while
     * preserving the window's original size and z-order. The original position is retained for restoration.
     *
     * @return {@code true} when a visible Elite Dangerous window was repositioned or was already repositioned
     */
    public static boolean hideEliteDangerousTitleBar() {
        if (!Platform.isWindows()) {
            return false;
        }
        Optional<WinDef.HWND> gameWindow = findWindowsGameWindow();
        if (gameWindow.isEmpty()) {
            log.debug("Elite Dangerous window not found for title-bar hiding");
            return false;
        }

        WinDef.HWND hwnd = gameWindow.get();
        synchronized (WINDOW_POSITION_LOCK) {
            if (shiftedWindow != null) {
                return true;
            }

            WinUser.WINDOWINFO windowInfo = new WinUser.WINDOWINFO();
            if (!User32.INSTANCE.GetWindowInfo(hwnd, windowInfo)) {
                log.debug("Unable to read Elite Dangerous window geometry for title-bar hiding");
                return false;
            }
            WinUser.HMONITOR monitor = User32.INSTANCE.MonitorFromWindow(hwnd, WinUser.MONITOR_DEFAULTTONEAREST);
            WinUser.MONITORINFO monitorInfo = new WinUser.MONITORINFO();
            if (monitor == null || !User32.INSTANCE.GetMonitorInfo(monitor, monitorInfo).booleanValue()) {
                log.debug("Unable to determine Elite Dangerous monitor for title-bar hiding");
                return false;
            }

            int targetTop = topForHiddenCaption(
                    monitorInfo.rcMonitor.top,
                    windowInfo.rcWindow.top,
                    windowInfo.rcClient.top);
            if (targetTop >= windowInfo.rcWindow.top) {
                log.debug("Elite Dangerous window has no movable title area");
                return false;
            }
            boolean repositioned = User32.INSTANCE.SetWindowPos(
                    hwnd,
                    null,
                    windowInfo.rcWindow.left,
                    targetTop,
                    0,
                    0,
                    WinUser.SWP_NOSIZE | WinUser.SWP_NOZORDER | WinUser.SWP_NOACTIVATE);
            if (repositioned) {
                shiftedWindow = new SavedWindowPosition(hwnd, windowInfo.rcWindow.left, windowInfo.rcWindow.top);
            } else {
                log.debug("Unable to move Elite Dangerous window above its title bar");
            }
            return repositioned;
        }
    }

    /** Restores the game window position saved by {@link #hideEliteDangerousTitleBar()}. */
    public static void restoreEliteDangerousWindowPosition() {
        if (!Platform.isWindows()) {
            return;
        }
        SavedWindowPosition saved;
        synchronized (WINDOW_POSITION_LOCK) {
            saved = shiftedWindow;
            shiftedWindow = null;
        }
        if (saved == null || !User32.INSTANCE.IsWindow(saved.window())) {
            return;
        }
        boolean restored = User32.INSTANCE.SetWindowPos(
                saved.window(),
                null,
                saved.left(),
                saved.top(),
                0,
                0,
                WinUser.SWP_NOSIZE | WinUser.SWP_NOZORDER | WinUser.SWP_NOACTIVATE);
        log.debug("Elite Dangerous window position restored={}", restored);
    }

    /** Calculates the outer-window top coordinate that places its client area at the monitor's top edge. */
    static int topForHiddenCaption(int monitorTop, int windowTop, int clientTop) {
        return monitorTop - Math.max(0, clientTop - windowTop);
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

    private record SavedWindowPosition(WinDef.HWND window, int left, int top) {
    }
}
