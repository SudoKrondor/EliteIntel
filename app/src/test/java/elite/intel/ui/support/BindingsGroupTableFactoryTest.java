package elite.intel.ui.support;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A row's CLEAR or AUTO FIX runs on a click - and does nothing while it is off, as it is while BindForge is read only
 * (Storage S14b).
 */
class BindingsGroupTableFactoryTest {

    private static final String CONTROL = "ForwardKey";
    /** The row action's column: after the control, its two slots and AI ACCESS. */
    private static final int ACTION_COLUMN = 4;

    private final List<String> cleared = new ArrayList<>();
    private final BindingsGroupTableFactory factory = new BindingsGroupTableFactory(
            new BindingsSelectionController(), (id, slot) -> {
    }, id -> {
    }, cleared::add, id -> false, id -> false, id -> null);

    @Test
    void aClickOnClearRunsIt() {
        JTable table = table(new BindingsGroupTableFactory.RowActionCell(BindingsGroupTableFactory.RowAction.CLEAR, "CLEAR"));

        click(table);

        assertEquals(List.of(CONTROL), cleared);
        assertEquals(Cursor.HAND_CURSOR, cursorOver(table));
    }

    @Test
    void aClickOnAnActionThatIsOffDoesNothing() {
        JTable table = table(new OffCell("CLEAR", "Off while BindForge is read only"));

        click(table);

        assertTrue(cleared.isEmpty(), "a CLEAR that is off ran");
        assertEquals(Cursor.DEFAULT_CURSOR, cursorOver(table), "an action that is off offered a hand");
    }

    @Test
    void anActionThatIsOffSaysWhyAndReadsAsItsWord() {
        OffCell off = new OffCell("CLEAR", "Off while BindForge is read only");

        assertEquals("CLEAR", off.toString());
        assertEquals("Off while BindForge is read only", off.tooltip());
    }

    private JTable table(Object action) {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{CONTROL, "W", "-", "", action});
        JScrollPane pane = factory.groupTable(rows, new JScrollPane(),
                "ACTION", "PRIMARY", "SECONDARY", "AI ACCESS", "CLEAR");
        JTable table = (JTable) pane.getViewport().getView();
        table.setSize(500, table.getRowHeight());
        table.doLayout();
        return table;
    }

    /** The centre of the row's action cell, the table's last column. */
    private static Point actionCell(JTable table) {
        Rectangle cell = table.getCellRect(0, ACTION_COLUMN, true);
        return new Point(cell.x + cell.width / 2, cell.y + cell.height / 2);
    }

    /**
     * WHY only the factory's listeners: Swing's own table handler asks the toolkit for the menu shortcut key, which a
     * headless test run refuses.
     */
    private static boolean factorys(Object listener) {
        return !listener.getClass().getName().startsWith("javax.swing.");
    }

    private static void click(JTable table) {
        Point at = actionCell(table);
        MouseEvent press = new MouseEvent(table, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                MouseEvent.BUTTON1_DOWN_MASK, at.x, at.y, 1, false, MouseEvent.BUTTON1);
        for (MouseListener listener : table.getMouseListeners()) {
            if (factorys(listener)) listener.mousePressed(press);
        }
    }

    private static int cursorOver(JTable table) {
        Point at = actionCell(table);
        MouseEvent move = new MouseEvent(table, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, at.x, at.y,
                0, false, MouseEvent.NOBUTTON);
        for (MouseMotionListener listener : table.getMouseMotionListeners()) {
            if (factorys(listener)) listener.mouseMoved(move);
        }
        return table.getCursor().getType();
    }
}
