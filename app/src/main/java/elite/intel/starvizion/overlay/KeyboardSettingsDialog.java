package elite.intel.starvizion.overlay;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudForms;
import elite.intel.starvizion.StarVizionPalette;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class KeyboardSettingsDialog extends JDialog {

    private final KeyboardVizlet vizlet;

    private JButton  backgroundColorButton;
    private Color    chosenBackgroundColor;
    private JButton  textColorButton;
    private Color    chosenTextColor;
    private JSpinner widthSpinner;
    private JSpinner heightSpinner;

    public KeyboardSettingsDialog(KeyboardVizlet vizlet) {
        super((Frame) null, getText("starvizion.keyboard.settings.title"), false);
        this.vizlet = vizlet;
        this.chosenBackgroundColor = vizlet.getBackgroundColor();
        this.chosenTextColor = vizlet.getTextColor();
        setSize(380, 280);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        buildUi();
    }

    private void buildUi() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(new EmptyBorder(12, 16, 12, 16));
        root.setBackground(StarVizionPalette.SETTINGS_DIALOG_BACKGROUND);
        setContentPane(root);

        GridBagConstraints gbc = HudForms.baseGbc();

        // Background color
        nextRow(gbc);
        addLabel(root, getText("starvizion.keyboard.backgroundColor"), gbc);
        backgroundColorButton = AppTheme.makeButton("  ");
        backgroundColorButton.setBackground(chosenBackgroundColor);
        backgroundColorButton.setOpaque(true);
        backgroundColorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, getText("starvizion.keyboard.chooseBackgroundColor"), chosenBackgroundColor);
            if (c != null) {
                chosenBackgroundColor = c;
                backgroundColorButton.setBackground(c);
            }
        });
        addField(root, backgroundColorButton, gbc);

        // Text color
        nextRow(gbc);
        addLabel(root, getText("starvizion.keyboard.textColor"), gbc);
        textColorButton = AppTheme.makeButton("  ");
        textColorButton.setBackground(chosenTextColor);
        textColorButton.setOpaque(true);
        textColorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, getText("starvizion.keyboard.chooseTextColor"), chosenTextColor);
            if (c != null) {
                chosenTextColor = c;
                textColorButton.setBackground(c);
            }
        });
        addField(root, textColorButton, gbc);

        // Width
        nextRow(gbc);
        addLabel(root, getText("starvizion.keyboard.width"), gbc);
        widthSpinner = new JSpinner(new SpinnerNumberModel(vizlet.getWidth(), 80, 600, 10));
        addField(root, widthSpinner, gbc);

        // Height
        nextRow(gbc);
        addLabel(root, getText("starvizion.keyboard.height"), gbc);
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

    private void apply() {
        vizlet.configure(chosenBackgroundColor, chosenTextColor);

        int w = (int) widthSpinner.getValue();
        int h = (int) heightSpinner.getValue();
        vizlet.setSize(w, h);

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
