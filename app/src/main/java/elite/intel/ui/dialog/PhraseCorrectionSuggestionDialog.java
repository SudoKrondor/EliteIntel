package elite.intel.ui.dialog;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudSection;

import elite.intel.ai.brain.actions.catalog.CommandCatalogEntry;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.BrowserUtil;
import elite.intel.util.GitHubIssueUrlBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Collects phrase correction suggestions and opens a prefilled GitHub issue in the default browser.
 */
public final class PhraseCorrectionSuggestionDialog extends JDialog {

    private final CommandCatalogEntry entry;
    private final JLabel    languageValue        = AppTheme.hudReadoutValue("", HudPalette.FG);
    private final JTextArea currentPhrasesArea   = makeTextArea(6);
    private final JTextArea suggestedPhrasesArea = makeTextArea(6);
    private final JTextArea commentArea          = makeTextArea(4);

    /**
     * Creates a modal phrase correction dialog prefilled from the selected command entry.
     */
    public PhraseCorrectionSuggestionDialog(Component parent, CommandCatalogEntry entry, List<String> currentPhrases) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        this.entry = entry;
        populate(currentPhrases);
        buildUi();
    }

    /**
     * Displays the phrase correction dialog.
     */
    public void showDialog() {
        setVisible(true);
    }

    private void populate(List<String> currentPhrases) {
        languageValue.setText(languageDisplayName(SystemSession.getInstance().getLanguage()).toUpperCase());
        currentPhrasesArea.setText(currentPhrases.isEmpty() ? "" : String.join(System.lineSeparator(), currentPhrases));
        currentPhrasesArea.setEditable(false);
    }

    private void buildUi() {
        setUndecorated(true);

        JPanel content = AppTheme.transparentPanel(new BorderLayout(0, HudPalette.HUD_GAP));
        content.add(AppTheme.commandTitleBlock(entry.name(), entry.id()), BorderLayout.NORTH);

        HudSection section = HudSection.flat(
                getText("actions.commands.suggest.section.correction"), new BorderLayout());
        section.body().add(formPanel(), BorderLayout.CENTER);
        content.add(section, BorderLayout.CENTER);

        JButton openIssue = AppTheme.makeButton(getText("actions.commands.suggest.openGitHubIssue"));
        openIssue.addActionListener(event -> openGitHubIssue());
        JButton back = AppTheme.makeButtonSubtle(getText("button.back"));
        back.addActionListener(event -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("actions.commands.suggest.title"))
                .onClose(this::dispose)
                .body(content)
                .scrollBody(true)             // long body wrapped in scroll (was manual hudScrollPane)
                .primary(openIssue)           // right side
                .dismiss(back)                // left side
                .build();

        setContentPane(AppTheme.hudModalScaffold(spec));

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(openIssue);
        pack();
        setMinimumSize(new Dimension(760, 620));
        setLocationRelativeTo(getOwner());
    }

    private JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(6, 0, 6, 12);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        addLabelValue(panel, gbc, getText("actions.commands.suggest.language"),   languageValue);
        addArea(panel, gbc, getText("actions.commands.details.phrases"),           currentPhrasesArea);
        addArea(panel, gbc, getText("actions.commands.suggest.suggestedPhrases"), suggestedPhrasesArea);
        addArea(panel, gbc, getText("actions.commands.suggest.comment"),           commentArea);

        gbc.gridx = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createGlue(), gbc);
        return panel;
    }

    private void addLabelValue(JPanel panel, GridBagConstraints gbc,
                               String labelText, JLabel value) {
        gbc.gridx = 0; gbc.weightx = 0.0; gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(detailLabel(labelText), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(value, gbc);
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
        panel.add(AppTheme.hudScrollPane(area), gbc);
        gbc.gridy++;
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
                entry.id(),
                entry.name(),
                languageValue.getText(),
                currentPhrasesArea.getText(),
                suggestedPhrasesArea.getText(),
                commentArea.getText()
        );
    }

    private static JTextArea makeTextArea(int rows) {
        JTextArea area = AppTheme.makeTextArea(rows, 0);
        area.setBorder(AppTheme.hudFieldBorder());
        return area;
    }

    private static JLabel detailLabel(String text) {
        return AppTheme.hudReadoutLabel(text);
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
