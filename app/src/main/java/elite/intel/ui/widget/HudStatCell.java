package elite.intel.ui.widget;

import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;

/**
 * Single statistic readout in the Elite Dangerous rank-panel style: a colour-tinted emblem on the
 * left, a right-aligned muted label above a prominent value, and a thin progress bar with a
 * percentage at the right edge. One cell shows one metric; a panel stacks several vertically.
 * <p>
 * The bar is <b>adaptive</b>: the caller supplies a fraction (0..1) of the value against the metric's
 * observed session peak, since the underlying counters have no fixed ceiling. Update with
 * {@link #setValue(String, double, String)}.
 */
public class HudStatCell extends JPanel {

    private final ImageIcon emblem;
    private final String label;
    private final String unit;
    private final Color bright;
    private final Color dim;
    private final Color track;

    private String value = HudTelemetryBlock.PLACEHOLDER;
    private double fraction;
    private String percent = "";

    /**
     * @param label    short caps label (e.g. {@code "LAST PROMPT"})
     * @param iconRes  classpath image resource for the emblem (tinted to {@code color})
     * @param color    category colour for the emblem, value, bar fill and percentage
     * @param unit     unit suffix shown dim after the value (e.g. {@code "T/S"}); may be null/blank
     */
    public HudStatCell(String label, String iconRes, Color color, String unit) {
        this.label = label == null ? "" : label.toUpperCase();
        this.unit = unit;
        this.bright = color;
        this.dim = withAlpha(color, 190);
        this.track = withAlpha(color, 60);
        ImageIcon base = HudGlyphs.scaledIcon(HudStatCell.class, iconRes, HudPalette.HUD_ICON_MAIN);
        this.emblem = HudGlyphs.tintIcon(base, HudPalette.HUD_ICON_MAIN, HudPalette.HUD_ICON_MAIN, color);
        setOpaque(false);
    }

    /**
     * Updates the readout.
     *
     * @param value    formatted value text; null/blank reverts to the placeholder dash
     * @param fraction bar fill as a fraction (0..1) of the metric's session peak
     * @param percent  text shown at the right edge (e.g. {@code "86%"})
     */
    public void setValue(String value, double fraction, String percent) {
        this.value = (value == null || value.isBlank()) ? HudTelemetryBlock.PLACEHOLDER : value;
        this.fraction = Math.max(0, Math.min(1, fraction));
        this.percent = percent == null ? "" : percent;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, HudPalette.HUD_STAT_CELL_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, HudPalette.HUD_STAT_CELL_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, HudPalette.HUD_STAT_CELL_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int pad = HudPalette.HUD_PADDING;
            int w = getWidth();
            int h = getHeight();
            int icon = HudPalette.HUD_ICON_MAIN;

            // Emblem, vertically centered on the left.
            g2.drawImage(emblem.getImage(), pad, (h - icon) / 2, this);

            int contentLeft = pad + icon + HudPalette.HUD_GAP * 2;
            int right = w - pad;

            Font baseFont = getFont();
            Font labelFont = baseFont.deriveFont(Font.PLAIN, HudPalette.HUD_FONT_FIELD_LABEL);
            Font valueFont = baseFont.deriveFont(Font.BOLD, HudPalette.HUD_FONT_STAT_VALUE);
            Font pctFont = baseFont.deriveFont(Font.PLAIN, HudPalette.HUD_FONT_STAT_LG);

            // Label (top, right-aligned, dim tint).
            g2.setFont(labelFont);
            FontMetrics lfm = g2.getFontMetrics();
            int labelBaseline = pad + lfm.getAscent();
            g2.setColor(dim);
            g2.drawString(label, right - lfm.stringWidth(label), labelBaseline);

            // Value + optional unit (right-aligned, bright tint).
            g2.setFont(valueFont);
            FontMetrics vfm = g2.getFontMetrics();
            int valueBaseline = labelBaseline + vfm.getAscent() + HudPalette.HUD_PADDING_SMALL;
            int valueW = vfm.stringWidth(value);
            int unitW = (unit != null && !unit.isBlank()) ? lfm.stringWidth(unit) + HudPalette.HUD_GAP : 0;
            int valueX = right - valueW - unitW;
            g2.setColor(bright);
            g2.drawString(value, valueX, valueBaseline);
            if (unitW > 0) {
                g2.setFont(labelFont);
                g2.setColor(dim);
                g2.drawString(unit, valueX + valueW + HudPalette.HUD_GAP, valueBaseline);
            }

            // Adaptive progress bar with the percentage reserved at the right.
            g2.setFont(pctFont);
            FontMetrics pfm = g2.getFontMetrics();
            int pctReserve = pfm.stringWidth("100%") + HudPalette.HUD_GAP;
            // Thin dim track with a thicker bright data-fill, both centered on the same line.
            int fillT = HudPalette.HUD_STAT_CELL_BAR_THICKNESS;
            int trackT = HudPalette.HUD_STAT_CELL_TRACK_THICKNESS;
            int centerY = h - pad - fillT / 2;
            int barRight = right - pctReserve;
            int trackW = Math.max(0, barRight - contentLeft);
            g2.setColor(track);
            g2.fillRect(contentLeft, centerY - trackT / 2, trackW, trackT);
            g2.setColor(bright);
            g2.fillRect(contentLeft, centerY - fillT / 2, (int) (trackW * fraction), fillT);

            // Percentage, right-aligned, vertically centered on the bar.
            int pctBaseline = centerY + pfm.getAscent() / 2;
            g2.drawString(percent, right - pfm.stringWidth(percent), pctBaseline);
        } finally {
            g2.dispose();
        }
    }

    /** Returns the colour with the given alpha, used to derive dim label and track tones from a role colour. */
    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
}
