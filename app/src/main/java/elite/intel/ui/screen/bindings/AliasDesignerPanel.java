package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.*;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DivergenceWaysOut.WayOut;
import elite.intel.bindforge.devices.BuiltInDevice;
import elite.intel.bindforge.devices.InstallationMarkers;
import elite.intel.bindforge.devices.MissingControllerWarning;
import elite.intel.bindforge.devices.MyDevice;
import elite.intel.bindforge.install.GameInstallationProvider;
import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.session.PlayerSession;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudFooter;
import elite.intel.ui.widget.HudPanel;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * Alias Designer, the view: My Devices and Built-in Devices above the divergence list, the selected row's ways out, and
 * the footer's SET UP, CHECK AGAIN, DISCARD and APPLY. The list compares each installation with the master, or before
 * activation, the installations with each other.
 * <p>
 * <strong>It draws; {@link AliasDesignerController} acts.</strong> Every button hands its press to the controller, which
 * runs the flow - reading and writing off the EDT, asking and telling the user - and hands back a
 * {@link AliasDesignerReader.Snapshot} to draw. What the screen reads is {@link AliasDesignerReader}'s; what it says once
 * a write has run is {@link AliasDesignerReports}'.
 * <p>
 * <strong>Activation runs from here</strong>, through the controller, the first time the BindForge tab is shown in a run (Alan, 2026-10-13,
 * A1b; {@link Activation}): the master filled with no prompt when the installations are identical, first setup
 * opened when they are not. Every test of "set up" here is "activated", never "the master has rows" - a user who
 * owns nothing is activated with an empty master.
 * <p>
 * Every installation is supposed to hold the same device files, so anywhere they disagree is something to
 * resolve. <strong>One list, not three</strong> - every kind of disagreement appears together, because
 * splitting them would mean BindForge deciding which kinds deserve attention. What ranks them is the
 * severity, and severity is judged against the bindings: a missing entry only costs the user something when
 * a binding actually names that device.
 * <p>
 * <strong>The ways out are offered per row</strong> once the master exists: select a row and its buttons appear
 * beneath the list - revert, adopt, or the label re-merge - each confirmed before it runs (Alan, 2026-10-08).
 * <p>
 * <strong>My Devices carries one Installations column per installation</strong> once the master exists: each cell
 * says whether that installation matches the master for the device, in the divergence list's colour - a readout,
 * never a switch (Alan, 2026-10-08).
 * <p>
 * <strong>Built-in Devices is the second view of the device list</strong>, a tab beside My Devices: Frontier's
 * shipped entries from the reference in the jar, read-only, with the divergence list beneath both (Alan, 2026-10-08).
 * <p>
 * <strong>A gear ends every My Devices row</strong> and opens the Device Editor for that device - the one record,
 * edited in a dialog and saved to the draft (Alan, 2026-10-10). Before activation it says SET UP comes first and
 * offers it: first setup throws any draft away as it fills the master, so edits saved before then would be lost.
 * <p>
 * <strong>APPLY and DISCARD are the screen's</strong>, in its footer (Alan, 2026-10-11, A10). APPLY writes the master
 * and the draft's changes to every installation and reports per installation; it is offered whenever the master
 * exists, since Apply with nothing saved is the retry after a partial one. DISCARD throws the whole draft away and
 * is offered only when the draft changes something, which the line beside it names.
 * <p>
 * <strong>The first time it is shown in a run, it says which named controllers are missing</strong> - those the
 * bindings name, spoken and logged, once (Alan, 2026-10-11, A13; {@link MissingControllerWarning}).
 * <p>
 * <strong>Onboarding opens once per run, right after activation</strong>, when an attached controller has no name
 * - again on CHECK AGAIN, and from the gear, for one controller (Alan, 2026-10-14, A6b; {@link OnboardingDialog}). A
 * controller no binding names by hardware id is written to every installation when the user finishes; one
 * {@code .binds} names by hex is held in the draft for APPLY, which moves those bindings to the name.
 */
