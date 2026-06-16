package elite.intel.ui.render;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudTable;

import javax.swing.*;
import java.awt.*;

/**
 * Renders command table name cells as a cockpit-style two-line identity block.
 */
public final class HudCommandNameCellRenderer extends HudTable.CellRenderer {

    /**
     * Data contract for command identity cells that show a display name and technical action id.
     */
    public interface Value {
        String name();

        String id();
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
        JLabel fallback = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        if (!(value instanceof Value commandValue)) {
            return fallback;
        }

        JPanel cell = new JPanel();
        cell.setOpaque(true);
        cell.setBackground(fallback.getBackground());
        cell.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(nullToBlank(commandValue.name()));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        name.setForeground(isSelected ? AppTheme.SEL_FG : AppTheme.FG);
        name.setFont(fallback.getFont().deriveFont(Font.BOLD, fallback.getFont().getSize2D()));

        JLabel id = new JLabel(nullToBlank(commandValue.id()));
        id.setAlignmentX(Component.LEFT_ALIGNMENT);
        id.setForeground(isSelected ? AppTheme.SEL_FG : AppTheme.FG_MUTED);
        id.setFont(fallback.getFont().deriveFont(Font.PLAIN, Math.max(10f, fallback.getFont().getSize2D() - 3f)));

        cell.add(Box.createVerticalGlue());
        cell.add(name);
        cell.add(id);
        cell.add(Box.createVerticalGlue());
        return cell;
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
