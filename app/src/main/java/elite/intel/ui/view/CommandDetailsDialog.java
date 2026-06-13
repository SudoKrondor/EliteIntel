package elite.intel.ui.view;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AiActionsMap;
import elite.intel.ai.brain.actions.catalog.CommandCatalogEntry;
import elite.intel.ai.brain.actions.catalog.CommandCatalogEntryType;
import elite.intel.ai.brain.actions.customcommand.CustomCommandParameterSpec;
import elite.intel.ai.brain.i18n.AiActionLocalizations;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Displays command metadata and provides a GUI entry point into post-LLM command dispatch.
 */
public final class CommandDetailsDialog extends JDialog {

    private static final Pattern PARAMETER_BLOCK = Pattern.compile("\\{([^}]+)}");
    private final CommandCatalogEntry entry;
    private final List<String> phrases;
    private final boolean showPhraseCorrection;
    private final String sequenceText;
    private final List<CustomCommandParameterSpec> customCommandParameters;
    private final Runnable editAction;
    private final Runnable deleteAction;

    public CommandDetailsDialog(Component parent, CommandCatalogEntry entry) {
        this(parent, entry, AiActionLocalizations.phrasesForAction(entry.id()), true);
    }

    /**
     * Creates a details dialog for entries whose phrases do not come from built-in localization aliases.
     */
    public CommandDetailsDialog(
            Component parent,
            CommandCatalogEntry entry,
            List<String> phrases,
            boolean showPhraseCorrection
    ) {
        this(parent, entry, phrases, showPhraseCorrection, "");
    }

    /**
     * Creates a details dialog with an extra read-only customCommand sequence section.
     */
    CommandDetailsDialog(
            Component parent,
            CommandCatalogEntry entry,
            List<String> phrases,
            boolean showPhraseCorrection,
            String sequenceText
    ) {
        this(parent, entry, phrases, showPhraseCorrection, sequenceText, List.of(), null, null);
    }

    /**
     * Creates a customCommand details dialog with optional editing actions owned by the custom command list panel.
     * Use the overload with {@code customCommandParameters} to show declared custom command parameters.
     */
    CommandDetailsDialog(
            Component parent,
            CommandCatalogEntry entry,
            List<String> phrases,
            boolean showPhraseCorrection,
            String sequenceText,
            Runnable editAction,
            Runnable deleteAction
    ) {
        this(parent, entry, phrases, showPhraseCorrection, sequenceText, List.of(), editAction, deleteAction);
    }

    /**
     * Creates a customCommand details dialog that shows declared custom command parameters and prompts for them on Run.
     */
    CommandDetailsDialog(
            Component parent,
            CommandCatalogEntry entry,
            List<String> phrases,
            boolean showPhraseCorrection,
            String sequenceText,
            List<CustomCommandParameterSpec> customCommandParameters,
            Runnable editAction,
            Runnable deleteAction
    ) {
        super(SwingUtilities.getWindowAncestor(parent), dialogTitle(entry), ModalityType.APPLICATION_MODAL);
        this.entry = Objects.requireNonNull(entry, "entry");
        this.phrases = phrases == null ? List.of() : List.copyOf(phrases);
        this.showPhraseCorrection = showPhraseCorrection;
        this.sequenceText = sequenceText == null ? "" : sequenceText;
        this.customCommandParameters = customCommandParameters == null ? List.of() : List.copyOf(customCommandParameters);
        this.editAction = editAction;
        this.deleteAction = deleteAction;
        buildUi();
    }

    public void showDialog() {
        setVisible(true);
    }

