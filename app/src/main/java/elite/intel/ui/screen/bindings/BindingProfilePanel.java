package elite.intel.ui.screen.bindings;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandDefinition;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandRegistry;
import elite.intel.bindforge.devices.CaptureControllers;
import elite.intel.bindforge.io.*;
import elite.intel.bindforge.model.BindingDisplayNames;
import elite.intel.bindforge.model.BindingModifier;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.model.BindingSlotType;
import elite.intel.bindforge.rules.AiAccess;
import elite.intel.bindforge.rules.BindingConflictScanner;
import elite.intel.bindforge.rules.KeyboardKeyAvailabilityService;
import elite.intel.bindforge.rules.MissingBindingAutoAssigner;
import elite.intel.bindforge.storage.BindsEdits;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.Edits;
import elite.intel.bindforge.storage.SavedDeviceLabels;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.DataDirectoryValidator;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.dialog.AssignKeyboardBindingDialog;
import elite.intel.ui.dialog.ClearKeyboardBindingDialog;
import elite.intel.ui.event.BindForgeEditsChangedEvent;
import elite.intel.ui.event.BindingsSummaryChangedEvent;
import elite.intel.ui.event.BindingsUpdatedEvent;
import elite.intel.ui.support.*;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static elite.intel.ui.dialog.HudConfirmDialog.info;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.sizeFieldLabel;
import static elite.intel.ui.theme.HudGlyphs.verticalEllipsisIcon;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * The "Binding Profile" sub-tab of BIND FORGE - the Bind Editor: the binds file chosen from the working copy, the
 * Used/Missing Bindings tables, conflict detection, hover callouts and Auto-Assign Missing Binds. Extracted from the
 * former monolithic {@code BindingsTabPanel}.
 * <p>
 * <strong>It shows and changes the edits not applied</strong> (Storage S5, 2026-10-28): any binds file in the working
 * copy loads, and capture SAVE, CLEAR and Auto-Assign are edits that light the one APPLY / DISCARD bar above
 * BindForge's tabs. Nothing here writes a file (storage.md rule 6). Reading and changing the edits touch the disk, so
 * both run off the EDT.
 */
public class BindingProfilePanel extends JPanel {
    private static final Logger log = LogManager.getLogger(BindingProfilePanel.class);


    private static final int SCROLL_UNIT_ROWS = 2;
    /**
     * Pacing slider granularity: 10ms notches between the FAST and SLOW ends.
     */
    private static final int KEY_INPUT_PACING_STEP_MS = 10;

    private final KeyBindingsParser parser = KeyBindingsParser.getInstance();
    private final BindingsMonitor monitor = BindingsMonitor.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();
    private final KeyboardKeyAvailabilityService availabilityService = new KeyboardKeyAvailabilityService();
    private final BindingsWriter bindingsWriter = new BindingsWriter();
    /**
     * Reads controller inputs by the names and labels in the edits - read with the binds file, off the EDT.
     */
    private BindingSlotDisplayFormatter slotFormatter = new BindingSlotDisplayFormatter();
    /** The names and labels {@link #slotFormatter} reads, for the capture dialog's controller tabs. */
    private SavedDeviceLabels labels = SavedDeviceLabels.NONE;
    private final BindingsSelectionController selectionController;
    private final BindingsGroupTableFactory tableFactory;
    private final MissingBindingAutoAssigner autoAssigner = new MissingBindingAutoAssigner();
    private final RefreshCoalescer refreshes = new RefreshCoalescer();

    private JComboBox<String> bindsFileBox;
    private JButton loadButton;
    private JTextField bindingsDirField;
    private JPanel keyboardOnlyBanner;
    private BindingSaveResultPresenter saveResultPresenter;
    private JPanel usedBindingsPanel;
    private JPanel missingBindingsPanel;
    private JScrollPane usedBindingsScrollPane;
    private JScrollPane missingBindingsScrollPane;
    private JTabbedPane tabs;
    private JButton fixAllButton;
    private JCheckBox conflictsOnlyCheck;
    /**
     * Narrows the grid to the controls Vega uses - the ones with something in the AI ACCESS column.
     */
    private JCheckBox vegaOnlyCheck;
    private JTextField bindingSearchField;

