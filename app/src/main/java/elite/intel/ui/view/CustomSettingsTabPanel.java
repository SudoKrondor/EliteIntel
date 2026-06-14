package elite.intel.ui.view;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.LanguageChangedEvent;
import elite.intel.util.StringUtls;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.view.AppTheme.*;

public class CustomSettingsTabPanel extends JPanel {

    private final SystemSession systemSession = SystemSession.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    private HudComboBox<LanguageOption> languageCombo;
    private JCheckBox conversationModeCheckBox;
    private JTextField journalDirField;

    public CustomSettingsTabPanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_BG);

        HudSection inputSection = new HudSection(getText("player.section.inputSettings"), new GridBagLayout());
        JPanel input = inputSection.body();
        GridBagConstraints inputGbc = baseGbc();
        addLabel(input, getText("player.commandLanguage"), inputGbc);
        languageCombo = makeLanguageCombo(systemSession.getLanguage());
        languageCombo.setToolTipText(getText("player.commandLanguage.tooltip"));
        languageCombo.addActionListener(e -> {
            LanguageOption selected = (LanguageOption) languageCombo.getSelectedItem();
            if (selected == null) return;
            Language language = selected.language();
            if (language == systemSession.getLanguage()) return;
            systemSession.setLanguage(language);
            EventBusManager.publish(new LanguageChangedEvent());
            SwingUtilities.invokeLater(() -> EventBusManager.publish(new MissionCriticalAnnouncementEvent(
                    StringUtls.localizedSpeech("speech.languageChanged", StringUtls.localizedSpeechLanguageName(language)))));
        });
        addField(input, languageCombo, inputGbc, 1, 1.0);

        nextRow(inputGbc);
        inputGbc.gridx = 0;
        inputGbc.gridwidth = 2;
        inputGbc.weightx = 1;
        inputGbc.fill = GridBagConstraints.HORIZONTAL;
        conversationModeCheckBox = makeCheckBox(getText("player.conversationMode"), false);
        conversationModeCheckBox.addActionListener(e -> systemSession.setConversationalMode(conversationModeCheckBox.isSelected()));
        input.add(conversationModeCheckBox, inputGbc);

        HudSection journalSection = new HudSection(getText("player.journalDirectory"), new GridBagLayout());
        JPanel journal = journalSection.body();
        GridBagConstraints jgbc = baseGbc();
        addLabel(journal, getText("player.journalDirectory"), jgbc);
        journalDirField = makeTextField();
        journalDirField.setEditable(false);
        int fieldHeight = 42;
        journalDirField.setPreferredSize(new Dimension(200, fieldHeight));
        journalDirField.setToolTipText(getText("player.journalDirectory.tooltip"));
        addField(journal, journalDirField, jgbc, 1, 0.8);
        JButton selectJournalDirButton = makeFieldButton(verticalEllipsisIcon(fieldHeight), fieldHeight);
        selectJournalDirButton.setToolTipText(getText("button.select"));
        selectJournalDirButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle(getText("player.journalDirectory.dialog"));
            String current = playerSession.getJournalPath().toString();
            if (!current.isBlank()) chooser.setCurrentDirectory(new File(current).getParentFile());
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                String path = chooser.getSelectedFile().getAbsolutePath();
                playerSession.setJournalPath(path);
                journalDirField.setText(path);
                EventBusManager.publish(new AppLogEvent("Journal directory updated"));
            }
        });
        // Compact square picker — keep fixed size, do not stretch like a normal field.
        jgbc.gridx = 2;
        jgbc.weightx = 0;
        jgbc.fill = GridBagConstraints.NONE;
        journal.add(selectJournalDirButton, jgbc);

        JPanel content = transparentPanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(inputSection);
        content.add(Box.createVerticalStrut(HUD_GAP));
        content.add(journalSection);

        add(content, BorderLayout.NORTH);
    }

    public void initData() {
        conversationModeCheckBox.setSelected(systemSession.conversationalModeOn());
        selectLanguage(systemSession.getLanguage());
        journalDirField.setText(playerSession.getJournalPath().toString());
    }

    private HudComboBox<LanguageOption> makeLanguageCombo(Language selected) {
        HudComboBox<LanguageOption> combo = new HudComboBox<>(new LanguageOption[]{
                new LanguageOption(getText("language.english"), Language.EN),
                new LanguageOption(getText("language.russian"), Language.RU),
                new LanguageOption(getText("language.ukrainian"), Language.UK),
                new LanguageOption(getText("language.german"), Language.DE),
                new LanguageOption(getText("language.french"), Language.FR),
                new LanguageOption(getText("language.spanish"), Language.ES)
        });
        selectLanguage(combo, selected);
        return combo;
    }

    private void selectLanguage(Language language) {
        selectLanguage(languageCombo, language);
    }

    private void selectLanguage(HudComboBox<LanguageOption> combo, Language language) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            LanguageOption option = combo.getItemAt(i);
            if (option.language() == language) {
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
