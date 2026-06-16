package elite.intel.ui.widget;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Full-width HUD toolbar that paints the "connected-to-table" top border:
 * top/left/right sides in {@link HudPalette#HUD_COLOR_ROLE_FRAME_BORDER}, a dim bottom separator in
 * {@link HudPalette#HUD_COLOR_ROLE_SECONDARY_BORDER}, and cyan L-shaped corner marks on the two top corners.
 * <p>
 * Place a {@link AppTheme#hudConnectedScrollPaneBorder()} data table directly below this toolbar
 * with no vertical gap so that the shared side borders form one continuous framed block.
 * <p>
 * Internal layout is {@code BorderLayout(HUD_GAP, 0)}.
 * Put the primary search control (typically {@link HudSearchField} with
 * {@link HudSearchField.Variant#TABLE_FILTER}) at {@code BorderLayout.CENTER} and supplementary
 * action buttons at {@code BorderLayout.EAST}.
 */
public class HudConnectedToolbar extends JPanel {

    private static final int CORNER_MARK = 6;

    public HudConnectedToolbar() {
        super(new BorderLayout(HudPalette.HUD_GAP, 0));
        setOpaque(true);
        setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        setBorder(new EmptyBorder(8, 8, 8, 8));
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int m = CORNER_MARK;
            g2.setColor(HudPalette.HUD_COLOR_ROLE_FRAME_BORDER);
            g2.drawLine(0, 0, w - 1, 0);           // top
            g2.drawLine(0, 0, 0, h - 1);           // left
            g2.drawLine(w - 1, 0, w - 1, h - 1);  // right
            // Dim separator marks the filter/table boundary
            g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER);
            g2.drawLine(0, h - 1, w - 1, h - 1);
        } finally {
            g2.dispose();
        }
    }
}
