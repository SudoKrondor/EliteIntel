package elite.intel.starvizion.overlay;

import elite.intel.devices.DeviceService;
import elite.intel.devices.model.Device;
import elite.intel.starvizion.model.SvButton;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudForms;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class ButtonSettingsDialog extends JDialog {

    private final ButtonVizlet vizlet;

    private JComboBox<Device>                  deviceCombo;
    private JComboBox<SvButton>                buttonCombo;
    private JComboBox<ButtonVizlet.ButtonShape> shapeCombo;
    private JButton                            colorButton;
    private Color                              chosenColor;
    private JSpinner                           widthSpinner;
    private JSpinner                           heightSpinner;

    public ButtonSettingsDialog(ButtonVizlet vizlet) {
        super((Frame) null, getText("starvizion.button.settings.title"), false);
        this.vizlet = vizlet;
        this.chosenColor = vizlet.getPressedColor();
        setSize(380, 310);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        buildUi();
        loadCurrent();
    }

    private void buildUi() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(new EmptyBorder(12, 16, 12, 16));
        root.setBackground(HudPalette.BG);
        setContentPane(root);

        GridBagConstraints gbc = HudForms.baseGbc();

        // Device
        nextRow(gbc);
        addLabel(root, getText("starvizion.button.device"), gbc);
        deviceCombo = new JComboBox<>();
        DeviceService.getInstance().getConnectedDevices().forEach(d -> deviceCombo.addItem(d));
        addField(root, deviceCombo, gbc);
        deviceCombo.addActionListener(e -> repopulateButtons());

        // Button
        nextRow(gbc);
        addLabel(root, getText("starvizion.button.button"), gbc);
        buttonCombo = new JComboBox<>();
        addField(root, buttonCombo, gbc);

        // Shape
        nextRow(gbc);
        addLabel(root, getText("starvizion.button.shape"), gbc);
        shapeCombo = new JComboBox<>(ButtonVizlet.ButtonShape.values());
        addField(root, shapeCombo, gbc);

        // Pressed color
        nextRow(gbc);
        addLabel(root, getText("starvizion.button.pressedColor"), gbc);
        colorButton = AppTheme.makeButton("  ");
        colorButton.setBackground(chosenColor);
        colorButton.setOpaque(true);
        colorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, getText("starvizion.button.chooseColor"), chosenColor);
            if (c != null) {
                chosenColor = c;
                colorButton.setBackground(c);
            }
        });
        addField(root, colorButton, gbc);

        // Width
        nextRow(gbc);
        addLabel(root, getText("starvizion.button.width"), gbc);
        widthSpinner = new JSpinner(new SpinnerNumberModel(vizlet.getWidth(), 60, 400, 10));
        addField(root, widthSpinner, gbc);

        // Height
        nextRow(gbc);
        addLabel(root, getText("starvizion.button.height"), gbc);
        heightSpinner = new JSpinner(new SpinnerNumberModel(vizlet.getHeight(), 60, 400, 10));
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
        shapeCombo.setSelectedItem(vizlet.getButtonShape());
    }

    private void repopulateButtons() {
        Device dev = (Device) deviceCombo.getSelectedItem();
        buttonCombo.removeAllItems();
        if (dev == null) return;
        for (int i = 0; i < dev.buttonCount(); i++) {
            buttonCombo.addItem(new SvButton(i, "Button " + i));
        }
        selectButtonByIndex(vizlet.getButtonIndex());
    }

    private void selectButtonByIndex(int index) {
        for (int i = 0; i < buttonCombo.getItemCount(); i++) {
            if (buttonCombo.getItemAt(i).index() == index) { buttonCombo.setSelectedIndex(i); return; }
        }
    }

    private void apply() {
        Device dev = (Device) deviceCombo.getSelectedItem();
        if (dev == null) { dispose(); return; }
        SvButton btn = (SvButton) buttonCombo.getSelectedItem();
        int b = btn != null ? btn.index() : 0;
        ButtonVizlet.ButtonShape shape = (ButtonVizlet.ButtonShape) shapeCombo.getSelectedItem();

        vizlet.configure(dev, b, chosenColor, shape != null ? shape : ButtonVizlet.ButtonShape.CIRCLE);

        int w = (int) widthSpinner.getValue();
        int h = (int) heightSpinner.getValue();
        vizlet.setSize(w, h);

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
