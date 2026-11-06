package elite.intel.ui.inputmonitor.overlay;

import elite.intel.devices.DeviceService;
import elite.intel.devices.model.Device;
import elite.intel.ui.inputmonitor.model.DeviceButton;
import elite.intel.ui.theme.HudForms;
import elite.intel.ui.widget.HudColorSwatch;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudSpinner;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

final class ButtonSettingsDialog extends ReadoutSettingsDialog {

    private final ButtonReadout readout;

    private JComboBox<Device>                  deviceCombo;
    private JComboBox<DeviceButton> buttonCombo;
    private JComboBox<ButtonReadout.ButtonShape> shapeCombo;
    private HudColorSwatch colorSwatch;
    private JSpinner                           widthSpinner;
    private JSpinner                           heightSpinner;

    ButtonSettingsDialog(ButtonReadout readout) {
        super(readout, getText("inputMonitor.button.settings.title"));
        this.readout = readout;
        buildUi();
        loadCurrent();
    }

    private void buildUi() {
        JPanel form = newForm();
        GridBagConstraints gbc = HudForms.baseGbc();

        deviceCombo = new HudComboBox<>(new DefaultComboBoxModel<>());
        DeviceService.getInstance().getConnectedDevices().forEach(d -> deviceCombo.addItem(d));
        addRow(form, getText("inputMonitor.button.device"), deviceCombo, gbc);
        deviceCombo.addActionListener(e -> repopulateButtons());

        buttonCombo = new HudComboBox<>(new DefaultComboBoxModel<>());
        addRow(form, getText("inputMonitor.button.button"), buttonCombo, gbc);

        shapeCombo = new HudComboBox<>(ButtonReadout.ButtonShape.values());
        addRow(form, getText("inputMonitor.button.shape"), shapeCombo, gbc);

        colorSwatch = addColorRow(form, getText("inputMonitor.button.pressedColor"),
                getText("inputMonitor.button.chooseColor"), readout.getPressedColor(), gbc);

        widthSpinner = new HudSpinner(readout.getWidth(), 60, 400, 10);
        addRow(form, getText("inputMonitor.button.width"), widthSpinner, gbc);

        heightSpinner = new HudSpinner(readout.getHeight(), 60, 400, 10);
        addRow(form, getText("inputMonitor.button.height"), heightSpinner, gbc);

        install(form);
    }

    private void loadCurrent() {
        repopulateButtons();
        shapeCombo.setSelectedItem(readout.getButtonShape());
    }

    private void repopulateButtons() {
        Device dev = (Device) deviceCombo.getSelectedItem();
        buttonCombo.removeAllItems();
        if (dev == null) return;
        for (int i = 0; i < dev.buttonCount(); i++) {
            buttonCombo.addItem(new DeviceButton(i, "Button " + i));
        }
        selectButtonByIndex(readout.getButtonIndex());
    }

    private void selectButtonByIndex(int index) {
        for (int i = 0; i < buttonCombo.getItemCount(); i++) {
            if (buttonCombo.getItemAt(i).index() == index) { buttonCombo.setSelectedIndex(i); return; }
        }
    }

    @Override
    void apply() {
        Device dev = (Device) deviceCombo.getSelectedItem();
        if (dev == null) { dispose(); return; }
        DeviceButton btn = (DeviceButton) buttonCombo.getSelectedItem();
        int b = btn != null ? btn.index() : 0;
        ButtonReadout.ButtonShape shape = (ButtonReadout.ButtonShape) shapeCombo.getSelectedItem();

        readout.configure(dev, b, colorSwatch.getColor(), shape != null ? shape : ButtonReadout.ButtonShape.CIRCLE);

        int w = (int) widthSpinner.getValue();
        int h = (int) heightSpinner.getValue();
        readout.setSize(w, h);

        dispose();
    }
}
