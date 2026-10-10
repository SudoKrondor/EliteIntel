package elite.intel.ui.screen;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.LocationBookmarkManager;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.bookmarks.LocationBookmark;
import elite.intel.gameapi.bookmarks.LocationBookmarkFile;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.dialog.LocationBookmarkDialog;
import elite.intel.ui.dialog.LocationBookmarkImportDialog;
import elite.intel.ui.event.CommanderChangedEvent;
import elite.intel.ui.event.LocationBookmarksChangedEvent;
import elite.intel.ui.overlay.LocationBookmarkCard;
import elite.intel.ui.render.HudBooleanCellEditor;
import elite.intel.ui.render.HudBooleanCellRenderer;
import elite.intel.ui.render.HudCheckBoxHeaderRenderer;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.HudPalette.HUD_GAP;

/**
 * The Bookmarks page of the Commander tab: every bookmark in the order the overlay numbers them, with a
 * tick box for choosing which to export or delete.
 * <p>
 * The commander arranges the list here: Move Up / Move Down (or Alt+Up / Alt+Down) moves the highlighted row
 * one place, and a line between rows marks where one overlay page ends and the next begins. The tick boxes
 * play no part in it - a move is one row at a time.
 * <p>
 * Double-clicking a row shows everything known about the place and lets the commander rename it - the name
 * the overlay shows, and the one VEGA uses. Import adds bookmarks from a file to the bottom of the list and
 * never replaces it.
 * <p>
 * The list re-reads when a bookmark is saved or deleted by voice, and when another commander loads, since
 * bookmarks live in each commander's own database.
 */
