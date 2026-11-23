package elite.intel.ui.widget;

import com.formdev.flatlaf.ui.FlatToolTipUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

/**
 * The app's tooltip UI: FlatLaf's, plus a repaint once the tooltip is on screen.
 * <p>
 * WHY: a tooltip that does not fit inside the app's window gets a window of its own (a heavy-weight popup), and
 * FlatLaf reuses that window from one tooltip to the next. On Linux the tooltip's first paint is sometimes lost - it
 * lands before the desktop has mapped the window, or the reused window keeps its emptied content pane - and the
 * commander sees a blank pale box the size of the tooltip. Painting again once the tooltip is showing, and once more
 * a moment later, fills it in. The binding grid's callout ({@link BindingConflictPopup}) shows inside the app's window,
 * so it never needed this.
 * <p>
 * Installed for every tooltip by {@code HudLookAndFeel} through the {@code ToolTipUI} default.
 */
public class HudToolTipUI extends FlatToolTipUI {

    /** Long enough for the desktop to map a new window; short enough that a blank box is never noticed. */
    private static final int LATE_REPAINT_MS = 80;

    private static final HierarchyListener REPAINT_WHEN_SHOWN = HudToolTipUI::onHierarchyChanged;

    /** Swing calls it by reflection through the {@code ToolTipUI} default. */
    @SuppressWarnings("unused")
    public static ComponentUI createUI(JComponent c) {
        return new HudToolTipUI();
    }

    @Override
    protected void installListeners(JComponent c) {
        super.installListeners(c);
        c.addHierarchyListener(REPAINT_WHEN_SHOWN);
    }

    @Override
    protected void uninstallListeners(JComponent c) {
        super.uninstallListeners(c);
        c.removeHierarchyListener(REPAINT_WHEN_SHOWN);
    }

    private static void onHierarchyChanged(HierarchyEvent e) {
        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !e.getComponent().isShowing()) {
            return;
        }
        JComponent tip = (JComponent) e.getComponent();
        Window window = SwingUtilities.getWindowAncestor(tip);
        if (window instanceof JWindow popupWindow && tip.getParent() == popupWindow.getContentPane()) {
            // The tooltip's own window: should a paint still go missing, show the tooltip's colour, not a pale box.
            popupWindow.getContentPane().setBackground(tip.getBackground());
        }
        SwingUtilities.invokeLater(tip::repaint);
        Timer late = new Timer(LATE_REPAINT_MS, ignored -> {
            if (tip.isShowing()) {
                tip.repaint();
            }
        });
        late.setRepeats(false);
        late.start();
    }
}
