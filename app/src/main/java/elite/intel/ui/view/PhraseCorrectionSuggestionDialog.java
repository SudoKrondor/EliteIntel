package elite.intel.ui.view;

import elite.intel.ai.brain.actions.catalog.CommandCatalogEntry;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.BrowserUtil;
import elite.intel.util.GitHubIssueUrlBuilder;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Collects phrase correction suggestions and opens a prefilled GitHub issue in the default browser.
 */
public final class PhraseCorrectionSuggestionDialog extends JDialog {

    private final CommandCatalogEntry entry;
    private final JLabel    languageValue        = AppTheme.hudReadoutValue("", AppTheme.FG);
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

        JPanel content = AppTheme.transparentPanel(new BorderLayout(0, AppTheme.HUD_GAP));
        content.setOpaque(true);
        content.setBackground(AppTheme.HUD_DIALOG_BODY);
        content.setBorder(new EmptyBorder(AppTheme.HUD_GAP * 2, AppTheme.HUD_GAP * 2,
                AppTheme.HUD_GAP * 2, AppTheme.HUD_GAP * 2));

        content.add(AppTheme.commandTitleBlock(entry.name(), entry.id()), BorderLayout.NORTH);

        HudSection section = HudSection.flat(getText("actions.commands.suggest.section.correction"), new BorderLayout());
        section.body().add(formPanel(), BorderLayout.CENTER);
        content.add(section, BorderLayout.CENTER);

        JScrollPane scroll = AppTheme.hudScrollPane(content);
        scroll.getViewport().setBackground(AppTheme.HUD_DIALOG_BODY);

        HudDialogHeader header = new HudDialogHeader(getText("actions.commands.suggest.title"), this::dispose);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(AppTheme.HUD_BG);
        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(scroll, BorderLayout.CENTER);
        wrapper.add(buttonPanel(), BorderLayout.SOUTH);

        setContentPane(wrapper);
        getRootPane().setBorder(new LineBorder(AppTheme.HUD_ORANGE_FILL_HOVER, AppTheme.HUD_BORDER_THICKNESS_ACCENT));
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
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

    private JPanel buttonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        int gap = AppTheme.HUD_GAP;
        int th  = AppTheme.HUD_BORDER_THICKNESS;
        AbstractBorder topRule = new AbstractBorder() {
            @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                g.setColor(AppTheme.HUD_ORANGE_FILL_HOVER);
                g.fillRect(x + gap, y, w - gap * 2, th);
            }
            @Override public Insets getBorderInsets(Component c) { return new Insets(th, 0, 0, 0); }
            @Override public Insets getBorderInsets(Component c, Insets i) { i.set(th, 0, 0, 0); return i; }
        };
        panel.setBorder(BorderFactory.createCompoundBorder(
                topRule,
                BorderFactory.createEmptyBorder(gap, gap, gap, gap)));

        JButton openIssue = AppTheme.makeButton(getText("actions.commands.suggest.openGitHubIssue"));
        openIssue.addActionListener(event -> openGitHubIssue());
        panel.add(openIssue, BorderLayout.WEST);

        JButton back = AppTheme.makeButtonSubtle(getText("button.back"));
        back.addActionListener(event -> dispose());
        panel.add(back, BorderLayout.EAST);
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
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
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
