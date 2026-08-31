package elite.intel.ui.widget;

import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntFunction;

/**
 * HUD range slider rendered in the in-game Elite Dangerous form (HUD section 4).
 * <p>
 * Layout: a warm brown track plaque ({@link HudPalette#HUD_COLOR_ROLE_SLIDER_TRACK_BACKGROUND}) spans the full
 * width; a dim rail ({@link HudPalette#HUD_COLOR_ROLE_CONTROL_DECORATION}) with a horizontal edge inset runs through
 * its centre; the active portion left of the thumb is a saturated red fill
 * ({@link HudPalette#HUD_COLOR_ROLE_SLIDER_VALUE_TRACK}) drawn over a tall vertical start tick. The thumb is a round
 * {@link HudPalette#HUD_COLOR_ROLE_PRIMARY_ACTION} disc with a {@link HudPalette#HUD_COLOR_ROLE_BUTTON_TEXT} ring. The current value is
 * rendered above the thumb and follows it.
 * <p>
 * The control snaps to {@code step} and exposes a {@link JComponent}-level
 * {@link ChangeListener} API ({@link #getValue()}, {@link #setValue(int)},
 * {@link #addChangeListener(ChangeListener)}) mirroring the subset of {@link javax.swing.JSlider}
 * used by callers. Disabled state mutes every part to {@link HudPalette#HUD_COLOR_ROLE_DISABLED} (section 0.6).
 */
public class HudSlider extends JComponent {

    private final int min;
    private final int step;
    private int max;
    private int value;
    private boolean dragging;
    private IntFunction<String> valueFormatter = String::valueOf;

