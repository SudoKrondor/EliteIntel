package elite.intel.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Starts a fresh copy of the app through the install4j launcher, for the caller to exit after - and, in that fresh
 * copy, waits for the old one to be gone before anything opens the data folder.
 * <p>
 * WHY the wait: the new process starts while the old one is still shutting down. Its first act may be swapping in
 * a restored database, and on Windows a file the old process still holds open cannot be moved.
 */
public final class AppRelauncher {

    private static final Logger log = LogManager.getLogger(AppRelauncher.class);

    /**
     * The argument that names the process the new one waits for. install4j launchers pass arguments through.
     */
    private static final String AFTER_PID = "--after-pid=";
    private static final String UNIX_LAUNCHER = "elite-intel";
    private static final String WINDOWS_LAUNCHER = "elite-intel.exe";
    private static final long WAIT_SECONDS = 60;

    private AppRelauncher() {
    }

    /**
     * Starts the launcher, telling the new process to wait for this one.
     *
     * @return false when there is no launcher to start - a run from the IDE - or it would not start; the commander
     * then has to start the app again by hand
     */
    public static boolean relaunch() {
        Optional<Path> launcher = installLauncher();
        if (launcher.isEmpty()) return false;
        try {
            new ProcessBuilder(launcher.get().toString(), AFTER_PID + ProcessHandle.current().pid())
                    .directory(AppPaths.getAppDirectory().toFile())
                    // WHY: a pipe nobody reads fills up, and one whose reader has exited breaks the writer.
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return true;
        } catch (IOException e) {
            log.error("Could not start {}", launcher.get(), e);
            return false;
        }
    }

    /**
     * Waits for the process named by an {@code --after-pid=} argument to exit, if one was passed and is still
     * running. Gives up after a minute rather than never starting.
     */
    public static void awaitPreviousInstance(String[] args) {
        Arrays.stream(args)
                .filter(arg -> arg.startsWith(AFTER_PID))
                .map(arg -> arg.substring(AFTER_PID.length()))
                .findFirst()
                .flatMap(AppRelauncher::parsePid)
                .flatMap(ProcessHandle::of)
                .ifPresent(AppRelauncher::awaitExit);
    }

    private static Optional<Long> parsePid(String pid) {
        try {
            return Optional.of(Long.parseLong(pid));
        } catch (NumberFormatException e) {
            log.warn("Ignoring {}{}: not a process id", AFTER_PID, pid);
            return Optional.empty();
        }
    }

    private static void awaitExit(ProcessHandle previous) {
        try {
            previous.onExit().get(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Process {} is still running after {}s; starting anyway", previous.pid(), WAIT_SECONDS);
        } catch (ExecutionException e) {
            log.warn("Could not wait for process {}", previous.pid(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Optional<Path> installLauncher() {
        String name = OsDetector.getOs() == OsDetector.OS.WINDOWS ? WINDOWS_LAUNCHER : UNIX_LAUNCHER;
        Path launcher = AppPaths.getAppDirectory().resolve(name);
        return Files.isRegularFile(launcher) && Files.isExecutable(launcher) ? Optional.of(launcher) : Optional.empty();
    }
}
