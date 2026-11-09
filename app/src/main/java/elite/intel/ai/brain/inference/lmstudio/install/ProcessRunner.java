package elite.intel.ai.brain.inference.lmstudio.install;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Runs one of LM Studio's commands - its install script or {@code lms} - to the end, handing each line it prints to
 * a log as it arrives, and killing it when it runs past its time or the install is cancelled.
 * <p>
 * Every command runs with {@code LMS_NO_MODIFY_PATH=1}: the app calls {@code lms} by its full path, so LM Studio
 * has no reason to change the commander's PATH or shell profiles.
 */
final class ProcessRunner {

    private final Consumer<String> log;
    private volatile Process running;
    private volatile boolean cancelled;

    ProcessRunner(Consumer<String> log) {
        this.log = log;
    }

    /**
     * Runs {@code command} and returns its cleaned output lines.
     *
     * @throws InstallException when it cannot start, exits with an error, runs past {@code timeout}, or is
     *                                cancelled
     */
    List<String> run(List<String> command, Duration timeout) throws InstallException {
        if (cancelled) throw new InstallException("Cancelled");
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().put("LMS_NO_MODIFY_PATH", "1");
        builder.directory(Path.of(System.getProperty("user.home")).toFile());
        log.accept("> " + String.join(" ", command));
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new InstallException("Could not start " + command.getFirst() + ": " + e.getMessage(), e);
        }
        running = process;
        try {
            return awaitExit(process, command, timeout);
        } finally {
            running = null;
        }
    }

    // WHY one method for the read and the wait: the output is read on its own thread while this one waits, and the two
    // belong together - a process whose output nobody reads blocks once the pipe fills and never exits.
    private List<String> awaitExit(Process process, List<String> command, Duration timeout) throws InstallException {
        List<String> output = new ArrayList<>();
        Thread reader = Thread.ofVirtual().name("lm-studio-output").start(() -> readLines(process, output));
        try {
            if (!process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new InstallException(command.getFirst() + " did not finish within " + timeout.toMinutes() + " min");
            }
            reader.join(Duration.ofSeconds(5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new InstallException("Interrupted");
        }
        if (cancelled) throw new InstallException("Cancelled");
        if (process.exitValue() != 0) {
            throw new InstallException(String.join(" ", command) + " failed with exit code " + process.exitValue());
        }
        synchronized (output) {
            return List.copyOf(output);
        }
    }

    private void readLines(Process process, List<String> output) {
        try (BufferedReader lines = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            String previous = null;
            while ((line = lines.readLine()) != null) {
                String text = ConsoleText.clean(line).orElse(null);
                // WHY drop a repeat: a spinner redraws the same words many times a second.
                if (text == null || text.equals(previous)) continue;
                previous = text;
                synchronized (output) {
                    output.add(text);
                }
                log.accept(text);
            }
        } catch (IOException closed) {
            // WHY: the stream closes under the reader when the process is killed - cancelled or timed out - and that
            // is already reported by the thread waiting on it.
        }
    }

    /**
     * Stops the command running now, if any, and every command after it.
     */
    void cancel() {
        cancelled = true;
        Process process = running;
        if (process != null) process.destroyForcibly();
    }

    boolean isCancelled() {
        return cancelled;
    }
}
