package elite.intel.ui.screen;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.actions.catalog.CommandCatalog;
import elite.intel.ai.brain.actions.catalog.CommandCatalogEntry;
import elite.intel.ai.brain.actions.customcommand.CustomCommandDefinition;
import elite.intel.ai.brain.actions.customcommand.CustomCommandRegistry;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.prompt.GameToolCandidates;
import elite.intel.companion.prompt.SemanticActionReducer;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.NormalizedUserInputEvent;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import elite.intel.ui.dialog.CommandDetailsDialog;
import elite.intel.ui.event.CustomCommandsSummaryChangedEvent;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudTable;
import elite.intel.ui.widget.HudTwoColumns;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudPalette.*;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * The "Help" tab. At the top of the page it shows two read-only fields: the commander's current physical
 * situation together with the concrete place (in ship / SRV / fighter / taxi / on foot; docked, landed,
 * supercruise, ring, orbit, deep space, station, planet surface), and below it the commander's current
 * spoken phrase. Below those, an "Available commands and queries" section splits (canon 9) into two
 * single-column read-only tables (canon 6): one combined, alphabetically sorted list of the commands (built-in
 * actions plus custom-command macros) and queries available in the current game context, divided evenly by
 * count between the left and right tables. All of it updates live off game events while the tab is showing
 * (the tables rebuild when the situation changes).
 */
public class HelpTabPanel extends JPanel {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final Status status = Status.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final CommandCatalog commandCatalog = new CommandCatalog();
    private final CompanionActionReducer semanticFallbackReducer = new SemanticActionReducer();

    private JTextField locationField;
    private JTextField phraseField;
    private JTable leftTable;
    private JTable rightTable;
    private DefaultTableModel leftModel;
    private DefaultTableModel rightModel;
    private HudSection actionsSection;
    /** Last situation the action tables were built for; they rebuild only when it changes (EDT-only field). */
    private PlayerSituation lastSituation;
    /** Last status flags rendered; the frequent Status tick is skipped while these are unchanged (event-thread-only). */
    private long lastFlags = -1L;
    private long lastFlags2 = -1L;
    /** Guards GameEventBus register/unregister so a hide without a prior show cannot throw or double-subscribe. */
    private boolean subscribed;
    /** Action ids selected by the semantic reducer for the current commander phrase (EDT-only). */
    private Set<String> highlightedActionIds = Set.of();
    /** Monotonic token used to ignore stale background semantic-selection results. */
    private int highlightRequest;
    /** Current available action rows before highlight ordering is applied (EDT-only). */
    private List<ActionRow> visibleActionRows = List.of();

