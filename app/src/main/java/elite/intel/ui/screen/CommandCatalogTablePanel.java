package elite.intel.ui.screen;

import elite.intel.ui.dialog.CommandDetailsDialog;
import elite.intel.ui.render.HudCommandNameCellRenderer;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudConnectedToolbar;
import elite.intel.ui.widget.HudSearchField;
import elite.intel.ui.widget.HudTable;

import elite.intel.ai.brain.actions.catalog.CommandCatalog;
import elite.intel.ai.brain.actions.catalog.CommandCatalogEntry;
import elite.intel.ai.brain.actions.catalog.CommandCatalogEntryType;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Read-only command catalog view used to inspect and launch built-in commands.
 */
public class CommandCatalogTablePanel extends JPanel {

    private final CommandCatalog commandCatalog = new CommandCatalog();
    private List<CommandCatalogEntry> visibleEntries = List.of();
    private JTextField searchField;
    private JTable table;
    private DefaultTableModel tableModel;

    public CommandCatalogTablePanel() {
        buildUi();
        initData();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> styleTable(table));
    }

    private void buildUi() {
        setLayout(new BorderLayout(AppTheme.HUD_GAP, 0));
        setBorder(AppTheme.hudSubtabContentBorder());
        setBackground(AppTheme.HUD_BG);

        tableModel = new ReadOnlyTableModel(columnNames(), 0);
        table = new JTable(tableModel);
        styleTable(table);
        installRowHover(table);
        table.addMouseListener(new CommandDetailsMouseListener());

        JScrollPane scrollPane = HudTable.dataPlaneScrollPane(table);

        add(searchPanel(), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void initData() {
        tableModel.setRowCount(0);
        visibleEntries = filteredEntries();
        for (CommandCatalogEntry entry : visibleEntries) {
            tableModel.addRow(new Object[]{
                    new CommandNameCell(entry.name(), entry.id()),
                    readableType(entry.type())
            });
        }
    }

    private JPanel searchPanel() {
        HudConnectedToolbar toolbar = new HudConnectedToolbar();

        HudSearchField searchPanel = new HudSearchField(
                getText("actions.commands.search.placeholder"),
                getText("actions.commands.search.clearTooltip"),
                HudSearchField.Variant.TABLE_FILTER);
        searchField = searchPanel.textField();
        searchField.getDocument().addDocumentListener(new SearchDocumentListener());
        toolbar.add(searchPanel, BorderLayout.CENTER);

        return toolbar;
    }

    private String[] columnNames() {
        return new String[]{
                getText("actions.commands.column.name"),
                getText("actions.commands.column.type")
        };
    }

    private List<CommandCatalogEntry> sortedEntries() {
        return commandCatalog.entries().stream()
                .sorted(Comparator
                        .comparing(CommandCatalogEntry::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CommandCatalogEntry::id))
                .toList();
    }

    private List<CommandCatalogEntry> filteredEntries() {
        String query = normalizedSearchText();
        return sortedEntries().stream()
                .filter(entry -> query.isBlank() || matchesSearch(entry, query))
                .toList();
    }

    private boolean matchesSearch(CommandCatalogEntry entry, String query) {
        return entry.name().toLowerCase(Locale.ROOT).contains(query)
                || entry.id().toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalizedSearchText() {
        if (searchField == null) {
            return "";
        }
        return searchField.getText().trim().toLowerCase(Locale.ROOT);
    }

    private void resetSearch() {
        searchField.setText("");
        initData();
    }

    private void openCommandDetailsAtViewRow(int viewRow) {
        if (viewRow < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= visibleEntries.size()) {
            return;
        }
        AppTheme.runWithModalScrim(
                SwingUtilities.getWindowAncestor(this),
                () -> new CommandDetailsDialog(this, visibleEntries.get(modelRow)).showDialog());
    }

    private String readableType(CommandCatalogEntryType type) {
        return switch (type) {
            case BUILT_IN_BINDING -> getText("actions.commands.type.builtInBinding");
            case BUILT_IN_ACTION -> getText("actions.commands.type.builtInAction");
            case CUSTOM_COMMAND -> getText("actions.commands.type.customCommand");
        };
    }

    private void styleTable(JTable table) {
        HudTable.style(table);
        table.setBackground(AppTheme.HUD_BG);   // зазор intercellSpacing(0,2) рисуется фоном окна, без «линий» (§2)
        table.setRowHeight(44);
        table.setAutoCreateRowSorter(true);
        table.setDefaultRenderer(Object.class, new CellRenderer());
        table.putClientProperty(AppTheme.HUD_TABLE_STYLE_LOCKED, Boolean.TRUE);

        table.getColumnModel().getColumn(0).setPreferredWidth(220);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
    }

    private void installRowHover(JTable table) {
        table.putClientProperty(HudTable.HOVER_ROW_PROPERTY, -1);
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                setHoveredRow(table, rowUnderPoint(table, event.getPoint()));
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                setHoveredRow(table, -1);
            }
        });
    }

    private int rowUnderPoint(JTable table, Point point) {
        int row = table.rowAtPoint(point);
        if (row >= 0) {
            return row;
        }
        Point up = new Point(point.x, point.y - 2);
        row = table.rowAtPoint(up);
        if (row >= 0) {
            return row;
        }
        Point down = new Point(point.x, point.y + 2);
        return table.rowAtPoint(down);
    }

    private void setHoveredRow(JTable table, int row) {
        Object current = table.getClientProperty(HudTable.HOVER_ROW_PROPERTY);
        int currentRow = current instanceof Integer value ? value : -1;
        if (currentRow == row) {
            return;
        }
        table.putClientProperty(HudTable.HOVER_ROW_PROPERTY, row);
        repaintTableRow(table, currentRow);
        repaintTableRow(table, row);
    }

    private void repaintTableRow(JTable table, int row) {
        if (row < 0 || row >= table.getRowCount()) {
            return;
        }
        Rectangle rect = table.getCellRect(row, 0, true);
        rect.width = table.getWidth();
        table.repaint(rect);
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

    private static final class CellRenderer extends HudTable.CellRenderer {
        private final HudCommandNameCellRenderer commandNameRenderer = new HudCommandNameCellRenderer();

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            if (value instanceof CommandNameCell commandNameCell) {
                return commandNameRenderer.getTableCellRendererComponent(
                        table, commandNameCell, isSelected, hasFocus, row, column);
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    private record CommandNameCell(String name, String id) implements HudCommandNameCellRenderer.Value {
        @Override
        public String toString() {
            return name + " " + id;
        }
    }


    private final class SearchDocumentListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            initData();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            initData();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            initData();
        }
    }

    private final class CommandDetailsMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent event) {
            if (!SwingUtilities.isLeftMouseButton(event)) {
                return;
            }
            int row = table.rowAtPoint(event.getPoint());
            if (row >= 0) {
                openCommandDetailsAtViewRow(row);
            }
        }
    }
}
