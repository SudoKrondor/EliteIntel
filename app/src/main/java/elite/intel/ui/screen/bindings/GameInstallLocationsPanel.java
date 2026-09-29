package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.bindforge.install.WindowsGameInstallationProvider;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.session.PlayerSession;
import elite.intel.ui.dialog.HudConfirmDialog;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

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
 * which is why the list is editable rather than merely displayed.
 */
public class GameInstallLocationsPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(GameInstallLocationsPanel.class);

    private final InstallationRegistry registry;

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField configFolderField;
    private List<InstallationRow> currentRows = List.of();

    private JButton relocateButton;
    private JButton removeButton;

    public GameInstallLocationsPanel() {
        this(new InstallationRegistry(
                new WindowsGameInstallationProvider(PlayerSession.getInstance().getBindingsDir())));
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
        JButton rescanButton = makeButton(getText("bindings.installLocations.button.rescan"));
        rescanButton.addActionListener(e -> performRescan());

        JButton addButton = makeButton(getText("bindings.installLocations.button.add"));
        addButton.addActionListener(e -> performAdd());

        relocateButton = makeButtonSubtle(getText("bindings.installLocations.button.relocate"));
        relocateButton.setEnabled(false);
        relocateButton.addActionListener(e -> performRelocate());

        removeButton = makeButtonSubtle(getText("bindings.installLocations.button.remove"));
        removeButton.setEnabled(false);
        removeButton.addActionListener(e -> performRemove());

        return HudFooter.build(false, null, null,
                List.of(rescanButton, addButton, relocateButton, removeButton));
    }

    public void initData() {
        refreshConfigFolder();
        showRows(registry.current());
    }

    private void performRescan() {
        showRows(registry.rescan());
        refreshConfigFolder();
    }

    private void performAdd() {
        Path chosen = chooseFolder(getText("bindings.installLocations.add.chooserTitle"), null);
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
        showRows(registry.current());
        selectRow(added.id());
    }

    private void performRelocate() {
        InstallationRow row = selectedRow();
        if (row == null) return;
        Path chosen = chooseFolder(getText("bindings.installLocations.relocate.chooserTitle"), row.rootPath());
        if (chosen == null) return;
        try {
            registry.relocate(row.id(), chosen);
        } catch (IllegalArgumentException e) {
            HudConfirmDialog.info(this,
                    getText("bindings.installLocations.add.rejected.title"),
                    getText("bindings.installLocations.add.rejected.text", chosen.toString()),
                    getText("button.ok"));
            return;
        }
        showRows(registry.current());
    }

    private void performRemove() {
        InstallationRow row = selectedRow();
        if (row == null) return;
        // WHY: confirmed because it discards what was held against this installation, and there is no undo
        // beyond adding the folder again and letting import run.
        boolean confirmed = HudConfirmDialog.confirm(this,
                getText("bindings.installLocations.remove.confirm.title"),
                getText("bindings.installLocations.remove.confirm.text", row.rootPath()),
                getText("bindings.installLocations.button.remove"),
                getText("button.cancel"));
        if (!confirmed) return;
        registry.remove(row.id());
        showRows(registry.current());
    }

    private void refreshConfigFolder() {
        Path bindingsDir = PlayerSession.getInstance().getBindingsDir();
        boolean found = bindingsDir != null && Files.isDirectory(bindingsDir);
        configFolderField.setText(bindingsDir == null
                ? getText("bindings.installLocations.configFolder.unset")
                : bindingsDir + "   -   " + getText(found
                        ? "bindings.installLocations.state.found"
                        : "bindings.installLocations.state.missing"));
    }

    private void showRows(List<InstallationRow> rows) {
        currentRows = rows;
        tableModel.setRowCount(0);
        for (InstallationRow row : rows) {
            tableModel.addRow(new Object[]{
                    row.storefront(),
                    row.rootPath(),
                    getText(row.addedByHand()
                            ? "bindings.installLocations.source.addedByHand"
                            : "bindings.installLocations.source.detected"),
                    describeDeviceFiles(row),
                    getText(row.missing()
                            ? "bindings.installLocations.state.missing"
                            : "bindings.installLocations.state.found")
            });
        }
        updateSelectionActionsEnabled();
    }

    /**
     * What was actually found in this installation's {@code ControlSchemes} folder.
     * <p>
     * A missing {@code DeviceButtonMaps} folder is the normal case rather than a fault - Frontier ships
     * button maps for only a couple of devices - so it is reported as a count, not as an error.
     */
    private String describeDeviceFiles(InstallationRow row) {
        Path controlSchemes = GameInstallation.controlSchemesUnder(Path.of(row.rootPath()));
        if (!Files.isDirectory(controlSchemes)) {
            return getText("bindings.installLocations.deviceFiles.none");
        }
        boolean hasMappings = Files.isRegularFile(controlSchemes.resolve("DeviceMappings.xml"));
        return getText("bindings.installLocations.deviceFiles.summary",
                hasMappings ? "DeviceMappings.xml" : getText("bindings.installLocations.deviceFiles.noMappings"),
                String.valueOf(countButtonMaps(controlSchemes.resolve("DeviceButtonMaps"))));
    }

    private long countButtonMaps(Path deviceButtonMaps) {
        if (!Files.isDirectory(deviceButtonMaps)) return 0;
        try (Stream<Path> files = Files.list(deviceButtonMaps)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".buttonMap")).count();
        } catch (IOException e) {
            log.warn("Could not list {}: {}", deviceButtonMaps, e.getMessage());
            return 0;
        }
    }

    private Path chooseFolder(String title, String startAt) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (startAt != null) chooser.setCurrentDirectory(new File(startAt));
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
        boolean hasSelection = selectedRow() != null;
        relocateButton.setEnabled(hasSelection);
        removeButton.setEnabled(hasSelection);
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
