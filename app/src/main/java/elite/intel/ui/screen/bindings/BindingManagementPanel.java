package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.io.BindingsApplyException;
import elite.intel.bindforge.io.BindingsMonitor;
import elite.intel.bindforge.io.PlayerBackupService;
import elite.intel.bindforge.storage.Backup;
import elite.intel.bindforge.storage.BackupDestination;
import elite.intel.bindforge.storage.Backups;
import elite.intel.db.managers.BindForgeSettingsManager;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.support.BindingApplyResultPresenter;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudFooter;
import elite.intel.ui.widget.HudMetadataField;
import elite.intel.ui.widget.HudPanel;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudStepper;
import elite.intel.ui.widget.HudTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND;

/**
 * The "Binding Management" sub-tab of BIND FORGE, which holds two tabs of its own.
 * <p>
 * <strong>Game Install Locations</strong> is where BindForge looks for the files it manages.
 * <strong>Player Backups</strong> lists every backup ({@link Backups}) newest first, with a <em>Taken</em> column
 * naming its kind, and holds the backup settings: where backups go, and the two retention rules (file-manager.md,
 * <em>Settings</em>; Storage S1b, 2026-10-25).
 * <p>
 * <strong>Delete is for user backups only</strong> (Alan, 2026-10-25): the user made them, so they decide when they
 * go. Retention looks after daily and before-changes backups, and first setup's is kept forever.
 * <p>
 * Both restore actions share the same first step (loading the backup's file for the active
 * preset into the working copy as the new draft): "Restore to Editing Slot" stops there, the
 * Binding Profile tab picks up the new draft reactively via the existing
 * {@code BindingsUpdatedEvent}. "Restore to Live" continues into the existing safe-apply
 * pipeline ({@code BindingsApplyService.apply()}), so it gets that pipeline's own
 * conflict-check and pre-write backup rather than a separate, less-safe direct write.
 * RESTORE replaces both (Storage S3).
 */
