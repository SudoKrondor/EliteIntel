package elite.intel.ui.screen;

import com.google.common.eventbus.Subscribe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import elite.intel.ai.brain.actions.catalog.CommandCatalog;
import elite.intel.ai.brain.actions.catalog.CommandCatalogEntry;
import elite.intel.ai.brain.actions.customcommand.CustomCommandDefinition;
import elite.intel.ai.brain.actions.customcommand.CustomCommandRegistry;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.prompt.GameToolCandidates;
import elite.intel.companion.prompt.SemanticActionReducer;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.NormalizedUserInputEvent;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.i18n.EventsTextProvider;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import elite.intel.ui.dialog.CommandDetailsDialog;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.event.CommanderMatchInputChangedEvent;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudPalette.*;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * The "Help" tab. At the top of the page a two-column row (canon 9, equal halves + centred divider) pairs a
 * situation picker (an editable combo of every physical situation - in ship / SRV / fighter / taxi / on foot;
 * docked, landed, supercruise, ring, orbit, deep space; UNKNOWN when undetermined) with the concrete place for
 * it (read-only: station / body / system); below it sits the commander's current spoken phrase in an editable
 * field. The picker auto-follows the live game situation
 * but can be changed by hand, and the selected situation - not the live game - is what gates the list below; a
 * phrase edit re-runs the reducer over the field's text and re-highlights the matching actions (reducer-only,
 * no LLM turn, so both filters work with no LLM connected). Below those, an "Available commands and queries"
 * section (canon 9) holds one combined, alphabetically sorted list of the commands (built-in actions plus
 * custom-command macros) and queries available in the selected situation, laid out row-major across three
 * equal, header-less columns of a single read-only HUD table (canon 6). All of it updates live off game events
 * while the tab is showing (the table rebuilds when the selected situation changes).
 */
