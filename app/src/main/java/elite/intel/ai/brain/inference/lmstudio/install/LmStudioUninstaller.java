package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.LmStudioDefaults;
import elite.intel.db.managers.LmStudioInstallManager;
import elite.intel.db.managers.LmStudioInstallManager.Install;
import elite.intel.io.FileTrees;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Removes the LM Studio the app installed (docs/LM_STUDIO_INSTALL_ASSISTANCE.md, "Uninstall"): stops it, deletes its
 * home with everything in it, takes back the pointer and the PATH entry, clears the Model setting, and forgets the
 * install.
 * <p>
 * Only ever the app's own install: the home must still hold this install's {@link InstallMarker}. Blocks until done -
 * run it off the event thread.
 */
public final class LmStudioUninstaller {

    private static final Logger log = LogManager.getLogger(LmStudioUninstaller.class);

    /**
     * On Windows a process that has just exited can hold its files for a moment, so the delete is tried again.
     */
    private static final int DELETE_ATTEMPTS = 3;

    /**
     * How the uninstall ended.
     */
    public sealed interface Outcome {
        record Uninstalled() implements Outcome {
        }

        record Failed(String reason) implements Outcome {
        }
    }

    /**
     * A model file in the install, for the confirmation to list before anything is deleted.
     */
    public record ModelFile(String name, long bytes) {
    }

    private final Runnable stopHost;
    private final LmStudioInstaller.FieldsSaver saver;
    private final LmStudioMachine machine;
    private final UserPath userPath;
    private final Path userHome;
    private final LmStudioInstallManager installs;
    private final Duration deleteRetryPause;
    private final Consumer<String> report;

    /**
     * @param stopHost stops the service running the app's own LM Studio, if it runs, and returns once it has
     * @param saver    writes the Address and the now empty Model
     * @param report   one line for each thing done, for the app log; each is written to the LM Studio log as well
     */
    public LmStudioUninstaller(Runnable stopHost, LmStudioInstaller.FieldsSaver saver, Consumer<String> report) {
        this(stopHost, saver, new LocalLmStudioMachine(line -> LmStudioUninstaller.log.info("lms: {}", line)),
                UserPath.forThisPc(), Path.of(System.getProperty("user.home")), LmStudioInstallManager.getInstance(),
                Duration.ofSeconds(2), line -> {
                    LmStudioUninstaller.log.info(line);
                    report.accept(line);
                });
    }

    LmStudioUninstaller(Runnable stopHost, LmStudioInstaller.FieldsSaver saver, LmStudioMachine machine,
                        UserPath userPath, Path userHome, LmStudioInstallManager installs, Duration deleteRetryPause,
                        Consumer<String> report) {
        this.stopHost = stopHost;
        this.saver = saver;
        this.machine = machine;
        this.userPath = userPath;
        this.userHome = userHome;
        this.installs = installs;
        this.deleteRetryPause = deleteRetryPause;
        this.report = report;
    }

    /**
     * Every model file under the install's {@code models} folder, largest first.
     */
    public static List<ModelFile> modelsIn(Install owned) {
        Path models = owned.homeDir().resolve("models");
        if (!Files.isDirectory(models)) return List.of();
        try (Stream<Path> walk = Files.walk(models)) {
            return walk.filter(path -> path.getFileName().toString().endsWith(".gguf"))
                    .filter(Files::isRegularFile)
                    .map(LmStudioUninstaller::modelFile)
                    .sorted(Comparator.comparingLong(ModelFile::bytes).reversed())
                    .toList();
        } catch (IOException e) {
            log.warn("Could not list the models in {}", models, e);
            return List.of();
        }
    }

    private static ModelFile modelFile(Path file) {
        try {
            return new ModelFile(file.getFileName().toString(), Files.size(file));
        } catch (IOException e) {
            return new ModelFile(file.getFileName().toString(), 0);
        }
    }

    public Outcome uninstall(Install owned) {
        Path home = owned.homeDir();
        log.info("Uninstalling the app's install in {} ({} {})", home, System.getProperty("os.name"),
                System.getProperty("os.version"));
        Optional<String> refusal = refusal(owned);
        if (refusal.isPresent()) {
            report.accept("Not uninstalled: " + refusal.get());
            return new Outcome.Failed(refusal.get());
        }
        try {
            stop(home);
            delete(home);
            if (owned.pointerWritten()) removePointer(home);
            removePathEntry(home.resolve("bin"));
            installs.forget();
            saver.save(LmStudioDefaults.ADDRESS, "");
            report.accept("LM Studio is uninstalled.");
            return new Outcome.Uninstalled();
        } catch (InstallException e) {
            report.accept("Uninstall failed: " + e.getMessage());
            return new Outcome.Failed(e.getMessage());
        }
    }

    /**
     * Why {@code owned}'s home must not be deleted, if it must not: it is no longer the app's install, or it is a
     * folder no install can be - the commander's own home, a folder above it, or a drive root.
     */
    Optional<String> refusal(Install owned) {
        Path home = owned.homeDir().toAbsolutePath().normalize();
        Path user = userHome.toAbsolutePath().normalize();
        if (!InstallMarker.matches(home, owned.installId())) {
            return Optional.of(home + " is no longer the LM Studio Elite Intel installed");
        }
        if (home.getParent() == null || user.startsWith(home)) {
            return Optional.of(home + " is not a folder Elite Intel would install LM Studio into");
        }
        return Optional.empty();
    }

    /**
     * Stops LM Studio whoever started it: the service, or the install that just ran with the services stopped.
     * WHY each step best-effort: the delete that follows is what matters, and it says so itself if LM Studio still
     * holds its files.
     */
    private void stop(Path home) {
        stopHost.run();
        Optional<Path> lms = new LmStudioHome(home).lmsCommand();
        if (lms.isEmpty()) return;
        if (machine.isServerAnswering()) {
            tryTo("unload the model", () -> machine.unloadAll(lms.get()));
            tryTo("stop the server", () -> machine.stopServer(lms.get()));
        }
        tryTo("stop the daemon", () -> machine.daemonDown(lms.get()));
    }

    private void delete(Path home) throws InstallException {
        for (int attempt = 1; ; attempt++) {
            try {
                FileTrees.delete(home);
                report.accept("Deleted " + home);
                return;
            } catch (IOException e) {
                if (attempt == DELETE_ATTEMPTS) {
                    throw new InstallException("Could not delete " + home + ": " + e.getMessage(), e);
                }
                pause();
            }
        }
    }

    private void removePointer(Path home) throws InstallException {
        try {
            LmStudioHome.removePointerTo(userHome, home);
        } catch (IOException e) {
            throw new InstallException("Could not remove LM Studio's home pointer: " + e.getMessage(), e);
        }
    }

    /**
     * WHY not a failure: a stale PATH entry names a folder that no longer exists, which Windows skips without a word.
     * Said in the log, so a support bundle shows it.
     */
    private void removePathEntry(Path bin) {
        try {
            userPath.removeEntry(bin);
        } catch (InstallException e) {
            report.accept(e.getMessage());
        }
    }

    @FunctionalInterface
    private interface Step {
        void run() throws InstallException;
    }

    private void tryTo(String what, Step step) {
        try {
            step.run();
        } catch (InstallException e) {
            report.accept("Could not " + what + ": " + e.getMessage());
        }
    }

    private void pause() throws InstallException {
        try {
            Thread.sleep(deleteRetryPause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InstallException("Interrupted", e);
        }
    }
}
