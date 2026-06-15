package elite.intel.ui.view;

import javax.swing.*;
import java.awt.*;

/**
 * Two equal-width columns separated by a centred vertical divider in the warm subordinate rail
 * tone ({@link AppTheme#HUD_ORANGE_FILL_HOVER} — quieter than a section header rail). Shared HUD
 * layout for side-by-side form/section columns (e.g. local/cloud setup, command identity vs steps).
 * <p>
 * Columns are exactly equal ({@link GridLayout}); each child fills its half. If a child's content
 * must top-align, wrap it in a {@code BorderLayout} panel and add it at {@code NORTH}.
 */
public class HudTwoColumns extends JPanel {

    /**
     * Creates a two-column row with a centred divider.
     *
     * @param left  left column component (fills its half)
     * @param right right column component (fills its half)
     */
    public HudTwoColumns(Component left, Component right) {
        super(new GridLayout(1, 2, AppTheme.HUD_GAP * 2, 0));
        setOpaque(false);
        add(left);
        add(right);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Painted directly (not a child's background) so the global dark palette can't overwrite it.
        int t = AppTheme.HUD_BORDER_THICKNESS;
        int x = (getWidth() - t) / 2;
        g.setColor(AppTheme.HUD_ORANGE_FILL_HOVER);
        g.fillRect(x, 0, t, getHeight());
    }
}
