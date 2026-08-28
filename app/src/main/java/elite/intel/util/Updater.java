package elite.intel.util;

import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.OsDetector.OS;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
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

            List<String> command = buildLaunchCommand(updaterJar);
            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(JAR_DIR.toFile());
                pb.inheritIO();    // updater writes to its own window, not ours
                pb.start();
                return true;       // caller should now exit

            } catch (IOException e) {
                UiBus.publish(new AppLogEvent(
                        "Failed to launch updater with '" + command.get(0) + "': " + e.getMessage()));
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
     * Builds the command to launch the updater jar. On every platform we
     * resolve a concrete java binary rather than trusting PATH - see
     * {@link #resolveJavaExecutable()} - because a bundled-runtime install has
     * no {@code java} on PATH at all.
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

    /**
     * Finds a java binary to launch the updater with.
     * <p>
     * A bundled-runtime install has no system JDK at all, so a bare
     * {@code "java"} / {@code "javaw"} fails with "Cannot run program ..." and
     * the updater silently never appears. That is the *normal* case on Windows:
     * the install4j media set bundles a JRE, so a commander who installed
     * EliteIntel has no reason to own one on PATH.
     * <p>
     * In preference order:
     * <ol>
     *   <li>the runtime running us right now ({@code java.home}) - for an
     *       install4j install that is the bundled JRE, so it always matches
     *       what the launcher used;</li>
     *   <li>{@code <installDir>/jre/bin} - the install4j bundled-JRE layout;</li>
     *   <li>{@code <installDir>/jdk/bin} - the older zip/installer.sh layout;</li>
     *   <li>{@code $JAVA_HOME/bin};</li>
     *   <li>the bare executable name, letting the OS search PATH, as a last resort.</li>
     * </ol>
     * Within each candidate runtime Windows prefers {@code javaw.exe} (no
     * console window) and falls back to {@code java.exe} in the same runtime.
     */
    private static String resolveJavaExecutable() {
        List<String> names = OsDetector.getOs() == OS.WINDOWS
                ? List.of("javaw.exe", "java.exe")
                : List.of("java");

        for (Path binDir : javaBinDirectories(JAR_DIR)) {
            for (String name : names) {
                Path candidate = binDir.resolve(name);
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate))
                    return candidate.toAbsolutePath().toString();
            }
        }
        return names.get(0);
    }

    /**
     * The {@code bin} directories that may hold a usable runtime, most
     * trustworthy first. See {@link #resolveJavaExecutable()} for the order.
     */
    private static List<Path> javaBinDirectories(Path installDir) {
        List<Path> dirs = new ArrayList<>();

        String javaHome = System.getProperty("java.home", "");
        if (!javaHome.isBlank()) dirs.add(Path.of(javaHome, "bin"));

        dirs.add(installDir.resolve("jre").resolve("bin"));
        dirs.add(installDir.resolve("jdk").resolve("bin"));

        String envHome = System.getenv("JAVA_HOME");
        if (envHome != null && !envHome.isBlank()) dirs.add(Path.of(envHome, "bin"));

        return dirs;
    }
}