    private Map<String, KeyBindingsParser.ReadOnlyBindingSlots> currentSlots = Map.of();
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
    /** The binds file shown, by file name - {@code null} when none could be read. */
    private volatile String loadedFile;
    /** Its bytes as the edits held them when it was read - what the capture dialog checks a key against. */
    private byte[] loadedContent;
    /** The file LOAD asked for, shown at the next read if the edits still hold it. */
    private volatile String wantedFile;
    private boolean assignDialogOpen;
    /** A change to the edits is running - SAVE, CLEAR or Auto-Assign; a second waits for it. */
    private boolean changeRunning;

    public BindingProfilePanel() {
        selectionController = new BindingsSelectionController();
        tableFactory = new BindingsGroupTableFactory(
                selectionController, this::openAssignKeyboardBindingDialog, this::autoFixSingleBinding,
                this::openClearKeyboardBindingDialog,
                // Lambda (not a bound method ref) so it reads the field live: the map is
                // reassigned on each load, and a bound method ref would pin the initial empty map.
                id -> conflicts.contains(id),
                id -> recommendationsByBinding.containsKey(id),
                this::buildRowCalloutContent);
        buildUi();
        // WHY: what the grid shows also comes from outside the binds file - which controls custom commands tap - and
        // nothing announces a change to it. Coming back to this tab is when the commander looks, so that is when it
        // is read again.
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !isShowing()) return;
            initData();
        });
        saveResultPresenter = new BindingSaveResultPresenter(this);
        UiBus.register(this);
    }

    public void dispose() {
        UiBus.unregister(this);
    }

    /**
     * The edits changed - an edit here or in the Alias Designer, DISCARD, APPLY, RESTORE or first-time startup - so the
     * binds file shown may have.
     */
    @Subscribe
    public void onEditsChanged(BindForgeEditsChangedEvent event) {
        SwingUtilities.invokeLater(this::initData);
    }

    /**
     * The live bindings changed - {@code BindingsMonitor} saw the bindings folder change. Only the AI tab's counts
     * read them; the grid shows the edits.
     */
    @Subscribe
    public void onBindingsUpdated(BindingsUpdatedEvent event) {
        new Thread(this::publishLiveSummary, "BindForge-BindingsSummary").start();
    }

    private void buildUi() {
        setLayout(new BorderLayout(2, SCREEN_TOP_GAP));
        setBorder(hudSubtabContentBorder());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        JPanel details = compactProfilePanel();
        JPanel header = transparentPanel(new BorderLayout(0, 2));
        header.add(bindingProfileCard(details), BorderLayout.NORTH);
        header.add(keyInputPacingRow(), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        usedBindingsPanel = groupedTablesPanel();
        missingBindingsPanel = groupedTablesPanel();
        usedBindingsScrollPane = groupedTablesScrollPane(usedBindingsPanel);
        missingBindingsScrollPane = groupedTablesScrollPane(missingBindingsPanel);

        tabs = AppTheme.makeCompactTabs();
        tabs.addTab(getText("bindings.usedBindings"), nestedTabContent(usedBindingsScrollPane));
        tabs.addTab(getText("bindings.missingBindings"), nestedTabContent(missingBindingsScrollPane));
        tabs.addChangeListener(e -> selectionController.clearSelection());

        JPanel tablesArea = transparentPanel(new BorderLayout(0, 2));
        tablesArea.add(bindingSearchRow(), BorderLayout.NORTH);
        tablesArea.add(tabs, BorderLayout.CENTER);
        add(tablesArea, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel compactProfilePanel() {
        JPanel profileCardBody = transparentPanel(new BorderLayout(0, 0));
        JPanel details = transparentPanel(new GridBagLayout());
        details.setBorder(BorderFactory.createEmptyBorder(2, 0, 6, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(2, 0, 6, 7);
        gbc.anchor = GridBagConstraints.WEST;

        // Single-column grid: [label | field | affordance]. Both rows share the label column width and the field
        // right edge; the vertical-ellipsis and LOAD line up on the right.
        // Row 0 - Bindings Directory + picker
        addProfileLabel(details, getText("player.bindingsDirectory"), gbc, 0, LABEL_COL_WIDTH);
        bindingsDirField = readOnlyField();
        bindingsDirField.setToolTipText(getText("player.bindingsDirectory.tooltip"));
        addProfileField(details, bindingsDirField, gbc, 1, 1, 1.0);

        JButton selectBindingsDirButton = compactDirectoryChooserButton();
        selectBindingsDirButton.addActionListener(e -> selectBindingsDirectory());
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(2, 0, 6, 7);
        details.add(selectBindingsDirButton, gbc);

        // Row 1 - the binds file, from the working copy with the edits on it, and LOAD (bind-editor.md, Shell)
        gbc.gridy = 1;
        addProfileLabel(details, getText("bindings.bindsFile"), gbc, 0, LABEL_COL_WIDTH);
        bindsFileBox = new JComboBox<>();
        AppTheme.styleComboBox(bindsFileBox);
        bindsFileBox.setToolTipText(getText("bindings.bindsFile.tooltip"));
        addProfileField(details, bindsFileBox, gbc, 1, 1, 1.0);

        loadButton = makeButtonSubtle(getText("bindings.bindsFile.load"));
        loadButton.setToolTipText(getText("bindings.bindsFile.load.tooltip"));
        loadButton.addActionListener(e -> loadSelected());
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(2, 0, 6, 7);
        details.add(loadButton, gbc);

        profileCardBody.add(details, BorderLayout.CENTER);
        return profileCardBody;
    }

    private JComponent bindingProfileCard(JPanel body) {
        // Working zone of the tab -> FLAT section (HUD section 9), not a framed accent box.
        HudSection card = new HudSection(
                getText("bindings.section.profile"),
                new BorderLayout(),
                HudPanel.Variant.FLAT,
                6);
        card.body().add(body, BorderLayout.CENTER);
        card.setFooter(keyboardOnlyWarningStrip(), HUD_COLOR_ROLE_WARNING_PANEL_BACKGROUND);

        JPanel wrapper = transparentPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
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

        // Non-modal footer: the filters and AUTO-ASSIGN MISSING BINDS on the right, no BACK. APPLY and DISCARD are
        // the bar's, above BindForge's tabs (Storage S5).
        return HudFooter.build(false, null, null,
                List.of(conflictsOnlyCheck, vegaOnlyCheck, fixAllButton));
    }

    /**
     * Reads the edits and the binds file to show, off the EDT, then shows them - the file LOAD asked for, else the one
     * shown, else the one the preset file's General line names, else the first.
     * <p>
     * A read already in flight is left to finish, and one more follows it - see {@link RefreshCoalescer}.
     */
    public void initData() {
        bindingsDirField.setText(playerSession.getBindingsDir().toString());
        if (!refreshes.tryStart()) return;
        String wanted = wantedFile != null ? wantedFile : loadedFile;
        new Thread(() -> {
            Loaded read = read(wanted);
            SwingUtilities.invokeLater(() -> {
                show(read);
                if (refreshes.finish()) initData();
            });
            publishLiveSummary();
        }, "BindForge-BindEditorRead").start();
    }

    /** LOAD: the file chosen in the list. */
    private void loadSelected() {
        Object chosen = bindsFileBox.getSelectedItem();
        if (chosen == null) return;
        wantedFile = chosen.toString();
        initData();
    }

    /**
     * What one read found. {@code file} is {@code null} when nothing could be shown - the log says why.
     *
     * @param asked the file this read was asked for, LOAD's or the one shown - {@code null} for neither
     */
    private record Loaded(String asked, List<String> files, String file, byte[] content,
                          Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots, SavedDeviceLabels labels) {
        static Loaded nothing(String asked) {
            return new Loaded(asked, List.of(), null, null, Map.of(), SavedDeviceLabels.NONE);
        }
    }

    /** Reads the edits and parses the file to show. Off the EDT. */
    private Loaded read(String asked) {
        try {
            ConfigurationFiles current = Edits.stored().snapshot().current();
            List<String> files = BindsEdits.bindsFiles(current);
            if (files.isEmpty()) return Loaded.nothing(asked);
            String file = asked != null && files.contains(asked)
                    ? asked
                    : BindsEdits.generalFile(current).orElse(files.getFirst());
            byte[] content = current.bindings().get(file);
            return new Loaded(asked, files, file, content, parser.parseReadOnlyBindingSlots(content),
                    SavedDeviceLabels.of(current));
        } catch (Exception e) {
            // WHY: broad on purpose - a thread boundary. Before first-time startup there is no working copy, and the
            // BindForge tab says so; a damaged one is offered a restore there too. The grid shows nothing.
            log.warn("The Bind Editor could not read the edits", e);
            return Loaded.nothing(asked);
        }
    }

    /** Shows what {@link #read} found. On the EDT. */
    private void show(Loaded read) {
        selectionController.resetTables();
        bindsFileBox.setModel(new DefaultComboBoxModel<>(read.files().toArray(String[]::new)));
        bindsFileBox.setSelectedItem(read.file());
        boolean any = read.file() != null;
        bindsFileBox.setEnabled(any);
        loadButton.setEnabled(any);
        // WHY only when this read was LOAD's: a LOAD pressed while an older read ran is read next, not forgotten.
        if (Objects.equals(wantedFile, read.asked())) wantedFile = null;
        loadedFile = read.file();
        loadedContent = read.content();
        labels = read.labels();
        slotFormatter = new BindingSlotDisplayFormatter(labels);
        currentSlots = read.slots();
        if (!any) {
            conflicts = ConflictIndex.empty();
            recommendationsByBinding = Map.of();
            renderGroupedTables(usedBindingsPanel, Map.of(), BindingsGroupTableFactory.RowAction.NONE, getText("bindings.column.action"));
            renderGroupedTables(missingBindingsPanel, Map.of(), BindingsGroupTableFactory.RowAction.NONE, getText("bindings.column.action"));
            tabs.setTitleAt(0, getText("bindings.usedBindings", 0));
            tabs.setTitleAt(1, getText("bindings.missingBindings", 0));
            fixAllButton.setEnabled(false);
            return;
        }
        Map<String, KeyBindingsParser.BindingSlots> parsedSlots = parser.toExecutableSlots(currentSlots);
        conflicts = computeConflicts(parsedSlots);
        recommendationsByBinding = computeRecommendations(parsedSlots);
        renderBindingTables();
    }

    /**
     * The AI tab's counts, from the live bindings {@code BindingsMonitor} reads - what Vega presses, not what is on
     * screen (Alan, 2026-10-28, Storage S5). Only the slot EliteIntel would press counts, not both slots.
     */
    private void publishLiveSummary() {
        Map<String, KeyBindingsParser.KeyBinding> live = monitor.getBindings();
        if (live == null) {
            UiBus.publish(new BindingsSummaryChangedEvent(0, 0));
            return;
        }
        UiBus.publish(new BindingsSummaryChangedEvent(
                monitor.findMissingGameBindings(live).size(),
                monitor.findFoundGameBindings(live).size()));
    }

    private void fixAllMissing() {
        if (loadedFile == null || currentSlots.isEmpty() || changeRunning) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                getText("bindings.autofix.confirm.text"),
                getText("bindings.autofix.confirm.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        MissingBindingAutoAssigner.Plan plan = autoAssigner.planAll(currentSlots);
        applyPlanInBackground(plan, outcome -> showBatchSummary(outcome, plan));
    }

    private void autoFixSingleBinding(String bindingId) {
        if (bindingId == null || bindingId.isBlank() || loadedFile == null || changeRunning) {
            return;
        }
        MissingBindingAutoAssigner.Plan plan = autoAssigner.planOne(bindingId, currentSlots);
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
        offEdt(getText("bindings.autofix.result.title"),
                () -> Edits.stored().change(current -> applyPlan(current, file, plan, outcome)),
                () -> onComplete.accept(outcome.get()), () -> { });
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
                this, sb.toString(), getText("bindings.autofix.result.title"), JOptionPane.INFORMATION_MESSAGE);
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
                    this,
                    getText("bindings.autofix.single.success", BindingDisplayNames.label(bindingId)),
                    getText("bindings.autofix.result.title"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (outcome.failed() > 0) {
            JOptionPane.showMessageDialog(
                    this,
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
                this, getText(messageKey), getText("bindings.autofix.result.title"), JOptionPane.WARNING_MESSAGE);
    }

    private record ApplyOutcome(int saved, int failed) {
    }

    private void selectBindingsDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(getText("player.bindingsDirectory.dialog"));
        String current = playerSession.getBindingsDir().toString();
        if (!current.isBlank())
            chooser.setCurrentDirectory(new File(current).getParentFile());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            if (!playerSession.setBindingsDir(path)) {
                DataDirectoryValidator.warnUnusable(DataDirectoryValidator.DirectoryKind.BINDINGS);
                return;
            }
            bindingsDirField.setText(playerSession.getBindingsDir().toString());
            DataDirectoryValidator.validateAndWarn(playerSession.getBindingsDir(), DataDirectoryValidator.DirectoryKind.BINDINGS);
            initData();
        }
    }

    private void addProfileLabel(JPanel panel, String text, GridBagConstraints gbc, int column, int width) {
        gbc.gridx = column;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(2, 0, 6, 7);
        JLabel label = hudReadoutLabel(text);
        sizeFieldLabel(label, width);
        panel.add(label, gbc);
    }

    private void addProfileField(
            JPanel panel,
            JComponent component,
            GridBagConstraints gbc,
            int column,
            int gridWidth,
            double weightX
    ) {
        gbc.gridx = column;
        gbc.gridwidth = gridWidth;
        gbc.weightx = weightX;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 0, 6, 7);
        component.setPreferredSize(new Dimension(0, component.getPreferredSize().height));
        panel.add(component, gbc);
    }

    /** Shared width of the left label column so all rows align (fits the longest label). */
    private static final int LABEL_COL_WIDTH = 180;

    private JTextField readOnlyField() {
        JTextField field = makeTextField();
        field.setEditable(false);
        return field;
    }

    private JButton compactDirectoryChooserButton() {
        JButton button = makeFieldButton(verticalEllipsisIcon(HUD_FIELD_HEIGHT), HUD_FIELD_HEIGHT);
        button.setToolTipText(getText("player.bindingsDirectory.select.tooltip"));
        return button;
    }

    private JPanel keyboardOnlyWarningStrip() {
        // Centralised on HudBanner (section 7.3) with the leading warning glyph - no hand-rolled strip.
        keyboardOnlyBanner = new HudBanner(getText("bindings.keyboardOnlyHint"),
                StatusBadge.State.STANDBY, true);
        return keyboardOnlyBanner;
    }

    /**
     * Pacing of the keystrokes this app sends to the game, sitting directly under the keyboard-only
     * banner because it governs exactly what that banner describes.
     * <p>
     * The slider sets the floor of the pause held after each keystroke of a sequence; the executor
     * adds its own randomised spread on top. FAST is the pacing every build so far has used and stays
     * the default - SLOW is for machines where the game drops keystrokes out of a sequence it cannot
     * keep up with.
     */
    private JPanel keyInputPacingRow() {
        JPanel row = transparentPanel(new GridBagLayout());
        row.setBorder(new EmptyBorder(0, 6, 0, 6));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 7);

        gbc.gridx = 0;
        row.add(sliderRowLabel(getText("bindings.keyInputPacing"), LABEL_COL_WIDTH), gbc);

        gbc.gridx = 1;
        row.add(sliderRowLabel(getText("bindings.keyInputPacing.fast"), 0), gbc);

        HudSlider slider = new HudSlider(
                SystemSession.KEY_INPUT_DELAY_MIN_MS,
                SystemSession.KEY_INPUT_DELAY_MAX_MS,
                KEY_INPUT_PACING_STEP_MS,
                systemSession.getKeyInputDelayMs());
        slider.setToolTipText(getText("bindings.keyInputPacing.tooltip"));
        slider.addChangeListener(e -> systemSession.setKeyInputDelayMs(slider.getValue()));
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        row.add(slider, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        row.add(sliderRowLabel(getText("bindings.keyInputPacing.slow"), 0), gbc);

        return row;
    }

    /**
     * Label for the pacing row, dropped by the slider's value area so it sits level with the track
     * rather than with the value printed above the thumb. A {@code width} of 0 leaves the label at
     * its natural width.
     */
    private static JLabel sliderRowLabel(String text, int width) {
        JLabel label = hudReadoutLabel(text);
        label.setBorder(new EmptyBorder(HUD_SLIDER_VALUE_AREA, 0, 0, 0));
        if (width > 0) {
            sizeFieldLabel(label, width);
        }
        return label;
    }

    /**
     * Search-as-you-type over both tables, sitting directly above them because it narrows exactly
     * what they show. Every keystroke re-renders; the tables are rebuilt from an in-memory map, so
     * there is nothing to debounce.
     */
    private JPanel bindingSearchRow() {
        JPanel row = transparentPanel(new GridBagLayout());
        row.setBorder(new EmptyBorder(0, 6, 2, 6));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 7);

        JLabel searchLabel = hudReadoutLabel(getText("bindings.search.label"));
        sizeFieldLabel(searchLabel, LABEL_COL_WIDTH);
        gbc.gridx = 0;
        row.add(searchLabel, gbc);

        bindingSearchField = makeTextField();
        bindingSearchField.setToolTipText(getText("bindings.search.tooltip"));
        bindingSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                renderBindingTables();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                renderBindingTables();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                renderBindingTables();
            }
        });
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        row.add(bindingSearchField, gbc);

        return row;
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
            SlotCell primary = slotCell(bindingSlots == null ? null : bindingSlots.primary());
            SlotCell secondary = slotCell(bindingSlots == null ? null : bindingSlots.secondary());
            AiAccessCell ai = aiAccessCell(aiAccess.stateOf(bindingId, bindingSlots), bindingSlots);
            Object[] row = switch (rowAction) {
                case AUTO_FIX ->
                        new Object[]{bindingId, primary, secondary, ai, getText("bindings.column.autofix.action")};
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
     */
    private static AiAccess currentAiAccess() {
        Set<String> vegaControls = new HashSet<>(BindingsMonitor.appDrivenControls());
        for (CustomCommandDefinition command : CustomCommandRegistry.getInstance().getCustomCommands()) {
            vegaControls.addAll(command.distinctBindingIds());
        }
        return new AiAccess(vegaControls);
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
     * the auto-assigner targets, so the Missing tab and Auto-Assign Missing Binds never diverge.
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
        return new BindingPartition(used, missing);
    }

    private record BindingPartition(List<String> used, List<String> missing) {
    }

    /**
     * Maps every conflicting binding id to the partners it collides with, grouped by the chord each
     * collision happens on. The {@code keySet} drives the RED/green row coloring; the grouped
     * partner lists feed the hover callout.
     */
    private ConflictIndex computeConflicts(
            Map<String, KeyBindingsParser.BindingSlots> bindings) {
        Map<String, Map<Set<String>, Set<String>>> byChord = new HashMap<>();
        for (BindingConflictScanner.Conflict conflict : BindingConflictScanner.scanSlots(bindings)) {
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

        /** The chords this binding is in conflict on, each with the partners it collides with there. */
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
     * Renders both tabs from {@link #currentSlots}, honoring the "show conflicts only" filter. Tab
     * counts and the Auto-Assign Missing Binds enabled state always reflect the full (unfiltered) data, so the
     * filter is a pure view toggle. Called on load and whenever the filter checkbox changes.
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

        renderGroupedTables(
                usedBindingsPanel,
                groupedBindings(filterRows(usedBindings, aiAccess), currentSlots, BindingsGroupTableFactory.RowAction.CLEAR, aiAccess),
                BindingsGroupTableFactory.RowAction.CLEAR,
                getText("bindings.column.action"),
                getText("bindings.column.primary"),
                getText("bindings.column.secondary"),
                getText("bindings.column.aiAccess"),
                getText("bindings.column.clear"));
        tabs.setTitleAt(0, getText("bindings.usedBindings", usedBindings.size()));

        renderGroupedTables(
                missingBindingsPanel,
                groupedBindings(filterRows(missingBindings, aiAccess), currentSlots, BindingsGroupTableFactory.RowAction.AUTO_FIX, aiAccess),
                BindingsGroupTableFactory.RowAction.AUTO_FIX,
                getText("bindings.column.action"),
                getText("bindings.column.primary"),
                getText("bindings.column.secondary"),
                getText("bindings.column.aiAccess"),
                getText("bindings.column.autofix"));
        tabs.setTitleAt(1, getText("bindings.missingBindings", missingBindings.size()));
        fixAllButton.setEnabled(!missingBindings.isEmpty());
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
     * Auto-Assign Missing Binds enabled state are computed from the unfiltered lists.
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
                .filter(id -> aiAccess.stateOf(id, currentSlots.get(id)) != AiAccess.State.NOT_USED)
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
        if (bindingSearchField == null) {
            return bindingIds;
        }
        String query = bindingSearchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            return bindingIds;
        }
        return bindingIds.stream()
                .filter(id -> searchText(id).contains(query))
                .toList();
    }

    private String searchText(String bindingId) {
        String text = BindingDisplayNames.searchText(bindingId, sectionLabelFor(bindingId));
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

    private boolean matchesChord(KeyBindingsParser.ReadOnlyBindingSlot slot, Set<String> chord) {
        return slot != null
                && slot.keyboardUsable()
                && chord.equals(BindingConflictScanner.chordOf(slot));
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
        return targetPanel == usedBindingsPanel ? usedBindingsScrollPane : missingBindingsScrollPane;
    }

    private void openAssignKeyboardBindingDialog(String bindingId, BindingSlotType slotType) {
        if (bindingId == null || bindingId.isBlank() || loadedFile == null || assignDialogOpen || changeRunning) {
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
                this,
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
                    SwingUtilities.getWindowAncestor(this),
                    () -> saveSelections(file, bindingId, dialog.showDialog(), slots));
        } finally {
            assignDialogOpen = false;
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
        offEdt(getText("bindings.assign.dialogTitle"),
                () -> Edits.stored().change(current -> {
                    BindsEdits.Changed changed = BindsEdits.change(current, file, changes);
                    result.set(changed.result());
                    return changed.files();
                }),
                () -> saveResultPresenter.show(result.get()), () -> { });
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
        if (bindingId == null || bindingId.isBlank() || loadedFile == null || changeRunning) {
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
                this, bindingId, slots, slotFormatter,
                (slotType, done) -> clearKeyboardSlot(file, bindingId, slotType, done));
        AppTheme.runWithModalScrim(SwingUtilities.getWindowAncestor(this), dialog::showDialog);
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
        offEdt(getText("bindings.clear.dialogTitle"), () -> {
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

    /** A change to the edits, run by {@link #offEdt}. */
    @FunctionalInterface
    private interface Change {
        void run() throws Exception;
    }

    /**
     * Changes the edits off the EDT, one change at a time, then runs {@code onEdt} on it. A failure is shown under
     * {@code title}, not only logged - the user pressed a button and must be told - and {@code afterFailure} runs
     * instead. The screen reads the edits again either way.
     */
    private void offEdt(String title, Change change, Runnable onEdt, Runnable afterFailure) {
        changeRunning = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        fixAllButton.setEnabled(false);
        new Thread(() -> {
            String failure = null;
            try {
                change.run();
            } catch (Edits.WriteRunningException e) {
                // WHY: a write is running - said in the user's language, not the exception's.
                log.info("{} not done: {}", title, e.getMessage());
                failure = getText("bindings.writeRunning");
            } catch (Exception e) {
                // WHY: broad on purpose - a thread boundary, and anything escaping it would leave the screen waiting
                // with no word of why.
                log.warn("{} failed", title, e);
                failure = getText("bindings.change.failed");
            }
            String shown = failure;
            SwingUtilities.invokeLater(() -> {
                changeRunning = false;
                setCursor(Cursor.getDefaultCursor());
                initData();
                if (shown != null) {
                    info(this, title, shown, getText("button.close"));
                    afterFailure.run();
                    return;
                }
                onEdt.run();
            });
        }, "BindForge-BindEditorChange").start();
    }
}
