package elite.intel.ui.widget;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.theme.HudGlyphs;

import javax.swing.JComponent;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * HUD discrete numeric stepper rendered as "< value >" (HUD section 4).
 * <p>
 * Triangle buttons sit at the left/right edges of a borderless warm slab
 * ({@link HudPalette#HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND}, like the HUD checkbox OFF state, section 5.2), separated from the
 * centred value by {@link HudPalette#HUD_COLOR_ROLE_APPLICATION_BACKGROUND} dividers. Clicking a triangle steps the value by
 * {@code step}, clamped to {@code [min, max]}.
 * <p>
 * Arrow states: rest {@link HudPalette#HUD_COLOR_ROLE_PRIMARY_ACTION}; hover - light HUD_COLOR_ROLE_PRIMARY_ACTION wash on the zone; pressed -
 * full {@link HudPalette#HUD_COLOR_ROLE_PRIMARY_ACTION} fill with the arrow inverted to {@link HudPalette#HUD_COLOR_ROLE_SELECTED_TEXT} (matching
 * the subtle button); disabled at the range end - {@link HudPalette#HUD_COLOR_ROLE_DISABLED}. The value is
 * display-only (no free text entry).
 */
public class HudStepper extends JComponent {

    /** Width allotted to each triangle glyph inside its zone. */
    private static final int ARROW_BOX = 14;

    private final int min;
    private final int max;
    private final int step;
    private int value;

    /** Hovered/pressed zone: -1 left, +1 right, 0 none. */
    private int hoverZone;
    private int pressedZone;

    /**
     * @param min     lowest allowed value (inclusive)
     * @param max     highest allowed value (inclusive)
     * @param step    increment applied per arrow click
     * @param initial initial value (clamped into range)
     */
    public HudStepper(int min, int max, int step, int initial) {
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.value = clamp(initial);

        setOpaque(false);
        Font base = UIManager.getFont("Label.font");
        if (base == null) {
            base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
        setFont(base.deriveFont(Font.BOLD, HudPalette.HUD_FONT_FIELD_VALUE));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int zone = enabledZoneAt(e.getX());
                if (zone != 0) {
                    pressedZone = zone;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (pressedZone != 0) {
                    if (enabledZoneAt(e.getX()) == pressedZone) {
                        setValue(value + (pressedZone < 0 ? -step : step));
                    }
                    pressedZone = 0;
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateHover(e.getX());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updateHover(e.getX());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverZone != 0 || pressedZone != 0) {
                    hoverZone = 0;
                    pressedZone = 0;
                    repaint();
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /** @return the current value. */
    public int getValue() {
        return value;
    }

    /** Sets the value, clamped to the configured range, and repaints. */
    public void setValue(int newValue) {
        int clamped = clamp(newValue);
        if (clamped != value) {
            value = clamped;
            repaint();
        }
    }

    private int clamp(int v) {
        return Math.max(min, Math.min(max, v));
    }

    private void updateHover(int x) {
        int zone = zoneAt(x);
        if (zone != hoverZone) {
            hoverZone = zone;
            setCursor(Cursor.getPredefinedCursor(
                    enabledZoneAt(x) != 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            repaint();
        }
    }

    /** Square arrow zone width on each side (tracks the plaque height). */
    private int zoneWidth() {
        return getHeight();
    }

    /** @return -1 if x is in the left arrow zone, +1 right, 0 in the value area. */
    private int zoneAt(int x) {
        int zw = zoneWidth();
        if (x < zw) {
            return -1;
        }
        if (x >= getWidth() - zw) {
            return 1;
        }
        return 0;
    }

    /** Like {@link #zoneAt} but 0 when stepping in that direction is not allowed. */
    private int enabledZoneAt(int x) {
        int zone = zoneAt(x);
        if (zone < 0 && value <= min) {
            return 0;
        }
        if (zone > 0 && value >= max) {
            return 0;
        }
        return zone;
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        int h = HudPalette.HUD_FIELD_HEIGHT;
        FontMetrics fm = getFontMetrics(getFont());
        int textW = Math.max(fm.stringWidth(String.valueOf(value)), fm.stringWidth("0000"));
        int width = 2 * h + 2 * HudPalette.HUD_SEP_W + textW + 2 * HudPalette.HUD_GAP;
        return new Dimension(width, h);
    }

    @Override
    public Dimension getMinimumSize() {
        return isMinimumSizeSet() ? super.getMinimumSize() : getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth();
            int h = getHeight();
            int zw = zoneWidth();

            // Borderless warm slab, like the HUD checkbox in its OFF state (section 5.2).
            g2.setColor(HudPalette.HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND);
            g2.fillRect(0, 0, w, h);

            // Arrow zones at the edges, with hover/press state fills.
            paintArrowZone(g2, -1, 0, zw, h, value > min);
            paintArrowZone(g2, 1, w - zw, zw, h, value < max);

            // Vertical dividers (HUD_COLOR_ROLE_APPLICATION_BACKGROUND) between arrow zones and the value, as in the checkbox gap.
            g2.setColor(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
            g2.fillRect(zw, 0, HudPalette.HUD_SEP_W, h);
            g2.fillRect(w - zw - HudPalette.HUD_SEP_W, 0, HudPalette.HUD_SEP_W, h);

            // Value centred.
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String text = String.valueOf(value);
            g2.setColor(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
            int tx = (w - fm.stringWidth(text)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, tx, ty);
        } finally {
            g2.dispose();
        }
    }

    private void paintArrowZone(Graphics2D g2, int dir, int x, int zw, int h, boolean enabled) {
        Color arrowColor;
        if (!enabled) {
            arrowColor = HudPalette.HUD_COLOR_ROLE_DISABLED;
        } else if (pressedZone == dir) {
            // Full HUD_COLOR_ROLE_PRIMARY_ACTION fill + inverted arrow, matching the subtle button's pressed state.
            g2.setColor(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
            g2.fillRect(x, 0, zw, h);
            arrowColor = HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT;
        } else if (hoverZone == dir) {
            // Light HUD_COLOR_ROLE_PRIMARY_ACTION wash on hover (base slab is already HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND).
            g2.setColor(new Color(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION.getRed(), HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION.getGreen(),
                    HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION.getBlue(), 45));
            g2.fillRect(x, 0, zw, h);
            arrowColor = HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION;
        } else {
            arrowColor = HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION;
        }
        if (dir < 0) {
            HudGlyphs.paintHudArrowLeft(g2, x, 0, zw, h, arrowColor);
        } else {
            HudGlyphs.paintHudArrowRight(g2, x, 0, zw, h, arrowColor);
        }
    }
}
