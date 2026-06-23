package elite.intel.ui.support;

import elite.intel.ai.hands.BindingSlotType;
import elite.intel.ui.render.BindingSlotCellRenderer;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudTable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static elite.intel.ui.theme.AppTheme.HUD_SCROLL_STYLE_LOCKED;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND;

public class BindingsGroupTableFactory {
    public static final int TABLE_ROW_HEIGHT = HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT;
    public static final String HOVER_ROW_PROPERTY = "elite.intel.bindings.hoverRow";
    private static final Border TABLE_SECTION_BORDER = BorderFactory.createEmptyBorder(1, 0, 0, 0);

    private final BindingsSelectionController selectionController;
    private final BiConsumer<String, BindingSlotType> slotClickHandler;
    private final Consumer<String> autoFixHandler;

    public BindingsGroupTableFactory(
            BindingsSelectionController selectionController,
            BiConsumer<String, BindingSlotType> slotClickHandler,
            Consumer<String> autoFixHandler
    ) {
        this.selectionController = selectionController;
        this.slotClickHandler = slotClickHandler;
        this.autoFixHandler = autoFixHandler;
    }

    /**
     * Tables with a fourth column expose a per-row "auto fix" action in that
     * column; three-column tables (the Used view) have none.
     */
    private int autoFixColumn(JTable table) {
        return autoFixHandler != null && table.getColumnCount() == 4 ? 3 : -1;
    }

    public JScrollPane groupTable(List<Object[]> rows, JScrollPane outerScrollPane, String... columnNames) {
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (Object[] row : rows) {
            model.addRow(row);
        }

        JTable table = new JTable(model);
        styleGroupTable(table);
        configureColumnWidths(table);
        selectionController.register(table);
        openDialogOnDataCellPress(table);
        highlightDataRowsOnHover(table);
        table.setPreferredScrollableViewportSize(new Dimension(0, table.getRowHeight() * Math.max(1, rows.size())));
        forwardMouseWheelToOuterScrollPane(table, outerScrollPane);
        forwardMouseWheelToOuterScrollPane(table.getTableHeader(), outerScrollPane);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setWheelScrollingEnabled(false);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.getViewport().setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        scrollPane.setBorder(TABLE_SECTION_BORDER);
        scrollPane.putClientProperty(HUD_SCROLL_STYLE_LOCKED, Boolean.TRUE);
        forwardMouseWheelToOuterScrollPane(scrollPane, outerScrollPane);
        forwardMouseWheelToOuterScrollPane(scrollPane.getViewport(), outerScrollPane);

        int height = table.getRowHeight() * rows.size() + table.getTableHeader().getPreferredSize().height + 3;
        scrollPane.setPreferredSize(new Dimension(0, height));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return scrollPane;
    }

