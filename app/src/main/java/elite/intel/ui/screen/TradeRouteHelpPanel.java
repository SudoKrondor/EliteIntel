package elite.intel.ui.screen;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.catalog.CommandCatalog;
import elite.intel.ai.brain.actions.command.builtin.CalculateTradeRouteCommand;
import elite.intel.ai.brain.actions.command.builtin.CancelTradeRouteCommand;
import elite.intel.ai.brain.actions.command.builtin.NavigateToTradeStopCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyzeTradeScheduleQuery;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ShipSettingsManager;
import elite.intel.db.managers.TradeRouteManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.search.spansh.station.marketstation.TradeStopDto;
import elite.intel.search.spansh.traderoute.TradeCommodity;
import elite.intel.search.spansh.traderoute.TradeCommodityInfo;
import elite.intel.session.PlayerSession;
import elite.intel.ui.dialog.CommandDetailsDialog;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.screen.settings.ShipSettingsPopup;
import elite.intel.ui.support.GuiCommandRunner;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudButton;
import elite.intel.ui.widget.HudFooter;
import elite.intel.ui.widget.HudGlyphButton;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudStatusReadout;
import elite.intel.ui.widget.HudTable;
import elite.intel.ui.widget.HudTwoColumns;
import elite.intel.ui.widget.StatusBadge;
import elite.intel.util.ClipboardUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.HUD_TABLE_STYLE_LOCKED;
import static elite.intel.ui.theme.AppTheme.hudSubtabContentBorder;
import static elite.intel.ui.theme.AppTheme.makeFieldButton;
import static elite.intel.ui.theme.AppTheme.makeTextField;
import static elite.intel.ui.theme.AppTheme.runWithModalScrim;
import static elite.intel.ui.theme.AppTheme.transparentPanel;
import static elite.intel.ui.theme.HudPalette.HUD_BORDER_THICKNESS;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_DISABLED;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_SUCCESS;
import static elite.intel.ui.theme.HudPalette.HUD_GAP;
import static elite.intel.ui.theme.HudPalette.HUD_FIELD_HEIGHT;
import static elite.intel.ui.theme.HudPalette.HUD_ICON_TABLE;
import static elite.intel.ui.theme.HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT;
import static elite.intel.ui.theme.HudForms.addField;
import static elite.intel.ui.theme.HudForms.addLabel;
import static elite.intel.ui.theme.HudForms.baseGbc;
import static elite.intel.ui.theme.HudForms.nextRow;

/**
 * Read-only Help surface for the persisted trade route. It presents the live player location,
 * every remaining buy/sell point and the selected point's commodity details, while reusing the
 * existing voice-command handlers for calculate, cancel and navigate actions.
 */
