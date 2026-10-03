package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devices.DeviceDivergence;
import elite.intel.bindforge.devices.DeviceDivergenceScanner;
import elite.intel.bindforge.devices.DeviceEntry;
import elite.intel.bindforge.devices.DeviceMappingsParser;
import elite.intel.bindforge.devices.FrontierStockDevices;
import elite.intel.bindforge.devices.MyDevice;
import elite.intel.bindforge.devices.MyDeviceList;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.bindforge.install.WindowsGameInstallationProvider;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.managers.BindForgeDeviceMasterManager;
import elite.intel.devices.DeviceService;
import elite.intel.devices.model.Device;
import elite.intel.session.PlayerSession;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudFooter;
import elite.intel.ui.widget.HudPanel;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND;

/**
 * Alias Designer, being built in stages. This is the first: the divergence list.
 * <p>
 * Every installation is supposed to hold the same device files, so anywhere they disagree is something to
 * resolve. <strong>One list, not three</strong> - every kind of disagreement appears together, because
 * splitting them would mean BindForge deciding which kinds deserve attention. What ranks them is the
 * severity, and severity is judged against the bindings: a missing entry only costs the user something when
 * a binding actually names that device.
 * <p>
 * Still to come: the device list, the one-record editor, and the repairs offered per row. Until it can edit,
 * this panel is not yet what "Alias Designer" promises and is deliberately not in the tab bar.
 */
