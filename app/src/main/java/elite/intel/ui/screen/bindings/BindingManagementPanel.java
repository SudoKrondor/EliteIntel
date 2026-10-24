package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.io.BindingsApplyException;
import elite.intel.bindforge.io.BindingsMonitor;
import elite.intel.bindforge.io.PlayerBackupService;
import elite.intel.bindforge.storage.Backup;
import elite.intel.bindforge.storage.Backups;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.support.BindingApplyResultPresenter;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudFooter;
import elite.intel.ui.widget.HudPanel;
import elite.intel.ui.widget.HudSection;
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
 * <strong>Player Backups</strong> lists the backups in the backup folder ({@link Backups}, Storage S1), newest first,
 * with Backup Now and the two restore actions on the selected backup.
 * <p>
 * <strong>No Delete</strong> (Alan, 2026-10-24): retention at Elite-Intel start keeps the folder from growing, and a
 * button could delete first setup's backup, the one kept forever.
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

    private final Backups backups = Backups.stored();
    private final PlayerBackupService restoreService = PlayerBackupService.getInstance();
    private final BindingApplyResultPresenter applyResultPresenter = new BindingApplyResultPresenter(this);

    private final GameInstallLocationsPanel gameInstallLocationsPanel = new GameInstallLocationsPanel();

    private DefaultTableModel tableModel;
    private JTable table;
    private List<Backup> currentBackups = List.of();

    private JButton backupNowButton;
    private JButton restoreToEditingSlotButton;
    private JButton restoreToLiveButton;
    private boolean operationInProgress;
    /** The newest list refresh asked for. Read and written on the EDT only. */
    private int latestRefresh;

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

        panel.add(section, BorderLayout.CENTER);
        panel.add(buildFooter(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildFooter() {
        backupNowButton = makeButton(getText("bindings.bindingManagement.button.backupNow"));
        backupNowButton.addActionListener(e -> performBackup());

        restoreToEditingSlotButton = makeButtonSubtle(getText("bindings.bindingManagement.button.restoreToEditingSlot"));
        restoreToEditingSlotButton.setEnabled(false);
        restoreToEditingSlotButton.addActionListener(e -> performRestoreToEditingSlot());

        restoreToLiveButton = makeButton(getText("bindings.bindingManagement.button.restoreToLive"));
        restoreToLiveButton.setEnabled(false);
        restoreToLiveButton.addActionListener(e -> performRestoreToLive());

        return HudFooter.build(false, null, null,
                List.of(backupNowButton, restoreToEditingSlotButton, restoreToLiveButton));
    }

    public void initData() {
        refreshBackups();
        gameInstallLocationsPanel.initData();
    }

    /** Backs up everything off the EDT - it runs installation detection and copies every file. */
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
                taken = backups.backUpEverything();
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
                    // WHY: not a fault - a new player has no files yet, and is told how the game makes them (Alan,
                    // 2026-10-24).
                    HudConfirmDialog.info(this, getText("bindings.bindingManagement.backup.nothingYet.title"),
                            getText("bindings.bindingManagement.backup.nothingYet"), getText("button.ok"));
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
                restoreService.restoreToWorkingCopy(backup.folder(), presetFileName);
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
                appliedBackup = restoreService.restoreToLive(backup.folder(), presetFileName, gameFile.toPath());
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
        boolean hasSelection = !operationInProgress && selectedBackup() != null;
        restoreToEditingSlotButton.setEnabled(hasSelection);
        restoreToLiveButton.setEnabled(hasSelection);
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
                    getText(backup.full() ? "bindings.bindingManagement.status.full"
                            : "bindings.bindingManagement.status.partial"),
                    backup.firstSetup() ? getText("bindings.bindingManagement.kept.forever") : ""
            });
        }
        updateSelectionActionsEnabled();
    }

    private String[] columnNames() {
        return new String[]{
                getText("bindings.bindingManagement.column.created"),
                getText("bindings.bindingManagement.column.status"),
                getText("bindings.bindingManagement.column.kept")
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