final class TradeRouteHelpPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(TradeRouteHelpPanel.class);
    private static final int REFRESH_INTERVAL_MS = 1_250;
    private static final String ROUTE_CARD = "route";
    private static final String EMPTY_CARD = "empty";
    private static final int ROUTE_SYSTEM_COLUMN = 3;
    private static final int ROUTE_STATION_COLUMN = 4;
    private static final int ROUTE_COPY_AREA_WIDTH = HUD_TABLE_ROW_HEIGHT_COMPACT;
    private static final ImageIcon CURRENT_SHIP_PROFILE_GEAR_BASE =
            HudGlyphs.scaledIcon(TradeRouteHelpPanel.class, "/images/settings.png", HUD_ICON_TABLE);
    private static final ImageIcon CURRENT_SHIP_PROFILE_GEAR_ICON = HudGlyphs.tintIcon(
            CURRENT_SHIP_PROFILE_GEAR_BASE, HUD_ICON_TABLE, HUD_ICON_TABLE, HUD_COLOR_ROLE_SELECTED_TEXT);
    private static final ImageIcon CURRENT_SHIP_PROFILE_GEAR_DISABLED_ICON = HudGlyphs.tintIcon(
            CURRENT_SHIP_PROFILE_GEAR_BASE, HUD_ICON_TABLE, HUD_ICON_TABLE, HUD_COLOR_ROLE_DISABLED);

    private record ReadoutRow(String labelText, JComponent field, JComponent trailingAction) {
    }

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final TradeRouteManager tradeRouteManager = TradeRouteManager.getInstance();
    private final CommandCatalog commandCatalog = new CommandCatalog();

    private final DefaultTableModel routeModel = readOnlyModel(
            "help.trade.column.point",
            "help.trade.column.status",
            "help.trade.column.action",
            "help.trade.column.system",
            "help.trade.column.station",
            "help.trade.column.cargo",
            "help.trade.column.profit");
    private final DefaultTableModel commodityModel = readOnlyModel(
            "help.trade.detail.commodity",
            "help.trade.detail.amount",
            "help.trade.detail.buy",
            "help.trade.detail.supply",
            "help.trade.detail.sell",
            "help.trade.detail.demand",
            "help.trade.detail.unitProfit",
            "help.trade.detail.totalProfit");

    private final JTable routeTable = new JTable(routeModel) {
        @Override
        public String getToolTipText(MouseEvent event) {
            return TradeRouteHelpPanel.this.isRouteCopyHit(event)
                    ? getText("help.trade.copy.tooltip")
                    : null;
        }
    };
    private final JTable commodityTable = new JTable(commodityModel);
    private final CardLayout routeCardLayout = new CardLayout();
    private final JPanel routeCards = transparentPanel(routeCardLayout);
    private final JTextField currentLocationField = makeTextField();
    private final JTextField currentShipField = makeTextField();
    private final JButton currentShipProfileButton =
            makeFieldButton(CURRENT_SHIP_PROFILE_GEAR_ICON, HUD_FIELD_HEIGHT);
    private final JTextField startSystemField = makeTextField(this::showStartSystemInfo);
    private final HudStatusReadout routeStatus = new HudStatusReadout(
            getText("help.trade.status.label"),
            getText("help.trade.status.none"),
            StatusBadge.State.IDLE);
    private final HudButton cancelButton = new HudButton(getText("help.trade.action.cancel"), false);
    private final HudButton navigateButton = new HudButton(getText("help.trade.action.navigate"), false);
    private final HudButton calculateButton = new HudButton(getText("help.trade.action.calculate"), true);
    private final Timer refreshTimer = new Timer(REFRESH_INTERVAL_MS, event -> requestRefresh());

    private HudSection routeSection;
    private RouteViewState state;
    private SwingWorker<RouteViewState, Void> refreshWorker;
    private boolean refreshPending;
    private boolean applyingState;
    private boolean disposed;
    private int copyHoverRow = -1;
    private int copyHoverColumn = -1;
    private int copyPressedRow = -1;
    private int copyPressedColumn = -1;
    private BusyAction busyAction = BusyAction.NONE;

    TradeRouteHelpPanel() {
        buildUi();
        configureActions();
        applyState(buildViewState(List.of(), new CurrentPlace("", "")));
        refreshCurrentShip();
        refreshTimer.setRepeats(true);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || disposed) {
                return;
            }
            if (isShowing()) {
                requestRefresh();
                refreshTimer.start();
            } else {
                refreshTimer.stop();
            }
        });
    }

    private void buildUi() {
        setLayout(new BorderLayout(0, HUD_GAP));
        setOpaque(false);
        setBorder(hudSubtabContentBorder());

        currentLocationField.setEditable(false);
        currentShipField.setEditable(false);
        startSystemField.setEditable(false);
        currentShipProfileButton.setToolTipText(getText("help.trade.field.currentShip.openProfile.tooltip"));
        currentShipProfileButton.addActionListener(event -> openCurrentShipTradeProfile());

        HudSection contextSection = HudSection.compactFlat(
                getText("help.trade.section.context"), new BorderLayout());
        contextSection.body().add(new HudTwoColumns(
                readoutColumn(
                        new ReadoutRow(getText("help.trade.field.currentLocation"), currentLocationField, null),
                        new ReadoutRow(getText("help.trade.field.currentShip"), currentShipField,
                                currentShipProfileButton)),
                readoutColumn(new ReadoutRow(getText("help.trade.field.startSystem"), startSystemField, null))),
                BorderLayout.CENTER);

        configureRouteTable();
        JScrollPane routeScroll = HudTable.dataPlaneScrollPane(routeTable);
        routeCards.add(routeScroll, ROUTE_CARD);
        JPanel emptyRoute = transparentPanel(new BorderLayout());
        emptyRoute.add(HudBanner.multiline(
                getText("help.trade.empty"), StatusBadge.State.INFO), BorderLayout.NORTH);
        routeCards.add(emptyRoute, EMPTY_CARD);

        routeSection = HudSection.compactFlat(
                getText("help.trade.section.route"), new BorderLayout());
        routeSection.body().add(routeCards, BorderLayout.CENTER);
        routeSection.setHeaderActions(new HudGlyphButton(
                HudGlyphs::paintHudInfoGlyph,
                HUD_COLOR_ROLE_CONTROL_DECORATION,
                HUD_COLOR_ROLE_PRIMARY_ACTION,
                getText("help.trade.info.tooltip"),
                commandInfo(this, AnalyzeTradeScheduleQuery.ID)));

        configureCommodityTable();
        HudSection detailSection = HudSection.compactFlat(
                getText("help.trade.section.details"), new BorderLayout());
        detailSection.body().add(HudTable.dataPlaneScrollPane(commodityTable), BorderLayout.CENTER);

        JPanel content = transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets.bottom = HUD_GAP;
        content.add(contextSection, gbc);

        gbc.gridy++;
        gbc.weighty = 0.58;
        gbc.fill = GridBagConstraints.BOTH;
        content.add(routeSection, gbc);

        gbc.gridy++;
        gbc.weighty = 0.42;
        gbc.insets.top = HUD_GAP;
        gbc.insets.bottom = 0;
        content.add(detailSection, gbc);
        add(content, BorderLayout.CENTER);

        add(HudFooter.build(
                false,
                null,
                routeStatus,
                List.of(cancelButton, navigateButton, calculateButton)), BorderLayout.SOUTH);
    }

    private JPanel readoutColumn(ReadoutRow... rows) {
        JPanel content = transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = baseGbc();
        for (ReadoutRow row : rows) {
            addLabel(content, row.labelText(), gbc, 0);
            addField(content, row.field(), gbc, 1, 1.0);
            if (row.trailingAction() != null) {
                gbc.gridx = 2;
                gbc.weightx = 0;
                gbc.fill = GridBagConstraints.NONE;
                content.add(row.trailingAction(), gbc);
            }
            nextRow(gbc);
        }

        JPanel topAligned = transparentPanel(new BorderLayout());
        topAligned.add(content, BorderLayout.NORTH);
        return topAligned;
    }

    private void configureRouteTable() {
        routeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        routeTable.setAutoCreateRowSorter(false);
        routeTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !applyingState) {
                updateCommodityDetails();
            }
        });
        styleRouteTable();
        installRouteValueCopying();
        routeTable.getColumnModel().getColumn(0).setMaxWidth(52);
        routeTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        routeTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        routeTable.getColumnModel().getColumn(ROUTE_SYSTEM_COLUMN).setPreferredWidth(180);
        routeTable.getColumnModel().getColumn(ROUTE_STATION_COLUMN).setPreferredWidth(210);
        routeTable.getColumnModel().getColumn(5).setPreferredWidth(240);
        routeTable.getColumnModel().getColumn(6).setPreferredWidth(115);
    }

    private void configureCommodityTable() {
        commodityTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        commodityTable.setRowSelectionAllowed(false);
        commodityTable.setAutoCreateRowSorter(false);
        styleCommodityTable();
        commodityTable.getColumnModel().getColumn(0).setPreferredWidth(190);
        for (int column = 1; column < commodityTable.getColumnCount(); column++) {
            commodityTable.getColumnModel().getColumn(column).setPreferredWidth(85);
        }
    }

    private void styleRouteTable() {
        HudTable.styleCompact(routeTable);
        routeTable.putClientProperty(HUD_TABLE_STYLE_LOCKED, Boolean.TRUE);
        routeTable.setDefaultRenderer(Object.class, new HudTable.ValueCellRenderer());
        routeTable.getColumnModel().getColumn(0).setCellRenderer(
                new HudTable.ValueCellRenderer(HUD_COLOR_ROLE_PRIMARY_TEXT, SwingConstants.RIGHT));
        routeTable.getColumnModel().getColumn(ROUTE_SYSTEM_COLUMN).setCellRenderer(
                new RouteValueCopyRenderer());
        routeTable.getColumnModel().getColumn(ROUTE_STATION_COLUMN).setCellRenderer(
                new RouteValueCopyRenderer());
        routeTable.getColumnModel().getColumn(6).setCellRenderer(
                new HudTable.ValueCellRenderer(HUD_COLOR_ROLE_SUCCESS, SwingConstants.RIGHT));
    }

    private void installRouteValueCopying() {
        // JTable renderers are paint-only, so the table owns copy hit-testing and clipboard writes.
        routeTable.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                updateRouteCopyHover(event);
            }
        });
        routeTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                updateRouteCopyHover(event);
            }

            @Override
            public void mousePressed(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                int row = routeTable.rowAtPoint(event.getPoint());
                int column = routeTable.columnAtPoint(event.getPoint());
                if (isRouteCopyHit(row, column, event.getX())) {
                    setRouteCopyPressed(row, column);
                } else {
                    setRouteCopyPressed(-1, -1);
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                setRouteCopyPressed(-1, -1);
                updateRouteCopyHover(event);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                setRouteCopyPressed(-1, -1);
                setRouteCopyHover(-1, -1);
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event) && isRouteCopyHit(event)) {
                    ClipboardUtils.setClipboardText(routeValueAt(event));
                }
            }
        });
    }

    private void updateRouteCopyHover(MouseEvent event) {
        int row = routeTable.rowAtPoint(event.getPoint());
        int column = routeTable.columnAtPoint(event.getPoint());
        if (isRouteCopyHit(row, column, event.getX())) {
            setRouteCopyHover(row, column);
        } else {
            setRouteCopyHover(-1, -1);
        }
    }

    private void setRouteCopyHover(int row, int column) {
        if (copyHoverRow == row && copyHoverColumn == column) {
            return;
        }
        copyHoverRow = row;
        copyHoverColumn = column;
        routeTable.setCursor(Cursor.getPredefinedCursor(
                row < 0 ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
        routeTable.repaint();
    }

    private void setRouteCopyPressed(int row, int column) {
        if (copyPressedRow == row && copyPressedColumn == column) {
            return;
        }
        copyPressedRow = row;
        copyPressedColumn = column;
        routeTable.repaint();
    }

    private boolean isRouteCopyHit(MouseEvent event) {
        return isRouteCopyHit(
                routeTable.rowAtPoint(event.getPoint()),
                routeTable.columnAtPoint(event.getPoint()),
                event.getX());
    }

    private boolean isRouteCopyHit(int row, int column, int pointerX) {
        if (row < 0 || column < 0) {
            return false;
        }
        return isRouteValueCopyHit(
                routeTable.getCellRect(row, column, false),
                routeTable.convertColumnIndexToModel(column),
                routeValueAt(row, column),
                pointerX);
    }

    private String routeValueAt(MouseEvent event) {
        return routeValueAt(
                routeTable.rowAtPoint(event.getPoint()),
                routeTable.columnAtPoint(event.getPoint()));
    }

    private String routeValueAt(int row, int column) {
        Object value = routeTable.getValueAt(row, column);
        return value == null ? "" : clean(value.toString());
    }

    /** Returns whether the displayed route name is a value rather than the unknown placeholder. */
    static boolean isCopyableRouteValue(String value) {
        String cleaned = clean(value);
        return !cleaned.isEmpty() && !cleaned.equals(getText("help.trade.value.unknown"));
    }

    /** Returns whether a pointer falls within the copy affordance of a populated System or Station cell. */
    static boolean isRouteValueCopyHit(Rectangle bounds, int modelColumn, String value, int pointerX) {
        return bounds != null
                && (modelColumn == ROUTE_SYSTEM_COLUMN || modelColumn == ROUTE_STATION_COLUMN)
                && isCopyableRouteValue(value)
                && pointerX >= bounds.x + bounds.width - ROUTE_COPY_AREA_WIDTH;
    }

    private final class RouteValueCopyRenderer extends HudTable.ValueCellRenderer {
        private boolean selected;
        private boolean copyGlyphVisible;
        private boolean copyGlyphHovered;
        private boolean copyGlyphPressed;

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            Component component = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            selected = isSelected;
            copyGlyphVisible = isCopyableRouteValue(value == null ? "" : value.toString());
            copyGlyphHovered = copyGlyphVisible && copyHoverRow == row && copyHoverColumn == column;
            copyGlyphPressed = copyGlyphVisible && copyPressedRow == row && copyPressedColumn == column;
            setBorder(new EmptyBorder(
                    getVerticalPadding(), HUD_GAP, getVerticalPadding(), ROUTE_COPY_AREA_WIDTH));
            return component;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!copyGlyphVisible) {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int affordanceX = getWidth() - ROUTE_COPY_AREA_WIDTH;
                Color stateColor = selected
                        ? HUD_COLOR_ROLE_SELECTED_TEXT : HUD_COLOR_ROLE_PRIMARY_ACTION;
                if (copyGlyphPressed) {
                    g2.setColor(stateColor);
                    g2.fillRect(affordanceX, 0, ROUTE_COPY_AREA_WIDTH, getHeight());
                } else if (copyGlyphHovered) {
                    int inset = HUD_BORDER_THICKNESS;
                    g2.setColor(stateColor);
                    g2.drawRect(
                            affordanceX + inset,
                            inset,
                            Math.max(0, ROUTE_COPY_AREA_WIDTH - inset * 2 - 1),
                            Math.max(0, getHeight() - inset * 2 - 1));
                }
                int glyphX = getWidth() - ROUTE_COPY_AREA_WIDTH
                        + (ROUTE_COPY_AREA_WIDTH - HUD_ICON_TABLE) / 2;
                int glyphY = (getHeight() - HUD_ICON_TABLE) / 2;
                Color glyphColor = copyGlyphPressed
                        ? selected ? HUD_COLOR_ROLE_PRIMARY_ACTION : HUD_COLOR_ROLE_SELECTED_TEXT
                        : copyGlyphHovered
                                ? selected ? HUD_COLOR_ROLE_PRIMARY_TEXT : HUD_COLOR_ROLE_PRIMARY_ACTION
                                : selected ? HUD_COLOR_ROLE_SELECTED_TEXT : HUD_COLOR_ROLE_CONTROL_DECORATION;
                HudGlyphs.paintHudCopyGlyph(
                        g2, glyphX, glyphY, HUD_ICON_TABLE, HUD_ICON_TABLE, glyphColor);
            } finally {
                g2.dispose();
            }
        }
    }

    private void styleCommodityTable() {
        HudTable.styleCompact(commodityTable);
        commodityTable.putClientProperty(HUD_TABLE_STYLE_LOCKED, Boolean.TRUE);
        commodityTable.setDefaultRenderer(Object.class, new HudTable.ValueCellRenderer());
        for (int column = 1; column < commodityTable.getColumnCount(); column++) {
            commodityTable.getColumnModel().getColumn(column).setCellRenderer(
                    new HudTable.ValueCellRenderer(
                            column == commodityTable.getColumnCount() - 1
                                    ? HUD_COLOR_ROLE_SUCCESS
                                    : HUD_COLOR_ROLE_PRIMARY_TEXT,
                            SwingConstants.RIGHT));
        }
    }

    private void configureActions() {
        cancelButton.setInfoAction(commandInfo(cancelButton, CancelTradeRouteCommand.ID));
        cancelButton.addActionListener(event -> cancelRoute());

        navigateButton.setInfoAction(commandInfo(navigateButton, NavigateToTradeStopCommand.ID));
        navigateButton.addActionListener(event -> navigateToNextStop());

        calculateButton.setInfoAction(commandInfo(calculateButton, CalculateTradeRouteCommand.ID));
        calculateButton.addActionListener(event -> calculateRoute());
    }

    private void openCurrentShipTradeProfile() {
        ShipLoadOutDto currentShip = playerSession.getShipLoadout();
        if (currentShip == null) {
            refreshCurrentShip();
            return;
        }
        String identifier = currentShipDisplayName(currentShip);
        var settings = ShipSettingsManager.getInstance().getSettings(currentShip.getShipId());
        var popup = ShipSettingsPopup.create(this, identifier, settings);
        runWithModalScrim(SwingUtilities.getWindowAncestor(this), () -> popup.setVisible(true));
    }

    private Runnable commandInfo(Component parent, String commandId) {
        return () -> commandCatalog.builtInEntries().stream()
                .filter(entry -> entry.id().equalsIgnoreCase(commandId))
                .findFirst()
                .ifPresent(entry -> runWithModalScrim(
                        SwingUtilities.getWindowAncestor(parent),
                        () -> new CommandDetailsDialog(parent, entry).showDialog()));
    }

    private void calculateRoute() {
        setBusy(BusyAction.CALCULATING);
        GuiCommandRunner.runInApp(
                CalculateTradeRouteCommand.ID,
                new JsonObject(),
                true,
                () -> finishAction(BusyAction.CALCULATING));
    }

    private void navigateToNextStop() {
        boolean scheduled = GuiCommandRunner.runAfterActivatingGame(
                NavigateToTradeStopCommand.ID, new JsonObject(), true);
        if (!scheduled) {
            HudConfirmDialog.info(
                    this,
                    getText("help.trade.navigate.unavailable.title"),
                    getText("help.trade.navigate.unavailable.message"),
                    getText("button.ok"));
        }
    }

    private void cancelRoute() {
        boolean confirmed = HudConfirmDialog.confirm(
                this,
                getText("help.trade.cancel.title"),
                getText("help.trade.cancel.message"),
                getText("help.trade.action.cancel"),
                getText("button.back"));
        if (!confirmed) {
            return;
        }
        setBusy(BusyAction.CANCELLING);
        GuiCommandRunner.runInApp(
                CancelTradeRouteCommand.ID,
                new JsonObject(),
                true,
                () -> finishAction(BusyAction.CANCELLING));
    }

    private void finishAction(BusyAction completedAction) {
        if (busyAction == completedAction) {
            busyAction = BusyAction.NONE;
        }
        updateStatusAndActions();
        requestRefresh();
    }

    private void setBusy(BusyAction action) {
        busyAction = action;
        updateStatusAndActions();
    }

    /** Loads the first route snapshot and starts polling when this sub-tab is visible. EDT-only. */
    void initData() {
        requestRefresh();
        if (isShowing() && !refreshTimer.isRunning()) {
            refreshTimer.start();
        }
    }

    /** Stops polling and prevents an outstanding background read from updating a discarded panel. */
    void dispose() {
        disposed = true;
        refreshTimer.stop();
        if (refreshWorker != null) {
            refreshWorker.cancel(true);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> {
            styleRouteTable();
            styleCommodityTable();
        });
    }

    private void requestRefresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::requestRefresh);
            return;
        }
        if (disposed) {
            return;
        }
        if (refreshWorker != null) {
            refreshPending = true;
            return;
        }
        refreshWorker = new SwingWorker<>() {
            @Override
            protected RouteViewState doInBackground() {
                return loadState();
            }

            @Override
            protected void done() {
                try {
                    if (!disposed && !isCancelled()) {
                        RouteViewState loaded = get();
                        refreshCurrentShip();
                        if (!loaded.equals(state)) {
                            applyState(loaded);
                        }
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    log.debug("Unable to refresh the Help trade route", exception.getCause());
                } finally {
                    refreshWorker = null;
                    if (refreshPending && !disposed) {
                        refreshPending = false;
                        requestRefresh();
                    }
                }
            }
        };
        refreshWorker.execute();
    }

    private RouteViewState loadState() {
        CurrentPlace currentPlace = readCurrentPlace();
        List<TradeRouteManager.TradeRouteLegTuple<Integer, TradeStopDto>> tuples =
                tradeRouteManager.getAllStops();
        List<RouteLegData> legs = new ArrayList<>();
        if (tuples != null) {
            for (TradeRouteManager.TradeRouteLegTuple<Integer, TradeStopDto> tuple : tuples) {
                RouteLegData leg = toLegData(tuple);
                if (leg != null) {
                    legs.add(leg);
                }
            }
        }
        return buildViewState(legs, currentPlace);
    }

    private CurrentPlace readCurrentPlace() {
        String fallbackSystem = "";
        try {
            fallbackSystem = clean(playerSession.getPrimaryStarName());
        } catch (RuntimeException exception) {
            log.debug("Unable to read the current primary star for the Help trade route", exception);
        }

        try {
            var locationData = playerSession.getLocationData();
            if (locationData != null) {
                LocationDto location = locationManager.findByLocationData(locationData);
                if (location != null) {
                    String system = firstNonBlank(location.getStarName(), fallbackSystem);
                    return new CurrentPlace(system, location.getStationName());
                }
            }
        } catch (RuntimeException exception) {
            log.debug("Unable to read the current location for the Help trade route", exception);
        }
        return new CurrentPlace(fallbackSystem, "");
    }

    private static RouteLegData toLegData(
            TradeRouteManager.TradeRouteLegTuple<Integer, TradeStopDto> tuple) {
        if (tuple == null || tuple.getTradeStopDto() == null) {
            return null;
        }
        TradeStopDto stop = tuple.getTradeStopDto();
        Integer tupleNumber = tuple.getLegNumber();
        int legNumber = tupleNumber == null ? stop.getStopNumber() : tupleNumber;
        List<CommodityData> commodities = new ArrayList<>();
        if (stop.getCommodities() != null) {
            for (TradeCommodity commodity : stop.getCommodities()) {
                if (commodity == null) {
                    continue;
                }
                TradeCommodityInfo source = commodity.getSourceCommodity();
                TradeCommodityInfo destination = commodity.getDestinationCommodity();
                commodities.add(new CommodityData(
                        commodity.getName(),
                        commodity.getAmount(),
                        source == null ? null : source.getBuyPrice(),
                        source == null ? null : source.getSupply(),
                        destination == null ? null : destination.getSellPrice(),
                        destination == null ? null : destination.getDemand(),
                        commodity.getProfit(),
                        commodity.getTotalProfit()));
            }
        }
        return new RouteLegData(
                legNumber,
                stop.getSourceSystem(),
                stop.getSourceStation(),
                stop.getDestinationSystem(),
                stop.getDestinationStation(),
                commodities);
    }

    private void applyState(RouteViewState newState) {
        RoutePointKey selectedPoint = selectedPointKey();
        state = newState;
        applyingState = true;
        try {
            currentLocationField.setText(caps(placeText(newState.currentPlace())));
            startSystemField.setText(caps(valueOrUnknown(newState.currentPlace().system())));

            routeModel.setRowCount(0);
            int activePoint = activePointIndex(newState);
            for (int index = 0; index < newState.points().size(); index++) {
                RoutePointData point = newState.points().get(index);
                boolean here = index == activePoint
                        && isAtRoutePoint(newState.currentPlace(), point);
                routeModel.addRow(new Object[]{
                        point.pointNumber(),
                        routeRowStatus(index, activePoint, here),
                        getText(point.action().localizationKey()),
                        valueOrUnknown(point.system()),
                        valueOrUnknown(point.station()),
                        cargoText(point.commodities()),
                        number(point.profit())
                });
            }

            if (newState.points().isEmpty()) {
                routeCardLayout.show(routeCards, EMPTY_CARD);
            } else {
                routeCardLayout.show(routeCards, ROUTE_CARD);
                int selection = indexOfPoint(newState.points(), selectedPoint);
                if (selection < 0) {
                    selection = activePoint;
                }
                routeTable.setRowSelectionInterval(selection, selection);
            }
            routeSection.setTitle(routeSectionTitle(newState.points().size()));
        } finally {
            applyingState = false;
        }
        updateCommodityDetails();
        updateStatusAndActions();
    }

    private void updateCommodityDetails() {
        commodityModel.setRowCount(0);
        RoutePointData selected = selectedPoint();
        if (selected == null) {
            return;
        }
        for (CommodityData commodity : selected.commodities()) {
            commodityModel.addRow(new Object[]{
                    valueOrUnknown(commodity.name()),
                    number(commodity.amount()),
                    number(commodity.buyPrice()),
                    number(commodity.supply()),
                    number(commodity.sellPrice()),
                    number(commodity.demand()),
                    number(commodity.unitProfit()),
                    number(commodity.totalProfit())
            });
        }
    }

    private void updateStatusAndActions() {
        boolean hasRoute = state != null && !state.points().isEmpty();
        boolean idle = busyAction == BusyAction.NONE;
        cancelButton.setMainActionEnabled(idle && hasRoute);
        navigateButton.setMainActionEnabled(idle && hasRoute);
        calculateButton.setMainActionEnabled(idle);
        calculateButton.setText(caps(getText(hasRoute
                ? "help.trade.action.recalculate"
                : "help.trade.action.calculate")));

        if (busyAction == BusyAction.CALCULATING) {
            routeStatus.setValue(getText("help.trade.status.calculating"), StatusBadge.State.STANDBY);
        } else if (busyAction == BusyAction.CANCELLING) {
            routeStatus.setValue(getText("help.trade.status.cancelling"), StatusBadge.State.STANDBY);
        } else if (!hasRoute) {
            routeStatus.setValue(getText("help.trade.status.none"), StatusBadge.State.IDLE);
        } else {
            RoutePointData current = state.points().get(activePointIndex(state));
            routeStatus.setValue(getText(
                    "help.trade.status.summary",
                    number(current.pointNumber()),
                    number(state.points().size()),
                    number(state.totalProfit())), StatusBadge.State.OK);
        }
    }

    private RoutePointData selectedPoint() {
        if (state == null || routeTable.getSelectedRow() < 0) {
            return null;
        }
        int modelRow = routeTable.convertRowIndexToModel(routeTable.getSelectedRow());
        return modelRow >= 0 && modelRow < state.points().size()
                ? state.points().get(modelRow)
                : null;
    }

    private RoutePointKey selectedPointKey() {
        RoutePointData selected = selectedPoint();
        return selected == null ? null : selected.key();
    }

    private void showStartSystemInfo() {
        HudConfirmDialog.info(
                this,
                getText("help.trade.field.startSystem"),
                getText("help.trade.field.startSystem.info"),
                getText("button.ok"));
    }

    private void refreshCurrentShip() {
        ShipLoadOutDto currentShip = playerSession.getShipLoadout();
        boolean hasCurrentShip = currentShip != null;
        currentShipField.setText(caps(valueOrUnknown(currentShipDisplayName(currentShip))));
        currentShipProfileButton.setEnabled(hasCurrentShip);
        currentShipProfileButton.setIcon(hasCurrentShip
                ? CURRENT_SHIP_PROFILE_GEAR_ICON
                : CURRENT_SHIP_PROFILE_GEAR_DISABLED_ICON);
    }

    private static String currentShipDisplayName(ShipLoadOutDto currentShip) {
        return currentShip == null
                ? null
                : LoadoutConverter.toDisplayShipName(currentShip.getShipName(), currentShip.getShipMake());
    }

    private String routeSectionTitle(int count) {
        String title = getText("help.trade.section.route");
        return count == 0 ? title : title + " (" + number(count) + ")";
    }

    private static DefaultTableModel readOnlyModel(String... columnKeys) {
        Object[] columns = new Object[columnKeys.length];
        for (int index = 0; index < columnKeys.length; index++) {
            columns[index] = getText(columnKeys[index]);
        }
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static String routeRowStatus(int index, int activePoint, boolean here) {
        if (index < activePoint) {
            return getText("help.trade.row.completed");
        }
        if (index == activePoint) {
            return getText(here ? "help.trade.row.currentHere" : "help.trade.row.current");
        }
        if (index == activePoint + 1) {
            return getText("help.trade.row.next");
        }
        return getText("help.trade.row.planned");
    }

    private static String cargoText(List<CommodityData> commodities) {
        if (commodities.isEmpty()) {
            return getText("help.trade.value.unknown");
        }
        return commodities.stream()
                .map(commodity -> valueOrUnknown(commodity.name()) + " ×" + number(commodity.amount()))
                .reduce((left, right) -> left + ", " + right)
                .orElseGet(() -> getText("help.trade.value.unknown"));
    }

    private static String placeText(CurrentPlace place) {
        return placeText(place.system(), place.station());
    }

    private static String placeText(String system, String station) {
        String cleanSystem = clean(system);
        String cleanStation = clean(station);
        if (cleanSystem.isEmpty()) {
            return cleanStation.isEmpty() ? getText("help.trade.value.unknown") : cleanStation;
        }
        return cleanStation.isEmpty() ? cleanSystem : cleanSystem + " / " + cleanStation;
    }

    private static String valueOrUnknown(String value) {
        String cleaned = clean(value);
        return cleaned.isEmpty() ? getText("help.trade.value.unknown") : cleaned;
    }

    private static String number(Number value) {
        return value == null
                ? getText("help.trade.value.unknown")
                : NumberFormat.getIntegerInstance(Locale.US).format(value.longValue());
    }

    private static String caps(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String firstNonBlank(String preferred, String fallback) {
        String value = clean(preferred);
        return value.isEmpty() ? clean(fallback) : value;
    }

    private static int indexOfPoint(List<RoutePointData> points, RoutePointKey key) {
        if (key == null) {
            return -1;
        }
        for (int index = 0; index < points.size(); index++) {
            if (points.get(index).key().equals(key)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Sorts a read-only route snapshot by persisted leg number and expands every trade leg into
     * separate buy and sell points for display. Completed legs are already removed by the existing
     * route workflow.
     */
    static RouteViewState buildViewState(List<RouteLegData> legs, CurrentPlace currentPlace) {
        List<RouteLegData> sorted = legs == null
                ? List.of()
                : legs.stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparingInt(RouteLegData::legNumber))
                        .toList();
        return new RouteViewState(
                currentPlace == null ? new CurrentPlace("", "") : currentPlace,
                sorted,
                buildRoutePoints(sorted));
    }

    private static List<RoutePointData> buildRoutePoints(List<RouteLegData> legs) {
        List<RoutePointData> points = new ArrayList<>(legs.size() * 2);
        int pointNumber = 0;
        for (RouteLegData leg : legs) {
            points.add(new RoutePointData(
                    ++pointNumber,
                    leg.legNumber(),
                    RoutePointAction.BUY,
                    leg.sourceSystem(),
                    leg.sourceStation(),
                    leg.commodities(),
                    null));
            points.add(new RoutePointData(
                    ++pointNumber,
                    leg.legNumber(),
                    RoutePointAction.SELL,
                    leg.destinationSystem(),
                    leg.destinationStation(),
                    leg.commodities(),
                    leg.totalProfit()));
        }
        return List.copyOf(points);
    }

    /**
     * Returns the first displayed point matching the known player location, falling back to the
     * first remaining point while the player is travelling between route stations.
     */
    static int activePointIndex(RouteViewState state) {
        if (state == null || state.points().isEmpty()) {
            return -1;
        }
        for (int index = 0; index < state.points().size(); index++) {
            if (isAtRoutePoint(state.currentPlace(), state.points().get(index))) {
                return index;
            }
        }
        return 0;
    }

    /** Returns whether the player's known system/station coincides with either end of a route leg. */
    static boolean isAtRoutePoint(CurrentPlace place, RouteLegData leg) {
        if (place == null || leg == null || place.system().isEmpty()) {
            return false;
        }
        return samePlace(place, leg.sourceSystem(), leg.sourceStation())
                || samePlace(place, leg.destinationSystem(), leg.destinationStation());
    }

    private static boolean isAtRoutePoint(CurrentPlace place, RoutePointData point) {
        return place != null
                && point != null
                && !place.system().isEmpty()
                && samePlace(place, point.system(), point.station());
    }

    private static boolean samePlace(CurrentPlace actual, String expectedSystem, String expectedStation) {
        if (!actual.system().equalsIgnoreCase(clean(expectedSystem))) {
            return false;
        }
        String station = clean(expectedStation);
        return station.isEmpty()
                || (!actual.station().isEmpty() && actual.station().equalsIgnoreCase(station));
    }

    private enum BusyAction {
        NONE,
        CALCULATING,
        CANCELLING
    }

    /** Action performed at one displayed route point. */
    enum RoutePointAction {
        BUY("help.trade.row.buy"),
        SELL("help.trade.row.sell");

        private final String localizationKey;

        RoutePointAction(String localizationKey) {
            this.localizationKey = localizationKey;
        }

        private String localizationKey() {
            return localizationKey;
        }
    }

    record CurrentPlace(String system, String station) {
        CurrentPlace {
            system = clean(system);
            station = clean(station);
        }
    }

    record CommodityData(
            String name,
            int amount,
            Long buyPrice,
            Long supply,
            Long sellPrice,
            Long demand,
            long unitProfit,
            long totalProfit) {
        CommodityData {
            name = clean(name);
        }
    }

    record RouteLegData(
            int legNumber,
            String sourceSystem,
            String sourceStation,
            String destinationSystem,
            String destinationStation,
            List<CommodityData> commodities) {
        RouteLegData {
            sourceSystem = clean(sourceSystem);
            sourceStation = clean(sourceStation);
            destinationSystem = clean(destinationSystem);
            destinationStation = clean(destinationStation);
            commodities = commodities == null ? List.of() : List.copyOf(commodities);
        }

        long totalProfit() {
            long total = 0L;
            for (CommodityData commodity : commodities) {
                total += commodity.totalProfit();
            }
            return total;
        }
    }

    private record RoutePointKey(int legNumber, RoutePointAction action) {
    }

    /** One table row representing either the buy or sell end of a persisted trade leg. */
    record RoutePointData(
            int pointNumber,
            int legNumber,
            RoutePointAction action,
            String system,
            String station,
            List<CommodityData> commodities,
            Long profit) {
        RoutePointData {
            action = Objects.requireNonNull(action);
            system = clean(system);
            station = clean(station);
            commodities = commodities == null ? List.of() : List.copyOf(commodities);
        }

        RoutePointKey key() {
            return new RoutePointKey(legNumber, action);
        }
    }

    record RouteViewState(
            CurrentPlace currentPlace,
            List<RouteLegData> legs,
            List<RoutePointData> points) {
        RouteViewState {
            currentPlace = currentPlace == null ? new CurrentPlace("", "") : currentPlace;
            legs = legs == null ? List.of() : List.copyOf(legs);
            points = points == null ? List.of() : List.copyOf(points);
        }

        long totalProfit() {
            long total = 0L;
            for (RouteLegData leg : legs) {
                total += leg.totalProfit();
            }
            return total;
        }
    }
}
