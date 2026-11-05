package elite.intel.ui.widget;

import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;

/**
 * A block of one colour, clickable.
 * <p>
 * A button so it is reachable by keyboard and announces itself as something to press, but painted rather than
 * themed: the HUD button styling would cover the very thing the control exists to show.
 */
public final class HudColorSwatch extends JButton {

    private static final int WIDTH = 46;
    private static final int HEIGHT = 18;

    private Color color;

    public HudColorSwatch(Color color) {
        this.color = color;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(color);
        g2.fillRect(1, 1, getWidth() - 2, getHeight() - 2);
        // Framed, because a swatch set to the dialog's own background would
        // otherwise look like a missing control rather than a dark colour.
        g2.setColor(hasFocus()
                ? HudPalette.HUD_COLOR_ROLE_INPUT_FOCUS
                : HudPalette.HUD_COLOR_ROLE_FRAME_BORDER);
        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        g2.dispose();
    }
}
