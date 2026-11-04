package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devices.CaptureControllers;
import elite.intel.bindforge.io.*;
import elite.intel.bindforge.model.*;
import elite.intel.bindforge.rules.AiAccess;
import elite.intel.bindforge.rules.BindingConflictScanner;
import elite.intel.bindforge.rules.KeyboardKeyAvailabilityService;
import elite.intel.bindforge.rules.MissingBindingAutoAssigner;
import elite.intel.bindforge.storage.BindsEdits;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.EditedDeviceLabels;
import elite.intel.bindforge.storage.Edits;
import elite.intel.ui.dialog.AssignAxisDialog;
import elite.intel.ui.dialog.AssignKeyboardBindingDialog;
import elite.intel.ui.dialog.ClearKeyboardBindingDialog;
import elite.intel.ui.support.*;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudFooter;
import elite.intel.ui.widget.HudPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * The Bind Editor's Game Mode: the Used/Missing Bindings tables, split by the game's sections, with conflict detection,
 * hover callouts, the view filters and Auto-Assign VEGA Required Binds. {@link BindingProfilePanel}, the shell, reads the
 * binds file and hands over what it found ({@link #show}); this panel shows it and turns what the commander does into
 * edits not applied.
 * <p>
 * It also supplies the Anomalies mode's view ({@link #anomaliesView()}): the same rows and the same dialogs, narrowed to
 * the rows drawn red, so that a row can never be red in one place and missing from the other.
 * <p>
 * Axis rows sit among the button rows, as the game lists them. VEGA never presses one and no keyboard key can go on one,
 * so they have no AI access, take no auto-fix, and open {@link AssignAxisDialog} rather than the button dialog.
 * <p>
 * Nothing here writes a file (storage.md rule 6), and one change runs at a time across the whole Bind Editor - so
 * changes go through {@link EditRunner}, the shell's.
 */
class GameModePanel extends JPanel {

    /**
     * Runs a change to the edits off the EDT, one at a time, and tells the commander if it fails. The shell's.
     */
    interface EditRunner {
        /**
         * A change to the edits is running - SAVE, CLEAR or Auto-Assign; a second waits for it.
         */
        boolean running();

        /**
         * Runs {@code change}, then {@code onEdt} on the EDT - or, if it failed and the commander was told, {@code afterFailure}.
         */
        void run(String title, Change change, Runnable onEdt, Runnable afterFailure);
    }

    /**
     * A change to the edits, run by {@link EditRunner}.
     */
    @FunctionalInterface
    interface Change {
        void run() throws IOException;
    }

    /**
     * What the shell's read found. {@code file} is {@code null} when nothing could be shown.
     */
    record Shown(String file, byte[] content, Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots,
                 GameKeyboardLayout layout, EditedDeviceLabels labels, BindsValues values) {
    }

    /**
     * A section tab. {@code section} is {@code null} for All, the one tab that also lists a control the name table does
     * not know yet, which has no section of its own to be found under.
     */
    private record SectionTab(String titleKey, BindingSection section) {
    }

    private static final List<SectionTab> SECTION_TABS = List.of(
            new SectionTab("bindings.sectionTab.all", null),
            new SectionTab("bindings.sectionTab.general", BindingSection.GENERAL),
            new SectionTab("bindings.sectionTab.ship", BindingSection.SHIP),
            new SectionTab("bindings.sectionTab.srv", BindingSection.SRV),
            new SectionTab("bindings.sectionTab.onFoot", BindingSection.ON_FOOT));

    private static final int SCROLL_UNIT_ROWS = 2;

    private final KeyBindingsParser parser = KeyBindingsParser.getInstance();
    private final KeyboardKeyAvailabilityService availabilityService = new KeyboardKeyAvailabilityService();
    private final BindingsWriter bindingsWriter = new BindingsWriter();
    /**
     * Reads controller inputs by the names and labels in the edits - read with the binds file, off the EDT.
     */
    private BindingSlotDisplayFormatter slotFormatter = new BindingSlotDisplayFormatter();
    /**
     * The names and labels {@link #slotFormatter} reads, for the capture dialog's controller tabs.
     */
    private EditedDeviceLabels labels = EditedDeviceLabels.NONE;
    private final BindingsSelectionController selectionController;
    private final BindingsGroupTableFactory tableFactory;
    private final MissingBindingAutoAssigner autoAssigner = new MissingBindingAutoAssigner();
    /**
     * What dialogs open over: the shell, so they sit where they always have.
     */
    private final JComponent dialogOwner;
    private final EditRunner editRunner;
    private final BindingSaveResultPresenter saveResultPresenter;

    private JPanel usedBindingsPanel;
    private JPanel missingBindingsPanel;
    private JScrollPane usedBindingsScrollPane;
    private JScrollPane missingBindingsScrollPane;
    private JTabbedPane tabs;
    private JTabbedPane sectionTabs;
    private JPanel anomaliesPanel;
    private JScrollPane anomaliesScrollPane;
    /**
     * Told how many rows are in conflict, for the Anomalies tab's title.
     */
    private final IntConsumer anomalyCount;
    private JButton fixAllButton;
    private JCheckBox conflictsOnlyCheck;
    /**
     * Narrows the grid to the controls Vega uses - the ones with something in the AI ACCESS column.
     */
    private JCheckBox vegaOnlyCheck;
    /**
     * What the shell's search field holds.
     */
    private String searchQuery = "";

    private Map<String, KeyBindingsParser.ReadOnlyBindingSlots> currentSlots = Map.of();
    private BindsValues currentValues = BindsValues.NONE;
    private GameKeyboardLayout currentKeyboardLayout = GameKeyboardLayout.OTHER;
    /**
     * Every conflict in the loaded file, indexed for the two things this panel asks: whether a row
     * is in conflict (RED coloring) and what to say in its hover callout. Recomputed on each load.
     * <p>
     * Grouped by chord rather than flattened because one binding can be in conflict on both its
     * slots at once, against different partners. Telling the commander about only one of them
     * leaves the other to be discovered in the cockpit.
     */
    private ConflictIndex conflicts = ConflictIndex.empty();
    /**
     * Each binding id mapped to its ship/SRV twin id(s) bound to a different key. Drives the CYAN
     * row tint and the soft "use the same key" callout - a recommendation, never a conflict.
     * Recomputed on each load.
     */
    private Map<String, Set<String>> recommendationsByBinding = Map.of();
    /**
     * The binds file shown, by file name - {@code null} when none could be read.
     */
    private String loadedFile;
    /**
     * Its bytes as the edits held them when it was read - what the capture dialog checks a key against.
     */
    private byte[] loadedContent;
    private boolean assignDialogOpen;

    GameModePanel(JComponent dialogOwner, EditRunner editRunner, IntConsumer anomalyCount) {
        this.dialogOwner = dialogOwner;
        this.editRunner = editRunner;
        this.anomalyCount = anomalyCount;
        selectionController = new BindingsSelectionController();
        tableFactory = new BindingsGroupTableFactory(
                selectionController, this::openCapture, this::autoFixSingleBinding,
                this::openClearDialog,
                // Lambda (not a bound method ref) so it reads the field live: the map is
                // reassigned on each load, and a bound method ref would pin the initial empty map.
                id -> conflicts.contains(id),
                id -> recommendationsByBinding.containsKey(id),
                this::buildRowCalloutContent);
        buildUi();
        saveResultPresenter = new BindingSaveResultPresenter(dialogOwner);
    }

    private void buildUi() {
        setOpaque(false);
        setLayout(new BorderLayout(2, SCREEN_TOP_GAP));

        usedBindingsPanel = groupedTablesPanel();
        missingBindingsPanel = groupedTablesPanel();
        usedBindingsScrollPane = groupedTablesScrollPane(usedBindingsPanel);
        missingBindingsScrollPane = groupedTablesScrollPane(missingBindingsPanel);

        tabs = AppTheme.makeCompactTabs();
        tabs.addTab(getText("bindings.usedBindings"), nestedTabContent(usedBindingsScrollPane));
        tabs.addTab(getText("bindings.missingBindings"), nestedTabContent(missingBindingsScrollPane));
        tabs.addChangeListener(e -> selectionController.clearSelection());
        add(tabs, BorderLayout.CENTER);

        // WHY a tab bar over one set of tables, its tabs empty: a tab per section would mean a Used and a Missing table
        // per section, and what they show is one list narrowed. Tabs rather than a segmented bar, for the counts: with a
        // search typed, the counts say which section holds the hits.
        sectionTabs = AppTheme.makeCompactTabs();
        for (SectionTab tab : SECTION_TABS) {
            sectionTabs.addTab(getText(tab.titleKey(), 0), transparentPanel(null));
        }
        sectionTabs.addChangeListener(e -> {
            selectionController.clearSelection();
            renderBindingTables();
            usedBindingsScrollPane.getVerticalScrollBar().setValue(0);
            missingBindingsScrollPane.getVerticalScrollBar().setValue(0);
        });
        add(sectionTabs, BorderLayout.NORTH);

        add(buildFooter(), BorderLayout.SOUTH);

        anomaliesPanel = groupedTablesPanel();
        anomaliesScrollPane = groupedTablesScrollPane(anomaliesPanel);
    }

    /**
     * The Anomalies mode: every row in conflict, whichever section or tab it is in. It takes the same search and opens
     * the same dialogs, because it is a view of this panel's rows.
     */
    JComponent anomaliesView() {
        return nestedTabContent(anomaliesScrollPane);
    }

    /**
     * Shows what the shell's read found. On the EDT.
     */
    void show(Shown read) {
        selectionController.resetTables();
        loadedFile = read.file();
        loadedContent = read.content();
        labels = read.labels();
        slotFormatter = new BindingSlotDisplayFormatter(labels);
        currentSlots = read.slots();
        currentValues = read.values();
        currentKeyboardLayout = read.layout();
        if (read.file() == null) {
            conflicts = ConflictIndex.empty();
            recommendationsByBinding = Map.of();
            renderGroupedTables(usedBindingsPanel, Map.of(), BindingsGroupTableFactory.RowAction.NONE, getText("bindings.column.action"));
            renderGroupedTables(missingBindingsPanel, Map.of(), BindingsGroupTableFactory.RowAction.NONE, getText("bindings.column.action"));
            tabs.setTitleAt(0, getText("bindings.usedBindings", 0));
            tabs.setTitleAt(1, getText("bindings.missingBindings", 0));
            showSectionCounts(List.of());
            renderAnomalies(currentAiAccess());
            fixAllButton.setEnabled(false);
            return;
        }
        conflicts = computeConflicts(currentSlots);
        recommendationsByBinding = computeRecommendations(parser.toExecutableSlots(currentSlots));
        renderBindingTables();
    }

    /**
     * Narrows the tables to {@code query}, as the shell's search field is typed in. On the EDT.
     */
    void search(String query) {
        searchQuery = query;
        renderBindingTables();
    }

    /**
     * A change to the edits has started: Auto-Assign waits. The read after it enables the button again if it should be.
     */
    void changeStarted() {
        fixAllButton.setEnabled(false);
    }

    private JPanel buildFooter() {
        fixAllButton = makeButtonSubtle(getText("bindings.button.fixAll.short"));
        fixAllButton.setToolTipText(getText("bindings.button.fixAll.tooltip"));
        fixAllButton.addActionListener(e -> fixAllMissing());

        conflictsOnlyCheck = makeCheckBox(getText("bindings.filter.conflictsOnly"), false);
        conflictsOnlyCheck.addActionListener(e -> renderBindingTables());

        vegaOnlyCheck = makeCheckBox(getText("bindings.filter.vegaOnly"), false);
        vegaOnlyCheck.setToolTipText(getText("bindings.filter.vegaOnly.tooltip"));
        vegaOnlyCheck.addActionListener(e -> renderBindingTables());

        // Non-modal footer: the filters and AUTO-ASSIGN VEGA REQUIRED BINDS on the right, no BACK. APPLY and DISCARD are
        // the bar's, above BindForge's tabs (Storage S5).
        return HudFooter.build(false, null, null,
                List.of(conflictsOnlyCheck, vegaOnlyCheck, fixAllButton));
    }

    private void fixAllMissing() {
        if (loadedFile == null || currentSlots.isEmpty() || editRunner.running()) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
                dialogOwner,
                getText("bindings.autofix.confirm.text"),
                getText("bindings.autofix.confirm.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        MissingBindingAutoAssigner.Plan plan = autoAssigner.planAll(currentSlots, BindingsMonitor.requiredControls(), currentKeyboardLayout);
        applyPlanInBackground(plan, outcome -> showBatchSummary(outcome, plan));
    }

    private void autoFixSingleBinding(String bindingId) {
        if (bindingId == null || bindingId.isBlank() || loadedFile == null || editRunner.running()
                || isAxis(bindingId)) {
            return;
        }
        MissingBindingAutoAssigner.Plan plan = autoAssigner.planOne(bindingId, currentSlots, currentKeyboardLayout);
        if (plan.edits().isEmpty() && plan.skipped().isEmpty()) {
            return; // already bound or unknown - nothing to do
        }
        applyPlanInBackground(plan, outcome -> showSingleResult(bindingId, outcome, plan));
    }

    /**
     * Makes a plan's edits off the EDT, as one change to the edits. Each edit is checked against the file as the
     * previous ones left it, so the conflict checks see them. The result dialog and the reload come back on the EDT.
     */
    private void applyPlanInBackground(MissingBindingAutoAssigner.Plan plan, Consumer<ApplyOutcome> onComplete) {
        if (plan.edits().isEmpty()) {
            // Nothing to change (every target was skipped); report without touching the edits.
            onComplete.accept(new ApplyOutcome(0, 0));
            return;
        }
        String file = loadedFile;
        AtomicReference<ApplyOutcome> outcome = new AtomicReference<>();
        editRunner.run(getText("bindings.autofix.result.title"),
                () -> Edits.stored().change(current -> applyPlan(current, file, plan, outcome)),
                () -> onComplete.accept(outcome.get()), () -> {
                });
    }

    /**
     * Each planned edit made to the binds file in turn; one refused is counted and the rest go on. What was saved and
     * refused goes in {@code outcome}. Pure but for that.
     */
    private ConfigurationFiles applyPlan(ConfigurationFiles current, String file, MissingBindingAutoAssigner.Plan plan,
                                         AtomicReference<ApplyOutcome> outcome) {
        int saved = 0;
        int failed = 0;
        ConfigurationFiles files = current;
        for (MissingBindingAutoAssigner.PlannedEdit edit : plan.edits()) {
            KeyboardBindingEdit kbe = new KeyboardBindingEdit(edit.bindingId(), edit.slotType(), edit.key());
            BindsEdits.Changed changed = BindsEdits.change(files, file, binds -> edit.modifier() == null
                    ? bindingsWriter.assignKeyboardKey(binds, kbe)
                    : bindingsWriter.assignKeyboardKeyWithModifier(binds, kbe, edit.modifier()));
            if (changed.result() == BindingSaveResult.SAVED) {
                saved++;
                files = changed.files();
            } else {
                failed++;
            }
        }
        outcome.set(new ApplyOutcome(saved, failed));
        return files;
    }

    private void showBatchSummary(ApplyOutcome outcome, MissingBindingAutoAssigner.Plan plan) {
        StringBuilder sb = new StringBuilder(getText("bindings.autofix.summary", outcome.saved()));
        if (outcome.failed() > 0) {
            sb.append('\n').append(getText("bindings.autofix.failed", outcome.failed()));
        }
        appendReason(sb, plan, MissingBindingAutoAssigner.SkipReason.BOTH_SLOTS_OCCUPIED, "bindings.autofix.skipped.bothSlots");
        appendReason(sb, plan, MissingBindingAutoAssigner.SkipReason.NO_FREE_KEY, "bindings.autofix.skipped.noFreeKey");
        appendReason(sb, plan, MissingBindingAutoAssigner.SkipReason.NO_EDITABLE_SLOT, "bindings.autofix.skipped.notEditable");
        appendReason(sb, plan, MissingBindingAutoAssigner.SkipReason.GAME_MENU_LEFT_UNBOUND, "bindings.autofix.skipped.gameMenu");
        appendReason(sb, plan, MissingBindingAutoAssigner.SkipReason.LEFT_UNBOUND_ON_PURPOSE, "bindings.autofix.skipped.onPurpose");

        JOptionPane.showMessageDialog(
                dialogOwner, sb.toString(), getText("bindings.autofix.result.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void appendReason(
            StringBuilder sb,
            MissingBindingAutoAssigner.Plan plan,
            MissingBindingAutoAssigner.SkipReason reason,
            String messageKey
    ) {
        long count = plan.skipped().stream().filter(s -> s.reason() == reason).count();
        if (count > 0) {
            sb.append('\n').append(getText(messageKey, count));
        }
    }

    private void showSingleResult(String bindingId, ApplyOutcome outcome, MissingBindingAutoAssigner.Plan plan) {
        if (outcome.saved() > 0) {
            JOptionPane.showMessageDialog(
                    dialogOwner,
                    getText("bindings.autofix.single.success", BindingDisplayNames.label(bindingId)),
                    getText("bindings.autofix.result.title"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (outcome.failed() > 0) {
            JOptionPane.showMessageDialog(
                    dialogOwner,
                    getText("bindings.change.failed"),
                    getText("bindings.autofix.result.title"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (plan.skipped().isEmpty()) {
            return;
        }
        String messageKey = switch (plan.skipped().get(0).reason()) {
            case BOTH_SLOTS_OCCUPIED -> "bindings.autofix.single.skipped.bothSlots";
            case NO_FREE_KEY -> "bindings.autofix.single.skipped.noFreeKey";
            case NO_EDITABLE_SLOT -> "bindings.autofix.single.skipped.notEditable";
            case GAME_MENU_LEFT_UNBOUND -> "bindings.autofix.single.skipped.gameMenu";
            case LEFT_UNBOUND_ON_PURPOSE -> "bindings.autofix.single.skipped.onPurpose";
        };
        JOptionPane.showMessageDialog(
                dialogOwner, getText(messageKey), getText("bindings.autofix.result.title"), JOptionPane.WARNING_MESSAGE);
    }

    private record ApplyOutcome(int saved, int failed) {
    }

    private JPanel groupedTablesPanel() {
        JPanel panel = transparentPanel(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(2, 0, 2, 0));
        return panel;
    }

    private JScrollPane groupedTablesScrollPane(JPanel panel) {
        JScrollPane scrollPane = hudScrollPane(panel);
        scrollPane.getViewport().setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(BindingsGroupTableFactory.TABLE_ROW_HEIGHT * SCROLL_UNIT_ROWS);
        scrollPane.setBorder(hudDataPlaneBorder());
        scrollPane.putClientProperty(HUD_SCROLL_STYLE_LOCKED, Boolean.TRUE);
        return scrollPane;
    }

    private JPanel nestedTabContent(JComponent content) {
        JPanel panel = transparentPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Buckets the given controls under the game's own section headings, each bucket in the order
     * the game lists its rows (see {@link BindingDisplayNames}), so a commander can read this
     * table and their control screen side by side.
     */
    private Map<BindingSection, List<Object[]>> groupedBindings(
            List<String> bindingIds,
            Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots,
            BindingsGroupTableFactory.RowAction rowAction,
            AiAccess aiAccess
    ) {
        Map<BindingSection, List<Object[]>> grouped = groupedRows();
        for (String bindingId : inGameOrder(bindingIds)) {
            KeyBindingsParser.ReadOnlyBindingSlots bindingSlots = slots.get(bindingId);
            BindsValues.Axis axis = currentValues.axes().get(bindingId);
            SlotCell primary = axis != null ? axisInputCell(axis) : slotCell(bindingSlots == null ? null : bindingSlots.primary());
            SlotCell secondary = axis != null ? axisSettingsCell(axis) : slotCell(bindingSlots == null ? null : bindingSlots.secondary());
            AiAccessCell ai = axis != null
                    ? aiAccessCell(AiAccess.State.NOT_USED, null)
                    : aiAccessCell(aiAccess.stateOf(bindingId, bindingSlots), bindingSlots);
            Object[] row = switch (rowAction) {
                case AUTO_FIX -> new Object[]{bindingId, primary, secondary, ai,
                        axis != null ? "" : getText("bindings.column.autofix.action")};
                case CLEAR -> new Object[]{bindingId, primary, secondary, ai, getText("bindings.column.clear.action")};
                case NONE -> new Object[]{bindingId, primary, secondary, ai};
            };
            grouped.get(BindingDisplayNames.lookup(bindingId).section()).add(row);
        }
        return grouped;
    }

    private SlotCell slotCell(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        return new SlotCell(slotFormatter.formatSlot(slot), slotFormatter.rawTokens(slot));
    }

    private boolean isAxis(String bindingId) {
        return currentValues.axes().containsKey(bindingId);
    }

    /**
     * An axis row's first cell: the axis it is on.
     */
    private SlotCell axisInputCell(BindsValues.Axis axis) {
        return axis.input()
                .map(input -> new SlotCell(slotFormatter.formatInputs(input.device(), List.of(), input.key()),
                        input.device() + " " + input.key()))
                .orElseGet(() -> new SlotCell(getText("bindings.status.notDefined"), null));
    }

    /**
     * An axis row's second cell. An axis has one binding, not two slots, so this column carries what varies on it
     * instead: Normal or Inverted, the dead zone and any throttle range. Blank while nothing is bound.
     */
    private SlotCell axisSettingsCell(BindsValues.Axis axis) {
        if (!axis.bound()) return new SlotCell("", null);
        List<String> parts = new ArrayList<>();
        parts.add(getText(axis.inverted() ? "bindings.axis.inverted" : "bindings.axis.normal"));
        if (axis.editable()) {
            parts.add(getText("bindings.axis.deadzone.value",
                    AssignAxisDialog.deadzoneText(Math.round(axis.deadzone() * 1000))));
        }
        rangeChoice(axis.id()).ifPresent(choice -> parts.add(switch (choice.range().read(choice.value())) {
            case FULL_RANGE -> getText("bindings.axis.throttleRange.full");
            case FORWARD_ONLY -> getText("bindings.axis.throttleRange.forward");
            case UNRECOGNISED -> getText("bindings.axis.throttleRange.unknown", choice.value());
        }));
        return new SlotCell(String.join(" \u00B7 ", parts), null);
    }

    /**
     * The throttle range that goes with this axis, when the file holds one that can be written.
     */
    private Optional<AssignAxisDialog.RangeChoice> rangeChoice(String axisId) {
        return ThrottleRanges.forAxis(axisId).flatMap(range ->
                Optional.ofNullable(currentValues.settings().get(range.setting()))
                        .filter(BindsValues.Setting::editable)
                        .map(setting -> new AssignAxisDialog.RangeChoice(range, setting.value())));
    }

    /**
     * The AI ACCESS cell for one row. The hover text says what to do in plain words, because the commander
     * reading it will not have read anything else: which slot to use, and that the controller binding stays.
     */
    private static AiAccessCell aiAccessCell(AiAccess.State state, KeyBindingsParser.ReadOnlyBindingSlots slots) {
        return switch (state) {
            case NOT_USED -> new AiAccessCell(state, "", null);
            case READY -> new AiAccessCell(state,
                    getText("bindings.aiAccess.ready"), getText("bindings.aiAccess.ready.tooltip"));
            case CANT_PRESS -> new AiAccessCell(state,
                    getText("bindings.aiAccess.cantPress"),
                    getText(AiAccess.bothSlotsTaken(slots)
                            ? "bindings.aiAccess.cantPress.bothSlots.tooltip"
                            : "bindings.aiAccess.cantPress.tooltip"));
            case NOT_BOUND -> new AiAccessCell(state,
                    getText("bindings.aiAccess.notBound"), getText("bindings.aiAccess.notBound.tooltip"));
        };
    }

    /**
     * The controls VEGA presses: its built-in commands' controls, and every binding a custom command taps -
     * a custom command on a stick-only control fails just the same, so the commander is warned about it too.
     * The same list auto-assign fills and the startup check measures - {@link BindingsMonitor#requiredControls()}.
     */
    private static AiAccess currentAiAccess() {
        return new AiAccess(BindingsMonitor.requiredControls());
    }

    /**
     * Orders controls the way the game's control screen lists them, falling back to the label for
     * the unmapped tail so those stay in a stable, readable order rather than an arbitrary one.
     */
    private static List<String> inGameOrder(List<String> bindingIds) {
        return bindingIds.stream()
                .sorted(Comparator
                        .comparingInt((String id) -> BindingDisplayNames.lookup(id).order())
                        .thenComparing(BindingDisplayNames::label, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Splits all keyboard-capable controls into those that already have a usable
     * keyboard binding and those that do not, using the same definition of "bound"
     * the auto-assigner uses. The Missing tab lists every such control; Auto-Assign VEGA Required Binds fills
     * only the ones in {@link BindingsMonitor#requiredControls()}, and each row can still be filled on its own.
     */
    private BindingPartition partitionByKeyboardBinding(Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots) {
        List<String> used = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, KeyBindingsParser.ReadOnlyBindingSlots> entry : slots.entrySet()) {
            if (autoAssigner.isKeyboardBound(entry.getValue())) {
                used.add(entry.getKey());
            } else {
                missing.add(entry.getKey());
            }
        }
        // Left unsorted on purpose: groupedBindings puts them in the game's own row order, and the
        // counts these lists also feed do not care about order.
        // An axis has no keyboard binding to have or to miss, so it is used once it is on an axis.
        for (BindsValues.Axis axis : currentValues.axes().values()) {
            (axis.bound() ? used : missing).add(axis.id());
        }
        return new BindingPartition(used, missing);
    }

    private record BindingPartition(List<String> used, List<String> missing) {
    }

    /**
     * Maps every conflicting binding id to the partners it collides with, grouped by the chord each
     * collision happens on. The {@code keySet} drives the RED/green row coloring; the grouped
     * partner lists feed the hover callout. Every slot the file holds is scanned, a modifier held on a
     * controller included ({@link BindingConflictScanner#scanReadOnlySlots}).
     */
    private ConflictIndex computeConflicts(
            Map<String, KeyBindingsParser.ReadOnlyBindingSlots> bindings) {
        Map<String, Map<Set<String>, Set<String>>> byChord = new HashMap<>();
        for (BindingConflictScanner.Conflict conflict : BindingConflictScanner.scanReadOnlySlots(bindings)) {
            indexConflict(byChord, conflict.actionA(), conflict.actionB(), conflict.chordA());
            indexConflict(byChord, conflict.actionB(), conflict.actionA(), conflict.chordB());
        }
        return new ConflictIndex(byChord);
    }

    /**
     * Which partners each conflicting binding collides with, grouped by the chord the collision
     * happens on. Named rather than passed around as its raw map type, which says nothing about
     * which nesting level means what.
     */
    private record ConflictIndex(Map<String, Map<Set<String>, Set<String>>> byBinding) {

        static ConflictIndex empty() {
            return new ConflictIndex(Map.of());
        }

        boolean contains(String bindingId) {
            return byBinding.containsKey(bindingId);
        }

        /**
         * The chords this binding is in conflict on, each with the partners it collides with there.
         */
        Map<Set<String>, Set<String>> partnersByChord(String bindingId) {
            return byBinding.getOrDefault(bindingId, Map.of());
        }
    }

    /**
     * Records one direction of one conflict: {@code partner} collides with {@code binding} on
     * {@code chord}, which is {@code binding}'s own chord - for a modifier shadow the partner's differs. The scan reports each pair once per shared chord, so a binding in conflict on
     * both its slots accumulates an entry per chord rather than keeping only the first.
     */
    private static void indexConflict(
            Map<String, Map<Set<String>, Set<String>>> byChord,
            String binding, String partner, Set<String> chord) {
        byChord.computeIfAbsent(binding, k -> new LinkedHashMap<>())
                .computeIfAbsent(chord, k -> new TreeSet<>())
                .add(partner);
    }

    /**
     * Renders both tabs from {@link #currentSlots}, honoring the "show conflicts only" filter. The Used and Missing
     * counts are of the selected section, ignoring the filters and the search; the Auto-Assign VEGA Required Binds enabled
     * state always reflects the whole file, so the filters and tabs are pure view toggles. Called on load and whenever a
     * filter or section tab changes.
     */
    private void renderBindingTables() {
        // Drop any open conflict callout before the tables (and their hover listeners) are rebuilt.
        tableFactory.hideConflictPopup();

        // Tables show every keyboard-capable control (custom commands can target any of them),
        // split by whether it currently has a usable keyboard binding.
        BindingPartition partition = partitionByKeyboardBinding(currentSlots);
        List<String> usedBindings = partition.used();
        List<String> missingBindings = partition.missing();
        AiAccess aiAccess = currentAiAccess();
        List<String> usedShown = filterRows(usedBindings, aiAccess);
        List<String> missingShown = filterRows(missingBindings, aiAccess);
        List<String> allShown = new ArrayList<>(usedShown);
        allShown.addAll(missingShown);
        showSectionCounts(allShown);

        renderGroupedTables(
                usedBindingsPanel,
                groupedBindings(inSelectedSection(usedShown), currentSlots, BindingsGroupTableFactory.RowAction.CLEAR, aiAccess),
                BindingsGroupTableFactory.RowAction.CLEAR,
                getText("bindings.column.action"),
                getText("bindings.column.primary"),
                getText("bindings.column.secondary"),
                getText("bindings.column.aiAccess"),
                getText("bindings.column.clear"));
        tabs.setTitleAt(0, getText("bindings.usedBindings", inSelectedSection(usedBindings).size()));

        renderGroupedTables(
                missingBindingsPanel,
                groupedBindings(inSelectedSection(missingShown), currentSlots, BindingsGroupTableFactory.RowAction.AUTO_FIX, aiAccess),
                BindingsGroupTableFactory.RowAction.AUTO_FIX,
                getText("bindings.column.action"),
                getText("bindings.column.primary"),
                getText("bindings.column.secondary"),
                getText("bindings.column.aiAccess"),
                getText("bindings.column.autofix"));
        tabs.setTitleAt(1, getText("bindings.missingBindings", inSelectedSection(missingBindings).size()));
        // Enabled only when a control VEGA presses is missing: the button fills those and nothing else, so with
        // only emotes and the like left in the Missing tab a click could do nothing.
        Set<String> required = BindingsMonitor.requiredControls();
        fixAllButton.setEnabled(missingBindings.stream().anyMatch(id -> !isAxis(id) && required.contains(id)));
        renderAnomalies(aiAccess);
    }

    /**
     * The rows the selected section tab keeps. A pure view of the filtered list, as the other filters are.
     */
    private List<String> inSelectedSection(List<String> bindingIds) {
        BindingSection section = SECTION_TABS.get(sectionTabs.getSelectedIndex()).section();
        return section == null ? bindingIds : bindingIds.stream()
                .filter(id -> BindingDisplayNames.lookup(id).section() == section)
                .toList();
    }

    /**
     * Each section tab's count of the rows the filters leave, so a search shows where its hits are from any tab.
     */
    private void showSectionCounts(List<String> shown) {
        for (int i = 0; i < SECTION_TABS.size(); i++) {
            SectionTab tab = SECTION_TABS.get(i);
            long count = tab.section() == null ? shown.size() : shown.stream()
                    .filter(id -> BindingDisplayNames.lookup(id).section() == tab.section())
                    .count();
            sectionTabs.setTitleAt(i, getText(tab.titleKey(), count));
        }
    }

    /**
     * Every row drawn red, by the same conflict index that draws it. Only the search narrows them: the section tab and
     * the footer's filters belong to Game Mode.
     */
    private void renderAnomalies(AiAccess aiAccess) {
        List<String> inConflict = currentSlots.keySet().stream().filter(conflicts::contains).toList();
        anomalyCount.accept(inConflict.size());
        renderGroupedTables(
                anomaliesPanel,
                groupedBindings(filterBySearch(inConflict), currentSlots, BindingsGroupTableFactory.RowAction.CLEAR, aiAccess),
                BindingsGroupTableFactory.RowAction.CLEAR,
                getText("bindings.column.action"),
                getText("bindings.column.primary"),
                getText("bindings.column.secondary"),
                getText("bindings.column.aiAccess"),
                getText("bindings.column.clear"));
        if (inConflict.isEmpty()) {
            JLabel none = hudReadoutLabel(getText("bindings.anomalies.none"));
            none.setBorder(new EmptyBorder(10, 8, 10, 0));
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            anomaliesPanel.add(none, 0);
            anomaliesPanel.revalidate();
        }
    }

    /**
     * Maps every binding that has a ship/SRV twin bound to a different key to that twin id (both
     * directions). The {@code keySet} drives the CYAN tint; the partner lists feed the soft callout.
     */
    private Map<String, Set<String>> computeRecommendations(
            Map<String, KeyBindingsParser.BindingSlots> bindings) {
        Map<String, Set<String>> recommendations = new HashMap<>();
        for (BindingConflictScanner.Recommendation r : BindingConflictScanner.recommendVehicleTwinsFromSlots(bindings)) {
            recommendations.computeIfAbsent(r.shipAction(), k -> new TreeSet<>()).add(r.buggyAction());
            recommendations.computeIfAbsent(r.buggyAction(), k -> new TreeSet<>()).add(r.shipAction());
        }
        return recommendations;
    }

    /**
     * Builds the hover-callout for a row: the RED conflict callout if the binding is in a conflict,
     * else the CYAN ship/SRV-twin recommendation callout, else {@code null}. A conflict outranks a
     * recommendation so the more urgent message wins when a binding has both.
     */
    private JComponent buildRowCalloutContent(String bindingId) {
        JComponent conflict = buildConflictPopupContent(bindingId);
        return conflict != null ? conflict : buildRecommendationPopupContent(bindingId);
    }

    /**
     * Builds the soft ship/SRV-twin callout: a non-warning (cyan) suggestion to bind this control to
     * the same key as its counterpart in the other vehicle, or {@code null} if it has no such twin.
     * It is a recommendation, not a rule - different keys are perfectly valid.
     */
    private JComponent buildRecommendationPopupContent(String bindingId) {
        Set<String> partners = recommendationsByBinding.get(bindingId);
        if (partners == null || partners.isEmpty()) {
            return null;
        }
        return buildCalloutCard(HUD_COLOR_ROLE_INFORMATION,
                List.of(new CalloutSection(getText("bindings.recommendation.popup.title"), partners)));
    }

    /**
     * The view filters, in one place so the two tables always narrow the same way: the conflicts-only and
     * required-by-Vega checkboxes, and the search field. Purely a view concern - the tab counts and the
     * Auto-Assign VEGA Required Binds enabled state are computed from the unfiltered lists.
     */
    private List<String> filterRows(List<String> bindingIds, AiAccess aiAccess) {
        return filterBySearch(filterVegaOnly(filterConflictsOnly(bindingIds), aiAccess));
    }

    /**
     * When the filter is on, narrows the ids to the controls Vega uses - its built-in commands' and every one a
     * custom command taps, the same set the AI ACCESS column judges; otherwise a no-op.
     */
    private List<String> filterVegaOnly(List<String> bindingIds, AiAccess aiAccess) {
        if (vegaOnlyCheck == null || !vegaOnlyCheck.isSelected()) {
            return bindingIds;
        }
        return bindingIds.stream()
                .filter(id -> !isAxis(id) && aiAccess.stateOf(id, currentSlots.get(id)) != AiAccess.State.NOT_USED)
                .toList();
    }

    /**
     * When the filter is on, narrows the ids to those that participate in a conflict; otherwise a no-op.
     */
    private List<String> filterConflictsOnly(List<String> bindingIds) {
        if (conflictsOnlyCheck == null || !conflictsOnlyCheck.isSelected()) {
            return bindingIds;
        }
        return bindingIds.stream().filter(conflicts::contains).toList();
    }

    /**
     * Narrows the ids to those whose in-game section, group, name or raw XML tag contain what has
     * been typed. Literal, case-insensitive substring matching - what is typed is what is looked
     * for - and the tag is in the haystack so a commander who arrived here from a forum post
     * naming {@code ExplorationSAANextGenus} can still find the row.
     * <p>
     * A controller binding is in the haystack too, by the button name the commander gave it in the
     * Device Editor: "what did I put on 1st Hat Up?" is answered by typing that name.
     */
    private List<String> filterBySearch(List<String> bindingIds) {
        String query = searchQuery.trim().toLowerCase();
        if (query.isEmpty()) {
            return bindingIds;
        }
        return bindingIds.stream()
                .filter(id -> searchText(id).contains(query))
                .toList();
    }

    private String searchText(String bindingId) {
        String text = BindingDisplayNames.searchText(bindingId, sectionLabelFor(bindingId));
        BindsValues.Axis axis = currentValues.axes().get(bindingId);
        if (axis != null) {
            return axis.input()
                    .map(input -> text + " " + (slotFormatter.formatInputs(input.device(), List.of(), input.key())
                            + " " + input.device() + " " + input.key()).toLowerCase())
                    .orElse(text);
        }
        KeyBindingsParser.ReadOnlyBindingSlots slots = currentSlots.get(bindingId);
        if (slots == null) {
            return text;
        }
        return text
                + " " + slotFormatter.controllerSearchText(slots.primary()).toLowerCase()
                + " " + slotFormatter.controllerSearchText(slots.secondary()).toLowerCase();
    }

    private static String sectionLabelFor(String bindingId) {
        return getText(BindingDisplayNames.lookup(bindingId).section().getLabelKey());
    }

    /**
     * Builds the themed hover-callout content for a conflicting binding, or {@code null} if it has no
     * conflict. One titled group per chord it collides on, each naming that chord and listing,
     * humanized, the bindings it collides with there - a binding in conflict on both slots has two.
     */
    private JComponent buildConflictPopupContent(String bindingId) {
        Map<Set<String>, Set<String>> byChord = conflicts.partnersByChord(bindingId);
        if (byChord.isEmpty()) {
            return null;
        }
        List<CalloutSection> sections = new ArrayList<>();
        for (Map.Entry<Set<String>, Set<String>> e : byChord.entrySet()) {
            sections.add(new CalloutSection(
                    getText("bindings.conflict.popup.title", chordText(bindingId, e.getKey())),
                    e.getValue()));
        }
        return buildCalloutCard(HUD_COLOR_ROLE_DANGER, sections);
    }

    /**
     * One titled group inside a hover callout.
     * <p>
     * A list of these rather than a title-keyed map: the title is rendered text, and two chords that
     * happen to render the same way - both empty, if a slot cannot be matched - would silently
     * collapse into one and take a partner list with them.
     */
    private record CalloutSection(String title, Set<String> partners) {
    }

    /**
     * Shared structure for conflict and recommendation hover callouts: one accented title per
     * section, each over a humanized bullet list of partner binding names, framed in the given
     * accent color.
     */
    private JComponent buildCalloutCard(Color accent, List<CalloutSection> sections) {
        HudPanel card = new HudPanel(new BorderLayout(), accent, HudPanel.Variant.FRAMED);
        JPanel body = transparentPanel(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(6, 10, 6, 10));

        for (CalloutSection section : sections) {
            JLabel titleLabel = new JLabel(section.title());
            titleLabel.setForeground(accent);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(titleLabel);

            for (String partner : section.partners()) {
                JLabel item = new JLabel("• " + BindingDisplayNames.label(partner));
                item.setForeground(HUD_COLOR_ROLE_PRIMARY_TEXT);
                item.setAlignmentX(Component.LEFT_ALIGNMENT);
                body.add(item);
            }
        }

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /**
     * The formatted chord (e.g. "Left Ctrl + A") as the commander sees it on the row.
     * <p>
     * WHY it formats the matching slot rather than the raw chord: the scan reads both slots, so a
     * binding can be in conflict on its Secondary while its Primary holds a different chord
     * entirely. The slot that actually carries {@code chord} is the one to name - pointing at the
     * Primary there sends the commander to rebind a key that is not in collision.
     */
    private String chordText(String bindingId, Set<String> chord) {
        KeyBindingsParser.ReadOnlyBindingSlots slots = currentSlots.get(bindingId);
        if (slots == null || chord == null || chord.isEmpty()) {
            return "";
        }
        KeyBindingsParser.ReadOnlyBindingSlot slot = matchesChord(slots.primary(), chord)
                ? slots.primary()
                : matchesChord(slots.secondary(), chord) ? slots.secondary() : null;
        return slot == null ? "" : slotFormatter.formatChord(slot.bindingModifiers(), slot.key());
    }

    /**
     * WHY no keyboard-usable test: the scan reads a slot whose modifier is held on a controller (FN-6), and
     * {@link BindingConflictScanner#chordOf} already gives an empty chord for a slot with no keyboard main key.
     */
    private boolean matchesChord(KeyBindingsParser.ReadOnlyBindingSlot slot, Set<String> chord) {
        return chord.equals(BindingConflictScanner.chordOf(slot));
    }

    private Map<BindingSection, List<Object[]>> groupedRows() {
        Map<BindingSection, List<Object[]>> grouped = new EnumMap<>(BindingSection.class);
        for (BindingSection section : BindingSection.values()) {
            grouped.put(section, new ArrayList<>());
        }
        return grouped;
    }

    private void renderGroupedTables(JPanel targetPanel, Map<BindingSection, List<Object[]>> grouped,
                                     BindingsGroupTableFactory.RowAction rowAction, String... columnNames) {
        targetPanel.removeAll();
        for (BindingSection section : BindingSection.values()) {
            // Already in the game's own row order - groupedBindings sorted them, so leave them alone.
            List<Object[]> rows = grouped.getOrDefault(section, List.of());
            if (rows.isEmpty())
                continue;

            targetPanel.add(sectionHeader(section));
            targetPanel.add(tableFactory.groupTable(rows, outerScrollPaneFor(targetPanel), rowAction, columnNames));
            targetPanel.add(Box.createVerticalStrut(6));
        }
        targetPanel.add(Box.createVerticalGlue());
        targetPanel.revalidate();
        targetPanel.repaint();
    }

    private JComponent sectionHeader(BindingSection section) {
        JLabel label = hudGroupLabel(getText(section.getLabelKey()).toUpperCase());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(10, 8, 10, 0));
        return label;
    }

    private JScrollPane outerScrollPaneFor(JPanel targetPanel) {
        if (targetPanel == anomaliesPanel) return anomaliesScrollPane;
        return targetPanel == usedBindingsPanel ? usedBindingsScrollPane : missingBindingsScrollPane;
    }

    /**
     * Opens capture for a control's first slot. For a caller that names a control rather than a slot; a grid row names
     * both and comes in below. It does nothing when no file is loaded, the control is blank, or a dialog or change is
     * already under way.
     */
    void openCapture(String bindingId) {
        openCapture(bindingId, BindingSlotType.PRIMARY);
    }

    private void openCapture(String bindingId, BindingSlotType slotType) {
        if (bindingId == null || bindingId.isBlank() || loadedFile == null || assignDialogOpen || editRunner.running()) {
            return;
        }
        if (isAxis(bindingId)) {
            openAxisEditor(bindingId);
            return;
        }
        // Opening the dialog comes from a row click, so no mouseExited fires to dismiss the hover
        // callout - drop it explicitly so it does not float over the modal.
        tableFactory.hideConflictPopup();

        // WHY no gate on what the slots hold: a controller slot opens like any other (Alan, 2026-10-15). The
        // writer refuses, without rewriting, a slot whose structure it does not know, and SAVE says so.
        KeyBindingsParser.ReadOnlyBindingSlots slots = currentSlots.get(bindingId);
        String file = loadedFile;
        AssignKeyboardBindingDialog dialog = new AssignKeyboardBindingDialog(
                dialogOwner,
                loadedContent,
                bindingId,
                slotType,
                slots,
                availabilityService,
                parser.toExecutableSlots(currentSlots),
                currentSlots,
                existingConflicts(bindingId, slots),
                // WHY: the names and labels read with the file, off the EDT (S4b review; Storage S5).
                CaptureControllers.all(currentSlots, labels),
                labels,
                currentAiAccess().stateOf(bindingId, slots) != AiAccess.State.NOT_USED
        );

        assignDialogOpen = true;
        try {
            AppTheme.runWithModalScrim(
                    SwingUtilities.getWindowAncestor(dialogOwner),
                    () -> saveSelections(file, bindingId, dialog.showDialog(), slots));
        } finally {
            assignDialogOpen = false;
        }
    }

    /**
     * Opens the axis editor for an axis row, whichever of the row's cells was clicked.
     */
    private void openAxisEditor(String bindingId) {
        tableFactory.hideConflictPopup();
        BindsValues.Axis axis = currentValues.axes().get(bindingId);
        String file = loadedFile;
        Set<String> axisDevices = new HashSet<>();
        currentValues.axes().values().forEach(a -> a.input().ifPresent(input -> axisDevices.add(input.device())));
        AssignAxisDialog dialog = new AssignAxisDialog(
                dialogOwner,
                axis,
                rangeChoice(bindingId).orElse(null),
                CaptureControllers.forAxes(currentSlots, axisDevices, labels),
                labels,
                input -> currentValues.axes().values().stream()
                        .filter(other -> !other.id().equals(bindingId))
                        .filter(other -> other.input().filter(input::sameInputAs).isPresent())
                        .map(other -> BindingDisplayNames.label(other.id()))
                        .sorted()
                        .toList());
        assignDialogOpen = true;
        try {
            AppTheme.runWithModalScrim(SwingUtilities.getWindowAncestor(dialogOwner),
                    () -> dialog.showDialog().ifPresent(choice -> saveAxis(file, choice)));
        } finally {
            assignDialogOpen = false;
        }
    }

    /**
     * Keeps the axis and its throttle range as one edit not applied, both or neither.
     */
    private void saveAxis(String file, AssignAxisDialog.Choice choice) {
        List<BindsEdits.SlotChange> changes = new ArrayList<>();
        changes.add(binds -> bindingsWriter.assignAxis(binds, choice.axis()));
        if (choice.rangeSetting() != null) {
            changes.add(binds -> bindingsWriter.assignSetting(binds, choice.rangeSetting(), choice.rangeValue()));
        }
        AtomicReference<BindingSaveResult> result = new AtomicReference<>();
        editRunner.run(getText("bindings.assign.dialogTitle"),
                () -> Edits.stored().change(current -> {
                    BindsEdits.Changed changed = BindsEdits.change(current, file, changes);
                    result.set(changed.result());
                    return changed.files();
                }),
                () -> saveResultPresenter.show(result.get()), () -> {
                });
    }

    /**
     * The row's Clear: the keyboard dialog, or the axis editor, whose CLEAR is the only clear an axis has.
     */
    private void openClearDialog(String bindingId) {
        if (isAxis(bindingId)) {
            openCapture(bindingId);
        } else {
            openClearKeyboardBindingDialog(bindingId);
        }
    }

    /**
     * Each slot's own keyboard clash, as the grid found it - so the dialog reports that slot's collision, not
     * whichever of the binding's chords happened to be indexed first. A missing or empty slot has none.
     */
    private Map<BindingSlotType, AssignKeyboardBindingDialog.ExistingConflict> existingConflicts(
            String bindingId, KeyBindingsParser.ReadOnlyBindingSlots slots) {
        Map<BindingSlotType, AssignKeyboardBindingDialog.ExistingConflict> found = new EnumMap<>(BindingSlotType.class);
        if (slots == null) return found;
        for (BindingSlotType type : BindingSlotType.values()) {
            KeyBindingsParser.ReadOnlyBindingSlot slot = type == BindingSlotType.PRIMARY ? slots.primary() : slots.secondary();
            if (slot == null) continue;
            Set<String> partners = conflicts.partnersByChord(bindingId).get(BindingConflictScanner.chordOf(slot));
            if (partners != null && !partners.isEmpty()) {
                found.put(type, new AssignKeyboardBindingDialog.ExistingConflict(
                        slotFormatter.formatChord(slot.bindingModifiers(), slot.key()), partners));
            }
        }
        return found;
    }

    /**
     * Keeps what the dialog changed as one edit not applied, every changed slot or none (Storage S5): the first slot
     * the writer refuses leaves the edits as they were, and says why.
     */
    private void saveSelections(String file, String bindingId, List<AssignSelection> selections,
                                KeyBindingsParser.ReadOnlyBindingSlots slots) {
        if (selections.isEmpty()) return;
        List<BindsEdits.SlotChange> changes = new ArrayList<>();
        for (AssignSelection selection : selections) {
            BindsEdits.SlotChange change = switch (selection) {
                case AssignKeyboardBindingSelection keyboard -> binds -> saveKeyboardBinding(binds,
                        new KeyboardBindingEdit(bindingId, keyboard.slotType(), keyboard.key()), keyboard.modifiers());
                case AssignSlotSelection any -> binds -> bindingsWriter.assignSlot(binds, new SlotAssignment(
                        bindingId, any.slotType(), any.input(), any.modifiers(),
                        // The slot keeps its Hold - an inline setting of the slot, not part of what was captured.
                        any.input() != null && holds(slots, any.slotType())));
            };
            changes.add(change);
        }
        AtomicReference<BindingSaveResult> result = new AtomicReference<>();
        editRunner.run(getText("bindings.assign.dialogTitle"),
                () -> Edits.stored().change(current -> {
                    BindsEdits.Changed changed = BindsEdits.change(current, file, changes);
                    result.set(changed.result());
                    return changed.files();
                }),
                () -> saveResultPresenter.show(result.get()), () -> {
                });
    }

    private static boolean holds(KeyBindingsParser.ReadOnlyBindingSlots slots, BindingSlotType type) {
        if (slots == null) return false;
        KeyBindingsParser.ReadOnlyBindingSlot slot = type == BindingSlotType.PRIMARY ? slots.primary() : slots.secondary();
        return slot != null && slot.hold();
    }

    private EditedBinds saveKeyboardBinding(byte[] binds, KeyboardBindingEdit edit, List<BindingModifier> modifiers) {
        return modifiers == null || modifiers.isEmpty()
                ? bindingsWriter.assignKeyboardKey(binds, edit)
                : bindingsWriter.assignKeyboardKeyWithModifiers(binds, edit, modifiers);
    }

    /**
     * Opens the per-row "clear keyboard binding" dialog for the Used view. The dialog offers to clear
     * the Primary and/or Secondary <em>keyboard</em> slot while leaving any Controller/HOTAS binding in
     * the other slot untouched. The actual edit + reload is delegated back via {@link #clearKeyboardSlot}.
     */
    private void openClearKeyboardBindingDialog(String bindingId) {
        if (bindingId == null || bindingId.isBlank() || loadedFile == null || editRunner.running()) {
            return;
        }
        // Opened from a row click - no mouseExited fires, so drop the hover callout explicitly.
        tableFactory.hideConflictPopup();

        KeyBindingsParser.ReadOnlyBindingSlots slots = currentSlots.get(bindingId);
        if (slots == null) {
            return;
        }
        String file = loadedFile;
        ClearKeyboardBindingDialog dialog = new ClearKeyboardBindingDialog(
                dialogOwner, bindingId, slots, slotFormatter,
                (slotType, done) -> clearKeyboardSlot(file, bindingId, slotType, done));
        AppTheme.runWithModalScrim(SwingUtilities.getWindowAncestor(dialogOwner), dialog::showDialog);
    }

    /**
     * Clears one slot's keyboard binding in the edits, off the EDT, then hands the dialog the row as it is now so it
     * can re-evaluate what is left to clear. Clearing writes Elite's empty-slot marker for that slot only; the other
     * slot (and any HOTAS binding on it) is not touched.
     */
    private void clearKeyboardSlot(String file, String bindingId, BindingSlotType slotType,
                                   Consumer<ClearKeyboardBindingDialog.ClearOutcome> done) {
        AtomicReference<BindingSaveResult> result = new AtomicReference<>();
        AtomicReference<KeyBindingsParser.ReadOnlyBindingSlots> row = new AtomicReference<>(currentSlots.get(bindingId));
        editRunner.run(getText("bindings.clear.dialogTitle"), () -> {
                    Edits.stored().change(current -> {
                        BindsEdits.Changed changed = BindsEdits.change(current, file, binds -> bindingsWriter.assignKeyboardKey(
                                binds, new KeyboardBindingEdit(bindingId, slotType, null))); // null key clears the slot
                        result.set(changed.result());
                        return changed.files();
                    });
                    row.set(parser.parseReadOnlyBindingSlots(Edits.stored().snapshot().current().bindings().get(file))
                            .get(bindingId));
                }, () -> done.accept(new ClearKeyboardBindingDialog.ClearOutcome(
                        result.get() == BindingSaveResult.SAVED || result.get() == BindingSaveResult.NO_CHANGE
                                ? ClearKeyboardBindingDialog.Cleared.CLEARED
                                : ClearKeyboardBindingDialog.Cleared.REFUSED, row.get())),
                // WHY: the dialog waits for an answer. The failure was already shown, so it is not told twice; the row
                // as it stands shows nothing was cleared.
                () -> done.accept(new ClearKeyboardBindingDialog.ClearOutcome(
                        ClearKeyboardBindingDialog.Cleared.ALREADY_REPORTED, row.get())));
    }
}
