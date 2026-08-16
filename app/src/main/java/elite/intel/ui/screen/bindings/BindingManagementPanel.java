package elite.intel.ui.screen.bindings;

import elite.intel.ai.hands.BindingsApplyException;
import elite.intel.ai.hands.BindingsMonitor;
import elite.intel.ai.hands.PlayerBackupService;
import elite.intel.ui.support.BindingApplyResultPresenter;
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
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND;

/**
 * The "Binding Management" sub-tab of BIND FORGE: a manual "Backup Now" action, a flat list of
 * existing {@code playerbackups} snapshots, and two restore actions plus a delete action on the
 * selected backup.
 * <p>
 * Both restore actions share the same first step (loading the backup's file for the active
 * preset into the working copy as the new draft): "Restore to Editing Slot" stops there, the
 * Binding Profile tab picks up the new draft reactively via the existing
 * {@code BindingsUpdatedEvent}. "Restore to Live" continues into the existing safe-apply
 * pipeline ({@code BindingsApplyService.apply()}), so it gets that pipeline's own
 * conflict-check and pre-write backup rather than a separate, less-safe direct write.
 */
public class BindingManagementPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(BindingManagementPanel.class);

    private final PlayerBackupService backupService = PlayerBackupService.getInstance();
    private final BindingApplyResultPresenter applyResultPresenter = new BindingApplyResultPresenter(this);

    private DefaultTableModel tableModel;
    private JTable table;
    private List<PlayerBackupService.PlayerBackup> currentBackups = List.of();

    private JButton backupNowButton;
    private JButton restoreToEditingSlotButton;
    private JButton restoreToLiveButton;
    private JButton deleteBackupButton;
    private boolean operationInProgress;

    public BindingManagementPanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBorder(hudSubtabContentBorder());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

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

        add(section, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
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

        deleteBackupButton = makeButtonSubtle(getText("bindings.bindingManagement.button.deleteBackup"));
        deleteBackupButton.setEnabled(false);
        deleteBackupButton.addActionListener(e -> performDeleteBackup());

        return HudFooter.build(false, null, null,
                List.of(backupNowButton, restoreToEditingSlotButton, restoreToLiveButton, deleteBackupButton));
    }

    public void initData() {
        refreshBackups();
    }

    /**
     * Runs the backup sweep off the EDT, same as {@code BindingProfilePanel}'s
     * {@code applyPlanInBackground()} - a multi-file copy would otherwise freeze the UI.
     */
    private void performBackup() {
        if (operationInProgress) {
            return;
        }
        setOperationBusy(true);
        new Thread(() -> {
            IOException failure = null;
            try {
                backupService.createBackup();
            } catch (IOException e) {
                failure = e;
            }
            IOException result = failure;
            SwingUtilities.invokeLater(() -> {
                setOperationBusy(false);
                if (result != null) {
                    showBackupError(result);
                } else {
                    refreshBackups();
                }
            });
        }, "PlayerBackup-Thread").start();
    }

    private void performRestoreToEditingSlot() {
        PlayerBackupService.PlayerBackup backup = selectedBackup();
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
                backupService.restoreToWorkingCopy(backup.folder(), presetFileName);
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
        PlayerBackupService.PlayerBackup backup = selectedBackup();
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
                appliedBackup = backupService.restoreToLive(backup.folder(), presetFileName, gameFile.toPath());
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

    /**
     * Deletes the selected backup folder so the list does not grow indefinitely. Guarded by a
     * confirmation dialog because the snapshot it removes is not recoverable from anywhere else.
     */
    private void performDeleteBackup() {
        PlayerBackupService.PlayerBackup backup = selectedBackup();
        if (backup == null || operationInProgress) {
            return;
        }
        if (!confirm("bindings.bindingManagement.delete.confirm.text",
                "bindings.bindingManagement.delete.confirm.title",
                backup.timestamp())) {
            return;
        }

        setOperationBusy(true);
        new Thread(() -> {
            IOException failure = null;
            try {
                backupService.deleteBackup(backup.folder());
            } catch (IOException e) {
                failure = e;
            }
            IOException result = failure;
            SwingUtilities.invokeLater(() -> {
                setOperationBusy(false);
                if (result != null) {
                    showDeleteError(result.getMessage());
                }
                // Refresh either way: on failure the list re-reads what is actually on disk.
                refreshBackups();
            });
        }, "PlayerBackupDelete-Thread").start();
    }

    private boolean confirmRestore(String messageKey, String presetFileName) {
        return confirm(messageKey, "bindings.bindingManagement.restore.confirm.title", presetFileName);
    }

    private boolean confirm(String messageKey, String titleKey, String argument) {
        int choice = JOptionPane.showConfirmDialog(
                this,
                getText(messageKey, argument),
                getText(titleKey),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
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

    private PlayerBackupService.PlayerBackup selectedBackup() {
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
        deleteBackupButton.setEnabled(hasSelection);
    }

    private void showBackupError(IOException e) {
        JOptionPane.showMessageDialog(
                this,
                getText("bindings.bindingManagement.backup.error", e.getMessage()),
                getText("bindings.bindingManagement.backup.dialogTitle"),
                JOptionPane.ERROR_MESSAGE);
    }

    private void showRestoreError(String message) {
        JOptionPane.showMessageDialog(
                this,
                getText("bindings.bindingManagement.restore.error", message),
                getText("bindings.bindingManagement.restore.error.dialogTitle"),
                JOptionPane.ERROR_MESSAGE);
    }

    private void showDeleteError(String message) {
        JOptionPane.showMessageDialog(
                this,
                getText("bindings.bindingManagement.delete.error", message),
                getText("bindings.bindingManagement.delete.error.dialogTitle"),
                JOptionPane.ERROR_MESSAGE);
    }

    private void refreshBackups() {
        tableModel.setRowCount(0);
        try {
            currentBackups = backupService.listBackups();
        } catch (IOException e) {
            // Leave the table empty; the user can still retry via Backup Now.
            log.warn("Could not list player backups: {}", e.getMessage());
            currentBackups = List.of();
        }
        for (PlayerBackupService.PlayerBackup backup : currentBackups) {
            tableModel.addRow(new Object[]{
                    backup.timestamp(),
                    String.join(", ", backup.fileNames())
            });
        }
        updateSelectionActionsEnabled();
    }

    private String[] columnNames() {
        return new String[]{
                getText("bindings.bindingManagement.column.created"),
                getText("bindings.bindingManagement.column.files")
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
