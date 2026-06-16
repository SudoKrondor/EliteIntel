package elite.intel.ui.dialog;

import elite.intel.ui.render.HudBooleanCellEditor;
import elite.intel.ui.render.HudBooleanCellRenderer;
import elite.intel.ui.render.HudCheckBoxHeaderRenderer;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudTable;

import elite.intel.ai.brain.actions.customcommand.CustomCommandDefinition;
import elite.intel.ai.brain.actions.customcommand.CustomCommandExportImportService;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * Modal dialog for selecting and exporting custom command definitions to a JSON file.
 * All commands are pre-selected. The checkbox column header acts as a toggle-all control.
 */
public final class CustomCommandExportDialog extends JDialog {

    private final ExportTableModel tableModel;

    public CustomCommandExportDialog(Component parent, List<CustomCommandDefinition> commands) {
        super(
            SwingUtilities.getWindowAncestor(parent),
            getText("actions.customCommands.export.title"),
            ModalityType.APPLICATION_MODAL
        );
        setUndecorated(true);
        this.tableModel = new ExportTableModel(commands);
        buildUi();
        pack();
        setMinimumSize(new Dimension(720, 380));
        setLocationRelativeTo(parent);
    }

    /** Displays the dialog; blocks until closed. */
    public void showDialog() {
        setVisible(true);
    }

    private void buildUi() {
        HudSection exportSection = HudSection.flat(
                getText("actions.customCommands.export.section.selection"), new BorderLayout());
        exportSection.body().add(buildScrollPane(), BorderLayout.CENTER);

        JButton export = AppTheme.makeButton(getText("actions.customCommands.export.button"));
        export.addActionListener(e -> doExport());
        JButton back = AppTheme.makeButtonSubtle(getText("button.back"));
        back.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("actions.customCommands.export.title"))
                .onClose(this::dispose)
                .body(exportSection)
                .scrollBody(false)            // scroll already inside the table
                .primary(export)              // right side
                .dismiss(back)                // left side
                .build();

        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(export);
    }

    private JScrollPane buildScrollPane() {
        JTable table = new JTable(tableModel);
        HudTable.style(table);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.getColumnModel().getColumn(ExportTableModel.COL_SELECTED).setMaxWidth(40);
        table.getColumnModel().getColumn(ExportTableModel.COL_SELECTED).setPreferredWidth(40);
        table.getColumnModel().getColumn(ExportTableModel.COL_NAME).setPreferredWidth(260);
        table.getColumnModel().getColumn(ExportTableModel.COL_ACTION_KEY).setPreferredWidth(360);

        // Checkbox column: HUD renderer and single-click editor, no native LAF checkbox
        table.getColumnModel().getColumn(ExportTableModel.COL_SELECTED)
                .setCellRenderer(new HudBooleanCellRenderer());
        table.getColumnModel().getColumn(ExportTableModel.COL_SELECTED)
                .setCellEditor(new HudBooleanCellEditor());
        table.getColumnModel().getColumn(ExportTableModel.COL_SELECTED)
                .setHeaderRenderer(new HudCheckBoxHeaderRenderer(tableModel::areAllSelected));

        // Name and action-key columns: caps + ACCENT colour
        var nameRenderer = new HudTable.ValueCellRenderer();
        table.getColumnModel().getColumn(ExportTableModel.COL_NAME).setCellRenderer(nameRenderer);
        table.getColumnModel().getColumn(ExportTableModel.COL_ACTION_KEY).setCellRenderer(nameRenderer);

        // Header checkbox click toggles all rows
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (table.columnAtPoint(e.getPoint()) == ExportTableModel.COL_SELECTED) {
                    tableModel.setAllSelected(!tableModel.areAllSelected());
                    table.getTableHeader().repaint();
                }
            }
        });

        return HudTable.scrollPane(table);
    }

    private void doExport() {
        List<CustomCommandDefinition> selected = tableModel.selectedCommands();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                getText("actions.customCommands.export.noSelection"),
                getText("actions.customCommands.export.title"),
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getText("actions.customCommands.export.title"));
        chooser.setSelectedFile(new File("custom_commands_export.json"));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().endsWith(".json")) {
            file = new File(file.getAbsolutePath() + ".json");
        }

        try {
            String json = CustomCommandExportImportService.toJson(selected);
            Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this,
                getText("actions.customCommands.export.success", selected.size()),
                getText("actions.customCommands.export.title"),
                JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                getText("actions.customCommands.export.error"),
                getText("actions.customCommands.export.title"),
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------------------

    private static final class ExportTableModel extends AbstractTableModel {
        static final int COL_SELECTED = 0;
        static final int COL_NAME = 1;
        static final int COL_ACTION_KEY = 2;

        private final List<CustomCommandDefinition> commands;
        private final boolean[] selected;

        ExportTableModel(List<CustomCommandDefinition> commands) {
            this.commands = new ArrayList<>(commands);
            this.selected = new boolean[commands.size()];
            Arrays.fill(selected, true);
        }

        boolean areAllSelected() {
            for (boolean s : selected) if (!s) return false;
            return selected.length > 0;
        }

        void setAllSelected(boolean value) {
            Arrays.fill(selected, value);
            fireTableDataChanged();
        }

        List<CustomCommandDefinition> selectedCommands() {
            List<CustomCommandDefinition> result = new ArrayList<>();
            for (int i = 0; i < commands.size(); i++) {
                if (selected[i]) result.add(commands.get(i));
            }
            return result;
        }

        @Override public int getRowCount() { return commands.size(); }
        @Override public int getColumnCount() { return 3; }
        @Override public Class<?> getColumnClass(int col) { return col == COL_SELECTED ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int row, int col) { return col == COL_SELECTED; }

        @Override
        public String getColumnName(int col) {
            return switch (col) {
                case COL_NAME       -> getText("actions.customCommands.column.name");
                case COL_ACTION_KEY -> getText("actions.customCommands.editor.actionKey");
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            CustomCommandDefinition cmd = commands.get(row);
            return switch (col) {
                case COL_SELECTED   -> selected[row];
                case COL_NAME       -> cmd.getName();
                case COL_ACTION_KEY -> cmd.getActionKey();
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == COL_SELECTED) {
                selected[row] = Boolean.TRUE.equals(value);
                fireTableCellUpdated(row, col);
            }
        }
    }
}
