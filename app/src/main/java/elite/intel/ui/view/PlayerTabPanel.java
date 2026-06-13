package elite.intel.ui.view;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.ShipCadence;
import elite.intel.ai.brain.ShipPersonality;
import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.ai.mouth.subscribers.events.AiVoxDemoEvent;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.db.managers.ShipSettingsManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.TTSProviderChangedEvent;
import elite.intel.ui.view.settings.SettingsPopup;
import elite.intel.ui.view.settings.ShipSettingsPopup;
import elite.intel.util.StringUtls;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.view.AppTheme.*;

public class PlayerTabPanel extends JPanel {

    private final PlayerSession playerSession = PlayerSession.getInstance();

    private JTextField playerAltNameField;
    private JTable fleetTable;
    private FleetTableModel fleetTableModel;

    public PlayerTabPanel() {
        buildUi();
        EventBusManager.register(this);
    }

    @Subscribe
    public void onTTSProviderChanged(TTSProviderChangedEvent event) {
        SwingUtilities.invokeLater(this::initData);
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_BG);
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

        content.add(shipOptionsSection);
        content.add(Box.createVerticalStrut(HUD_GAP));

        HudSection fleetSection = HudSection.flat(getText("player.section.fleetVoice"), new BorderLayout());

        fleetTableModel = new FleetTableModel(playerSession);
        fleetTable = new JTable(fleetTableModel);
        HudTable.style(fleetTable);
        fleetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        fleetTable.getColumnModel().getColumn(0).setCellRenderer(new HudTable.ValueCellRenderer());
        fleetTable.getColumnModel().getColumn(1).setCellRenderer(new ComboColumnRenderer(null));
        fleetTable.getColumnModel().getColumn(2).setCellRenderer(new ComboColumnRenderer("ship.personality."));
        fleetTable.getColumnModel().getColumn(3).setCellRenderer(new ComboColumnRenderer("ship.cadence."));
        fleetTable.getColumnModel().getColumn(4).setCellRenderer(new GearButtonRenderer());
        fleetTable.getColumnModel().getColumn(4).setCellEditor(new GearButtonEditor());

        fleetTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        fleetTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        fleetTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        fleetTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        TableColumn gearCol = fleetTable.getColumnModel().getColumn(4);
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
        fleetTable.getColumnModel().getColumn(1)
                .setCellEditor(new HudComboCellEditor(new HudComboBox<>(voiceOptions)));

