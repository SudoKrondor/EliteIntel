package elite.intel.ui.screen.bindings;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devices.DeviceEditorInputs;
import elite.intel.bindforge.devices.DeviceIdentities;
import elite.intel.devices.DeviceService;
import elite.intel.devices.events.DeviceButtonEvent;
import elite.intel.devices.events.DeviceConnectedEvent;
import elite.intel.devices.events.DeviceDisconnectedEvent;
import elite.intel.devices.events.DeviceHatEvent;
import elite.intel.devices.model.ButtonInputMapper;
import elite.intel.devices.model.Device;
import elite.intel.devices.events.DeviceServiceStateEvent;
import elite.intel.eventbus.DeviceBus;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The controllers plugged in now, by VID/PID, for the reconcile's screens: which are there, what Windows calls them,
 * and which input was just pressed - so a player can tell two sticks apart by pressing one (multi-install-reconcile.md,
 * <em>Plug in</em>). Read from the Device Service and kept current from {@link DeviceBus} while it is open.
 * <p>
 * Every call to the listener is on the EDT. Opened on the EDT, and closed when the dialog using it closes - it holds
 * a bus registration until then.
 */
final class LiveControllers implements AutoCloseable {

    /** What changed, on the EDT. */
    interface Listener {
        /** A controller was plugged in or out. */
        void changed();

        /**
         * A button was pressed, or a hat pushed, on a controller.
         *
         * @param token the input in {@code .buttonMap} terms - {@code Joy_1}, {@code Joy_POV1Up}
         */
        void pressed(HardwareId hardware, String token);
    }

    /** What a screen with no live part listens with. */
    static final Listener NOBODY = new Listener() {
        @Override
        public void changed() {
        }

        @Override
        public void pressed(HardwareId hardware, String token) {
        }
    };

    private final Map<Integer, HardwareId> hardwareById = new HashMap<>();
    private final Map<Integer, String> nameById = new HashMap<>();
    private Listener listener = NOBODY;
    private boolean available;
    private boolean open;

    /** Reads what is plugged in now and starts listening. On the EDT. */
    static LiveControllers open() {
        LiveControllers live = new LiveControllers();
        DeviceService service = DeviceService.getInstance();
        live.available = service.isAvailable();
        if (live.available) service.getConnectedDevices().forEach(live::add);
        DeviceBus.register(live);
        live.open = true;
        return live;
    }

    /** Where the changes go from now on - one screen at a time. On the EDT. */
    void listen(Listener listener) {
        this.listener = listener == null ? NOBODY : listener;
    }

    /**
     * Whether the Device Service is running, so that "not plugged in" can be believed. When it is not, every
     * controller reads as not plugged in, and the screens say they cannot tell.
     */
    boolean available() {
        return available;
    }

    /** The controllers plugged in now. */
    Set<HardwareId> connected() {
        return Set.copyOf(hardwareById.values());
    }

    boolean isConnected(HardwareId hardware) {
        return hardwareById.containsValue(hardware);
    }

    /** What Windows calls the controller, when it is plugged in. */
    Optional<String> productName(HardwareId hardware) {
        return hardwareById.entrySet().stream().filter(entry -> entry.getValue().equals(hardware))
                .map(entry -> nameById.get(entry.getKey())).filter(name -> name != null && !name.isBlank())
                .findFirst();
    }

    @Override
    public void close() {
        if (!open) return;
        open = false;
        DeviceBus.unregister(this);
    }

    @Subscribe
    public void onConnected(DeviceConnectedEvent event) {
        SwingUtilities.invokeLater(() -> {
            available = true;
            add(event.device());
            listener.changed();
        });
    }

    @Subscribe
    public void onDisconnected(DeviceDisconnectedEvent event) {
        SwingUtilities.invokeLater(() -> {
            hardwareById.remove(event.deviceId());
            nameById.remove(event.deviceId());
            listener.changed();
        });
    }

    @Subscribe
    public void onServiceState(DeviceServiceStateEvent event) {
        SwingUtilities.invokeLater(() -> {
            available = event.available();
            if (!available) {
                hardwareById.clear();
                nameById.clear();
            }
            listener.changed();
        });
    }

    @Subscribe
    public void onButton(DeviceButtonEvent event) {
        if (!event.pressed()) return;
        int id = event.deviceId();
        String token = ButtonInputMapper.toBindsToken(event.buttonIndex());
        SwingUtilities.invokeLater(() -> pressed(id, token));
    }

    @Subscribe
    public void onHat(DeviceHatEvent event) {
        int[] bits = {DeviceHatEvent.UP, DeviceHatEvent.RIGHT, DeviceHatEvent.DOWN, DeviceHatEvent.LEFT};
        List<String> tokens = DeviceEditorInputs.hatTokens(event.hatIndex() + 1);
        for (int i = 0; i < bits.length; i++) {
            if ((event.directions() & bits[i]) == 0) continue;
            int id = event.deviceId();
            String token = tokens.get(i);
            SwingUtilities.invokeLater(() -> pressed(id, token));
            return;
        }
    }

    private void pressed(int deviceId, String token) {
        HardwareId hardware = hardwareById.get(deviceId);
        if (hardware != null) listener.pressed(hardware, token);
    }

    private void add(Device device) {
        DeviceIdentities.of(device).ifPresent(identity -> {
            hardwareById.put(device.id(), new HardwareId(identity.vid(), identity.pid()));
            nameById.put(device.id(), device.name());
        });
    }

    @Override
    public String toString() {
        return hardwareById.values().stream().map(HardwareId::toString).collect(Collectors.joining(", "));
    }
}
