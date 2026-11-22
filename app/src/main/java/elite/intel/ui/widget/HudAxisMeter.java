package elite.intel.ui.widget;

import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;

/**
 * Where a controller's axis is right now, across its whole travel: a track with a tick at its centre, a fill from the
 * centre out to the value, and a marker on the value.
 * <p>
 * WHY not a progress bar: that fills one way from one end, and a stick moves both ways from the middle. This reads the
 * same for every kind of axis without knowing which it is. A stick swings either side of the tick, and a throttle or
 * slider, which the controller reports the same way, walks from one end to the other with the marker showing where it
 * is. A zero of its own would have to be guessed from where the axis happened to be sitting.
 * <p>
 * Shows only the tick until the first value arrives, because a controller reports an axis only when it moves.
 */
public class HudAxisMeter extends JComponent {

    private static final int PREFERRED_WIDTH = 120;
    private static final int MARKER_WIDTH = 3;
    private static final int EDGE_INSET = 3;

    /**
     * {@code null} until a value has been seen.
     */
    private Float value;

    public HudAxisMeter() {
        setOpaque(false);
    }

    /**
     * @param value the axis's position, -1 to 1; {@code null} when none is known, as after the controller was unplugged
     */
    public void setValue(Float value) {
        if (!java.util.Objects.equals(this.value, value)) {
            this.value = value;
            repaint();
        }
    }

    public Float getValue() {
        return value;
    }

    /**
     * The pixel a value falls on, in a track running from {@code left} for {@code width} pixels. Out-of-range values
     * stop at the ends.
     */
    static int position(float value, int left, int width) {
        float clamped = Math.max(-1f, Math.min(1f, value));
        return left + Math.round((clamped + 1f) / 2f * (width - 1));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PREFERRED_WIDTH, HudPalette.HUD_FIELD_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int height = HudPalette.HUD_SLIDER_TRACK_HEIGHT;
        int top = (getHeight() - height) / 2;
        int left = EDGE_INSET;
        int width = getWidth() - 2 * EDGE_INSET;

        g.setColor(HudPalette.HUD_COLOR_ROLE_SLIDER_TRACK_BACKGROUND);
        g.fillRect(0, top, getWidth(), height);

        int centre = position(0f, left, width);
        if (value != null) {
            int at = position(value, left, width);
            g.setColor(HudPalette.HUD_COLOR_ROLE_SLIDER_VALUE_TRACK);
            g.fillRect(Math.min(centre, at), top + height / 4, Math.abs(at - centre), height / 2);
        }
        g.setColor(HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION);
        g.fillRect(centre, top, 1, height);
        if (value != null) {
            g.setColor(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
            g.fillRect(position(value, left, width) - MARKER_WIDTH / 2, top, MARKER_WIDTH, height);
        }
    }
}