public class LocationBookmarksTabPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(LocationBookmarksTabPanel.class);

    /**
     * The colour of the line drawn between overlay pages.
     */
    private static final Color PAGE_BREAK = HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION;

    private final LocationBookmarkManager bookmarks = LocationBookmarkManager.getInstance();
    private final BookmarkTableModel tableModel = new BookmarkTableModel();
    private JTable table;
    private JButton exportButton;
    private JButton deleteButton;
    private JButton moveUpButton;
    private JButton moveDownButton;

    public LocationBookmarksTabPanel() {
        buildUi();
        UiBus.register(this);
        initData();
    }

    @Subscribe
    public void onBookmarksChanged(LocationBookmarksChangedEvent event) {
        initData();
    }

    @Subscribe
    public void onCommanderChanged(CommanderChangedEvent event) {
        initData();
    }

    /**
     * Reads the list off the EDT and shows it, keeping the ticks on bookmarks that are still there.
     */
    public void initData() {
        Thread.ofVirtual().start(() -> {
            List<LocationBookmark> all = bookmarks.inOrder();
            SwingUtilities.invokeLater(() -> {
                if (table.isEditing()) table.getCellEditor().cancelCellEditing();
                Long highlighted = highlightedId();
                tableModel.setRows(all);
                highlight(highlighted);
                table.getTableHeader().repaint();
                updateButtons();
            });
        });
    }

    private void buildUi() {
        setLayout(new BorderLayout(0, HUD_GAP));
        setOpaque(false);
        setBorder(new EmptyBorder(HUD_GAP, 0, 0, 0));

        table = new JTable(tableModel) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintPageBreaks(this, g);
            }
        };
        HudTable.style(table);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        var selectColumn = table.getColumnModel().getColumn(BookmarkTableModel.COL_SELECTED);
        selectColumn.setMaxWidth(60);
        selectColumn.setPreferredWidth(60);
        selectColumn.setCellRenderer(new HudBooleanCellRenderer());
        selectColumn.setCellEditor(new HudBooleanCellEditor());
        selectColumn.setHeaderRenderer(new HudCheckBoxHeaderRenderer(tableModel::areAllSelected));
        var numberColumn = table.getColumnModel().getColumn(BookmarkTableModel.COL_NUMBER);
        numberColumn.setMaxWidth(60);
        numberColumn.setPreferredWidth(50);
        numberColumn.setCellRenderer(new HudTable.ValueCellRenderer(null, SwingConstants.RIGHT));
        table.getColumnModel().getColumn(BookmarkTableModel.COL_NAME).setPreferredWidth(360);
        table.getColumnModel().getColumn(BookmarkTableModel.COL_SYSTEM).setPreferredWidth(240);
        table.getColumnModel().getColumn(BookmarkTableModel.COL_NAME).setCellRenderer(new HudTable.ValueCellRenderer());

        // Header tick box: select all / none.
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (table.columnAtPoint(e.getPoint()) == BookmarkTableModel.COL_SELECTED) {
                    tableModel.setAllSelected(!tableModel.areAllSelected());
                    table.getTableHeader().repaint();
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e)) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && table.columnAtPoint(e.getPoint()) != BookmarkTableModel.COL_SELECTED) {
                    openDetails(tableModel.bookmark(table.convertRowIndexToModel(row)));
                }
            }
        });
        tableModel.addTableModelListener(e -> {
            updateButtons();
            table.getTableHeader().repaint();
        });
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        bindMoveKey(KeyEvent.VK_UP, "bookmarks.moveUp", true);
        bindMoveKey(KeyEvent.VK_DOWN, "bookmarks.moveDown", false);

        add(HudTable.dataPlaneScrollPane(table), BorderLayout.CENTER);
        add(buttonBar(), BorderLayout.SOUTH);
    }

    private void bindMoveKey(int key, String actionName, boolean up) {
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key, InputEvent.ALT_DOWN_MASK), actionName);
        table.getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                moveHighlighted(up);
            }
        });
    }

    /**
     * The move buttons sit on the left, apart from the tick-box actions on the right: they act on the
     * highlighted row, not the ticked ones.
     */
    private JPanel buttonBar() {
        JPanel bar = AppTheme.transparentPanel(new BorderLayout());
        JPanel arrange = AppTheme.transparentPanel(new FlowLayout(FlowLayout.LEFT, HudPalette.HUD_GAP, 0));
        moveUpButton = AppTheme.makeButtonSubtle(getText("bookmarks.action.moveUp"));
        moveUpButton.addActionListener(e -> moveHighlighted(true));
        moveDownButton = AppTheme.makeButtonSubtle(getText("bookmarks.action.moveDown"));
        moveDownButton.addActionListener(e -> moveHighlighted(false));
        arrange.add(moveUpButton);
        arrange.add(moveDownButton);

        JPanel ticked = AppTheme.transparentPanel(new FlowLayout(FlowLayout.RIGHT, HudPalette.HUD_GAP, 0));
        exportButton = AppTheme.makeButtonSubtle(getText("bookmarks.action.export"));
        exportButton.addActionListener(e -> exportSelected());
        JButton importButton = AppTheme.makeButtonSubtle(getText("bookmarks.action.import"));
        importButton.addActionListener(e -> importFromFile());
        deleteButton = AppTheme.makeButtonSubtle(getText("bookmarks.action.delete"));
        deleteButton.addActionListener(e -> deleteSelected());
        ticked.add(exportButton);
        ticked.add(importButton);
        ticked.add(deleteButton);
        bar.add(arrange, BorderLayout.WEST);
        bar.add(ticked, BorderLayout.EAST);
        updateButtons();
        return bar;
    }

    private void updateButtons() {
        boolean any = !tableModel.selectedBookmarks().isEmpty();
        exportButton.setEnabled(any);
        deleteButton.setEnabled(any);
        int row = table.getSelectedRow();
        moveUpButton.setEnabled(row > 0);
        moveDownButton.setEnabled(row >= 0 && row < tableModel.getRowCount() - 1);
    }

    /**
     * Moves the highlighted bookmark one place and keeps it highlighted, so pressing again carries on moving
     * the same one.
     */
    private void moveHighlighted(boolean up) {
        Long id = highlightedId();
        if (id == null) return;
        Thread.ofVirtual().start(() -> {
            if (!bookmarks.move(id, up)) return;
            // Every number from the old place to the new one has changed under an open card.
            LocationBookmarkCard.getInstance().close();
            initData();
        });
    }

    private Long highlightedId() {
        int row = table.getSelectedRow();
        return row < 0 ? null : tableModel.bookmark(table.convertRowIndexToModel(row)).id();
    }

    private void highlight(Long id) {
        if (id == null) return;
        int row = tableModel.rowOf(id);
        if (row < 0) return;
        table.setRowSelectionInterval(row, row);
        table.scrollRectToVisible(table.getCellRect(row, 0, true));
    }

    /**
     * Draws a line above the first row of each overlay page after the first, across the whole table, so the
     * commander sees what lands on which page of the card without counting rows.
     */
    private static void paintPageBreaks(JTable table, Graphics g) {
        g.setColor(PAGE_BREAK);
        for (int row = LocationBookmarkCard.PAGE_SIZE; row < table.getRowCount(); row += LocationBookmarkCard.PAGE_SIZE) {
            int y = table.getCellRect(row, 0, true).y;
            g.fillRect(0, y - 1, table.getWidth(), 2);
        }
    }

    private void openDetails(LocationBookmark bookmark) {
        LocationBookmarkDialog.edit(this, bookmark).ifPresent(rename -> {
            bookmarks.rename(bookmark.id(), rename.displayName());
            initData();
        });
    }

    private void exportSelected() {
        List<LocationBookmark> selected = tableModel.selectedBookmarks();
        if (selected.isEmpty()) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getText("bookmarks.export.title"));
        chooser.setSelectedFile(new File("bookmarks_export.json"));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        if (chooser.showSaveDialog(SwingUtilities.getWindowAncestor(this)) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".json")) file = new File(file.getAbsolutePath() + ".json");
        try {
            Files.writeString(file.toPath(), LocationBookmarkFile.toJson(selected), StandardCharsets.UTF_8);
            HudConfirmDialog.info(this, getText("bookmarks.export.title"),
                    getText("bookmarks.export.success", selected.size()), getText("button.ok"));
        } catch (IOException e) {
            log.warn("Could not export bookmarks to {}: {}", file, e.getMessage());
            HudConfirmDialog.info(this, getText("bookmarks.export.title"), getText("bookmarks.export.error"),
                    getText("button.ok"));
        }
    }

    private void importFromFile() {
        List<LocationBookmark> chosen = LocationBookmarkImportDialog.showImportFlow(this, bookmarks.inOrder());
        if (chosen == null || chosen.isEmpty()) return;
        int added = bookmarks.importAll(chosen);
        initData();
        HudConfirmDialog.info(this, getText("bookmarks.import.title"),
                getText("bookmarks.import.success", added), getText("button.ok"));
    }

    private void deleteSelected() {
        List<LocationBookmark> selected = tableModel.selectedBookmarks();
        if (selected.isEmpty()) return;
        boolean confirmed = HudConfirmDialog.confirm(this,
                getText("bookmarks.delete.title"),
                getText("bookmarks.delete.confirm", selected.size()),
                getText("bookmarks.action.delete"),
                getText("button.cancel"));
        if (!confirmed) return;
        bookmarks.deleteAll(selected.stream().map(LocationBookmark::id).toList());
        // The bookmarks after a deleted one have moved up a number, as when one is deleted by voice.
        LocationBookmarkCard.getInstance().close();
        initData();
    }

    // -------------------------------------------------------------------------

    static final class BookmarkTableModel extends AbstractTableModel {
        static final int COL_SELECTED = 0;
        static final int COL_NUMBER = 1;
        static final int COL_NAME = 2;
        static final int COL_SYSTEM = 3;

        private List<LocationBookmark> rows = List.of();
        /**
         * Ticks by row id, so a re-read of the list keeps them on the bookmarks that are still there.
         */
        private final Set<Long> selected = new HashSet<>();

        void setRows(List<LocationBookmark> bookmarks) {
            rows = List.copyOf(bookmarks);
            Set<Long> present = new HashSet<>();
            rows.forEach(b -> present.add(b.id()));
            selected.retainAll(present);
            fireTableDataChanged();
        }

        LocationBookmark bookmark(int row) {
            return rows.get(row);
        }

        int rowOf(long id) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).id() == id) return i;
            }
            return -1;
        }

        boolean areAllSelected() {
            return !rows.isEmpty() && selected.size() == rows.size();
        }

        void setAllSelected(boolean value) {
            selected.clear();
            if (value) rows.forEach(b -> selected.add(b.id()));
            fireTableDataChanged();
        }

        /**
         * The ticked bookmarks, in list order.
         */
        List<LocationBookmark> selectedBookmarks() {
            return rows.stream().filter(b -> selected.contains(b.id())).toList();
        }

        @Override
        public int getRowCount() {
            return rows.size();
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
            return col == COL_SELECTED;
        }

        @Override
        public String getColumnName(int col) {
            return switch (col) {
                case COL_SELECTED -> getText("bookmarks.column.select");
                case COL_NUMBER -> getText("bookmarks.column.number");
                case COL_NAME -> getText("bookmarks.column.displayName");
                case COL_SYSTEM -> getText("bookmarks.column.starSystem");
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            LocationBookmark bookmark = rows.get(row);
            return switch (col) {
                case COL_SELECTED -> selected.contains(bookmark.id());
                case COL_NUMBER -> String.valueOf(row + 1);
                case COL_NAME -> LocationBookmarkCard.label(bookmark);
                case COL_SYSTEM -> bookmark.starSystem();
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col != COL_SELECTED) return;
            long id = rows.get(row).id();
            if (Boolean.TRUE.equals(value)) selected.add(id);
            else selected.remove(id);
            fireTableCellUpdated(row, col);
        }
    }
}
