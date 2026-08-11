package elite.intel.ui.inputmonitor.overlay;

import elite.intel.devices.DeviceService;
import elite.intel.devices.model.Device;
import elite.intel.ui.inputmonitor.InputMonitorPalette;
import elite.intel.ui.inputmonitor.model.DeviceAxis;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudForms;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class AxesSettingsDialog extends JDialog {

    private final AxesReadout readout;

    private JComboBox<Device> deviceCombo;
    private JComboBox<DeviceAxis> xAxisCombo;
    private JComboBox<DeviceAxis> yAxisCombo;
    private JButton             colorButton;
    private Color               chosenColor;
    private JComboBox<AxesReadout.DotShape> shapeCombo;
    private JSpinner            widthSpinner;
    private JSpinner            heightSpinner;

    public AxesSettingsDialog(AxesReadout readout) {
        super((Frame) null, getText("inputMonitor.axes.settings.title"), false);
        this.readout = readout;
        this.chosenColor = readout.getDotColor();
        setSize(380, 340);
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
        addLabel(root, getText("inputMonitor.axes.device"), gbc);
        deviceCombo = new JComboBox<>();
        DeviceService.getInstance().getConnectedDevices().forEach(d -> deviceCombo.addItem(d));
        addField(root, deviceCombo, gbc);
        deviceCombo.addActionListener(e -> repopulateAxes());

        // X Axis
        nextRow(gbc);
        addLabel(root, getText("inputMonitor.axes.xAxis"), gbc);
        xAxisCombo = new JComboBox<>();
        addField(root, xAxisCombo, gbc);

        // Y Axis
        nextRow(gbc);
        addLabel(root, getText("inputMonitor.axes.yAxis"), gbc);
        yAxisCombo = new JComboBox<>();
        addField(root, yAxisCombo, gbc);

        // Dot color
        nextRow(gbc);
        addLabel(root, getText("inputMonitor.axes.dotColor"), gbc);
        colorButton = AppTheme.makeButton("  ");
        colorButton.setBackground(chosenColor);
        colorButton.setOpaque(true);
        colorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, getText("inputMonitor.axes.chooseColor"), chosenColor);
            if (c != null) {
                chosenColor = c;
                colorButton.setBackground(c);
            }
        });
        addField(root, colorButton, gbc);

        // Dot shape
        nextRow(gbc);
        addLabel(root, getText("inputMonitor.axes.dotShape"), gbc);
        shapeCombo = new JComboBox<>(AxesReadout.DotShape.values());
        addField(root, shapeCombo, gbc);

        // Width
        nextRow(gbc);
        addLabel(root, getText("inputMonitor.axes.width"), gbc);
        widthSpinner = new JSpinner(new SpinnerNumberModel(readout.getWidth(), 80, 600, 10));
        addField(root, widthSpinner, gbc);

        // Height
        nextRow(gbc);
        addLabel(root, getText("inputMonitor.axes.height"), gbc);
        heightSpinner = new JSpinner(new SpinnerNumberModel(readout.getHeight(), 80, 600, 10));
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
        repopulateAxes();
        shapeCombo.setSelectedItem(readout.getDotShape());
    }

    private void repopulateAxes() {
        Device dev = (Device) deviceCombo.getSelectedItem();
        xAxisCombo.removeAllItems();
        yAxisCombo.removeAllItems();
        if (dev == null) return;
        for (int i = 0; i < dev.axisCount(); i++) {
            DeviceAxis axis = new DeviceAxis(i, "Axis " + i);
            xAxisCombo.addItem(axis);
            yAxisCombo.addItem(axis);
        }
        selectAxisByIndex(xAxisCombo, readout.getXAxisIndex());
        selectAxisByIndex(yAxisCombo, readout.getYAxisIndex());
    }

    private void selectAxisByIndex(JComboBox<DeviceAxis> combo, int index) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).index() == index) { combo.setSelectedIndex(i); return; }
        }
    }

    private void apply() {
        Device dev = (Device) deviceCombo.getSelectedItem();
        if (dev == null) { dispose(); return; }
        DeviceAxis xAxis = (DeviceAxis) xAxisCombo.getSelectedItem();
        DeviceAxis yAxis = (DeviceAxis) yAxisCombo.getSelectedItem();
        int x = xAxis != null ? xAxis.index() : 0;
        int y = yAxis != null ? yAxis.index() : 1;
        AxesReadout.DotShape shape = (AxesReadout.DotShape) shapeCombo.getSelectedItem();

        readout.configure(dev, x, y, chosenColor, shape != null ? shape : AxesReadout.DotShape.CIRCLE);

        int w = (int) widthSpinner.getValue();
        int h = (int) heightSpinner.getValue();
        readout.setSize(w, h);

        dispose();
    }

    // -- Layout helpers (mirrors AppTheme style) --------------------------------

    private static void nextRow(GridBagConstraints gbc) { gbc.gridy++; gbc.gridwidth = 1; }

    private static void addLabel(JPanel p, String text, GridBagConstraints gbc) {
        HudForms.addLabel(p, text, gbc, 160); // central §5.1 field label
    }

    private static void addField(JPanel p, JComponent c, GridBagConstraints gbc) {
        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(c, gbc);
    }
}
