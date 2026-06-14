package elite.intel.ui.view;

import elite.intel.ai.hands.BindingGroup;
import elite.intel.ai.hands.BindingGroupClassifier;
import elite.intel.ai.hands.BindingModifier;
import elite.intel.ai.hands.BindingsApplyException;
import elite.intel.ai.hands.BindingsApplyService;
import elite.intel.ai.hands.BindingSaveResult;
import elite.intel.ai.hands.BindingSlotType;
import elite.intel.ai.hands.BindingsLoader;
import elite.intel.ai.hands.BindingsMonitor;
import elite.intel.ai.hands.BindingsWorkingCopyRepository;
import elite.intel.ai.hands.BindingsWriter;
import elite.intel.ai.hands.KeyBindingsParser;
import elite.intel.ai.hands.KeyboardBindingEdit;
import elite.intel.ai.hands.KeyboardKeyAvailabilityService;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.BindingsSummaryChangedEvent;
import elite.intel.ui.event.BindingsUpdatedEvent;
import elite.intel.ui.event.KeymapSyncStateChangedEvent;
import com.google.common.eventbus.Subscribe;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.view.AppTheme.*;

public class BindingsTabPanel extends JPanel {

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
    private final List<JButton> headerInfoButtons = new ArrayList<>();

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

    private Map<String, KeyBindingsParser.ReadOnlyBindingSlots> currentSlots = Map.of();
    /** Working copy file currently loaded in the editor — used for stale checks. */
    private File activeBindingsFile;
    private FileTime activeBindingsLastModified;
    private long activeBindingsFileSize = -1;
    /** The actual game binds file — source for Apply and for the file-path display field. */
    private File gameBindingsFile;
    /** The preset file name (e.g. {@code Custom.3.0.binds}) — key for the working copy. */
    private String activePresetFileName;
    private boolean assignDialogOpen;

    public BindingsTabPanel() {
        selectionController = new BindingsSelectionController();
        tableFactory = new BindingsGroupTableFactory(selectionController, this::openAssignKeyboardBindingDialog);
        buildUi();
        saveResultPresenter = new BindingSaveResultPresenter(this);
        EventBusManager.register(this);
    }

    public void dispose() {
        EventBusManager.unregister(this);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::applyHeaderDisplayStyle);
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
        return !workingCopyRepo.isSyncedWithGame(activePresetFileName, gameBindingsFile.toPath());
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
        setBackground(HUD_BG);

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

        addProfileLabel(details, getText("player.bindingsDirectory"), gbc, 0, 128);
        bindingsDirField = readOnlyField();
        bindingsDirField.setToolTipText(getText("player.bindingsDirectory.tooltip"));
        addProfileField(details, bindingsDirField, gbc, 1, 6, 1.0);

        JButton selectBindingsDirButton = compactDirectoryChooserButton();
        selectBindingsDirButton.addActionListener(e -> selectBindingsDirectory());
        gbc.gridx = 7;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        details.add(selectBindingsDirButton, gbc);

        gbc.gridy = 1;
        addProfileLabel(details, getText("bindings.profileName"), gbc, 0, 80);
        profileField = readOnlyMetadataField();
        addProfileField(details, profileField, gbc, 1, 1, 0.28);
        addInfoButton(details, gbc, 2, "bindings.profileName.info");

        addProfileSpacer(details, gbc, 3, 24);
        addProfileLabel(details, getText("bindings.filePath"), gbc, 4, 58);
        filePathField = readOnlyMetadataField();
        addProfileField(details, filePathField, gbc, 5, 2, 0.72);
        addInfoButton(details, gbc, 7, "bindings.filePath.info");

