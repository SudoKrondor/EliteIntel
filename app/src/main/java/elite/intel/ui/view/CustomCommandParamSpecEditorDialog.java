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
    private final HudComboBox<String> typeCombo = new HudComboBox<>(TYPES);
    private final JCheckBox requiredCheck = new JCheckBox();
    private final JTextField descriptionField = new JTextField(28);
    private final JTextField examplesField = new JTextField(28);
    private final JTextField extractionHintField = new JTextField(28);
    private CustomCommandParameterSpec result;

    CustomCommandParamSpecEditorDialog(Component parent, CustomCommandParameterSpec spec) {
        super(SwingUtilities.getWindowAncestor(parent),
                getText("actions.customCommands.editor.param.title"),
                ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
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
        HudSection section = HudSection.flat(
                getText("actions.customCommands.editor.param.section.definition"), new BorderLayout());
        section.body().add(form(), BorderLayout.CENTER);

        JButton save = AppTheme.makeButton(getText("button.save"));
        save.addActionListener(e -> save());
        JButton back = AppTheme.makeButtonSubtle(getText("button.back"));
        back.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("actions.customCommands.editor.param.title"))
                .onClose(this::dispose)
                .body(section)
                .scrollBody(false)
                .primary(save)                // right side
                .dismiss(back)                // left side
                .build();

        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(save);
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
