package elite.intel.ui.render;
import static elite.intel.ui.theme.HudGlyphs.*;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.widget.HudTable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * HUD table cell renderer for Boolean selector columns.
 * Paints the row background matching {@link HudTable.CellRenderer} and centres a
 * HUD checkbox marker via {@link AppTheme#paintHudCheckMarker}.
 * Does not render text or an info-zone — intended for narrow selector columns only.
 */
public class HudBooleanCellRenderer extends JComponent implements TableCellRenderer {

    private boolean filled;
    private Color markerColor;
    private Color bgColor;

    public HudBooleanCellRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        filled = Boolean.TRUE.equals(value);

        if (isSelected) {
            bgColor = HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION;
            // Marker must remain legible on the HUD_COLOR_ROLE_PRIMARY_ACTION background; HUD_COLOR_ROLE_SELECTED_TEXT provides the contrast.
            markerColor = HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT;
        } else {
            Object hoveredObj = table.getClientProperty(HudTable.HOVER_ROW_PROPERTY);
            boolean hovered = hoveredObj instanceof Integer h && h == row;
            bgColor = hovered ? HudPalette.HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND : HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND;
            boolean editable = table.getModel().isCellEditable(row, column);
            markerColor = editable ? HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION : HudPalette.HUD_COLOR_ROLE_DISABLED;
        }

        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(bgColor != null ? bgColor : HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
            g2.fillRect(0, 0, w, h);
            int size = HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT - 2 * HudPalette.HUD_PADDING_SMALL;
            int x = (w - size) / 2;
            int y = (h - size) / 2;
            HudGlyphs.paintHudCheckMarker(g2, x, y, size,
                    markerColor != null ? markerColor : HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION, filled);
        } finally {
            g2.dispose();
        }
    }
}
