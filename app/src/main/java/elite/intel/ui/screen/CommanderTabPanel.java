package elite.intel.ui.screen;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.ShipPersonality;
import elite.intel.ai.mouth.google.GoogleVoiceProvider;
import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.ai.mouth.subscribers.events.AiVoxDemoEvent;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.db.managers.ShipSettingsManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.TTSProviderChangedEvent;
import elite.intel.ui.screen.settings.SettingsPopup;
import elite.intel.ui.screen.settings.ShipSettingsPopup;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudTable;
import elite.intel.util.StringUtls;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudPalette.*;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class CommanderTabPanel extends JPanel {

    private static final int COL_SHIP = 0;
    private static final int COL_SHIP_MAKE = 1;
    private static final int COL_VOICE = 2;
    private static final int COL_PERSONALITY = 3;
    private static final int COL_GEAR = 4;

    /**
     * i18n key prefix for {@link ShipPersonality} labels; single owner for the cell renderer and the dropdown editor.
     */
    private static final String PERSONALITY_I18N_PREFIX = "ship.personality.";
    /**
     * Columns the Ship Options and Announcements toggle grids are laid out across.
     */
    private static final int COLUMN_COUNT = 3;

    /**
     * Maps a {@link ShipPersonality} enum name to its localized, HUD-cased display label.
     */
    private static String personalityLabel(String enumName) {
        return getText(PERSONALITY_I18N_PREFIX + enumName.toLowerCase(Locale.ROOT))
                .toUpperCase(Locale.ROOT);
    }

    /**
     * Maps a stored voice enum name to a readable "DisplayName - accent · quality" label, resolved against the
     * active TTS provider's voices. The accent disambiguates voices that share a display name (e.g. Spanish vs
     * Portuguese "Dora"); the quality tier (HD vs Standard) shows what the selected language actually delivers.
     * Falls back to the raw name when the stored voice is not valid for the active provider (for example after
     * a TTS provider switch).
     */
    private String voiceLabel(String enumName) {
        if (enumName == null) return "";
        try {
            if (SystemSession.getInstance().useLocalTTS()) {
                KokoroVoices v = KokoroVoices.valueOf(enumName);
                return v.getDisplayName() + " - " + v.getDescription();
            }
            GoogleVoices v = GoogleVoices.valueOf(enumName);
            String base = v.getDisplayName() + " - " + googleVoiceDescriptor(v);
            // Quality is filled off the EDT (resolving a voice may query the provider); until then, no tier.
            String quality = voiceQualityLabels.get(enumName);
            return quality == null ? base : base + " · " + quality;
        } catch (IllegalArgumentException e) {
            return enumName;
        }
    }

    /**
     * Fleet-label descriptor for a Google voice. In English the accent (American/British/Australian) tells the
     * voices apart; in any other language every Google voice is synthesized in that language (its Chirp3-HD
     * character), so only the localized gender is shown instead of a misleading English accent.
     */
    private static String googleVoiceDescriptor(GoogleVoices v) {
        if (SystemSession.getInstance().getLanguage() == Language.EN) {
            return v.getDescription();
        }
        return getText(v.isMale() ? "player.fleet.voice.male" : "player.fleet.voice.female");
    }

    /**
     * Recomputes each Google voice's quality tier (HD vs Standard) for the current language off the EDT, since
     * resolving a voice may query the TTS provider (a listVoices round-trip on first use), then repaints the
     * fleet grid so the labels show the tier. Shown only for a non-English cloud voice once the TTS engine has
     * wired its voice lookup: English is uniformly HD (no tier needed), the local (Kokoro) engine already has
     * accurate accent labels, and before the lookup is wired resolution is optimistic (so no tier is shown).
     */
    private void refreshVoiceQualityLabels() {
        voiceQualityLabels.clear();
        if (SystemSession.getInstance().useLocalTTS()
                || SystemSession.getInstance().getLanguage() == Language.EN
                || !GoogleVoiceProvider.getInstance().hasVoiceLookup()) {
            return;
        }
        Thread.ofVirtual().start(() -> {
            GoogleVoiceProvider provider = GoogleVoiceProvider.getInstance();
            for (GoogleVoices v : GoogleVoices.values()) {
                voiceQualityLabels.put(v.name(), qualityLabel(provider.getVoiceParams(v.name()).getName()));
            }
            SwingUtilities.invokeLater(() -> {
                if (fleetTable != null) fleetTable.repaint();
            });
        });
    }

    /** HD when the resolved Google voice is a Chirp voice (Chirp3-HD / Chirp-HD); Standard otherwise. */
    private static String qualityLabel(String googleVoiceName) {
        boolean hd = googleVoiceName != null && googleVoiceName.contains("Chirp");
        return getText(hd ? "player.fleet.voice.hd" : "player.fleet.voice.standard");
    }

    private final PlayerSession playerSession = PlayerSession.getInstance();

    private JTextField playerAltNameField;
    private JCheckBox discoveryAnnouncementBox;
    private JCheckBox routeAnnouncementBox;
    private JCheckBox radarContactAnnouncementBox;
    private JCheckBox miningAnnouncementBox;
    private JCheckBox navigationAnnouncementBox;
    private JCheckBox radioTransmissionBox;
    private JTable fleetTable;
    private FleetTableModel fleetTableModel;
    /** Per-voice quality tier label (enum name -> localized "HD"/"Standard") for the current language, filled off-EDT. */
    private final Map<String, String> voiceQualityLabels = new ConcurrentHashMap<>();

    public CommanderTabPanel() {
        buildUi();
        UiBus.register(this);
    }

    @Subscribe
    public void onTTSProviderChanged(TTSProviderChangedEvent event) {
        SwingUtilities.invokeLater(this::initData);
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        setBorder(hudScreenBorder());

        JPanel content = transparentPanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        HudSection profileSection = HudSection.flat(getText("player.section.commanderProfile"), new GridBagLayout());
        JPanel profile = profileSection.body();
        GridBagConstraints gbc = baseGbc();

        addLabel(profile, getText("player.commanderName"), gbc);
        playerAltNameField = makeTextField();
        playerAltNameField.setToolTipText(getText("player.commanderName.tooltip"));
        addField(profile, playerAltNameField, gbc, 1, 1.0);
        playerAltNameField.addActionListener(e -> saveCommanderName());
        playerAltNameField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { saveCommanderName(); }
        });

        content.add(profileSection);
        content.add(Box.createVerticalStrut(HUD_GAP));

        JTabbedPane optionTabs = AppTheme.makeSectionTabs();
        optionTabs.setTabPlacement(JTabbedPane.TOP);
        optionTabs.addTab(getText("player.tab.shipOptions"), buildShipOptionsTab());
        optionTabs.addTab(getText("player.tab.announcements"), buildAnnouncementsTab());

        content.add(optionTabs);
        content.add(Box.createVerticalStrut(HUD_GAP));

        HudSection fleetSection = HudSection.flat(getText("player.section.fleetVoice"), new BorderLayout());

        fleetTableModel = new FleetTableModel(playerSession);
        fleetTable = new JTable(fleetTableModel);
        HudTable.style(fleetTable);
        fleetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        fleetTable.getColumnModel().getColumn(COL_SHIP).setCellRenderer(new HudTable.ValueCellRenderer());
        fleetTable.getColumnModel().getColumn(COL_SHIP_MAKE).setCellRenderer(new HudTable.ValueCellRenderer());
        fleetTable.getColumnModel().getColumn(COL_VOICE).setCellRenderer(new ComboColumnRenderer(this::voiceLabel));
        fleetTable.getColumnModel().getColumn(COL_PERSONALITY).setCellRenderer(new ComboColumnRenderer(CommanderTabPanel::personalityLabel));
        fleetTable.getColumnModel().getColumn(COL_GEAR).setCellRenderer(new GearButtonRenderer());
        fleetTable.getColumnModel().getColumn(COL_GEAR).setCellEditor(new GearButtonEditor());

        fleetTable.getColumnModel().getColumn(COL_SHIP).setPreferredWidth(200);
        fleetTable.getColumnModel().getColumn(COL_SHIP_MAKE).setPreferredWidth(150);
        fleetTable.getColumnModel().getColumn(COL_VOICE).setPreferredWidth(160);
        fleetTable.getColumnModel().getColumn(COL_PERSONALITY).setPreferredWidth(160);
        TableColumn gearCol = fleetTable.getColumnModel().getColumn(COL_GEAR);
        gearCol.setPreferredWidth(HUD_TABLE_ROW_HEIGHT + 4);
        gearCol.setMaxWidth(HUD_TABLE_ROW_HEIGHT + 10);

        fleetSection.body().add(HudTable.dataPlaneScrollPane(fleetTable), BorderLayout.CENTER);

        add(content, BorderLayout.NORTH);
        add(fleetSection, BorderLayout.CENTER);

        refreshVoiceQualityLabels(); // initial HD/Standard tiers for the fleet voice list
    }

    /**
     * Ship automation toggles, all backed by {@link GlobalSettingsManager}.
     */
    /**
     * Ship automation toggles, backed by {@link GlobalSettingsManager}. Announcements live on their own tab,
     * including the jump-related ones that used to sit in this grid's third column.
     */
    private JPanel buildShipOptionsTab() {
        GlobalSettingsManager mgr = GlobalSettingsManager.getInstance();
        List<JCheckBox> boxes = new ArrayList<>();

        boxes.add(toggle("automation.autoSpeedUpForFtl", mgr.getAutoSpeedUpForFtl(), mgr::setAutoSpeedUpForFtl));
        boxes.add(toggle("automation.autoLightsOffForFtl", mgr.getAutoLightsForFtl(), mgr::setAutoLightsForFtl));
        boxes.add(toggle("automation.autoNightVisionOffForFtl", mgr.getAutoNightVisionOff(), mgr::setAutoNightVisionOffForSrv));
        boxes.add(toggle("automation.autoHardpointsRetractForFtl", mgr.getAutoHardpointsRetractForFtl(), mgr::setAutoHardpointsRetractForFtl));
        boxes.add(toggle("automation.autoLandingGearUpForFtl", mgr.getAutoLandingGearUpForFtl(), mgr::setAutoLandingGearUpForFtl));
        boxes.add(toggle("automation.autoCargoScoopRetractForFtl", mgr.getAutoCargoScoopRetractForFtl(), mgr::setAutoCargoScoopRetractForFtl));
        boxes.add(toggle("automation.autoGearUpOnTakeOff", mgr.getAutoGearUpOnTakeOff(), mgr::setAutoGearUpOnTakeOff));
        boxes.add(toggle("automation.autoExitUiBeforeOpeningAnotherPanel", mgr.getAutoExitUiBeforeOpeningAnotherWindow(), mgr::setAutoExitUiBeforeOpeningAnotherWindow));
        boxes.add(toggle("automation.autoLightsOffForSrvDeployment", mgr.getAutoLightsOffForSrvDeployment(), mgr::setAutoLightsOffForSrvDeployment));

        JCheckBox fighterDocking = toggle("automation.requestFighterDockOnFtl",
                mgr.getAutoFighterOutFighterDocking(), mgr::setAutoFighterOutFighterDocking);
        fighterDocking.setToolTipText("Disabled until FDev fixes Nomad related bug");
        fighterDocking.setEnabled(false); ///NOTE disabled until FDev fixes their bug
        boxes.add(fighterDocking);

        return threeColumnGrid(boxes);
    }

    /**
     * Every spoken-announcement toggle, in one place.
     * <p>
     * The first six are backed by {@link PlayerSession} and are the categories the
     * {@code toggle_all_announcements} voice command flips, so {@link #initData()} re-reads them: a voice
     * command may have changed one while the tab was not visible. The jump-related ones below them are backed
     * by {@link GlobalSettingsManager}, are read only here, and moved off the Ship Options tab so that a
     * commander looking for an announcement has one place to look.
     */
    private JPanel buildAnnouncementsTab() {
        GlobalSettingsManager mgr = GlobalSettingsManager.getInstance();
        List<JCheckBox> boxes = new ArrayList<>();

        discoveryAnnouncementBox = toggle("announcements.discovery",
                playerSession.isDiscoveryAnnouncementOn(), playerSession::setDiscoveryAnnouncementOn);
        routeAnnouncementBox = toggle("announcements.route",
                playerSession.isRouteAnnouncementOn(), playerSession::setRouteAnnouncementOn);
        radarContactAnnouncementBox = toggle("announcements.radarContact",
                playerSession.isRadarContactAnnouncementOn(), playerSession::setRadarContactAnnouncementOn);
        miningAnnouncementBox = toggle("announcements.mining",
                playerSession.isMiningAnnouncementOn(), playerSession::setMiningAnnouncementOn);
        navigationAnnouncementBox = toggle("announcements.navigation",
                playerSession.isNavigationAnnouncementOn(), playerSession::setNavigationAnnouncementOn);
        radioTransmissionBox = toggle("announcements.radioTransmissions",
                playerSession.isRadioTransmissionOn(), playerSession::setRadioTransmissionOn);

        boxes.add(discoveryAnnouncementBox);
        boxes.add(routeAnnouncementBox);
        boxes.add(radarContactAnnouncementBox);
        boxes.add(miningAnnouncementBox);
        boxes.add(navigationAnnouncementBox);
        boxes.add(radioTransmissionBox);

        boxes.add(toggle("automation.announceJumpRoute", mgr.getAnnounceJumpRoute(), mgr::setAnnounceJumpRoute));
        boxes.add(toggle("automation.announceJumpTraffic", mgr.getAnnounceJumpTraffic(), mgr::setAnnounceJumpTraffic));
        boxes.add(toggle("automation.announceJumpDeaths", mgr.getAnnounceJumpDeaths(), mgr::setAnnounceJumpDeaths));
        boxes.add(toggle("automation.announceRemainingJumps", mgr.getAnnounceRemainingJumps(), mgr::setAnnounceRemainingJumps));
        boxes.add(toggle("automation.announceFuelAvailable", mgr.getAnnounceFuelAvailable(), mgr::setAnnounceFuelAvailable));

        return threeColumnGrid(boxes);
    }

    /**
     * A labelled checkbox that writes straight back to the setting it reads.
     */
    private static JCheckBox toggle(String labelKey, boolean selected, Consumer<Boolean> onChange) {
        JCheckBox box = makeCheckBox(getText(labelKey), selected);
        box.addActionListener(e -> onChange.accept(box.isSelected()));
        return box;
    }

    /**
     * Lays the toggles out in three equal columns, filled top to bottom so reading down a column follows the
     * order they were added. Computing the placement from the list is what keeps the two tabs consistent as
     * toggles are added or moved between them; the columns used to be hand-numbered, which is how five
     * announcements ended up living on the automation tab.
     */
    private static JPanel threeColumnGrid(List<JCheckBox> boxes) {
        JPanel grid = transparentPanel(new GridBagLayout());
        grid.setBorder(new EmptyBorder(HUD_GAP, HUD_GAP, HUD_GAP, HUD_GAP));

        GridBagConstraints gbc = optionGbc();
        int rows = (boxes.size() + COLUMN_COUNT - 1) / COLUMN_COUNT;
        for (int i = 0; i < boxes.size(); i++) {
            gbc.gridx = i / rows;
            gbc.gridy = i % rows;
            grid.add(boxes.get(i), gbc);
        }

        // Filler so the grid keeps its slack on the right rather than stretching the columns across the
        // full width.
        gbc.gridx = COLUMN_COUNT;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        grid.add(Box.createHorizontalGlue(), gbc);

        return grid;
    }

    /**
     * Shared grid geometry for the three-column checkbox grids of both option tabs.
     */
    private static GridBagConstraints optionGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 6, 4, 6);
        return gbc;
    }

    public void initData() {
        playerAltNameField.setText(
                playerSession.getAlternativeName() != null ? playerSession.getAlternativeName() : "");

        // A voice command (toggle_all_announcements and friends) can flip these behind the UI's back.
        discoveryAnnouncementBox.setSelected(playerSession.isDiscoveryAnnouncementOn());
        routeAnnouncementBox.setSelected(playerSession.isRouteAnnouncementOn());
        radarContactAnnouncementBox.setSelected(playerSession.isRadarContactAnnouncementOn());
        miningAnnouncementBox.setSelected(playerSession.isMiningAnnouncementOn());
        navigationAnnouncementBox.setSelected(playerSession.isNavigationAnnouncementOn());
        radioTransmissionBox.setSelected(playerSession.isRadioTransmissionOn());

        String commanderName = playerSession.getInGameName();
        List<ShipDao.Ship> ships = (commanderName != null && !commanderName.isBlank())
                ? ShipManager.getInstance().getShipsForCommander(commanderName)
                : ShipManager.getInstance().getAllShips();
        ships.sort((a, b) -> displayShipName(a).compareToIgnoreCase(displayShipName(b)));

        fleetTableModel.setShips(ships);
        fleetTableModel.fireTableDataChanged();

        // Voice options depend on current TTS provider; rebuild editor on every call. Ship voices are
        // female-only (radio transmissions keep the full voice set), so the male voices are filtered out.
        boolean useLocal = SystemSession.getInstance().useLocalTTS();
        String[] voiceOptions = useLocal
                ? Arrays.stream(KokoroVoices.values()).filter(v -> !v.isMale()).map(Enum::name).toArray(String[]::new)
                : Arrays.stream(GoogleVoices.values()).filter(v -> !v.isMale()).map(Enum::name).toArray(String[]::new);
        // labelFn shows "DisplayName - accent"; getCellEditorValue() still returns the raw enum name to store.
        fleetTable.getColumnModel().getColumn(COL_VOICE)
                .setCellEditor(new HudComboCellEditor(new HudComboBox<>(voiceOptions, this::voiceLabel)));

        String[] personalityOptions =
                Arrays.stream(ShipPersonality.values()).map(Enum::name).toArray(String[]::new);
        // labelFn localizes the dropdown display only; getCellEditorValue() still returns the raw enum name to store.
        fleetTable.getColumnModel().getColumn(COL_PERSONALITY)
                .setCellEditor(new HudComboCellEditor(
                        new HudComboBox<>(personalityOptions, CommanderTabPanel::personalityLabel)));

        refreshVoiceQualityLabels(); // provider/language may have changed; recompute the HD/Standard tiers
    }

    /**
     * Normalizes a stored ship voice to a female voice for the active TTS provider. Ship voices are
     * female-only, so a legacy male (or otherwise invalid) stored voice resolves to the provider's default
     * female — keeping the fleet grid's displayed/selected voice a valid, female dropdown option and in step
     * with what {@link SystemSession#getKokoroVoice()} / {@link SystemSession#getGoogleVoice()} actually speak.
     */
    static String normalizeVoiceToFemale(String voiceName) {
        return SystemSession.getInstance().useLocalTTS()
                ? KokoroVoices.femaleOrDefault(voiceName).name()
                : GoogleVoices.femaleOrDefault(voiceName).name();
    }

    static String displayShipName(ShipDao.Ship ship) {
        String displayName = LoadoutConverter.toDisplayShipName(ship.getShipName(), ship.getShipIdentifier());
        return displayName == null ? getText("player.fleet.unknown") : displayName;
    }

    static String shipMakeName(ShipDao.Ship ship) {
        String resolved = LoadoutConverter.toDisplayShipName(null, ship.getShipIdentifier());
        return resolved != null ? resolved : getText("player.fleet.unknown");
    }

    private void saveCommanderName() {
        String current = playerAltNameField.getText();
        String stored = playerSession.getAlternativeName();
        // The field loses focus on every tab switch; only persist (and log) when the value actually
        // changed, so an untouched name does not spam "Commander name saved" into the log.
        if (current.equals(stored == null ? "" : stored)) return;
        playerSession.setAlternativeName(current);
        UiBus.publish(new AppLogEvent("Commander name saved"));
    }

    // -------------------------------------------------------------------------

    /** Table model for the fleet voice configuration grid. */
    private static class FleetTableModel extends AbstractTableModel {
        private final PlayerSession playerSession;
        private final String[] columnNames;
        private List<ShipDao.Ship> ships = Collections.emptyList();

        FleetTableModel(PlayerSession playerSession) {
            this.playerSession = playerSession;
            columnNames = new String[]{
                    getText("player.fleet.ship"),
                    getText("player.fleet.shipMake"),
                    getText("player.fleet.voice"),
                    getText("player.fleet.personality"),
                    ""
            };
        }

        void setShips(List<ShipDao.Ship> ships) {
            this.ships = ships;
        }

        @Override public int getRowCount()    { return ships.size(); }

        @Override
        public int getColumnCount() {
            return 5;
        }
        @Override public String getColumnName(int col) { return columnNames[col]; }

        @Override
        public Class<?> getColumnClass(int col) {
            return col == COL_GEAR ? Object.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col >= COL_VOICE;
        }

        @Override
        public Object getValueAt(int row, int col) {
            ShipDao.Ship ship = ships.get(row);
            return switch (col) {
                case COL_SHIP -> displayShipName(ship);
                case COL_SHIP_MAKE -> shipMakeName(ship);
                case COL_VOICE -> normalizeVoiceToFemale(ship.getVoice());
                case COL_PERSONALITY -> ship.getPersonality();
                case COL_GEAR -> ship;
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            ShipDao.Ship ship = ships.get(row);
            switch (col) {
                case COL_VOICE -> {
                    String voiceName = (String) value;
                    ship.setVoice(voiceName);
                    String speakerName = trimToNull(displayShipName(ship));
                    if (speakerName == null) speakerName = voiceName;
                    String tts = StringUtls.shipIntroduction(
                            playerSession.getConfiguredPlayerName(), speakerName);
                    GameEventBus.publish(new AiVoxDemoEvent(tts, voiceName));
                    ShipManager.getInstance().saveShip(ship);
                }
                case COL_PERSONALITY -> {
                    ship.setPersonality((String) value);
                    ShipManager.getInstance().saveShip(ship);
                }
            }
            fireTableCellUpdated(row, col);
        }
    }

    /**
     * Cell renderer for editable combo columns (Voice/Personality).
     * Optionally localizes enum values and draws a muted down affordance at the right edge.
     */
    private static final class ComboColumnRenderer extends HudTable.CellRenderer {
        /**
         * Display-text mapper applied to the raw cell value; {@code null} renders the value as-is (Voice).
         * Personality passes {@link CommanderTabPanel#personalityLabel}, the shared owner used by the dropdown editor too.
         */
        private final Function<? super String, String> labelFn;
        private boolean selectedRow;
        // Local pixel geometry - not a colour/font/component-height token.
        private static final int ARROW_AREA = 18;

        ComboColumnRenderer(Function<? super String, String> labelFn) {
            this.labelFn = labelFn;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            this.selectedRow = isSelected;
            Object display = (labelFn != null && value != null) ? labelFn.apply((String) value) : value;
            super.getTableCellRendererComponent(table, display, isSelected, hasFocus, row, col);
            // Restore vpad from super, widen right side to reserve space for down.
            int vpad = getVerticalPadding();
            setBorder(new EmptyBorder(vpad, 8, vpad, ARROW_AREA));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Color arrow = selectedRow ? HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT : HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION;
                HudGlyphs.paintHudArrowDown(g2, getWidth() - ARROW_AREA, 0, ARROW_AREA - 4, getHeight(), arrow);
            } finally {
                g2.dispose();
            }
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Combo cell editor that keeps HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND background regardless of row selection.
     */
    private static final class HudComboCellEditor extends DefaultCellEditor {
        HudComboCellEditor(HudComboBox<String> combo) {
            super(combo);
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int col) {
            Component c = super.getTableCellEditorComponent(table, value, isSelected, row, col);
            c.setBackground(HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND); // section 3: input field stays warm on any row state
            c.setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
            return c;
        }
    }

    // -------------------------------------------------------------------------

    /** Shared appearance of a fleet gear cell (section 6: borderless raster icon + per-row tint).
     *  Composition: the renderer and editor hold a GearCell and delegate the painting to it. */
    private static class GearCell {
        final JPanel panel = new JPanel(new BorderLayout());
        final JButton gear = new JButton();
        private final ImageIcon gearBase =
                HudGlyphs.scaledIcon(CommanderTabPanel.class, "/images/settings.png", HUD_ICON_TABLE);
        private ImageIcon gearOrange;
        private ImageIcon gearDark;

        GearCell() {
            gear.setOpaque(false);
            gear.setContentAreaFilled(false);
            gear.setBorderPainted(false);
            gear.setFocusPainted(false);
            gear.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(gear, BorderLayout.CENTER);
        }

        private ImageIcon gearIcon(boolean selected) {
            if (selected) {
                if (gearDark == null)
                    gearDark = HudGlyphs.tintIcon(gearBase, HUD_ICON_TABLE, HUD_ICON_TABLE, HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT);
                return gearDark;
            }
            if (gearOrange == null)
                gearOrange = HudGlyphs.tintIcon(gearBase, HUD_ICON_TABLE, HUD_ICON_TABLE, HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION);
            return gearOrange;
        }

        /** Applies the cell appearance for the current selection state and returns the ready panel. */
        JPanel apply(boolean isSelected) {
            panel.setBackground(isSelected ? HUD_COLOR_ROLE_PRIMARY_ACTION : HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
            panel.setOpaque(true);
            gear.setIcon(gearIcon(isSelected));
            return panel;
        }
    }

    // -------------------------------------------------------------------------

    /** Stamp renderer for the gear settings button column. */
    private static class GearButtonRenderer implements TableCellRenderer {
        private final GearCell cell = new GearCell();

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            return cell.apply(isSelected);
        }
    }

    // -------------------------------------------------------------------------

    /** Cell editor that opens {@link ShipSettingsPopup} on a single click. */
    private static class GearButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final GearCell cell = new GearCell();
        private ShipDao.Ship currentShip;
        private JTable ownerTable;

        GearButtonEditor() {
            cell.gear.addActionListener(e -> {
                if (currentShip != null) {
                    String identifier = displayShipName(currentShip);
                    ShipSettingsDao.ShipSettings settings =
                            ShipSettingsManager.getInstance().getSettings(currentShip.getShipId());
                    SettingsPopup popup = ShipSettingsPopup.create(ownerTable, identifier, settings);
                    Window owner = SwingUtilities.getWindowAncestor(ownerTable);
                    AppTheme.runWithModalScrim(owner, () -> popup.setVisible(true));
                }
                fireEditingStopped();
            });
        }

        @Override public Object getCellEditorValue() { return currentShip; }

        @Override public boolean isCellEditable(EventObject e) { return true; }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int col) {
            ownerTable = table;
            currentShip = (ShipDao.Ship) value;
            JPanel panel = cell.apply(isSelected);
            // Defer the click so editCellAt completes before the popup opens and editing stops.
            SwingUtilities.invokeLater(cell.gear::doClick);
            return panel;
        }
    }
}
