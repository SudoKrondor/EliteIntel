package elite.intel.util;

import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.OsDetector.OS;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static elite.intel.session.SystemSession.getInstance;
import static elite.intel.util.StringUtls.normalizeVersion;

/**
 * Handles version checking and update delegation for EliteIntel.
 * <p>
 * When an update is requested, the main application is not responsible for
 * downloading or unpacking anything. Instead, it locates the companion
 * {@code elite_intel_updater.jar} sitting alongside the main jar, launches it
 * as a separate process (passing the install directory as the first argument),
 * and then exits.  All download / extraction / relaunch logic lives in
 * {@code UpdaterApp} inside that companion jar.
 * <p>
 * The updater jar is intentionally tiny - no native STT/TTS/LLM dependencies -
 * so it starts in under a second even on modest hardware.
 */
public class Updater {

    /**
     * Name of the companion updater jar, expected in the same directory as the main jar.
     */
    private static final String UPDATER_JAR_NAME = "elite_intel_updater.jar";

    private static final Path JAR_DIR = resolveJarDirectory();

    private Updater() {
    }

    // -- Directory resolution --------------------------------------------------

    private static Path resolveJarDirectory() {
        try {
            URI uri = Updater.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();
            return Path.of(uri).getParent();
        } catch (Exception e) {
            throw new RuntimeException("Cannot detect JAR directory", e);
        }
    }

    // -- Public API ------------------------------------------------------------

    /**
     * Launches the companion updater jar in a separate process, then signals the
     * caller that the main application should exit.
     * <p>
     * Returns {@code true} when the updater process was successfully spawned
     * (the caller should call {@code System.exit(0)} after this).
     * Returns {@code false} if the updater jar is missing or the process cannot
     * be started, in which case the main app stays running.
     *
     * @return a {@code CompletableFuture<Boolean>} - {@code true} means "exit now".
     */
    public static CompletableFuture<Boolean> performUpdateAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Path updaterJar = JAR_DIR.resolve(UPDATER_JAR_NAME);

            if (!updaterJar.toFile().exists()) {
                UiBus.publish(new AppLogEvent(
                        "Updater jar not found: " + updaterJar));
                return false;
            }

            try {
                List<String> command = buildLaunchCommand(updaterJar);
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(JAR_DIR.toFile());
                pb.inheritIO();    // updater writes to its own window, not ours
                pb.start();
                return true;       // caller should now exit

            } catch (IOException e) {
                UiBus.publish(new AppLogEvent(
                        "Failed to launch updater: " + e.getMessage()));
                return false;
            }
        });
    }

    /**
     * Checks asynchronously whether a newer version is available on GitHub.
     *
     * @return {@code true} if a newer version exists; {@code false} if up-to-date
     *         or the check could not be completed.
     */
    public static CompletableFuture<Boolean> isUpdateAvailableAsync() {
        return CompletableFuture.supplyAsync(() -> {
            String local = normalizeVersion(getInstance().readVersionFromResources());
            if (local.isBlank()) return false;

            long localBuild = StringUtls.getNumericBuild(local);

            try (HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "https://github.com/stone-alex/EliteIntel/releases/latest"))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String path = response.uri().getPath();          // e.g. /releases/tag/v-0.0316-beta
                    String tag = path.substring(path.lastIndexOf('/') + 1);
                    long remoteBuild = StringUtls.getNumericBuild(tag);
                    return remoteBuild > localBuild;
                }
            } catch (Exception e) {
                UiBus.publish(new AppLogEvent(
                        "Update check failed: " + e.getMessage()));
            }
            return false;
        });
    }

    // -- Private helpers -------------------------------------------------------

    /**
     * Builds the OS-appropriate command to launch the updater jar.
     * On Windows we use {@code javaw} (no console window); on Linux/macOS we
     * prefer the bundled JDK installed at {@code <installDir>/jdk/bin/java} by
     * installer.sh, falling back to whatever {@code java} is on PATH.
     */
    private static List<String> buildLaunchCommand(Path updaterJar) {
        List<String> cmd = new ArrayList<>();
        cmd.add(resolveJavaExecutable());
        cmd.add("-jar");
        cmd.add(updaterJar.toAbsolutePath().toString());
        cmd.add(JAR_DIR.toAbsolutePath().toString());        // argv[0] = install dir
        cmd.add(String.valueOf(ProcessHandle.current().pid())); // argv[1] = main app PID
        return cmd;
    }

    private static String resolveJavaExecutable() {
        OS os = OsDetector.getOs();
        if (os == OS.WINDOWS) return "javaw";
        if (os == OS.LINUX) {
            Path bundled = JAR_DIR.resolve("jdk/bin/java");
            if (bundled.toFile().exists())
                return bundled.toAbsolutePath().toString();
        }
        return "java";
    }
}