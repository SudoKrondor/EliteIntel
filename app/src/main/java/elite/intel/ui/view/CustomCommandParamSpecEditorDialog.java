package elite.intel.ui.view;

import elite.intel.ai.brain.actions.customcommand.CustomCommandParameterSpec;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Modal editor for one {@link CustomCommandParameterSpec}.
 */
final class CustomCommandParamSpecEditorDialog extends JDialog {

    private static final String[] TYPES = {"string", "number", "boolean"};

    private final JTextField nameField = new JTextField(28);
    private final JComboBox<String> typeCombo = new JComboBox<>(TYPES);
    private final JCheckBox requiredCheck = new JCheckBox();
    private final JTextField descriptionField = new JTextField(28);
    private final JTextField examplesField = new JTextField(28);
    private final JTextField extractionHintField = new JTextField(28);
    private CustomCommandParameterSpec result;

    CustomCommandParamSpecEditorDialog(Component parent, CustomCommandParameterSpec spec) {
        super(SwingUtilities.getWindowAncestor(parent),
                getText("actions.customCommands.editor.param.title"),
                ModalityType.APPLICATION_MODAL);
        populate(spec);
        buildUi();
    }

    CustomCommandParameterSpec showDialog() {
        setVisible(true);
        return result;
    }

    private void populate(CustomCommandParameterSpec spec) {
        if (spec == null) return;
        nameField.setText(spec.getName() != null ? spec.getName() : "");
        typeCombo.setSelectedItem(spec.getType() != null ? spec.getType() : "string");
        requiredCheck.setSelected(spec.isRequired());
        descriptionField.setText(spec.getDescription());
        examplesField.setText(String.join(", ", spec.getExamples()));
        extractionHintField.setText(spec.getExtractionHint() != null ? spec.getExtractionHint() : "");
    }

    private void buildUi() {
        JPanel content = AppTheme.transparentPanel(new BorderLayout(0, AppTheme.HUD_GAP));
        content.setOpaque(true);
        content.setBackground(AppTheme.HUD_BG);
        content.setBorder(new EmptyBorder(16, 18, 12, 18));
        HudSection section = new HudSection(getText("actions.customCommands.editor.param.section.definition"), new BorderLayout());
        section.body().add(form(), BorderLayout.CENTER);
        content.add(section, BorderLayout.CENTER);
        content.add(buttons(), BorderLayout.SOUTH);
        setContentPane(content);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(480, 320));
        setLocationRelativeTo(getOwner());
    }

    private JPanel form() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = AppTheme.baseGbc();

        addRow(panel, gbc, getText("actions.customCommands.editor.param.name"), nameField);
        addRow(panel, gbc, getText("actions.customCommands.editor.param.type"), typeCombo);
        addRow(panel, gbc, getText("actions.customCommands.editor.param.required"), requiredCheck);
        addRow(panel, gbc, getText("actions.customCommands.editor.param.description"), descriptionField);
        addRow(panel, gbc, getText("actions.customCommands.editor.param.examples"), examplesField);
        addRow(panel, gbc, getText("actions.customCommands.editor.param.extractionHint"), extractionHintField);

        AppTheme.applyDarkPalette(panel);
        AppTheme.styleTextComponent(nameField);
        AppTheme.styleComboBox(typeCombo);
        AppTheme.styleCheckBox(requiredCheck);
        AppTheme.styleTextComponent(descriptionField);
        AppTheme.styleTextComponent(examplesField);
        AppTheme.styleTextComponent(extractionHintField);
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel label = new JLabel(labelText);
        label.setForeground(AppTheme.FG_MUTED);
        label.setBorder(new EmptyBorder(0, 0, 0, 12));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
        gbc.gridy++;
    }

    private JPanel buttons() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JButton save = AppTheme.makeButton(getText("button.save"));
        save.addActionListener(e -> save());
        panel.add(save, BorderLayout.WEST);
        JButton cancel = AppTheme.makeButtonSubtle(getText("button.cancel"));
        cancel.addActionListener(e -> dispose());
        panel.add(cancel, BorderLayout.EAST);
        getRootPane().setDefaultButton(save);
        return panel;
    }

    private void save() {
        String name = nameField.getText().trim();
        if (name.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    getText("actions.customCommands.editor.param.name") + " is required.",
                    getText("actions.customCommands.editor.validation.title"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        String type = (String) typeCombo.getSelectedItem();
        if (type == null) type = "string";
        boolean required = requiredCheck.isSelected();
        String description = descriptionField.getText().trim();
        List<String> examples = Arrays.stream(examplesField.getText().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        String hint = extractionHintField.getText().trim();

        result = new CustomCommandParameterSpec(name, type, required, description,
                examples.isEmpty() ? null : examples,
                hint.isBlank() ? null : hint);
        dispose();
    }
}
