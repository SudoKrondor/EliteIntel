package elite.intel.ui.screen;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.ShipPersonality;
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
import java.util.function.Function;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudPalette.*;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class CommanderTabPanel extends JPanel {

    private static final int COL_SHIP = 0;
    private static final int COL_VOICE = 1;
    private static final int COL_PERSONALITY = 2;
    private static final int COL_GEAR = 3;

    /** i18n key prefix for {@link ShipPersonality} labels; single owner for the cell renderer and the dropdown editor. */
    private static final String PERSONALITY_I18N_PREFIX = "ship.personality.";

    /** Maps a {@link ShipPersonality} enum name to its localized, HUD-cased display label. */
    private static String personalityLabel(String enumName) {
        return getText(PERSONALITY_I18N_PREFIX + enumName.toLowerCase(Locale.ROOT))
                .toUpperCase(Locale.ROOT);
    }

    private final PlayerSession playerSession = PlayerSession.getInstance();

    private JTextField playerAltNameField;
    private JTable fleetTable;
    private FleetTableModel fleetTableModel;

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

        GlobalSettingsManager mgr = GlobalSettingsManager.getInstance();
        HudSection shipOptionsSection = HudSection.flat(getText("popup.shipOptions"), new GridBagLayout());
        JPanel shipOptions = shipOptionsSection.body();

        GridBagConstraints sc = new GridBagConstraints();
        sc.fill = GridBagConstraints.HORIZONTAL;
        sc.anchor = GridBagConstraints.WEST;
        sc.weightx = 1.0;
        sc.insets = new Insets(4, 6, 4, 6);

        sc.gridx = 0; sc.gridy = 0;
        JCheckBox cb1 = makeCheckBox(getText("automation.autoSpeedUpForFtl"), mgr.getAutoSpeedUpForFtl());
        cb1.addActionListener(e -> mgr.setAutoSpeedUpForFtl(cb1.isSelected()));
        shipOptions.add(cb1, sc);

        sc.gridy = 1;
        JCheckBox cb2 = makeCheckBox(getText("automation.autoLightsOffForFtl"), mgr.getAutoLightsForFtl());
        cb2.addActionListener(e -> mgr.setAutoLightsForFtl(cb2.isSelected()));
        shipOptions.add(cb2, sc);

        sc.gridy = 2;
        JCheckBox cb3 = makeCheckBox(getText("automation.autoNightVisionOffForFtl"), mgr.getAutoNightVisionOff());
        cb3.addActionListener(e -> mgr.setAutoNightVisionOffForSrv(cb3.isSelected()));
        shipOptions.add(cb3, sc);

        sc.gridy = 3;
        JCheckBox cb4 = makeCheckBox(getText("automation.autoHardpointsRetractForFtl"), mgr.getAutoHardpointsRetractForFtl());
        cb4.addActionListener(e -> mgr.setAutoHardpointsRetractForFtl(cb4.isSelected()));
        shipOptions.add(cb4, sc);

        sc.gridy = 4;
        JCheckBox cb5 = makeCheckBox(getText("automation.autoLandingGearUpForFtl"), mgr.getAutoLandingGearUpForFtl());
        cb5.addActionListener(e -> mgr.setAutoLandingGearUpForFtl(cb5.isSelected()));
        shipOptions.add(cb5, sc);

        sc.gridx = 1; sc.gridy = 0;
        JCheckBox cb6 = makeCheckBox(getText("automation.autoCargoScoopRetractForFtl"), mgr.getAutoCargoScoopRetractForFtl());
        cb6.addActionListener(e -> mgr.setAutoCargoScoopRetractForFtl(cb6.isSelected()));
        shipOptions.add(cb6, sc);

        sc.gridy = 1;
        JCheckBox cb7 = makeCheckBox(getText("automation.autoGearUpOnTakeOff"), mgr.getAutoGearUpOnTakeOff());
        cb7.addActionListener(e -> mgr.setAutoGearUpOnTakeOff(cb7.isSelected()));
        shipOptions.add(cb7, sc);

        sc.gridy = 2;
        JCheckBox cb8 = makeCheckBox(getText("automation.autoExitUiBeforeOpeningAnotherPanel"), mgr.getAutoExitUiBeforeOpeningAnotherWindow());
        cb8.addActionListener(e -> mgr.setAutoExitUiBeforeOpeningAnotherWindow(cb8.isSelected()));
        shipOptions.add(cb8, sc);

        sc.gridy = 3;
        JCheckBox cb9 = makeCheckBox(getText("automation.autoLightsOffForSrvDeployment"), mgr.getAutoLightsOffForSrvDeployment());
        cb9.addActionListener(e -> mgr.setAutoLightsOffForSrvDeployment(cb9.isSelected()));
        shipOptions.add(cb9, sc);

        sc.gridy = 4;
        JCheckBox cb10 = makeCheckBox(getText("automation.requestFighterDockOnFtl"), mgr.getAutoFighterOutFighterDocking());
        cb10.addActionListener(e -> mgr.setAutoFighterOutFighterDocking(cb10.isSelected()));
        shipOptions.add(cb10, sc);

        sc.gridx = 2;
        sc.gridy = 0;
        JCheckBox cb11 = makeCheckBox(getText("automation.announceJumpRoute"), mgr.getAnnounceJumpRoute());
        cb11.addActionListener(e -> mgr.setAnnounceJumpRoute(cb11.isSelected()));
        shipOptions.add(cb11, sc);

        sc.gridy = 1;
        JCheckBox cb12 = makeCheckBox(getText("automation.announceJumpTraffic"), mgr.getAnnounceJumpTraffic());
        cb12.addActionListener(e -> mgr.setAnnounceJumpTraffic(cb12.isSelected()));
        shipOptions.add(cb12, sc);

        sc.gridy = 2;
        JCheckBox cb13 = makeCheckBox(getText("automation.announceJumpDeaths"), mgr.getAnnounceJumpDeaths());
        cb13.addActionListener(e -> mgr.setAnnounceJumpDeaths(cb13.isSelected()));
        shipOptions.add(cb13, sc);

        sc.gridy = 3;
        JCheckBox cb14 = makeCheckBox(getText("automation.announceRemainingJumps"), mgr.getAnnounceRemainingJumps());
        cb14.addActionListener(e -> mgr.setAnnounceRemainingJumps(cb14.isSelected()));
        shipOptions.add(cb14, sc);

        sc.gridy = 4;
        JCheckBox cb15 = makeCheckBox(getText("automation.announceFuelAvailable"), mgr.getAnnounceFuelAvailable());
        cb15.addActionListener(e -> mgr.setAnnounceFuelAvailable(cb15.isSelected()));
        shipOptions.add(cb15, sc);

        content.add(shipOptionsSection);
        content.add(Box.createVerticalStrut(HUD_GAP));

        HudSection fleetSection = HudSection.flat(getText("player.section.fleetVoice"), new BorderLayout());

        fleetTableModel = new FleetTableModel(playerSession);
        fleetTable = new JTable(fleetTableModel);
        HudTable.style(fleetTable);
        fleetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        fleetTable.getColumnModel().getColumn(COL_SHIP).setCellRenderer(new HudTable.ValueCellRenderer());
        fleetTable.getColumnModel().getColumn(COL_VOICE).setCellRenderer(new ComboColumnRenderer(null));
        fleetTable.getColumnModel().getColumn(COL_PERSONALITY).setCellRenderer(new ComboColumnRenderer(CommanderTabPanel::personalityLabel));
        fleetTable.getColumnModel().getColumn(COL_GEAR).setCellRenderer(new GearButtonRenderer());
        fleetTable.getColumnModel().getColumn(COL_GEAR).setCellEditor(new GearButtonEditor());

        fleetTable.getColumnModel().getColumn(COL_SHIP).setPreferredWidth(200);
        fleetTable.getColumnModel().getColumn(COL_VOICE).setPreferredWidth(160);
        fleetTable.getColumnModel().getColumn(COL_PERSONALITY).setPreferredWidth(160);
        TableColumn gearCol = fleetTable.getColumnModel().getColumn(COL_GEAR);
        gearCol.setPreferredWidth(HUD_TABLE_ROW_HEIGHT + 4);
        gearCol.setMaxWidth(HUD_TABLE_ROW_HEIGHT + 10);

        fleetSection.body().add(HudTable.dataPlaneScrollPane(fleetTable), BorderLayout.CENTER);

        add(content, BorderLayout.NORTH);
        add(fleetSection, BorderLayout.CENTER);
    }

    public void initData() {
        playerAltNameField.setText(
                playerSession.getAlternativeName() != null ? playerSession.getAlternativeName() : "");

        String commanderName = playerSession.getInGameName();
        List<ShipDao.Ship> ships = (commanderName != null && !commanderName.isBlank())
                ? ShipManager.getInstance().getShipsForCommander(commanderName)
                : ShipManager.getInstance().getAllShips();
        ships.sort((a, b) -> displayShipName(a).compareToIgnoreCase(displayShipName(b)));

        fleetTableModel.setShips(ships);
        fleetTableModel.fireTableDataChanged();

        // Voice options depend on current TTS provider; rebuild editor on every call.
        boolean useLocal = SystemSession.getInstance().useLocalTTS();
        String[] voiceOptions = useLocal
                ? Arrays.stream(KokoroVoices.values()).map(Enum::name).toArray(String[]::new)
                : Arrays.stream(GoogleVoices.values()).map(Enum::name).toArray(String[]::new);
        fleetTable.getColumnModel().getColumn(COL_VOICE)
                .setCellEditor(new HudComboCellEditor(new HudComboBox<>(voiceOptions)));

        String[] personalityOptions =
                Arrays.stream(ShipPersonality.values()).map(Enum::name).toArray(String[]::new);
        // labelFn localizes the dropdown display only; getCellEditorValue() still returns the raw enum name to store.
        fleetTable.getColumnModel().getColumn(COL_PERSONALITY)
                .setCellEditor(new HudComboCellEditor(
                        new HudComboBox<>(personalityOptions, CommanderTabPanel::personalityLabel)));

    }

    static String displayShipName(ShipDao.Ship ship) {
        String displayName = LoadoutConverter.toDisplayShipName(ship.getShipName(), ship.getShipIdentifier());
        return displayName == null ? getText("player.fleet.unknown") : displayName;
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
            return 4;
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
                case COL_VOICE -> ship.getVoice();
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

    /** Combo cell editor that keeps HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND background regardless of row selection. */
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
