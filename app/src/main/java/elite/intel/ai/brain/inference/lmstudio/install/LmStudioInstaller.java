package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.LmStudioDefaults;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadStart;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadStatus;
import elite.intel.db.managers.LmStudioInstallManager;
import elite.intel.db.managers.LmStudioInstallManager.Install;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Installs LM Studio, or sets up one already here, step by step (docs/LM_STUDIO_INSTALL_ASSISTANCE.md, "The
 * install"), reporting each step to an {@link InstallProgress}.
 * <p>
 * Every step can be run again: a step whose work is already done is skipped, so an interrupted install is continued
 * simply by running it again. Blocks until the install ends - run it off the event thread.
 */
public final class LmStudioInstaller {

    private static final Logger log = LogManager.getLogger(LmStudioInstaller.class);

    private static final String MODEL = LmStudioDefaults.MODEL;
    private static final double BYTES_PER_GB = 1024.0 * 1024 * 1024;

    /**
     * Writes the Address and Model the install ends with. The AI Services panel's own save, so the brain restarts as
     * it does for any other change.
     */
    @FunctionalInterface
    public interface FieldsSaver {
        void save(String address, String model) throws InstallException;
    }

    private final LmStudioMachine machine;
    private final InstallProgress progress;
    private final FieldsSaver saver;
    private final LmStudioInstallManager installs;
    private final long requiredBytes;
    private final Duration downloadPoll;
    /**
     * A download whose byte count has not moved for this long is treated as stuck.
     */
    private final Duration downloadStall;

    private volatile boolean cancelled;
    private volatile InstallStep current = InstallStep.CHECK;

    public LmStudioInstaller(InstallProgress progress, FieldsSaver saver) {
        this(new LocalLmStudioMachine(progress::log), progress, saver, LmStudioInstallManager.getInstance(),
                InstallFolder.REQUIRED_BYTES, Duration.ofSeconds(1), Duration.ofMinutes(5));
    }

    LmStudioInstaller(LmStudioMachine machine, InstallProgress progress, FieldsSaver saver,
                      LmStudioInstallManager installs, long requiredBytes, Duration downloadPoll,
                      Duration downloadStall) {
        this.machine = machine;
        this.progress = progress;
        this.saver = saver;
        this.installs = installs;
        this.requiredBytes = requiredBytes;
        this.downloadPoll = downloadPoll;
        this.downloadStall = downloadStall;
    }

    // -------------------------------------------------------------------------
    // The three ways in
    // -------------------------------------------------------------------------

    /**
     * A fresh install into {@code folder}: every step.
     */
    public InstallOutcome install(InstallFolder folder) {
        return run(() -> {
            checkSpace(folder);
            Path lms = installInto(folder);
            setUp(lms, true);
            installs.markSetupComplete();
        });
    }

    /**
     * Picks up the app's own install where it stopped.
     */
    public InstallOutcome continueSetup(Install owned) {
        return run(() -> {
            skip(InstallStep.CHECK, "Continuing the install in " + owned.homeDir());
            Path lms = ensureInstalled(owned);
            setUp(lms, true);
            installs.markSetupComplete();
        });
    }

    /**
     * An LM Studio the app did not install: the model and the fields only. Nothing is recorded, so the app never
     * starts, stops or uninstalls it.
     */
    public InstallOutcome setUpExisting(LmStudioHome home) {
        return run(() -> {
            current = InstallStep.START;
            Path lms = home.lmsCommand()
                    .orElseThrow(() -> new InstallException("No lms command in " + home.dir().resolve("bin")));
            setUp(lms, false);
        });
    }

    /**
     * Stops the install at the next chance. During the download of the app's own install it also shuts LM Studio's
     * daemon down, which pauses the download so the next attempt resumes it.
     */
    public void cancel() {
        cancelled = true;
        // WHY not during the download: the download loop pauses it with daemon down, which a cancelled machine
        // would refuse to run.
        if (current != InstallStep.DOWNLOAD) machine.cancel();
    }

    @FunctionalInterface
    private interface Steps {
        void run() throws InstallException;
    }

    private InstallOutcome run(Steps steps) {
        try {
            steps.run();
            progress.log("LM Studio is set up.");
            return new InstallOutcome.Completed();
        } catch (InstallException e) {
            if (cancelled) {
                progress.log("Stopped.");
                return new InstallOutcome.Cancelled();
            }
            return failed(e.getMessage());
        } catch (RuntimeException e) {
            // WHY: broad catch at the install's top level - it runs on a worker thread the dialog waits on, and a
            // database or programming error must end as a failed step the commander sees, not a dialog left waiting.
            log.error("LM Studio install failed at {}", current, e);
            return failed(String.valueOf(e.getMessage()));
        }
    }

    private InstallOutcome failed(String reason) {
        progress.log("Failed: " + reason);
        return new InstallOutcome.Failed(current, reason);
    }

    // -------------------------------------------------------------------------
    // Getting LM Studio onto the PC
    // -------------------------------------------------------------------------

    private void checkSpace(InstallFolder folder) throws InstallException {
        begin(InstallStep.CHECK);
        long free;
        try {
            free = folder.usableBytes();
        } catch (IOException e) {
            throw new InstallException("Could not tell how much space is free for " + folder.home() + ": " + e.getMessage(), e);
        }
        progress.log(String.format("%.1f GB free for %s", free / BYTES_PER_GB, folder.home()));
        if (free < requiredBytes) {
            throw new InstallException(String.format("Only %.1f GB free for %s, and %.0f GB is needed",
                    free / BYTES_PER_GB, folder.home(), requiredBytes / BYTES_PER_GB));
        }
        finish(InstallStep.CHECK);
    }

    private Path installInto(InstallFolder folder) throws InstallException {
        begin(InstallStep.INSTALL);
        Optional<Install> stale = installs.find();
        if (stale.isPresent()) {
            // WHY: only reachable for a record whose marker no longer matches - a live one is continued or
            // uninstalled instead - so it describes no install the app can still reach.
            progress.log("Replacing the record of an earlier install in " + stale.get().homeDir());
            installs.forget();
        }
        try {
            Files.createDirectories(folder.home());
            Install owned = installs.recordStarted(folder.home(), folder.writesPointer());
            InstallMarker.write(folder.home(), owned.installId());
        } catch (IOException e) {
            throw new InstallException("Could not prepare " + folder.home() + ": " + e.getMessage(), e);
        }
        if (folder.writesPointer()) machine.writePointer(folder.home());
        machine.runInstallScript();
        Path lms = lmsIn(folder.home());
        finish(InstallStep.INSTALL);
        return lms;
    }

    private Path ensureInstalled(Install owned) throws InstallException {
        Optional<Path> installed = new LmStudioHome(owned.homeDir()).lmsCommand();
        if (installed.isPresent()) {
            skip(InstallStep.INSTALL, "LM Studio is already installed in " + owned.homeDir());
            return installed.get();
        }
        begin(InstallStep.INSTALL);
        if (owned.pointerWritten()) machine.writePointer(owned.homeDir());
        machine.runInstallScript();
        Path lms = lmsIn(owned.homeDir());
        finish(InstallStep.INSTALL);
        return lms;
    }

    private static Path lmsIn(Path home) throws InstallException {
        return new LmStudioHome(home).lmsCommand().orElseThrow(() -> new InstallException(
                "LM Studio's installer finished, but there is no lms command in " + home.resolve("bin")));
    }

    // -------------------------------------------------------------------------
    // Getting the model served
    // -------------------------------------------------------------------------

    private void setUp(Path lms, boolean owned) throws InstallException {
        start(lms);
        download(lms, owned);
        OptionalLong before = load(lms);
        checkGpu(before);
        test();
        save();
    }

    private void start(Path lms) throws InstallException {
        current = InstallStep.START;
        if (machine.isServerAnswering()) {
            skip(InstallStep.START, "LM Studio's server is already running");
            return;
        }
        begin(InstallStep.START);
        machine.daemonUp(lms);
        machine.startServer(lms);
        finish(InstallStep.START);
    }

    private void download(Path lms, boolean owned) throws InstallException {
        current = InstallStep.DOWNLOAD;
        throwIfCancelled();
        DownloadStart start = machine.startDownload(MODEL);
        if (start instanceof DownloadStart.Started(String jobId)) {
            begin(InstallStep.DOWNLOAD);
            awaitDownload(jobId, lms, owned);
            finish(InstallStep.DOWNLOAD);
        } else {
            skip(InstallStep.DOWNLOAD, MODEL + " is already downloaded");
        }
    }

    // WHY one loop of ~40 lines: polling, the stall clock and the cancel all act on the same status, once a second.
    private void awaitDownload(String jobId, Path lms, boolean owned) throws InstallException {
        long lastBytes = -1;
        Instant lastMoved = Instant.now();
        while (true) {
            if (cancelled) {
                if (owned) {
                    progress.log("Pausing the download. Continue setup resumes it.");
                    machine.daemonDown(lms);
                }
                throw new InstallException("Cancelled");
            }
            DownloadStatus status = machine.downloadStatus(jobId);
            progress.download(status.downloadedBytes(), status.totalBytes(), status.bytesPerSecond());
            switch (status.state()) {
                case COMPLETED -> {
                    return;
                }
                case FAILED -> throw new InstallException("LM Studio reported that the download of " + MODEL + " failed");
                case PAUSED -> throw new InstallException("The download of " + MODEL + " was paused");
                case DOWNLOADING -> {
                }
            }
            if (status.downloadedBytes() != lastBytes) {
                lastBytes = status.downloadedBytes();
                lastMoved = Instant.now();
            } else if (Duration.between(lastMoved, Instant.now()).compareTo(downloadStall) >= 0) {
                throw new InstallException("The download of " + MODEL + " made no progress for "
                        + downloadStall.toMinutes() + " min");
            }
            pause(downloadPoll);
        }
    }

    /**
     * Loads the model with Elite Intel's settings, unloading it first if it is loaded already - with settings that may
     * not be these, and so that the GPU check measures this load.
     *
     * @return GPU memory in use just before the load
     */
    private OptionalLong load(Path lms) throws InstallException {
        begin(InstallStep.LOAD);
        if (machine.isLoaded(MODEL)) {
            progress.log(MODEL + " is loaded already. Reloading it with Elite Intel's settings.");
            machine.unload(lms, MODEL);
        }
        OptionalLong before = machine.gpuMemoryUsedMib();
        machine.load(lms, MODEL);
        finish(InstallStep.LOAD);
        return before;
    }

    private void checkGpu(OptionalLong before) throws InstallException {
        begin(InstallStep.GPU_CHECK);
        OptionalLong after = machine.gpuMemoryUsedMib();
        if (before.isEmpty() || after.isEmpty()) {
            throw new InstallException("nvidia-smi could not say how much graphics memory is in use");
        }
        long rise = after.getAsLong() - before.getAsLong();
        progress.log("Graphics memory in use rose by " + rise + " MiB");
        if (rise < GpuMemory.MODEL_ON_GPU_MIB) {
            throw new InstallException("Graphics memory rose by only " + rise
                    + " MiB, so the model is not on the graphics card");
        }
        finish(InstallStep.GPU_CHECK);
    }

    private void test() throws InstallException {
        begin(InstallStep.TEST);
        if (!machine.answersWithToolCall(MODEL)) {
            throw new InstallException("LM Studio answered the test request without a tool call");
        }
        finish(InstallStep.TEST);
    }

    private void save() throws InstallException {
        begin(InstallStep.SAVE);
        saver.save(LmStudioDefaults.ADDRESS, MODEL);
        finish(InstallStep.SAVE);
    }

    // -------------------------------------------------------------------------
    // Step bookkeeping
    // -------------------------------------------------------------------------

    private void begin(InstallStep step) throws InstallException {
        current = step;
        throwIfCancelled();
        progress.stepStarted(step);
    }

    private void finish(InstallStep step) {
        progress.stepFinished(step);
    }

    private void skip(InstallStep step, String why) {
        current = step;
        progress.log(why);
        progress.stepSkipped(step);
    }

    private void throwIfCancelled() throws InstallException {
        if (cancelled) throw new InstallException("Cancelled");
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
