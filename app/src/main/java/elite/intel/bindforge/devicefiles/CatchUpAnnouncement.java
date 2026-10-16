package elite.intel.bindforge.devicefiles;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.bindforge.install.GameInstallationProvider;
import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.db.managers.BindForgeActivationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.session.PlayerSession;
import elite.intel.ui.controller.ManagedService;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * At Elite-Intel startup, says once that an installation which missed a write has been found again (Alan, 2026-10-15,
 * A14). <strong>Says, never asks:</strong> at startup the user is often already in the game, with Elite-Intel's window
 * behind it, so a dialog would block the window unseen. The question is asked on the Alias Designer screen the first
 * time it is shown in the run - spoken here, asked where its context is, as the missing-controller warning is.
 * <p>
 * Started as {@code ServiceType.CATCH_UP_ANNOUNCEMENT} in {@code AppController.buildServices}; diagnostics mode stubs
 * it. Once per Elite-Intel process: services can be restarted, the announcement is not repeated.
 */
public final class CatchUpAnnouncement implements ManagedService {

    private static final Logger log = LogManager.getLogger(CatchUpAnnouncement.class);

    /** Once per process, across a restart of the services. */
    private static final AtomicBoolean ANNOUNCED = new AtomicBoolean();

    private final BooleanSupplier activated;
    private final Runnable detectOnce;
    private final Supplier<List<Target>> targets;
    private final Consumer<String> speak;
    private final Consumer<String> appLog;
    private final AtomicBoolean announced;

    public CatchUpAnnouncement() {
        this(BindForgeActivationManager.getInstance()::isActivated,
                () -> new InstallationRegistry(GameInstallationProvider.forThisPlatform(
                        PlayerSession.getInstance().getBindingsDir())).currentWithStartupScan(),
                DeviceFilesPush::storedInstallations,
                text -> GameEventBus.publish(
                        new VocalisationRequestEvent(text, MissionCriticalAnnouncementEvent.class, false)),
                text -> UiBus.publish(new AppLogEvent(text)),
                ANNOUNCED);
    }

    /**
     * @param detectOnce the run's one detection of the installations - shared with the screen's, so the two never run
     *                   it twice - which also marks a folder that came back as present
     */
    CatchUpAnnouncement(BooleanSupplier activated, Runnable detectOnce, Supplier<List<Target>> targets,
                        Consumer<String> speak, Consumer<String> appLog, AtomicBoolean announced) {
        this.activated = activated;
        this.detectOnce = detectOnce;
        this.targets = targets;
        this.speak = speak;
        this.appLog = appLog;
        this.announced = announced;
    }

    /** Looks off the starting thread: detection and the folder checks touch the disk, and a drive may be asleep. */
    @Override
    public void start() {
        if (announced.get()) return;
        Thread thread = new Thread(this::announceOnce, "bindforge-catch-up-announcement");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void stop() {
        // WHY: nothing to stop - one short look, and a line spoken after stop is harmless.
    }

    /** Says it, once, when there is something to say. Package-private for the tests. */
    void announceOnce() {
        try {
            // WHY: before activation nothing was ever written, so nothing can have been missed.
            if (!activated.getAsBoolean()) return;
            detectOnce.run();
            List<Target> found = MissedWrites.foundAgain(targets.get());
            if (found.isEmpty() || !announced.compareAndSet(false, true)) return;
            String names = String.join(", ", found.stream().map(Target::storefront).toList());
            speak.accept(found.size() == 1
                    ? StringUtls.localizedSpeech("speech.catchUpFound", names)
                    : StringUtls.localizedSpeech("speech.catchUpFoundMany", names));
            for (Target target : found) {
                appLog.accept("The " + target.storefront() + " installation (" + target.controlSchemes()
                        + ") missed a BindForge write and is back - open Alias Designer to update it");
            }
        } catch (RuntimeException e) {
            // WHY: broad on purpose - a thread boundary. Startup goes on; the screen still asks when it is shown.
            log.warn("Could not look for installations that missed a write", e);
        }
    }
}
