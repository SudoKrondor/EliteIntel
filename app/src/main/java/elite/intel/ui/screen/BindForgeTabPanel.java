package elite.intel.ui.screen;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.hands.*;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.DataDirectoryValidator;
import elite.intel.session.PlayerSession;
import elite.intel.ui.dialog.AssignKeyboardBindingDialog;
import elite.intel.ui.event.BindingsSummaryChangedEvent;
import elite.intel.ui.event.BindingsUpdatedEvent;
import elite.intel.ui.event.KeymapSyncStateChangedEvent;
import elite.intel.ui.support.*;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.*;
import elite.intel.util.StringUtls;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.sizeFieldLabel;
import static elite.intel.ui.theme.HudGlyphs.verticalEllipsisIcon;
import static elite.intel.ui.theme.HudPalette.*;

public class BindForgeTabPanel extends JPanel {

    private static final int SCROLL_UNIT_ROWS = 2;

    private final BindingsLoader loader = new BindingsLoader();
    private final KeyBindingsParser parser = KeyBindingsParser.getInstance();
    private final BindingsMonitor monitor = BindingsMonitor.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final KeyboardKeyAvailabilityService availabilityService = new KeyboardKeyAvailabilityService();
    private final BindingsWriter bindingsWriter = new BindingsWriter();
    private final BindingSlotDisplayFormatter slotFormatter = new BindingSlotDisplayFormatter();
    private final BindingsSelectionController selectionController;
    private final BindingsGroupTableFactory tableFactory;
    private final BindingsWorkingCopyRepository workingCopyRepo = new BindingsWorkingCopyRepository();
    private final BindingsApplyService applyService = new BindingsApplyService();
    private final MissingBindingAutoAssigner autoAssigner = new MissingBindingAutoAssigner();

    private JTextField profileField;
    private JTextField filePathField;
    private JTextField bindingsDirField;
    private JPanel keyboardOnlyBanner;
    private BindingSaveResultPresenter saveResultPresenter;
    private JPanel usedBindingsPanel;
    private JPanel missingBindingsPanel;
    private JScrollPane usedBindingsScrollPane;
    private JScrollPane missingBindingsScrollPane;
    private JTabbedPane tabs;
    private StatusBadge syncStatusBadge;
    private JButton applyButton;
    private JButton revertButton;
    private JButton fixAllButton;

    private Map<String, KeyBindingsParser.ReadOnlyBindingSlots> currentSlots = Map.of();
    /**
     * Ids of bindings in a conflict, driving the RED row coloring; recomputed on each load.
     */
    private Set<String> conflictedBindings = Set.of();
    /** Working copy file currently loaded in the editor - used for stale checks. */
    private File activeBindingsFile;
    private FileTime activeBindingsLastModified;
    private long activeBindingsFileSize = -1;
    /** The actual game binds file - source for Apply and for the file-path display field. */
    private File gameBindingsFile;
    /** The preset file name (e.g. {@code Custom.3.0.binds}) - key for the working copy. */
    private String activePresetFileName;
    private boolean assignDialogOpen;
    private boolean autoFixInProgress;

    public BindForgeTabPanel() {
        selectionController = new BindingsSelectionController();
        tableFactory = new BindingsGroupTableFactory(
                selectionController, this::openAssignKeyboardBindingDialog, this::autoFixSingleBinding,
                // Lambda (not conflictedBindings::contains) so it reads the field live: the set is
                // reassigned on each load, and a bound method ref would pin the initial empty set.
                id -> conflictedBindings.contains(id));
        buildUi();
        saveResultPresenter = new BindingSaveResultPresenter(this);
        UiBus.register(this);
    }

    public void dispose() {
        UiBus.unregister(this);
    }

    @Subscribe
    public void onBindingsUpdated(BindingsUpdatedEvent event) {
        SwingUtilities.invokeLater(this::initData);
    }

