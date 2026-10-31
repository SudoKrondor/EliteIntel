package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.ButtonMapAudit;
import elite.intel.bindforge.devicefiles.DeviceEntry;
import elite.intel.bindforge.devicefiles.DeviceMappingsParser;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.install.GameInstallationProvider;
import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.bindforge.storage.FirstTimeStartup;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.session.PlayerSession;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.widget.HudFooter;
import elite.intel.ui.widget.HudPanel;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudTable;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND;

/**
 * Where BindForge looks for the files it manages.
 * <p>
 * Two sections rather than one list, because the file domains do not live in comparable places: the
 * {@code .binds} files sit in <strong>one folder shared by every installation</strong>, while
 * {@code DeviceMappings.xml} and the {@code .buttonMap} files are <strong>duplicated per
 * installation</strong>. Presenting them as one flat list of "locations" would imply otherwise, and that is
 * the thing people get wrong.
 * <p>
 * Backup, restore and apply all work from this list, so when detection is wrong every one of them is wrong -
 * which is why the list is editable rather than merely displayed: <strong>Add Missing</strong> and
 * <strong>Remove</strong>, the same list and buttons as first-time startup (storage.md rule 1). A removed installation
 * stays removed. <em>Rescan and Relocate were removed in Storage S2b (Alan, 2026-10-24): detection runs at every
 * start, and moving the game is Remove, then Add Missing.</em>
 * <p>
 * <strong>Run First-Time Startup Again</strong> is for testing V1.2, shown to every user, and is to be hidden or
 * removed before V1.2 ships (Alan, 2026-10-24; build plan, <em>Before V1.2 ships</em>).
 */
