package elite.intel.ui.render;

import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudTable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.function.BooleanSupplier;

/**
 * HUD table header renderer for Boolean selector columns.
 * Paints the standard HUD header background ({@link HudPalette#HUD_COLOR_ROLE_APPLICATION_BACKGROUND}) and a
 * checkbox marker centred in the header cell: an outline, or an accent fill once every row is selected.
 * No bottom border is painted here - {@link HudTable#style} applies a
 * {@code MatteBorder(0,0,1,0, HUD_COLOR_ROLE_CONTROL_DECORATION)} on the entire {@link JTableHeader},
 * which already covers this column. Adding another border here would double the line.
 */
public class HudCheckBoxHeaderRenderer extends JComponent implements TableCellRenderer {

    private final BooleanSupplier allSelectedQuery;
    private boolean filled;

    public HudCheckBoxHeaderRenderer(BooleanSupplier allSelectedQuery) {
        this.allSelectedQuery = allSelectedQuery;
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        filled = allSelectedQuery.getAsBoolean();
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
            g2.fillRect(0, 0, w, h);
            int size = HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT - 2 * HudPalette.HUD_PADDING_SMALL;
            int x = (w - size) / 2;
            int y = (h - size) / 2;
            HudGlyphs.paintHudCheckMarker(g2, x, y, size,
                    filled ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION : HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION,
                    HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT, filled);
        } finally {
            g2.dispose();
        }
    }
}
