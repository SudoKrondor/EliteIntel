package elite.intel.bindforge.devices;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.devicefiles.ButtonMapGeneration;
import elite.intel.bindforge.devicefiles.ButtonMapGeneration.Controller;
import elite.intel.devices.DeviceService;
import elite.intel.devices.events.DeviceConnectedEvent;
import elite.intel.devices.model.Device;
import elite.intel.eventbus.DeviceBus;
import elite.intel.ui.controller.ManagedService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Gives every connected, named controller a generated {@code .buttonMap} when it has none - at Elite-Intel
 * startup, so the labels are there the first time the capture dialog opens.
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
    private ExecutorService worker;

    public ButtonMapStartup() {
        this(new ButtonMapGeneration()::generateFor, DeviceService.getInstance()::getConnectedDevices);
    }

    ButtonMapStartup(Consumer<Controller> generation, Supplier<List<Device>> alreadyConnected) {
        this.generation = generation;
        this.alreadyConnected = alreadyConnected;
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

    private synchronized void submit(Device device) {
        if (worker == null) return;
        worker.execute(() -> generate(device));
    }

    private void generate(Device device) {
        try {
            DeviceIdentities.of(device).ifPresent(identity -> generation.accept(
                    new Controller(identity.vid(), identity.pid(), device.buttonCount(), device.axisCount())));
        } catch (RuntimeException e) {
            log.error("Generating a .buttonMap for '{}' failed", device.name(), e);
        }
    }
}
