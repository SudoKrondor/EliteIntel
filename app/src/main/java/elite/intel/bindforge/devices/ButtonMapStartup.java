package elite.intel.bindforge.devices;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.devicefiles.ButtonMapGeneration;
import elite.intel.bindforge.devicefiles.ButtonMapGeneration.Controller;
import elite.intel.bindforge.storage.StartupBackup;
import elite.intel.db.managers.BindForgeActivationManager;
import elite.intel.devices.DeviceService;
import elite.intel.devices.events.DeviceConnectedEvent;
import elite.intel.devices.model.Device;
import elite.intel.devices.model.DeviceDescription;
import elite.intel.eventbus.DeviceBus;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.controller.ManagedService;
import elite.intel.ui.event.BindForgeActivatedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Gives every connected, named controller a generated {@code .buttonMap} when it has none - at Elite-Intel
 * startup, so the labels are there the first time the capture dialog opens.
 * <p>
 * <strong>Only once BindForge is activated</strong> (Krondor, 2026-10-14). Before then it writes nothing into a game
 * folder: the "before BindForge" backup is taken at activation and must show the files as they were, and a user who
 * never opens BindForge has asked for nothing. When activation happens, the controllers connected then are handled
 * ({@link BindForgeActivatedEvent}).
 * <p>
 * <strong>Only once the startup backup is taken</strong> (storage.md rule 12; Alan, 2026-10-24). It waits for
 * {@link StartupBackup}, and writes nothing that run if the backup failed - nothing reaches a game file until a backup
 * of everything exists.
 * <p>
 * <strong>Every connect, not only the ones at startup</strong> (Alan, 2026-10-06). {@code DeviceService} says
 * nothing when its first enumeration is done, but it publishes a {@link DeviceConnectedEvent} for every
 * controller present then - so acting on each connect covers startup exactly, and a named controller plugged in
 * later meets the same rule. Naming a controller that has no entry is onboarding's, never this.
 * <p>
 * Started as {@code ServiceType.BUTTON_MAP_STARTUP} in {@code AppController.buildServices}, after {@code DEVICE};
 * diagnostics mode stubs it, since it writes into game folders.
 */
public final class ButtonMapStartup implements ManagedService {

    private static final Logger log = LogManager.getLogger(ButtonMapStartup.class);

    /** Plenty for one small file to notice the interrupt and clean up. */
    private static final long STOP_WAIT_SECONDS = 1;

    private final Consumer<Controller> generation;
    private final Supplier<List<Device>> alreadyConnected;
    private final IntFunction<Optional<DeviceDescription>> descriptions;
    private final BooleanSupplier activated;
    private ExecutorService worker;

    public ButtonMapStartup() {
        this(new ButtonMapGeneration()::generateFor, DeviceService.getInstance()::getConnectedDevices,
                DeviceService.getInstance()::description,
                () -> StartupBackup.getInstance().awaitTaken() && BindForgeActivationManager.getInstance().isActivated());
    }

    /**
     * @param activated whether it may write: the startup backup was taken and BindForge is activated - asked on the
     *                  worker thread, before each controller; it may block until the startup backup is done
     */
    ButtonMapStartup(Consumer<Controller> generation, Supplier<List<Device>> alreadyConnected,
                     IntFunction<Optional<DeviceDescription>> descriptions, BooleanSupplier activated) {
        this.generation = generation;
        this.alreadyConnected = alreadyConnected;
        this.descriptions = descriptions;
        this.activated = activated;
    }

    /**
     * Listens for connects, and also handles the controllers already connected: a connect published before this
     * registered is otherwise missed. A controller handled twice costs nothing, because generation only creates.
     */
    @Override
    public synchronized void start() {
        if (worker != null) return;
        // WHY: file reads and writes leave the device bus's single thread, which also carries push-to-talk.
        worker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "bindforge-buttonmap-startup");
            thread.setDaemon(true);
            return thread;
        });
        DeviceBus.register(this);
        UiBus.register(this);
        alreadyConnected.get().forEach(this::submit);
    }

    /**
     * Nothing runs once this returns: queued controllers are dropped, and one being written is interrupted, which
     * removes its temp file. Left to finish, a write could land after stop - or, on this daemon thread, be killed at
     * exit and leave a temp file in a game folder.
     */
    @Override
    public synchronized void stop() {
        if (worker == null) return;
        DeviceBus.unregister(this);
        UiBus.unregister(this);
        worker.shutdownNow();
        try {
            if (!worker.awaitTermination(STOP_WAIT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("The .buttonMap generation did not stop within {} s", STOP_WAIT_SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        worker = null;
    }

    @Subscribe
    public void onDeviceConnected(DeviceConnectedEvent event) {
        submit(event.device());
    }

    /**
     * Activation has just happened: the controllers connected now get what startup held back from them.
     */
    @Subscribe
    public void onActivated(BindForgeActivatedEvent event) {
        alreadyConnected.get().forEach(this::submit);
    }

    /**
     * The description is taken now, on the caller's thread: {@code DeviceService} holds it before it publishes the
     * connect, and drops it at disconnect. A controller already gone has none, and gets nothing - its next connect
     * is handled then.
     */
    private synchronized void submit(Device device) {
        if (worker == null) return;
        Optional<DeviceDescription> description = descriptions.apply(device.id());
        if (description.isEmpty()) {
            log.debug("'{}' disconnected before its .buttonMap was considered", device.name());
            return;
        }
        worker.execute(() -> generate(device, description.get()));
    }

    private void generate(Device device, DeviceDescription description) {
        try {
            // WHY: asked here, on the worker, not on the device bus's thread, which also carries push-to-talk -
            // it is a database read.
            if (!activated.getAsBoolean()) {
                log.debug("BindForge is not activated, or the startup backup was not taken, so no .buttonMap is "
                        + "generated for '{}'", device.name());
                return;
            }
            DeviceIdentities.of(device).ifPresent(identity -> generation.accept(new Controller(identity.vid(),
                    identity.pid(), device.buttonCount(), device.axisCount(), description)));
        } catch (RuntimeException e) {
            log.error("Generating a .buttonMap for '{}' failed", device.name(), e);
        }
    }
}
