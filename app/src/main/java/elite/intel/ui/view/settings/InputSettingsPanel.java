package elite.intel.ui.view.settings;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.SystemSession;
import elite.intel.starvizion.event.SvButtonStateEvent;
import elite.intel.starvizion.event.SvDeviceConnectedEvent;
import elite.intel.starvizion.event.SvDeviceDisconnectedEvent;
import elite.intel.starvizion.input.SdlInputService;
import elite.intel.starvizion.model.SvDevice;
import elite.intel.ui.event.VoiceInputModeToggleEvent;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.view.AppTheme.*;

/**
 * "Input" settings tab — lets the user map a controller button to push-to-talk, monitored via
 * the existing SDL3 poll loop in {@link SdlInputService}. Session-only, no DB persistence.
 */
public class InputSettingsPanel extends JPanel {

    private JCheckBox enablePushToTalkCheck;
    private JComboBox<Object> controllerCombo;
    private JComboBox<String> buttonCombo;
    private JRadioButton toggleModeRadio;
    private JRadioButton holdModeRadio;

    // Mirrors of the Swing selection state above, read from the SDL poll thread.
    private volatile boolean pushToTalkEnabled = false;
    private volatile SvDevice selectedDevice = null;
    private volatile int selectedButtonIndex = -1; // 0-based SDL button index, -1 = none
    private volatile boolean toggleMode = true;

    public InputSettingsPanel() {
        EventBusManager.register(this);
        buildUi();
    }

    public void dispose() {
        EventBusManager.unregister(this);
    }

    public void initData() {
        refreshControllerCombo();
    }

    private void buildUi() {
        setLayout(new BorderLayout());

        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints gc = baseGbc();

        // Row 0: Enable Push to Talk
        gc.gridx = 0;
        gc.gridwidth = 2;
        enablePushToTalkCheck = new JCheckBox(getText("settings.input.enablePushToTalk"), false);
        enablePushToTalkCheck.addActionListener(e -> onPushToTalkToggled());
        fields.add(enablePushToTalkCheck, gc);
        gc.gridwidth = 1;

        // Row 1: Controller
        nextRow(gc);
        addLabel(fields, getText("settings.input.controller"), gc);
        controllerCombo = new JComboBox<>();
        controllerCombo.addItem(getText("settings.input.controller.placeholder"));
        controllerCombo.addActionListener(e -> onControllerSelected());
        addField(fields, controllerCombo, gc, 1, 1.0);

        // Row 2: Button
        nextRow(gc);
        addLabel(fields, getText("settings.input.button"), gc);
        buttonCombo = new JComboBox<>();
        buttonCombo.addItem(getText("settings.input.button.placeholder"));
        buttonCombo.addActionListener(e -> onButtonSelected());
        addField(fields, buttonCombo, gc, 1, 1.0);

        // Row 3 & 4: Toggle / Hold radio buttons
        toggleModeRadio = new JRadioButton(getText("settings.input.mode.toggle"), true);
        holdModeRadio = new JRadioButton(getText("settings.input.mode.hold"), false);
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(toggleModeRadio);
        modeGroup.add(holdModeRadio);
        toggleModeRadio.addActionListener(e -> toggleMode = true);
        holdModeRadio.addActionListener(e -> toggleMode = false);

        nextRow(gc);
        gc.gridx = 0;
        gc.gridwidth = 2;
        fields.add(toggleModeRadio, gc);

        nextRow(gc);
        fields.add(holdModeRadio, gc);
        gc.gridwidth = 1;

        fields.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BUTTON_BG, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(fields);

        add(content, BorderLayout.NORTH);

        setControlsEnabled(false);
    }

    // -- UI handlers -----------------------------------------------------------

    private void onPushToTalkToggled() {
        boolean enabled = enablePushToTalkCheck.isSelected();
        pushToTalkEnabled = enabled;
        setControlsEnabled(enabled);
        if (enabled) {
            SdlInputService.getInstance().start();
            refreshControllerCombo();
        }
    }

    private void setControlsEnabled(boolean enabled) {
        controllerCombo.setEnabled(enabled);
        buttonCombo.setEnabled(enabled);
        toggleModeRadio.setEnabled(enabled);
        holdModeRadio.setEnabled(enabled);
    }

    private void onControllerSelected() {
        Object selected = controllerCombo.getSelectedItem();
        SvDevice device = (selected instanceof SvDevice d) ? d : null;
        selectedDevice = device;
        populateButtonCombo(device);
    }

    private void onButtonSelected() {
        selectedButtonIndex = buttonCombo.getSelectedIndex() - 1; // -1 = placeholder
    }

    // -- Combo population --------------------------------------------------------

    private void refreshControllerCombo() {
        SvDevice previouslySelected = selectedDevice;

        controllerCombo.removeAllItems();
        controllerCombo.addItem(getText("settings.input.controller.placeholder"));
        for (SvDevice device : SdlInputService.getInstance().getConnectedDevices()) {
            controllerCombo.addItem(device);
        }

        if (previouslySelected != null) {
            for (int i = 1; i < controllerCombo.getItemCount(); i++) {
                if (controllerCombo.getItemAt(i) instanceof SvDevice d && d.id() == previouslySelected.id()) {
                    controllerCombo.setSelectedIndex(i);
                    return;
                }
            }
        }
        controllerCombo.setSelectedIndex(0);
    }

    private void populateButtonCombo(SvDevice device) {
        buttonCombo.removeAllItems();
        buttonCombo.addItem(getText("settings.input.button.placeholder"));
        if (device != null) {
            for (int i = 1; i <= device.buttonCount(); i++) {
                buttonCombo.addItem("Button " + i);
            }
        }
        buttonCombo.setSelectedIndex(0);
    }

    // -- SDL event subscriptions -------------------------------------------------

    @Subscribe
    public void onDeviceConnected(SvDeviceConnectedEvent event) {
        SwingUtilities.invokeLater(this::refreshControllerCombo);
    }

    @Subscribe
    public void onDeviceDisconnected(SvDeviceDisconnectedEvent event) {
        SwingUtilities.invokeLater(() -> {
            if (selectedDevice != null && selectedDevice.id() == event.deviceId()) {
                selectedDevice = null;
                selectedButtonIndex = -1;
                populateButtonCombo(null);
            }
            refreshControllerCombo();
        });
    }

    @Subscribe
    public void onButtonState(SvButtonStateEvent event) {
        if (!pushToTalkEnabled) return;

        SvDevice device = selectedDevice;
        int buttonIndex = selectedButtonIndex;
        if (device == null || buttonIndex < 0) return;
        if (event.deviceId() != device.id() || event.buttonIndex() != buttonIndex) return;

        if (toggleMode) {
            if (event.pressed()) toggleSleepWake();
        } else {
            if (event.pressed()) wakeUp(); else sleep();
        }
    }

    // -- Sleep / Wake actions -----------------------------------------------------
    // Same code paths as the WAKEUP/SLEEP voice commands (StartListeningHandler / IgnoreMeHandler).

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
