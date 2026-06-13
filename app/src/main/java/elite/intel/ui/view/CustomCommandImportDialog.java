package elite.intel.ui.view;

import elite.intel.ai.brain.actions.customcommand.CustomCommandDefinition;
import elite.intel.ai.brain.actions.customcommand.CustomCommandExportImportService;
import elite.intel.ui.i18n.MultiLingualTextProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Modal dialog for reviewing and importing custom commands from a JSON export file.
 * <p>
 * Invalid entries are shown in a muted color and cannot be selected. Conflicting entries
 * (same actionKey as an existing command) are pre-selected and shown in orange; importing
 * them will overwrite the existing command. The checkbox column header acts as a toggle-all
 * control (operates on valid entries only). Use {@link #showImportFlow} as the entry point.
 */
final class CustomCommandImportDialog extends JDialog {

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
    static List<CustomCommandDefinition> showImportFlow(Component parent, List<CustomCommandDefinition> existing) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getText("actions.customCommands.import.title"));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
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
        JPanel root = AppTheme.transparentPanel(new BorderLayout(AppTheme.HUD_GAP, AppTheme.HUD_GAP));
        root.setOpaque(true);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        root.setBackground(AppTheme.HUD_BG);
        HudSection importSection = new HudSection(getText("actions.customCommands.import.section.review"), new BorderLayout());
        importSection.body().add(buildScrollPane(candidates), BorderLayout.CENTER);
        root.add(importSection, BorderLayout.CENTER);
        root.add(buildBottomBar(), BorderLayout.SOUTH);
        setContentPane(root);
        getContentPane().setBackground(AppTheme.HUD_BG);
    }

    private JScrollPane buildScrollPane(List<CustomCommandExportImportService.ImportCandidate> candidates) {
        JTable table = new JTable(tableModel);
        HudTable.style(table);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setDefaultRenderer(String.class, new StatusCellRenderer(candidates));
        table.getColumnModel().getColumn(ImportTableModel.COL_SELECTED).setMaxWidth(40);
        table.getColumnModel().getColumn(ImportTableModel.COL_SELECTED).setPreferredWidth(40);
        table.getColumnModel().getColumn(ImportTableModel.COL_NAME).setPreferredWidth(220);
        table.getColumnModel().getColumn(ImportTableModel.COL_ACTION_KEY).setPreferredWidth(280);
        table.getColumnModel().getColumn(ImportTableModel.COL_STATUS).setPreferredWidth(120);

        // Column header is a clickable checkbox that toggles all valid rows
        table.getColumnModel().getColumn(ImportTableModel.COL_SELECTED)
            .setHeaderRenderer(new CheckBoxHeaderRenderer(tableModel::areAllValidSelected));
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

    private JPanel buildBottomBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        panel.setOpaque(false);
        JButton cancel = AppTheme.makeButtonSubtle(getText("button.cancel"));
        cancel.addActionListener(e -> dispose());
        JButton importBtn = AppTheme.makeButton(getText("actions.customCommands.import.button"));
        importBtn.addActionListener(e -> doImport());
        panel.add(cancel);
        panel.add(importBtn);
        return panel;
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

    /**
     * Renders a checkbox whose state reflects whether all valid rows are selected.
     * The checkbox is non-opaque inside a JPanel wrapper that carries the LAF border,
     * so column separator lines remain visible regardless of Look and Feel.
     */
    private static final class CheckBoxHeaderRenderer implements TableCellRenderer {
        private final JCheckBox checkBox = new JCheckBox();
        private final JPanel wrapper = new JPanel(new GridBagLayout());
        private final BooleanSupplier allSelectedQuery;

        CheckBoxHeaderRenderer(BooleanSupplier allSelectedQuery) {
            this.allSelectedQuery = allSelectedQuery;
            checkBox.setOpaque(false);
            wrapper.setOpaque(true);
            wrapper.setBackground(AppTheme.HUD_PANEL_BG_ALT);
            wrapper.add(checkBox);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            checkBox.setSelected(allSelectedQuery.getAsBoolean());
            // Copy border and background from the default header renderer to get
            // the same LAF-drawn separator lines as the other header columns.
            Component defaultComp = table.getTableHeader().getDefaultRenderer()
                .getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (defaultComp instanceof JComponent jc) {
                wrapper.setBorder(jc.getBorder());
                wrapper.setBackground(jc.getBackground());
            }
            return wrapper;
        }
    }

    // -------------------------------------------------------------------------

    /** Colors invalid entries red and conflict entries orange; shows tooltip on the Status column. */
    private static final class StatusCellRenderer extends DefaultTableCellRenderer {
        private static final Icon CONFLICT_ICON = buildConflictIcon();

        private static Icon buildConflictIcon() {
            int s = 13;
            BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int[] xp = {s / 2, 0, s - 1};
            int[] yp = {0, s - 1, s - 1};
            g.setColor(AppTheme.ACCENT);
            g.fillPolygon(xp, yp, 3);
            g.setColor(AppTheme.BG);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("!", (s - fm.stringWidth("!")) / 2, s - 3);
            g.dispose();
            return new ImageIcon(img);
        }

        private final List<CustomCommandExportImportService.ImportCandidate> candidates;

        StatusCellRenderer(List<CustomCommandExportImportService.ImportCandidate> candidates) {
            this.candidates = candidates;
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
        ) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            label.setBorder(new EmptyBorder(4, 8, 4, 8));
            label.setToolTipText(null);

            if (row >= candidates.size()) return label;
            CustomCommandExportImportService.ImportCandidate c = candidates.get(row);

            if (!isSelected) {
                label.setBackground(row % 2 == 0 ? AppTheme.HUD_TABLE_BG : AppTheme.HUD_ROW_ALT);
                if (!c.isValid()) {
                    label.setForeground(AppTheme.HUD_DANGER);
                } else if (c.hasConflict()) {
                    label.setForeground(AppTheme.ACCENT);
                } else {
                    label.setForeground(AppTheme.FG);
                }
            }

            if (column == ImportTableModel.COL_STATUS) {
                if (!c.isValid()) {
                    label.setIcon(null);
                    label.setToolTipText(MultiLingualTextProvider.getText(
                        "actions.customCommands.import.invalid.description",
                        String.join("; ", c.validationErrors())));
                } else if (c.hasConflict()) {
                    label.setIcon(CONFLICT_ICON);
                    label.setHorizontalTextPosition(SwingConstants.LEADING);
                    label.setIconTextGap(5);
                    label.setToolTipText(MultiLingualTextProvider.getText(
                        "actions.customCommands.import.conflict.description",
                        c.definition().getActionKey()));
                } else {
                    label.setIcon(null);
                }
            } else {
                label.setIcon(null);
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
