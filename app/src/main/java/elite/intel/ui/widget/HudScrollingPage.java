package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;

import javax.swing.*;
import java.awt.*;

/**
 * A tab page that scrolls vertically once the window is too short for it, and lays out exactly as it
 * did without the scroll pane while there is room.
 * <p>
 * A plain panel dropped into a scroll pane takes its preferred size, so its columns stop sharing the
 * tab's width and a horizontal bar appears. This view tracks the viewport's width always, and its
 * height only while the page fits - so a page that pins buttons to its bottom keeps them there in a
 * tall window and scrolls them away in a short one, rather than squashing the rows above.
 */
public class HudScrollingPage extends JPanel implements Scrollable {

    private HudScrollingPage(JComponent page) {
        super(new BorderLayout());
        setOpaque(false);
        add(page, BorderLayout.CENTER);
    }

    /**
     * Wraps {@code page} in the HUD scroll pane with only a vertical bar, on the application background.
     */
    public static JScrollPane scrollPane(JComponent page) {
        JScrollPane scrollPane = AppTheme.hudApplicationScrollPane(new HudScrollingPage(page));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(16, visibleRect.height);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /**
     * Fill the viewport while the page fits in it, so the page lays out as if there were no scroll pane.
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport viewport && viewport.getHeight() >= getPreferredSize().height;
    }
}