public class HelpTabPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(HelpTabPanel.class);

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final Status status = Status.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final CommandCatalog commandCatalog = new CommandCatalog();
    /** Situation the highlight reducer's candidates are gated by (read off the EDT by the background worker). */
    private volatile PlayerSituation highlightSituation = PlayerSituation.UNKNOWN;
    /**
     * Reducer that highlights the actions matching the typed phrase. Its candidate visibility is gated by the
     * PICKED situation ({@link #highlightSituation}) and the CURRENT language, so the highlight follows the
     * "where I am" picker exactly like the available-actions list — unlike the running companion reducer, whose
     * candidates are frozen to the live game status and to the companion-start language.
     */
    private final SemanticActionReducer highlightReducer =
            SemanticActionReducer.withCandidateSource(this::highlightCandidates);

    /** Number of equal columns the available-actions list is laid out across (row-major fill). */
    private static final int COLUMN_COUNT = 3;
    /** The action categories the Help tab lists and highlights: built-in commands, macros, and queries. */
    private static final Set<IntelActionCategory> ACTION_CATEGORIES =
            Set.of(IntelActionCategory.ACTION, IntelActionCategory.MACRO, IntelActionCategory.QUERY);

    private JComboBox<PlayerSituation> situationCombo;
    private JTextField locationField;
    private JTextField phraseField;
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
    /** Action ids selected by the semantic reducer for the current commander phrase (EDT-only). */
    private Set<String> highlightedActionIds = Set.of();
    /** Monotonic token used to ignore stale background semantic-selection results. */
    private int highlightRequest;
    /** Current available action rows before highlight ordering is applied (EDT-only). */
    private List<ActionRow> visibleActionRows = List.of();
    /** True while we set the phrase field ourselves, so its edit listener skips our echo (EDT-only). */
    private boolean settingPhraseText;
    /** True while we sync the situation picker to the live game situation, so its listener skips it (EDT-only). */
    private boolean syncingSituation;
    /** Cell currently under the mouse in the actions table (row/col), or -1/-1 for none (EDT-only). */
    private int hoverRow = -1;
    private int hoverCol = -1;

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

        // Top of the page (no framed section): a two-column row splitting situation | place down the middle
        // (canon 9, HudTwoColumns), then the commander phrase full-width below.
        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        GridBagConstraints gbc = baseGbc();

        // Left column - situation picker: selectable list of every PlayerSituation (localized, capsed),
        // UNKNOWN = "unknown". It auto-follows the live game situation, but a manual pick re-filters the
        // available actions below for that what-if context - the selected situation, not the live game, drives
        // the filter (see availableRows).
        JPanel situationCol = transparentPanel(new GridBagLayout());
        GridBagConstraints sgc = baseGbc();
        addLabel(situationCol, EventsTextProvider.getText("game.situation.label"), sgc);
        situationCombo = new HudComboBox<>(PlayerSituation.values(),
                s -> caps(EventsTextProvider.getText(s.i18nKey())));
        situationCombo.setSelectedItem(PlayerSituation.UNKNOWN); // no context known until the first refresh/sync
        situationCombo.addActionListener(e -> {
            if (syncingSituation) {
                return; // our own live-sync; it re-filters explicitly, so skip the duplicate here
            }
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

        // Phrase row - the same nested label+field column as the two above, added full-width, so its input's
        // left edge lines up exactly with the situation combo's (identical label width and nesting insets).
        nextRow(gbc);
        JPanel phraseCol = transparentPanel(new GridBagLayout());
        GridBagConstraints fgc = baseGbc();
        addLabel(phraseCol, getText("location.field.phrase"), fgc);
        // In-field info-"i" (HUD section 5.1) explaining what this field is for.
        phraseField = makeTextField(this::showPhraseInfo);
        // Editable input, not a read-out: every edit re-runs the reducer over the field's current text and
        // re-highlights the matching available actions below - the same filtering a spoken phrase drives, but
        // reducer-only (no LLM turn), so it works even with no LLM connected. Guarded against our own
        // programmatic echo (see setPhraseText) so an incoming phrase written back into the field does not run
        // the reducer twice.
        phraseField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onPhraseEdited(); }
            @Override public void removeUpdate(DocumentEvent e) { onPhraseEdited(); }
            @Override public void changedUpdate(DocumentEvent e) { onPhraseEdited(); }
        });
        addField(phraseCol, phraseField, fgc, 1, 1.0);
        addSpanComponent(top, phraseCol, gbc);

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
                .put(KeyStroke.getKeyStroke("ENTER"), "openHelpCommandDetails");
        table.getActionMap().put("openHelpCommandDetails", new AbstractAction() {
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
     * Live update of the commander's current spoken phrase (the normalized voice input), fired off the game
     * event thread; the field update is marshaled onto the EDT.
     * <p>
     * In companion mode the authoritative {@link CommanderMatchInputChangedEvent} follows this for the same
     * utterance and drives the reducer highlight on the exact match input, so re-running the reducer here would
     * recompute (and log) the same reduce twice. Legacy command mode never fires that event, so this remains the
     * only highlight trigger there - hence the reducer runs here only when the companion runtime is absent.
     */
    @Subscribe
    public void onCommanderPhrase(NormalizedUserInputEvent event) {
        String text = event.getText() == null ? "" : event.getText();
        SwingUtilities.invokeLater(() -> {
            setPhraseText(text);
            if (!companionRunning()) {
                updateSemanticHighlights(text);
            }
        });
    }

    /**
     * Runs on every edit of the phrase field: re-runs the reducer over the field's current text to re-highlight
     * the matching available actions (reducer-only, no LLM turn). Our own programmatic echo of an incoming
     * phrase (see {@link #setPhraseText}) is skipped so the reducer does not run twice for it.
     */
    private void onPhraseEdited() {
        if (settingPhraseText) {
            return; // our echo of an incoming phrase; the phrase handler already ran the reducer for it
        }
        updateSemanticHighlights(phraseField.getText());
    }

    /**
     * Sets the phrase field from an incoming (voice / companion) phrase without tripping the edit listener that
     * would re-submit it: the flag makes {@link #onPhraseEdited} treat the change as an echo, and we only write
     * when the text actually differs so a matching echo does not churn the document or jump the caret while the
     * commander is typing. EDT-only.
     */
    private void setPhraseText(String text) {
        String value = text == null ? "" : text;
        if (value.equals(phraseField.getText())) {
            return;
        }
        settingPhraseText = true;
        try {
            phraseField.setText(value);
        } finally {
            settingPhraseText = false;
        }
    }

    /** Opens the in-field info-"i" (HUD section 5.1) help for the commander-phrase field. */
    private void showPhraseInfo() {
        HudConfirmDialog.info(
                this,
                getText("location.field.phrase"),
                getText("location.field.phrase.info"),
                getText("button.ok"));
    }

    /** Live update from the companion path: this is the exact match input used by the companion reducer. */
    @Subscribe
    public void onCommanderMatchInputChanged(CommanderMatchInputChangedEvent event) {
        String text = event.text() == null ? "" : event.text();
        SwingUtilities.invokeLater(() -> {
            setPhraseText(text);
            updateSemanticHighlights(text);
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
        setPhraseText(currentCommanderMatchInput());
        lastSituation = situation;
        syncSituationToLive(situation);
    }

    /**
     * Selects the picker to the live game situation without firing its listener (which would rebuild once more),
     * then rebuilds the available actions for that selection. The programmatic-select guard mirrors the phrase
     * field's echo guard; the explicit rebuild covers the case where the selection is unchanged (a no-op select
     * fires no event) yet the list still needs a refresh (e.g. on tab show or a macro-set change).
     */
    private void syncSituationToLive(PlayerSituation situation) {
        syncingSituation = true;
        try {
            situationCombo.setSelectedItem(situation);
        } finally {
            syncingSituation = false;
        }
        rebuildAvailableActionsForSelection();
    }

    /** Rebuilds the available actions for the situation currently chosen in the picker (the filter's context). */
    private void rebuildAvailableActionsForSelection() {
        rebuildAvailableActions((PlayerSituation) situationCombo.getSelectedItem());
    }

    private LocationDto currentLocation() {
        return locationManager.findByLocationData(playerSession.getLocationData());
    }

    private String currentCommanderMatchInput() {
        try {
            return CompanionRuntime.state().lastCommanderMatchInput();
        } catch (IllegalStateException ignored) {
            // WHY: companion runtime not installed yet (e.g. before services start); no match input to show.
            return "";
        }
    }

    /**
     * Rebuilds the actions table from one combined list of everything available in the given situation
     * (commands, macros, queries), laid out row-major across the {@link #COLUMN_COUNT} columns (see
     * {@link #applyActionRows}). UNKNOWN clears the table.
     */
    private void rebuildAvailableActions(PlayerSituation situation) {
        // Gate the highlight reducer by the same picked situation that gates the list below (see highlightReducer).
        highlightSituation = situation;
        if (situation == null || situation == PlayerSituation.UNKNOWN) {
            clearAvailableActions();
            return;
        }
        visibleActionRows = availableRows(ACTION_CATEGORIES, situation);
        updateAvailableActionsTitle();
        applyActionRows();
        updateSemanticHighlights(phraseField.getText());
    }

    private void clearAvailableActions() {
        visibleActionRows = List.of();
        updateAvailableActionsTitle();
        actionsModel.setRowCount(0);
        highlightedActionIds = Set.of();
        ++highlightRequest;
        actionsTable.repaint();
    }

    /**
     * Sorts the visible actions (highlighted first, then alphabetically) and lays them out row-major across the
     * {@link #COLUMN_COUNT} columns: cell (r, c) holds item {@code r * COLUMN_COUNT + c}, so reading
     * left-to-right, top-to-bottom follows the sorted order and the columns stay balanced (the short last row
     * trails empty cells).
     */
    private void applyActionRows() {
        List<ActionRow> ordered = new ArrayList<>(visibleActionRows);
        ordered.sort(Comparator
                .comparing((ActionRow row) -> !highlightedActionIds.contains(row.id()))
                .thenComparing(ActionRow::name, String.CASE_INSENSITIVE_ORDER));
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

    private void updateAvailableActionsTitle() {
        if (actionsSection != null) {
            actionsSection.setTitle(availableActionsTitle(visibleActionRows.size()));
        }
    }

    private String availableActionsTitle(int count) {
        String base = getText("location.section.availableActions");
        return count <= 0 ? base : base + " (" + count + ")";
    }

    /**
     * Localized, alphabetically sorted display names of the actions offered for the given categories in the
     * given situation. {@link GameToolCandidates} owns "what is available" (context visibility plus the internal
     * fallback-id exclusions); we gate it against a {@link Status#detached} status standing in for the chosen
     * situation, so the list reflects the picked context rather than the live game. This only maps each
     * surviving id to its localized name.
     */
    private List<ActionRow> availableRows(Set<IntelActionCategory> categories, PlayerSituation situation) {
        Status situationStatus = Status.detached(situation);
        Map<String, String> nameById = nameIndex();
        return new GameToolCandidates(situationStatus).collect(categories).stream()
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
        new SwingWorker<Set<String>, Void>() {
            @Override protected Set<String> doInBackground() {
                List<LlmToolDefinition> tools = highlightReducer.selectTools(ACTION_CATEGORIES, phrase);
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
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    highlightedActionIds = Set.of();
                } catch (ExecutionException e) {
                    log.debug("Reducer highlight computation failed", e.getCause());
                    highlightedActionIds = Set.of();
                }
                applyActionRows();
            }
        }.execute();
    }

    /**
     * Candidates for {@link #highlightReducer}: gated by the picked situation (a what-if {@link Status#detached}
     * context, so the highlight matches the available-actions list) and built fresh each call, so it reads the
     * current command language rather than a frozen one. Empty for UNKNOWN. Runs off the EDT (background worker).
     */
    private List<GameToolCandidates.Candidate> highlightCandidates(Set<IntelActionCategory> categories) {
        PlayerSituation situation = highlightSituation;
        if (situation == null || situation == PlayerSituation.UNKNOWN) {
            return List.of();
        }
        return new GameToolCandidates(Status.detached(situation)).collect(categories);
    }

    /** True while the companion runtime is installed (companion mode); false in legacy command mode. */
    private boolean companionRunning() {
        try {
            CompanionRuntime.reducer();
            return true;
        } catch (IllegalStateException notRunning) {
            return false;
        }
    }

    private record ActionRow(String id, String name) {
        @Override public String toString() {
            return name;
        }
    }

    /**
     * Per-cell renderer: an empty filler cell blends into the app background (no tile); a hovered action cell
     * gets the hover tile; an action selected by the reducer gets the success foreground. Selection styling is
     * left to the shared {@link HudTable.CellRenderer}.
     */
    private final class ActionHighlightRenderer extends HudTable.ValueCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                return component;
            }
            if (!(value instanceof ActionRow actionRow)) {
                component.setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND); // empty slot, not a data tile
                return component;
            }
            if (row == hoverRow && column == hoverCol) {
                component.setBackground(HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND);
            }
            if (highlightedActionIds.contains(actionRow.id())) {
                component.setForeground(HUD_COLOR_ROLE_SUCCESS);
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
