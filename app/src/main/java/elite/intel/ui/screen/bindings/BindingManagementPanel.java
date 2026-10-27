package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.storage.Backup;
import elite.intel.bindforge.storage.BackupDestination;
import elite.intel.bindforge.storage.Backups;
import elite.intel.db.managers.BindForgeSettingsManager;
import elite.intel.ui.dialog.HudConfirmDialog;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND;

/**
 * The "Binding Management" sub-tab of BIND FORGE, which holds two tabs of its own.
 * <p>
 * <strong>Game Install Locations</strong> is where BindForge looks for the files it manages.
 * <strong>Player Backups</strong> lists every backup ({@link Backups}) newest first, with a <em>Taken</em> column
 * naming its kind, and holds the backup settings: where backups go, and the two retention rules (file-manager.md,
 * <em>Settings</em>; Storage S1b, 2026-10-24).
 * <p>
 * <strong>Delete is for user backups only</strong> (Alan, 2026-10-24): the user made them, so they decide when they
 * go. Retention looks after daily and before-changes backups, and first setup's is kept forever.
 * <p>
 * <strong>RESTORE</strong> puts the whole selected backup back, and <strong>Browse for Backup File</strong> one that is
 * not in the list ({@link RestoreFlow}; storage.md rule 10, Storage S3, 2026-10-25). They replaced Restore to Editing
 * Slot and Restore to Live, which restored one binds file.
 */
public class BindingManagementPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(BindingManagementPanel.class);
    private static final DateTimeFormatter CREATED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int BEFORE_CHANGES_STEP = 10;

    /**
     * Every settings read and save, one at a time and in the order asked (review, 2026-10-24): a thread each let three
     * quick stepper clicks land out of order, and the stored value end at one the screen did not show. Daemon, so it
     * never keeps Elite-Intel from closing.
     */
    private final ExecutorService settingsWork = Executors.newSingleThreadExecutor(work -> {
        Thread thread = new Thread(work, "PlayerBackupSettings-Thread");
        thread.setDaemon(true);
        return thread;
    });

    private final Backups backups = Backups.stored();
    private final BindForgeSettingsManager settings = BindForgeSettingsManager.getInstance();
    private final RestoreFlow restoreFlow = new RestoreFlow(this);

    private final GameInstallLocationsPanel gameInstallLocationsPanel = new GameInstallLocationsPanel();

    private DefaultTableModel tableModel;
    private JTable table;
    private List<Backup> currentBackups = List.of();

    private JButton backupNowButton;
    private JButton deleteButton;
    private JButton browseButton;
    private JButton restoreButton;
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

        browseButton = makeButtonSubtle(getText("bindings.restore.browse"));
        browseButton.addActionListener(e -> performBrowse());

        restoreButton = makeButton(getText("bindings.restore.button"));
        restoreButton.setEnabled(false);
        restoreButton.addActionListener(e -> performRestore());

        return HudFooter.build(false, null, null,
                List.of(backupNowButton, deleteButton, browseButton, restoreButton));
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
                    // 2026-10-24).
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

    /** RESTORE: the whole selected backup goes back (storage.md rule 10). */
    private void performRestore() {
        Backup backup = selectedBackup();
        if (backup == null || operationInProgress) {
            return;
        }
        restore(backup.zip(), false);
    }

    /**
     * Browse for Backup File: a backup that is not in the list - off an old PC, out of a cloud folder - restored the
     * same way, its confirmation saying to restore only the user's own (file-manager.md).
     */
    private void performBrowse() {
        if (operationInProgress) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getText("bindings.restore.browse.title"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(new FileNameExtensionFilter(getText("bindings.restore.browse.filter"), "zip"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        restore(chooser.getSelectedFile().toPath(), true);
    }

    private void restore(Path zip, boolean browsed) {
        setOperationBusy(true);
        restoreFlow.restore(zip, browsed, () -> {
            setOperationBusy(false);
            // WHY: RESTORE took a before-RESTORE backup, which belongs in the list.
            refreshBackups();
        });
    }

    // ---- settings

    /** Reads the settings and resolves the backup folder off the EDT, then fills the controls without saving. */
    private void loadSettings() {
        settingsWork.execute(() -> {
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
        });
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
        saveDestination(chooser.getSelectedFile().toPath());
    }

    /**
     * Saves the folder ({@code null} for the default), then shows where backups now go and that folder's list.
     * <p>
     * <strong>A folder that would not be used is not saved, and the user is told why</strong> (review, 2026-10-24;
     * principle 9): stored, it would fall back to the default without a word, and Browse... would look broken.
     */
    private void saveDestination(Path folder) {
        settingsWork.execute(() -> {
            BackupDestination.Refusal refusal = folder == null ? null : BackupDestination.stored().check(folder);
            if (refusal != null) {
                SwingUtilities.invokeLater(() -> HudConfirmDialog.info(this,
                        getText("bindings.bindingManagement.settings.refused.title"),
                        getText(refusalKey(refusal), folder.toString()),
                        getText("button.ok")));
                return;
            }
            String failure = null;
            String shown = null;
            try {
                settings.setBackupDestination(folder == null ? null : folder.toString());
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
        });
    }

    static String refusalKey(BackupDestination.Refusal refusal) {
        return switch (refusal) {
            case INSIDE_GAME_FOLDER -> "bindings.bindingManagement.settings.refused.insideGameFolder";
            case CANNOT_CREATE -> "bindings.bindingManagement.settings.refused.cannotCreate";
            case CANNOT_WRITE -> "bindings.bindingManagement.settings.refused.cannotWrite";
        };
    }

    /** Saves one retention setting off the EDT, after any save asked for before it. Applies at the next start. */
    private void saveSetting(Runnable save) {
        settingsWork.execute(() -> {
            try {
                save.run();
            } catch (RuntimeException e) {
                log.warn("Could not save a backup setting", e);
                String message = String.valueOf(e.getMessage());
                SwingUtilities.invokeLater(() -> showError("bindings.bindingManagement.settings.error.title",
                        "bindings.bindingManagement.settings.error", message));
            }
        });
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
        restoreButton.setEnabled(selected != null);
        browseButton.setEnabled(!operationInProgress);
        deleteButton.setEnabled(selected != null && selected.kind() == Backups.Kind.USER);
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