public class AliasDesignerPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(AliasDesignerPanel.class);

    private static final int MAX_COLUMN_WIDTH = 420;

    private final InstallationRegistry registry;

    private DefaultTableModel tableModel;
    private JTable table;
    private DefaultTableModel myDevicesModel;
    private JTable myDevicesTable;
    private List<DeviceDivergence.Finding> currentFindings = List.of();
    private Map<String, String> installLabels = Map.of();
    private final AtomicBoolean refreshInProgress = new AtomicBoolean();
    private boolean deviceServiceRunning;

    public AliasDesignerPanel() {
        this(new InstallationRegistry(
                new WindowsGameInstallationProvider(PlayerSession.getInstance().getBindingsDir())));
    }

    AliasDesignerPanel(InstallationRegistry registry) {
        this.registry = registry;
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBorder(hudSubtabContentBorder());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        tableModel = new ReadOnlyTableModel(columnNames(), 0);
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        HudTable.style(table);
        // WHY: severity is the row's text colour, never a fill or a pill - the HUD canon's rule for state.
        table.setDefaultRenderer(Object.class, new SeverityRenderer());
        // WHY: locked, or the palette pass that walks the tree later calls styleTable and puts the standard
        // renderer back, taking the severity colours with it. HUD_TABLE_STYLE_LOCKED is how a table says it
        // has styled itself deliberately - see ED_HUD_REFERENCE section 8.6.
        table.putClientProperty(AppTheme.HUD_TABLE_STYLE_LOCKED, Boolean.TRUE);

        HudSection section = new HudSection(
                getText("bindings.aliasDesigner.section.divergence"),
                new BorderLayout(),
                HudPanel.Variant.FLAT,
                6);
        section.body().add(HudTable.dataPlaneScrollPane(table), BorderLayout.CENTER);

        myDevicesModel = new ReadOnlyTableModel(myDevicesColumnNames(), 0);
        myDevicesTable = new JTable(myDevicesModel);
        myDevicesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        HudTable.style(myDevicesTable);

        HudSection myDevices = new HudSection(
                getText("bindings.aliasDesigner.section.myDevices"),
                new BorderLayout(),
                HudPanel.Variant.FLAT,
                6);
        myDevices.body().add(HudTable.dataPlaneScrollPane(myDevicesTable), BorderLayout.CENTER);

        JButton rescanButton = makeButton(getText("bindings.aliasDesigner.button.recheck"));
        rescanButton.addActionListener(e -> initData());

        // WHY: stacked rather than tabbed. The two answer different questions - what devices there are, and
        // where the installations disagree about them - and a user resolving a divergence wants the device it
        // names in sight. Equal halves because neither is the subordinate of the other.
        JPanel stacked = new JPanel(new GridLayout(2, 1, 0, 6));
        stacked.setOpaque(false);
        stacked.add(myDevices);
        stacked.add(section);

        add(stacked, BorderLayout.CENTER);
        add(HudFooter.build(false, null, null, List.of(rescanButton)), BorderLayout.SOUTH);
    }

    /**
     * Reads the installations and their device files off the EDT, same as
     * {@code BindingManagementPanel.performBackup()} - parsing every {@code .binds} and every installation's
     * {@code DeviceMappings.xml} would otherwise freeze the window, and an installation on a spun-down or
     * disconnected drive freezes it for as long as the filesystem takes to answer.
     * <p>
     * A refresh already in flight is left to finish rather than a second one being started beside it: this is
     * called again on every ship-profile change, and nothing here depends on the ship.
     */
    public void initData() {
        if (!refreshInProgress.compareAndSet(false, true)) return;
        new Thread(() -> {
            Map<String, String> labels = Map.of();
            List<DeviceDivergence.Finding> findings = List.of();
            List<MyDevice> myDevices = List.of();
            try {
                List<InstallationRow> rows = registry.currentWithStartupScan();
                labels = labelsFor(rows);
                findings = DeviceDivergenceScanner.scan(
                        controlSchemesByInstall(rows), PlayerSession.getInstance().getBindingsDir());
                myDevices = readMyDevices(rows);
            } catch (RuntimeException e) {
                // WHY: broad on purpose. This is a thread boundary, and an exception escaping it would kill
                // the thread silently and leave the table showing whatever it showed before, with no clue why.
                log.warn("Could not refresh the divergence list", e);
            }
            Map<String, String> loadedLabels = labels;
            List<DeviceDivergence.Finding> loadedFindings = findings;
            List<MyDevice> loadedMyDevices = myDevices;
            SwingUtilities.invokeLater(() -> {
                installLabels = loadedLabels;
                showMyDevices(loadedMyDevices);
                showFindings(loadedFindings);
                refreshInProgress.set(false);
            });
        }, "BindForge-Divergence").start();
    }

    /**
     * Gathers the four things the device list is built from: the attached controllers, every installation's
     * device entries, the master's records, and Frontier's shipped list.
     * <p>
     * {@code DeviceService} runs as one of the application's services, so with those stopped it reports no
     * controllers at all. The list still builds - the file half needs no hardware - but it is then only half
     * a list, and {@link #showMyDevices} says so on screen rather than leaving an absence to be mistaken for
     * "nothing is plugged in".
     */
    private List<MyDevice> readMyDevices(List<InstallationRow> rows) {
        DeviceService service = DeviceService.getInstance();
        deviceServiceRunning = service.isAvailable();
        if (!deviceServiceRunning) {
            log.info("Device service is not running, so no controllers can be read - the list will show only "
                    + "what the installations' files name");
        }
        List<Device> attached = deviceServiceRunning ? service.getConnectedDevices() : List.of();

        return MyDeviceList.build(
                attached,
                MyDeviceList.entriesAcross(entriesByInstall(rows).values()),
                BindForgeDeviceMasterManager.getInstance().findAll(),
                FrontierStockDevices.getInstance());
    }

    /**
     * Each present installation's device entries.
     * <p>
     * An installation whose file cannot be read is left out rather than counted as holding nothing. The device
     * list is a union, so a missing contribution only makes it shorter - but silently treating an unreadable
     * file as an empty one would hide a real problem behind a shorter list.
     */
    private Map<String, List<DeviceEntry>> entriesByInstall(List<InstallationRow> rows) {
        Map<String, List<DeviceEntry>> byInstall = new LinkedHashMap<>();
        controlSchemesByInstall(rows).forEach((install, controlSchemes) -> {
            try {
                byInstall.put(install,
                        DeviceMappingsParser.parseIfPresent(controlSchemes.resolve("DeviceMappings.xml")));
            } catch (IOException e) {
                log.warn("Could not read device entries for {}: {}", install, e.getMessage());
            }
        });
        return byInstall;
    }

    /**
     * Only installations whose folder is there: a missing one cannot be read, and reporting its entries as
     * absent would fill the list with red caused by an unplugged drive.
     * <p>
     * Keyed by row id. <strong>Not by storefront</strong>, which is a label rather than a key - a machine can
     * hold two Frontier copies, several hand-added installations, or three Steams on Linux, and keying by
     * storefront would drop all but the last of them from the comparison without saying so.
     */
    private Map<String, Path> controlSchemesByInstall(List<InstallationRow> rows) {
        Map<String, Path> byInstall = new LinkedHashMap<>();
        for (InstallationRow row : rows) {
            if (row.missing()) continue;
            byInstall.put(keyOf(row), GameInstallation.controlSchemesUnder(Path.of(row.rootPath())));
        }
        return byInstall;
    }

    /** The identity a finding carries. Unique by construction, and never shown to the user. */
    static String keyOf(InstallationRow row) {
        return String.valueOf(row.id());
    }

    /**
     * What each installation is called on screen.
     * <p>
     * The storefront alone where it identifies one installation, and the storefront with its folder where it
     * does not - so two Frontier copies read as two different things rather than twice as "FRONTIER". Verbose
     * only when being brief would be ambiguous.
     */
    static Map<String, String> labelsFor(List<InstallationRow> rows) {
        Map<String, Long> countByStorefront = rows.stream()
                .collect(Collectors.groupingBy(InstallationRow::storefront, Collectors.counting()));

        Map<String, String> labels = new LinkedHashMap<>();
        for (InstallationRow row : rows) {
            boolean storefrontIsEnough = countByStorefront.getOrDefault(row.storefront(), 0L) == 1;
            labels.put(keyOf(row), storefrontIsEnough
                    ? row.storefront()
                    : row.storefront() + " (" + row.rootPath() + ")");
        }
        return labels;
    }

    private String[] myDevicesColumnNames() {
        return new String[]{
                getText("bindings.aliasDesigner.column.device"),
                getText("bindings.aliasDesigner.column.vidPid"),
                getText("bindings.aliasDesigner.column.status"),
                getText("bindings.aliasDesigner.column.alias")
        };
    }

    private void showMyDevices(List<MyDevice> devices) {
        myDevicesModel.setRowCount(0);
        for (MyDevice device : devices) {
            myDevicesModel.addRow(new Object[]{
                    device.label(),
                    device.vid() + ":" + device.pid(),
                    getText(device.attached()
                            ? "bindings.aliasDesigner.status.attached"
                            : "bindings.aliasDesigner.status.missing"),
                    aliasCell(device)
            });
        }
        // WHY: said in the table rather than only in the log. An empty list because nothing is plugged in and
        // an empty list because nothing is looking are different states, and the second is the one a user
        // cannot diagnose.
        if (!deviceServiceRunning) {
            myDevicesModel.addRow(new Object[]{
                    getText("bindings.aliasDesigner.deviceService.stopped"), "", "", ""});
        }
        HudTable.fitColumnsToContent(myDevicesTable, MAX_COLUMN_WIDTH);
    }

    /**
     * The master's name for this device, or why there is not one.
     * <p>
     * A built-in is not <em>not added</em>: Frontier's entry already names it, so there is nothing for
     * BindForge to create, and saying "not added" would invite the user to add what the game already has.
     */
    private String aliasCell(MyDevice device) {
        if (device.alias() != null) return device.alias();
        if (device.builtIn()) return getText("bindings.aliasDesigner.alias.builtIn");
        return getText("bindings.aliasDesigner.alias.notAdded");
    }

    private void showFindings(List<DeviceDivergence.Finding> findings) {
        currentFindings = findings;
        tableModel.setRowCount(0);
        for (DeviceDivergence.Finding finding : findings) {
            tableModel.addRow(new Object[]{
                    getText(severityKey(finding.severity())),
                    finding.deviceName(),
                    getText(issueKey(finding.issue())),
                    finding.installs().stream()
                            .map(key -> installLabels.getOrDefault(key, key))
                            .sorted()
                            .collect(Collectors.joining(", "))
            });
        }
        HudTable.fitColumnsToContent(table, MAX_COLUMN_WIDTH);
    }

    private String severityKey(DeviceDivergence.Severity severity) {
        return switch (severity) {
            case RED -> "bindings.aliasDesigner.severity.red";
            case YELLOW -> "bindings.aliasDesigner.severity.yellow";
            case GREEN -> "bindings.aliasDesigner.severity.green";
        };
    }

    private String issueKey(DeviceDivergence.Issue issue) {
        return switch (issue) {
            case ENTRY_MISSING_FROM_SOME -> "bindings.aliasDesigner.issue.entryMissing";
            case ENTRY_HARDWARE_DIFFERS -> "bindings.aliasDesigner.issue.hardwareDiffers";
            case ORPHANED_BUTTON_MAP -> "bindings.aliasDesigner.issue.orphanedButtonMap";
        };
    }

    private String[] columnNames() {
        return new String[]{
                getText("bindings.aliasDesigner.column.severity"),
                getText("bindings.aliasDesigner.column.device"),
                getText("bindings.aliasDesigner.column.issue"),
                getText("bindings.aliasDesigner.column.installations")
        };
    }

    /**
     * Colours the whole row by its severity, reading it from the findings rather than from the cell text.
     * <p>
     * Extends the HUD's own cell renderer rather than replacing it, so the row keeps its font, padding,
     * hover and selection behaviour and only the foreground changes. A selected row keeps the selection
     * colour: severity is worth less than knowing which row you are on.
     */
    private final class SeverityRenderer extends HudTable.CellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Component cell =
                    super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (!selected && row < currentFindings.size()) {
                cell.setForeground(colourOf(currentFindings.get(row).severity()));
            }
            return cell;
        }

        private Color colourOf(DeviceDivergence.Severity severity) {
            return switch (severity) {
                case RED -> HudPalette.HUD_COLOR_ROLE_DANGER;
                case YELLOW -> HudPalette.HUD_COLOR_ROLE_WARNING;
                case GREEN -> HudPalette.HUD_COLOR_ROLE_SUCCESS;
            };
        }
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