    public HelpTabPanel() {
        buildUi();
        // Subscribe to the (frequent) live Status event only while the tab is actually showing; JTabbedPane
        // toggles child visibility on tab switch, so componentShown/Hidden bracket the tab being open.
        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) {
                subscribe();
                refresh();
            }
            @Override public void componentHidden(ComponentEvent e) {
                unsubscribe();
            }
        });
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        setBorder(hudScreenBorder());

        // Two read-only rows directly at the top of the page (no framed section), stacked: location, then phrase.
        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        GridBagConstraints gbc = baseGbc();

        addLabel(top, getText("location.field.label"), gbc);
        locationField = makeTextField();
        locationField.setEditable(false); // read-only readout: a bounded surface, not an input (HUD canon 5.1)
        addField(top, locationField, gbc, 1, 1.0);

        nextRow(gbc);
        addLabel(top, getText("location.field.phrase"), gbc);
        phraseField = makeTextField();
        phraseField.setEditable(false);
        addField(top, phraseField, gbc, 1, 1.0);

        add(top, BorderLayout.NORTH);

        // "Available commands and queries": one combined list, divided evenly by count into a left and a right
        // single-column read-only HUD table (canon 6). The two columns share ONE vertical scrollbar (canon 8):
        // their bodies scroll together in a single viewport while the two headers ride in the shared
        // column-header row and stay put.
        leftModel = readOnlyModel(getText("location.column.commandName"));
        rightModel = readOnlyModel(getText("location.column.commandName"));
        leftTable = new JTable(leftModel);
        rightTable = new JTable(rightModel);
        styleActionsTable(leftTable);
        styleActionsTable(rightTable);
        installRowHover(leftTable);
        installRowHover(rightTable);
        installCommandDetailsOpen(leftTable);
        installCommandDetailsOpen(rightTable);

        // Bodies and headers both use HudTwoColumns, so their halves (and the centre divider) line up.
        HudTwoColumns bodies = new HudTwoColumns(leftTable, rightTable);
        HudTwoColumns headers = new HudTwoColumns(leftTable.getTableHeader(), rightTable.getTableHeader());
        JScrollPane scroll = hudScrollPane(new WidthTrackingScrollContent(bodies));
        scroll.setColumnHeaderView(headers);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Data-plane treatment (canon 8): the tables float on the app background, no frame around the scroll.
        scroll.getViewport().setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        scroll.setBorder(hudDataPlaneBorder());
        scroll.putClientProperty(HUD_SCROLL_STYLE_LOCKED, Boolean.TRUE);

        actionsSection = HudSection.flat(availableActionsTitle(0), new BorderLayout());
        actionsSection.body().add(scroll, BorderLayout.CENTER);
        add(actionsSection, BorderLayout.CENTER);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // Re-assert table styling after applyDarkPalette has walked the tree (mirrors CommandCatalogTablePanel).
        SwingUtilities.invokeLater(() -> {
            styleActionsTable(leftTable);
            styleActionsTable(rightTable);
        });
    }

    /** Single-column, read-only table model whose one column carries the given header title. */
    private static DefaultTableModel readOnlyModel(String columnTitle) {
        return new DefaultTableModel(new Object[]{columnTitle}, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /** Applies the shared HUD table look (canon 6): capsed value renderer, style locked against re-theming. */
    private void styleActionsTable(JTable table) {
        HudTable.style(table);
        table.setDefaultRenderer(Object.class, new ActionHighlightRenderer());
        table.putClientProperty(HUD_TABLE_STYLE_LOCKED, Boolean.TRUE);
    }

    private void installCommandDetailsOpen(JTable table) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                openCommandDetailsAt(table, table.rowAtPoint(event.getPoint()));
            }
        });
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ENTER"), "openHelpCommandDetails");
        table.getActionMap().put("openHelpCommandDetails", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                openCommandDetailsAt(table, table.getSelectedRow());
            }
        });
    }

    private void installRowHover(JTable table) {
        table.putClientProperty(HudTable.HOVER_ROW_PROPERTY, -1);
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                setHoveredRow(table, rowUnderPoint(table, event.getPoint()));
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                setHoveredRow(table, -1);
            }
        });
    }

    private int rowUnderPoint(JTable table, Point point) {
        int row = table.rowAtPoint(point);
        if (row >= 0) {
            return row;
        }
        Point up = new Point(point.x, point.y - 2);
        row = table.rowAtPoint(up);
        if (row >= 0) {
            return row;
        }
        Point down = new Point(point.x, point.y + 2);
        return table.rowAtPoint(down);
    }

    private void setHoveredRow(JTable table, int row) {
        Object current = table.getClientProperty(HudTable.HOVER_ROW_PROPERTY);
        int currentRow = current instanceof Integer value ? value : -1;
        if (currentRow == row) {
            return;
        }
        table.putClientProperty(HudTable.HOVER_ROW_PROPERTY, row);
        repaintTableRow(table, currentRow);
        repaintTableRow(table, row);
    }

    private void repaintTableRow(JTable table, int row) {
        if (row < 0 || row >= table.getRowCount()) {
            return;
        }
        Rectangle rect = table.getCellRect(row, 0, true);
        rect.width = table.getWidth();
        table.repaint(rect);
    }

    /**
     * Scroll viewport host that stretches its content to the viewport width (so the side-by-side tables fill
     * the row, no horizontal scroll) while letting it grow taller than the viewport, so a single vertical
     * scrollbar drives both tables at once.
     */
    private static final class WidthTrackingScrollContent extends JPanel implements Scrollable {
        WidthTrackingScrollContent(Component view) {
            super(new BorderLayout());
            setOpaque(false);
            add(view, BorderLayout.CENTER);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }

        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visible.height : visible.width;
        }

        @Override public boolean getScrollableTracksViewportWidth() { return true; }

        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** Reads the persisted situation + location once. Called by {@link AppView} at startup and on tab show. */
    public void initData() {
        refresh();
    }

    /** Unregisters from the event bus; safe to call when never registered (e.g. on a language-change rebuild). */
    public void dispose() {
        unsubscribe();
    }

    /**
     * Live update: fires off the game event thread on every Status.json tick, so it classifies from the
     * event's own flags (avoiding a persistence race) and marshals the field update onto the EDT.
     */
    @Subscribe
    public void onStatusChanged(GameEvents.StatusEvent event) {
        long flags = event.getFlags();
        long flags2 = event.getFlags2();
        // Status.json ticks several times a second; our view only depends on the flags, so skip the DB read and
        // re-render entirely while they are unchanged (the flags always change on the transitions we display).
        if (flags == lastFlags && flags2 == lastFlags2) {
            return;
        }
        lastFlags = flags;
        lastFlags2 = flags2;
        LocationDto location = currentLocation();
        PlayerSituation situation = status.getSituation(flags, flags2, location);
        String text = describe(situation, location);
        SwingUtilities.invokeLater(() -> {
            locationField.setText(text);
            // The available-actions tables are context-gated, so rebuild them only when the situation changes
            // (cheap), not on every status change. The event bus is synchronous, so by the time this EDT runnable
            // runs the status snapshot is already persisted and GameToolCandidates sees the current context.
            if (situation != lastSituation) {
                lastSituation = situation;
                rebuildAvailableActions(situation, location);
            }
        });
    }

    /**
     * Live update of the commander's current spoken phrase (the normalized voice input), fired off the game
     * event thread; the field update is marshaled onto the EDT.
     */
    @Subscribe
    public void onCommanderPhrase(NormalizedUserInputEvent event) {
        String text = event.getText() == null ? "" : event.getText();
        SwingUtilities.invokeLater(() -> {
            phraseField.setText(text);
            updateSemanticHighlights(text);
        });
    }

    /**
     * Rebuilds the available-actions tables when the set of custom-command macros changes (added, removed, or
     * reloaded, including a macro created by voice while this tab is open), so the list stays current without a
     * situation change or a tab switch.
     */
    @Subscribe
    public void onCustomCommandsChanged(CustomCommandsSummaryChangedEvent event) {
        SwingUtilities.invokeLater(this::rebuildAvailableActions);
    }

    private void subscribe() {
        if (!subscribed) {
            GameEventBus.register(this); // live Status + commander-phrase events
            UiBus.register(this);        // custom-command set changes
            subscribed = true;
        }
    }

    private void unsubscribe() {
        if (subscribed) {
            GameEventBus.unregister(this);
            UiBus.unregister(this);
            subscribed = false;
        }
    }

    private void refresh() {
        LocationDto location = currentLocation();
        PlayerSituation situation = status.getSituation(location);
        locationField.setText(describe(situation, location));
        lastSituation = situation;
        rebuildAvailableActions(situation, location);
    }

    private LocationDto currentLocation() {
        return locationManager.findByLocationData(playerSession.getLocationData());
    }

    /**
     * Rebuilds the two tables from one combined list of everything available now (commands, macros, queries),
     * divided evenly by count: the left table gets the first half (the extra row when the count is odd), the
     * right table the rest, so both columns hold roughly the same number of rows.
     */
    private void rebuildAvailableActions() {
        LocationDto location = currentLocation();
        rebuildAvailableActions(status.getSituation(location), location);
    }

    private void rebuildAvailableActions(PlayerSituation situation, LocationDto location) {
        if (location == null || situation == PlayerSituation.UNKNOWN) {
            clearAvailableActions();
            return;
        }
        visibleActionRows = availableRows(
                EnumSet.of(IntelActionCategory.ACTION, IntelActionCategory.MACRO, IntelActionCategory.QUERY));
        updateAvailableActionsTitle();
        applyActionRows();
        updateSemanticHighlights(phraseField.getText());
    }

    private void clearAvailableActions() {
        visibleActionRows = List.of();
        updateAvailableActionsTitle();
        leftModel.setRowCount(0);
        rightModel.setRowCount(0);
        highlightedActionIds = Set.of();
        ++highlightRequest;
        repaintActionTables();
    }

    private void applyActionRows() {
        List<ActionRow> ordered = new ArrayList<>(visibleActionRows);
        ordered.sort(Comparator
                .comparing((ActionRow row) -> !highlightedActionIds.contains(row.id()))
                .thenComparing(ActionRow::name, String.CASE_INSENSITIVE_ORDER));
        int half = (ordered.size() + 1) / 2;
        setRows(leftModel, ordered.subList(0, half));
        setRows(rightModel, ordered.subList(half, ordered.size()));
    }

    private void updateAvailableActionsTitle() {
        if (actionsSection != null) {
            actionsSection.setTitle(availableActionsTitle(visibleActionRows.size()));
        }
    }

    private String availableActionsTitle(int count) {
        String base = getText("location.section.availableActions");
        return count <= 0 ? base : base + " (" + count + ")";
    }

    private static void setRows(DefaultTableModel model, List<ActionRow> rows) {
        model.setRowCount(0);
        for (ActionRow row : rows) {
            model.addRow(new Object[]{row});
        }
    }

    /**
     * Localized, alphabetically sorted display names of the actions currently offered to the companion for the
     * given categories. {@link GameToolCandidates} owns "what is available now" (context visibility plus the
     * internal fallback-id exclusions); this only maps each surviving id to its localized name.
     */
    private List<ActionRow> availableRows(Set<IntelActionCategory> categories) {
        Map<String, String> nameById = nameIndex();
        return new GameToolCandidates().collect(categories).stream()
                .map(candidate -> new ActionRow(candidate.id(), nameById.getOrDefault(candidate.id(), candidate.id())))
                .sorted((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.name(), right.name()))
                .toList();
    }

    /** Recomputes semantic reducer hits for the current phrase without blocking the UI thread. */
    private void updateSemanticHighlights(String phrase) {
        int request = ++highlightRequest;
        if (phrase == null || phrase.isBlank()) {
            highlightedActionIds = Set.of();
            applyActionRows();
            return;
        }
        Set<IntelActionCategory> categories = EnumSet.of(
                IntelActionCategory.ACTION, IntelActionCategory.MACRO, IntelActionCategory.QUERY);
        new SwingWorker<Set<String>, Void>() {
            @Override protected Set<String> doInBackground() {
                List<LlmToolDefinition> tools = companionReducer().selectTools(categories, phrase);
                Set<String> ids = new HashSet<>();
                for (LlmToolDefinition tool : tools) {
                    ids.add(tool.name());
                }
                return ids;
            }

            @Override protected void done() {
                if (request != highlightRequest) {
                    return;
                }
                try {
                    highlightedActionIds = get();
                } catch (Exception ignored) {
                    highlightedActionIds = Set.of();
                }
                applyActionRows();
            }
        }.execute();
    }

    private CompanionActionReducer companionReducer() {
        try {
            return CompanionRuntime.reducer();
        } catch (IllegalStateException notRunning) {
            return semanticFallbackReducer;
        }
    }

    private void repaintActionTables() {
        leftTable.repaint();
        rightTable.repaint();
    }

    private record ActionRow(String id, String name) {
        @Override public String toString() {
            return name;
        }
    }

    private final class ActionHighlightRenderer extends HudTable.ValueCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected && value instanceof ActionRow actionRow && highlightedActionIds.contains(actionRow.id())) {
                component.setForeground(HUD_COLOR_ROLE_SUCCESS);
            }
            return component;
        }
    }

    private void openCommandDetailsAt(JTable table, int viewRow) {
        if (viewRow < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Object value = table.getModel().getValueAt(modelRow, 0);
        if (!(value instanceof ActionRow actionRow)) {
            return;
        }
        CommandCatalogEntry entry = commandEntryIndex().get(actionRow.id());
        if (entry == null) {
            return;
        }
        runWithModalScrim(
                SwingUtilities.getWindowAncestor(this),
                () -> new CommandDetailsDialog(this, entry).showDialog());
    }

    /** Maps every built-in command/query id (localized name from the catalog) and every macro id to a display name. */
    private Map<String, String> nameIndex() {
        Map<String, String> byId = new HashMap<>();
        for (CommandCatalogEntry entry : commandCatalog.builtInEntries()) {
            byId.put(entry.id(), entry.name());
        }
        for (CustomCommandDefinition macro : CustomCommandRegistry.getInstance().getCustomCommands()) {
            byId.put(macro.getActionKey(), macro.getName());
        }
        return byId;
    }

    private Map<String, CommandCatalogEntry> commandEntryIndex() {
        Map<String, CommandCatalogEntry> byId = new HashMap<>();
        for (CommandCatalogEntry entry : commandCatalog.builtInEntries()) {
            byId.put(entry.id(), entry);
        }
        for (CommandCatalogEntry entry : commandCatalog.entries(CustomCommandRegistry.getInstance().getCustomCommands())) {
            byId.put(entry.id(), entry);
        }
        return byId;
    }

    /** Builds "situation - place", where the place is the most specific known name for the situation. */
    private String describe(PlayerSituation situation, LocationDto location) {
        String label = getText(situation.i18nKey());
        String place = placeName(situation, location);
        return place == null ? label : label + " · " + place;
    }

    /**
     * Picks the most relevant place name for the situation: the station when docked or on foot inside one, the
     * body when on/near a planet surface, otherwise the star system. Falls back down the chain when the
     * preferred name is missing.
     */
    private String placeName(PlayerSituation situation, LocationDto location) {
        if (location == null) return null;
        String station = trimToNull(location.getStationName());
        String body = trimToNull(location.getPlanetShortName());
        String system = trimToNull(location.getStarName());
        return switch (situation) {
            case IN_SHIP_DOCKED, ON_FOOT_STATION, ON_FOOT_HANGAR, ON_FOOT_SOCIAL -> firstNonNull(station, system);
            case IN_SHIP_LANDED, ON_FOOT_PLANET, IN_SRV, IN_SHIP_ORBIT, IN_SHIP_GLIDE -> firstNonNull(body, system);
            default -> system;
        };
    }

    private static String firstNonNull(String preferred, String fallback) {
        return preferred != null ? preferred : fallback;
    }
}
