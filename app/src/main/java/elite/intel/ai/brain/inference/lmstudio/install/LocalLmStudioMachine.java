package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.LmStudioDefaults;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadStart;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadStatus;
import elite.intel.util.OsDetector;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Consumer;

/**
 * The real {@link LmStudioMachine}: LM Studio's own script and {@code lms}, its server on this PC, and
 * the graphics memory readings ({@link GpuMemory}).
 */
final class LocalLmStudioMachine implements LmStudioMachine {

    private static final URI WINDOWS_SCRIPT = URI.create("https://lmstudio.ai/install.ps1");
    private static final URI LINUX_SCRIPT = URI.create("https://lmstudio.ai/install.sh");

    /**
     * The context window VEGA runs on, exactly. Found by trial against the V1.0 brain and tested again on Linux and
     * Windows for V1.1: a smaller window makes the model hallucinate, a larger one makes each turn too slow.
     */
    static final int CONTEXT_LENGTH = 8192;

    private static final Duration SCRIPT = Duration.ofMinutes(30);
    private static final Duration COMMAND = Duration.ofMinutes(2);
    /**
     * How long {@code lms load} may go without reporting progress, and how long it may take in all. WHY not one fixed
     * limit: a Windows tester's first load on a new install crawled through its last few percent and ran past 5 minutes,
     * while every load after it took 5 seconds (2026-11-10). Each percent took longest just before the end (97 to 98 took
     * 71 s), so the quiet limit leaves a wide margin over that.
     */
    private static final Duration LOAD_QUIET = Duration.ofMinutes(5);
    private static final Duration LOAD = Duration.ofMinutes(30);
    private static final Duration SERVER_STARTUP = Duration.ofSeconds(60);

    private final ProcessRunner runner;
    private final LmStudioServer server = new LmStudioServer();

    LocalLmStudioMachine(Consumer<String> log) {
        this.runner = new ProcessRunner(log);
    }

    @Override
    public void writePointer(Path home) throws InstallException {
        try {
            LmStudioHome.writePointer(home);
        } catch (IOException e) {
            throw new InstallException("Could not point LM Studio at " + home + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void runInstallScript() throws InstallException {
        boolean windows = OsDetector.getOs() == OsDetector.OS.WINDOWS;
        Path script = download(windows ? WINDOWS_SCRIPT : LINUX_SCRIPT, windows ? ".ps1" : ".sh");
        try {
            runner.run(windows
                    ? List.of("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.toString())
                    : List.of("bash", script.toString()), SCRIPT);
        } finally {
            try {
                Files.deleteIfExists(script);
            } catch (IOException e) {
                script.toFile().deleteOnExit();
            }
        }
    }

    @Override
    public void daemonUp(Path lms) throws InstallException {
        runner.run(List.of(lms.toString(), "daemon", "up"), COMMAND);
    }

    @Override
    public void daemonDown(Path lms) throws InstallException {
        runner.run(List.of(lms.toString(), "daemon", "down"), COMMAND);
    }

    @Override
    public void startServer(Path lms) throws InstallException {
        runner.run(List.of(lms.toString(), "server", "start", "--port", String.valueOf(LmStudioDefaults.PORT),
                "--bind", "127.0.0.1"), COMMAND);
        Instant deadline = Instant.now().plus(SERVER_STARTUP);
        while (!server.isAnswering()) {
            if (runner.isCancelled()) throw new InstallException("Cancelled");
            if (Instant.now().isAfter(deadline)) {
                throw new InstallException("LM Studio's server did not answer within " + SERVER_STARTUP.toSeconds() + " s");
            }
            pause(Duration.ofSeconds(1));
        }
    }

    @Override
    public boolean isServerAnswering() {
        return server.isAnswering();
    }

    @Override
    public void stopServer(Path lms) throws InstallException {
        runner.run(List.of(lms.toString(), "server", "stop"), COMMAND);
    }

    @Override
    public DownloadStart startDownload(String model) throws InstallException {
        return server.startDownload(model);
    }

    @Override
    public DownloadStatus downloadStatus(String jobId) throws InstallException {
        return server.downloadStatus(jobId);
    }

    @Override
    public boolean isListed(String model) throws InstallException {
        return server.isListed(model);
    }

    @Override
    public boolean isLoaded(String model) throws InstallException {
        return server.isLoaded(model);
    }

    @Override
    public OptionalInt loadedContextLength(String model) throws InstallException {
        return server.loadedContextLength(model);
    }

    /**
     * With the flags the website guides have always used. The CLI rather than REST, because REST has no GPU-offload
     * setting.
     * <p>
     * A load the command gave up on still counts when the server has the model loaded. WHY: {@code lms} only asks the
     * daemon to load it, and the daemon goes on after {@code lms} is stopped - the Windows tester's model was loaded
     * moments after the load step had given up on it (2026-11-10).
     */
    @Override
    public void load(Path lms, String model) throws InstallException {
        try {
            runner.run(List.of(lms.toString(), "load", model, "--context-length", String.valueOf(CONTEXT_LENGTH),
                    "--gpu", "max", "--yes"), LOAD_QUIET, LOAD);
        } catch (InstallException failed) {
            if (runner.isCancelled() || !loadedAfterAll(model)) throw failed;
        }
    }

    private boolean loadedAfterAll(String model) {
        try {
            return server.isLoaded(model);
        } catch (InstallException notAnswering) {
            return false;
        }
    }

    @Override
    public void unload(Path lms, String model) throws InstallException {
        runner.run(List.of(lms.toString(), "unload", model), COMMAND);
    }

    @Override
    public void unloadAll(Path lms) throws InstallException {
        runner.run(List.of(lms.toString(), "unload", "--all"), COMMAND);
    }

    @Override
    public OptionalLong gpuMemoryUsedMib() {
        return GpuMemory.usedMib();
    }

    @Override
    public boolean answersWithToolCall(String model) throws InstallException {
        return server.answersWithToolCall(model);
    }

    @Override
    public void cancel() {
        runner.cancel();
    }

    private static Path download(URI script, String extension) throws InstallException {
        try {
            Path file = Files.createTempFile("lmstudio-install", extension);
            HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpResponse<Path> response = http.send(HttpRequest.newBuilder(script).GET().build(),
                    HttpResponse.BodyHandlers.ofFile(file));
            if (response.statusCode() != 200) {
                Files.deleteIfExists(file);
                throw new InstallException("Could not download LM Studio's installer from " + script
                        + ": HTTP " + response.statusCode());
            }
            return file;
        } catch (IOException e) {
            throw new InstallException("Could not download LM Studio's installer from " + script + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InstallException("Interrupted while downloading LM Studio's installer", e);
        }
    }

    private static void pause(Duration duration) throws InstallException {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InstallException("Interrupted", e);
        }
    }
}