public class GameInstallLocationsPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(GameInstallLocationsPanel.class);

    /** Wide enough for a real install path, narrow enough that one cannot crowd out the other columns. */
    private static final int MAX_COLUMN_WIDTH = 420;

    private final InstallationRegistry registry;

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField configFolderField;
    private List<InstallationRow> currentRows = List.of();
    private final AtomicBoolean refreshInProgress = new AtomicBoolean();
    private Long pendingSelection;

    private JButton removeButton;

    public GameInstallLocationsPanel() {
        this(new InstallationRegistry(
                GameInstallationProvider.forThisPlatform(PlayerSession.getInstance().getBindingsDir())));
    }

    GameInstallLocationsPanel(InstallationRegistry registry) {
        this.registry = registry;
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBorder(hudSubtabContentBorder());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        add(buildConfigFolderSection(), BorderLayout.NORTH);
        add(buildInstallationsSection(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    /** One folder, shared by every installation. Read-only: it is Elite-Intel's own bindings setting. */
    private JPanel buildConfigFolderSection() {
        configFolderField = new JTextField();
        configFolderField.setEditable(false);

        HudSection section = new HudSection(
                getText("bindings.installLocations.section.configFolder"),
                new BorderLayout(),
                HudPanel.Variant.FLAT,
                6);
        section.body().add(configFolderField, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildInstallationsSection() {
        tableModel = new ReadOnlyTableModel(columnNames(), 0);
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> updateSelectionActionsEnabled());
        HudTable.style(table);

        HudSection section = new HudSection(
                getText("bindings.installLocations.section.installations"),
                new BorderLayout(),
                HudPanel.Variant.FLAT,
                6);
        section.body().add(HudTable.dataPlaneScrollPane(table), BorderLayout.CENTER);
        return section;
    }

    private JPanel buildFooter() {
        JButton addButton = makeButton(getText("bindings.installLocations.button.addMissing"));
        addButton.addActionListener(e -> performAdd());

        removeButton = makeButtonSubtle(getText("bindings.installLocations.button.remove"));
        removeButton.setEnabled(false);
        removeButton.addActionListener(e -> performRemove());

        // WHY: shown to every user, because Elite-Intel cannot tell a tester from anyone else - to be hidden or removed
        // before V1.2 ships (Alan, 2026-10-24; build plan, storage.md, Before V1.2 ships).
        JButton runAgainButton = makeButtonSubtle(getText("bindings.installLocations.button.runFirstTimeStartupAgain"));
        runAgainButton.addActionListener(e -> performRunAgain());

        return HudFooter.build(false, null, null, List.of(addButton, removeButton, runAgainButton));
    }

    /**
     * Reads the installations and their device files off the EDT, same as
     * {@code BindingManagementPanel.performBackup()}: each row's Device Files summary parses that
     * installation's {@code DeviceMappings.xml} and lists its button maps, and an installation on a
     * disconnected drive takes as long as the filesystem needs to say so.
     * <p>
     * A refresh already in flight is left to finish. This is called again on every ship-profile change, and
     * nothing here depends on the ship.
     */
    public void initData() {
        loadInBackground(registry::currentWithStartupScan);
    }

    private void loadInBackground(Supplier<List<InstallationRow>> source) {
        if (!refreshInProgress.compareAndSet(false, true)) return;
        // WHY: a list that cannot be read shows empty rather than as it was, with the log saying why.
        Function<String, Runnable> empty = reason -> () -> loaded(List.of(), List.of(), null);
        FlowSteps.inBackground("BindForge-Installations", log, "Refreshing the installation list", () -> {
            List<InstallationRow> rows = source.get();
            List<Object[]> cells = rows.stream().map(this::cellsOf).toList();
            Path bindingsDir = PlayerSession.getInstance().getBindingsDir();
            return () -> loaded(rows, cells, bindingsDir);
        }, empty, empty, FlowSteps.NOTHING);
    }

    /** Shows what {@link #loadInBackground} read, then the row asked to be selected. On the EDT. */
    private void loaded(List<InstallationRow> rows, List<Object[]> cells, Path bindingsDir) {
        showConfigFolder(bindingsDir);
        showRows(rows, cells);
        if (pendingSelection != null) {
            selectRow(pendingSelection);
            pendingSelection = null;
        }
        refreshInProgress.set(false);
    }

    private void performAdd() {
        Path chosen = chooseFolder(getText("bindings.installLocations.add.chooserTitle"));
        if (chosen == null) return;
        InstallationRow added;
        try {
            added = registry.addByHand(chosen);
        } catch (IllegalArgumentException e) {
            // WHY: the folder is the user's choice and rejecting it is an ordinary outcome, not a fault. They
            // are told which folder and what was expected, so the next attempt can be a better one.
            HudConfirmDialog.info(this,
                    getText("bindings.installLocations.add.rejected.title"),
                    getText("bindings.installLocations.add.rejected.text", chosen.toString()),
                    getText("button.ok"));
            return;
        }
        // WHY: remembered rather than selected here. The list reloads off the EDT, so the row does not exist
        // in the table yet - the selection happens when it does.
        pendingSelection = added.id();
        initData();
    }

    private void performRemove() {
        InstallationRow row = selectedRow();
        if (row == null) return;
        // WHY: confirmed, and saying what it costs - a removed installation is neither backed up nor written from
        // then on (Alan, 2026-10-25).
        boolean confirmed = HudConfirmDialog.confirm(this,
                getText("bindings.installLocations.remove.confirm.title"),
                getText("bindings.installLocations.remove.confirm.text", row.storefront(), row.rootPath()),
                getText("bindings.installLocations.button.remove"),
                getText("button.cancel"));
        if (!confirmed) return;
        registry.remove(row.id());
        initData();
    }

    /**
     * Clears the "first-time startup complete" flag and nothing else - no backup is deleted, no game file is touched
     * (file-manager.md, <em>Game Install Locations</em>). The next time the BindForge tab is opened, first-time startup
     * runs from the live copies, with its backup first.
     */
    private void performRunAgain() {
        boolean confirmed = HudConfirmDialog.confirm(this,
                getText("bindings.installLocations.runAgain.confirm.title"),
                getText("bindings.installLocations.runAgain.confirm.text"),
                getText("bindings.installLocations.runAgain.confirm.do"),
                getText("button.cancel"));
        if (!confirmed) return;
        try {
            FirstTimeStartup.stored().runAgain();
        } catch (RuntimeException e) {
            // WHY: the flag is a database write, whose failures are unchecked - the user pressed a button and is told.
            log.warn("Could not clear the first-time startup flag", e);
            HudConfirmDialog.info(this, getText("bindings.installLocations.runAgain.confirm.title"),
                    getText("bindings.installLocations.runAgain.failed", StringUtls.messageOf(e)), getText("button.ok"));
        }
    }

    private void showConfigFolder(Path bindingsDir) {
        boolean found = bindingsDir != null && Files.isDirectory(bindingsDir);
        configFolderField.setText(bindingsDir == null
                ? getText("bindings.installLocations.configFolder.unset")
                : bindingsDir + "   -   " + getText(found
                        ? "bindings.installLocations.state.found"
                        : "bindings.installLocations.state.missing"));
    }

    /** Everything one row displays. Built off the EDT, because the Device Files cell reads files. */
    private Object[] cellsOf(InstallationRow row) {
        return new Object[]{
                row.storefront(),
                row.rootPath(),
                getText(row.addedByHand()
                        ? "bindings.installLocations.source.addedByHand"
                        : "bindings.installLocations.source.detected"),
                describeDeviceFiles(row),
                getText(row.missing()
                        ? "bindings.installLocations.state.missing"
                        : "bindings.installLocations.state.found")
        };
    }

    private void showRows(List<InstallationRow> rows, List<Object[]> cells) {
        currentRows = rows;
        tableModel.setRowCount(0);
        cells.forEach(tableModel::addRow);
        // WHY: after the rows, not with the columns. The widths are measured from what is actually in the
        // table, and an install path is only known once it is there.
        HudTable.fitColumnsToContent(table, MAX_COLUMN_WIDTH);
        updateSelectionActionsEnabled();
    }

    /**
     * What was actually found in this installation's {@code ControlSchemes} folder.
     * <p>
     * Reports the device entries, the button maps, and <strong>how many of those button maps resolve to
     * nothing</strong>. An orphan is a file the game never reads: it is named for a device entry that is not
     * there, so its labels are never shown. An entry with no button map is the opposite - the normal case,
     * and not counted as a problem, because Frontier ships button maps for only a couple of devices.
     */
    private String describeDeviceFiles(InstallationRow row) {
        Path controlSchemes = GameInstallation.controlSchemesUnder(Path.of(row.rootPath()));
        if (!Files.isDirectory(controlSchemes)) {
            return getText("bindings.installLocations.deviceFiles.none");
        }
        Path deviceMappings = GameInstallation.deviceMappingsIn(controlSchemes);
        if (!Files.isRegularFile(deviceMappings)) {
            return getText("bindings.installLocations.deviceFiles.noMappings");
        }
        try {
            List<DeviceEntry> entries = DeviceMappingsParser.parse(deviceMappings);
            ButtonMapAudit.Result audit =
                    ButtonMapAudit.audit(entries, GameInstallation.deviceButtonMapsIn(controlSchemes));
            int maps = audit.attached().size() + audit.orphaned().size();
            return audit.isClean()
                    ? getText("bindings.installLocations.deviceFiles.summary",
                            entries.size(), maps)
                    : getText("bindings.installLocations.deviceFiles.summaryWithOrphans",
                            entries.size(), maps, audit.orphaned().size());
        } catch (IOException e) {
            // WHY: a file that is there but cannot be read is a different state from one that is absent, and
            // the user can act on it - so it is reported rather than folded into "nothing found".
            log.warn("Could not read device files in {}: {}", controlSchemes, e.getMessage());
            return getText("bindings.installLocations.deviceFiles.unreadable");
        }
    }

    private Path chooseFolder(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        return chooser.getSelectedFile().toPath();
    }

    /** Puts the cursor on a row the user just acted on, so the result of their action is visible. */
    private void selectRow(long id) {
        for (int index = 0; index < currentRows.size(); index++) {
            if (currentRows.get(index).id() == id) {
                table.setRowSelectionInterval(index, index);
                return;
            }
        }
    }

    private InstallationRow selectedRow() {
        int index = table.getSelectedRow();
        if (index < 0 || index >= currentRows.size()) return null;
        return currentRows.get(index);
    }

    private void updateSelectionActionsEnabled() {
        removeButton.setEnabled(selectedRow() != null);
    }

    private String[] columnNames() {
        return new String[]{
                getText("bindings.installLocations.column.storefront"),
                getText("bindings.installLocations.column.location"),
                getText("bindings.installLocations.column.source"),
                getText("bindings.installLocations.column.deviceFiles"),
                getText("bindings.installLocations.column.status")
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
