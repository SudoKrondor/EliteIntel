package elite.intel.ui.view;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.ShipCadence;
import elite.intel.ai.brain.ShipPersonality;
import elite.intel.ai.mouth.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.ai.mouth.subscribers.events.AiVoxDemoEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.ShipManager;
import elite.intel.db.managers.ShipSettingsManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.LanguageChangedEvent;
import elite.intel.ui.event.TTSProviderChangedEvent;
import elite.intel.ui.view.settings.GlobalSettingsPopup;
import elite.intel.ui.view.settings.ShipSettingsPopup;
import elite.intel.util.StringUtls;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.view.AppTheme.*;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class PlayerTabPanel extends JPanel {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();

    private JTextField playerAltNameField;
    private JTextField journalDirField;
    private JComboBox<LanguageOption> languageCombo;
    private JScrollPane fleetScrollPane;
    private JCheckBox conversationModeCheckBox;

    public PlayerTabPanel() {
        buildUi();
        EventBusManager.register(this);
    }

    @Subscribe
    public void onTTSProviderChanged(TTSProviderChangedEvent event) {
        SwingUtilities.invokeLater(this::initData);
    }

    private void buildUi() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = baseGbc();

        // Row 0: Commander Name
        addLabel(this, getText("player.commanderName"), gbc);
        playerAltNameField = new JTextField();
        playerAltNameField.setToolTipText(getText("player.commanderName.tooltip"));
        playerAltNameField.setPreferredSize(new Dimension(200, 42));
        addField(this, playerAltNameField, gbc, 1, 1.0);

        // Row 1: Journal Directory
        nextRow(gbc);
        addLabel(this, getText("player.journalDirectory"), gbc);
        journalDirField = new JTextField();
        journalDirField.setEditable(false);
        journalDirField.setPreferredSize(new Dimension(200, 42));
        journalDirField.setToolTipText(getText("player.journalDirectory.tooltip"));
        addField(this, journalDirField, gbc, 1, 0.8);
        JButton selectJournalDirButton = makeButton(getText("button.select"));
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
            }
        });
        addField(this, selectJournalDirButton, gbc, 2, 0.2);


        // Row 3: Command Language
        nextRow(gbc);
        addLabel(this, getText("player.commandLanguage"), gbc);
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
        addField(this, languageCombo, gbc, 1, 1.0);

        // Row 4: Save button
        nextRow(gbc);
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        JButton saveButton = makeButton(getText("button.save"));
        saveButton.addActionListener(e -> savePlayerConfig());
        btns.add(saveButton);

        JButton automationButton = makeButtonSubtle("");
        automationButton.setIcon(scaledIcon("/images/settings.png"));
        automationButton.addActionListener(e -> GlobalSettingsPopup.create(this).setVisible(true));
        btns.add(automationButton);

        conversationModeCheckBox = new JCheckBox(getText("player.conversationMode"));
        conversationModeCheckBox.addActionListener(e -> systemSession.setConversationalMode(conversationModeCheckBox.isSelected()));
        btns.add(conversationModeCheckBox);

        add(btns, gbc);

        // Row 4: Fleet Management header
        nextRow(gbc);
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(16, 6, 4, 6);
        JLabel fleetLabel = new JLabel(getText("player.fleetManagement"));
        fleetLabel.setFont(fleetLabel.getFont().deriveFont(Font.BOLD, 13f));
        fleetLabel.setForeground(ACCENT);
        fleetLabel.setBorder(new MatteBorder(0, 0, 1, 0, BUTTON_BG));
        add(fleetLabel, gbc);

        // Row 5: Fleet grid (scrollable, takes remaining space)
        nextRow(gbc);
        gbc.insets = new Insets(0, 6, 6, 6);
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        fleetScrollPane = new JScrollPane();
        fleetScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        fleetScrollPane.setBackground(BG);
        fleetScrollPane.getViewport().setBackground(BG);
        add(fleetScrollPane, gbc);
    }

    public void initData() {
        playerAltNameField.setText(playerSession.getAlternativeName() != null ? playerSession.getAlternativeName() : "");
        journalDirField.setText(playerSession.getJournalPath().toString());
        conversationModeCheckBox.setSelected(systemSession.conversationalModeOn());
        selectLanguage(systemSession.getLanguage());

        String commanderName = playerSession.getInGameName();
        List<ShipDao.Ship> ships = (commanderName != null && !commanderName.isBlank())
                ? ShipManager.getInstance().getShipsForCommander(commanderName)
                : ShipManager.getInstance().getAllShips();
        ships.sort((a, b) -> {
            String nameA = displayShipName(a);
            String nameB = displayShipName(b);
            return nameA.compareToIgnoreCase(nameB);
        });
        fleetScrollPane.setViewportView(buildFleetGrid(ships));
    }

    private JPanel buildFleetGrid(List<ShipDao.Ship> ships) {
        boolean useLocal = SystemSession.getInstance().useLocalTTS();
        String[] voiceOptions = useLocal
                ? Arrays.stream(KokoroVoices.values()).map(Enum::name).toArray(String[]::new)
                : Arrays.stream(GoogleVoices.values()).map(Enum::name).toArray(String[]::new);
        String[] personalityOptions = Arrays.stream(ShipPersonality.values()).map(Enum::name).toArray(String[]::new);
        String[] cadenceOptions = Arrays.stream(ShipCadence.values()).map(Enum::name).toArray(String[]::new);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        // Header row
        c.gridy = 0;
        c.weightx = 0.30;
        c.gridx = 0;
        panel.add(makeColHeader(getText("player.fleet.ship")), c);
        c.weightx = 0.24;
        c.gridx = 1;
        panel.add(makeColHeader(getText("player.fleet.voice")), c);
        c.weightx = 0.24;
        c.gridx = 2;
        panel.add(makeColHeader(getText("player.fleet.personality")), c);
        c.weightx = 0.14;
        c.gridx = 3;
        panel.add(makeColHeader(getText("player.fleet.cadence")), c);
        c.weightx = 0.08;
        c.gridx = 4;
        panel.add(makeColHeader(""), c);

        // Separator under header
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 5;
        c.insets = new Insets(0, 6, 4, 6);
        JSeparator sep = new JSeparator();
        sep.setForeground(BUTTON_BG);
        panel.add(sep, c);
        c.gridwidth = 1;
        c.insets = new Insets(3, 6, 3, 6);

        // Ship rows
        for (int i = 0; i < ships.size(); i++) {
            ShipDao.Ship ship = ships.get(i);
            ShipSettingsDao.ShipSettings shipSettings = ShipSettingsManager.getInstance().getSettings(ship.getShipId());
            int gridRow = i + 2;

            c.gridy = gridRow;
            c.gridx = 0;
            c.weightx = 0.30;
            String shipDisplayName = displayShipName(ship);
            JLabel nameLabel = new JLabel(shipDisplayName);
            nameLabel.setForeground(FG);
            panel.add(nameLabel, c);

            c.gridx = 1;
            c.weightx = 0.24;
            JComboBox<String> voiceCombo = makeCombo(voiceOptions, ship.getVoice());
            voiceCombo.addActionListener(e -> {
                String voiceName = (String) voiceCombo.getSelectedItem();
                ship.setVoice(voiceName);
                // The preview introduces the ship when named; otherwise it identifies the selected voice model.
                String speakerName = trimToNull(shipDisplayName);
                if (speakerName == null) speakerName = voiceName;
                String tts = StringUtls.shipIntroduction(playerSession.getConfiguredPlayerName(), speakerName);
                EventBusManager.publish(new AiVoxDemoEvent(tts, voiceName));
                ShipManager.getInstance().saveShip(ship);
            });
            panel.add(voiceCombo, c);

            c.gridx = 2;
            c.weightx = 0.24;
            JComboBox<String> personalityCombo = makeCombo(personalityOptions, ship.getPersonality(),
                    value -> getText(enumDisplayKey("ship.personality", value)));
            personalityCombo.addActionListener(e -> {
                ship.setPersonality((String) personalityCombo.getSelectedItem());
                ShipManager.getInstance().saveShip(ship);
            });
            panel.add(personalityCombo, c);

            c.gridx = 3;
            c.weightx = 0.14;
            JComboBox<String> cadenceCombo = makeCombo(cadenceOptions, ship.getCadence(),
                    value -> getText(enumDisplayKey("ship.cadence", value)));
            cadenceCombo.addActionListener(e -> {
                ship.setCadence((String) cadenceCombo.getSelectedItem());
                ShipManager.getInstance().saveShip(ship);
            });
            panel.add(cadenceCombo, c);

            c.gridx = 4;
            c.weightx = 0.08;
            c.fill = GridBagConstraints.NONE;
            JButton shipSettingsBtn = makeButtonSubtle("");
            shipSettingsBtn.setIcon(scaledIcon("/images/settings.png"));
            shipSettingsBtn.addActionListener(e -> ShipSettingsPopup.create(panel, shipDisplayName, shipSettings).setVisible(true));
            panel.add(shipSettingsBtn, c);
            c.fill = GridBagConstraints.HORIZONTAL;
        }

        // Push rows to top
        c.gridy = ships.size() + 2;
        c.gridx = 0;
        c.gridwidth = 5;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(Box.createGlue(), c);

        return panel;
    }

    private String displayShipName(ShipDao.Ship ship) {
        String displayName = LoadoutConverter.toDisplayShipName(ship.getShipName(), ship.getShipIdentifier());
        return displayName == null ? getText("player.fleet.unknown") : displayName;
    }

    private JLabel makeColHeader(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(FG_MUTED);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
        return label;
    }

    private JComboBox<String> makeCombo(String[] options, String selected) {
        return makeCombo(options, selected, Function.identity());
    }

    private JComboBox<String> makeCombo(String[] options, String selected, Function<String, String> displayText) {
        JComboBox<String> combo = new JComboBox<>(options);
        combo.setBackground(BG_PANEL);
        combo.setForeground(FG);
        combo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = (JLabel) new DefaultListCellRenderer()
                    .getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setText(value == null ? "" : displayText.apply(value));
            return label;
        });
        if (selected != null) {
            for (String opt : options) {
                if (opt.equals(selected)) {
                    combo.setSelectedItem(selected);
                    break;
                }
            }
        }
        return combo;
    }

    private String enumDisplayKey(String prefix, String value) {
        return prefix + "." + value.toLowerCase(Locale.ROOT);
    }

    private JComboBox<LanguageOption> makeLanguageCombo(Language selected) {
        JComboBox<LanguageOption> combo = new JComboBox<>(new LanguageOption[]{
                new LanguageOption(getText("language.english"), Language.EN),
                new LanguageOption(getText("language.russian"), Language.RU),
                new LanguageOption(getText("language.ukrainian"), Language.UK),
                new LanguageOption(getText("language.german"), Language.DE),
                new LanguageOption(getText("language.french"), Language.FR),
                new LanguageOption(getText("language.spanish"), Language.ES),
        });
        combo.setBackground(BG_PANEL);
        combo.setForeground(FG);
        selectLanguage(combo, selected);
        return combo;
    }

    private void selectLanguage(Language language) {
        selectLanguage(languageCombo, language);
    }

    private void selectLanguage(JComboBox<LanguageOption> combo, Language language) {
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

    private void savePlayerConfig() {
        playerSession.setAlternativeName(playerAltNameField.getText());
        playerSession.setJournalPath(journalDirField.getText());
        EventBusManager.publish(new AppLogEvent("Player config saved"));
        initData();
    }

    private ImageIcon scaledIcon(String resource) {
        return new ImageIcon(
                new ImageIcon(Objects.requireNonNull(getClass().getResource(resource)))
                        .getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH));
    }
}
