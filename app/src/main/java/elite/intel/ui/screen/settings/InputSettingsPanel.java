package elite.intel.ui.screen.settings;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.devices.DeviceService;
import elite.intel.devices.events.DeviceButtonEvent;
import elite.intel.devices.events.DeviceConnectedEvent;
import elite.intel.devices.events.DeviceDisconnectedEvent;
import elite.intel.devices.model.Device;
import elite.intel.eventbus.DeviceBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.*;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudSegmentedControl;
import elite.intel.ui.widget.HudTwoColumns;
import elite.intel.util.AudioPlayer;
import elite.intel.util.PlayBeepEvent;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeCheckBox;
import static elite.intel.ui.theme.AppTheme.transparentPanel;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND;

/**
 * "Input" settings tab - lets the user map a controller button to push-to-talk, monitored via
 * the shared SDL3 poll loop in {@link DeviceService}.
 */
public class InputSettingsPanel extends JPanel {

    private JCheckBox enablePushToTalkCheck;
    private HudComboBox<Object> controllerCombo;
    private HudComboBox<String> buttonCombo;
    private HudSegmentedControl modeControl;

    // Mode segment indices - order matches the segments built in buildUi().
    private static final int MODE_TOGGLE = 0;
    private static final int MODE_HOLD = 1;

    // Mirrors of the Swing selection state above, read from the SDL poll thread.
    private volatile boolean pushToTalkEnabled = false;
    private volatile Device selectedDevice = null;
    private volatile int selectedButtonIndex = -1; // 0-based SDL button index, -1 = none
    private volatile boolean toggleMode = true;

    // Name of the controller persisted in game_session, used to re-select it once it appears
    // in the connected-devices list (initial load, or reconnect after a disconnect).
    private volatile String persistedControllerName = null;

    // Suppresses SystemSession writes while combo selections are being driven programmatically
    // (initial load, or reacting to DeviceConnected/DeviceDisconnectedEvent) rather than by the user.
    private boolean suppressPersistence = false;

    public InputSettingsPanel() {
        UiBus.register(this);
        DeviceBus.register(this);
        buildUi();
    }

    public void dispose() {
        UiBus.unregister(this);
        DeviceBus.unregister(this);
    }

    public void initData() {
        SystemSession session = SystemSession.getInstance();
        pushToTalkEnabled = session.isPushToTalkEnabled();
        toggleMode = session.isPushToTalkToggleMode();
        persistedControllerName = session.getPushToTalkControllerName();

        enablePushToTalkCheck.setSelected(pushToTalkEnabled);
        setControlsEnabled(pushToTalkEnabled);
        modeControl.setSelectedIndex(toggleMode ? MODE_TOGGLE : MODE_HOLD);

        if (pushToTalkEnabled) {
            SystemSession.getInstance().stopStartListening(true);
            UiBus.publish(new SleepWakeStateChangedEvent(true));
            if (!toggleMode) {
                UiBus.publish(new PttModeChangedEvent(true));
            }
        }

        reconcileControllerSelection();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        // Single flat working section (section 9). Two-column body (section 10): left column holds the master enable
        // slab and the mode switch (both stretched to the column width, no labels); right column holds
        // the controller/button pickers as aligned rows.
        HudSection section = HudSection.flat(getText("settings.input.section.binding"), new BorderLayout());
        JPanel body = section.body();

        enablePushToTalkCheck = makeCheckBox(getText("settings.input.enablePushToTalk"), false);
        enablePushToTalkCheck.addActionListener(e -> onPushToTalkToggled());

        modeControl = new HudSegmentedControl(
                new String[]{getText("settings.input.mode.toggle"), getText("settings.input.mode.hold")},
                MODE_TOGGLE);
        modeControl.addChangeListener(e -> onModeChanged());

        // Left column - enable slab + mode switch, both full-width (span) and label-less. GridBag + baseGbc
        // so its row insets come from the same shared source as the right column (no hand-tuned border).
        JPanel leftCol = transparentPanel(new GridBagLayout());
        GridBagConstraints lgc = baseGbc();
        addSpanComponent(leftCol, enablePushToTalkCheck, lgc);
        nextRow(lgc);
        addSpanComponent(leftCol, modeControl, lgc);
        JPanel leftWrap = transparentPanel(new BorderLayout());
        leftWrap.add(leftCol, BorderLayout.NORTH);

        // Right column - controller / button as aligned label->control rows.
        JPanel rightCol = transparentPanel(new GridBagLayout());
        GridBagConstraints gc = baseGbc();

        addLabel(rightCol, getText("settings.input.controller"), gc, 0);
        controllerCombo = new HudComboBox<>(new Object[0]);
        controllerCombo.addItem(getText("settings.input.controller.placeholder"));
        controllerCombo.addActionListener(e -> onControllerSelected());
        addField(rightCol, controllerCombo, gc, 1, 1.0);

        nextRow(gc);
        addLabel(rightCol, getText("settings.input.button"), gc, 0);
        buttonCombo = new HudComboBox<>(new String[0]);
        buttonCombo.addItem(getText("settings.input.button.placeholder"));
        buttonCombo.addActionListener(e -> onButtonSelected());
        addField(rightCol, buttonCombo, gc, 1, 1.0);

        JPanel rightWrap = transparentPanel(new BorderLayout());
        rightWrap.add(rightCol, BorderLayout.NORTH);

        body.add(new HudTwoColumns(leftWrap, rightWrap), BorderLayout.CENTER);

        JPanel content = transparentPanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(section);

        add(content, BorderLayout.NORTH);

        setControlsEnabled(false);
    }

