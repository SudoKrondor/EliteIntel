package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.LmStudioDefaults;
import elite.intel.db.managers.LmStudioInstallManager.Install;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.controller.ManagedService;
import elite.intel.ui.event.AppLogEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Runs the LM Studio the app installed, for as long as the app's services run (docs/LM_STUDIO_INSTALL_ASSISTANCE.md,
 * "Starting and stopping"): started with them, and stopped with them or when the app exits, so the model holds the
 * graphics card's memory only while Elite Intel is using it.
 * <p>
 * Only ever the app's own install: an LM Studio the commander set up themselves keeps running however they set it up.
 */
public final class LmStudioHost implements ManagedService {

    private static final Logger log = LogManager.getLogger(LmStudioHost.class);

    private static final String MODEL = LmStudioDefaults.MODEL;

    private final Path lms;
    private final LmStudioMachine machine;
    private final LmStudioExitStop exitStop;
    private volatile boolean running;

    LmStudioHost(Path lms, LmStudioMachine machine, LmStudioExitStop exitStop) {
        this.lms = lms;
        this.machine = machine;
        this.exitStop = exitStop;
    }

    /**
     * The host for the app's own finished install, or {@code null} when there is nothing for the app to run: no
     * install of its own, an unfinished one, or the commander has moved the brain to the cloud or another machine.
     * {@code null} is what the service registry takes as "do not start".
     */
    public static LmStudioHost forOwnedInstall() {
        Optional<Install> owned = LmStudioSetupOffer.ownedInstall();
        if (!shouldRun(owned)) return null;
        Optional<Path> lms = new LmStudioHome(owned.get().homeDir()).lmsCommand();
        if (lms.isEmpty()) {
            appLog("LM Studio: the install in " + owned.get().homeDir() + " has no lms command, so it was not started.");
            return null;
        }
        return new LmStudioHost(lms.get(), loggedMachine(), LmStudioExitStop.INSTANCE);
    }

    /**
     * A machine whose {@code lms} output goes to the LM Studio log only - for work no dialog is watching.
     */
    static LmStudioMachine loggedMachine() {
        return new LocalLmStudioMachine(line -> log.info("lms: {}", line));
    }

    /**
     * Whether the app should be running its own LM Studio now, from the saved settings. Asked when the brain restarts,
     * so an install that just finished starts and a switch to the cloud stops it.
     */
    public static boolean isWanted() {
        return shouldRun(LmStudioSetupOffer.ownedInstall());
    }

    private static boolean shouldRun(Optional<Install> owned) {
        SystemSession session = SystemSession.getInstance();
        return shouldRun(owned, session.useLocalCommandLlm(), session.getLmStudioAddress());
    }

    static boolean shouldRun(Optional<Install> owned, boolean localLlm, String address) {
        return owned.isPresent() && owned.get().setupComplete() && localLlm && LmStudioSetupOffer.isOnThisPc(address);
    }

    /**
     * Starts LM Studio's server if it is not running and loads the model if it is not loaded, returning once it is
     * ready, so the brain's connection check finds it answering.
     * <p>
     * WHY a failure is logged rather than thrown: it would stop every other service with it, and the app is whole
     * without LM Studio - the brain's own connection check then says LM Studio is not answering, as it would for any
     * local setup.
     */
    @Override
    public void start() {
        try {
            if (!machine.isServerAnswering()) {
                machine.daemonUp(lms);
                machine.startServer(lms);
            }
            loadWithVegasContext();
            running = true;
            exitStop.arm(lms, this::shutDown);
            appLog("LM Studio started with " + MODEL + ".");
        } catch (InstallException e) {
            appLog("LM Studio could not be started: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the model unless it is loaded already with exactly VEGA's context window. One loaded with any other window -
     * by hand, or by LM Studio's own default - is reloaded, since VEGA hallucinates on a smaller one and runs too slowly
     * on a larger one, and nothing else would say so.
     */
    private void loadWithVegasContext() throws InstallException {
        OptionalInt loaded = machine.loadedContextLength(MODEL);
        if (loaded.isPresent() && loaded.getAsInt() == LocalLmStudioMachine.CONTEXT_LENGTH) return;
        if (loaded.isPresent()) {
            appLog("LM Studio: " + MODEL + " was loaded with a " + loaded.getAsInt() + "-token context. Reloading it with "
                    + LocalLmStudioMachine.CONTEXT_LENGTH + ".");
            machine.unload(lms, MODEL);
        } else if (machine.isLoaded(MODEL)) {
            // WHY: loaded, but LM Studio did not say with what window, so it cannot be trusted to be VEGA's.
            machine.unload(lms, MODEL);
        }
        machine.load(lms, MODEL);
    }

    @Override
    public void stop() {
        if (!running) return;
        // WHY: when the app is already exiting, the exit hook is stopping LM Studio itself.
        if (!exitStop.disarm()) return;
        shutDown();
    }

    /**
     * Unloads the model, stops the server and the daemon. Each step is tried even when one before it failed, so a
     * server that will not stop still lets the daemon go.
     */
    private void shutDown() {
        running = false;
        shutDown(lms, machine);
    }

    /**
     * Unloads every model, stops the server and the daemon, each tried even when one before it failed.
     */
    static void shutDown(Path lms, LmStudioMachine machine) {
        tryTo("unload the model", () -> machine.unloadAll(lms));
        tryTo("stop the server", () -> machine.stopServer(lms));
        tryTo("stop the daemon", () -> machine.daemonDown(lms));
    }

    @FunctionalInterface
    private interface Step {
        void run() throws InstallException;
    }

    private static void tryTo(String what, Step step) {
        try {
            step.run();
        } catch (InstallException e) {
            appLog("LM Studio: could not " + what + ": " + e.getMessage(), e);
        }
    }

    /**
     * To the in-app system log, and to the LM Studio log, which outlives a restart.
     */
    private static void appLog(String line) {
        log.info(line);
        UiBus.publish(new AppLogEvent(line));
    }

    private static void appLog(String line, InstallException failure) {
        log.warn(line, failure);
        UiBus.publish(new AppLogEvent(line));
    }
}
