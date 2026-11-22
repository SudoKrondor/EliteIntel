package elite.intel.ui.screen.services;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.LanguageChangedEvent;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.util.StringUtls;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeLockCheckBox;
import static elite.intel.ui.theme.AppTheme.transparentPanel;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudGlyphs.scaledIcon;
import static elite.intel.ui.theme.HudGlyphs.tintIcon;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * The command language, above the SERVICES sub-tabs because every service speaks it.
 * The language dropdown is locked until the commander unlocks it, so it cannot be switched by accident.
 */
public class LanguagePanel extends JPanel {

    private final SystemSession systemSession = SystemSession.getInstance();

    private static final String ICON_LANGUAGE = "/images/language.png";

    private HudComboBox<LanguageOption> languageCombo;

    public LanguagePanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new GridBagLayout());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        GridBagConstraints g = baseGbc();
        // The globe stays recognisable whatever language the labels have switched to, so a commander who
        // changed language by accident can still find the way back.
        JLabel languageLabel = addLabel(this, getText("player.commandLanguage"), g);
        languageLabel.setIcon(accentIcon(ICON_LANGUAGE));
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
        JCheckBox languageLockCheck = makeLockCheckBox(true);
        bindLock(languageLockCheck, languageCombo);
        JPanel languageRow = transparentPanel(new BorderLayout(HUD_SEP_W, 0));
        languageRow.add(languageCombo, BorderLayout.CENTER);
        languageRow.add(languageLockCheck, BorderLayout.EAST);
        g.gridx = 1;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        add(languageRow, g);
    }

    private ImageIcon accentIcon(String resource) {
        return tintIcon(scaledIcon(getClass(), resource, HUD_ICON_NAV), HUD_ICON_NAV, HUD_ICON_NAV, HUD_COLOR_ROLE_PRIMARY_ACTION);
    }

    public void initData() {
        selectLanguage(systemSession.getLanguage());
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