    /** Applies the selected PTT mode: toggle (sleep/wake) vs hold-to-talk, and syncs session + events. */
    private void onModeChanged() {
        if (modeControl.getSelectedIndex() == MODE_TOGGLE) {
            toggleMode = true;
            SystemSession.getInstance().setPushToTalkToggleMode(true);
            UiBus.publish(new PttModeChangedEvent(false));
        } else {
            toggleMode = false;
            SystemSession.getInstance().setPushToTalkToggleMode(false);
            // Lock the system to sleeping - PTT button is the only wake trigger in this mode.
            SystemSession.getInstance().stopStartListening(true);
            UiBus.publish(new SleepWakeStateChangedEvent(true));
            UiBus.publish(new PttModeChangedEvent(true));
        }
    }

    // -- UI handlers -----------------------------------------------------------

    private void onPushToTalkToggled() {
        boolean enabled = enablePushToTalkCheck.isSelected();
        pushToTalkEnabled = enabled;
        setControlsEnabled(enabled);
        if (enabled) {
            reconcileControllerSelection();

            SystemSession.getInstance().stopStartListening(true);
            UiBus.publish(new SleepWakeStateChangedEvent(true));
            if (!toggleMode) UiBus.publish(new PttModeChangedEvent(true));
        } else {

            SystemSession.getInstance().stopStartListening(false);
            UiBus.publish(new SleepWakeStateChangedEvent(false));
            UiBus.publish(new PttModeChangedEvent(false));
        }
        SystemSession.getInstance().setPushToTalkEnabled(enabled);
        // PTT on/off only affects the STT pipeline; it previously took effect only after an app
        // restart. Restart just the EARS service so the change applies now (no full rebuild needed).
        UiBus.publish(new RestartEarsEvent());
    }

    private void setControlsEnabled(boolean enabled) {
        controllerCombo.setEnabled(enabled);
        buttonCombo.setEnabled(enabled);
        modeControl.setEnabled(enabled);
    }

    private void onControllerSelected() {
        Object selected = controllerCombo.getSelectedItem();
        Device device = (selected instanceof Device d) ? d : null;
        selectedDevice = device;
        populateButtonCombo(device);
        if (!suppressPersistence) {
            persistedControllerName = device != null ? device.name() : null;
            SystemSession.getInstance().setPushToTalkControllerName(persistedControllerName);
        }
    }

    private void onButtonSelected() {
        selectedButtonIndex = buttonCombo.getSelectedIndex() - 1; // -1 = placeholder
        if (!suppressPersistence) {
            SystemSession.getInstance().setPushToTalkButtonIndex(selectedButtonIndex);
        }
    }

    // -- Combo population --------------------------------------------------------

    private void refreshControllerCombo() {
        Device previouslySelected = selectedDevice;

        controllerCombo.removeAllItems();
        controllerCombo.addItem(getText("settings.input.controller.placeholder"));
        for (Device device : DeviceService.getInstance().getConnectedDevices()) {
            controllerCombo.addItem(device);
        }

        if (previouslySelected != null) {
            for (int i = 1; i < controllerCombo.getItemCount(); i++) {
                if (controllerCombo.getItemAt(i) instanceof Device d && d.id() == previouslySelected.id()) {
                    controllerCombo.setSelectedIndex(i);
                    return;
                }
            }
        } else if (persistedControllerName != null) {
            for (int i = 1; i < controllerCombo.getItemCount(); i++) {
                if (controllerCombo.getItemAt(i) instanceof Device d && d.name().equals(persistedControllerName)) {
                    controllerCombo.setSelectedIndex(i);
                    return;
                }
            }
        }
        controllerCombo.setSelectedIndex(0);
    }