    /**
     * Returns {@code true} if a draft working copy exists that differs from the
     * current game file. Used by the parent window to decide whether to show a
     * close confirmation dialog.
     */
    public boolean hasUnappliedChanges() {
        if (activePresetFileName == null || gameBindingsFile == null) {
            return false;
        }
        try {
            return workingCopyRepo.hasUnappliedDraft(activePresetFileName, gameBindingsFile.toPath());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Shows a modal dialog when the user closes the application with an unapplied
     * draft. Offers Apply, Keep Draft, or Discard. Blocks until the user responds.
     * Must be called on the EDT.
     */
    public void promptCloseWithDraft() {
        if (!hasUnappliedChanges()) {
            return;
        }
        Object[] options = {
                getText("bindings.close.draft.apply"),
                getText("bindings.close.draft.keep"),
                getText("bindings.close.draft.discard")
        };
        int choice = JOptionPane.showOptionDialog(
                this,
                getText("bindings.close.draft.text"),
                getText("bindings.close.draft.title"),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]
        );
        if (choice == 0) {
            // Apply
            performApply();
        } else if (choice == 2) {
            // Discard
            if (activePresetFileName != null) {
                workingCopyRepo.delete(activePresetFileName);
            }
        }
        // choice == 1 (Keep Draft) or dialog closed: do nothing, draft is already on disk
    }

    private void buildUi() {
        setLayout(new BorderLayout(2, SCREEN_TOP_GAP));
        setBorder(hudSubtabContentBorder());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        JPanel details = compactProfilePanel();
        add(bindingProfileCard(details), BorderLayout.NORTH);

        usedBindingsPanel = groupedTablesPanel();
        missingBindingsPanel = groupedTablesPanel();
        usedBindingsScrollPane = groupedTablesScrollPane(usedBindingsPanel);
        missingBindingsScrollPane = groupedTablesScrollPane(missingBindingsPanel);

        tabs = AppTheme.makeCompactTabs();
        tabs.addTab(getText("bindings.usedBindings"), nestedTabContent(usedBindingsScrollPane));
        tabs.addTab(getText("bindings.missingBindings"), nestedTabContent(missingBindingsScrollPane));
        tabs.addChangeListener(e -> selectionController.clearSelection());

        add(tabs, BorderLayout.CENTER);

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

        // Single-column grid: [label | field | affordance]. All three rows share the label
        // column width and the field right edge; vertical-ellipsis and the in-field "i" line up on the right.
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

        // Row 1 - Profile (field spans the picker column so its right edge aligns)
        gbc.gridy = 1;
        addProfileLabel(details, getText("bindings.profileName"), gbc, 0, LABEL_COL_WIDTH);
        profileField = readOnlyInfoField("bindings.profileName.info");
        addProfileField(details, profileField, gbc, 1, 2, 1.0);

        // Row 2 - File
        gbc.gridy = 2;
        addProfileLabel(details, getText("bindings.filePath"), gbc, 0, LABEL_COL_WIDTH);
        filePathField = readOnlyInfoField("bindings.filePath.info");
        addProfileField(details, filePathField, gbc, 1, 2, 1.0);

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
        syncStatusBadge = new StatusBadge("", StatusBadge.State.INFO);

        revertButton = makeButtonSubtle(getText("bindings.button.revert.short"));
        revertButton.setToolTipText(getText("bindings.button.revert.tooltip"));
        revertButton.addActionListener(e -> revertFromGame());

        applyButton = makeButton(getText("bindings.button.apply.short"));
        applyButton.setToolTipText(getText("bindings.button.apply.tooltip"));
        applyButton.addActionListener(e -> performApply());

        fixAllButton = makeButtonSubtle(getText("bindings.button.fixAll.short"));
        fixAllButton.setToolTipText(getText("bindings.button.fixAll.tooltip"));
        fixAllButton.addActionListener(e -> fixAllMissing());

        // Non-modal footer: sync status on the left, FIX MISSING + REVERT + APPLY (primary) on the right, no BACK.
        return HudFooter.build(false, null, syncStatusBadge, List.of(fixAllButton, revertButton, applyButton));
    }

    public void initData() {
        selectionController.resetTables();
        bindingsDirField.setText(playerSession.getBindingsDir().toString());
        try {
            File resolvedGameFile = resolveGameBindsFile();
            String presetFileName = resolvedGameFile.getName();

            Path workingCopyPath = workingCopyRepo.loadOrImportFromGame(
                    presetFileName, resolvedGameFile.toPath());

            Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots =
                    parser.parseReadOnlyBindingSlots(workingCopyPath.toFile());
            Map<String, KeyBindingsParser.KeyBinding> parsedBindings = effectiveBindings(slots);
            conflictedBindings = computeConflictedBindings(parsedBindings);

            currentSlots = slots;
            activeBindingsFile = workingCopyPath.toFile();
            activeBindingsLastModified = Files.getLastModifiedTime(workingCopyPath);
            activeBindingsFileSize = Files.size(workingCopyPath);
            gameBindingsFile = resolvedGameFile;
            activePresetFileName = presetFileName;

            profileField.setText(activeProfileName(resolvedGameFile));
            filePathField.setText(resolvedGameFile.getAbsolutePath());

            // Tables show every keyboard-capable control (custom commands can target any of them),
            // split by whether it currently has a usable keyboard binding.
            BindingPartition partition = partitionByKeyboardBinding(slots);
            List<String> usedBindings = partition.used();
            List<String> missingBindings = partition.missing();

            renderGroupedTables(
                    usedBindingsPanel,
                    groupedBindings(usedBindings, slots, false),
                    getText("bindings.column.action"),
                    getText("bindings.column.primary"),
                    getText("bindings.column.secondary"));
            tabs.setTitleAt(0, getText("bindings.usedBindings", usedBindings.size()));

            renderGroupedTables(
                    missingBindingsPanel,
                    groupedBindings(missingBindings, slots, true),
                    getText("bindings.column.action"),
                    getText("bindings.column.primary"),
                    getText("bindings.column.secondary"),
                    getText("bindings.column.autofix"));
            tabs.setTitleAt(1, getText("bindings.missingBindings", missingBindings.size()));
            fixAllButton.setEnabled(!missingBindings.isEmpty());

            // The AiTab badge reflects only the controls EliteIntel itself drives.
            UiBus.publish(new BindingsSummaryChangedEvent(
                    monitor.findMissingGameBindings(parsedBindings).size(),
                    monitor.findFoundGameBindings(parsedBindings).size()));
        } catch (Exception e) {
            UiBus.publish(new BindingsSummaryChangedEvent(0, 0));
            conflictedBindings = Set.of();
            clearLoadedBindingsSnapshot();
            profileField.setText(getText("bindings.notAvailable"));
            filePathField.setText(getText("bindings.notAvailable"));
            renderGroupedTables(usedBindingsPanel, Map.of(), getText("bindings.column.action"));
            renderGroupedTables(missingBindingsPanel, Map.of(), getText("bindings.column.action"));
            tabs.setTitleAt(0, getText("bindings.usedBindings", 0));
            tabs.setTitleAt(1, getText("bindings.missingBindings", 0));
            fixAllButton.setEnabled(false);
        }
        updateSyncStatus();
    }

    private void updateSyncStatus() {
        if (applyButton == null || syncStatusBadge == null) {
            return;
        }
        boolean hasDraft = hasUnappliedChanges();

        syncStatusBadge.setStatus(
                hasDraft ? getText("bindings.status.draft.badge") : getText("bindings.status.synced.badge"),
                hasDraft ? StatusBadge.State.STANDBY : StatusBadge.State.OK);
        syncStatusBadge.setToolTipText(hasDraft ? getText("bindings.status.draft") : getText("bindings.status.synced"));

        applyButton.setEnabled(hasDraft && activePresetFileName != null);
        revertButton.setEnabled(activePresetFileName != null && workingCopyRepo.exists(activePresetFileName));

        UiBus.publish(new KeymapSyncStateChangedEvent(!hasDraft));
    }

    private void performApply() {
        if (activePresetFileName == null || gameBindingsFile == null) {
            return;
        }
        try {
            Path backupPath = applyService.apply(activePresetFileName, gameBindingsFile.toPath());
            // Elite only re-reads the .binds when its Controls screen is opened, so remind the
            // user to cycle that screen to actually load what we just wrote to the game file.
            String successMsg = (backupPath != null
                    ? getText("bindings.apply.success", backupPath.getFileName())
                    : getText("bindings.apply.success.noBackup"))
                    + System.lineSeparator() + System.lineSeparator()
                    + getText("bindings.apply.reloadReminder");
            // Also speak the reload reminder — users reflexively dismiss dialogs without reading.
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedSpeech("speech.bindingsAppliedReload")));
            JOptionPane.showMessageDialog(
                    this,
                    successMsg,
                    getText("bindings.apply.dialogTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
            updateSyncStatus();
        } catch (BindingsApplyException e) {
            String errorMessage = e.localizationKey() == null ? e.getMessage() : getText(e.localizationKey());
            JOptionPane.showMessageDialog(
                    this,
                    getText("bindings.apply.error", errorMessage),
                    getText("bindings.apply.dialogTitle"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fixAllMissing() {
        if (activeBindingsFile == null || currentSlots.isEmpty() || autoFixInProgress) {
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
        if (bindingId == null || bindingId.isBlank() || activeBindingsFile == null || autoFixInProgress) {
            return;
        }
        MissingBindingAutoAssigner.Plan plan = autoAssigner.planOne(bindingId, currentSlots);
        if (plan.edits().isEmpty() && plan.skipped().isEmpty()) {
            return; // already bound or unknown - nothing to do
        }
        applyPlanInBackground(plan, outcome -> showSingleResult(bindingId, outcome, plan));
    }

    /**
     * Writes a plan off the EDT. Each edit re-reads and rewrites the binds file
     * (and re-parses it for conflict checks), so a large batch would freeze the
     * UI if run on the dispatch thread. The file path is captured up front so the
     * worker never touches mutable UI state; the result dialog and reload are
     * marshalled back onto the EDT.
     */
    private void applyPlanInBackground(MissingBindingAutoAssigner.Plan plan, Consumer<ApplyOutcome> onComplete) {
        if (plan.edits().isEmpty()) {
            // Nothing to write (every target was skipped); report without touching the file.
            onComplete.accept(new ApplyOutcome(0, 0));
            return;
        }
        Path file = activeBindingsFile.toPath();
        setAutoFixBusy(true);
        new Thread(() -> {
            ApplyOutcome outcome = applyPlan(file, plan);
            SwingUtilities.invokeLater(() -> {
                setAutoFixBusy(false);
                initData();
                onComplete.accept(outcome);
            });
        }, "BindingsAutoFix-Thread").start();
    }

    private void setAutoFixBusy(boolean busy) {
        autoFixInProgress = busy;
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        if (busy) {
            fixAllButton.setEnabled(false);
        }
        // When clearing busy, initData() recomputes the button's enabled state.
    }

    /**
     * Writes each planned edit to the working copy via {@link BindingsWriter}.
     * The file's timestamp and size change after every successful write, so the
     * stale-check snapshot is refreshed before each edit.
     */
    private ApplyOutcome applyPlan(Path file, MissingBindingAutoAssigner.Plan plan) {
        int saved = 0;
        int failed = 0;
        for (MissingBindingAutoAssigner.PlannedEdit edit : plan.edits()) {
            FileTime lastModified;
            long size;
            try {
                lastModified = Files.getLastModifiedTime(file);
                size = Files.size(file);
            } catch (IOException e) {
                // The working copy became unreadable mid-batch: count every
                // remaining edit as failed so the summary total stays honest.
                failed = plan.edits().size() - saved;
                break;
            }
            KeyboardBindingEdit kbe = new KeyboardBindingEdit(
                    file, edit.bindingId(), edit.slotType(), edit.key(), lastModified, size);
            BindingSaveResult result = edit.modifier() == null
                    ? bindingsWriter.assignKeyboardKey(kbe)
                    : bindingsWriter.assignKeyboardKeyWithModifier(kbe, edit.modifier());
            if (result == BindingSaveResult.SAVED) {
                saved++;
            } else {
                failed++;
            }
        }
        return new ApplyOutcome(saved, failed);
    }

    private void showBatchSummary(ApplyOutcome outcome, MissingBindingAutoAssigner.Plan plan) {
        StringBuilder sb = new StringBuilder(getText("bindings.autofix.summary", outcome.saved()));
        if (outcome.failed() > 0) {
            sb.append('\n').append(getText("bindings.autofix.failed", outcome.failed()));
        }
        appendReason(sb, plan, MissingBindingAutoAssigner.SkipReason.BOTH_SLOTS_OCCUPIED, "bindings.autofix.skipped.bothSlots");
        appendReason(sb, plan, MissingBindingAutoAssigner.SkipReason.NO_FREE_KEY, "bindings.autofix.skipped.noFreeKey");
        appendReason(sb, plan, MissingBindingAutoAssigner.SkipReason.NO_EDITABLE_SLOT, "bindings.autofix.skipped.notEditable");

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
                    getText("bindings.autofix.single.success", bindingId),
                    getText("bindings.autofix.result.title"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (outcome.failed() > 0) {
            JOptionPane.showMessageDialog(
                    this,
                    getText("bindings.assign.writeFailed"),
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
        };
        JOptionPane.showMessageDialog(
                this, getText(messageKey), getText("bindings.autofix.result.title"), JOptionPane.WARNING_MESSAGE);
    }

    private record ApplyOutcome(int saved, int failed) {
    }

    private void revertFromGame() {
        if (activePresetFileName == null || gameBindingsFile == null) {
            return;
        }
        int response = JOptionPane.showConfirmDialog(
                this,
                getText("bindings.revert.confirm.text"),
                getText("bindings.revert.confirm.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (response == JOptionPane.YES_OPTION) {
            workingCopyRepo.delete(activePresetFileName);
            initData();
        }
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
            playerSession.setBindingsDir(path);
            bindingsDirField.setText(path);
            DataDirectoryValidator.validateAndWarn(playerSession.getBindingsDir(), DataDirectoryValidator.DirectoryKind.BINDINGS);
            initData();
        }
    }

    private File resolveGameBindsFile() throws Exception {
        File currentFile = monitor.getCurrentBindsFile();
        return currentFile != null ? currentFile : loader.getLatestBindsFile();
    }

    private String activeProfileName(File bindingsFile) {
        String presetName = loader.getActivePresetName();
        if (presetName != null && !presetName.isBlank())
            return presetName;

        String fileName = bindingsFile.getName();
        int profileEnd = fileName.indexOf('.');
        return profileEnd > 0 ? fileName.substring(0, profileEnd) : fileName;
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

    /**
     * Creates a read-only value field carrying an in-field info-"i" (HUD section 5.1) that opens the
     * help text for {@code infoKey} on click - replaces the former external Unicode info button.
     */
    private JTextField readOnlyInfoField(String infoKey) {
        HudTextField field = makeTextField(() -> showFieldInfo(infoKey));
        field.setEditable(false);
        return field;
    }

    private void showFieldInfo(String messageKey) {
        JOptionPane.showMessageDialog(
                this,
                getText(messageKey),
                getText("bindings.info.title"),
                JOptionPane.INFORMATION_MESSAGE);
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

    private Map<BindingGroup, List<Object[]>> groupedBindings(
            List<String> bindingIds,
            Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots,
            boolean withAutoFix
    ) {
        Map<BindingGroup, List<Object[]>> grouped = groupedRows();
        for (String bindingId : bindingIds) {
            KeyBindingsParser.ReadOnlyBindingSlots bindingSlots = slots.get(bindingId);
            String primary = slotFormatter.formatSlot(bindingSlots == null ? null : bindingSlots.primary());
            String secondary = slotFormatter.formatSlot(bindingSlots == null ? null : bindingSlots.secondary());
            Object[] row = withAutoFix
                    ? new Object[]{bindingId, primary, secondary, getText("bindings.column.autofix.action")}
                    : new Object[]{bindingId, primary, secondary};
            grouped.get(BindingGroupClassifier.classify(bindingId)).add(row);
        }
        return grouped;
    }

    /**
     * Splits all keyboard-capable controls into those that already have a usable
     * keyboard binding and those that do not, using the same definition of "bound"
     * the auto-assigner targets, so the Missing tab and Fix Missing never diverge.
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
        used.sort(String::compareToIgnoreCase);
        missing.sort(String::compareToIgnoreCase);
        return new BindingPartition(used, missing);
    }

    private record BindingPartition(List<String> used, List<String> missing) {
    }

    /**
     * Builds the same keyboard-only view that command execution uses while keeping
     * diagnostic slots available for tables.
     * <p>
     * Non-keyboard slots remain visible in the read-only UI, but they are not included
     * in this map and therefore still count as missing for EliteIntel command execution.
     */
    private Map<String, KeyBindingsParser.KeyBinding> effectiveBindings(
            Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots) {
        Map<String, KeyBindingsParser.KeyBinding> bindings = new HashMap<>();
        for (Map.Entry<String, KeyBindingsParser.ReadOnlyBindingSlots> entry : slots.entrySet()) {
            KeyBindingsParser.KeyBinding keyBinding = executableBinding(entry.getValue().primary());
            if (keyBinding == null)
                keyBinding = executableBinding(entry.getValue().secondary());
            if (keyBinding != null)
                bindings.put(entry.getKey(), keyBinding);
        }
        return bindings;
    }

    private KeyBindingsParser.KeyBinding executableBinding(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        if (slot == null || !slot.keyboardUsable())
            return null;
        return parser.new KeyBinding(slot.key(), slot.modifiers(), slot.hold());
    }

    /**
     * The ids of all bindings that participate in a conflict, for the RED/green row coloring.
     */
    private Set<String> computeConflictedBindings(
            Map<String, KeyBindingsParser.KeyBinding> bindings) {
        Set<String> conflicted = new HashSet<>();
        for (BindingConflictScanner.Conflict conflict : BindingConflictScanner.scan(bindings)) {
            conflicted.add(conflict.actionA());
            conflicted.add(conflict.actionB());
        }
        return conflicted;
    }

    private Map<BindingGroup, List<Object[]>> groupedRows() {
        Map<BindingGroup, List<Object[]>> grouped = new EnumMap<>(BindingGroup.class);
        for (BindingGroup group : BindingGroup.values()) {
            grouped.put(group, new ArrayList<>());
        }
        return grouped;
    }

    private void renderGroupedTables(JPanel targetPanel, Map<BindingGroup, List<Object[]>> grouped,
            String... columnNames) {
        targetPanel.removeAll();
        for (BindingGroup group : BindingGroup.values()) {
            List<Object[]> rows = grouped.getOrDefault(group, List.of()).stream()
                    .sorted(Comparator.comparing(row -> row[0].toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
            if (rows.isEmpty())
                continue;

            targetPanel.add(sectionHeader(group));
            targetPanel.add(tableFactory.groupTable(rows, outerScrollPaneFor(targetPanel), columnNames));
            targetPanel.add(Box.createVerticalStrut(6));
        }
        targetPanel.add(Box.createVerticalGlue());
        targetPanel.revalidate();
        targetPanel.repaint();
    }

    private JComponent sectionHeader(BindingGroup group) {
        JLabel label = hudGroupLabel(getText(group.getLabelKey()).toUpperCase());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(10, 8, 10, 0));
        return label;
    }

    private JScrollPane outerScrollPaneFor(JPanel targetPanel) {
        return targetPanel == usedBindingsPanel ? usedBindingsScrollPane : missingBindingsScrollPane;
    }

    private void openAssignKeyboardBindingDialog(String bindingId, BindingSlotType slotType) {
        if (bindingId == null || bindingId.isBlank() || activeBindingsFile == null || assignDialogOpen) {
            return;
        }

        KeyBindingsParser.ReadOnlyBindingSlots slots = currentSlots.get(bindingId);
        KeyBindingsParser.ReadOnlyBindingSlot slot = slotType == BindingSlotType.PRIMARY
                ? (slots == null ? null : slots.primary())
                : (slots == null ? null : slots.secondary());
        if (!isBasicEditableSlot(slot)) {
            JOptionPane.showMessageDialog(
                    this,
                    getText("bindings.assign.unsupportedReadOnlyMessage"),
                    getText("bindings.assign.dialogTitle"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        AssignKeyboardBindingDialog dialog = new AssignKeyboardBindingDialog(
                this,
                activeBindingsFile.toPath(),
                bindingId,
                slotType,
                slot,
                availabilityService,
                effectiveBindings(currentSlots)
        );

        assignDialogOpen = true;
        try {
            dialog.showDialog().ifPresent(selection -> saveKeyboardBinding(bindingId, selection));
        } finally {
            assignDialogOpen = false;
        }
    }

    private void saveKeyboardBinding(String bindingId, AssignKeyboardBindingSelection selection) {
        if (activeBindingsFile == null || activeBindingsLastModified == null || activeBindingsFileSize < 0) {
            saveResultPresenter.showWriteFailed();
            return;
        }

        Path file = activeBindingsFile.toPath();
        KeyboardBindingEdit edit = new KeyboardBindingEdit(
                file,
                bindingId,
                selection.slotType(),
                selection.key(),
                activeBindingsLastModified,
                activeBindingsFileSize
        );
        BindingSaveResult result = saveKeyboardBinding(edit, selection.modifiers());
        saveResultPresenter.show(result);

        if (result == BindingSaveResult.SAVED || result == BindingSaveResult.NO_CHANGE || result == BindingSaveResult.STALE_FILE) {
            initData();
        }
    }

    private BindingSaveResult saveKeyboardBinding(KeyboardBindingEdit edit, List<BindingModifier> modifiers) {
        return modifiers == null || modifiers.isEmpty()
                ? bindingsWriter.assignKeyboardKey(edit)
                : bindingsWriter.assignKeyboardKeyWithModifiers(edit, modifiers);
    }

    private boolean isBasicEditableSlot(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        return slot == null || slot.editable() || isClearedSlot(slot);
    }

    private boolean isClearedSlot(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        return "{NoDevice}".equals(slot.device())
                && (slot.key() == null || slot.key().isBlank());
    }

    private void clearLoadedBindingsSnapshot() {
        currentSlots = Map.of();
        activeBindingsFile = null;
        activeBindingsLastModified = null;
        activeBindingsFileSize = -1;
        gameBindingsFile = null;
        activePresetFileName = null;
    }
}