    private void openDialogOnDataCellPress(JTable table) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }

                int row = table.rowAtPoint(event.getPoint());
                if (row < 0) {
                    return;
                }
                int column = table.columnAtPoint(event.getPoint());

                if (column == autoFixColumn(table)) {
                    String bindingId = selectionController.selectRow(table, row);
                    autoFixHandler.accept(bindingId);
                    return;
                }

                BindingSlotType slotType = slotTypeForColumn(column);
                if (slotType == null) {
                    return;
                }
                String bindingId = selectionController.selectRow(table, row);
                slotClickHandler.accept(bindingId, slotType);
            }
        });
    }

    private void highlightDataRowsOnHover(JTable table) {
        table.putClientProperty(HOVER_ROW_PROPERTY, -1);
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                boolean clickable = isClickableCell(table, event.getPoint());
                table.setCursor(clickable ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                setHoveredRow(table, hoveredRowAt(table, event.getPoint()));
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                table.setCursor(Cursor.getDefaultCursor());
                setHoveredRow(table, -1);
            }
        });
    }

    private int hoveredRowAt(JTable table, Point point) {
        int row = table.rowAtPoint(point);
        if (row >= 0) {
            return row;
        }
        // Point landed in intercellSpacing gap - rowAtPoint returns -1.
        // Probe adjacent rows within +/-3px (sufficient for a 2px gap).
        for (int dy = 1; dy <= 3; dy++) {
            int up = table.rowAtPoint(new Point(point.x, point.y - dy));
            if (up >= 0) {
                return up;
            }
            int down = table.rowAtPoint(new Point(point.x, point.y + dy));
            if (down >= 0) {
                return down;
            }
        }
        return -1;
    }

    private boolean isClickableCell(JTable table, Point point) {
        int row = table.rowAtPoint(point);
        if (row < 0) {
            return false;
        }
        int column = table.columnAtPoint(point);
        return slotTypeForColumn(column) != null || column == autoFixColumn(table);
    }

    private BindingSlotType slotTypeForColumn(int column) {
        return switch (column) {
            case 1 -> BindingSlotType.PRIMARY;
            case 2 -> BindingSlotType.SECONDARY;
            default -> null;
        };
    }

    private void setHoveredRow(JTable table, int row) {
        Object currentValue = table.getClientProperty(HOVER_ROW_PROPERTY);
        int currentRow = currentValue instanceof Integer value ? value : -1;
        if (currentRow == row) {
            return;
        }

        table.putClientProperty(HOVER_ROW_PROPERTY, row);
        repaintRow(table, currentRow);
        repaintRow(table, row);
    }

    private void repaintRow(JTable table, int row) {
        if (row < 0 || row >= table.getRowCount()) {
            return;
        }

        Rectangle rowBounds = table.getCellRect(row, 0, true);
        for (int column = 1; column < table.getColumnCount(); column++) {
            rowBounds = rowBounds.union(table.getCellRect(row, column, true));
        }
        table.repaint(rowBounds);
    }

    private void styleGroupTable(JTable table) {
        table.setFillsViewportHeight(false);
        HudTable.styleCompact(table);
        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setAutoCreateRowSorter(false);
        table.getTableHeader().setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        table.getTableHeader().setDefaultRenderer(new GroupTableHeaderRenderer());
        table.setDefaultRenderer(Object.class, new BindingSlotCellRenderer());
    }

    private void configureColumnWidths(JTable table) {
        if (table.getColumnCount() < 3)
            return;

        table.getColumnModel().getColumn(0).setPreferredWidth(320);
        table.getColumnModel().getColumn(1).setPreferredWidth(270);
        table.getColumnModel().getColumn(2).setPreferredWidth(270);
        if (table.getColumnCount() > 3) {
            table.getColumnModel().getColumn(3).setPreferredWidth(180);
        }
    }

    private void forwardMouseWheelToOuterScrollPane(JComponent source, JScrollPane outerScrollPane) {
        source.addMouseWheelListener(event -> {
            Point point = SwingUtilities.convertPoint(source, event.getPoint(), outerScrollPane);
            MouseWheelEvent converted = new MouseWheelEvent(
                    outerScrollPane,
                    event.getID(),
                    event.getWhen(),
                    event.getModifiersEx(),
                    point.x,
                    point.y,
                    event.getXOnScreen(),
                    event.getYOnScreen(),
                    event.getClickCount(),
                    event.isPopupTrigger(),
                    event.getScrollType(),
                    event.getScrollAmount(),
                    event.getWheelRotation(),
                    event.getPreciseWheelRotation());
            outerScrollPane.dispatchEvent(converted);
            event.consume();
        });
    }

    private static class GroupTableHeaderRenderer extends HudTable.HeaderRenderer {
        private static final Border HEADER_CELL_BORDER = new EmptyBorder(4, 8, 7, 8);

        private GroupTableHeaderRenderer() {
            super(3);
            setBorder(HEADER_CELL_BORDER);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);
            label.setBorder(HEADER_CELL_BORDER);
            label.setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
            return label;
        }
    }
}