        String[] personalityOptions =
                Arrays.stream(ShipPersonality.values()).map(Enum::name).toArray(String[]::new);
        String[] cadenceOptions =
                Arrays.stream(ShipCadence.values()).map(Enum::name).toArray(String[]::new);
        fleetTable.getColumnModel().getColumn(2)
                .setCellEditor(new HudComboCellEditor(new HudComboBox<>(personalityOptions)));
        fleetTable.getColumnModel().getColumn(3)
                .setCellEditor(new HudComboCellEditor(new HudComboBox<>(cadenceOptions)));
    }

    static String displayShipName(ShipDao.Ship ship) {
        String displayName = LoadoutConverter.toDisplayShipName(ship.getShipName(), ship.getShipIdentifier());
        return displayName == null ? getText("player.fleet.unknown") : displayName;
    }

    private void saveCommanderName() {
        playerSession.setAlternativeName(playerAltNameField.getText());
        EventBusManager.publish(new AppLogEvent("Commander name saved"));
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
                    getText("player.fleet.cadence"),
                    ""
            };
        }

        void setShips(List<ShipDao.Ship> ships) {
            this.ships = ships;
        }

        @Override public int getRowCount()    { return ships.size(); }
        @Override public int getColumnCount() { return 5; }
        @Override public String getColumnName(int col) { return columnNames[col]; }

        @Override
        public Class<?> getColumnClass(int col) {
            return col == 4 ? Object.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) { return col >= 1; }

        @Override
        public Object getValueAt(int row, int col) {
            ShipDao.Ship ship = ships.get(row);
            return switch (col) {
                case 0 -> displayShipName(ship);
                case 1 -> ship.getVoice();
                case 2 -> ship.getPersonality();
                case 3 -> ship.getCadence();
                case 4 -> ship;
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            ShipDao.Ship ship = ships.get(row);
            switch (col) {
                case 1 -> {
                    String voiceName = (String) value;
                    ship.setVoice(voiceName);
                    String speakerName = trimToNull(displayShipName(ship));
                    if (speakerName == null) speakerName = voiceName;
                    String tts = StringUtls.shipIntroduction(
                            playerSession.getConfiguredPlayerName(), speakerName);
                    EventBusManager.publish(new AiVoxDemoEvent(tts, voiceName));
                    ShipManager.getInstance().saveShip(ship);
                }
                case 2 -> {
                    ship.setPersonality((String) value);
                    ShipManager.getInstance().saveShip(ship);
                }
                case 3 -> {
                    ship.setCadence((String) value);
                    ShipManager.getInstance().saveShip(ship);
                }
            }
            fireTableCellUpdated(row, col);
        }
    }

    /**
     * Cell renderer for editable combo columns (Voice/Personality/Cadence).
     * Optionally localizes enum values and draws a muted ▼ affordance at the right edge.
     */
    private static final class ComboColumnRenderer extends HudTable.CellRenderer {
        /** null → raw value (Voice); non-null → i18n key prefix (Personality/Cadence). */
        private final String i18nPrefix;
        private boolean selectedRow;
        // Local pixel geometry — not a colour/font/component-height token.
        private static final int ARROW_AREA = 18;

        ComboColumnRenderer(String i18nPrefix) {
            this.i18nPrefix = i18nPrefix;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            this.selectedRow = isSelected;
            // Fully qualify to avoid shadowing by the inherited JLabel.getText() no-arg method.
            Object display = (i18nPrefix != null && value != null)
                    ? elite.intel.ui.i18n.MultiLingualTextProvider
                    .getText(i18nPrefix + ((String) value).toLowerCase(Locale.ROOT))
                    .toUpperCase(Locale.ROOT)
                    : value;
            super.getTableCellRendererComponent(table, display, isSelected, hasFocus, row, col);
            // Restore vpad from super, widen right side to reserve space for ▼.
            int vpad = getVerticalPadding();
            setBorder(new EmptyBorder(vpad, 8, vpad, ARROW_AREA));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Color arrow = selectedRow ? AppTheme.SEL_FG : AppTheme.HUD_ORANGE_SOFT;
                AppTheme.paintHudArrowDown(g2, getWidth() - ARROW_AREA, 0, ARROW_AREA - 4, getHeight(), arrow);
            } finally {
                g2.dispose();
            }
        }
    }

    // -------------------------------------------------------------------------

    /** Combo cell editor that keeps HUD_TABLE_ROW background regardless of row selection. */
    private static final class HudComboCellEditor extends DefaultCellEditor {
        HudComboCellEditor(HudComboBox<String> combo) {
            super(combo);
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int col) {
            Component c = super.getTableCellEditorComponent(table, value, isSelected, row, col);
            c.setBackground(AppTheme.HUD_TABLE_ROW); // §3: input field stays warm on any row state
            c.setForeground(AppTheme.FG);
            return c;
        }
    }

    // -------------------------------------------------------------------------

    /** Stamp renderer for the gear settings button column. */
    private static class GearButtonRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new BorderLayout());
        private final JButton gear = new JButton();
        private final ImageIcon gearBase =
                AppTheme.scaledIcon(PlayerTabPanel.class, "/images/settings.png", HUD_ICON_TABLE);
        private ImageIcon gearOrange;
        private ImageIcon gearDark;

        GearButtonRenderer() {
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
                    gearDark = AppTheme.tintIcon(gearBase, HUD_ICON_TABLE, HUD_ICON_TABLE, AppTheme.SEL_FG);
                return gearDark;
            }
            if (gearOrange == null)
                gearOrange = AppTheme.tintIcon(gearBase, HUD_ICON_TABLE, HUD_ICON_TABLE, AppTheme.HUD_ORANGE_SOFT);
            return gearOrange;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            panel.setBackground(isSelected ? ACCENT : HUD_TABLE_ROW);
            panel.setOpaque(true);
            gear.setIcon(gearIcon(isSelected));
            return panel;
        }
    }

    // -------------------------------------------------------------------------

    /** Cell editor that opens {@link ShipSettingsPopup} on a single click. */
    private static class GearButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new BorderLayout());
        private final JButton gear = new JButton();
        private final ImageIcon gearBase =
                AppTheme.scaledIcon(PlayerTabPanel.class, "/images/settings.png", HUD_ICON_TABLE);
        private ImageIcon gearOrange;
        private ImageIcon gearDark;
        private ShipDao.Ship currentShip;
        private JTable ownerTable;

        GearButtonEditor() {
            gear.setOpaque(false);
            gear.setContentAreaFilled(false);
            gear.setBorderPainted(false);
            gear.setFocusPainted(false);
            gear.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(gear, BorderLayout.CENTER);
            gear.addActionListener(e -> {
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

        private ImageIcon gearIcon(boolean selected) {
            if (selected) {
                if (gearDark == null)
                    gearDark = AppTheme.tintIcon(gearBase, HUD_ICON_TABLE, HUD_ICON_TABLE, AppTheme.SEL_FG);
                return gearDark;
            }
            if (gearOrange == null)
                gearOrange = AppTheme.tintIcon(gearBase, HUD_ICON_TABLE, HUD_ICON_TABLE, AppTheme.HUD_ORANGE_SOFT);
            return gearOrange;
        }

        @Override public Object getCellEditorValue() { return currentShip; }

        @Override public boolean isCellEditable(EventObject e) { return true; }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int col) {
            ownerTable = table;
            currentShip = (ShipDao.Ship) value;
            panel.setBackground(isSelected ? ACCENT : HUD_TABLE_ROW);
            panel.setOpaque(true);
            gear.setIcon(gearIcon(isSelected));
            // Defer the click so editCellAt completes before the popup opens and editing stops.
            SwingUtilities.invokeLater(gear::doClick);
            return panel;
        }
    }
}