        profileCardBody.add(details, BorderLayout.CENTER);
        return profileCardBody;
    }

    private JComponent bindingProfileCard(JPanel body) {
        HudSection card = new HudSection(
                getText("bindings.section.profile"),
                new BorderLayout(),
                HudPanel.Variant.FRAMED,
                6,
                HUD_ORANGE_SOFT);
        card.body().add(body, BorderLayout.CENTER);
        card.setFooter(keyboardOnlyWarningStrip(), HUD_WARN_BG);

        JPanel wrapper = transparentPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildFooter() {
        syncStatusBadge = new StatusBadge("", StatusBadge.State.INFO);

        JPanel statusArea = transparentPanel(new GridBagLayout());
        GridBagConstraints statusGbc = new GridBagConstraints();
        statusGbc.anchor = GridBagConstraints.WEST;
        statusGbc.weightx = 1.0;
        statusGbc.weighty = 1.0;
        statusGbc.fill = GridBagConstraints.NONE;
        statusArea.add(syncStatusBadge, statusGbc);

        revertButton = makeButtonSubtle(getText("bindings.button.revert.short"));
        revertButton.setToolTipText(getText("bindings.button.revert.tooltip"));
        revertButton.addActionListener(e -> revertFromGame());

        applyButton = makeButton(getText("bindings.button.apply.short"));
        applyButton.setToolTipText(getText("bindings.button.apply.tooltip"));
        applyButton.addActionListener(e -> performApply());

        JPanel footer = new JPanel(new BorderLayout(HUD_GAP, 0));
        footer.setOpaque(true);
        footer.setBackground(HUD_BG);
        footer.setBorder(hudFooterSeparatorBorder());

        JPanel buttonBar = transparentPanel(new GridBagLayout());
        GridBagConstraints btnGbc = new GridBagConstraints();
        btnGbc.anchor = GridBagConstraints.CENTER;
        btnGbc.weighty = 1.0;
        btnGbc.insets = new Insets(0, HUD_GAP, 0, 0);
        buttonBar.add(revertButton, btnGbc);
        btnGbc.gridx = 1;
        buttonBar.add(applyButton, btnGbc);

        footer.add(statusArea, BorderLayout.CENTER);
        footer.add(buttonBar, BorderLayout.EAST);
        return footer;
    }

    public void initData() {
        applyOuterScrollPaneDataPlaneBorders();
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

            currentSlots = slots;
            activeBindingsFile = workingCopyPath.toFile();
            activeBindingsLastModified = Files.getLastModifiedTime(workingCopyPath);
            activeBindingsFileSize = Files.size(workingCopyPath);
            gameBindingsFile = resolvedGameFile;
            activePresetFileName = presetFileName;

            profileField.setText(activeProfileName(resolvedGameFile));
            filePathField.setText(resolvedGameFile.getAbsolutePath());

            List<String> usedBindings = monitor.findFoundGameBindings(parsedBindings).stream()
                    .sorted(String::compareToIgnoreCase)
                    .toList();
            renderGroupedTables(
                    usedBindingsPanel,
                    groupedBindings(usedBindings, slots),
                    getText("bindings.column.action"),
                    getText("bindings.column.primary"),
                    getText("bindings.column.secondary"));
            tabs.setTitleAt(0, getText("bindings.usedBindings", usedBindings.size()));

            List<String> missingBindings = monitor.findMissingGameBindings(parsedBindings).stream()
                    .sorted(String::compareToIgnoreCase)
                    .toList();
            renderGroupedTables(
                    missingBindingsPanel,
                    groupedBindings(missingBindings, slots),
                    getText("bindings.column.action"),
                    getText("bindings.column.primary"),
                    getText("bindings.column.secondary"));
            tabs.setTitleAt(1, getText("bindings.missingBindings", missingBindings.size()));
            EventBusManager.publish(new BindingsSummaryChangedEvent(missingBindings.size(), usedBindings.size()));
        } catch (Exception e) {
            EventBusManager.publish(new BindingsSummaryChangedEvent(0, 0));
            clearLoadedBindingsSnapshot();
            profileField.setText(getText("bindings.notAvailable"));
            filePathField.setText(getText("bindings.notAvailable"));
            renderGroupedTables(usedBindingsPanel, Map.of(), getText("bindings.column.action"));
            renderGroupedTables(missingBindingsPanel, Map.of(), getText("bindings.column.action"));
            tabs.setTitleAt(0, getText("bindings.usedBindings", 0));
            tabs.setTitleAt(1, getText("bindings.missingBindings", 0));
        }
        updateSyncStatus();
    }

    private void updateSyncStatus() {
        if (applyButton == null || syncStatusBadge == null) {
            return;
        }
        boolean synced = activePresetFileName == null
                || gameBindingsFile == null
                || workingCopyRepo.isSyncedWithGame(activePresetFileName, gameBindingsFile.toPath());

        syncStatusBadge.setStatus(
                synced ? getText("bindings.status.synced.badge") : getText("bindings.status.draft.badge"),
                synced ? StatusBadge.State.OK : StatusBadge.State.STANDBY);
        syncStatusBadge.setToolTipText(synced ? getText("bindings.status.synced") : getText("bindings.status.draft"));

        applyButton.setEnabled(!synced && activePresetFileName != null);
        revertButton.setEnabled(activePresetFileName != null && workingCopyRepo.exists(activePresetFileName));

        EventBusManager.publish(new KeymapSyncStateChangedEvent(synced));
    }

    private void performApply() {
        if (activePresetFileName == null || gameBindingsFile == null) {
            return;
        }
        try {
            Path backupPath = applyService.apply(activePresetFileName, gameBindingsFile.toPath());
            String successMsg = backupPath != null
                    ? getText("bindings.apply.success", backupPath.getFileName())
                    : getText("bindings.apply.success.noBackup");
            JOptionPane.showMessageDialog(
                    this,
                    successMsg,
                    getText("bindings.apply.dialogTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
            updateSyncStatus();
        } catch (BindingsApplyException e) {
            JOptionPane.showMessageDialog(
                    this,
                    getText("bindings.apply.error", e.getMessage()),
                    getText("bindings.apply.dialogTitle"),
                    JOptionPane.ERROR_MESSAGE);
        }
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
        JLabel label = new JLabel(text);
        label.setForeground(FG_MUTED);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, HUD_FONT_XS));
        label.setPreferredSize(new Dimension(width, HEADER_ROW_HEIGHT));
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

    private void addProfileSpacer(JPanel panel, GridBagConstraints gbc, int column, int width) {
        gbc.gridx = column;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(Box.createHorizontalStrut(width), gbc);
    }

    private void addHeaderLabel(JPanel panel, String text, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel label = new JLabel(text);
        label.setForeground(FG_MUTED);
        label.setPreferredSize(new Dimension(180, HEADER_ROW_HEIGHT));
        panel.add(label, gbc);
    }

    private void resetHeaderRow(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
    }

    private static final int HEADER_ROW_HEIGHT = 24;

    private JTextField readOnlyField() {
        JTextField field = makeTextField();
        field.setEditable(false);
        field.setFont(field.getFont().deriveFont(Font.PLAIN, HUD_FONT_FIELD_VALUE));
        field.setPreferredSize(new Dimension(0, HEADER_ROW_HEIGHT));
        return field;
    }

    private JTextField readOnlyMetadataField() {
        JTextField field = makeMetadataField();
        field.setPreferredSize(new Dimension(0, HEADER_ROW_HEIGHT));
        return field;
    }

    private JButton compactDirectoryChooserButton() {
        JButton button = makeFieldButton(verticalEllipsisIcon(HEADER_ROW_HEIGHT), HEADER_ROW_HEIGHT);
        button.setToolTipText(getText("player.bindingsDirectory.select.tooltip"));
        return button;
    }

    private void addInfoButton(JPanel panel, GridBagConstraints gbc, String messageKey) {
        addInfoButton(panel, gbc, 2, messageKey);
    }

    private void addInfoButton(JPanel panel, GridBagConstraints gbc, int column, String messageKey) {
        JButton button = new JButton("\u24D8");
        String message = getText(messageKey);
        button.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                message,
                getText("bindings.info.title"),
                JOptionPane.INFORMATION_MESSAGE));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setForeground(FG_MUTED);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(button.getFont().deriveFont(Font.PLAIN, AppTheme.HUD_FONT_ICON_BUTTON));
        Dimension size = new Dimension(HEADER_ROW_HEIGHT, HEADER_ROW_HEIGHT);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        headerInfoButtons.add(button);

        gbc.gridx = column;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(2, 0, 6, 7);
        panel.add(button, gbc);
    }

    private void applyHeaderDisplayStyle() {
        styleReadOnlyValueField(profileField);
        styleReadOnlyValueField(filePathField);
        headerInfoButtons.forEach(this::styleHeaderInfoButton);
    }

    private void styleReadOnlyValueField(JTextField field) {
        if (field == null) {
            return;
        }
        if (field instanceof HudMetadataField) {
            return;
        }
        field.setOpaque(false);
        field.setBackground(HUD_PANEL_BG);
        field.setForeground(FG_MUTED);
        field.setCaretColor(FG_MUTED);
        field.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
    }

    private void styleHeaderInfoButton(JButton button) {
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setForeground(FG_MUTED);
        button.setBackground(HUD_PANEL_BG);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(button.getFont().deriveFont(Font.PLAIN, AppTheme.HUD_FONT_ICON_BUTTON));
        Dimension size = new Dimension(HEADER_ROW_HEIGHT, HEADER_ROW_HEIGHT);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
    }

    private JPanel keyboardOnlyWarningStrip() {
        JPanel strip = new JPanel(new GridBagLayout());
        strip.setOpaque(false);
        strip.setBackground(HUD_WARN_BG);
        strip.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JLabel message = new JLabel("\u26A0  " + getText("bindings.keyboardOnlyHint"), SwingConstants.CENTER);
        message.setForeground(HUD_WARN);
        message.setFont(message.getFont().deriveFont(Font.BOLD, HUD_FONT_XS));
        message.putClientProperty("eliteIntel.hud.lockedForeground", Boolean.TRUE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        strip.add(message, gbc);

        keyboardOnlyBanner = strip;
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
        scrollPane.getViewport().setBackground(HUD_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(BindingsGroupTableFactory.TABLE_ROW_HEIGHT * SCROLL_UNIT_ROWS);
        scrollPane.setBorder(hudDataPlaneBorder());
        return scrollPane;
    }

    private void applyOuterScrollPaneDataPlaneBorders() {
        // AppTheme restyles scroll panes after buildUi; restore the Bindings data-plane frame
        // (border AND viewport bg) after palette passes — styleScrollPane resets viewport to HUD_PANEL_BG.
        if (usedBindingsScrollPane != null) {
            usedBindingsScrollPane.setBorder(hudDataPlaneBorder());
            usedBindingsScrollPane.getViewport().setBackground(HUD_BG);
        }
        if (missingBindingsScrollPane != null) {
            missingBindingsScrollPane.setBorder(hudDataPlaneBorder());
            missingBindingsScrollPane.getViewport().setBackground(HUD_BG);
        }
    }

    private JPanel nestedTabContent(JComponent content) {
        JPanel panel = transparentPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private Map<BindingGroup, List<Object[]>> groupedBindings(
            List<String> bindingIds,
            Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots
    ) {
        Map<BindingGroup, List<Object[]>> grouped = groupedRows();
        for (String bindingId : bindingIds) {
            KeyBindingsParser.ReadOnlyBindingSlots bindingSlots = slots.get(bindingId);
            grouped.get(BindingGroupClassifier.classify(bindingId)).add(new Object[] {
                    bindingId,
                    slotFormatter.formatSlot(bindingSlots == null ? null : bindingSlots.primary()),
                    slotFormatter.formatSlot(bindingSlots == null ? null : bindingSlots.secondary())
            });
        }
        return grouped;
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
                availabilityService
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
        BindingSaveResult result = saveKeyboardBinding(edit, selection.modifier());
        saveResultPresenter.show(result);

        if (result == BindingSaveResult.SAVED || result == BindingSaveResult.NO_CHANGE || result == BindingSaveResult.STALE_FILE) {
            initData();
        }
    }

    private BindingSaveResult saveKeyboardBinding(KeyboardBindingEdit edit, BindingModifier modifier) {
        return modifier == null
                ? bindingsWriter.assignKeyboardKey(edit)
                : bindingsWriter.assignKeyboardKeyWithModifier(edit, modifier);
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