public class AliasDesignerPanel extends JPanel {

    private static final int MAX_COLUMN_WIDTH = 420;

    /**
     * The My Devices columns describing the device, in order. The Installations columns follow them, so their count
     * is where the markers start - stated once here, for the column names and the renderer alike.
     */
    private static final List<String> DEVICE_COLUMN_KEYS = List.of(
            "bindings.aliasDesigner.column.device",
            "bindings.aliasDesigner.column.vidPid",
            "bindings.aliasDesigner.column.status",
            "bindings.aliasDesigner.column.alias");

    /** The Built-in Devices columns, in order - stated once, for the column names and the tooltip's column alike. */
    private static final List<String> BUILT_IN_COLUMN_KEYS = List.of(
            "bindings.aliasDesigner.column.device",
            "bindings.aliasDesigner.column.vidPid",
            "bindings.aliasDesigner.column.alsoCovers",
            "bindings.aliasDesigner.column.status");

    /** The Built-in Devices column carrying the alternative pairs, whose cell lists them all as a tooltip. */
    private static final int ALSO_COVERS_COLUMN =
            BUILT_IN_COLUMN_KEYS.indexOf("bindings.aliasDesigner.column.alsoCovers");

    /** Pairs per line of that tooltip - GamePad's 79 on one line would run off the screen. */
    private static final int PAIRS_PER_TOOLTIP_LINE = 6;

    private final AliasDesignerController controller;

    private DefaultTableModel tableModel;
    private JTable table;
    private DefaultTableModel myDevicesModel;
    private JTable myDevicesTable;
    private DefaultTableModel builtInModel;
    private JTable builtInTable;
    private JPanel waysOutPanel;
    private JButton setUpButton;
    private JButton applyButton;
    private JButton discardButton;
    private JLabel pendingLine;

    // WHY: what was last drawn, kept for the renderers, the ways out and the gear - all on the EDT, all set by render().
    private List<BuiltInDevice> currentBuiltIn = List.of();
    private List<DeviceDivergence.Finding> currentFindings = List.of();
    private AliasDesignerReader.Markers currentMarkers = AliasDesignerReader.Markers.NONE;
    /**
     * The check the findings came from, or {@code null} before activation, when there are no ways out.
     */
    private DeviceFilesCheck.Report currentReport;
    private Map<String, String> installLabels = Map.of();
    /** Each installation's folder, keyed as findings key them - for the Device Editor's install strip. */
    private Map<String, String> installPaths = Map.of();
    /** The My Devices rows, in table order - the gear opens the one it sits on. */
    private List<MyDevice> currentMyDevices = List.of();
    /** Whether the Device Service was running at the last refresh. */
    private boolean deviceServiceRunning;

    public AliasDesignerPanel() {
        this(new InstallationRegistry(
                GameInstallationProvider.forThisPlatform(PlayerSession.getInstance().getBindingsDir())));
    }

