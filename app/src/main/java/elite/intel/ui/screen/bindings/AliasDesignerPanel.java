package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.bindforge.devices.BuiltInDevice;
import elite.intel.bindforge.devices.MissingControllerWarning;
import elite.intel.bindforge.devices.MyDevice;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudFooter;
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

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * Alias Designer, the view: My Devices and Built-in Devices, and CHECK AGAIN.
 * <p>
 * <strong>It draws; {@link AliasDesignerController} acts.</strong> Every button hands its press to the controller, which
 * runs the flow - reading off the EDT, asking and telling the user - and hands back a {@link AliasDesignerReader.Snapshot}
 * to draw.
 * <p>
 * <strong>It shows the working copy with the edits on screen</strong> (Storage S4a): a name given or changed shows at
 * once, and APPLY above BindForge's tabs writes it. <strong>No installations on this screen</strong> (principle 12):
 * the divergence list and the Installations columns were removed - File Manager's Game Install Locations is where the
 * files are, and the check (Storage S9) reconciles an installation that differs.
 * <p>
 * <strong>First-time startup does not run from here</strong> - it runs from a click on the BindForge tab
 * ({@link FirstTimeStartupFlow}).
 * <p>
 * <strong>Built-in Devices is the second view of the device list</strong>, a tab beside My Devices: Frontier's shipped
 * entries from the reference in the jar, read-only (Alan, 2026-10-08).
 * <p>
 * <strong>A gear ends every My Devices row</strong> and opens the Device Editor for that device (Alan, 2026-10-10).
 * <p>
 * <strong>The first time it is shown in a run, it says which named controllers are missing</strong> - those the
 * bindings name, spoken and logged, once (Alan, 2026-10-11, A13; {@link MissingControllerWarning}).
 * <p>
 * <strong>Onboarding opens once per run, right after first-time startup</strong>, when an attached controller has no
 * name - again on CHECK AGAIN, and from the gear, for one controller (Alan, 2026-10-14, A6b; {@link OnboardingDialog}).
 * Its names are edits not applied, like every other change here.
 */
public class AliasDesignerPanel extends JPanel {

    private static final int MAX_COLUMN_WIDTH = 420;

    /** The My Devices columns describing the device, in order. The gear follows them. */
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

    private DefaultTableModel myDevicesModel;
    private JTable myDevicesTable;
    private DefaultTableModel builtInModel;
    private JTable builtInTable;

    // WHY: what was last drawn, kept for the renderers and the gear - all on the EDT, all set by render().
    private List<BuiltInDevice> currentBuiltIn = List.of();
    /** The My Devices rows, in table order - the gear opens the one it sits on. */
    private List<MyDevice> currentMyDevices = List.of();
    /** Whether the Device Service was running at the last refresh. */
    private boolean deviceServiceRunning;

    public AliasDesignerPanel() {
        buildUi();
        controller = new AliasDesignerController(this);
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

        myDevicesModel = new MyDevicesTableModel(myDevicesColumnNames());
        myDevicesTable = new JTable(myDevicesModel);
        myDevicesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        HudTable.style(myDevicesTable);
        // WHY: locked, so the palette pass that walks the tree later does not restyle the table and take the gear's
        // renderer with it (ED_HUD_REFERENCE section 8.6). The cells keep the HUD's own renderer.
        myDevicesTable.setDefaultRenderer(Object.class, new HudTable.CellRenderer());
        myDevicesTable.putClientProperty(AppTheme.HUD_TABLE_STYLE_LOCKED, Boolean.TRUE);

        builtInModel = new ReadOnlyTableModel(builtInColumnNames(), 0);
        builtInTable = new JTable(builtInModel);
        builtInTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        HudTable.style(builtInTable);
        // WHY: locked, or the palette pass that walks the tree later calls styleTable and puts the standard renderer
        // back - this renderer carries the alternative pairs' tooltip (ED_HUD_REFERENCE section 8.6).
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

        add(myDevices, BorderLayout.CENTER);
        add(HudFooter.build(false, null, null, List.of(rescanButton)), BorderLayout.SOUTH);
    }

    /**
     * Draws one refresh. On the EDT.
     */
    void render(AliasDesignerReader.Snapshot snapshot) {
        deviceServiceRunning = Boolean.TRUE.equals(snapshot.running());
        showMyDevices(snapshot.myDevices());
        showBuiltIn(snapshot.builtIn());
    }

    /** The columns describing the device, then the gear's. */
    private static String[] myDevicesColumnNames() {
        List<String> names = new ArrayList<>();
        DEVICE_COLUMN_KEYS.forEach(key -> names.add(getText(key)));
        // WHY: the gear is last, and on every row (spec, My Devices).
        names.add("");
        return names.toArray(String[]::new);
    }

    private void showMyDevices(List<MyDevice> devices) {
        currentMyDevices = devices;
        myDevicesModel.setRowCount(0);
        for (MyDevice device : devices) {
            myDevicesModel.addRow(new Object[]{
                    device.label(),
                    device.vid() + ":" + device.pid(),
                    getText(device.attached()
                            ? "bindings.aliasDesigner.status.attached"
                            : "bindings.aliasDesigner.status.missing"),
                    aliasCell(device),
                    ""});
        }
        // WHY: said in the table rather than only in the log. An empty list because nothing is plugged in and
        // an empty list because nothing is looking are different states, and the second is the one a user
        // cannot diagnose.
        if (!deviceServiceRunning) {
            myDevicesModel.addRow(new Object[]{getText("bindings.aliasDesigner.deviceService.stopped")});
        }
        HudTable.fitColumnsToContent(myDevicesTable, MAX_COLUMN_WIDTH);
        TableColumn gear = myDevicesTable.getColumnModel().getColumn(myDevicesModel.getColumnCount() - 1);
        gear.setCellRenderer(new GearRenderer());
        gear.setCellEditor(new GearEditor());
        gear.setPreferredWidth(HUD_TABLE_ROW_HEIGHT + 4);
        gear.setMaxWidth(HUD_TABLE_ROW_HEIGHT + 10);
    }

    /** The gear: the row's device, for the controller to open the Device Editor with. */
    private void openDeviceEditor(int row) {
        if (row < 0 || row >= currentMyDevices.size()) return;
        controller.openDeviceEditor(currentMyDevices.get(row));
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
     * The device's name in the edits, or why there is not one.
     * <p>
     * A built-in is not <em>not added</em>: Frontier's entry already names it, so there is nothing for
     * BindForge to create, and saying "not added" would invite the user to add what the game already has.
     */
    private String aliasCell(MyDevice device) {
        if (device.alias() != null) return device.alias();
        if (device.builtIn()) return getText("bindings.aliasDesigner.alias.builtIn");
        return getText("bindings.aliasDesigner.alias.notAdded");
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
