package elite.intel.ui.support;

import elite.intel.ai.hands.BindingSlotType;
import elite.intel.ui.render.BindingSlotCellRenderer;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.BindingConflictPopup;
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
import java.util.function.Function;
import java.util.function.Predicate;

import static elite.intel.ui.theme.AppTheme.HUD_SCROLL_STYLE_LOCKED;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND;

public class BindingsGroupTableFactory {
    public static final int TABLE_ROW_HEIGHT = HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT;
    public static final String HOVER_ROW_PROPERTY = "elite.intel.bindings.hoverRow";
    private static final String ROW_ACTION_PROPERTY = "elite.intel.bindings.rowAction";
    private static final Border TABLE_SECTION_BORDER = BorderFactory.createEmptyBorder(1, 0, 0, 0);

    /**
     * The per-row action offered in a table's trailing column: the Missing view offers AUTO_FIX,
     * the Used view offers CLEAR (of the keyboard binding only), and other tables offer none.
     */
    public enum RowAction {NONE, AUTO_FIX, CLEAR}

    private final BindingsSelectionController selectionController;
    private final BiConsumer<String, BindingSlotType> slotClickHandler;
    private final Consumer<String> autoFixHandler;
    private final Consumer<String> clearHandler;
    private final Predicate<String> hasConflict;
    private final Predicate<String> hasRecommendation;
    /**
     * Builds the hover-callout content for a conflicting binding id, or returns {@code null} if it has none.
     */
    private final Function<String, JComponent> conflictPopupContent;
    /**
     * Shared across all this factory's tables so only one callout is ever visible.
     */
    private final BindingConflictPopup conflictPopup = new BindingConflictPopup();

    public BindingsGroupTableFactory(
            BindingsSelectionController selectionController,
            BiConsumer<String, BindingSlotType> slotClickHandler,
            Consumer<String> autoFixHandler,
            Consumer<String> clearHandler,
            Predicate<String> hasConflict,
            Predicate<String> hasRecommendation,
            Function<String, JComponent> conflictPopupContent
    ) {
        this.selectionController = selectionController;
        this.slotClickHandler = slotClickHandler;
        this.autoFixHandler = autoFixHandler;
        this.clearHandler = clearHandler;
        this.hasConflict = hasConflict;
        this.hasRecommendation = hasRecommendation;
        this.conflictPopupContent = conflictPopupContent;
    }

    /**
     * Dismisses any visible conflict callout - call before the tables are rebuilt.
     */
    public void hideConflictPopup() {
        conflictPopup.hide();
    }

    /**
     * The trailing column that carries this table's per-row action, or {@code -1} when the table has
     * none. The action kind is stamped on the table in {@link #groupTable} and read back here so the
     * click and hover logic stay agnostic to which action (auto fix vs. clear) the column represents.
     */
    private int actionColumn(JTable table) {
        return rowAction(table) == RowAction.NONE ? -1 : table.getColumnCount() - 1;
    }

    private RowAction rowAction(JTable table) {
        Object value = table.getClientProperty(ROW_ACTION_PROPERTY);
        return value instanceof RowAction action ? action : RowAction.NONE;
    }

    public JScrollPane groupTable(
            List<Object[]> rows, JScrollPane outerScrollPane, RowAction rowAction, String... columnNames) {
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
        table.putClientProperty(ROW_ACTION_PROPERTY, rowAction);
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

                if (column == actionColumn(table)) {
                    String bindingId = selectionController.selectRow(table, row);
                    dispatchRowAction(table, bindingId);
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

    private void dispatchRowAction(JTable table, String bindingId) {
        switch (rowAction(table)) {
            case AUTO_FIX -> {
                if (autoFixHandler != null) {
                    autoFixHandler.accept(bindingId);
                }
            }
            case CLEAR -> {
                if (clearHandler != null) {
                    clearHandler.accept(bindingId);
                }
            }
            case NONE -> {
            }
        }
    }

    private void highlightDataRowsOnHover(JTable table) {
        table.putClientProperty(HOVER_ROW_PROPERTY, -1);
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                boolean clickable = isClickableCell(table, event.getPoint());
                table.setCursor(clickable ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                int row = hoveredRowAt(table, event.getPoint());
                setHoveredRow(table, row);
                updateConflictPopup(table, row, event);
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                table.setCursor(Cursor.getDefaultCursor());
                setHoveredRow(table, -1);
                conflictPopup.hide();
            }
        });
    }

    /**
     * Shows the conflict callout next to the pointer when it rests on a conflicting row, and hides it
     * otherwise. The callout is offset down-right of the pointer so it never sits under the cursor
     * (which would stop {@code mouseMoved} from firing and leave it stuck on screen).
     */
    private void updateConflictPopup(JTable table, int row, MouseEvent event) {
        if (conflictPopupContent == null || row < 0 || row >= table.getRowCount()) {
            conflictPopup.hide();
            return;
        }
        String bindingId = String.valueOf(table.getValueAt(row, 0));
        JComponent content = conflictPopupContent.apply(bindingId);
        if (content == null) {
            conflictPopup.hide();
            return;
        }
        conflictPopup.show(table, event.getXOnScreen() + 16, event.getYOnScreen() + 18, bindingId, content);
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
        return slotTypeForColumn(column) != null || column == actionColumn(table);
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
        table.setDefaultRenderer(Object.class, new BindingSlotCellRenderer(hasConflict, hasRecommendation));
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
