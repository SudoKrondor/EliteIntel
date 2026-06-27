package elite.intel.ui.screen.settings;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.DataDirectoryValidator;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.LanguageChangedEvent;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudSection;
import elite.intel.util.StringUtls;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudGlyphs.verticalEllipsisIcon;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * COMMON settings shown above the SETTINGS sub-tabs (settings shared across all of them):
 * command language and journal directory (left, wider column) plus the conversation-mode toggle
 * (right column). Moved here from the now-removed CUSTOM tab. FLAT section (section 9).
 */
public class CommonSettingsPanel extends JPanel {

    private final SystemSession systemSession = SystemSession.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    private HudComboBox<LanguageOption> languageCombo;
    private JCheckBox conversationModeCheckBox;
    private JCheckBox companionModeCheckBox;
    private JTextField journalDirField;
    /**
     * Conversation-mode state captured before companion mode forced it on, so it can be restored.
     */
    private boolean conversationModeBeforeCompanion;

    public CommonSettingsPanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        HudSection section = HudSection.flat(getText("settings.section.common"), new GridBagLayout());
        JPanel body = section.body();
        int fieldHeight = HUD_FIELD_HEIGHT;

        // Row 0 - command language (wide first column, cols 0-2) + conversation mode (right, col 3).
        GridBagConstraints g = baseGbc();
        addLabel(body, getText("player.commandLanguage"), g);

        languageCombo = makeLanguageCombo(systemSession.getLanguage());
        languageCombo.setToolTipText(getText("player.commandLanguage.tooltip"));
        languageCombo.addActionListener(e -> {
            LanguageOption selected = (LanguageOption) languageCombo.getSelectedItem();
            if (selected == null) return;
            Language language = selected.language();
            if (language == systemSession.getLanguage()) return;
            systemSession.setLanguage(language);
            UiBus.publish(new LanguageChangedEvent());
            SwingUtilities.invokeLater(() -> GameEventBus.publish(new MissionCriticalAnnouncementEvent(
                    StringUtls.localizedSpeech("speech.languageChanged", StringUtls.localizedSpeechLanguageName(language)))));
        });
        g.gridx = 1;
        g.gridy = 0;
        g.gridwidth = 2;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        body.add(languageCombo, g);

        conversationModeCheckBox = makeCheckBox(getText("player.conversationMode"), false);
        conversationModeCheckBox.addActionListener(e ->
                systemSession.setConversationalMode(conversationModeCheckBox.isSelected()));
        GridBagConstraints cg = baseGbc();
        cg.gridx = 3;
        cg.gridy = 0;
        cg.weightx = 0;
        cg.fill = GridBagConstraints.NONE;
        cg.anchor = GridBagConstraints.WEST;
        cg.insets = new Insets(6, HUD_GAP * 3, 6, 6);
        body.add(conversationModeCheckBox, cg);

        // Companion mode toggle, right column under conversation mode (row 1, col 3).
        companionModeCheckBox = makeCheckBox(getText("player.companionMode"), false);
        companionModeCheckBox.addActionListener(e -> {
            systemSession.setCompanionMode(companionModeCheckBox.isSelected());
            applyCompanionModeToConversation(companionModeCheckBox.isSelected());
        });
        GridBagConstraints mg = baseGbc();
        mg.gridx = 3;
        mg.gridy = 1;
        mg.weightx = 0;
        mg.fill = GridBagConstraints.NONE;
        mg.anchor = GridBagConstraints.WEST;
        mg.insets = new Insets(6, HUD_GAP * 3, 6, 6);
        body.add(companionModeCheckBox, mg);

        // Row 1 - journal directory under language (label + field + compact picker).
        GridBagConstraints jg = baseGbc();
        jg.gridy = 1;
        addLabel(body, getText("player.journalDirectory"), jg);

        journalDirField = makeTextField();
        journalDirField.setEditable(false);
        journalDirField.setToolTipText(getText("player.journalDirectory.tooltip"));
        addField(body, journalDirField, jg, 1, 1.0);

        JButton selectJournalDirButton = makeFieldButton(verticalEllipsisIcon(fieldHeight), fieldHeight);
        selectJournalDirButton.setToolTipText(getText("button.select"));
        selectJournalDirButton.addActionListener(e -> chooseJournalDir());
        // Compact square picker - fixed size, do not stretch like a field.
        jg.gridx = 2;
        jg.weightx = 0;
        jg.fill = GridBagConstraints.NONE;
        body.add(selectJournalDirButton, jg);

        add(section, BorderLayout.CENTER);
    }

    /**
     * Companion mode forces conversation mode on and locks the toggle. Turning companion mode off
     * re-enables the toggle and restores the conversation-mode state the user had before companion
     * forced it on, so toggling companion never changes the user's conversation-mode preference.
     */
    private void applyCompanionModeToConversation(boolean companionOn) {
        if (companionOn) {
            conversationModeBeforeCompanion = conversationModeCheckBox.isSelected();
            if (!conversationModeCheckBox.isSelected()) {
                conversationModeCheckBox.setSelected(true);
                systemSession.setConversationalMode(true);
            }
            conversationModeCheckBox.setEnabled(false);
        } else {
            conversationModeCheckBox.setEnabled(true);
            if (conversationModeCheckBox.isSelected() != conversationModeBeforeCompanion) {
                conversationModeCheckBox.setSelected(conversationModeBeforeCompanion);
                systemSession.setConversationalMode(conversationModeBeforeCompanion);
            }
        }
    }

    private void chooseJournalDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(getText("player.journalDirectory.dialog"));
        String current = playerSession.getJournalPath().toString();
        if (!current.isBlank()) chooser.setCurrentDirectory(new File(current).getParentFile());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            playerSession.setJournalPath(path);
            journalDirField.setText(path);
            UiBus.publish(new AppLogEvent("Journal directory updated"));
            DataDirectoryValidator.validateAndWarn(playerSession.getJournalPath(), DataDirectoryValidator.DirectoryKind.JOURNAL);
        }
    }

    public void initData() {
        selectLanguage(systemSession.getLanguage());
        conversationModeCheckBox.setSelected(systemSession.conversationalModeOn());
        companionModeCheckBox.setSelected(systemSession.companionModeOn());
        conversationModeBeforeCompanion = conversationModeCheckBox.isSelected();
        applyCompanionModeToConversation(systemSession.companionModeOn());
        journalDirField.setText(playerSession.getJournalPath().toString());
    }

    private HudComboBox<LanguageOption> makeLanguageCombo(Language selected) {
        HudComboBox<LanguageOption> combo = new HudComboBox<>(new LanguageOption[]{
                new LanguageOption(getText("language.english"), Language.EN),
                new LanguageOption(getText("language.russian"), Language.RU),
                new LanguageOption(getText("language.ukrainian"), Language.UK),
                new LanguageOption(getText("language.german"), Language.DE),
                new LanguageOption(getText("language.french"), Language.FR),
                new LanguageOption(getText("language.spanish"), Language.ES),
                new LanguageOption(getText("language.italian"), Language.IT),
                new LanguageOption(getText("language.portuguese"), Language.PT)
        });
        selectLanguage(combo, selected);
        return combo;
    }

    private void selectLanguage(Language language) {
        selectLanguage(languageCombo, language);
    }

    private void selectLanguage(HudComboBox<LanguageOption> combo, Language language) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).language() == language) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(0);
    }

    private record LanguageOption(String label, Language language) {
        @Override
        public String toString() {
            return label;
        }
    }
}
