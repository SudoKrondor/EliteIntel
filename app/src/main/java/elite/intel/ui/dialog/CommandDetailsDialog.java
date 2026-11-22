package elite.intel.ui.dialog;

import elite.intel.ai.brain.actions.catalog.CommandCatalogEntry;
import elite.intel.ai.brain.actions.catalog.CommandCatalogEntryType;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudSection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Displays command metadata. Commands run by voice only, so nothing here executes one.
 */
public final class CommandDetailsDialog extends JDialog {

    /**
     * What the Edit, Duplicate and Delete buttons do on a custom command's details, owned by the custom command
     * list panel. Named so a call site cannot wire one button to another's action by argument order.
     */
    public record CustomCommandActions(Runnable edit, Runnable duplicate, Runnable delete) {
        public CustomCommandActions {
            Objects.requireNonNull(edit, "edit");
            Objects.requireNonNull(duplicate, "duplicate");
            Objects.requireNonNull(delete, "delete");
        }
    }

    private final CommandCatalogEntry entry;
    private final List<String> phrases;
    private final boolean showPhraseCorrection;
    private final String sequenceText;
    /**
     * {@code null} for entries that are not custom commands: no Edit, Duplicate or Delete buttons.
     */
    private final CustomCommandActions customCommandActions;

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
        this(parent, entry, phrases, showPhraseCorrection, sequenceText, null);
    }

    /**
     * Creates a customCommand details dialog; {@code customCommandActions} adds its Edit, Duplicate and Delete
     * buttons, or {@code null} shows none.
     */
    public CommandDetailsDialog(
            Component parent,
            CommandCatalogEntry entry,
            List<String> phrases,
            boolean showPhraseCorrection,
            String sequenceText,
            CustomCommandActions customCommandActions
    ) {
        super(SwingUtilities.getWindowAncestor(parent), dialogTitle(entry), ModalityType.APPLICATION_MODAL);
        this.entry = Objects.requireNonNull(entry, "entry");
        this.phrases = phrases == null ? List.of() : List.copyOf(phrases);
        this.showPhraseCorrection = showPhraseCorrection;
        this.sequenceText = sequenceText == null ? "" : sequenceText;
        this.customCommandActions = customCommandActions;
        buildUi();
    }

    public void showDialog() {
        setVisible(true);
    }

    private void buildUi() {
        setUndecorated(true);

        JPanel content = AppTheme.transparentPanel(new BorderLayout(0, HudPalette.HUD_GAP));
        content.add(header(), BorderLayout.NORTH);   // commandTitleBlock(entry.name(), entry.id())

        HudSection detailsSection = HudSection.flat(
                getText("actions.commands.details.section.metadata"), new BorderLayout());
        detailsSection.body().add(details(), BorderLayout.CENTER);
        content.add(detailsSection, BorderLayout.CENTER);

        JButton close = AppTheme.makeButtonSubtle(getText("button.back"));
        close.addActionListener(event -> dispose());

        HudModalSpec.Builder b = HudModalSpec.builder()
                .title(dialogTitle(entry))
                .onClose(this::dispose)
                .body(content)
                .scrollBody(true)
                .dismiss(close);         // left side

        // edit/duplicate/delete are custom-command extras, grouped on the right
        if (customCommandActions != null) {
            b.extra(actionButton("actions.customCommands.action.edit", customCommandActions.edit()));
            b.extra(actionButton("actions.customCommands.action.duplicate", customCommandActions.duplicate()));
            b.extra(actionButton("actions.customCommands.action.delete", customCommandActions.delete()));
        }

        setContentPane(AppTheme.hudModalScaffold(b.build()));

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(close);   // Enter = close
        pack();
        setMinimumSize(new Dimension(720, 520));
        setLocationRelativeTo(getOwner());
    }

    private JPanel header() {
        return AppTheme.commandTitleBlock(entry.name(), entry.id());
    }

    private JPanel details() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 0, 5, HudPalette.HUD_GAP);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        addLabelValue(panel, gbc, getText("actions.commands.details.name"), entry.name(), HudPalette.HUD_COLOR_ROLE_INFORMATION);
        addLabelValue(panel, gbc, getText("actions.commands.details.actionKey"), entry.id(), HudPalette.HUD_COLOR_ROLE_VALUE_TEXT);
        addLabelValue(panel, gbc, getText("actions.commands.details.type"), readableType(entry.type()), HudPalette.HUD_COLOR_ROLE_VALUE_TEXT);
        addDescription(panel, gbc);
        addPhrases(panel, gbc);
        addSequence(panel, gbc);

        gbc.gridx = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createGlue(), gbc);
        return panel;
    }

    private void addLabelValue(JPanel panel, GridBagConstraints gbc,
                               String labelText, String value, Color valueColor) {
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel label = detailLabel(labelText);
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(AppTheme.hudReadoutValue(value, valueColor), gbc);
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
            empty.setForeground(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
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
        area.setCaretColor(HudPalette.HUD_COLOR_ROLE_FIELD_BACKGROUND);
        area.setBorder(AppTheme.hudFieldBorder());
        return area;
    }

    private JLabel detailLabel(String text) {
        return AppTheme.hudReadoutLabel(text);
    }

    private JButton actionButton(String labelKey, Runnable action) {
        JButton button = AppTheme.makeButtonSubtle(getText(labelKey));
        button.addActionListener(event -> runAfterClose(action));
        return button;
    }

    private void runAfterClose(Runnable action) {
        dispose();
        action.run();
    }

    private void openPhraseCorrectionDialog() {
        new PhraseCorrectionSuggestionDialog(this, entry, currentPhrases()).showDialog();
    }

    private List<String> currentPhrases() {
        return phrases;
    }

    private String readableType(CommandCatalogEntryType type) {
        return switch (type) {
            case BUILT_IN_BINDING -> getText("actions.commands.type.builtInBinding");
            case BUILT_IN_ACTION -> getText("actions.commands.type.builtInAction");
            case BUILT_IN_QUERY -> getText("actions.commands.type.builtInQuery");
            case CUSTOM_COMMAND -> getText("actions.commands.type.customCommand");
        };
    }

    private static String dialogTitle(CommandCatalogEntry entry) {
        return entry != null && entry.isCustomCommand()
                ? getText("actions.customCommands.details.title")
                : getText("actions.commands.details.title");
    }
}
