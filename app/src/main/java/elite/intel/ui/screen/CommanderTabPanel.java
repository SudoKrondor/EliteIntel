package elite.intel.ui.screen;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.ShipPersonality;
import elite.intel.ai.mouth.RadioVoicing;
import elite.intel.ai.mouth.TtsProvider;
import elite.intel.ai.mouth.edge.EdgeVoices;
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
import elite.intel.gameapi.carrier.OurCarriers;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudPalette.*;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class CommanderTabPanel extends JPanel {

    /**
     * The carrier voice column's "no voice picked" entry: traffic control is a different stranger every time,
     * which is what every station on the channel sounds like and what carriers sounded like before this.
     */
    static final String RANDOM_VOICE = "";

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
     * Maps a stored voice enum name to a readable label, resolved against the active TTS provider's voices.
     * Google/Kokoro show "DisplayName - accent · quality": the accent disambiguates voices that share a display
     * name (e.g. Spanish vs Portuguese "Dora"), and the quality tier (HD vs Standard) shows what the selected
     * language actually delivers. Edge shows "DisplayName - accent" using the same friendly descriptor Google
     * uses for the same logical identity (see {@link #edgeVoiceLabel}) — the provider-native ShortName (e.g.
     * "en-US-EmmaMultilingualNeural") is an implementation detail and must never leak into this UI.
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
            if (usesEdgeTts()) {
                return edgeVoiceLabel(enumName);
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
     * <p>
     * Also reused for Edge (see {@link #edgeVoiceLabel}): every {@link EdgeVoices} name intentionally has a
     * {@link GoogleVoices} twin with the same accent, so the two providers show identical descriptors for the
     * same logical identity rather than duplicating the "American female" / "British female" literals. The
     * correspondence is one-way (Google also carries the WaveNet pair) and {@code EdgeVoicesTest} pins it.
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
    private JCheckBox addressMeBox;
    private JCheckBox discoveryAnnouncementBox;
    private JCheckBox routeAnnouncementBox;
    private JCheckBox planetaryApproachAnnouncementBox;
    private JCheckBox radarContactAnnouncementBox;
    private JCheckBox miningAnnouncementBox;
    private JCheckBox navigationAnnouncementBox;
    private JCheckBox radioTransmissionBox;
    private JTable fleetTable;
    private FleetTableModel fleetTableModel;
    /**
     * Voice column for carrier rows: the radio engine's roster, rebuilt whenever the fleet grid reloads.
     */
    private final ComboColumnRenderer carrierVoiceRenderer = new ComboColumnRenderer(this::radioVoiceLabel);
    private final TableCellRenderer emptyCellRenderer = new HudTable.ValueCellRenderer();
    private TableCellEditor carrierVoiceEditor;
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

        // Beside the name, because it decides what becomes of it: with this off the commander is not
        // addressed at all - no name, no rank, no honorific - rather than addressed some other way.
        addressMeBox = toggle("player.addressMe",
                playerSession.isAddressMeOn(), playerSession::setAddressMeOn);
        addressMeBox.setToolTipText(getText("player.addressMe.tooltip"));
        addCheck(profile, addressMeBox, gbc);

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
        // Carrier rows share the grid but not its editors: their voices come from the radio engine's roster,
        // and they have neither a personality nor per-ship settings to open.
        fleetTable = new JTable(fleetTableModel) {
            @Override
            public TableCellRenderer getCellRenderer(int row, int col) {
                if (!fleetTableModel.rowAt(row).isCarrier()) return super.getCellRenderer(row, col);
                return switch (col) {
                    case COL_VOICE -> carrierVoiceRenderer;
                    case COL_PERSONALITY, COL_GEAR -> emptyCellRenderer;
                    default -> super.getCellRenderer(row, col);
                };
            }

            @Override
            public TableCellEditor getCellEditor(int row, int col) {
                if (col == COL_VOICE && carrierVoiceEditor != null && fleetTableModel.rowAt(row).isCarrier()) {
                    return carrierVoiceEditor;
                }
                return super.getCellEditor(row, col);
            }
        };
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
     * The first seven are backed by {@link PlayerSession} and are the categories the
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
        planetaryApproachAnnouncementBox = toggle("announcements.planetaryApproach",
                playerSession.isPlanetaryApproachAnnouncementOn(), playerSession::setPlanetaryApproachAnnouncementOn);
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
        boxes.add(planetaryApproachAnnouncementBox);
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
        addressMeBox.setSelected(playerSession.isAddressMeOn());

        // A voice command (toggle_all_announcements and friends) can flip these behind the UI's back.
        discoveryAnnouncementBox.setSelected(playerSession.isDiscoveryAnnouncementOn());
        routeAnnouncementBox.setSelected(playerSession.isRouteAnnouncementOn());
        planetaryApproachAnnouncementBox.setSelected(playerSession.isPlanetaryApproachAnnouncementOn());
        radarContactAnnouncementBox.setSelected(playerSession.isRadarContactAnnouncementOn());
        miningAnnouncementBox.setSelected(playerSession.isMiningAnnouncementOn());
        navigationAnnouncementBox.setSelected(playerSession.isNavigationAnnouncementOn());
        radioTransmissionBox.setSelected(playerSession.isRadioTransmissionOn());

        String commanderName = playerSession.getInGameName();
        List<ShipDao.Ship> ships = (commanderName != null && !commanderName.isBlank())
                ? ShipManager.getInstance().getShipsForCommander(commanderName)
                : ShipManager.getInstance().getAllShips();
        ships.sort((a, b) -> displayShipName(a).compareToIgnoreCase(displayShipName(b)));

        List<FleetRow> rows = new ArrayList<>(ships.size() + 2);
        ships.forEach(ship -> rows.add(new ShipRow(ship)));
        // The carriers go last rather than into the name sort: there are at most two of them, and a
        // commander looking for their carrier expects it in the same place every time.
        OurCarriers.known().forEach(carrier -> rows.add(new CarrierRow(carrier)));

        fleetTableModel.setRows(rows);
        fleetTableModel.fireTableDataChanged();

        // The radio engine follows the language, so this roster is rebuilt with the rest of the grid.
        carrierVoiceEditor = new HudComboCellEditor(new HudComboBox<>(
                radioVoiceOptions(), this::radioVoiceLabel, RANDOM_VOICE::equals));

        // Voice options depend on current TTS provider; rebuild editor on every call. Every voice the active
        // engine has is offered, male and female alike - the picked voice also decides how the companion
        // speaks of itself (see SystemSession.getVoiceGender()).
        boolean useLocal = SystemSession.getInstance().useLocalTTS();
        String[] voiceOptions;
        if (useLocal) {
            voiceOptions = Arrays.stream(KokoroVoices.values()).map(Enum::name).toArray(String[]::new);
        } else if (usesEdgeTts()) {
            voiceOptions = Arrays.stream(EdgeVoices.values()).map(Enum::name).toArray(String[]::new);
        } else {
            voiceOptions = Arrays.stream(GoogleVoices.values()).map(Enum::name).toArray(String[]::new);
        }
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
     * Normalizes a stored ship voice to a voice of the active TTS provider, keeping its gender: a voice
     * belonging to another engine (or no voice at all) resolves to this provider's default. That keeps the
     * fleet grid's displayed/selected voice a valid dropdown option and in step with what the active provider
     * actually speaks.
     */
    static String normalizeVoice(String voiceName) {
        if (SystemSession.getInstance().useLocalTTS()) {
            return KokoroVoices.voiceOrDefault(voiceName).name();
        }
        if (usesEdgeTts()) {
            return EdgeVoices.voiceOrDefault(voiceName).name();
        }
        return GoogleVoices.voiceOrDefault(voiceName).name();
    }

    /**
     * The voices a carrier's traffic control can be given: the radio engine's roster, not the main mouth's.
     * A transmission is voiced by whichever engine {@code RadioVoicing} names for the commander's language -
     * Kokoro almost everywhere, Edge for the Cyrillic locales - so a Google voice picked here would name a
     * speaker the engine that has to say the line has never heard of.
     */
    private static String[] radioVoiceOptions() {
        return Stream.concat(Stream.of(RANDOM_VOICE), radioVoiceRoster()).toArray(String[]::new);
    }

    /**
     * Every voice the engine that speaks radio currently carries, by enum name.
     */
    private static Stream<String> radioVoiceRoster() {
        return RadioVoicing.engine() == TtsProvider.EDGE
                ? Arrays.stream(EdgeVoices.values()).map(Enum::name)
                : Arrays.stream(KokoroVoices.values()).map(Enum::name);
    }

    /**
     * A carrier's stored voice as the fleet grid must show it - the carrier equivalent of
     * {@link #normalizeVoice}, which does the same job for a ship, against the radio engine's roster rather
     * than the active provider's.
     * <p>
     * No voice stored means the commander never picked one, which is shown as {@code RANDOM_VOICE} and draws a
     * stranger per transmission. A voice the radio engine no longer carries is a different thing: the Kokoro
     * cast is curated by hand and a voice that breaks immersion is removed from it, so a carrier can hold a
     * name that is no longer offered. That is shown as the engine's default, because that is what
     * {@code KokoroTTS.resolveVoiceName} will actually speak it in - the grid must not promise a voice the
     * channel will not use.
     * <p>
     * Showing the stale name instead would be worse than cosmetic: these combos are not editable, and
     * {@code JComboBox.setSelectedItem} silently <em>rejects</em> a value its model does not hold, leaving
     * whatever was selected before - so opening that dropdown and clicking away would reassign the carrier to a
     * voice the commander never picked.
     */
    static String carrierVoiceCell(String stored) {
        if (stored == null || stored.isBlank()) return RANDOM_VOICE;
        if (isRadioVoice(stored)) return stored;
        return RadioVoicing.engine() == TtsProvider.EDGE
                ? EdgeVoices.DEFAULT_VOICE.name()
                : KokoroVoices.DEFAULT_VOICE.name();
    }

    private static boolean isRadioVoice(String voiceName) {
        return voiceName != null && radioVoiceRoster().anyMatch(voiceName::equals);
    }

    /**
     * A carrier voice as a label, resolved against the radio engine rather than the active TTS provider.
     */
    private String radioVoiceLabel(String enumName) {
        if (enumName == null || enumName.isEmpty()) return getText("player.fleet.voice.random");
        try {
            if (RadioVoicing.engine() == TtsProvider.EDGE) return edgeVoiceLabel(enumName);
            KokoroVoices v = KokoroVoices.valueOf(enumName);
            return v.getDisplayName() + " - " + v.getDescription();
        } catch (IllegalArgumentException e) {
            return enumName;
        }
    }

    private static boolean usesEdgeTts() {
        return SystemSession.getInstance().getTtsProvider() == TtsProvider.EDGE;
    }

    /**
     * Fleet-label for an Edge voice: "DisplayName - accent · gender" (e.g. "Mary - American female"), the same
     * friendly format Google already uses for the same logical identity - see {@link #googleVoiceDescriptor}.
     * Every {@link EdgeVoices} name has a {@link GoogleVoices} twin by design (pinned by {@code EdgeVoicesTest}),
     * so the descriptor is looked up from {@link GoogleVoices} rather than duplicating "American female" /
     * "British female" literals in {@link EdgeVoices}. Edge's provider-native ShortName (e.g. "en-US-EmmaMultilingualNeural") is an
     * implementation detail of how the logical name is resolved for synthesis and must never appear in this UI.
     * Accepts either the logical enum name or a legacy ShortName (both resolve via {@link EdgeVoices#find}), so a
     * stored value in either form still renders as just the friendly logical label.
     */
    static String edgeVoiceLabel(String enumName) {
        EdgeVoices voice = EdgeVoices.find(enumName);
        if (voice == null) return enumName;
        return voice.displayName() + " - " + googleVoiceDescriptor(GoogleVoices.valueOf(voice.name()));
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

    /**
     * One line of the fleet grid. Ships and carriers share the voice column and nothing else: a carrier is
     * not a companion, so it has no personality and no per-ship settings, only the voice its traffic control
     * answers on. See {@link CarrierRow}.
     */
    private interface FleetRow {
        String name();

        /**
         * What kind of thing this is, for the second column: the ship's make, or which carrier it is.
         */
        String kind();

        String voice();

        /**
         * Stores the picked voice and returns the line to audition it with.
         */
        String applyVoice(String voiceName, String commanderName);

        default boolean isCarrier() {
            return false;
        }

        default ShipDao.Ship ship() {
            return null;
        }
    }

    private record ShipRow(ShipDao.Ship ship) implements FleetRow {
        @Override
        public String name() {
            return displayShipName(ship);
        }

        @Override
        public String kind() {
            return shipMakeName(ship);
        }

        @Override
        public String voice() {
            return normalizeVoice(ship.getVoice());
        }

        @Override
        public String applyVoice(String voiceName, String commanderName) {
            ship.setVoice(voiceName);
            ShipManager.getInstance().saveShip(ship);
            String speakerName = trimToNull(name());
            return StringUtls.shipIntroduction(commanderName, speakerName == null ? voiceName : speakerName);
        }
    }

    /**
     * A fleet or squadron carrier. Its voice is a radio-engine voice, not a ship voice: traffic control comes
     * over the comms channel, which the main mouth never speaks on (see {@code RadioVoicing}). The stored
     * value is null when the commander has not picked one, which is what draws a stranger at random - the
     * behaviour every transmission had before this column existed.
     */
    private record CarrierRow(OurCarriers.Ours carrier) implements FleetRow {
        @Override
        public String name() {
            String carrierName = trimToNull(carrier.data().getCarrierName());
            String callSign = trimToNull(carrier.data().getCallSign());
            if (carrierName == null) return callSign == null ? getText("player.fleet.unknown") : callSign;
            return callSign == null ? carrierName : carrierName + " " + callSign;
        }

        @Override
        public String kind() {
            return getText(carrier.kind() == OurCarriers.Kind.FLEET
                    ? "player.fleet.fleetCarrier"
                    : "player.fleet.squadronCarrier");
        }

        @Override
        public String voice() {
            return carrierVoiceCell(carrier.data().getVoice());
        }

        @Override
        public String applyVoice(String voiceName, String commanderName) {
            boolean random = RANDOM_VOICE.equals(voiceName);
            carrier.update(data -> data.setVoice(random ? null : voiceName));
            return random
                    ? null
                    : StringUtls.carrierTrafficControl(commanderName, carrier.data().getCarrierName());
        }

        @Override
        public boolean isCarrier() {
            return true;
        }
    }

    /** Table model for the fleet voice configuration grid. */
    private static class FleetTableModel extends AbstractTableModel {
        private final PlayerSession playerSession;
        private final String[] columnNames;
        private List<FleetRow> rows = Collections.emptyList();

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

        void setRows(List<FleetRow> rows) {
            this.rows = rows;
        }

        FleetRow rowAt(int row) {
            return rows.get(row);
        }

        @Override public int getRowCount()    { return rows.size(); }

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
            if (col < COL_VOICE) return false;
            // A carrier has a voice and nothing else to configure here.
            return col == COL_VOICE || !rows.get(row).isCarrier();
        }

        @Override
        public Object getValueAt(int row, int col) {
            FleetRow fleetRow = rows.get(row);
            return switch (col) {
                case COL_SHIP -> fleetRow.name();
                case COL_SHIP_MAKE -> fleetRow.kind();
                case COL_VOICE -> fleetRow.voice();
                case COL_PERSONALITY -> fleetRow.isCarrier() ? "" : fleetRow.ship().getPersonality();
                case COL_GEAR -> fleetRow.ship();
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            FleetRow fleetRow = rows.get(row);
            switch (col) {
                case COL_VOICE -> {
                    String voiceName = (String) value;
                    String audition = fleetRow.applyVoice(voiceName, playerSession.getConfiguredPlayerName());
                    // A carrier is auditioned over the radio, because that is the only way it is ever heard.
                    if (audition != null) {
                        GameEventBus.publish(new AiVoxDemoEvent(audition, voiceName, fleetRow.isCarrier()));
                    }
                }
                case COL_PERSONALITY -> {
                    if (fleetRow.isCarrier()) return;
                    fleetRow.ship().setPersonality((String) value);
                    ShipManager.getInstance().saveShip(fleetRow.ship());
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
