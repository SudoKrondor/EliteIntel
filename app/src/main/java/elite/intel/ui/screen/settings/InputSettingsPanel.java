package elite.intel.ui.screen.settings;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.ears.PushToTalkService;
import elite.intel.devices.DeviceService;
import elite.intel.devices.events.DeviceConnectedEvent;
import elite.intel.devices.events.DeviceDisconnectedEvent;
import elite.intel.devices.model.Device;
import elite.intel.eventbus.DeviceBus;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.PushToTalkSettingsChangedEvent;
import elite.intel.ui.event.RestartEarsEvent;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudTwoColumns;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeCheckBox;
import static elite.intel.ui.theme.AppTheme.transparentPanel;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND;

/**
 * "Input" settings tab - lets the commander map a controller button to push-to-talk, from the devices the
 * shared SDL3 poll loop in {@link DeviceService} reports.
 * <p>
 * Editing only. The mapped button is acted on by {@link PushToTalkService}, which reads these settings back
 * from {@code SystemSession}: a controller button must keep working whether or not this tab was ever opened,
 * and the microphone gate is not something anyone should have to find inside a settings screen.
 */
public class InputSettingsPanel extends JPanel {

    private JCheckBox enablePushToTalkCheck;
    private HudComboBox<Object> controllerCombo;
    private HudComboBox<String> buttonCombo;

    // Selection state behind the combos. EDT-only: the device subscriptions below hand straight to
    // invokeLater, and the button itself is read by PushToTalkService, not here.
    private boolean pushToTalkEnabled = false;
    private Device selectedDevice = null;
    private int selectedButtonIndex = -1; // 0-based SDL button index, -1 = none

    // Name of the controller persisted in game_session, used to re-select it once it appears
    // in the connected-devices list (initial load, or reconnect after a disconnect).
    private String persistedControllerName = null;

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
        persistedControllerName = session.getPushToTalkControllerName();

        enablePushToTalkCheck.setSelected(pushToTalkEnabled);
        setControlsEnabled(pushToTalkEnabled);

        reconcileControllerSelection();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        // Single flat working section (section 9). Two-column body (section 10): left column holds the master
        // enable slab (stretched to the column width, no label); right column holds the controller/button
        // pickers as aligned rows.
        HudSection section = HudSection.flat(getText("settings.input.section.binding"), new BorderLayout());
        JPanel body = section.body();

        enablePushToTalkCheck = makeCheckBox(getText("settings.input.enablePushToTalk"), false);
        enablePushToTalkCheck.addActionListener(e -> onPushToTalkToggled());

        // Left column - the enable slab, full-width (span) and label-less. GridBag + baseGbc so its row
        // insets come from the same shared source as the right column (no hand-tuned border).
        JPanel leftCol = transparentPanel(new GridBagLayout());
        GridBagConstraints lgc = baseGbc();
        addSpanComponent(leftCol, enablePushToTalkCheck, lgc);
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

    // -- UI handlers -----------------------------------------------------------

    private void onPushToTalkToggled() {
        boolean enabled = enablePushToTalkCheck.isSelected();
        pushToTalkEnabled = enabled;
        setControlsEnabled(enabled);
        if (enabled) {
            reconcileControllerSelection();
        }
        SystemSession.getInstance().setPushToTalkEnabled(enabled);
        UiBus.publish(new PushToTalkSettingsChangedEvent());
        // PTT on/off only affects the STT pipeline; it previously took effect only after an app
        // restart. Restart just the EARS service so the change applies now (no full rebuild needed).
        UiBus.publish(new RestartEarsEvent());
    }

    private void setControlsEnabled(boolean enabled) {
        controllerCombo.setEnabled(enabled);
        buttonCombo.setEnabled(enabled);
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

}
