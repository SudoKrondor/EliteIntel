package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
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
        style(table, AppTheme.HUD_TABLE_ROW_HEIGHT, AppTheme.HUD_TABLE_HEADER_HEIGHT,
                AppTheme.HUD_FONT_TABLE_ROW, AppTheme.HUD_FONT_TABLE_HEADER, 5, 4);
    }

    /**
     * Applies compact HUD table styling for dense cockpit data panels.
     *
     * @param table table to style
     */
    public static void styleCompact(JTable table) {
        style(table, AppTheme.HUD_TABLE_ROW_HEIGHT_COMPACT, AppTheme.HUD_TABLE_HEADER_HEIGHT_COMPACT,
                AppTheme.HUD_FONT_SM, AppTheme.HUD_FONT_SM - 1f, 3, 2);
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
        table.setBackground(AppTheme.HUD_BG);   // table body = window colour; darker than HUD_TABLE_ROW tile → gap reads as dark slot (§2)
        table.setForeground(AppTheme.FG);
        table.setGridColor(AppTheme.HUD_BG);
        table.setSelectionBackground(AppTheme.ACCENT);
        table.setSelectionForeground(AppTheme.SEL_FG);
        table.setShowGrid(false);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setIntercellSpacing(new Dimension(3, 3));
        table.setDefaultRenderer(Object.class, new CellRenderer(cellVerticalPadding));

        JTableHeader header = table.getTableHeader();
        header.setBackground(AppTheme.HUD_BG);
        header.setForeground(AppTheme.FG);
        header.setFont(header.getFont().deriveFont(Font.BOLD, headerFontSize));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, headerHeight));
        header.setDefaultRenderer(new HeaderRenderer(headerVerticalPadding));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.HUD_ORANGE_SOFT));
        // Belt-and-suspenders: FlatLaf also reads the client property per-component.
        header.putClientProperty("FlatLaf.style", String.format(
                "hoverBackground: #%06X; hoverForeground: #%06X; pressedBackground: #%06X; pressedForeground: #%06X",
                AppTheme.HUD_BG.getRGB() & 0xFFFFFF,
                AppTheme.FG_MUTED.getRGB() & 0xFFFFFF,
                AppTheme.HUD_BG.getRGB() & 0xFFFFFF,
                AppTheme.FG_MUTED.getRGB() & 0xFFFFFF));
    }

    /**
     * Creates a scroll pane suitable for a HUD table.
     *
     * @param table table to wrap
     */
    public static JScrollPane scrollPane(JTable table) {
        JScrollPane scrollPane = new HudScrollPane(table);
        scrollPane.getViewport().setBackground(AppTheme.HUD_BG);
        return scrollPane;
    }

    /**
     * Scroll pane for a HUD data table: warm HUD_BG viewport (matches the row
     * gap colour, no cold cant around/below rows) and a data-plane frame.
     * Marked HUD_SCROLL_STYLE_LOCKED so applyDarkPalette will not reset it to the
     * cold HUD_PANEL_BG viewport. Use this for table panels instead of
     * scrollPane(JTable) + manual restore-after-palette (ED_HUD_REFERENCE §8.6).
     *
     * @param table table to wrap
     */
    public static JScrollPane dataPlaneScrollPane(JTable table) {
        JScrollPane scrollPane = new HudScrollPane(table);          // ctor runs styleScrollPane (cold)
        scrollPane.getViewport().setBackground(AppTheme.HUD_BG);    // override to warm
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
            label.setBackground(AppTheme.HUD_BG);
            label.setForeground(AppTheme.FG_MUTED);
            label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D()));
            label.setBorder(new EmptyBorder(verticalPadding, 8, verticalPadding, 8));
            label.setHorizontalAlignment(SwingConstants.LEFT);
            return label;
        }
    }

    /**
     * Optional client property on a JTable: Integer index of the row currently
     * under the mouse, or -1 / absent for none. When present, the default
     * CellRenderer paints that row with HUD_TABLE_ROW_HOVER. Panels that want
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
                label.setBackground(AppTheme.ACCENT);
                label.setForeground(AppTheme.SEL_FG);
            } else {
                Object hoveredObj = table.getClientProperty(HOVER_ROW_PROPERTY);
                boolean hovered = hoveredObj instanceof Integer h && h == row;
                label.setBackground(hovered ? AppTheme.HUD_TABLE_ROW_HOVER : AppTheme.HUD_TABLE_ROW);
                label.setForeground(AppTheme.ACCENT);
            }
            label.setBorder(new EmptyBorder(verticalPadding, 8, verticalPadding, 8));
            label.setHorizontalAlignment(SwingConstants.LEFT);
            return label;
        }
    }

    /**
     * Extends {@link CellRenderer} with configurable upper-casing, foreground colour, and horizontal
     * alignment. Use for columns where the value is an identifier (pass {@link AppTheme#FG}),
     * a numeric (pass {@link SwingConstants#RIGHT}), or needs explicit caps without global impact.
     *
     * @param fg        non-selected foreground; {@code null} keeps the default {@link AppTheme#ACCENT}
     * @param alignment {@link SwingConstants#LEFT} or {@link SwingConstants#RIGHT}
     */
    public static class ValueCellRenderer extends CellRenderer {
        private final java.awt.Color fg;
        private final int alignment;

        /** Creates a renderer with caps, default ACCENT foreground, and LEFT alignment. */
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
