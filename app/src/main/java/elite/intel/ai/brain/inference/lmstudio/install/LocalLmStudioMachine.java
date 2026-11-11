package elite.intel.ai.brain.inference.lmstudio.install;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
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

    private static final String LLMSTER = "llmster.exe";
    /**
     * Where the llmster the app starts on Windows writes its output, in the LM Studio home.
     */
    static final String LLMSTER_LOG = "elite-intel-llmster.log";

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

    /**
     * Starts LM Studio's daemon. On Windows the app starts {@code llmster.exe} itself rather than through
     * {@code lms daemon up}. WHY: llmster started by {@code lms} has no console, so every console program it starts
     * - LM Studio's own {@code node.exe} helpers - opens a console window of its own: several flash up and two stay
     * open, which looks like a virus to a commander. Started by us it has our hidden console, its helpers share it,
     * and no window opens (a Windows tester ran llmster directly and saw none, 2026-11-11).
     */
    @Override
    public void daemonUp(Path lms) throws InstallException {
        if (!isWindows()) {
            runLms(lms, COMMAND, "daemon", "up");
            return;
        }
        if (daemonPid(lms).isPresent()) {
            log.accept("llmster is already running");
            return;
        }
        Optional<Path> llmster = new LmStudioHome(homeOf(lms)).installedPrograms().stream()
                .filter(program -> program.getFileName().toString().equalsIgnoreCase(LLMSTER))
                .findFirst();
        if (llmster.isEmpty()) {
            log.accept("LM Studio names no " + LLMSTER + ", so lms starts it");
            runLms(lms, COMMAND, "daemon", "up");
            return;
        }
        startLlmster(lms, llmster.get());
    }

    private void startLlmster(Path lms, Path llmster) throws InstallException {
        Path output = homeOf(lms).resolve(LLMSTER_LOG);
        ProcessBuilder builder = new ProcessBuilder(llmster.toString())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.to(output.toFile()))
                .directory(Path.of(System.getProperty("user.home")).toFile());
        builder.environment().put("LMS_NO_MODIFY_PATH", "1");
        log.accept("> " + llmster + " (its output goes to " + output + ")");
        Process process;
        try (LmStudioWindowHider.Watch ignored = LmStudioWindowHider.watch(homeOf(lms), log)) {
            try {
                process = builder.start();
            } catch (IOException e) {
                throw new InstallException("Could not start " + llmster + ": " + e.getMessage(), e);
            }
            Instant deadline = Instant.now().plus(COMMAND);
            while (true) {
                OptionalLong pid = daemonPid(lms);
                if (pid.isPresent()) {
                    log.accept("llmster started (PID: " + pid.getAsLong() + ").");
                    return;
                }
                if (!process.isAlive()) {
                    throw new InstallException(llmster + " stopped with exit code " + process.exitValue()
                            + " before it was running (its output is in " + output + ")");
                }
                if (Instant.now().isAfter(deadline)) {
                    process.destroy();
                    throw new InstallException(llmster + " was not running within " + COMMAND.toMinutes() + " min");
                }
                pause(Duration.ofSeconds(1));
            }
        }
    }

    /**
     * Stops LM Studio's daemon. On Windows an llmster {@code lms daemon down} leaves running - one the app started
     * itself may not count as LM Studio's daemon to {@code lms} - is stopped with everything it started.
     */
    @Override
    public void daemonDown(Path lms) throws InstallException {
        if (!isWindows()) {
            runLms(lms, COMMAND, "daemon", "down");
            return;
        }
        InstallException refused = null;
        try {
            runLms(lms, COMMAND, "daemon", "down");
        } catch (InstallException e) {
            refused = e;
        }
        OptionalLong pid = daemonPid(lms);
        if (pid.isEmpty()) return;
        Optional<ProcessHandle> llmster = ProcessHandle.of(pid.getAsLong());
        if (llmster.isEmpty()) return;
        log.accept("lms daemon down left llmster (PID: " + pid.getAsLong() + ") running, so it is stopped directly");
        llmster.get().descendants().forEach(ProcessHandle::destroy);
        llmster.get().destroy();
        try {
            llmster.get().onExit().get(30, TimeUnit.SECONDS);
        } catch (Exception notStopped) {
            if (notStopped instanceof InterruptedException) Thread.currentThread().interrupt();
            throw refused != null ? refused : new InstallException("llmster (PID: " + pid.getAsLong() + ") did not stop");
        }
    }

    /**
     * The PID of the running llmster, from {@code lms daemon status --json}; empty when none runs.
     */
    private OptionalLong daemonPid(Path lms) throws InstallException {
        List<String> output = runner.run(List.of(lms.toString(), "daemon", "status", "--json"), COMMAND);
        return runningPid(String.join("\n", output));
    }

    /**
     * Reads {@code lms daemon status --json}: {@code {"status": "running", "pid": 12345, ...}} or
     * {@code {"status": "not-running"}}. Anything else counts as not running.
     */
    static OptionalLong runningPid(String output) {
        int start = output.indexOf('{');
        int end = output.lastIndexOf('}');
        if (start < 0 || end < start) return OptionalLong.empty();
        try {
            JsonObject status = JsonParser.parseString(output.substring(start, end + 1)).getAsJsonObject();
            if (!status.has("status") || !"running".equals(status.get("status").getAsString())) return OptionalLong.empty();
            return status.has("pid") ? OptionalLong.of(status.get("pid").getAsLong()) : OptionalLong.empty();
        } catch (RuntimeException unreadable) {
            return OptionalLong.empty();
        }
    }

    private static boolean isWindows() {
        return OsDetector.getOs() == OsDetector.OS.WINDOWS;
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
