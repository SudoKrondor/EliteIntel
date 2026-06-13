package elite.intel.starvizion.overlay;

import elite.intel.devices.DeviceService;
import elite.intel.devices.model.Device;
import elite.intel.starvizion.model.SvAxis;
import elite.intel.ui.view.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class AxesSettingsDialog extends JDialog {

    private final AxesVizlet vizlet;

    private JComboBox<Device> deviceCombo;
    private JComboBox<SvAxis>   xAxisCombo;
    private JComboBox<SvAxis>   yAxisCombo;
    private JButton             colorButton;
    private Color               chosenColor;
    private JComboBox<AxesVizlet.DotShape> shapeCombo;
    private JSpinner            widthSpinner;
    private JSpinner            heightSpinner;

    public AxesSettingsDialog(AxesVizlet vizlet) {
        super((Frame) null, getText("starvizion.axes.settings.title"), false);
        this.vizlet = vizlet;
        this.chosenColor = vizlet.getDotColor();
        setSize(380, 340);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        buildUi();
        loadCurrent();
    }

    private void buildUi() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(new EmptyBorder(12, 16, 12, 16));
        root.setBackground(AppTheme.BG);
        setContentPane(root);

        GridBagConstraints gbc = AppTheme.baseGbc();

        // Device
        nextRow(gbc);
        addLabel(root, getText("starvizion.axes.device"), gbc);
        deviceCombo = new JComboBox<>();
        DeviceService.getInstance().getConnectedDevices().forEach(d -> deviceCombo.addItem(d));
        addField(root, deviceCombo, gbc);
        deviceCombo.addActionListener(e -> repopulateAxes());

        // X Axis
        nextRow(gbc);
        addLabel(root, getText("starvizion.axes.xAxis"), gbc);
        xAxisCombo = new JComboBox<>();
        addField(root, xAxisCombo, gbc);

        // Y Axis
        nextRow(gbc);
        addLabel(root, getText("starvizion.axes.yAxis"), gbc);
        yAxisCombo = new JComboBox<>();
        addField(root, yAxisCombo, gbc);

        // Dot color
        nextRow(gbc);
        addLabel(root, getText("starvizion.axes.dotColor"), gbc);
        colorButton = AppTheme.makeButton("  ");
        colorButton.setBackground(chosenColor);
        colorButton.setOpaque(true);
        colorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, getText("starvizion.axes.chooseColor"), chosenColor);
            if (c != null) {
                chosenColor = c;
                colorButton.setBackground(c);
            }
        });
        addField(root, colorButton, gbc);

        // Dot shape
        nextRow(gbc);
        addLabel(root, getText("starvizion.axes.dotShape"), gbc);
        shapeCombo = new JComboBox<>(AxesVizlet.DotShape.values());
        addField(root, shapeCombo, gbc);

        // Width
        nextRow(gbc);
        addLabel(root, getText("starvizion.axes.width"), gbc);
        widthSpinner = new JSpinner(new SpinnerNumberModel(vizlet.getWidth(), 80, 600, 10));
        addField(root, widthSpinner, gbc);

        // Height
        nextRow(gbc);
        addLabel(root, getText("starvizion.axes.height"), gbc);
        heightSpinner = new JSpinner(new SpinnerNumberModel(vizlet.getHeight(), 80, 600, 10));
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
        shapeCombo.setSelectedItem(vizlet.getDotShape());
    }

    private void repopulateAxes() {
        Device dev = (Device) deviceCombo.getSelectedItem();
        xAxisCombo.removeAllItems();
        yAxisCombo.removeAllItems();
        if (dev == null) return;
        for (int i = 0; i < dev.axisCount(); i++) {
            SvAxis axis = new SvAxis(i, "Axis " + i);
            xAxisCombo.addItem(axis);
            yAxisCombo.addItem(axis);
        }
        selectAxisByIndex(xAxisCombo, vizlet.getXAxisIndex());
        selectAxisByIndex(yAxisCombo, vizlet.getYAxisIndex());
    }

    private void selectAxisByIndex(JComboBox<SvAxis> combo, int index) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).index() == index) { combo.setSelectedIndex(i); return; }
        }
    }

    private void apply() {
        Device dev = (Device) deviceCombo.getSelectedItem();
        if (dev == null) { dispose(); return; }
        SvAxis xAxis = (SvAxis) xAxisCombo.getSelectedItem();
        SvAxis yAxis = (SvAxis) yAxisCombo.getSelectedItem();
        int x = xAxis != null ? xAxis.index() : 0;
        int y = yAxis != null ? yAxis.index() : 1;
        AxesVizlet.DotShape shape = (AxesVizlet.DotShape) shapeCombo.getSelectedItem();

        vizlet.configure(dev, x, y, chosenColor, shape != null ? shape : AxesVizlet.DotShape.CIRCLE);

        int w = (int) widthSpinner.getValue();
        int h = (int) heightSpinner.getValue();
        vizlet.setSize(w, h);

        dispose();
    }

    // -- Layout helpers (mirrors AppTheme style) --------------------------------

    private static void nextRow(GridBagConstraints gbc) { gbc.gridy++; gbc.gridwidth = 1; }

    private static void addLabel(JPanel p, String text, GridBagConstraints gbc) {
        gbc.gridx = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        JLabel lbl = new JLabel(text);
        lbl.setPreferredSize(new Dimension(160, 32));
        p.add(lbl, gbc);
    }

    private static void addField(JPanel p, JComponent c, GridBagConstraints gbc) {
        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(c, gbc);
    }
}
