package elite.intel.ui.support;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The road a simulated key press takes to the game on a Wayland session, for the support bundle manifest.
 * <p>
 * WHY: the app presses keys through XTEST, and on Wayland that lands in Xwayland. An Xwayland built with
 * libei does not keep the press: it hands it to the compositor, which may ask permission for it (KWin's
 * "Control input devices" prompt) and, in some KWin releases, drops the held Ctrl/Alt/Shift on the way
 * (KDE bug 519690), so every modifier chord reaches the game as its bare key. One distro's Xwayland keeps
 * the press and the next one's does not, at the same version number, so the version alone does not say
 * which road a commander is on. Establishing it took a round of {@code ldd} and {@code --version} by hand.
 * <p>
 * Blank anywhere but a Wayland session. Every probe is optional: inside a sandbox the host's Xwayland is
 * neither visible in {@code /proc} nor on disk, and the line then says so rather than guessing.
 */
final class WaylandInputReport {

    private static final Logger log = LogManager.getLogger(WaylandInputReport.class);

    private static final Duration VERSION_TIMEOUT = Duration.ofSeconds(5);
    private static final Path DEFAULT_XWAYLAND = Path.of("/usr/bin/Xwayland");
    private static final Pattern VERSION = Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)*)");
    private static final byte[] LIBEI = "libei.so".getBytes(StandardCharsets.US_ASCII);

    private WaylandInputReport() {
    }

    /**
     * The report as manifest lines, ending in a newline, or blank off a Wayland session.
     */
    static String describe() {
        if (!"wayland".equalsIgnoreCase(System.getenv("XDG_SESSION_TYPE"))) return "";
        StringBuilder text = new StringBuilder();
        String desktop = System.getenv("XDG_CURRENT_DESKTOP");
        if (desktop != null && desktop.toUpperCase().contains("KDE")) {
            text.append("KWin: ").append(versionOf("kwin_wayland", "--version").orElse("unknown")).append('\n');
        }
        return text.append(xwayland()).append('\n').toString();
    }

    private static String xwayland() {
        Path binary = runningXwayland().orElse(DEFAULT_XWAYLAND);
        if (!Files.isRegularFile(binary)) return "Xwayland: not visible to the app (" + binary + ")";
        String version = versionOf(binary.toString(), "-version").orElse("unknown version");
        String libei;
        try {
            libei = linksLibei(Files.readAllBytes(binary))
                    ? "libei linked - key presses pass through the compositor"
                    : "no libei - key presses stay inside Xwayland";
        } catch (IOException e) {
            libei = "libei unknown (" + e + ")";
        }
        return "Xwayland: " + version + " (" + binary + "), " + libei;
    }

    /**
     * The binary of the Xwayland actually serving this session, which on a machine with two installed is not
     * necessarily the one on the default path.
     */
    private static Optional<Path> runningXwayland() {
        try (Stream<Path> processes = Files.list(Path.of("/proc"))) {
            return processes
                    .filter(dir -> dir.getFileName().toString().chars().allMatch(Character::isDigit))
                    .filter(WaylandInputReport::isXwayland)
                    .map(dir -> {
                        try {
                            return Files.readSymbolicLink(dir.resolve("exe"));
                        } catch (IOException | UnsupportedOperationException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .findFirst();
        } catch (IOException | RuntimeException e) {
            log.debug("Could not look for a running Xwayland", e);
            return Optional.empty();
        }
    }

    private static boolean isXwayland(Path processDir) {
        try {
            return "Xwayland".equals(Files.readString(processDir.resolve("comm")).strip());
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /**
     * Whether an ELF binary names libei among the libraries it loads. Its dynamic string table carries the
     * soname verbatim, so finding the name in the file is what {@code ldd} would have answered.
     */
    static boolean linksLibei(byte[] binary) {
        outer:
        for (int i = 0; i <= binary.length - LIBEI.length; i++) {
            for (int j = 0; j < LIBEI.length; j++) {
                if (binary[i + j] != LIBEI[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    /**
     * The first dotted version number a program prints about itself: {@code kwin_wayland 6.5.3} and Xwayland's
     * banner ({@code The X.Org Foundation Xwayland Version 24.1.10 (12401010)}) both lead with it.
     */
    static Optional<String> parseVersion(@Nullable String printed) {
        if (printed == null) return Optional.empty();
        Matcher matcher = VERSION.matcher(printed);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    /**
     * Runs a version query. Output goes to a file rather than a pipe, so a chatty program cannot fill a buffer
     * nobody drains and be mistaken for a hung one (the same reason as {@code HardwareReport.nvidiaSmi}).
     */
    private static Optional<String> versionOf(String... command) {
        Path output = null;
        Process process = null;
        try {
            output = Files.createTempFile("version", ".txt");
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(output.toFile())
                    .start();
            if (!process.waitFor(VERSION_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Optional.empty();
            }
            return parseVersion(Files.readString(output, StandardCharsets.UTF_8));
        } catch (IOException notInstalled) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) process.destroyForcibly();
            return Optional.empty();
        } finally {
            if (output != null) {
                try {
                    Files.deleteIfExists(output);
                } catch (IOException e) {
                    log.debug("Could not delete {}", output, e);
                }
            }
        }
    }
}
