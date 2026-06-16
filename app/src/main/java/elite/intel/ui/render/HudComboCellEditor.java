package elite.intel.ui.render;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudComboBox;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

/**
 * HUD table cell editor backed by a {@link HudComboBox}.
 *
 * <p>Overrides {@link #getTableCellEditorComponent} to lock the combo box background
 * to {@link AppTheme#HUD_TABLE_ROW} regardless of the row selection state, keeping the
 * warm cockpit colour consistent with the surrounding data rows (§3).
 *
 * @param <E> option type
 */
public class HudComboCellEditor<E> extends DefaultCellEditor {

    /**
     * Creates a cell editor wrapping the supplied HUD combo box.
     *
     * @param combo pre-configured HudComboBox; its model determines available options
     */
    public HudComboCellEditor(HudComboBox<E> combo) {
        super(combo);
    }

    @Override
    public Component getTableCellEditorComponent(
            JTable table, Object value, boolean isSelected, int row, int col) {
        Component c = super.getTableCellEditorComponent(table, value, isSelected, row, col);
        c.setBackground(HudPalette.HUD_TABLE_ROW); // §3: input field stays warm on any row state
        c.setForeground(HudPalette.FG);
        return c;
    }
}