    /**
     * @param min   lowest selectable value (inclusive)
     * @param max   highest selectable value (inclusive)
     * @param step  snap increment; values are quantised to {@code min + k*step}
     * @param value initial value (clamped into range and snapped)
     */
    public HudSlider(int min, int max, int step, int value) {
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.value = snap(clamp(value));

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Font base = UIManager.getFont("Label.font");
        if (base == null) {
            base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
        setFont(base.deriveFont(Font.BOLD, HudPalette.HUD_FONT_FIELD_VALUE));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    dragging = true;
                    setValueFromX(e.getX());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging) {
                    setValueFromX(e.getX());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!dragging) return;
                dragging = false;
                // WHY an event with nothing changed: it is the only moment a listener can tell the drag
                // is over. One that acts on every step - a seek that reopens a file, a setting written to
                // the database - waits for this one and ignores the rest.
                fireStateChanged();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /** @return the current value. */
    public int getValue() {
        return value;
    }

    /** Sets the value (clamped and snapped) and fires a {@link ChangeEvent} if it changed. */
    public void setValue(int newValue) {
        int next = snap(clamp(newValue));
        if (next != value) {
            value = next;
            repaint();
            fireStateChanged();
        }
    }

    /**
     * Raises the top of the range, for a slider whose extent is not known when it is built - the length of
     * the track being played, say. The value is re-clamped, and moves (firing a change) only if it now
     * falls outside the range.
     */
    public void setMaximum(int newMaximum) {
        int next = Math.max(min, newMaximum);
        if (next == max) return;
        max = next;
        setValue(value);
        repaint();
    }

    /**
     * @return true while the thumb is being dragged, so a listener can act on the settled value only
     */
    public boolean isAdjusting() {
        return dragging;
    }

    /**
     * Replaces the plain number drawn above the thumb - a play-head reads better as {@code 1:23 / 4:05}
     * than as {@code 83}.
     */
    public void setValueFormatter(IntFunction<String> formatter) {
        valueFormatter = formatter == null ? String::valueOf : formatter;
        repaint();
    }

    /** Registers a listener notified whenever the value changes (programmatically or by drag). */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    private void fireStateChanged() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener l : listenerList.getListeners(ChangeListener.class)) {
            l.stateChanged(event);
        }
    }

    private int clamp(int v) {
        return Math.max(min, Math.min(max, v));
    }

    /** Quantises {@code v} to the nearest {@code min + k*step} within range. */
    private int snap(int v) {
        int snapped = min + Math.round((v - min) / (float) step) * step;
        return clamp(snapped);
    }

    /** Left edge of the thumb travel range (also the start-tick x). */
    private int thumbMin() {
        return HudPalette.HUD_SLIDER_RANGE_INSET;
    }

    /** Right edge of the thumb travel range. */
    private int thumbMax() {
        return getWidth() - HudPalette.HUD_SLIDER_RANGE_INSET;
    }

    /** Maps the current value to the thumb centre x. */
    private int thumbX() {
        if (max == min) {
            return thumbMin();
        }
        double pct = (value - min) / (double) (max - min);
        return thumbMin() + (int) Math.round(pct * (thumbMax() - thumbMin()));
    }

    private void setValueFromX(int x) {
        int lo = thumbMin();
        int hi = thumbMax();
        double pct = hi == lo ? 0 : (x - lo) / (double) (hi - lo);
        pct = Math.max(0, Math.min(1, pct));
        setValue((int) Math.round(min + pct * (max - min)));
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        // Width is nominal - the slider is expected to stretch horizontally in its layout.
        return new Dimension(200, HudPalette.HUD_SLIDER_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return isMinimumSizeSet()
                ? super.getMinimumSize()
                : new Dimension(2 * HudPalette.HUD_SLIDER_RANGE_INSET, HudPalette.HUD_SLIDER_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean enabled = isEnabled();
            int w = getWidth();
            int h = getHeight();

            int trackH = HudPalette.HUD_SLIDER_TRACK_HEIGHT;
            int diameter = HudPalette.HUD_SLIDER_THUMB_DIAMETER;
            // Reserve HUD_SLIDER_VALUE_AREA on top for the value above the thumb; the track band is
            // centred in the remaining height. Mirrors the label top inset so they line up.
            int valueArea = Math.min(HudPalette.HUD_SLIDER_VALUE_AREA, h);
            int centerY = valueArea + (h - valueArea) / 2;
            int trackTop = centerY - trackH / 2;

            int edge = HudPalette.HUD_SLIDER_EDGE_INSET;
            int thumbX = thumbX();

            // Warm brown track plaque (muted when disabled).
            g2.setColor(enabled ? HudPalette.HUD_COLOR_ROLE_SLIDER_TRACK_BACKGROUND : HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
            g2.fillRect(0, trackTop, w, trackH);

            // Dim rail with edge inset on both sides.
            int railY = centerY - HudPalette.HUD_SLIDER_RAIL_THICKNESS / 2;
            g2.setColor(enabled ? HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION : HudPalette.HUD_COLOR_ROLE_DISABLED);
            g2.fillRect(edge, railY, w - 2 * edge, HudPalette.HUD_SLIDER_RAIL_THICKNESS);

            // Tall vertical start tick (origin) at the left of the thumb travel range.
            int tickH = trackH - HudPalette.HUD_SLIDER_RAIL_THICKNESS * 2;
            g2.fillRect(thumbMin() - 1, centerY - tickH / 2, HudPalette.HUD_SLIDER_RAIL_THICKNESS, tickH);

            // Red active fill from the edge inset to the thumb (drawn over the start tick).
            int fillY = centerY - HudPalette.HUD_SLIDER_FILL_THICKNESS / 2;
            int fillW = Math.max(0, thumbX - edge);
            g2.setColor(enabled ? HudPalette.HUD_COLOR_ROLE_SLIDER_VALUE_TRACK : HudPalette.HUD_COLOR_ROLE_DISABLED);
            g2.fillRect(edge, fillY, fillW, HudPalette.HUD_SLIDER_FILL_THICKNESS);

            // Round thumb: ring then accent core.
            int ring = HudPalette.HUD_SLIDER_THUMB_RING;
            int tx = thumbX - diameter / 2;
            int ty = centerY - diameter / 2;
            g2.setColor(enabled ? HudPalette.HUD_COLOR_ROLE_BUTTON_TEXT : HudPalette.HUD_COLOR_ROLE_DISABLED);
            g2.fillOval(tx, ty, diameter, diameter);
            g2.setColor(enabled ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION : HudPalette.HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND);
            g2.fillOval(tx + ring, ty + ring, diameter - 2 * ring, diameter - 2 * ring);

            // Value above the thumb, centred on it and clamped within the component bounds.
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String text = valueFormatter.apply(value);
            int textW = fm.stringWidth(text);
            int textX = Math.max(0, Math.min(thumbX - textW / 2, w - textW));
            int baseline = ty - 2 - fm.getDescent();
            g2.setColor(enabled ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION : HudPalette.HUD_COLOR_ROLE_DISABLED);
            g2.drawString(text, textX, baseline);
        } finally {
            g2.dispose();
        }
    }
}