public class BindingManagementPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(BindingManagementPanel.class);
    private static final DateTimeFormatter CREATED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int BEFORE_CHANGES_STEP = 10;

    private final Backups backups = Backups.stored();
    private final BindForgeSettingsManager settings = BindForgeSettingsManager.getInstance();
    private final PlayerBackupService restoreService = PlayerBackupService.getInstance();
    private final BindingApplyResultPresenter applyResultPresenter = new BindingApplyResultPresenter(this);

    private final GameInstallLocationsPanel gameInstallLocationsPanel = new GameInstallLocationsPanel();

    private DefaultTableModel tableModel;
    private JTable table;
    private List<Backup> currentBackups = List.of();

    private JButton backupNowButton;
    private JButton deleteButton;
    private JButton restoreToEditingSlotButton;
    private JButton restoreToLiveButton;
    private boolean operationInProgress;
    /** The newest list refresh asked for. Read and written on the EDT only. */
    private int latestRefresh;

    private HudMetadataField destinationField;
    private HudComboBox<Integer> dailyRetentionCombo;
    private HudStepper beforeChangesStepper;
    /** True while the settings controls are being filled from the database, so filling them saves nothing. EDT only. */
    private boolean loadingSettings;

    public BindingManagementPanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        JTabbedPane tabs = AppTheme.makeSectionTabs();
        tabs.addTab(getText("bindings.tab.gameInstallLocations"), gameInstallLocationsPanel);
        tabs.addTab(getText("bindings.tab.playerBackups"), buildPlayerBackupsTab());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildPlayerBackupsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(hudSubtabContentBorder());
        panel.setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        tableModel = new ReadOnlyTableModel(columnNames(), 0);
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> updateSelectionActionsEnabled());
        HudTable.style(table);

        HudSection section = new HudSection(
                getText("bindings.bindingManagement.section.backups"),
                new BorderLayout(),
                HudPanel.Variant.FLAT,
                6);
        section.body().add(HudTable.dataPlaneScrollPane(table), BorderLayout.CENTER);

        panel.add(buildSettingsSection(), BorderLayout.NORTH);
        panel.add(section, BorderLayout.CENTER);
        panel.add(buildFooter(), BorderLayout.SOUTH);
        return panel;
    }

    /** Backups go to, Keep daily backups for, Keep before-changes backups (file-manager.md, <em>Settings</em>). */
    private JPanel buildSettingsSection() {
        destinationField = new HudMetadataField();
        JButton browseButton = makeButtonSubtle(getText("bindings.bindingManagement.settings.browse"));
        browseButton.addActionListener(e -> chooseDestination());
        JButton useDefaultButton = makeButtonSubtle(getText("bindings.bindingManagement.settings.useDefault"));
        useDefaultButton.addActionListener(e -> saveDestination(null));

        dailyRetentionCombo = new HudComboBox<>(
                BindForgeSettingsManager.DAILY_BACKUP_RETENTION_CHOICES.toArray(Integer[]::new),
                days -> getText("bindings.bindingManagement.settings.days", days));
        dailyRetentionCombo.addActionListener(e -> {
            Integer days = (Integer) dailyRetentionCombo.getSelectedItem();
            if (!loadingSettings && days != null) saveSetting(() -> settings.setDailyBackupRetentionDays(days));
        });

        beforeChangesStepper = new HudStepper(BindForgeSettingsManager.MIN_BEFORE_CHANGES_BACKUPS_KEPT,
                BindForgeSettingsManager.MAX_BEFORE_CHANGES_BACKUPS_KEPT, BEFORE_CHANGES_STEP, 100);
        beforeChangesStepper.addChangeListener(e -> {
            int kept = beforeChangesStepper.getValue();
            if (!loadingSettings) saveSetting(() -> settings.setBeforeChangesBackupsKept(kept));
        });

        HudSection section = new HudSection(
                getText("bindings.bindingManagement.section.settings"),
                new GridBagLayout(),
                HudPanel.Variant.FLAT,
                6);
        JPanel body = section.body();
        addRow(body, 0, "bindings.bindingManagement.settings.destination", destinationField, browseButton,
                useDefaultButton);
        addRow(body, 1, "bindings.bindingManagement.settings.dailyRetention", dailyRetentionCombo);
        addRow(body, 2, "bindings.bindingManagement.settings.beforeChangesKept", beforeChangesStepper);
        return section;
    }

    /** One settings row: the label, the first control stretched or not, then any buttons. */
    private static void addRow(JPanel body, int row, String labelKey, JComponent control, JComponent... buttons) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(2, 0, 2, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        body.add(hudReadoutLabel(getText(labelKey)), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = buttons.length > 0 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        body.add(control, c);

        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        for (int i = 0; i < buttons.length; i++) {
            c.gridx = 2 + i;
            body.add(buttons[i], c);
        }
    }

    private JPanel buildFooter() {
        backupNowButton = makeButton(getText("bindings.bindingManagement.button.backupNow"));
        backupNowButton.addActionListener(e -> performBackup());

        deleteButton = makeButtonSubtle(getText("bindings.bindingManagement.button.delete"));
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> performDelete());

        restoreToEditingSlotButton = makeButtonSubtle(getText("bindings.bindingManagement.button.restoreToEditingSlot"));
        restoreToEditingSlotButton.setEnabled(false);
        restoreToEditingSlotButton.addActionListener(e -> performRestoreToEditingSlot());

        restoreToLiveButton = makeButton(getText("bindings.bindingManagement.button.restoreToLive"));
        restoreToLiveButton.setEnabled(false);
        restoreToLiveButton.addActionListener(e -> performRestoreToLive());

        return HudFooter.build(false, null, null,
                List.of(backupNowButton, deleteButton, restoreToEditingSlotButton, restoreToLiveButton));
    }

    public void initData() {
        loadSettings();
        refreshBackups();
        gameInstallLocationsPanel.initData();
    }

    /** Takes a user backup off the EDT - it runs installation detection and copies every file. */
    private void performBackup() {
        if (operationInProgress) {
            return;
        }
        setOperationBusy(true);
        new Thread(() -> {
            Backup taken = null;
            IOException failure = null;
            boolean nothingYet = false;
            try {
                taken = backups.backUpEverything(Backups.Kind.USER);
            } catch (Backups.NothingToBackUpException e) {
                log.info("Backup Now found nothing to back up: {}", e.getMessage());
                nothingYet = true;
            } catch (IOException e) {
                log.warn("Backup Now failed", e);
                failure = e;
            }
            Backup result = taken;
            IOException resultFailure = failure;
            boolean resultNothingYet = nothingYet;
            SwingUtilities.invokeLater(() -> {
                setOperationBusy(false);
                if (resultNothingYet) {
                    // WHY: not a fault - a new player has no files yet, and is told how the game makes them. The same
                    // string the startup setup check speaks, so the two can never say different things (Alan,
                    // 2026-10-25).
                    HudConfirmDialog.info(this, getText("bindings.bindingManagement.backup.nothingYet.title"),
                            getText(Backups.NothingToBackUpException.ADVICE_KEY), getText("button.ok"));
                } else if (resultFailure != null) {
                    showError("bindings.bindingManagement.backup.dialogTitle", "bindings.bindingManagement.backup.error",
                            resultFailure.getMessage());
                } else if (!result.full()) {
                    // WHY: Partial is named, with what could not be read, so the user knows the backup is incomplete.
                    HudConfirmDialog.info(this, getText("bindings.bindingManagement.backup.partial.title"),
                            getText("bindings.bindingManagement.backup.partial.text", String.join("\n", result.notRead())),
                            getText("button.ok"));
                }
                refreshBackups();
            });
        }, "PlayerBackup-Thread").start();
    }

    /** Deletes the selected user backup, after asking (file-manager.md, <em>Retention</em>). */
    private void performDelete() {
        Backup backup = selectedBackup();
        if (backup == null || backup.kind() != Backups.Kind.USER || operationInProgress) {
            return;
        }
        if (!HudConfirmDialog.confirm(this,
                getText("bindings.bindingManagement.delete.confirm.title"),
                getText("bindings.bindingManagement.delete.confirm.text", backup.taken().format(CREATED)),
                getText("bindings.bindingManagement.button.delete"),
                getText("button.cancel"))) {
            return;
        }
        setOperationBusy(true);
        new Thread(() -> {
            IOException failure = null;
            try {
                backups.delete(backup);
            } catch (IOException e) {
                log.warn("Could not delete backup {}", backup.zip(), e);
                failure = e;
            }
            IOException result = failure;
            SwingUtilities.invokeLater(() -> {
                setOperationBusy(false);
                if (result != null) {
                    showError("bindings.bindingManagement.delete.error.dialogTitle",
                            "bindings.bindingManagement.delete.error", result.getMessage());
                }
                refreshBackups();
            });
        }, "PlayerBackupDelete-Thread").start();
    }

    private void performRestoreToEditingSlot() {
        Backup backup = selectedBackup();
        if (backup == null || operationInProgress) {
            return;
        }
        File gameFile = resolveActiveBindsFileOrShowError();
        if (gameFile == null) {
            return;
        }
        String presetFileName = gameFile.getName();
        if (!confirmRestore("bindings.bindingManagement.restore.editingSlot.confirm.text", presetFileName)) {
            return;
        }

        setOperationBusy(true);
        new Thread(() -> {
            IOException failure = null;
            try {
                restoreService.restoreToWorkingCopy(backup, presetFileName);
            } catch (IOException e) {
                failure = e;
            }
            IOException result = failure;
            SwingUtilities.invokeLater(() -> {
                setOperationBusy(false);
                if (result != null) {
                    showRestoreError(result.getMessage());
                }
                // No success dialog needed: Binding Profile reflects the new draft reactively
                // via the BindingsUpdatedEvent that restoreToWorkingCopy() publishes.
            });
        }, "PlayerBackupRestoreSlot-Thread").start();
    }

    private void performRestoreToLive() {
        Backup backup = selectedBackup();
        if (backup == null || operationInProgress) {
            return;
        }
        File gameFile = resolveActiveBindsFileOrShowError();
        if (gameFile == null) {
            return;
        }
        String presetFileName = gameFile.getName();
        if (!confirmRestore("bindings.bindingManagement.restore.live.confirm.text", presetFileName)) {
            return;
        }

        setOperationBusy(true);
        new Thread(() -> {
            Path appliedBackup = null;
            IOException ioFailure = null;
            BindingsApplyException applyFailure = null;
            try {
                appliedBackup = restoreService.restoreToLive(backup, presetFileName, gameFile.toPath());
            } catch (IOException e) {
                ioFailure = e;
            } catch (BindingsApplyException e) {
                applyFailure = e;
            }
            Path resultBackup = appliedBackup;
            IOException resultIoFailure = ioFailure;
            BindingsApplyException resultApplyFailure = applyFailure;
            SwingUtilities.invokeLater(() -> {
                setOperationBusy(false);
                if (resultIoFailure != null) {
                    showRestoreError(resultIoFailure.getMessage());
                } else if (resultApplyFailure != null) {
                    applyResultPresenter.showError(resultApplyFailure);
                } else if (resultBackup != null) {
                    table.clearSelection();
                    applyResultPresenter.showSuccess(resultBackup);
                }
            });
        }, "PlayerBackupRestoreLive-Thread").start();
    }

    private boolean confirmRestore(String messageKey, String presetFileName) {
        return HudConfirmDialog.confirm(this,
                getText("bindings.bindingManagement.restore.confirm.title"),
                getText(messageKey, presetFileName),
                getText("bindings.bindingManagement.restore.confirm.title"),
                getText("button.cancel"));
    }

    /**
     * Resolves the active bindings file the same way {@code BindingProfilePanel} does, for
     * the preset name restore writes into.
     */
    private File resolveActiveBindsFileOrShowError() {
        try {
            return BindingsMonitor.getInstance().resolveActiveBindsFile();
        } catch (IOException e) {
            log.warn("Could not resolve the active bindings preset for restore: {}", e.getMessage());
            showRestoreError(e.getMessage());
            return null;
        }
    }

    // ---- settings

    /** Reads the settings and resolves the backup folder off the EDT, then fills the controls without saving. */
    private void loadSettings() {
        new Thread(() -> {
            int days;
            int kept;
            String folder;
            try {
                days = settings.getDailyBackupRetentionDays();
                kept = settings.getBeforeChangesBackupsKept();
                folder = resolvedDestination();
            } catch (RuntimeException e) {
                log.warn("Could not read the backup settings", e);
                return;
            }
            SwingUtilities.invokeLater(() -> {
                loadingSettings = true;
                try {
                    dailyRetentionCombo.setSelectedItem(days);
                    beforeChangesStepper.setValue(kept);
                    destinationField.setText(folder);
                } finally {
                    loadingSettings = false;
                }
            });
        }, "PlayerBackupSettings-Thread").start();
    }

    /**
     * The folder backups actually go to - the chosen one, or the default when it is unset or unusable - so the field
     * never shows a folder a backup would not be written to.
     */
    private static String resolvedDestination() {
        try {
            return BackupDestination.stored().resolve().toString();
        } catch (IOException e) {
            log.warn("Could not resolve the backup folder", e);
            return "";
        }
    }

    private void chooseDestination() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getText("bindings.bindingManagement.settings.chooseFolder.title"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String current = destinationField.getText();
        if (current != null && !current.isBlank()) chooser.setCurrentDirectory(new File(current));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        saveDestination(chooser.getSelectedFile().getAbsolutePath());
    }

    /** Saves the folder ({@code null} for the default), then shows where backups now go and that folder's list. */
    private void saveDestination(String folder) {
        new Thread(() -> {
            String failure = null;
            String shown = null;
            try {
                settings.setBackupDestination(folder);
                shown = resolvedDestination();
            } catch (RuntimeException e) {
                log.warn("Could not save the backup folder {}", folder, e);
                failure = String.valueOf(e.getMessage());
            }
            String resultFailure = failure;
            String resultShown = shown;
            SwingUtilities.invokeLater(() -> {
                if (resultFailure != null) {
                    showError("bindings.bindingManagement.settings.error.title",
                            "bindings.bindingManagement.settings.error", resultFailure);
                    return;
                }
                destinationField.setText(resultShown);
                refreshBackups();
            });
        }, "PlayerBackupSettings-Thread").start();
    }

    /** Saves one retention setting off the EDT. Retention applies at the next Elite-Intel start. */
    private void saveSetting(Runnable save) {
        new Thread(() -> {
            try {
                save.run();
            } catch (RuntimeException e) {
                log.warn("Could not save a backup setting", e);
                String message = String.valueOf(e.getMessage());
                SwingUtilities.invokeLater(() -> showError("bindings.bindingManagement.settings.error.title",
                        "bindings.bindingManagement.settings.error", message));
            }
        }, "PlayerBackupSettings-Thread").start();
    }

    // ---- the list

    private Backup selectedBackup() {
        int row = table.getSelectedRow();
        return row >= 0 && row < currentBackups.size() ? currentBackups.get(row) : null;
    }

    private void setOperationBusy(boolean busy) {
        operationInProgress = busy;
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        backupNowButton.setEnabled(!busy);
        updateSelectionActionsEnabled();
    }

    private void updateSelectionActionsEnabled() {
        Backup selected = operationInProgress ? null : selectedBackup();
        restoreToEditingSlotButton.setEnabled(selected != null);
        restoreToLiveButton.setEnabled(selected != null);
        deleteButton.setEnabled(selected != null && selected.kind() == Backups.Kind.USER);
    }

    private void showRestoreError(String message) {
        showError("bindings.bindingManagement.restore.error.dialogTitle", "bindings.bindingManagement.restore.error",
                message);
    }

    private void showError(String titleKey, String messageKey, String message) {
        HudConfirmDialog.info(this, getText(titleKey), getText(messageKey, String.valueOf(message)),
                getText("button.ok"));
    }

    /**
     * Lists the backup folder off the EDT - it reads the settings and the disk - then fills the table. Called on the
     * EDT.
     * <p>
     * Only the newest request's listing is shown (review, 2026-10-24): two refreshes can finish out of order, and the
     * older listing, without the backup just taken, must not be the one left on screen.
     */
    private void refreshBackups() {
        int request = ++latestRefresh;
        new Thread(() -> {
            List<Backup> listed;
            try {
                listed = backups.list();
            } catch (IOException | RuntimeException e) {
                // Leave the table empty; the user can still retry via Backup Now.
                log.warn("Could not list backups", e);
                listed = List.of();
            }
            List<Backup> result = listed;
            SwingUtilities.invokeLater(() -> {
                if (request == latestRefresh) showBackups(result);
            });
        }, "PlayerBackupList-Thread").start();
    }

    private void showBackups(List<Backup> listed) {
        currentBackups = listed;
        tableModel.setRowCount(0);
        for (Backup backup : listed) {
            tableModel.addRow(new Object[]{
                    backup.taken().format(CREATED),
                    getText(takenKey(backup)),
                    getText(backup.full() ? "bindings.bindingManagement.status.full"
                            : "bindings.bindingManagement.status.partial")
            });
        }
        updateSelectionActionsEnabled();
    }

    /** The <em>Taken</em> column: Daily, before APPLY, before RESTORE, before the check, User, First setup. */
    static String takenKey(Backup backup) {
        return switch (backup.kind()) {
            case DAILY -> "bindings.bindingManagement.taken.daily";
            case USER -> "bindings.bindingManagement.taken.user";
            case FIRST_SETUP -> "bindings.bindingManagement.taken.firstSetup";
            case BEFORE_CHANGES -> backup.before() == null ? "bindings.bindingManagement.taken.beforeChanges"
                    : switch (backup.before()) {
                        case APPLY -> "bindings.bindingManagement.taken.beforeApply";
                        case RESTORE -> "bindings.bindingManagement.taken.beforeRestore";
                        case CHECK -> "bindings.bindingManagement.taken.beforeCheck";
                    };
        };
    }

    private String[] columnNames() {
        return new String[]{
                getText("bindings.bindingManagement.column.created"),
                getText("bindings.bindingManagement.column.taken"),
                getText("bindings.bindingManagement.column.status")
        };
    }

    private static final class ReadOnlyTableModel extends DefaultTableModel {
        private ReadOnlyTableModel(Object[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
