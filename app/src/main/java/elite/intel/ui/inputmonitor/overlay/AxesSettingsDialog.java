package elite.intel.ui.inputmonitor.overlay;

import elite.intel.devices.DeviceService;
import elite.intel.devices.model.Device;
import elite.intel.ui.inputmonitor.model.DeviceAxis;
import elite.intel.ui.theme.HudForms;
import elite.intel.ui.widget.HudColorSwatch;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudSpinner;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

final class AxesSettingsDialog extends ReadoutSettingsDialog {

    private final AxesReadout readout;

    private JComboBox<Device> deviceCombo;
    private JComboBox<DeviceAxis> xAxisCombo;
    private JComboBox<DeviceAxis> yAxisCombo;
    private HudColorSwatch colorSwatch;
    private JComboBox<AxesReadout.DotShape> shapeCombo;
    private JSpinner            widthSpinner;
    private JSpinner            heightSpinner;

    AxesSettingsDialog(AxesReadout readout) {
        super(readout, getText("inputMonitor.axes.settings.title"));
        this.readout = readout;
        buildUi();
        loadCurrent();
    }

    private void buildUi() {
        JPanel form = newForm();
        GridBagConstraints gbc = HudForms.baseGbc();

        deviceCombo = new HudComboBox<>(new DefaultComboBoxModel<>());
        DeviceService.getInstance().getConnectedDevices().forEach(d -> deviceCombo.addItem(d));
        addRow(form, getText("inputMonitor.axes.device"), deviceCombo, gbc);
        deviceCombo.addActionListener(e -> repopulateAxes());

        xAxisCombo = new HudComboBox<>(new DefaultComboBoxModel<>());
        addRow(form, getText("inputMonitor.axes.xAxis"), xAxisCombo, gbc);

        yAxisCombo = new HudComboBox<>(new DefaultComboBoxModel<>());
        addRow(form, getText("inputMonitor.axes.yAxis"), yAxisCombo, gbc);

        colorSwatch = addColorRow(form, getText("inputMonitor.axes.dotColor"),
                getText("inputMonitor.axes.chooseColor"), readout.getDotColor(), gbc);

        shapeCombo = new HudComboBox<>(AxesReadout.DotShape.values());
        addRow(form, getText("inputMonitor.axes.dotShape"), shapeCombo, gbc);

        widthSpinner = new HudSpinner(readout.getWidth(), 80, 600, 10);
        addRow(form, getText("inputMonitor.axes.width"), widthSpinner, gbc);

        heightSpinner = new HudSpinner(readout.getHeight(), 80, 600, 10);
        addRow(form, getText("inputMonitor.axes.height"), heightSpinner, gbc);

        install(form);
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

    @Override
    void apply() {
        Device dev = (Device) deviceCombo.getSelectedItem();
        if (dev == null) { dispose(); return; }
        DeviceAxis xAxis = (DeviceAxis) xAxisCombo.getSelectedItem();
        DeviceAxis yAxis = (DeviceAxis) yAxisCombo.getSelectedItem();
        int x = xAxis != null ? xAxis.index() : 0;
        int y = yAxis != null ? yAxis.index() : 1;
        AxesReadout.DotShape shape = (AxesReadout.DotShape) shapeCombo.getSelectedItem();

        readout.configure(dev, x, y, colorSwatch.getColor(), shape != null ? shape : AxesReadout.DotShape.CIRCLE);

        int w = (int) widthSpinner.getValue();
        int h = (int) heightSpinner.getValue();
        readout.setSize(w, h);

        dispose();
    }
}
