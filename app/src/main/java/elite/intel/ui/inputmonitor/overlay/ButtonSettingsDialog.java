package elite.intel.ui.inputmonitor.overlay;

import elite.intel.devices.DeviceService;
import elite.intel.devices.model.Device;
import elite.intel.ui.inputmonitor.InputMonitorPalette;
import elite.intel.ui.inputmonitor.model.DeviceButton;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudForms;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class ButtonSettingsDialog extends JDialog {

    private final ButtonReadout readout;

    private JComboBox<Device>                  deviceCombo;
    private JComboBox<DeviceButton> buttonCombo;
    private JComboBox<ButtonReadout.ButtonShape> shapeCombo;
    private JButton                            colorButton;
    private Color                              chosenColor;
    private JSpinner                           widthSpinner;
    private JSpinner                           heightSpinner;

    public ButtonSettingsDialog(ButtonReadout readout) {
        super((Frame) null, getText("inputMonitor.button.settings.title"), false);
        this.readout = readout;
        this.chosenColor = readout.getPressedColor();
        setSize(380, 310);
        setLocationRelativeTo(null);
        AppTheme.applyAppIcon(this);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        buildUi();
        loadCurrent();
    }

    private void buildUi() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(new EmptyBorder(12, 16, 12, 16));
        root.setBackground(InputMonitorPalette.SETTINGS_DIALOG_BACKGROUND);
        setContentPane(root);

        GridBagConstraints gbc = HudForms.baseGbc();

        // Device
        nextRow(gbc);
        addLabel(root, getText("inputMonitor.button.device"), gbc);
        deviceCombo = new JComboBox<>();
        DeviceService.getInstance().getConnectedDevices().forEach(d -> deviceCombo.addItem(d));
        addField(root, deviceCombo, gbc);
        deviceCombo.addActionListener(e -> repopulateButtons());

        // Button
        nextRow(gbc);
        addLabel(root, getText("inputMonitor.button.button"), gbc);
        buttonCombo = new JComboBox<>();
        addField(root, buttonCombo, gbc);

        // Shape
        nextRow(gbc);
        addLabel(root, getText("inputMonitor.button.shape"), gbc);
        shapeCombo = new JComboBox<>(ButtonReadout.ButtonShape.values());
        addField(root, shapeCombo, gbc);

        // Pressed color
        nextRow(gbc);
        addLabel(root, getText("inputMonitor.button.pressedColor"), gbc);
        colorButton = AppTheme.makeButton("  ");
        colorButton.setBackground(chosenColor);
        colorButton.setOpaque(true);
        colorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, getText("inputMonitor.button.chooseColor"), chosenColor);
            if (c != null) {
                chosenColor = c;
                colorButton.setBackground(c);
            }
        });
        addField(root, colorButton, gbc);

        // Width
        nextRow(gbc);
        addLabel(root, getText("inputMonitor.button.width"), gbc);
        widthSpinner = new JSpinner(new SpinnerNumberModel(readout.getWidth(), 60, 400, 10));
        addField(root, widthSpinner, gbc);

        // Height
        nextRow(gbc);
        addLabel(root, getText("inputMonitor.button.height"), gbc);
        heightSpinner = new JSpinner(new SpinnerNumberModel(readout.getHeight(), 60, 400, 10));
        addField(root, heightSpinner, gbc);

        // Buttons
        nextRow(gbc);
        gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setOpaque(false);
        JButton ok = AppTheme.makeButton(getText("button.save"));
        ok.addActionListener(e -> apply());
        JButton cancel = AppTheme.makeButtonSubtle(getText("button.cancel"));
        cancel.addActionListener(e -> dispose());
        btns.add(cancel);
        btns.add(ok);
        root.add(btns, gbc);

        AppTheme.applyDarkPalette(root);
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

    private void apply() {
        Device dev = (Device) deviceCombo.getSelectedItem();
        if (dev == null) { dispose(); return; }
        DeviceButton btn = (DeviceButton) buttonCombo.getSelectedItem();
        int b = btn != null ? btn.index() : 0;
        ButtonReadout.ButtonShape shape = (ButtonReadout.ButtonShape) shapeCombo.getSelectedItem();

        readout.configure(dev, b, chosenColor, shape != null ? shape : ButtonReadout.ButtonShape.CIRCLE);

        int w = (int) widthSpinner.getValue();
        int h = (int) heightSpinner.getValue();
        readout.setSize(w, h);

        dispose();
    }

    // -- Layout helpers --------------------------------------------------------

    private static void nextRow(GridBagConstraints gbc) { gbc.gridy++; gbc.gridwidth = 1; }

    private static void addLabel(JPanel p, String text, GridBagConstraints gbc) {
        HudForms.addLabel(p, text, gbc, 160); // central §5.1 field label
    }

    private static void addField(JPanel p, JComponent c, GridBagConstraints gbc) {
        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(c, gbc);
    }
}
