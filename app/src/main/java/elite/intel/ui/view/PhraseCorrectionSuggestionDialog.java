package elite.intel.ui.view;

import elite.intel.ai.brain.actions.catalog.CommandCatalogEntry;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.BrowserUtil;
import elite.intel.util.GitHubIssueUrlBuilder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Collects phrase correction suggestions and opens a prefilled GitHub issue in the default browser.
 */
public final class PhraseCorrectionSuggestionDialog extends JDialog {

    private static final int LABEL_COLUMN_WIDTH = 320;

    private final JTextField commandIdField = new JTextField(36);
    private final JTextField commandNameField = new JTextField(36);
    private final JTextField languageField = new JTextField(36);
    private final JTextArea currentPhrasesArea = textArea(6);
    private final JTextArea suggestedPhrasesArea = textArea(6);
    private final JTextArea commentArea = textArea(4);

    /**
     * Creates a modal phrase correction dialog prefilled from the selected command entry.
     */
    public PhraseCorrectionSuggestionDialog(Component parent, CommandCatalogEntry entry, List<String> currentPhrases) {
        super(
                SwingUtilities.getWindowAncestor(parent),
                getText("actions.commands.suggest.title"),
                ModalityType.APPLICATION_MODAL
        );
        populate(entry, currentPhrases);
        buildUi();
    }

    /**
     * Displays the phrase correction dialog.
     */
    public void showDialog() {
        setVisible(true);
    }

    private void populate(CommandCatalogEntry entry, List<String> currentPhrases) {
        commandIdField.setText(entry.id());
        commandNameField.setText(entry.name());
        languageField.setText(languageDisplayName(SystemSession.getInstance().getLanguage()));
        currentPhrasesArea.setText(currentPhrases.isEmpty() ? "" : String.join(System.lineSeparator(), currentPhrases));
    }

    private void buildUi() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(AppTheme.BG);
        content.setBorder(new EmptyBorder(16, 18, 12, 18));
        content.add(formPanel(), BorderLayout.CENTER);
        content.add(buttonPanel(), BorderLayout.SOUTH);
        setContentPane(content);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(760, 620));
        setLocationRelativeTo(getOwner());
    }

    private JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(AppTheme.BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(6, 0, 6, 12);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        addField(panel, gbc, getText("actions.commands.suggest.commandId"), commandIdField);
        addField(panel, gbc, getText("actions.commands.suggest.commandName"), commandNameField);
        addField(panel, gbc, getText("actions.commands.suggest.language"), languageField);
        addArea(panel, gbc, getText("actions.commands.suggest.currentPhrases"), currentPhrasesArea);
        addArea(panel, gbc, getText("actions.commands.suggest.suggestedPhrases"), suggestedPhrasesArea);
        addArea(panel, gbc, getText("actions.commands.suggest.comment"), commentArea);

        gbc.gridx = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createGlue(), gbc);
        return panel;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, String labelText, JTextField field) {
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(detailLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        styleEditor(field);
        panel.add(field, gbc);
        gbc.gridy++;
    }

    private void addArea(JPanel panel, GridBagConstraints gbc, String labelText, JTextArea area) {
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(detailLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.getViewport().setBackground(AppTheme.BG_PANEL);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.ACCENT, 1, true),
                new EmptyBorder(0, 0, 0, 0)
        ));
        panel.add(scrollPane, gbc);
        gbc.gridy++;
    }

    private JPanel buttonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JButton openIssue = AppTheme.makeButton(getText("actions.commands.suggest.openGitHubIssue"));
        openIssue.addActionListener(event -> openGitHubIssue());
        panel.add(openIssue, BorderLayout.WEST);

        JButton cancel = AppTheme.makeButtonSubtle(getText("button.cancel"));
        cancel.addActionListener(event -> dispose());
        panel.add(cancel, BorderLayout.EAST);
        getRootPane().setDefaultButton(openIssue);
        return panel;
    }

    private void openGitHubIssue() {
        // The browser remains the submission boundary; the app only prepares a prefilled issue URL.
        if (BrowserUtil.openUrl(buildIssueUrl())) {
            dispose();
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    getText("actions.commands.suggest.browserError"),
                    getText("actions.commands.suggest.title"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String buildIssueUrl() {
        return GitHubIssueUrlBuilder.buildPhraseCorrectionIssueUrl(
                commandIdField.getText(),
                commandNameField.getText(),
                languageField.getText(),
                currentPhrasesArea.getText(),
                suggestedPhrasesArea.getText(),
                commentArea.getText()
        );
    }

    private static JTextArea textArea(int rows) {
        JTextArea area = new JTextArea();
        area.setRows(rows);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        styleEditor(area);
        return area;
    }

    private static void styleEditor(JTextComponent component) {
        component.setBackground(AppTheme.BG_PANEL);
        component.setForeground(AppTheme.FG);
        component.setCaretColor(AppTheme.FG);
        component.setBorder(new EmptyBorder(8, 8, 8, 8));
    }

    private JLabel detailLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(AppTheme.FG_MUTED);
        // Localized labels in this form are longer than the command details dialog and need a wider fixed column.
        label.setMinimumSize(new Dimension(LABEL_COLUMN_WIDTH, 24));
        label.setPreferredSize(new Dimension(LABEL_COLUMN_WIDTH, 24));
        return label;
    }

    private static String languageDisplayName(Language language) {
        return switch (language) {
            case EN -> getText("language.english");
            case RU -> getText("language.russian");
            case UK -> getText("language.ukrainian");
            case DE -> getText("language.german");
            case FR -> getText("language.french");
            case ES -> getText("language.spanish");
        };
    }
}
