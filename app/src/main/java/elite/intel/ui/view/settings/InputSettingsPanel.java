package elite.intel.ui.view.settings;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.devices.DeviceService;
import elite.intel.devices.events.DeviceButtonEvent;
import elite.intel.devices.events.DeviceConnectedEvent;
import elite.intel.devices.events.DeviceDisconnectedEvent;
import elite.intel.devices.model.Device;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.PttButtonStateEvent;
import elite.intel.ui.event.PttModeChangedEvent;
import elite.intel.ui.event.SleepWakeStateChangedEvent;
import elite.intel.ui.event.VoiceInputModeToggleEvent;
import elite.intel.ui.view.HudSection;
import elite.intel.util.StringUtls;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.view.AppTheme.*;

/**
 * "Input" settings tab — lets the user map a controller button to push-to-talk, monitored via
 * the shared SDL3 poll loop in {@link DeviceService}.
 */
public class InputSettingsPanel extends JPanel {

    private JCheckBox enablePushToTalkCheck;
    private JComboBox<Object> controllerCombo;
    private JComboBox<String> buttonCombo;
    private JRadioButton toggleModeRadio;
    private JRadioButton holdModeRadio;

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
        EventBusManager.register(this);
        buildUi();
    }

    public void dispose() {
        EventBusManager.unregister(this);
    }

    public void initData() {
        SystemSession session = SystemSession.getInstance();
        pushToTalkEnabled = session.isPushToTalkEnabled();
        toggleMode = session.isPushToTalkToggleMode();
        persistedControllerName = session.getPushToTalkControllerName();

        enablePushToTalkCheck.setSelected(pushToTalkEnabled);
        setControlsEnabled(pushToTalkEnabled);
        toggleModeRadio.setSelected(toggleMode);
        holdModeRadio.setSelected(!toggleMode);

        if (pushToTalkEnabled) {
            DeviceService.getInstance().start();
            if (!toggleMode) {
                EventBusManager.publish(new PttModeChangedEvent(true));
            }
        }

        reconcileControllerSelection();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_BG);

        // Section 1: controller binding
        HudSection bindingSection = new HudSection(getText("settings.input.section.binding"), new GridBagLayout());
        JPanel fields = bindingSection.body();
        GridBagConstraints gc = baseGbc();

        // Row 0: Enable Push to Talk (full width)
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        enablePushToTalkCheck = makeCheckBox(getText("settings.input.enablePushToTalk"), false);
        enablePushToTalkCheck.addActionListener(e -> onPushToTalkToggled());
        fields.add(enablePushToTalkCheck, gc);
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;

        // Row 1: Controller combo
        nextRow(gc);
        addLabel(fields, getText("settings.input.controller"), gc);
        controllerCombo = new JComboBox<>();
        controllerCombo.addItem(getText("settings.input.controller.placeholder"));
        controllerCombo.addActionListener(e -> onControllerSelected());
        addField(fields, controllerCombo, gc, 1, 1.0);

        // Row 2: Button combo
        nextRow(gc);
        addLabel(fields, getText("settings.input.button"), gc);
        buttonCombo = new JComboBox<>();
        buttonCombo.addItem(getText("settings.input.button.placeholder"));
        buttonCombo.addActionListener(e -> onButtonSelected());
        addField(fields, buttonCombo, gc, 1, 1.0);

        // Section 2: mode selection
        HudSection modeSection = new HudSection(getText("settings.input.section.mode"), new FlowLayout(FlowLayout.LEFT, HUD_GAP, 0));
        JPanel modePanel = modeSection.body();

        toggleModeRadio = new JRadioButton(getText("settings.input.mode.toggle"), true);
        holdModeRadio = new JRadioButton(getText("settings.input.mode.hold"), false);
        styleCheckBox(toggleModeRadio);
        styleCheckBox(holdModeRadio);
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(toggleModeRadio);
        modeGroup.add(holdModeRadio);
        toggleModeRadio.addActionListener(e -> {
            toggleMode = true;
            SystemSession.getInstance().setPushToTalkToggleMode(true);
            EventBusManager.publish(new PttModeChangedEvent(false));
        });
        holdModeRadio.addActionListener(e -> {
            toggleMode = false;
            SystemSession.getInstance().setPushToTalkToggleMode(false);
            // Lock the system to sleeping — PTT button is the only wake trigger in this mode.
            SystemSession.getInstance().stopStartListening(true);
            EventBusManager.publish(new SleepWakeStateChangedEvent(true));
            EventBusManager.publish(new PttModeChangedEvent(true));
        });
        modePanel.add(toggleModeRadio);
        modePanel.add(holdModeRadio);

        JPanel content = transparentPanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(bindingSection);
        content.add(Box.createVerticalStrut(12));
        content.add(modeSection);

        add(content, BorderLayout.NORTH);

        setControlsEnabled(false);
    }

    // -- UI handlers -----------------------------------------------------------

    private void onPushToTalkToggled() {
        boolean enabled = enablePushToTalkCheck.isSelected();
        pushToTalkEnabled = enabled;
        setControlsEnabled(enabled);
        if (enabled) {
            DeviceService.getInstance().start();
            reconcileControllerSelection();
            if (!toggleMode) EventBusManager.publish(new PttModeChangedEvent(true));
        } else {
            EventBusManager.publish(new PttModeChangedEvent(false));
        }
        SystemSession.getInstance().setPushToTalkEnabled(enabled);
    }

    private void setControlsEnabled(boolean enabled) {
        controllerCombo.setEnabled(enabled);
        buttonCombo.setEnabled(enabled);
        toggleModeRadio.setEnabled(enabled);
        holdModeRadio.setEnabled(enabled);
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

    // -- SDL event subscriptions -------------------------------------------------

    @Subscribe
    public void onDeviceConnected(DeviceConnectedEvent event) {
        SwingUtilities.invokeLater(this::reconcileControllerSelection);
    }

    @Subscribe
    public void onDeviceDisconnected(DeviceDisconnectedEvent event) {
        if (selectedDevice != null && selectedDevice.id() == event.deviceId() && !toggleMode) {
            // Release PTT if the controller disconnects while the button is held.
            EventBusManager.publish(new PttButtonStateEvent(false));
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
            if (event.pressed()) toggleSleepWake();
        } else {
            if (event.pressed()) {
                EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedSpeech("speech.ignoreModeOff")));
                EventBusManager.publish(new PttButtonStateEvent(true));
            } else {
                EventBusManager.publish(new PttButtonStateEvent(false));
            }
        }
    }

    // -- Sleep / Wake actions (toggle mode only) ----------------------------------

    private void toggleSleepWake() {
        if (SystemSession.getInstance().isSleepingModeOn()) wakeUp(); else sleep();
    }

    private void wakeUp() {
        SystemSession.getInstance().stopStartListening(false);
        EventBusManager.publish(new VoiceInputModeToggleEvent(false));
    }

    private void sleep() {
        SystemSession.getInstance().stopStartListening(true);
        EventBusManager.publish(new VoiceInputModeToggleEvent(true));
    }
}
