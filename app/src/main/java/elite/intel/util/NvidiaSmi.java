package elite.intel.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs NVIDIA's own {@code nvidia-smi}, the one exact source of a card's memory and compute capability without a
 * vendor SDK. The driver puts it on the PATH on both platforms; every other vendor's machine simply lacks it.
 * <p>
 * Bounded: a wedged driver can hang the tool, so it gets {@link #TIMEOUT} and is then killed.
 * <p>
 * WHY the output goes to a temporary file rather than a pipe: the tool is waited on before its output is read,
 * and a pipe holds only a few kilobytes before the writer blocks - a tool that printed more than that would
 * never exit, and the timeout would call a healthy driver hung. A file has no such ceiling.
 */
public final class NvidiaSmi {

    private static final Logger log = LogManager.getLogger(NvidiaSmi.class);

    public static final Duration TIMEOUT = Duration.ofSeconds(5);

    private NvidiaSmi() {
    }

    /**
     * What the tool said.
     */
    public sealed interface Answer {

        /**
         * It ran and printed something: one line per card for a {@code --query-gpu} call.
         */
        record Lines(List<String> lines) implements Answer {
            public Lines {
                lines = List.copyOf(lines);
            }
        }

        /**
         * Nothing to report: no NVIDIA driver here, or the tool failed or printed nothing.
         */
        record Nothing() implements Answer {
        }

        /**
         * It ran but did not finish within {@link #TIMEOUT}.
         */
        record NoAnswer() implements Answer {
        }

        /**
         * Its output could not be read back.
         */
        record Unreadable(IOException cause) implements Answer {
        }
    }

    /**
     * Runs {@code nvidia-smi} with {@code arguments} and returns its stripped, non-blank output lines.
     */
    public static Answer query(String... arguments) {
        Path output;
        try {
            output = Files.createTempFile("nvidia-smi", ".txt");
        } catch (IOException e) {
            return new Answer.Unreadable(e);
        }
        try {
            return run(output, arguments);
        } finally {
            try {
                Files.deleteIfExists(output);
            } catch (IOException e) {
                log.debug("Could not delete {}", output, e);
            }
        }
    }

    private static Answer run(Path output, String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("nvidia-smi");
        command.addAll(List.of(arguments));
        Process process;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(output.toFile())
                    .start();
        } catch (IOException notInstalled) {
            return new Answer.Nothing();
        }
        try {
            if (!process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new Answer.NoAnswer();
            }
            String printed = Files.readString(output, StandardCharsets.UTF_8);
            if (process.exitValue() != 0 || printed.isBlank()) return new Answer.Nothing();
            List<String> lines = printed.strip().lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
            return new Answer.Lines(lines);
        } catch (IOException e) {
            return new Answer.Unreadable(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new Answer.Nothing();
        }
    }
}
