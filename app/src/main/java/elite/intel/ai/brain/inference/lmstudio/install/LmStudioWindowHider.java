package elite.intel.ai.brain.inference.lmstudio.install;

import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Hides the console windows LM Studio opens on Windows. WHY: {@code lms daemon up} and the install script start
 * LM Studio's own {@code node.exe} helpers, and each of them gets a console window of its own - several flash up and
 * two stay open, which looks like a virus to a commander (a Windows tester, 2026-11-10). Those helpers are started
 * by LM Studio, not by us, so the hidden launch of {@code lms} does not reach them; hiding the windows does. Hiding
 * a window leaves its program running: LM Studio keeps working and {@code lms daemon down} still stops it.
 * <p>
 * A watch looks for such windows a few times a second while an LM Studio command runs and for {@link #TAIL} after it,
 * since the helpers may open their windows after the command that started them has returned. Does nothing off
 * Windows.
 */
final class LmStudioWindowHider {

    static final Duration TAIL = Duration.ofSeconds(20);
    private static final Duration POLL = Duration.ofMillis(100);

    private LmStudioWindowHider() {
    }

    /**
     * A running watch. Closing it starts the {@link #TAIL}, after which the watch ends by itself.
     */
    interface Watch extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * Starts watching for windows of the LM Studio in {@code home}.
     *
     * @param log where each hidden window is reported, so a support bundle shows what was hidden
     */
    static Watch watch(Path home, Consumer<String> log) {
        if (!Platform.isWindows()) return () -> {
        };
        String homeText = home.toAbsolutePath().normalize().toString();
        long self = ProcessHandle.current().pid();
        Set<String> reported = new HashSet<>();
        AtomicReference<Instant> stopAt = new AtomicReference<>();
        Thread.ofPlatform().daemon().name("lm-studio-window-hider").start(() -> {
            while (true) {
                Instant end = stopAt.get();
                if (end != null && Instant.now().isAfter(end)) return;
                try {
                    hideOnce(homeText, self, reported, log);
                } catch (RuntimeException | LinkageError e) {
                    log.accept("Could not look for LM Studio's console windows: " + e);
                    return;
                }
                try {
                    Thread.sleep(POLL);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        return () -> stopAt.compareAndSet(null, Instant.now().plus(TAIL));
    }

    private static void hideOnce(String home, long self, Set<String> reported, Consumer<String> log) {
        User32 user32 = User32.INSTANCE;
        user32.EnumWindows((HWND window, Pointer data) -> {
            if (!user32.IsWindowVisible(window)) return true;
            IntByReference pid = new IntByReference();
            user32.GetWindowThreadProcessId(window, pid);
            if (pid.getValue() == self) return true;
            char[] buffer = new char[1024];
            int length = user32.GetWindowText(window, buffer, buffer.length);
            String title = new String(buffer, 0, Math.max(length, 0));
            Optional<String> command = ProcessHandle.of(pid.getValue()).flatMap(p -> p.info().command());
            if (isLmStudios(title, command, home)) {
                user32.ShowWindow(window, WinUser.SW_HIDE);
                String what = "pid " + pid.getValue() + " " + command.orElse("?") + " \"" + title + "\"";
                synchronized (reported) {
                    if (reported.add(what)) log.accept("Hid LM Studio's console window: " + what);
                }
            }
            return true;
        }, null);
    }

    /**
     * Whether a window belongs to the LM Studio in {@code home}: its program lives there, or its title names a path
     * there. WHY the title as well: when Windows Terminal hosts the console, the window's program is Windows Terminal,
     * and only the title - the path of the program in it - says whose console it is.
     */
    static boolean isLmStudios(String title, Optional<String> command, String home) {
        String root = withSeparator(home.toLowerCase(Locale.ROOT));
        if (command.map(c -> c.toLowerCase(Locale.ROOT).startsWith(root)).orElse(false)) return true;
        return title != null && title.toLowerCase(Locale.ROOT).replace('/', '\\').contains(root);
    }

    private static String withSeparator(String path) {
        return path.endsWith("\\") ? path : path + "\\";
    }
}
