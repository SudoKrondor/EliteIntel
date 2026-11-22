package elite.intel.bindforge.devices;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.bindforge.devicefiles.BindsDeviceReferences;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.bindforge.devices.MissingControllers.Missing;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Says once per run which of the user's named controllers are not attached while bindings name them - and then
 * waits and sees. Nothing is changed and nothing is repaired: an entry deliberately outlives its hardware, which is
 * what makes a stick work again when it is plugged back in.
 * <p>
 * <strong>Not at Elite-Intel startup - the first time the user opens Alias Designer</strong> (Alan, 2026-10-11, A13).
 * BindForge stays quiet until the user comes to it, and by then the Device Service has been listing controllers
 * since launch. "Once per run" is once per Elite-Intel process: this holds the one warning for the whole run.
 * <p>
 * <strong>Spoken, and logged</strong> (Alan, 2026-10-11): the spoken line names the controllers, the log line also
 * says how many bindings name each.
 */
public final class MissingControllerWarning {

    private static final Logger log = LogManager.getLogger(MissingControllerWarning.class);

    private static MissingControllerWarning instance;

    /** Reads how many bindings name each entry - strict, as {@link BindsDeviceReferences#referenceCounts} is. */
    @FunctionalInterface
    interface ReferenceReader {
        Map<String, Integer> read(Path bindingsFolder) throws IOException;
    }

    private final AtomicBoolean given = new AtomicBoolean();
    private final ReferenceReader references;
    private final Collection<String> frontierNames;
    private final Consumer<String> speak;
    private final Consumer<String> appLog;

    private MissingControllerWarning() {
        this(BindsDeviceReferences::referenceCounts, FrontierStockDevices.getInstance().names(),
                text -> GameEventBus.publish(
                        new VocalisationRequestEvent(text, MissionCriticalAnnouncementEvent.class, false)),
                text -> UiBus.publish(new AppLogEvent(text)));
    }

    MissingControllerWarning(ReferenceReader references, Collection<String> frontierNames,
                             Consumer<String> speak, Consumer<String> appLog) {
        this.references = references;
        this.frontierNames = List.copyOf(frontierNames);
        this.speak = speak;
        this.appLog = appLog;
    }

    public static synchronized MissingControllerWarning getInstance() {
        if (instance == null) instance = new MissingControllerWarning();
        return instance;
    }

    /** Whether this run's warning is still to come - so the screen knows a refresh is wanted for it. */
    public boolean pending() {
        return !given.get();
    }

    /**
     * Judges the controllers once, if this run has not yet. Safe from any thread; meant for the refresh thread, since
     * reading {@code .binds} touches the disk.
     *
     * @param hardwareListed whether the Device Service was running and had finished its first look at the hardware
     *                       when {@code devices} was built - {@code DeviceService.hasEnumerated()}
     * @param devices        the My Devices rows from a refresh made after the screen was first shown
     * @param bindingsFolder the folder holding the shared {@code .binds}
     */
    public void judgeOnce(boolean hardwareListed, List<MyDevice> devices, Path bindingsFolder) {
        // WHY: not used up. Until the hardware is listed every controller still being enumerated reads absent - the
        // service reports itself running a moment before that (review, 2026-10-11) - and the next showing of the
        // screen may find it done.
        if (!hardwareListed) {
            log.info("The Device Service has not listed the controllers, so missing ones cannot be judged yet");
            return;
        }
        if (!given.compareAndSet(false, true)) return;

        Map<String, Integer> counts;
        try {
            counts = references.read(bindingsFolder);
        } catch (IOException e) {
            // WHY: used up all the same - showing the screen again does not make the folder readable, and without
            // the counts a controller no binding uses would be announced as if it mattered.
            log.info("Could not read which controllers the bindings name, so no missing controller is announced: {}",
                    e.getMessage());
            return;
        }

        List<Missing> missing = MissingControllers.of(devices, counts, frontierNames);
        if (missing.isEmpty()) {
            log.debug("Every controller the bindings name is attached");
            return;
        }
        speak.accept(missing.size() == 1
                ? StringUtls.localizedSpeech("speech.missingController", missing.getFirst().name())
                : StringUtls.localizedSpeech("speech.missingControllers",
                        String.join(", ", missing.stream().map(Missing::name).toList())));
        for (Missing controller : missing) {
            appLog.accept("Controller " + controller.name() + " is not connected - " + controller.bindings()
                    + (controller.bindings() == 1 ? " binding names it" : " bindings name it"));
        }
    }
}
