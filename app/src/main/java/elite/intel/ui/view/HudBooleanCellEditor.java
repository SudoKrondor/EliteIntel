package elite.intel.ui.view;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

/**
 * HUD table cell editor for Boolean selector columns.
 * Toggles the cell value on a single click and immediately commits via
 * {@link #stopCellEditing()}, so one click is sufficient to check or uncheck.
 * Renders the already-toggled state using {@link HudBooleanCellRenderer} to
 * avoid a visual flash to the native LAF checkbox.
 */
public class HudBooleanCellEditor extends AbstractCellEditor implements TableCellEditor {

    private boolean currentValue;
    private final HudBooleanCellRenderer renderer = new HudBooleanCellRenderer();

    @Override
    public Component getTableCellEditorComponent(
            JTable table, Object value, boolean isSelected, int row, int column) {
        currentValue = !Boolean.TRUE.equals(value);
        // Commit immediately so a single click is enough; invokeLater avoids re-entrant model update.
        SwingUtilities.invokeLater(this::stopCellEditing);
        return renderer.getTableCellRendererComponent(table, currentValue, isSelected, false, row, column);
    }

    @Override
    public Object getCellEditorValue() {
        return currentValue;
    }
}
