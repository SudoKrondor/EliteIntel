package elite.intel.ui.render;

import elite.intel.ui.support.BindingsGroupTableFactory;
import elite.intel.ui.widget.HudTable;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_DISABLED;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * Table renderer for binding rows.
 * <p>
 * The comparison uses the localized "Not defined" text because the table model
 * stores already formatted display values, not raw slot objects.
 */
public class BindingSlotCellRenderer extends HudTable.CellRenderer {
    public BindingSlotCellRenderer() {
        super(2);
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
        label.setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
        boolean notDefined = elite.intel.ui.i18n.MultiLingualTextProvider
                .getText("bindings.status.notDefined")
                .equals(value);
        if (!isSelected) {
            Object hoveredObj = table.getClientProperty(BindingsGroupTableFactory.HOVER_ROW_PROPERTY);
            boolean hovered = hoveredObj instanceof Integer h && h == row;
            label.setBackground(hovered ? HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND : HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
            label.setForeground(notDefined ? HUD_COLOR_ROLE_DISABLED : HUD_COLOR_ROLE_PRIMARY_ACTION);
        }
        return label;
    }
}
