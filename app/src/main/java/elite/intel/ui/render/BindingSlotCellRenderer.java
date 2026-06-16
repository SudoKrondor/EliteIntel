package elite.intel.ui.render;

import elite.intel.ui.support.BindingsGroupTableFactory;
import elite.intel.ui.widget.HudTable;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.theme.AppTheme.ACCENT;
import static elite.intel.ui.theme.AppTheme.HUD_DISABLED;
import static elite.intel.ui.theme.AppTheme.HUD_TABLE_ROW;
import static elite.intel.ui.theme.AppTheme.HUD_TABLE_ROW_HOVER;

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
            label.setBackground(hovered ? HUD_TABLE_ROW_HOVER : HUD_TABLE_ROW);
            label.setForeground(notDefined ? HUD_DISABLED : ACCENT);
        }
        return label;
    }
}
