package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadStart;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadState;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadStatus;
import elite.intel.util.OsDetector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Answers as a healthy PC would, unless a test says otherwise, and records what it was asked to do. Its script
 * "installs" by creating {@code lms} and llmster's install record. Changes nothing on the PC.
 */
final class FakeLmStudioMachine implements LmStudioMachine {
    final Path home;
    final List<String> calls = new ArrayList<>();
    final List<Path> pointers = new ArrayList<>();
    boolean scriptInstallsLms = true;
    boolean serverAnswering;
    boolean loaded;
    /**
     * The context window the model is loaded with, while {@link #loaded}.
     */
    int loadedContext = LocalLmStudioMachine.CONTEXT_LENGTH;
    boolean toolCall = true;
    /**
     * How many test requests get no answer in time before one is answered.
     */
    int slowTestAnswers;
    /**
     * How many more times the model list is asked before it lists the model; negative never lists it.
     */
    int listedAfter;
    int listingAsks;
    boolean cancelled;
    DownloadStart downloadStart = new DownloadStart.Started("job_1");
    /**
     * What every start after the first returns; null returns {@link #downloadStart} again.
     */
    DownloadStart downloadStartsAfterFirst;
    Deque<DownloadStatus> statuses = new ArrayDeque<>(List.of(
            new DownloadStatus(DownloadState.COMPLETED, 6_000, 6_000, 0, Optional.empty())));
    Deque<OptionalLong> gpuReadings = new ArrayDeque<>(List.of(OptionalLong.of(2_000), OptionalLong.of(8_500)));
    Runnable onStatus = () -> {
    };

    FakeLmStudioMachine(Path home) {
        this.home = home;
    }

    /**
     * What LM Studio's script leaves: {@code lms}, and llmster with the record that names it.
     */
    void installLms() throws IOException {
        leaveOnlyLms();
        Path llmster = Files.createDirectories(home.resolve("llmster").resolve("0.0.27-1")).resolve("llmster");
        Files.writeString(llmster, "");
        LmStudioHomeTest.writeRecord(home, LmStudioHome.INSTALL_RECORDS.getFirst(), llmster);
    }

    /**
     * What removing LM Studio leaves: its home with {@code lms} still in {@code bin}, but nothing for it to start.
     */
    void leaveOnlyLms() throws IOException {
        Path bin = Files.createDirectories(home.resolve("bin"));
        Files.writeString(bin.resolve(OsDetector.getOs() == OsDetector.OS.WINDOWS ? "lms.exe" : "lms"), "");
    }

    @Override
    public void writePointer(Path home) {
        pointers.add(home);
    }

    @Override
    public void runInstallScript() throws InstallException {
        calls.add("script");
        if (!scriptInstallsLms) return;
        try {
            installLms();
        } catch (IOException e) {
            throw new InstallException(e.getMessage(), e);
        }
    }

    @Override
    public void daemonUp(Path lms) throws InstallException {
        if (cancelled) throw new InstallException("Cancelled");
        calls.add("daemon up");
    }

    @Override
    public void daemonDown(Path lms) throws InstallException {
        if (cancelled) throw new InstallException("Cancelled");
        calls.add("daemon down");
    }

    @Override
    public void startServer(Path lms) {
        calls.add("server start");
        serverAnswering = true;
    }

    @Override
    public boolean isServerAnswering() {
        return serverAnswering;
    }

    @Override
    public void stopServer(Path lms) {
        calls.add("server stop");
        serverAnswering = false;
    }

    @Override
    public DownloadStart startDownload(String model) {
        boolean first = !calls.contains("download");
        calls.add("download");
        return first || downloadStartsAfterFirst == null ? downloadStart : downloadStartsAfterFirst;
    }

    @Override
    public DownloadStatus downloadStatus(String jobId) {
        onStatus.run();
        return statuses.size() > 1 ? statuses.poll() : statuses.peek();
    }

    @Override
    public boolean isListed(String model) {
        listingAsks++;
        if (listedAfter < 0) return false;
        return listedAfter-- <= 0;
    }

    @Override
    public boolean isLoaded(String model) {
        return loaded;
    }

    @Override
    public OptionalInt loadedContextLength(String model) {
        return loaded && loadedContext > 0 ? OptionalInt.of(loadedContext) : OptionalInt.empty();
    }

    @Override
    public void load(Path lms, String model) {
        calls.add("load");
        loaded = true;
        loadedContext = LocalLmStudioMachine.CONTEXT_LENGTH;
    }

    @Override
    public void unload(Path lms, String model) {
        calls.add("unload");
        loaded = false;
    }

    @Override
    public void unloadAll(Path lms) {
        calls.add("unload all");
        loaded = false;
    }

    @Override
    public OptionalLong gpuMemoryUsedMib() {
        return gpuReadings.size() > 1 ? gpuReadings.poll() : gpuReadings.peek();
    }

    @Override
    public boolean answersWithToolCall(String model) throws InstallException {
        calls.add("test");
        if (slowTestAnswers > 0) {
            slowTestAnswers--;
            throw new NoAnswerInTimeException("LM Studio did not answer the test: request timed out", null);
        }
        return toolCall;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
}
