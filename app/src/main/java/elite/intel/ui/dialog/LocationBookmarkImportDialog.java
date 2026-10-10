package elite.intel.ui.dialog;

import elite.intel.gameapi.bookmarks.LocationBookmark;
import elite.intel.gameapi.bookmarks.LocationBookmarkFile;
import elite.intel.gameapi.bookmarks.LocationBookmarkFile.Candidate;
import elite.intel.ui.overlay.LocationBookmarkCard;
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
 * Picks a bookmark file and lets the commander choose which of its bookmarks to add.
 * <p>
 * Every new place starts ticked. A place already bookmarked, or one the file lists twice, is shown as a
 * duplicate and cannot be ticked; an entry that does not describe a place is shown as invalid, likewise.
 * Importing adds - it never replaces the list. Use {@link #showImportFlow} as the entry point.
 */
public final class LocationBookmarkImportDialog extends JDialog {

    private final ImportTableModel tableModel;
    private List<LocationBookmark> result;  // null = cancelled

    private LocationBookmarkImportDialog(Component parent, List<Candidate> candidates) {
        super(SwingUtilities.getWindowAncestor(parent), getText("bookmarks.import.title"), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.tableModel = new ImportTableModel(candidates);
        buildUi();
        pack();
        setMinimumSize(new Dimension(720, 400));
        setLocationRelativeTo(parent);
    }

    /**
     * Opens a file picker, reads the chosen file, then shows the bookmarks in it to choose from.
     *
     * @param existing the bookmarks already saved, to mark duplicates against
     * @return the bookmarks chosen, or {@code null} when the commander cancelled or the file was unreadable
     */
    public static List<LocationBookmark> showImportFlow(Component parent, List<LocationBookmark> existing) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getText("bookmarks.import.title"));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        if (chooser.showOpenDialog(SwingUtilities.getWindowAncestor(parent)) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        List<Candidate> candidates;
        try {
            String json = Files.readString(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8);
            candidates = LocationBookmarkFile.parse(json, existing);
        } catch (IOException | IllegalArgumentException e) {
            HudConfirmDialog.info(parent, getText("bookmarks.import.title"), getText("bookmarks.import.error"),
                    getText("button.ok"));
            return null;
        }
        if (candidates.isEmpty()) {
            HudConfirmDialog.info(parent, getText("bookmarks.import.title"), getText("bookmarks.import.empty"),
                    getText("button.ok"));
            return null;
        }

        LocationBookmarkImportDialog dialog = new LocationBookmarkImportDialog(parent, candidates);
        AppTheme.runWithModalScrim(SwingUtilities.getWindowAncestor(parent), () -> dialog.setVisible(true));
        return dialog.result;
    }

    private void buildUi() {
        HudSection section = HudSection.flat(getText("bookmarks.import.section"), new BorderLayout());
        section.body().add(buildScrollPane(), BorderLayout.CENTER);

        JButton importBtn = AppTheme.makeButton(getText("bookmarks.import.button"));
        importBtn.addActionListener(e -> doImport());
        JButton back = AppTheme.makeButtonSubtle(getText("button.back"));
        back.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bookmarks.import.title"))
                .onClose(this::dispose)
                .body(section)
                .scrollBody(false)
                .primary(importBtn)
                .dismiss(back)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(importBtn);
    }

    private JScrollPane buildScrollPane() {
        JTable table = new JTable(tableModel);
        HudTable.style(table);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);

        var selectColumn = table.getColumnModel().getColumn(ImportTableModel.COL_SELECTED);
        selectColumn.setMaxWidth(40);
        selectColumn.setPreferredWidth(40);
        selectColumn.setCellRenderer(new HudBooleanCellRenderer());
        selectColumn.setCellEditor(new HudBooleanCellEditor());
        selectColumn.setHeaderRenderer(new HudCheckBoxHeaderRenderer(tableModel::areAllImportableSelected));
        table.getColumnModel().getColumn(ImportTableModel.COL_NAME).setPreferredWidth(300);
        table.getColumnModel().getColumn(ImportTableModel.COL_SYSTEM).setPreferredWidth(220);
        table.getColumnModel().getColumn(ImportTableModel.COL_STATUS).setPreferredWidth(120);
        table.getColumnModel().getColumn(ImportTableModel.COL_STATUS).setCellRenderer(new StatusCellRenderer(tableModel));

        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (table.columnAtPoint(e.getPoint()) == ImportTableModel.COL_SELECTED) {
                    tableModel.setAllSelected(!tableModel.areAllImportableSelected());
                    table.getTableHeader().repaint();
                }
            }
        });
        return HudTable.scrollPane(table);
    }

    private void doImport() {
        List<LocationBookmark> selected = tableModel.selectedBookmarks();
        if (selected.isEmpty()) {
            HudConfirmDialog.info(this, getText("bookmarks.import.title"), getText("bookmarks.import.noSelection"),
                    getText("button.ok"));
            return;
        }
        result = selected;
        dispose();
    }

    // -------------------------------------------------------------------------

    /**
     * Status in caps, green for new, amber for a duplicate, red for an invalid entry.
     */
    private static final class StatusCellRenderer extends HudTable.CellRenderer {

        private final ImportTableModel model;

        StatusCellRenderer(ImportTableModel model) {
            this.model = model;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Object display = value == null ? "" : value.toString().toUpperCase(Locale.ROOT);
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, display, isSelected, hasFocus, row, column);
            label.setIcon(null);
            label.setToolTipText(null);
            if (isSelected) {
                label.setForeground(HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT);
                return label;
            }
            label.setForeground(switch (model.candidate(row).status()) {
                case NEW -> HudPalette.HUD_COLOR_ROLE_SUCCESS;
                case DUPLICATE -> HudPalette.HUD_COLOR_ROLE_WARNING;
                case INVALID -> HudPalette.HUD_COLOR_ROLE_DANGER;
            });
            return label;
        }
    }

    // -------------------------------------------------------------------------

    static final class ImportTableModel extends AbstractTableModel {
        static final int COL_SELECTED = 0;
        static final int COL_NAME = 1;
        static final int COL_SYSTEM = 2;
        static final int COL_STATUS = 3;

        private final List<Candidate> candidates;
        private final boolean[] selected;

        ImportTableModel(List<Candidate> candidates) {
            this.candidates = List.copyOf(candidates);
            this.selected = new boolean[candidates.size()];
            for (int i = 0; i < selected.length; i++) {
                selected[i] = this.candidates.get(i).importable();
            }
        }

        Candidate candidate(int row) {
            return candidates.get(row);
        }

        boolean areAllImportableSelected() {
            boolean any = false;
            for (int i = 0; i < selected.length; i++) {
                if (!candidates.get(i).importable()) continue;
                if (!selected[i]) return false;
                any = true;
            }
            return any;
        }

        void setAllSelected(boolean value) {
            for (int i = 0; i < selected.length; i++) {
                if (candidates.get(i).importable()) selected[i] = value;
            }
            fireTableDataChanged();
        }

        /**
         * The ticked bookmarks, in file order.
         */
        List<LocationBookmark> selectedBookmarks() {
            List<LocationBookmark> chosen = new ArrayList<>();
            for (int i = 0; i < selected.length; i++) {
                if (selected[i] && candidates.get(i).importable()) chosen.add(candidates.get(i).bookmark());
            }
            return chosen;
        }

        @Override
        public int getRowCount() {
            return candidates.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return col == COL_SELECTED ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == COL_SELECTED && candidates.get(row).importable();
        }

        @Override
        public String getColumnName(int col) {
            return switch (col) {
                case COL_NAME -> getText("bookmarks.column.displayName");
                case COL_SYSTEM -> getText("bookmarks.column.starSystem");
                case COL_STATUS -> getText("bookmarks.import.column.status");
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            Candidate c = candidates.get(row);
            return switch (col) {
                case COL_SELECTED -> selected[row];
                case COL_NAME -> c.bookmark() != null ? LocationBookmarkCard.label(c.bookmark())
                        : c.name() != null ? c.name() : "";
                case COL_SYSTEM -> c.starSystem() != null ? c.starSystem() : "";
                case COL_STATUS -> getText("bookmarks.import.status." + c.status().name().toLowerCase(Locale.ROOT));
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (isCellEditable(row, col)) {
                selected[row] = Boolean.TRUE.equals(value);
                fireTableCellUpdated(row, col);
            }
        }
    }
}
