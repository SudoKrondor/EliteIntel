package elite.intel.ui.render;

import elite.intel.ai.hands.BindingDisplayNames;
import elite.intel.ui.support.BindingsGroupTableFactory;
import elite.intel.ui.widget.HudTable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Predicate;

import static elite.intel.ui.theme.HudPalette.*;

/**
 * Table renderer for binding rows.
 * <p>
 * The comparison uses the localized "Not defined" text because the table model
 * stores already formatted display values, not raw slot objects.
 * <p>
 * Column 0 holds the raw .binds XML tag - every click, selection and conflict lookup reads the
 * binding id straight out of the model - but it is <em>drawn</em> as the control's in-game name
 * ({@link BindingDisplayNames}), so the table can be read against the game's own control screen
 * without the model ever carrying a display string.
 * <p>
 * Column 0 is also tinted by the binding's status: red when the binding is in a
 * conflict ({@code hasConflict}), cyan when it carries a soft ship/SRV-twin recommendation
 * ({@code hasRecommendation}), green otherwise. A real conflict outranks a recommendation.
 */
public class BindingSlotCellRenderer extends HudTable.CellRenderer {
    private final Predicate<String> hasConflict;
    private final Predicate<String> hasRecommendation;

    public BindingSlotCellRenderer(Predicate<String> hasConflict, Predicate<String> hasRecommendation) {
        super(2);
        this.hasConflict = hasConflict;
        this.hasRecommendation = hasRecommendation;
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
        if (column == 0) {
            String bindingId = String.valueOf(value);
            label.setText(BindingDisplayNames.label(bindingId));
            label.setToolTipText(bindingId);
        }
        boolean notDefined = elite.intel.ui.i18n.MultiLingualTextProvider
                .getText("bindings.status.notDefined")
                .equals(value);
        if (!isSelected) {
            Object hoveredObj = table.getClientProperty(BindingsGroupTableFactory.HOVER_ROW_PROPERTY);
            boolean hovered = hoveredObj instanceof Integer h && h == row;
            label.setBackground(hovered ? HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND : HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
            if (column == 0) {
                // Action name carries the conflict status for the whole row.
                label.setForeground(statusColor(String.valueOf(value)));
            } else {
                label.setForeground(notDefined ? HUD_COLOR_ROLE_DISABLED : HUD_COLOR_ROLE_PRIMARY_ACTION);
            }
        }
        return label;
    }

    private Color statusColor(String bindingId) {
        if (hasConflict != null && hasConflict.test(bindingId)) {
            return HUD_COLOR_ROLE_DANGER;
        }
        if (hasRecommendation != null && hasRecommendation.test(bindingId)) {
            return HUD_COLOR_ROLE_INFORMATION;
        }
        return HUD_COLOR_ROLE_SUCCESS;
    }
}
