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
import elite.intel.ui.event.*;
import elite.intel.util.AudioPlayer;
import elite.intel.util.PlayBeepEvent;

/**
 * Turns the commander's mapped controller button into the microphone gate.
 * <p>
 * Two modes, both driven by the same button. In <b>hold</b> mode a press opens the gate for as long as the
 * button is down: the current vocalisation is cut immediately, at the press rather than when the resulting
 * transcript arrives, so speaking over her is instant. In <b>toggle</b> mode a press flips the system between
 * sleeping and awake. Either way the button is the only thing that opens the gate while push-to-talk is on,
 * which is why arming it puts the system to sleep first.
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
        // A disabled push-to-talk is left alone: there is no policy to enforce, and forcing the microphone
        // awake here would undo a commander who put her to sleep by voice before starting the services.
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
     * Re-applies the policy after the commander changes the push-to-talk setting or its mode. Re-arming
     * returns to the asleep baseline on purpose: whichever mode was just chosen, the button is what opens the
     * gate, so leaving the microphone open would make the setting a lie until the next press.
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
        if (SystemSession.getInstance().isPushToTalkToggleMode()) {
            if (event.pressed()) {
                announcePress();
                toggleSleepWake();
            }
            return;
        }
        if (event.pressed()) {
            announcePress();
            holdingDeviceId = event.deviceId();
            UiBus.publish(new PttButtonStateEvent(true));
            return;
        }
        GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_1));
        releaseGate();
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
     * Toggle mode flips sleeping and awake. {@link VoiceInputModeToggleEvent} rather than a plain state change
     * on purpose: it is the commander's own instruction, so it earns the spoken confirmation that
     * {@code AppController} attaches to that event, exactly as the "go to sleep" voice command does.
     */
    private void toggleSleepWake() {
        SystemSession session = SystemSession.getInstance();
        boolean wakingUp = session.isSleepingModeOn();
        session.stopStartListening(!wakingUp);
        UiBus.publish(new VoiceInputModeToggleEvent(!wakingUp));
    }

    /**
     * Push-to-talk on: the system sleeps until the button says otherwise.
     */
    private void arm() {
        SystemSession session = SystemSession.getInstance();
        session.stopStartListening(true);
        UiBus.publish(new SleepWakeStateChangedEvent(true));
        UiBus.publish(new PttModeChangedEvent(!session.isPushToTalkToggleMode()));
    }

    /**
     * Push-to-talk off: the microphone goes back to listening on its own.
     */
    private void disarm() {
        releaseGate();
        SystemSession.getInstance().stopStartListening(false);
        UiBus.publish(new SleepWakeStateChangedEvent(false));
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
