package elite.intel.ui.support;

import elite.intel.bindforge.model.BindingSlotType;
import elite.intel.ui.render.BindingSlotCellRenderer;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.BindingConflictPopup;
import elite.intel.ui.widget.HudTable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
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
    /**
     * Primary's Device and Input columns, then Secondary's: each slot is two columns, the device apart from the input on
     * it (Alan, 2026-11-22).
     */
    private static final int PRIMARY_DEVICE_COLUMN = 1;
    private static final int PRIMARY_INPUT_COLUMN = 2;
    private static final int SECONDARY_DEVICE_COLUMN = 3;
    private static final int SECONDARY_INPUT_COLUMN = 4;
    /**
     * The AI ACCESS column, between Secondary and the row action.
     */
    public static final int AI_ACCESS_COLUMN = 5;
    /**
     * AI access is sized to "Vega requires a keyboard bind", the widest of its labels in any language, at the row size
     * (2026-11-24). Binding gave up 20 for it; the names have room since their group moved to a sub-heading.
     */
    private static final int BINDING_COLUMN_WIDTH = 300;
    private static final int AI_ACCESS_COLUMN_WIDTH = 230;
    private static final int DEVICE_COLUMN_WIDTH = 110;
    private static final int INPUT_COLUMN_WIDTH = 160;
    private static final int ACTION_COLUMN_WIDTH = 110;
    private static final Border TABLE_SECTION_BORDER = BorderFactory.createEmptyBorder(1, 0, 0, 0);

    /**
     * The action a row offers in the table's trailing column: CLEAR (of the keyboard binding only) on a row with a
     * keyboard key, AUTO_FIX on a row without one, NONE where neither applies.
     */
    public enum RowAction {NONE, AUTO_FIX, CLEAR}

    /**
     * A row's trailing cell: what it does when pressed, and the text drawn for that. Each row carries its own, because one
     * table holds rows that offer different actions.
     */
    public record RowActionCell(RowAction action, String text) {
        @Override
        public String toString() {
            return text;
        }
    }

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
     * What pressing this cell does: the row's action if the cell is its {@link RowActionCell}, else {@code NONE}.
     */
    private RowAction actionAt(JTable table, int row, int column) {
        return table.getValueAt(row, column) instanceof RowActionCell cell ? cell.action() : RowAction.NONE;
    }

    /**
     * One table of rows. {@code withColumnNames} draws the column headings; the grid's group tables pass false, and a
     * table of no rows with them is pinned above the grid, so the groups line up under the one row of headings.
     */
    public JScrollPane groupTable(List<Object[]> rows, JScrollPane outerScrollPane, boolean withColumnNames,
            String... columnNames) {
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
        int headerHeight = table.getTableHeader().getPreferredSize().height;
        if (!withColumnNames) {
            table.setTableHeader(null);
            headerHeight = 0;
        }

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

        int height = table.getRowHeight() * rows.size() + headerHeight + 3;
        scrollPane.setPreferredSize(new Dimension(0, height));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return scrollPane;
    }

    /**
     * The headings pinned above the grid: a table of no rows, built as the grid's tables are so its columns take the
     * same widths, in one band of two lines (Alan, 2026-11-22). Binding, AI access and the action are centred in the
     * band; {@code primary} and {@code secondary} sit in its upper half, each over its two columns with a rule under it,
     * and the smaller Device and Input titles in its lower half.
     */
    public JComponent pinnedHeading(JScrollPane outerScrollPane, String primary, String secondary, String... columnNames) {
        JScrollPane titles = groupTable(List.of(), outerScrollPane, true, columnNames);
        JTable table = (JTable) titles.getViewport().getView();
        JTableHeader plain = table.getTableHeader();
        SpannedHeader header = new SpannedHeader(table.getColumnModel(), List.of(
                new Span(primary, PRIMARY_DEVICE_COLUMN, PRIMARY_INPUT_COLUMN),
                new Span(secondary, SECONDARY_DEVICE_COLUMN, SECONDARY_INPUT_COLUMN)));
        header.setBackground(plain.getBackground());
        header.setForeground(plain.getForeground());
        header.setFont(plain.getFont());
        header.setBorder(plain.getBorder());
        header.setDefaultRenderer(plain.getDefaultRenderer());
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        table.setTableHeader(header);
        titles.setColumnHeaderView(header);
        forwardMouseWheelToOuterScrollPane(header, outerScrollPane);

        int height = header.getPreferredSize().height + 3;
        titles.setPreferredSize(new Dimension(0, height));
        titles.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return titles;
    }

    /**
     * A title over a run of columns, from {@code first} to {@code last}.
     */
    private record Span(String title, int first, int last) {
    }

    /**
     * The pinned titles' header: two lines tall, with each span's title drawn in the upper half over its columns and a
     * rule under it as wide as they are, so it reads as belonging to both. The cells' own titles are drawn first - the
     * Device and Input ones in the lower half, the rest centred (see {@link GroupTableHeaderRenderer}).
     */
    private static final class SpannedHeader extends JTableHeader {
        private static final int TOP_PADDING = 4;
        private static final int RULE_GAP = 2;
        private static final int RULE_INSET = 8;

        private final List<Span> spans;

        private SpannedHeader(TableColumnModel columns, List<Span> spans) {
            super(columns);
            this.spans = spans;
        }

        private Font spanFont() {
            return getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_BIND_GRID_SLOT_TITLE);
        }

        private Font subtitleFont() {
            return getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_BIND_GRID_SUBTITLE);
        }

        /**
         * The span line, its rule, and the Device and Input line under it, with the cell's own bottom padding.
         */
        @Override
        public Dimension getPreferredSize() {
            Dimension plain = super.getPreferredSize();
            int band = TOP_PADDING + getFontMetrics(spanFont()).getHeight() + RULE_GAP + 1 + RULE_GAP
                    + getFontMetrics(subtitleFont()).getHeight() + GroupTableHeaderRenderer.BOTTOM_PADDING;
            return new Dimension(plain.width, Math.max(plain.height, band));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setFont(spanFont());
                FontMetrics metrics = g.getFontMetrics();
                int ruleY = TOP_PADDING + metrics.getHeight() + RULE_GAP;
                for (Span span : spans) {
                    if (span.last() >= getColumnModel().getColumnCount())
                        continue;
                    int left = getHeaderRect(span.first()).x;
                    Rectangle last = getHeaderRect(span.last());
                    int right = last.x + last.width;
                    int textX = left + (right - left - metrics.stringWidth(span.title())) / 2;
                    g.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
                    g.drawString(span.title(), textX, TOP_PADDING + metrics.getAscent());
                    g.setColor(HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION);
                    g.drawLine(left + RULE_INSET, ruleY, right - RULE_INSET, ruleY);
                }
            } finally {
                g.dispose();
            }
        }
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

                RowAction action = actionAt(table, row, column);
                if (action != RowAction.NONE) {
                    dispatchRowAction(action, selectionController.selectRow(table, row));
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

    private void dispatchRowAction(RowAction action, String bindingId) {
        switch (action) {
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
        return slotTypeForColumn(column) != null || (column >= 0 && actionAt(table, row, column) != RowAction.NONE);
    }

    /**
     * The slot a column belongs to: either half of Primary or Secondary opens that slot's capture.
     */
    private BindingSlotType slotTypeForColumn(int column) {
        return switch (column) {
            case PRIMARY_DEVICE_COLUMN, PRIMARY_INPUT_COLUMN -> BindingSlotType.PRIMARY;
            case SECONDARY_DEVICE_COLUMN, SECONDARY_INPUT_COLUMN -> BindingSlotType.SECONDARY;
            default -> null;
        };
    }

    /**
     * Whether a column is one of the slots' Device or Input columns, which read left to right like the Binding column.
     */
    public static boolean isSlotColumn(int column) {
        return column >= PRIMARY_DEVICE_COLUMN && column <= SECONDARY_INPUT_COLUMN;
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
        table.setFont(table.getFont().deriveFont(Font.PLAIN, HudPalette.HUD_FONT_BIND_GRID_ROW));
        table.setAutoCreateRowSorter(false);
        table.getTableHeader().setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        table.getTableHeader().setDefaultRenderer(new GroupTableHeaderRenderer());
        // The compact style fixes the header's height for its small titles; these are larger, so it takes their height.
        table.getTableHeader().setPreferredSize(null);
        // Fixed widths: the pinned titles and every group's table are separate tables, so a dragged heading moved only
        // itself (Alan, 2026-11-22).
        table.getTableHeader().setResizingAllowed(false);
        table.setDefaultRenderer(Object.class, new BindingSlotCellRenderer(hasConflict, hasRecommendation));
    }

    private void configureColumnWidths(JTable table) {
        if (table.getColumnCount() <= AI_ACCESS_COLUMN)
            return;

        table.getColumnModel().getColumn(0).setPreferredWidth(BINDING_COLUMN_WIDTH);
        for (int column = PRIMARY_DEVICE_COLUMN; column <= SECONDARY_INPUT_COLUMN; column++) {
            boolean device = column == PRIMARY_DEVICE_COLUMN || column == SECONDARY_DEVICE_COLUMN;
            table.getColumnModel().getColumn(column).setPreferredWidth(device ? DEVICE_COLUMN_WIDTH : INPUT_COLUMN_WIDTH);
        }
        for (int column = AI_ACCESS_COLUMN; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(column == AI_ACCESS_COLUMN ? AI_ACCESS_COLUMN_WIDTH : ACTION_COLUMN_WIDTH);
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
        private static final int BOTTOM_PADDING = 7;
        private static final Border HEADER_CELL_BORDER = new EmptyBorder(4, 8, BOTTOM_PADDING, 8);

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
            // Device and Input are smaller and sit at the foot of the band, under their slot's title; the rest are centred
            // top to bottom (Alan, 2026-11-22).
            boolean slot = isSlotColumn(column);
            label.setFont(label.getFont().deriveFont(Font.BOLD,
                    slot ? HudPalette.HUD_FONT_BIND_GRID_SUBTITLE : HudPalette.HUD_FONT_BIND_GRID_COLUMN_TITLE));
            label.setVerticalAlignment(slot ? SwingConstants.BOTTOM : SwingConstants.CENTER);
            // Binding left, over the names; the other titles centred over their columns.
            label.setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
            return label;
        }
    }
}