    /**
     * Refreshes the controller combo and, if a device matching {@link #persistedControllerName}
     * becomes selected, restores the persisted button index. Runs with persistence suppressed so
     * that the intermediate "no button selected" state hit while rebuilding the button combo does
     * not overwrite the saved {@code pushToTalkButtonIndex}/{@code pushToTalkControllerName}.
     */
    private void reconcileControllerSelection() {
        int targetButtonIndex = (selectedDevice != null)
                ? selectedButtonIndex
                : SystemSession.getInstance().getPushToTalkButtonIndex();

        suppressPersistence = true;
        try {
            refreshControllerCombo();
            if (selectedDevice != null && targetButtonIndex >= 0
                    && targetButtonIndex < buttonCombo.getItemCount() - 1) {
                buttonCombo.setSelectedIndex(targetButtonIndex + 1);
            }
        } finally {
            suppressPersistence = false;
        }
    }

    private void populateButtonCombo(Device device) {
        buttonCombo.removeAllItems();
        buttonCombo.addItem(getText("settings.input.button.placeholder"));
        if (device != null) {
            for (int i = 1; i <= device.buttonCount(); i++) {
                buttonCombo.addItem(getText("settings.input.button.label", i));
            }
        }
        buttonCombo.setSelectedIndex(0);
    }

    // -- PTT mode sync -----------------------------------------------------------

    @Subscribe
    public void onPttModeChanged(PttModeChangedEvent event) {
        SwingUtilities.invokeLater(() -> {
            if (!pushToTalkEnabled) return;
            boolean newToggleMode = SystemSession.getInstance().isPushToTalkToggleMode();
            if (newToggleMode == toggleMode) return;
            toggleMode = newToggleMode;
            modeControl.setSelectedIndex(toggleMode ? MODE_TOGGLE : MODE_HOLD);
        });
    }

    // -- SDL event subscriptions -------------------------------------------------

    @Subscribe
    public void onDeviceConnected(DeviceConnectedEvent event) {
        SwingUtilities.invokeLater(this::reconcileControllerSelection);
    }

    @Subscribe
    public void onDeviceDisconnected(DeviceDisconnectedEvent event) {
        if (selectedDevice != null && selectedDevice.id() == event.deviceId() && !toggleMode) {
            // Release PTT if the controller disconnects while the button is held.
            UiBus.publish(new PttButtonStateEvent(false));
        }
        SwingUtilities.invokeLater(() -> {
            suppressPersistence = true;
            try {
                if (selectedDevice != null && selectedDevice.id() == event.deviceId()) {
                    selectedDevice = null;
                    selectedButtonIndex = -1;
                    populateButtonCombo(null);
                }
                refreshControllerCombo();
            } finally {
                suppressPersistence = false;
            }
        });
    }

    @Subscribe
    public void onButtonState(DeviceButtonEvent event) {
        if (!pushToTalkEnabled) return;

        Device device = selectedDevice;
        int buttonIndex = selectedButtonIndex;
        if (device == null || buttonIndex < 0) return;
        if (event.deviceId() != device.id() || event.buttonIndex() != buttonIndex) return;

        if (toggleMode) {
            if (event.pressed()) {
                GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));
                GameEventBus.publish(new TTSInterruptEvent(true));
                toggleSleepWake();
            }
        } else {
            if (event.pressed()) {
                GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));
                GameEventBus.publish(new TTSInterruptEvent(true));
                UiBus.publish(new PttButtonStateEvent(true));
            } else {
                GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_1));
                UiBus.publish(new PttButtonStateEvent(false));
            }
        }
    }

    // -- Sleep / Wake actions (toggle mode only) ----------------------------------

    private void toggleSleepWake() {
        if (SystemSession.getInstance().isSleepingModeOn()) wakeUp(); else sleep();
    }

    private void wakeUp() {
        SystemSession.getInstance().stopStartListening(false);
        UiBus.publish(new VoiceInputModeToggleEvent(false));
    }

    private void sleep() {
        SystemSession.getInstance().stopStartListening(true);
        UiBus.publish(new VoiceInputModeToggleEvent(true));
    }
}
