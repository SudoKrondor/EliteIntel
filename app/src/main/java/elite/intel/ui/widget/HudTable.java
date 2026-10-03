package elite.intel.ui.widget;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

/**
 * Shared table styling helper and default renderers for cockpit/HUD tables.
 */
public final class HudTable {

    private HudTable() {
    }

    /**
     * Applies the standard HUD table styling without changing the table model.
     *
     * @param table table to style
     */
    public static void style(JTable table) {
        style(table, HudPalette.HUD_TABLE_ROW_HEIGHT, HudPalette.HUD_TABLE_HEADER_HEIGHT,
                HudPalette.HUD_FONT_TABLE_ROW, HudPalette.HUD_FONT_TABLE_HEADER, 5, 4);
    }

    /**
     * Sizes every column to the widest thing actually in it - its header or one of its cells - so nothing is
     * cut off and nobody has to drag a divider.
     * <p>
     * Measured through each column's own renderer rather than guessed in pixels, which is what makes it
     * survive localization: the same table is German and Russian too, and those run considerably longer than
     * the English the numbers would have been picked from.
     * <p>
     * Call it after the rows are in. Widths are a snapshot of the current content, so a table that reloads
     * calls this again.
     *
     * @param table   the table to size, already populated
     * @param maxWidth the widest any single column may become, so one long value - a full install path -
     *                 cannot push every other column off the screen
     */
    public static void fitColumnsToContent(JTable table, int maxWidth) {
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = headerWidth(table, column);
            for (int row = 0; row < table.getRowCount(); row++) {
                width = Math.max(width, cellWidth(table, row, column));
            }
            width = Math.min(width + COLUMN_PADDING, maxWidth);
            table.getColumnModel().getColumn(column).setPreferredWidth(width);
        }
    }

    private static int headerWidth(JTable table, int column) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        TableCellRenderer renderer = tableColumn.getHeaderRenderer();
        if (renderer == null) renderer = table.getTableHeader().getDefaultRenderer();
        Component rendered = renderer.getTableCellRendererComponent(
                table, tableColumn.getHeaderValue(), false, false, -1, column);
        return rendered.getPreferredSize().width;
    }

    private static int cellWidth(JTable table, int row, int column) {
        Component rendered = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
        return rendered.getPreferredSize().width;
    }

    /** Breathing room either side of the widest value, so text never touches the column divider. */
    private static final int COLUMN_PADDING = 18;

    /**
     * Applies compact HUD table styling for dense cockpit data panels.
     *
     * @param table table to style
     */
    public static void styleCompact(JTable table) {
        style(table, HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT, HudPalette.HUD_TABLE_HEADER_HEIGHT_COMPACT,
                HudPalette.HUD_FONT_SM, HudPalette.HUD_FONT_SM - 1f, 3, 2);
    }

    private static void style(
            JTable table,
            int rowHeight,
            int headerHeight,
            float fontSize,
            float headerFontSize,
            int headerVerticalPadding,
            int cellVerticalPadding
    ) {
        table.setFillsViewportHeight(true);
        table.setRowHeight(rowHeight);
        table.setFont(table.getFont().deriveFont(Font.PLAIN, fontSize));
        table.setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);   // table body = window colour; darker than HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND tile -> gap reads as dark slot (section 2)
        table.setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        table.setGridColor(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        table.setSelectionBackground(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
        table.setSelectionForeground(HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT);
        table.setShowGrid(false);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setIntercellSpacing(new Dimension(3, 3));
        table.setDefaultRenderer(Object.class, new CellRenderer(cellVerticalPadding));

        JTableHeader header = table.getTableHeader();
        header.setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        header.setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        header.setFont(header.getFont().deriveFont(Font.BOLD, headerFontSize));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, headerHeight));
        header.setDefaultRenderer(new HeaderRenderer(headerVerticalPadding));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION));
        // Belt-and-suspenders: FlatLaf also reads the client property per-component.
        header.putClientProperty("FlatLaf.style", String.format(
                "hoverBackground: #%06X; hoverForeground: #%06X; pressedBackground: #%06X; pressedForeground: #%06X",
                HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND.getRGB() & 0xFFFFFF,
                HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT.getRGB() & 0xFFFFFF,
                HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND.getRGB() & 0xFFFFFF,
                HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT.getRGB() & 0xFFFFFF));
    }

    /**
     * Creates the standard data-plane scroll pane for a HUD table.
     *
     * @param table table to wrap
     */
    public static JScrollPane scrollPane(JTable table) {
        return dataPlaneScrollPane(table);
    }

    /**
     * Scroll pane for a HUD data table: warm HUD_COLOR_ROLE_APPLICATION_BACKGROUND viewport (matches the row
     * gap colour, no cold cant around/below rows) and a data-plane frame.
     * Marked HUD_SCROLL_STYLE_LOCKED so applyDarkPalette will not reset it to the
     * cold HUD_COLOR_ROLE_PANEL_BACKGROUND viewport. Use this for table panels instead of
     * scrollPane(JTable) + manual restore-after-palette (ED_HUD_REFERENCE section 8.6).
     *
     * @param table table to wrap
     */
    public static JScrollPane dataPlaneScrollPane(JTable table) {
        JScrollPane scrollPane = new HudScrollPane(table);          // ctor runs styleScrollPane (cold)
        scrollPane.getViewport().setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);    // override to warm
        scrollPane.setBorder(AppTheme.hudDataPlaneBorder());
        scrollPane.putClientProperty(AppTheme.HUD_SCROLL_STYLE_LOCKED, Boolean.TRUE);
        return scrollPane;
    }

    /**
     * Standard HUD table header renderer.
     */
    public static class HeaderRenderer extends DefaultTableCellRenderer {
        private final int verticalPadding;

        public HeaderRenderer() {
            this(5);
        }

        public HeaderRenderer(int verticalPadding) {
            this.verticalPadding = verticalPadding;
            setOpaque(true);
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
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
            label.setForeground(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
            label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D()));
            label.setBorder(new EmptyBorder(verticalPadding, 8, verticalPadding, 8));
            label.setHorizontalAlignment(SwingConstants.LEFT);
            return label;
        }
    }

    /**
     * Optional client property on a JTable: Integer index of the row currently
     * under the mouse, or -1 / absent for none. When present, the default
     * CellRenderer paints that row with HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND. Panels that want
     * row hover install a MouseMotionListener that maintains this property.
     */
    public static final String HOVER_ROW_PROPERTY = "elite.intel.hud.table.hoverRow";

    /**
     * Standard alternating-row HUD table cell renderer.
     */
    public static class CellRenderer extends DefaultTableCellRenderer {
        private final int verticalPadding;

        public CellRenderer() {
            this(4);
        }

        public CellRenderer(int verticalPadding) {
            this.verticalPadding = verticalPadding;
            setOpaque(true);
        }

        /** Returns the vertical cell padding used in the border, available to subclasses. */
        protected int getVerticalPadding() {
            return verticalPadding;
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
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                label.setBackground(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
                label.setForeground(HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT);
            } else {
                Object hoveredObj = table.getClientProperty(HOVER_ROW_PROPERTY);
                boolean hovered = hoveredObj instanceof Integer h && h == row;
                label.setBackground(hovered ? HudPalette.HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND : HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
                label.setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
            }
            label.setBorder(new EmptyBorder(verticalPadding, 8, verticalPadding, 8));
            label.setHorizontalAlignment(SwingConstants.LEFT);
            return label;
        }
    }

    /**
     * Extends {@link CellRenderer} with configurable upper-casing, foreground colour, and horizontal
     * alignment. Use for columns where the value is an identifier (pass {@link HudPalette#HUD_COLOR_ROLE_PRIMARY_TEXT}),
     * a numeric (pass {@link SwingConstants#RIGHT}), or needs explicit caps without global impact.
     *
     * @param fg        non-selected foreground; {@code null} keeps the default {@link HudPalette#HUD_COLOR_ROLE_PRIMARY_ACTION}
     * @param alignment {@link SwingConstants#LEFT} or {@link SwingConstants#RIGHT}
     */
    public static class ValueCellRenderer extends CellRenderer {
        private final java.awt.Color fg;
        private final int alignment;

        /** Creates a renderer with caps, default HUD_COLOR_ROLE_PRIMARY_ACTION foreground, and LEFT alignment. */
        public ValueCellRenderer() {
            this(null, SwingConstants.LEFT);
        }

        public ValueCellRenderer(java.awt.Color fg, int alignment) {
            super();
            this.fg = fg;
            this.alignment = alignment;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Object display = value == null ? "" : value.toString().toUpperCase(java.util.Locale.ROOT);
            Component c = super.getTableCellRendererComponent(
                    table, display, isSelected, hasFocus, row, column);
            if (!isSelected && fg != null) {
                c.setForeground(fg);
            }
            if (c instanceof JLabel l) {
                l.setHorizontalAlignment(alignment);
            }
            return c;
        }
    }
}
