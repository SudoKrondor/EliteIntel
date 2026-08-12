package elite.intel.ui.screen;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.actions.catalog.CommandCatalog;
import elite.intel.ai.brain.actions.catalog.CommandCatalogEntry;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandDefinition;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandRegistry;
import elite.intel.ai.brain.vega.model.IntelActionCategory;
import elite.intel.ai.brain.vega.prompt.GameToolCandidates;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.i18n.EventsTextProvider;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import elite.intel.ui.dialog.CommandDetailsDialog;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.event.CustomCommandsSummaryChangedEvent;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudTable;
import elite.intel.ui.widget.HudTwoColumns;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * The "Built-in Commands" tab, under Actions: what this build can do, and which of it the commander can use
 * where they are.
 * <p>
 * At the top of the page a two-column row (canon 9, equal halves + centred divider) pairs a scope picker with
 * the concrete place for it (read-only: station / body / system); below it sits a search box, full width.
 * <p>
 * The scope picker holds ALL plus every physical situation - in ship / SRV / fighter / taxi / on foot; docked,
 * landed, supercruise, ring, orbit, deep space. It auto-follows the live game situation until the commander
 * picks something themselves, after which it stays put; an undetermined situation shows ALL. ALL lists every
 * action this build has, including ones not usable right now; a situation lists only what is usable there.
 * <p>
 * The search box is a plain, literal text filter over the listed actions - name, action key, and the spoken
 * phrases that trigger them. It is deliberately not the companion's routing: that ranks by meaning, so it
 * would answer a typed word with commands that share none of it and no way to see why.
 * <p>
 * Below those, an "Available commands and queries" section (canon 9) holds one combined, alphabetically sorted
 * list of the commands (built-in actions plus custom-command macros) and queries in the chosen scope, laid out
 * row-major across three equal, header-less columns of a single read-only HUD table (canon 6). All of it
 * updates live off game events while the tab is showing.
 * <p>
 * This replaced a second, flat catalog table that listed the same built-in commands with a text filter. Both
 * listed the same entries and opened the same details dialog, so the one that also knows about context was
 * kept - and it took the other's search with it.
 */
public class BuiltInCommandsTabPanel extends JPanel {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final Status status = Status.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final CommandCatalog commandCatalog = new CommandCatalog();

    /** Number of equal columns the available-actions list is laid out across (row-major fill). */
    private static final int COLUMN_COUNT = 3;
    /**
     * The action categories this tab lists and highlights: built-in commands, macros, and queries.
     */
    private static final Set<IntelActionCategory> ACTION_CATEGORIES =
            Set.of(IntelActionCategory.ACTION, IntelActionCategory.MACRO, IntelActionCategory.QUERY);

    private JComboBox<Scope> situationCombo;
    private JTextField locationField;
    private JTextField searchField;
    private JTable actionsTable;
    private DefaultTableModel actionsModel;
    private HudSection actionsSection;
    /** Last situation the action table was built for; it rebuilds only when this changes (EDT-only field). */
    private PlayerSituation lastSituation;
    /** Last status flags rendered; the frequent Status tick is skipped while these are unchanged (event-thread-only). */
    private long lastFlags = -1L;
    private long lastFlags2 = -1L;
    /** Guards GameEventBus register/unregister so a hide without a prior show cannot throw or double-subscribe. */
    private boolean subscribed;
    /** Every action in the chosen scope, before the search box narrows it (EDT-only). */
    private List<ActionRow> visibleActionRows = List.of();
    /** True while we sync the situation picker to the live game situation, so its listener skips it (EDT-only). */
    private boolean syncingSituation;
    /**
     * True until the commander picks a scope by hand; the picker chases the live game only while set (EDT-only).
     */
    private boolean followLiveSituation = true;
    /** Cell currently under the mouse in the actions table (row/col), or -1/-1 for none (EDT-only). */
    private int hoverRow = -1;
    private int hoverCol = -1;

