package elite.intel.ui.support;


import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps a single selected binding row across multiple grouped tables.
 * <p>
 * The Bindings tab renders many independent JTable instances, so selection
 * cannot rely on one shared table model.
 */
public class BindingsSelectionController {
    private final List<JTable> tables = new ArrayList<>();
    private boolean clearingSelection;
    private SelectedBinding selectedBinding;

    void register(JTable table) {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tables.add(table);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || clearingSelection)
                return;

            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                if (selectedBinding != null && selectedBinding.table() == table) {
                    selectedBinding = null;
                }
                return;
            }

            selectRow(table, selectedRow);
        });
    }

    String selectRow(JTable table, int row) {
        clearingSelection = true;
        for (JTable other : tables) {
            if (other != table) {
                other.clearSelection();
            }
        }
        table.getSelectionModel().setSelectionInterval(row, row);
        clearingSelection = false;

        Object value = table.getValueAt(row, 0);
        String bindingId = value == null ? "" : value.toString();
        selectedBinding = new SelectedBinding(table);
        return bindingId;
    }

    public void clearSelection() {
        clearingSelection = true;
        for (JTable table : tables) {
            table.clearSelection();
        }
        clearingSelection = false;
        selectedBinding = null;
    }

    public void resetTables() {
        clearSelection();
        tables.clear();
    }

    private record SelectedBinding(JTable table) {
    }
}
