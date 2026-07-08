package elite.intel.diagnostics;

import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Single writer of the diagnostics session log ({@code session.log}). Every diagnostic line - mirrored GUI
 * SYSTEM LOG events and the harness {@code DIAG} markers - funnels through {@link #write(String)}, so the
 * file has one owner and one format. Lines carry a UTC, journal-style timestamp so a saved line lines up
 * with the game journal for debugging.
 * <p>
 * Active only in diagnostics mode. {@link #open()} truncates {@code session.log} at startup (never the phrase
 * input file, which is the tester-owned mode gate). All methods are no-ops if the log could not be opened, so a
 * diagnostics I/O problem never breaks app startup or a live turn; the first such failure is logged at debug.
 */
public final class DiagnosticsLog {

    private static final Logger log = LogManager.getLogger(DiagnosticsLog.class);
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    private static final Object LOCK = new Object();
    private static BufferedWriter writer;

    private DiagnosticsLog() {
    }

    /**
     * Creates the diagnostics dir and truncates {@code session.log}; call once at startup. Deliberately does
     * NOT touch {@code input.txt}: that file is the mode gate, owned entirely by the tester (create before
     * launch, delete after). If the app recreated it, the gate would outlive the run and re-enable diagnostics
     * on every later launch.
     */
    public static void open() {
        try {
            Path dir = AppPaths.getDiagnosticsDir();
            Files.createDirectories(dir);
            synchronized (LOCK) {
                writer = Files.newBufferedWriter(AppPaths.getDiagnosticsLogFile(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            }
            write("DIAG log opened");
        } catch (IOException e) {
            // diagnostics is a dev harness; never fail app startup because the log won't open, but leave a trace
            log.debug("Diagnostics session log could not be opened; diagnostics output disabled", e);
            synchronized (LOCK) {
                writer = null;
            }
        }
    }

    /** Appends one UTC-stamped line (newlines in the message are flattened); no-op if the log is not open. Thread-safe. */
    public static void write(String line) {
        synchronized (LOCK) {
            if (writer == null) {
                return;
            }
            try {
                writer.write(TS.format(Instant.now()));
                writer.write(' ');
                writer.write(line == null ? "" : line.replace('\n', ' ').replace('\r', ' '));
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                // keep the app alive even if a single log write fails; trace it for a broken-harness diagnosis
                log.debug("Diagnostics log write failed", e);
            }
        }
    }
}