    AliasDesignerPanel(InstallationRegistry registry) {
        buildUi();
        controller = new AliasDesignerController(this, registry);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !isShowing()) return;
            controller.onShown();
        });
    }

    public void dispose() {
        controller.dispose();
    }

    /**
     * Reads everything again - the tab's refresh.
     */
    public void initData() {
        controller.refresh();
    }

    /**
     * Activation, the first time the BindForge tab is shown in a run - see {@link AliasDesignerController}.
     */
    public void activateOnFirstShowing() {
        controller.activateOnFirstShowing();
    }

    /**
     * What <strong>Review in Bind Editor</strong> does, after a retarget (A12): the BindForge tab shows the bindings.
     * Set by the tab that holds both screens; until then the button only closes the notice.
     */
    public void setReviewBindings(Runnable showBindings) {
        controller.setReviewBindings(showBindings);
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
        // WHY: beneath the list rather than in the footer, as first setup offers its choices - the buttons belong
        // to the selected row, and change with it.
        waysOutPanel = transparentPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        section.body().add(waysOutPanel, BorderLayout.SOUTH);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showWaysOut();
        });

        myDevicesModel = new MyDevicesTableModel(myDevicesColumnNames(List.of()));
        myDevicesTable = new JTable(myDevicesModel);
        myDevicesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        HudTable.style(myDevicesTable);
        // WHY: each Installations marker is coloured by its own state, the canon's text colour - and locked, as the
        // divergence table is, so the palette pass does not put the plain renderer back.
        myDevicesTable.setDefaultRenderer(Object.class, new MarkerRenderer());
        myDevicesTable.putClientProperty(AppTheme.HUD_TABLE_STYLE_LOCKED, Boolean.TRUE);

        builtInModel = new ReadOnlyTableModel(builtInColumnNames(), 0);
        builtInTable = new JTable(builtInModel);
        builtInTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        HudTable.style(builtInTable);
        // WHY: locked for the same reason - this renderer carries the alternative pairs' tooltip.
        builtInTable.setDefaultRenderer(Object.class, new AlternativesRenderer());
        builtInTable.putClientProperty(AppTheme.HUD_TABLE_STYLE_LOCKED, Boolean.TRUE);

        JPanel builtIn = transparentPanel(new BorderLayout(0, 6));
        builtIn.add(HudTable.dataPlaneScrollPane(builtInTable), BorderLayout.CENTER);
        // WHY: the reference belongs to one game version, so the list says which - a controller Frontier added
        // since is not on it, and its absence must not read as Frontier not supporting it.
        builtIn.add(AppTheme.hudReadoutValue(getText("bindings.aliasDesigner.builtIn.captured",
                        FrontierStockDevices.CAPTURED.toString()),
                HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT), BorderLayout.SOUTH);

        // WHY: the two views of the device list are a COMPACT tab pair, as the spec and the UI component map give
        // them. The tabs name the views, so neither sits in a titled section of its own.
        JTabbedPane myDevices = AppTheme.makeCompactTabs();
        myDevices.addTab(getText("bindings.aliasDesigner.section.myDevices"),
                HudTable.dataPlaneScrollPane(myDevicesTable));
        myDevices.addTab(getText("bindings.aliasDesigner.section.builtIn"), builtIn);

        JButton rescanButton = makeButton(getText("bindings.aliasDesigner.button.recheck"));
        rescanButton.addActionListener(e -> controller.checkAgain());

        // WHY: offered only until BindForge is activated - first setup runs once. Activation opens it on its own the
        // first time the tab is shown; abandoned, it waits here for the user (Alan, 2026-10-13).
        setUpButton = makeButton(getText("bindings.aliasDesigner.firstSetup.button"));
        setUpButton.setEnabled(false);
        setUpButton.addActionListener(e -> controller.openFirstSetup());

        // WHY: stacked rather than tabbed. The two answer different questions - what devices there are, and
        // where the installations disagree about them - and a user resolving a divergence wants the device it
        // names in sight. Equal halves because neither is the subordinate of the other. The divergence list stays
        // beneath both device views, since it is not a view of the device list.
        JPanel stacked = new JPanel(new GridLayout(2, 1, 0, 6));
        stacked.setOpaque(false);
        stacked.add(myDevices);
        stacked.add(section);

        // WHY: APPLY is the primary, last on the right; DISCARD sits on the left with the line naming what it would
        // throw away, so the two are never side by side.
        applyButton = makeButton(getText("bindings.aliasDesigner.button.apply"));
        applyButton.setEnabled(false);
        applyButton.addActionListener(e -> controller.apply());
        discardButton = makeButtonSubtle(getText("bindings.aliasDesigner.button.discard"));
        discardButton.setEnabled(false);
        discardButton.addActionListener(e -> controller.discard());
        pendingLine = AppTheme.hudReadoutValue("", HudPalette.HUD_COLOR_ROLE_WARNING);
        JPanel left = transparentPanel(new FlowLayout(FlowLayout.LEFT, HudPalette.HUD_GAP, 0));
        left.add(discardButton);
        left.add(pendingLine);

        add(stacked, BorderLayout.CENTER);
        add(HudFooter.build(false, null, left, List.of(setUpButton, rescanButton, applyButton)), BorderLayout.SOUTH);
    }

    /**
     * Draws one refresh. On the EDT.
     */
    void render(AliasDesignerReader.Snapshot snapshot) {
        deviceServiceRunning = Boolean.TRUE.equals(snapshot.running());
        installLabels = snapshot.labels();
        installPaths = snapshot.paths();
        showMyDevices(snapshot.myDevices(), snapshot.markers());
        showBuiltIn(snapshot.builtIn());
        currentReport = snapshot.report();
        showFindings(snapshot.findings());
    }

    /**
     * The footer's buttons, as the controller judges them, and the line naming what DISCARD would throw away.
     *
     * @param changes what the draft changes, by device; empty when nothing
     */
    void refreshButtons(boolean setUp, boolean apply, boolean discard, List<String> changes) {
        setUpButton.setEnabled(setUp);
        applyButton.setEnabled(apply);
        // WHY a tooltip on the dimmed button: a user pressing APPLY to be sure is told there is nothing to send,
        // rather than left wondering why it does not answer. Before activation SET UP is the way in instead.
        applyButton.setToolTipText(apply || setUp ? null : getText("bindings.aliasDesigner.apply.nothing"));
        discardButton.setEnabled(discard);
        pendingLine.setText(changes.isEmpty()
                ? ""
                : getText("bindings.aliasDesigner.pending", String.join(", ", changes)));
    }

    /**
     * While a write runs, none of the ways out can start another.
     */
    void disableWaysOut() {
        for (Component button : waysOutPanel.getComponents()) button.setEnabled(false);
    }

    /**
     * The selected row's ways out, one button each - or, where there is none, why. Before first setup there is no
     * master to revert to or adopt into, so SET UP is the way out.
     */
    void showWaysOut() {
        waysOutPanel.removeAll();
        int index = table.getSelectedRow();
        if (index >= 0 && index < currentFindings.size()) {
            DeviceDivergence.Finding finding = currentFindings.get(index);
            if (currentReport == null) {
                waysOutPanel.add(note(getText("bindings.aliasDesigner.wayOut.beforeSetUp")));
            } else {
                List<WayOut> ways = DivergenceWaysOut.of(finding, currentReport);
                if (ways.isEmpty()) waysOutPanel.add(note(getText(noWayOutKey(DivergenceWaysOut.whyNone(finding)))));
                for (WayOut way : ways) waysOutPanel.add(wayOutButton(way, finding));
            }
        }
        waysOutPanel.revalidate();
        waysOutPanel.repaint();
    }

    private static JLabel note(String text) {
        return AppTheme.hudReadoutValue(text, HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
    }

    private static String noWayOutKey(DivergenceWaysOut.NoWayOut why) {
        return why == DivergenceWaysOut.NoWayOut.ORPHAN
                ? "bindings.aliasDesigner.wayOut.none.orphan"
                : "bindings.aliasDesigner.wayOut.none.unreadable";
    }

    private JButton wayOutButton(WayOut way, DeviceDivergence.Finding finding) {
        AliasDesignerReports names = new AliasDesignerReports(installLabels);
        JButton button = makeButtonSubtle(switch (way.kind()) {
            case REVERT -> getText("bindings.aliasDesigner.wayOut.revert", names.namesOf(way.installIds()));
            case ADOPT -> getText("bindings.aliasDesigner.wayOut.adopt", names.namesOf(way.installIds()),
                    String.valueOf(way.hardware()));
            case REMERGE -> getText("bindings.aliasDesigner.wayOut.remerge");
        });
        button.addActionListener(e -> {
            switch (way.kind()) {
                case REVERT -> controller.revert(way);
                case ADOPT -> controller.adopt(way, finding);
                case REMERGE -> controller.reMerge(way, finding);
            }
        });
        button.setEnabled(!controller.actionRunning());
        return button;
    }

    /** The columns describing the device, then one per installation once the master exists. */
    private String[] myDevicesColumnNames(List<String> markerInstalls) {
        List<String> names = new ArrayList<>();
        DEVICE_COLUMN_KEYS.forEach(key -> names.add(getText(key)));
        markerInstalls.forEach(install -> names.add(installLabels.getOrDefault(install, install)));
        // WHY: the gear is last, after the Installations columns, and on every row (spec, My Devices).
        names.add("");
        return names.toArray(String[]::new);
    }

    private void showMyDevices(List<MyDevice> devices, AliasDesignerReader.Markers markers) {
        currentMarkers = markers;
        currentMyDevices = devices;
        myDevicesModel.setRowCount(0);
        // WHY: the column set follows the installations, which can be added or removed between refreshes.
        myDevicesModel.setColumnIdentifiers(myDevicesColumnNames(markers.installs()));
        for (int i = 0; i < devices.size(); i++) {
            MyDevice device = devices.get(i);
            List<Object> cells = new ArrayList<>(List.of(
                    device.label(),
                    device.vid() + ":" + device.pid(),
                    getText(device.attached()
                            ? "bindings.aliasDesigner.status.attached"
                            : "bindings.aliasDesigner.status.missing"),
                    aliasCell(device)));
            if (i < markers.perDevice().size()) {
                Map<String, InstallationMarkers.Marker> deviceMarkers = markers.perDevice().get(i);
                markers.installs().forEach(install -> cells.add(markerText(deviceMarkers.get(install))));
            }
            while (cells.size() < myDevicesModel.getColumnCount()) cells.add("");
            myDevicesModel.addRow(cells.toArray());
        }
        // WHY: said in the table rather than only in the log. An empty list because nothing is plugged in and
        // an empty list because nothing is looking are different states, and the second is the one a user
        // cannot diagnose.
        if (!deviceServiceRunning) {
            myDevicesModel.addRow(new Object[]{getText("bindings.aliasDesigner.deviceService.stopped")});
        }
        HudTable.fitColumnsToContent(myDevicesTable, MAX_COLUMN_WIDTH);
        // WHY: after the columns are rebuilt - setColumnIdentifiers makes new ones, without the gear's renderer.
        TableColumn gear = myDevicesTable.getColumnModel().getColumn(myDevicesModel.getColumnCount() - 1);
        gear.setCellRenderer(new GearRenderer());
        gear.setCellEditor(new GearEditor());
        gear.setPreferredWidth(HUD_TABLE_ROW_HEIGHT + 4);
        gear.setMaxWidth(HUD_TABLE_ROW_HEIGHT + 10);
    }

    /** The gear: the row's device and its install strip, for the controller to open the Device Editor with. */
    private void openDeviceEditor(int row) {
        if (row < 0 || row >= currentMyDevices.size()) return;
        Map<String, InstallationMarkers.Marker> markers =
                row < currentMarkers.perDevice().size() ? currentMarkers.perDevice().get(row) : Map.of();
        List<DeviceEditorDialog.StripLine> strip = currentMarkers.installs().stream()
                .map(install -> new DeviceEditorDialog.StripLine(installLabels.getOrDefault(install, install),
                        markers.get(install), installPaths.getOrDefault(install, "")))
                .toList();
        controller.openDeviceEditor(currentMyDevices.get(row), strip);
    }

    /**
     * Frontier's entries, one row each in file order: the element's own pair, how many more it covers, and whether a
     * controller plugged in now resolves to it. Read-only - there is nothing on this view to act on.
     */
    private void showBuiltIn(List<BuiltInDevice> devices) {
        currentBuiltIn = devices;
        builtInModel.setRowCount(0);
        for (BuiltInDevice device : devices) {
            builtInModel.addRow(new Object[]{
                    device.name(),
                    device.primary() == null ? "" : pairText(device.primary()),
                    device.alternatives().isEmpty()
                            ? ""
                            : getText("bindings.aliasDesigner.builtIn.alsoCovers",
                            String.valueOf(device.alternatives().size())),
                    device.attached() ? getText("bindings.aliasDesigner.status.attached") : ""
            });
        }
        // WHY: as on My Devices - a list with nothing attached because nothing is looking must say so.
        if (!deviceServiceRunning) {
            builtInModel.addRow(new Object[]{getText("bindings.aliasDesigner.deviceService.stopped")});
        }
        HudTable.fitColumnsToContent(builtInTable, MAX_COLUMN_WIDTH);
    }

    private static String pairText(HardwareId pair) {
        return pair.vid() + ":" + pair.pid();
    }

    private static String[] builtInColumnNames() {
        return BUILT_IN_COLUMN_KEYS.stream().map(key -> getText(key)).toArray(String[]::new);
    }

    /**
     * What a marker says. A difference is a word as well as a colour, so the state never rests on colour alone
     * (Alan, 2026-10-08).
     */
    static String markerText(InstallationMarkers.Marker marker) {
        if (marker == null) return "";
        return getText(switch (marker.state()) {
            case MATCHES -> "bindings.aliasDesigner.marker.matches";
            // WHY: its own key, not the Alias column's. That one means the master has no record; this one means the
            // installation holds no entry. The same words today, but not the same fact.
            case NOT_ADDED -> "bindings.aliasDesigner.marker.notAdded";
            case DIFFERS -> "bindings.aliasDesigner.marker.differs";
            case NOT_FOUND -> "bindings.aliasDesigner.marker.notFound";
        });
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
            case ENTRY_MISSING -> "bindings.aliasDesigner.issue.entryMissing";
            case ENTRY_HARDWARE_DIFFERS -> "bindings.aliasDesigner.issue.hardwareDiffers";
            case ORPHANED_BUTTON_MAP -> "bindings.aliasDesigner.issue.orphanedButtonMap";
            case ENTRY_OUT_OF_PLACE -> "bindings.aliasDesigner.issue.outOfPlace";
            case ENTRY_NOT_IN_MASTER -> "bindings.aliasDesigner.issue.notInMaster";
            case LABELS_DIFFER -> "bindings.aliasDesigner.issue.labelsDiffer";
            case BUTTON_MAP_MISSING -> "bindings.aliasDesigner.issue.buttonMapMissing";
            case BUTTON_MAP_UNREADABLE -> "bindings.aliasDesigner.issue.buttonMapUnreadable";
            case FILE_RESET -> "bindings.aliasDesigner.issue.fileReset";
            case FILE_UNREADABLE -> "bindings.aliasDesigner.issue.fileUnreadable";
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
    }

    /**
     * Colours each Installations cell by its marker, and leaves the four device columns as the HUD draws them. A
     * marker with nothing to judge keeps the plain text colour. A selected row keeps the selection colour, as in
     * the divergence list.
     */
    private final class MarkerRenderer extends HudTable.CellRenderer {
        private static final int FIRST_MARKER_COLUMN = DEVICE_COLUMN_KEYS.size();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Component cell =
                    super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            int modelColumn = table.convertColumnIndexToModel(column);
            int marker = modelColumn - FIRST_MARKER_COLUMN;
            if (!selected && marker >= 0 && marker < currentMarkers.installs().size()
                    && row < currentMarkers.perDevice().size()) {
                InstallationMarkers.Marker found =
                        currentMarkers.perDevice().get(row).get(currentMarkers.installs().get(marker));
                if (found != null && found.severity() != null) cell.setForeground(colourOf(found.severity()));
            }
            return cell;
        }
    }

    /**
     * Lists every alternative pair as the <em>Also covers</em> cell's tooltip, a few to a line, and draws every cell
     * as the HUD otherwise does. The row shows the count; the pairs are there for whoever asks.
     */
    private final class AlternativesRenderer extends HudTable.CellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Component cell =
                    super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            String tooltip = null;
            if (table.convertColumnIndexToModel(column) == ALSO_COVERS_COLUMN && row < currentBuiltIn.size()) {
                tooltip = tooltipOf(currentBuiltIn.get(row).alternatives());
            }
            setToolTipText(tooltip);
            return cell;
        }

        private static String tooltipOf(List<HardwareId> pairs) {
            if (pairs.isEmpty()) return null;
            StringBuilder html = new StringBuilder("<html>");
            for (int i = 0; i < pairs.size(); i++) {
                if (i > 0) html.append(i % PAIRS_PER_TOOLTIP_LINE == 0 ? "<br>" : ", ");
                html.append(pairText(pairs.get(i)));
            }
            return html.append("</html>").toString();
        }
    }

    static Color colourOf(DeviceDivergence.Severity severity) {
        return switch (severity) {
            case RED -> HudPalette.HUD_COLOR_ROLE_DANGER;
            case YELLOW -> HudPalette.HUD_COLOR_ROLE_WARNING;
            case GREEN -> HudPalette.HUD_COLOR_ROLE_SUCCESS;
        };
    }

    /** My Devices: read-only but for the gear, which is the last column. */
    private static final class MyDevicesTableModel extends DefaultTableModel {
        private MyDevicesTableModel(Object[] columnNames) {
            super(columnNames, 0);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == getColumnCount() - 1;
        }
    }

    /**
     * A gear cell, copied from the fleet list's in {@code CommanderTabPanel} so the two lists open their editors the
     * same way (UI component map, Alias Designer). It becomes one shared HUD gear column when a third list takes it.
     */
    private static final class GearCell {
        final JPanel panel = new JPanel(new BorderLayout());
        final JButton gear = new JButton();
        private final ImageIcon gearBase =
                HudGlyphs.scaledIcon(AliasDesignerPanel.class, "/images/settings.png", HUD_ICON_TABLE);
        private ImageIcon gearOrange;
        private ImageIcon gearDark;

        GearCell() {
            gear.setOpaque(false);
            gear.setContentAreaFilled(false);
            gear.setBorderPainted(false);
            gear.setFocusPainted(false);
            gear.setHorizontalAlignment(SwingConstants.CENTER);
            gear.setToolTipText(getText("bindings.aliasDesigner.editor.gear"));
            panel.add(gear, BorderLayout.CENTER);
        }

        private ImageIcon gearIcon(boolean selected) {
            if (selected) {
                if (gearDark == null) {
                    gearDark = HudGlyphs.tintIcon(gearBase, HUD_ICON_TABLE, HUD_ICON_TABLE,
                            HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT);
                }
                return gearDark;
            }
            if (gearOrange == null) {
                gearOrange = HudGlyphs.tintIcon(gearBase, HUD_ICON_TABLE, HUD_ICON_TABLE,
                        HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION);
            }
            return gearOrange;
        }

        /** @param device whether the row is a device - the "service stopped" row has no gear */
        JPanel apply(boolean selected, boolean device) {
            panel.setBackground(selected
                    ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION
                    : HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
            panel.setOpaque(true);
            gear.setIcon(device ? gearIcon(selected) : null);
            gear.setEnabled(device);
            return panel;
        }
    }

    private final class GearRenderer implements TableCellRenderer {
        private final GearCell cell = new GearCell();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            return cell.apply(selected, row < currentMyDevices.size());
        }
    }

    /** Opens the Device Editor on a single click, as the fleet list's gear opens a ship's settings. */
    private final class GearEditor extends AbstractCellEditor implements TableCellEditor {
        private final GearCell cell = new GearCell();
        private int editingRow = -1;

        GearEditor() {
            cell.gear.addActionListener(e -> {
                int row = editingRow;
                fireEditingStopped();
                openDeviceEditor(row);
            });
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            return true;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row,
                                                     int column) {
            editingRow = row;
            return cell.apply(true, row < currentMyDevices.size());
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
