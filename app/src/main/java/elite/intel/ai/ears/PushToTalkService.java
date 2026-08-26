package elite.intel.ai.ears;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.devices.DeviceService;
import elite.intel.devices.events.DeviceButtonEvent;
import elite.intel.devices.events.DeviceDisconnectedEvent;
import elite.intel.devices.model.Device;
import elite.intel.eventbus.DeviceBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.controller.ManagedService;
import elite.intel.ui.event.PttButtonStateEvent;
import elite.intel.ui.event.PttModeChangedEvent;
import elite.intel.ui.event.PushToTalkSettingsChangedEvent;
import elite.intel.util.AudioPlayer;
import elite.intel.util.PlayBeepEvent;

/**
 * Turns the commander's mapped controller button into the microphone gate.
 * <p>
 * A press opens the gate for as long as the button is down: the current vocalisation is cut immediately, at
 * the press rather than when the resulting transcript arrives, so speaking over her is instant. While
 * push-to-talk is on the button is the only thing that opens the gate, and anything the microphone picks up
 * without it is discarded by the STT pipeline as room noise.
 * <p>
 * This lived in the Input settings panel, where the runtime behaviour of a controller button depended on a
 * Swing component existing: it worked only because the settings tab is built eagerly at startup, and nobody
 * would think to look for the microphone gate in a settings screen. It runs as a service now, started and
 * stopped with the rest of the pipeline.
 * <p>
 * The panel still owns the settings themselves. This reads them from {@link SystemSession} at the moment it
 * needs them rather than mirroring them, so there is no second copy of the mapping to keep in step, and a
 * change to the controller or button takes effect on the very next press.
 */
public final class PushToTalkService implements ManagedService {

    private static final PushToTalkService INSTANCE = new PushToTalkService();

    /**
     * No device is holding the gate open.
     */
    private static final int NO_DEVICE = -1;

    /**
     * The device whose button is currently holding the gate open, so a controller that disconnects mid-hold
     * can be told apart from any other controller disconnecting.
     */
    private volatile int holdingDeviceId = NO_DEVICE;
    private boolean running;

    private PushToTalkService() {
    }

    public static PushToTalkService getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        DeviceBus.register(this);
        UiBus.register(this);
        running = true;
        if (SystemSession.getInstance().isPushToTalkEnabled()) {
            arm();
        }
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        DeviceBus.unregister(this);
        UiBus.unregister(this);
        running = false;
        releaseGate();
    }

    /**
     * Re-applies the policy after the commander turns push-to-talk on or off, or remaps its button.
     */
    @Subscribe
    public void onSettingsChanged(PushToTalkSettingsChangedEvent event) {
        if (SystemSession.getInstance().isPushToTalkEnabled()) {
            arm();
        } else {
            disarm();
        }
    }

    @Subscribe
    public void onButtonState(DeviceButtonEvent event) {
        if (!isGateButton(event)) {
            return;
        }
        if (event.pressed()) {
            announcePress();
            holdingDeviceId = event.deviceId();
            UiBus.publish(new PttButtonStateEvent(true));
            return;
        }
        // Close the gate first. GameEventBus dispatches on this thread, and the capture loop records the
        // frame the release lands in on purpose (so the last word is not clipped) - so a beep published
        // ahead of the release can still be sounding inside that frame and land in the recording. That
        // matters more than it looks: Amplifier normalizes to the peak, so a beep in the buffer sets the
        // gain for the whole utterance and the commander's voice reaches the recogniser quiet.
        releaseGate();
        GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_1));
    }

    /**
     * A controller cannot report the release of a button it no longer has, so a disconnect while held would
     * leave the gate open for good.
     */
    @Subscribe
    public void onDeviceDisconnected(DeviceDisconnectedEvent event) {
        if (event.deviceId() == holdingDeviceId) {
            releaseGate();
        }
    }

    /**
     * Cuts the current vocalisation at the press itself, so the commander never talks over a reply.
     */
    private void announcePress() {
        GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));
        GameEventBus.publish(new TTSInterruptEvent(true));
    }

    private void releaseGate() {
        if (holdingDeviceId == NO_DEVICE) {
            return;
        }
        holdingDeviceId = NO_DEVICE;
        UiBus.publish(new PttButtonStateEvent(false));
    }

    /**
     * Push-to-talk on: nothing reaches the companion until the button is held.
     */
    private void arm() {
        UiBus.publish(new PttModeChangedEvent(true));
    }

    /**
     * Push-to-talk off: the microphone goes back to listening on its own.
     */
    private void disarm() {
        releaseGate();
        UiBus.publish(new PttModeChangedEvent(false));
    }

    /**
     * Whether this transition is the mapped button on the mapped controller. The controller is stored by name
     * (an SDL instance id is reassigned on every reconnect), so it is matched by name against the device the
     * event came from.
     */
    private boolean isGateButton(DeviceButtonEvent event) {
        SystemSession session = SystemSession.getInstance();
        if (!session.isPushToTalkEnabled() || event.buttonIndex() != session.getPushToTalkButtonIndex()) {
            return false;
        }
        String mapped = session.getPushToTalkControllerName();
        if (mapped == null || mapped.isBlank()) {
            return false;
        }
        for (Device device : DeviceService.getInstance().getConnectedDevices()) {
            if (device.id() == event.deviceId()) {
                return mapped.equals(device.name());
            }
        }
        return false;
    }
}
