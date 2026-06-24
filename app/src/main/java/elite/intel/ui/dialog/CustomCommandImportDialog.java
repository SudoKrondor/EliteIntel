package elite.intel.ui.dialog;

import elite.intel.ai.brain.actions.customcommand.CustomCommandDefinition;
import elite.intel.ai.brain.actions.customcommand.CustomCommandExportImportService;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import elite.intel.ui.render.HudBooleanCellEditor;
import elite.intel.ui.render.HudBooleanCellRenderer;
import elite.intel.ui.render.HudCheckBoxHeaderRenderer;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudTable;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Modal dialog for reviewing and importing custom commands from a JSON export file.
 * <p>
 * Invalid entries are shown in a muted color and cannot be selected. Conflicting entries
 * (same actionKey as an existing command) are pre-selected and shown in orange; importing
 * them will overwrite the existing command. The checkbox column header acts as a toggle-all
 * control (operates on valid entries only). Use {@link #showImportFlow} as the entry point.
 */
public final class CustomCommandImportDialog extends JDialog {

    private final ImportTableModel tableModel;
    private List<CustomCommandDefinition> result;  // null = cancelled

    private CustomCommandImportDialog(
            Component parent,
            List<CustomCommandExportImportService.ImportCandidate> candidates
    ) {
        super(
            SwingUtilities.getWindowAncestor(parent),
            getText("actions.customCommands.import.title"),
            ModalityType.APPLICATION_MODAL
        );
        setUndecorated(true);
        this.tableModel = new ImportTableModel(candidates);
        buildUi(candidates);
        pack();
        setMinimumSize(new Dimension(820, 420));
        setLocationRelativeTo(parent);
    }

    /**
     * Opens a file picker, parses the chosen file, then shows the import selection dialog.
     *
     * @return the definitions selected for import, or {@code null} if the user cancelled at any step
     */
    public static List<CustomCommandDefinition> showImportFlow(Component parent, List<CustomCommandDefinition> existing) {
        return showImportFlow(parent, existing, null);
    }

    /**
     * As {@link #showImportFlow(Component, List)} but opens the file picker in {@code startDir} when
     * non-null - used by "Restore from backup" to land the chooser in the backups folder.
     */
    public static List<CustomCommandDefinition> showImportFlow(
            Component parent, List<CustomCommandDefinition> existing, java.nio.file.Path startDir) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getText("actions.customCommands.import.title"));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        if (startDir != null && Files.isDirectory(startDir)) {
            chooser.setCurrentDirectory(startDir.toFile());
        }
        if (chooser.showOpenDialog(SwingUtilities.getWindowAncestor(parent)) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        String json;
        try {
            json = Files.readString(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent,
                getText("actions.customCommands.import.error"),
                getText("actions.customCommands.import.title"),
                JOptionPane.ERROR_MESSAGE);
            return null;
        }

        List<CustomCommandExportImportService.ImportCandidate> candidates;
        try {
            candidates = CustomCommandExportImportService.parseImport(json, existing);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(parent,
                getText("actions.customCommands.import.error"),
                getText("actions.customCommands.import.title"),
                JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (candidates.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                getText("actions.customCommands.import.empty"),
                getText("actions.customCommands.import.title"),
                JOptionPane.INFORMATION_MESSAGE);
            return List.of();
        }

        CustomCommandImportDialog dialog = new CustomCommandImportDialog(parent, candidates);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void buildUi(List<CustomCommandExportImportService.ImportCandidate> candidates) {
        HudSection importSection = HudSection.flat(
                getText("actions.customCommands.import.section.review"), new BorderLayout());
        importSection.body().add(buildScrollPane(candidates), BorderLayout.CENTER);

        JButton importBtn = AppTheme.makeButton(getText("actions.customCommands.import.button"));
        importBtn.addActionListener(e -> doImport());
        JButton back = AppTheme.makeButtonSubtle(getText("button.back"));
        back.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("actions.customCommands.import.title"))
                .onClose(this::dispose)
                .body(importSection)
                .scrollBody(false)            // scroll already inside the table, body is not scrolled
                .primary(importBtn)           // right side
                .dismiss(back)                // left side
                .build();

        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(importBtn);
    }

    private JScrollPane buildScrollPane(List<CustomCommandExportImportService.ImportCandidate> candidates) {
        JTable table = new JTable(tableModel);
        HudTable.style(table);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);

        table.getColumnModel().getColumn(ImportTableModel.COL_SELECTED).setMaxWidth(40);
        table.getColumnModel().getColumn(ImportTableModel.COL_SELECTED).setPreferredWidth(40);
        table.getColumnModel().getColumn(ImportTableModel.COL_NAME).setPreferredWidth(220);
        table.getColumnModel().getColumn(ImportTableModel.COL_ACTION_KEY).setPreferredWidth(280);
        table.getColumnModel().getColumn(ImportTableModel.COL_STATUS).setPreferredWidth(120);

        // Checkbox column: HUD renderer and single-click editor, no native LAF checkbox
        table.getColumnModel().getColumn(ImportTableModel.COL_SELECTED)
                .setCellRenderer(new HudBooleanCellRenderer());
        table.getColumnModel().getColumn(ImportTableModel.COL_SELECTED)
                .setCellEditor(new HudBooleanCellEditor());
        table.getColumnModel().getColumn(ImportTableModel.COL_SELECTED)
                .setHeaderRenderer(new HudCheckBoxHeaderRenderer(tableModel::areAllValidSelected));

        // Name and action-key columns: caps + HUD_COLOR_ROLE_PRIMARY_ACTION colour
        var nameRenderer = new HudTable.ValueCellRenderer();
        table.getColumnModel().getColumn(ImportTableModel.COL_NAME).setCellRenderer(nameRenderer);
        table.getColumnModel().getColumn(ImportTableModel.COL_ACTION_KEY).setCellRenderer(nameRenderer);

        // Status column: caps + status-specific colour (danger / warn / ok)
        table.getColumnModel().getColumn(ImportTableModel.COL_STATUS)
                .setCellRenderer(new StatusCellRenderer(candidates));

        // Header checkbox click toggles all valid rows
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (table.columnAtPoint(e.getPoint()) == ImportTableModel.COL_SELECTED) {
                    tableModel.setAllSelected(!tableModel.areAllValidSelected());
                    table.getTableHeader().repaint();
                }
            }
        });

        return HudTable.scrollPane(table);
    }

    private void doImport() {
        List<CustomCommandDefinition> selected = tableModel.selectedDefinitions();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                getText("actions.customCommands.import.noSelection"),
                getText("actions.customCommands.import.title"),
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        result = selected;
        dispose();
    }

    // -------------------------------------------------------------------------

    /** Caps status text and applies danger/warn/ok foreground by candidate validity. */
    private static final class StatusCellRenderer extends HudTable.CellRenderer {

        private final List<CustomCommandExportImportService.ImportCandidate> candidates;

        StatusCellRenderer(List<CustomCommandExportImportService.ImportCandidate> candidates) {
            this.candidates = candidates;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
        ) {
            Object display = value == null ? "" : value.toString().toUpperCase(Locale.ROOT);
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, display, isSelected, hasFocus, row, column);
            label.setToolTipText(null);
            label.setIcon(null);

            if (row >= candidates.size()) return label;
            CustomCommandExportImportService.ImportCandidate c = candidates.get(row);

            // On selected rows keep HUD_COLOR_ROLE_SELECTED_TEXT; coloured status text is only for unselected rows.
            if (isSelected) {
                label.setForeground(HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT);
            } else if (!c.isValid()) {
                label.setForeground(HudPalette.HUD_COLOR_ROLE_DANGER);
            } else if (c.hasConflict()) {
                label.setForeground(HudPalette.HUD_COLOR_ROLE_WARNING);
            } else {
                label.setForeground(HudPalette.HUD_COLOR_ROLE_SUCCESS);
            }

            if (!c.isValid()) {
                label.setToolTipText(MultiLingualTextProvider.getText(
                        "actions.customCommands.import.invalid.description",
                        String.join("; ", c.validationErrors())));
            } else if (c.hasConflict()) {
                label.setToolTipText(MultiLingualTextProvider.getText(
                        "actions.customCommands.import.conflict.description",
                        c.definition().getActionKey()));
            }
            return label;
        }
    }

    // -------------------------------------------------------------------------

    static final class ImportTableModel extends AbstractTableModel {
        static final int COL_SELECTED = 0;
        static final int COL_NAME = 1;
        static final int COL_ACTION_KEY = 2;
        static final int COL_STATUS = 3;

        private final List<CustomCommandExportImportService.ImportCandidate> candidates;
        private final boolean[] selected;

        ImportTableModel(List<CustomCommandExportImportService.ImportCandidate> candidates) {
            this.candidates = candidates;
            this.selected = new boolean[candidates.size()];
            for (int i = 0; i < candidates.size(); i++) {
                selected[i] = candidates.get(i).isValid();
            }
        }

        /** True when all valid entries are selected. */
        boolean areAllValidSelected() {
            for (int i = 0; i < selected.length; i++) {
                if (candidates.get(i).isValid() && !selected[i]) return false;
            }
            return true;
        }

        /** Toggles selection on all valid entries; invalid entries are unaffected. */
        void setAllSelected(boolean value) {
            for (int i = 0; i < selected.length; i++) {
                if (candidates.get(i).isValid()) selected[i] = value;
            }
            fireTableDataChanged();
        }

        List<CustomCommandDefinition> selectedDefinitions() {
            List<CustomCommandDefinition> result = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                if (selected[i] && candidates.get(i).isValid()) {
                    result.add(candidates.get(i).definition());
                }
            }
            return result;
        }

        @Override public int getRowCount() { return candidates.size(); }
        @Override public int getColumnCount() { return 4; }

        @Override
        public Class<?> getColumnClass(int col) {
            return col == COL_SELECTED ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == COL_SELECTED && candidates.get(row).isValid();
        }

        @Override
        public String getColumnName(int col) {
            return switch (col) {
                case COL_NAME       -> getText("actions.customCommands.column.name");
                case COL_ACTION_KEY -> getText("actions.customCommands.editor.actionKey");
                case COL_STATUS     -> getText("actions.customCommands.import.column.status");
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            CustomCommandExportImportService.ImportCandidate c = candidates.get(row);
            return switch (col) {
                case COL_SELECTED   -> selected[row];
                case COL_NAME       -> c.definition() != null ? c.definition().getName() : "(null)";
                case COL_ACTION_KEY -> c.definition() != null ? c.definition().getActionKey() : "";
                case COL_STATUS -> {
                    if (!c.isValid())    yield getText("actions.customCommands.import.status.invalid");
                    if (c.hasConflict()) yield getText("actions.customCommands.import.status.conflict");
                    yield getText("actions.customCommands.import.status.ok");
                }
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == COL_SELECTED && candidates.get(row).isValid()) {
                selected[row] = Boolean.TRUE.equals(value);
                fireTableCellUpdated(row, col);
            }
        }
    }
}
