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
import java.util.ArrayList;
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
    private static final Duration LOAD = Duration.ofMinutes(5);
    private static final Duration SERVER_STARTUP = Duration.ofSeconds(60);

    private final ProcessRunner runner;
    private final Consumer<String> log;
    private final LmStudioServer server = new LmStudioServer();

    LocalLmStudioMachine(Consumer<String> log) {
        this.runner = new ProcessRunner(log);
        this.log = log;
    }

    /**
     * Runs an {@code lms} command, hiding the console windows LM Studio opens meanwhile ({@link LmStudioWindowHider}).
     */
    private void runLms(Path lms, Duration timeout, String... arguments) throws InstallException {
        List<String> command = new ArrayList<>();
        command.add(lms.toString());
        command.addAll(List.of(arguments));
        try (LmStudioWindowHider.Watch ignored = LmStudioWindowHider.watch(homeOf(lms), log)) {
            runner.run(command, timeout);
        }
    }

    /**
     * The LM Studio home an {@code lms} belongs to: it lives in {@code <home>/bin}.
     */
    private static Path homeOf(Path lms) {
        Path bin = lms.toAbsolutePath().getParent();
        return bin.getParent() == null ? bin : bin.getParent();
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
        try (LmStudioWindowHider.Watch ignored = LmStudioWindowHider.watch(LmStudioHome.locate().dir(), log)) {
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
        runLms(lms, COMMAND, "daemon", "up");
    }

    @Override
    public void daemonDown(Path lms) throws InstallException {
        runLms(lms, COMMAND, "daemon", "down");
    }

    @Override
    public void startServer(Path lms) throws InstallException {
        runLms(lms, COMMAND, "server", "start", "--port", String.valueOf(LmStudioDefaults.PORT), "--bind", "127.0.0.1");
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
        runLms(lms, COMMAND, "server", "stop");
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
     */
    @Override
    public void load(Path lms, String model) throws InstallException {
        runLms(lms, LOAD, "load", model, "--context-length", String.valueOf(CONTEXT_LENGTH), "--gpu", "max", "--yes");
    }

    @Override
    public void unload(Path lms, String model) throws InstallException {
        runLms(lms, COMMAND, "unload", model);
    }

    @Override
    public void unloadAll(Path lms) throws InstallException {
        runLms(lms, COMMAND, "unload", "--all");
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
