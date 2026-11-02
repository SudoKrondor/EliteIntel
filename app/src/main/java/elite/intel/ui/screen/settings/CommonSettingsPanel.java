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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudGlyphs.*;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * COMMON settings shown above the SETTINGS sub-tabs (settings shared across all of them):
 * command language and journal directory. Moved here from the now-removed CUSTOM tab. FLAT section (section 9).
 * The language dropdown is locked until the commander unlocks it, so it cannot be switched by accident.
 */
public class CommonSettingsPanel extends JPanel {

    private final SystemSession systemSession = SystemSession.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    private static final String ICON_LANGUAGE = "/images/language-orange.png";
    private static final String ICON_LOCKED = "/images/locked-orange.png";

    private HudComboBox<LanguageOption> languageCombo;
    private JTextField journalDirField;

    public CommonSettingsPanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        HudSection section = HudSection.flat(getText("settings.section.common"), new GridBagLayout());
        JPanel body = section.body();
        int fieldHeight = HUD_FIELD_HEIGHT;

        // Row 0 - command language (wide first column, cols 0-2).
        GridBagConstraints g = baseGbc();
        // The globe stays recognisable whatever language the labels have switched to, so a commander who
        // changed language by accident can still find the way back.
        JLabel languageLabel = addLabel(body, getText("player.commandLanguage"), g);
        languageLabel.setIcon(scaledIcon(getClass(), ICON_LANGUAGE, HUD_ICON_NAV));
        languageLabel.setIconTextGap(HUD_PADDING);

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
        // Locked on every build of the panel, like the API keys: a stray click or scroll cannot switch the
        // language. A change rebuilds the whole UI, so the lock closes again behind it.
        JCheckBox languageLockCheck = makeCheckBox(getText("settings.cloud.locked"), true);
        JLabel lockIcon = makeLockIcon(languageLockCheck);
        bindLock(languageLockCheck, languageCombo);
        JPanel lockCell = transparentPanel(new BorderLayout(HUD_SEP_W, 0));
        lockCell.add(lockIcon, BorderLayout.WEST);
        lockCell.add(languageLockCheck, BorderLayout.CENTER);
        JPanel languageRow = transparentPanel(new BorderLayout(HUD_SEP_W, 0));
        languageRow.add(languageCombo, BorderLayout.CENTER);
        languageRow.add(lockCell, BorderLayout.EAST);
        g.gridx = 1;
        g.gridy = 0;
        g.gridwidth = 2;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        body.add(languageRow, g);

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
     * The lock glyph beside the LOCKED box: full strength while locked, receded while open, and a click on it
     * toggles the box too.
     */
    private JLabel makeLockIcon(JCheckBox lockCheck) {
        ImageIcon locked = scaledIcon(getClass(), ICON_LOCKED, HUD_ICON_NAV);
        ImageIcon open = dimIcon(locked, 0.35f);
        JLabel icon = new JLabel(lockCheck.isSelected() ? locked : open);
        icon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                lockCheck.setSelected(!lockCheck.isSelected());
            }
        });
        lockCheck.addItemListener(e -> icon.setIcon(lockCheck.isSelected() ? locked : open));
        return icon;
    }

    private void chooseJournalDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(getText("player.journalDirectory.dialog"));
        String current = playerSession.getJournalPath().toString();
        if (!current.isBlank()) chooser.setCurrentDirectory(new File(current).getParentFile());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            // A quoted path pasted into the chooser's file-name box comes back appended to the folder on
            // show, quotes and all. Storing that made every later read throw and the window stop opening,
            // so a choice that cannot be used is refused here rather than saved.
            if (!playerSession.setJournalPath(path)) {
                UiBus.publish(new AppLogEvent(getText("player.journalDirectory.unusable")));
                DataDirectoryValidator.warnUnusable(DataDirectoryValidator.DirectoryKind.JOURNAL);
                return;
            }
            journalDirField.setText(playerSession.getJournalPath().toString());
            UiBus.publish(new AppLogEvent("Journal directory updated"));
            DataDirectoryValidator.validateAndWarn(playerSession.getJournalPath(), DataDirectoryValidator.DirectoryKind.JOURNAL);
        }
    }

    public void initData() {
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
                new LanguageOption(getText("language.spanish"), Language.ES),
                new LanguageOption(getText("language.italian"), Language.IT),
                new LanguageOption(getText("language.portuguese"), Language.PT),
                new LanguageOption(getText("language.portugueseBrazilian"), Language.PTBZ)
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