    public BuiltInCommandsTabPanel() {
        buildUi();
        // Subscribe to the (frequent) live Status event only while the tab is actually showing. This sits
        // inside a nested tab pane (Actions > Built-in Commands), and JTabbedPane hides the tab it switches
        // away from without touching its children's own visible flag - so componentHidden never fires for a
        // panel one level down and it would stay subscribed behind a different top-level tab. SHOWING_CHANGED
        // is the event that accounts for every ancestor, which is exactly the question being asked here.
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (isShowing()) {
                subscribe();
                refresh();
            } else {
                unsubscribe();
            }
        });
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        setBorder(hudScreenBorder());

        // Top of the page (no framed section): a two-column row splitting situation | place down the middle
        // (canon 9, HudTwoColumns), then the commander phrase full-width below.
        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        GridBagConstraints gbc = baseGbc();

        // Left column - scope picker: ALL, then every PlayerSituation (localized, capsed), UNKNOWN = "unknown".
        // It auto-follows the live game situation until the commander picks something themselves, after which it
        // stays put - otherwise the next status tick would drag them off the scope they chose (ALL above all)
        // while they were reading it. The selected scope, not the live game, drives the list (see availableRows).
        JPanel situationCol = transparentPanel(new GridBagLayout());
        GridBagConstraints sgc = baseGbc();
        addLabel(situationCol, EventsTextProvider.getText("game.situation.label"), sgc);
        situationCombo = new HudComboBox<>(scopeChoices(), Scope::label);
        situationCombo.setSelectedItem(Scope.ALL); // nothing known about the game yet: show everything
        situationCombo.addActionListener(e -> {
            if (syncingSituation) {
                return; // our own live-sync; it re-filters explicitly, so skip the duplicate here
            }
            followLiveSituation = false; // a deliberate pick owns the picker from here
            rebuildAvailableActionsForSelection();
        });
        addField(situationCol, situationCombo, sgc, 1, 1.0);

        // Right column - the concrete place for that situation (read-only). Tight label (width 0): the place
        // field is unpaired (nothing below it to line up with), so it hugs its short label instead of leaving
        // the wide fixed-width gap the aligned left column needs.
        JPanel placeCol = transparentPanel(new GridBagLayout());
        GridBagConstraints pgc = baseGbc();
        addLabel(placeCol, getText("location.field.place"), pgc, 0);
        locationField = makeTextField();
        locationField.setEditable(false); // read-only readout: a bounded surface, not an input (HUD canon 5.1)
        addField(placeCol, locationField, pgc, 1, 1.0);

        // Equal halves + centred divider (canon 9); each column top-aligned via a NORTH wrap.
        JPanel situationWrap = transparentPanel(new BorderLayout());
        situationWrap.add(situationCol, BorderLayout.NORTH);
        JPanel placeWrap = transparentPanel(new BorderLayout());
        placeWrap.add(placeCol, BorderLayout.NORTH);
        addSpanComponent(top, new HudTwoColumns(situationWrap, placeWrap), gbc);

        // Search row - the same nested label+field column as the two above, added full-width, so its input's
        // left edge lines up exactly with the situation combo's (identical label width and nesting insets).
        nextRow(gbc);
        JPanel searchCol = transparentPanel(new GridBagLayout());
        GridBagConstraints fgc = baseGbc();
        addLabel(searchCol, getText("actions.commands.search.label"), fgc);
        // In-field info-"i" (HUD section 5.1) explaining what this field searches.
        searchField = makeTextField(this::showSearchInfo);
        // Plain text search: every edit narrows the list below to the actions whose name, id, or spoken
        // phrases contain what was typed. Literal substring matching, so what is typed is what is looked for.
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyActionRows();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyActionRows();
            }

            @Override
            public void changedUpdate(DocumentEvent e) { applyActionRows();
            }
        });
        addField(searchCol, searchField, fgc, 1, 1.0);
        addSpanComponent(top, searchCol, gbc);

        add(top, BorderLayout.NORTH);

        // "Available commands and queries": one combined list laid out across COLUMN_COUNT equal, header-less
        // read-only columns of a single HUD table (canon 6), filled row-major so reading left-to-right,
        // top-to-bottom follows the sorted order. One vertical scrollbar drives the whole table (canon 8).
        actionsModel = readOnlyModel();
        actionsTable = new JTable(actionsModel);
        actionsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS); // keep the columns equal as the table resizes
        actionsTable.setCellSelectionEnabled(true);                     // selection/open is per cell, not per row
        actionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        styleActionsTable(actionsTable);
        installCellHover(actionsTable);
        installCommandDetailsOpen(actionsTable);

        JScrollPane scroll = hudScrollPane(actionsTable);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Data-plane treatment (canon 8): the table floats on the app background, no frame around the scroll.
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
        SwingUtilities.invokeLater(() -> styleActionsTable(actionsTable));
    }

    /** Read-only table model with {@link #COLUMN_COUNT} untitled columns (this tab shows no column headers). */
    private static DefaultTableModel readOnlyModel() {
        return new DefaultTableModel(new Object[COLUMN_COUNT], 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * Applies the shared HUD table look (canon 6): capsed value renderer, style locked against re-theming.
     * {@link HudTable#style} configures the column header, but this tab shows none, so a throwaway header is
     * lent for styling and then dropped (recreated each call because {@code addNotify} re-styles after the
     * palette walk).
     */
    private void styleActionsTable(JTable table) {
        if (table.getTableHeader() == null) {
            table.setTableHeader(new JTableHeader(table.getColumnModel()));
        }
        HudTable.style(table);
        table.setDefaultRenderer(Object.class, new ActionHighlightRenderer());
        table.putClientProperty(HUD_TABLE_STYLE_LOCKED, Boolean.TRUE);
        table.setTableHeader(null); // no column names on this tab
    }

    private void installCommandDetailsOpen(JTable table) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                openCommandDetailsAt(table, table.rowAtPoint(event.getPoint()), table.columnAtPoint(event.getPoint()));
            }
        });
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ENTER"), "openBuiltInCommandDetails");
        table.getActionMap().put("openBuiltInCommandDetails", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                openCommandDetailsAt(table, table.getSelectedRow(), table.getSelectedColumn());
            }
        });
    }

    /** Tracks the cell under the mouse so the renderer can hover-highlight just that cell (not the whole row). */
    private void installCellHover(JTable table) {
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                setHoveredCell(table, table.rowAtPoint(event.getPoint()), table.columnAtPoint(event.getPoint()));
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                setHoveredCell(table, -1, -1);
            }
        });
    }

    private void setHoveredCell(JTable table, int row, int col) {
        // Only a cell holding an action hovers; empty filler cells (short last row) do not.
        boolean real = row >= 0 && col >= 0 && table.getValueAt(row, col) instanceof ActionRow;
        int newRow = real ? row : -1;
        int newCol = real ? col : -1;
        if (newRow == hoverRow && newCol == hoverCol) {
            return;
        }
        int prevRow = hoverRow;
        int prevCol = hoverCol;
        hoverRow = newRow;
        hoverCol = newCol;
        repaintCell(table, prevRow, prevCol);
        repaintCell(table, newRow, newCol);
    }

    private void repaintCell(JTable table, int row, int col) {
        if (row < 0 || col < 0 || row >= table.getRowCount() || col >= table.getColumnCount()) {
            return;
        }
        table.repaint(table.getCellRect(row, col, false));
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
        String place = caps(placeName(situation, location));
        SwingUtilities.invokeLater(() -> {
            locationField.setText(place);
            // The available-actions table is context-gated, so re-sync (and rebuild) only when the situation
            // changes (cheap), not on every status change. Syncing the picker to the live situation is what
            // drives the rebuild - the selected situation, not the live game, is the filter's source of truth.
            if (situation != lastSituation) {
                lastSituation = situation;
                syncSituationToLive(situation);
            }
        });
    }

    /**
     * Rebuilds the available-actions table when the set of custom-command macros changes (added, removed, or
     * reloaded, including a macro created by voice while this tab is open), so the list stays current without a
     * situation change or a tab switch.
     */
    @Subscribe
    public void onCustomCommandsChanged(CustomCommandsSummaryChangedEvent event) {
        SwingUtilities.invokeLater(this::rebuildAvailableActionsForSelection);
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
        locationField.setText(caps(placeName(situation, location)));
        lastSituation = situation;
        followLiveSituation = true; // a fresh look at the tab starts from where the commander actually is
        syncSituationToLive(situation);
    }

    /**
     * Selects the picker to the live game situation without firing its listener (which would rebuild once more),
     * then rebuilds the available actions for that selection. The programmatic-select guard mirrors the phrase
     * field's echo guard; the explicit rebuild covers the case where the selection is unchanged (a no-op select
     * fires no event) yet the list still needs a refresh (e.g. on tab show or a macro-set change).
     */
    private void syncSituationToLive(PlayerSituation situation) {
        if (!followLiveSituation) {
            return; // the commander chose a scope; the game does not get to change it under them
        }
        syncingSituation = true;
        try {
            situationCombo.setSelectedItem(scopeFor(situation));
        } finally {
            syncingSituation = false;
        }
        rebuildAvailableActionsForSelection();
    }

    /**
     * The scope to show for a live situation. An undetermined situation - the game not running, or not yet
     * read - falls back to ALL rather than to an empty list: with no idea where the commander is, every action
     * is the honest answer, and an empty tab reads as a broken one.
     */
    static Scope scopeFor(PlayerSituation situation) {
        return situation == null || situation == PlayerSituation.UNKNOWN ? Scope.ALL : Scope.of(situation);
    }

    /** Rebuilds the available actions for the scope currently chosen in the picker. */
    private void rebuildAvailableActionsForSelection() {
        rebuildAvailableActions((Scope) situationCombo.getSelectedItem());
    }

    /**
     * Opens the in-field info-"i" (HUD section 5.1) help for the search field.
     */
    private void showSearchInfo() {
        HudConfirmDialog.info(
                this,
                getText("actions.commands.search.label"),
                getText("actions.commands.search.info"),
                getText("button.ok"));
    }

    private LocationDto currentLocation() {
        return locationManager.findByLocationData(playerSession.getLocationData());
    }

    /**
     * Rebuilds the actions table from one combined list of everything in the given scope (commands, macros,
     * queries), laid out row-major across the {@link #COLUMN_COUNT} columns (see {@link #applyActionRows}).
     * UNKNOWN clears the table; ALL lists everything this build has.
     */
    private void rebuildAvailableActions(Scope scope) {
        if (scope == null || scope.isSituation(PlayerSituation.UNKNOWN)) {
            clearAvailableActions();
            return;
        }
        visibleActionRows = availableRows(ACTION_CATEGORIES, scope);
        applyActionRows();
    }

    private void clearAvailableActions() {
        visibleActionRows = List.of();
        applyActionRows();
    }

    /**
     * Narrows the scope's actions by the search box, sorts them alphabetically, and lays them out row-major
     * across the {@link #COLUMN_COUNT} columns: cell (r, c) holds item {@code r * COLUMN_COUNT + c}, so reading
     * left-to-right, top-to-bottom follows the sorted order and the columns stay balanced (the short last row
     * trails empty cells).
     */
    private void applyActionRows() {
        List<ActionRow> ordered = new ArrayList<>(
                matching(visibleActionRows, searchField == null ? "" : searchField.getText()));
        ordered.sort(Comparator.comparing(ActionRow::name, String.CASE_INSENSITIVE_ORDER));
        updateAvailableActionsTitle(ordered.size());
        int rowCount = (ordered.size() + COLUMN_COUNT - 1) / COLUMN_COUNT;
        actionsModel.setRowCount(0);
        for (int r = 0; r < rowCount; r++) {
            Object[] cells = new Object[COLUMN_COUNT];
            for (int c = 0; c < COLUMN_COUNT; c++) {
                int index = r * COLUMN_COUNT + c;
                cells[c] = index < ordered.size() ? ordered.get(index) : null;
            }
            actionsModel.addRow(cells);
        }
    }

    /**
     * The rows a search matches: a literal, case-insensitive substring of the action's name, its id, or the
     * spoken phrases that trigger it. Deliberately plain - what is typed is what is looked for - because this
     * is a reference list the commander reads, not the routing the companion performs on a spoken phrase.
     * A blank search matches everything. Static and package-private so it can be tested without a screen.
     */
    static List<ActionRow> matching(List<ActionRow> rows, String search) {
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return rows;
        }
        return rows.stream().filter(row -> row.searchText().contains(query)).toList();
    }

    private void updateAvailableActionsTitle(int count) {
        if (actionsSection != null) {
            actionsSection.setTitle(availableActionsTitle(count));
        }
    }

    private String availableActionsTitle(int count) {
        String base = getText("location.section.availableActions");
        return count <= 0 ? base : base + " (" + count + ")";
    }

    /**
     * Localized display names of the actions in the given scope. {@link GameToolCandidates} owns "what exists"
     * (and, for a single situation, "what is usable there"); this only maps each surviving id to its localized
     * name and the text a search looks through.
     */
    private List<ActionRow> availableRows(Set<IntelActionCategory> categories, Scope scope) {
        Map<String, String> nameById = nameIndex();
        List<GameToolCandidates.Candidate> candidates = scope.isAll()
                ? new GameToolCandidates(Status.detached(PlayerSituation.UNKNOWN)).collectIgnoringVisibility(categories)
                : new GameToolCandidates(Status.detached(scope.situation())).collect(categories);
        return candidates.stream()
                .map(candidate -> ActionRow.of(
                        candidate.id(),
                        nameById.getOrDefault(candidate.id(), candidate.id()),
                        candidate.localizedAliasGroup()))
                .sorted((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.name(), right.name()))
                .toList();
    }

    /**
     * One action in the list. {@code searchText} is the lower-cased haystack a search looks through - the
     * display name, the id, and the spoken phrases - built once here rather than on every keystroke.
     */
    record ActionRow(String id, String name, String searchText) {

        static ActionRow of(String id, String name, String phrases) {
            String haystack = (name + " " + id + " " + (phrases == null ? "" : phrases)).toLowerCase(Locale.ROOT);
            return new ActionRow(id, name, haystack);
        }

        @Override public String toString() {
            return name;
        }
    }

    /**
     * One entry in the scope picker: either every action this build has, or only those usable in one situation.
     * <p>
     * A wrapper rather than an extra {@link PlayerSituation} constant, because ALL is a way of looking at the
     * list and not a place the commander can be - the game's own situation enum has no business carrying it.
     */
    record Scope(PlayerSituation situation) {

        static final Scope ALL = new Scope(null);

        static Scope of(PlayerSituation situation) {
            return new Scope(situation);
        }

        boolean isAll() {
            return situation == null;
        }

        boolean isSituation(PlayerSituation other) {
            return situation == other;
        }

        String label() {
            return isAll()
                    ? caps(getText("actions.commands.scope.all"))
                    : caps(EventsTextProvider.getText(situation.i18nKey()));
        }
    }

    /**
     * ALL first, then every situation in enum order. UNKNOWN is left out on purpose: it is the game saying it
     * cannot tell where the commander is, which is not something to filter a reference list by - ALL is what
     * that means here, and an entry that always showed an empty list would only look broken.
     */
    static Scope[] scopeChoices() {
        List<Scope> choices = new ArrayList<>();
        choices.add(Scope.ALL);
        for (PlayerSituation situation : PlayerSituation.values()) {
            if (situation != PlayerSituation.UNKNOWN) {
                choices.add(Scope.of(situation));
            }
        }
        return choices.toArray(new Scope[0]);
    }

    /**
     * Per-cell renderer: an empty filler cell blends into the app background (no tile); a hovered action cell
     * gets the hover tile. Selection styling is left to the shared {@link HudTable.CellRenderer}.
     */
    private final class ActionHighlightRenderer extends HudTable.ValueCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                return component;
            }
            if (!(value instanceof ActionRow)) {
                component.setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND); // empty slot, not a data tile
                return component;
            }
            if (row == hoverRow && column == hoverCol) {
                component.setBackground(HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND);
            }
            return component;
        }
    }

    private void openCommandDetailsAt(JTable table, int viewRow, int viewColumn) {
        if (viewRow < 0 || viewColumn < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        int modelColumn = table.convertColumnIndexToModel(viewColumn);
        Object value = table.getModel().getValueAt(modelRow, modelColumn);
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

    private static String caps(String text) {
        return text == null ? "" : text.toUpperCase(Locale.ROOT);
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
