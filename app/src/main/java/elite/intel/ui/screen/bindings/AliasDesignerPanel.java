package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devices.DeviceDivergence;
import elite.intel.bindforge.devices.DeviceDivergenceScanner;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.bindforge.install.WindowsGameInstallationProvider;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.session.PlayerSession;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudFooter;
import elite.intel.ui.widget.HudPanel;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static final int MAX_COLUMN_WIDTH = 420;

    private final InstallationRegistry registry;

    private DefaultTableModel tableModel;
    private JTable table;
    private List<DeviceDivergence.Finding> currentFindings = List.of();

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

        JButton rescanButton = makeButton(getText("bindings.aliasDesigner.button.recheck"));
        rescanButton.addActionListener(e -> initData());

        add(section, BorderLayout.CENTER);
        add(HudFooter.build(false, null, null, List.of(rescanButton)), BorderLayout.SOUTH);
    }

    public void initData() {
        showFindings(DeviceDivergenceScanner.scan(
                controlSchemesByInstall(), PlayerSession.getInstance().getBindingsDir()));
    }

    /**
     * Only installations whose folder is there: a missing one cannot be read, and reporting its entries as
     * absent would fill the list with red caused by an unplugged drive.
     */
    private Map<String, Path> controlSchemesByInstall() {
        Map<String, Path> byInstall = new LinkedHashMap<>();
        for (InstallationRow row : registry.current()) {
            if (row.missing()) continue;
            byInstall.put(row.storefront(),
                    GameInstallation.controlSchemesUnder(Path.of(row.rootPath())));
        }
        return byInstall;
    }

    private void showFindings(List<DeviceDivergence.Finding> findings) {
        currentFindings = findings;
        tableModel.setRowCount(0);
        for (DeviceDivergence.Finding finding : findings) {
            tableModel.addRow(new Object[]{
                    getText(severityKey(finding.severity())),
                    finding.deviceName(),
                    getText(issueKey(finding.issue())),
                    String.join(", ", finding.installs())
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