    private void buildUi() {
        JPanel content = AppTheme.transparentPanel(new BorderLayout(0, AppTheme.HUD_GAP));
        content.setBackground(AppTheme.HUD_BG);
        content.setOpaque(true);
        content.setBorder(new EmptyBorder(16, 18, 12, 18));
        HudSection detailsSection = new HudSection(getText("actions.commands.details.section.metadata"), new BorderLayout());
        detailsSection.body().add(details(), BorderLayout.CENTER);
        content.add(header(), BorderLayout.NORTH);
        content.add(detailsSection, BorderLayout.CENTER);
        content.add(buttons(), BorderLayout.SOUTH);
        setContentPane(content);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(720, 520));
        setLocationRelativeTo(getOwner());
    }

    private JPanel header() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel name = new JLabel(entry.name());
        name.setForeground(AppTheme.FG);
        name.setFont(name.getFont().deriveFont(Font.BOLD, name.getFont().getSize2D() + 5f));

        JLabel id = new JLabel(entry.id());
        id.setForeground(AppTheme.FG_MUTED);
        id.setBorder(new EmptyBorder(4, 0, 0, 0));

        panel.add(name);
        panel.add(id);
        return panel;
    }

    private JPanel details() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 0, 5, 12);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        addLabelValue(panel, gbc, getText("actions.commands.details.name"), entry.name());
        addLabelValue(panel, gbc, getText("actions.commands.details.actionKey"), entry.id());
        addLabelValue(panel, gbc, getText("actions.commands.details.type"), readableType(entry.type()));
        addDescription(panel, gbc);
        addPhrases(panel, gbc);
        addParameters(panel, gbc);
        addSequence(panel, gbc);

        gbc.gridx = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createGlue(), gbc);
        return panel;
    }

    private void addLabelValue(JPanel panel, GridBagConstraints gbc, String labelText, String value) {
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel label = detailLabel(labelText);
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(AppTheme.FG);
        panel.add(valueLabel, gbc);
        gbc.gridy++;
    }

    private void addDescription(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(detailLabel(getText("actions.commands.details.description")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextArea description = readOnlyTextArea(entry.description());
        description.setRows(3);
        panel.add(AppTheme.hudScrollPane(description), gbc);
        gbc.gridy++;
    }

    private void addPhrases(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(detailLabel(getText("actions.commands.details.phrases")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(phrasesSection(), gbc);
        gbc.gridy++;
        gbc.weighty = 0.0;
    }

    private void addParameters(JPanel panel, GridBagConstraints gbc) {
        if (customCommandParameters.isEmpty()) return;

        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(detailLabel(getText("actions.customCommands.details.parameters")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        StringBuilder sb = new StringBuilder();
        for (CustomCommandParameterSpec spec : customCommandParameters) {
            sb.append(spec.getName()).append(" (").append(spec.getType());
            if (spec.isRequired()) sb.append(", required");
            sb.append(")");
            sb.append(System.lineSeparator());
        }
        JTextArea area = readOnlyTextArea(sb.toString().stripTrailing());
        area.setRows(customCommandParameters.size());
        panel.add(area, gbc);
        gbc.gridy++;
    }

    private void addSequence(JPanel panel, GridBagConstraints gbc) {
        if (sequenceText.isBlank()) {
            return;
        }

        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(detailLabel(getText("actions.customCommands.details.sequence")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JTextArea sequence = readOnlyTextArea(sequenceText);
        sequence.setRows(Math.min(10, Math.max(4, sequenceText.split("\\R").length)));
        panel.add(AppTheme.hudScrollPane(sequence), gbc);
        gbc.gridy++;
        gbc.weighty = 0.0;
    }

    private JComponent phrasesSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.add(phrasesComponent(), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        if (showPhraseCorrection) {
            JButton suggestCorrection = AppTheme.makeButtonSubtle(getText("actions.commands.details.suggestCorrection"));
            suggestCorrection.addActionListener(event -> openPhraseCorrectionDialog());
            actions.add(suggestCorrection);
        }
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent phrasesComponent() {
        List<String> phrases = currentPhrases();
        if (phrases.isEmpty()) {
            JLabel empty = new JLabel(getText("actions.commands.details.noPhrases"));
            empty.setForeground(AppTheme.FG_MUTED);
            empty.setBorder(new EmptyBorder(8, 8, 8, 8));
            return empty;
        }

        JTextArea area = readOnlyTextArea(String.join(System.lineSeparator(), phrases));
        area.setRows(Math.min(10, Math.max(4, phrases.size())));
        return AppTheme.hudScrollPane(area);
    }

    private JTextArea readOnlyTextArea(String text) {
        JTextArea area = AppTheme.makeTextArea(0, 0);
        area.setText(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(8, 8, 8, 8));
        return area;
    }

    private JLabel detailLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(AppTheme.FG_MUTED);
        label.setMinimumSize(new Dimension(220, 24));
        label.setPreferredSize(new Dimension(220, 24));
        return label;
    }

    private JPanel buttons() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftButtons.setOpaque(false);

        JButton run = runButton();
        run.addActionListener(event -> runCommand());
        leftButtons.add(run);

        panel.add(leftButtons, BorderLayout.WEST);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightButtons.setOpaque(false);
        if (editAction != null) {
            JButton edit = AppTheme.makeButtonSubtle(getText("actions.customCommands.action.edit"));
            edit.addActionListener(event -> runAfterClose(editAction));
            rightButtons.add(edit);
        }
        if (deleteAction != null) {
            JButton delete = AppTheme.makeButtonSubtle(getText("actions.customCommands.action.delete"));
            delete.addActionListener(event -> runAfterClose(deleteAction));
            rightButtons.add(delete);
        }

        JButton close = AppTheme.makeButton(getText("actions.commands.details.close"));
        close.addActionListener(event -> dispose());
        rightButtons.add(close);
        panel.add(rightButtons, BorderLayout.EAST);
        getRootPane().setDefaultButton(close);
        return panel;
    }

    private void runAfterClose(Runnable action) {
        dispose();
        action.run();
    }

    private JButton runButton() {
        JButton button = AppTheme.makeButton(getText("actions.commands.details.run"));
        button.setIcon(new PlayIcon());
        button.setIconTextGap(8);
        return button;
    }

    private void runCommand() {
        JsonObject params = promptForParams();
        if (params == null) {
            return;
        }

        GuiCommandRunner.runAfterClosingWindow(this, entry.id(), params, !entry.isCustomCommand());
    }

    private void openPhraseCorrectionDialog() {
        new PhraseCorrectionSuggestionDialog(this, entry, currentPhrases()).showDialog();
    }

    private List<String> currentPhrases() {
        return phrases;
    }

    private JsonObject promptForParams() {
        if (entry.isCustomCommand()) {
            return customCommandParameters.isEmpty() ? new JsonObject() : promptForCustomCommandParams();
        }
        List<CommandParameter> parameters = commandParameters();
        JsonObject params = new JsonObject();
        if (parameters.isEmpty()) {
            return params;
        }

        JPanel panel = AppTheme.transparentPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(AppTheme.HUD_PADDING, AppTheme.HUD_PADDING, AppTheme.HUD_PADDING, AppTheme.HUD_PADDING));
        Map<CommandParameter, JComponent> fields = new LinkedHashMap<>();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (CommandParameter parameter : parameters) {
            gbc.gridx = 0;
            gbc.weightx = 0.0;
            JLabel label = new JLabel(parameter.name());
            label.setForeground(AppTheme.FG);
            panel.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            JComponent field = parameter.isBoolean()
                    ? AppTheme.makeComboBox(new String[]{"", "true", "false"})
                    : AppTheme.makeTextField();
            field.setToolTipText(parameter.hint());
            panel.add(field, gbc);
            fields.put(parameter, field);
            gbc.gridy++;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                getText("actions.commands.details.parameters.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        for (Map.Entry<CommandParameter, JComponent> entry : fields.entrySet()) {
            CommandParameter parameter = entry.getKey();
            JComponent field = entry.getValue();
            if (field instanceof JComboBox<?> comboBox) {
                Object selected = comboBox.getSelectedItem();
                if (selected != null && !selected.toString().isBlank()) {
                    params.addProperty(parameter.name(), Boolean.parseBoolean(selected.toString()));
                }
            } else if (field instanceof JTextField textField) {
                String value = textField.getText().trim();
                if (!value.isBlank()) {
                    params.addProperty(parameter.name(), value);
                }
            }
        }
        return params;
    }

    private JsonObject promptForCustomCommandParams() {
        JPanel panel = AppTheme.transparentPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(AppTheme.HUD_PADDING, AppTheme.HUD_PADDING, AppTheme.HUD_PADDING, AppTheme.HUD_PADDING));
        Map<CustomCommandParameterSpec, JComponent> fields = new LinkedHashMap<>();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (CustomCommandParameterSpec spec : customCommandParameters) {
            gbc.gridx = 0;
            gbc.weightx = 0.0;
            String labelText = spec.getName() + (spec.isRequired() ? " *" : "");
            JLabel label = new JLabel(labelText);
            label.setForeground(AppTheme.FG);
            if (!spec.getDescription().isBlank()) {
                label.setToolTipText(spec.getDescription());
            }
            panel.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            JComponent field = "boolean".equals(spec.getType())
                    ? AppTheme.makeComboBox(new String[]{"", "true", "false"})
                    : AppTheme.makeTextField();
            if (!spec.getExamples().isEmpty()) {
                field.setToolTipText("E.g.: " + String.join(", ", spec.getExamples()));
            }
            panel.add(field, gbc);
            fields.put(spec, field);
            gbc.gridy++;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                getText("actions.commands.details.parameters.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        JsonObject params = new JsonObject();
        for (Map.Entry<CustomCommandParameterSpec, JComponent> e : fields.entrySet()) {
            CustomCommandParameterSpec spec = e.getKey();
            JComponent field = e.getValue();
            if (field instanceof JComboBox<?> combo) {
                Object selected = combo.getSelectedItem();
                if (selected != null && !selected.toString().isBlank()) {
                    params.addProperty(spec.getName(), Boolean.parseBoolean(selected.toString()));
                }
            } else if (field instanceof JTextField textField) {
                String value = textField.getText().trim();
                if (!value.isBlank()) {
                    if ("number".equals(spec.getType())) {
                        try {
                            params.addProperty(spec.getName(), Double.parseDouble(value));
                        } catch (NumberFormatException ex) {
                            params.addProperty(spec.getName(), value);
                        }
                    } else {
                        params.addProperty(spec.getName(), value);
                    }
                }
            }
        }
        return params;
    }

    private List<CommandParameter> commandParameters() {
        Map<String, CommandParameter> parameters = new LinkedHashMap<>();
        AiActionsMap.getInstance().actionMap(true).forEach((phraseGroup, action) -> {
            if (!entry.id().equalsIgnoreCase(action)) {
                return;
            }

            // Command aliases are the existing source of truth for LLM parameter names.
            Matcher matcher = PARAMETER_BLOCK.matcher(phraseGroup);
            while (matcher.find()) {
                for (String rawParameter : matcher.group(1).split(",")) {
                    CommandParameter parameter = CommandParameter.from(rawParameter);
                    if (parameter != null) {
                        parameters.putIfAbsent(parameter.name(), parameter);
                    }
                }
            }
        });
        return List.copyOf(parameters.values());
    }

    private String readableType(CommandCatalogEntryType type) {
        return switch (type) {
            case BUILT_IN_BINDING -> getText("actions.commands.type.builtInBinding");
            case BUILT_IN_ACTION -> getText("actions.commands.type.builtInAction");
            case CUSTOM_COMMAND -> getText("actions.commands.type.customCommand");
        };
    }

    private static String dialogTitle(CommandCatalogEntry entry) {
        return entry != null && entry.isCustomCommand()
                ? getText("actions.customCommands.details.title")
                : getText("actions.commands.details.title");
    }

    private record CommandParameter(String name, String hint) {
        private boolean isBoolean() {
            return "true/false".equalsIgnoreCase(hint);
        }

        private static CommandParameter from(String rawParameter) {
            if (rawParameter == null || rawParameter.isBlank()) {
                return null;
            }
            String[] parts = rawParameter.trim().split(":", 2);
            String name = parts[0].trim();
            if (name.isBlank()) {
                return null;
            }
            String hint = parts.length > 1 ? parts[1].trim() : "X";
            return new CommandParameter(name, hint);
        }
    }

    private static final class PlayIcon implements Icon {
        private static final int WIDTH = 11;
        private static final int HEIGHT = 12;

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BUTTON_FG);
                Polygon triangle = new Polygon(
                        new int[]{x, x, x + WIDTH},
                        new int[]{y, y + HEIGHT, y + HEIGHT / 2},
                        3
                );
                g2.fill(triangle);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return WIDTH;
        }

        @Override
        public int getIconHeight() {
            return HEIGHT;
        }
    }
}
