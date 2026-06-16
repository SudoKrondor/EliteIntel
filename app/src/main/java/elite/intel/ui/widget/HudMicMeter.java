package elite.intel.ui.widget;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.ears.AudioMonitorEvent;
import elite.intel.gameapi.AudioMonitorBus;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Segmented vertical mic-level meter in the HUD visual language (HUD section 4).
 * <p>
 * Two columns: a <b>LIVE</b> column whose segments light up to the current RMS, coloured by zone
 * ({@link AppTheme#HUD_COLOR_ROLE_DANGER} below the noise floor, {@link AppTheme#HUD_COLOR_ROLE_WARNING} between floor and
 * gate, {@link AppTheme#HUD_COLOR_ROLE_SUCCESS} above the gate), and a slim <b>PEAK-trail</b> column holding the
 * decaying maximum with a bright {@link AppTheme#HUD_COLOR_ROLE_BUTTON_TEXT} cap ({@link AppTheme#HUD_COLOR_ROLE_DANGER} when
 * the input is clipping = too hot). Floor and gate thresholds are drawn as labelled rails
 * ({@link AppTheme#HUD_COLOR_ROLE_SECONDARY_TEXT} / {@link AppTheme#HUD_COLOR_ROLE_INFORMATION}); the current value and gate status are
 * read out below the columns.
 * <p>
 * Data comes from {@link AudioMonitorBus} (one {@link AudioMonitorEvent} per ~100 ms capture
 * frame): {@code rms} drives the live level, {@code noiseFloor}/{@code rmsHigh} are the
 * floor/gate, the peak is a running maximum with slow decay, and clipping is detected from the
 * raw PCM buffer. The bus runs off-EDT, so frame state is held in volatile fields and a repaint
 * is marshalled to the EDT. Registration is tied to {@link #addNotify()}/{@link #removeNotify()}.
 */
public class HudMicMeter extends JComponent {

    /** 16-bit samples at/above this magnitude are treated as hardware clipping (~97.7% of full scale). */
    private static final short CLIP_THRESHOLD = (short) 32000;
    /** Keep the clip ("HOT") state for this long after the last saturated sample. */
    private static final long CLIP_HOLD_MS = 1500;
    /** Per-frame multiplicative decay of the held peak (slow fall-back). */
    private static final double PEAK_DECAY = 0.985;
    /** Gate position on the scale (gate at 30% -> full scale = gate / 0.30). */
    private static final double GATE_POS = 0.30;
    /** Clip ("too hot") threshold position on the scale (85% of full scale). */
    private static final double CLIP_POS = 0.85;
    /** Lower edge of the marginal band as a fraction of the gate (below = gate closed). */
    private static final double MARGINAL_LOW = 0.85;
    /** Upper edge of the marginal band as a fraction of the gate (above = gate comfortably open). */
    private static final double MARGINAL_HIGH = 1.15;

    /** Grey peak-hold trail (dimmed white toward the background). */
    private static final Color PEAK_TRAIL = mix(HudPalette.HUD_COLOR_ROLE_BUTTON_TEXT, HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND, 0.60);

    // Frame state - written on the audio-monitor bus thread, read on the EDT.
    private volatile double currentRms = 0;
    private volatile double noiseFloor = 0;
    private volatile double gate = 0;
    private volatile double peakHold = 0;
    private volatile long clipExpiry = 0;

    public HudMicMeter() {
        setOpaque(false);
        Font base = UIManager.getFont("Label.font");
        if (base == null) {
            base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
        setFont(base);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        AudioMonitorBus.register(this);
    }

    @Override
    public void removeNotify() {
        AudioMonitorBus.unregister(this);
        super.removeNotify();
    }

    /** Audio-monitor bus subscriber; runs off the EDT. */
    @Subscribe
    public void onAudioFrame(AudioMonitorEvent event) {
        double rms = event.getRms();
        currentRms = rms;
        noiseFloor = event.getNoiseFloor();
        gate = event.getRmsHigh();
        peakHold = Math.max(rms, peakHold * PEAK_DECAY);

        byte[] buf = event.getBuffer();
        int len = event.getLength();
        for (int i = 0; i + 1 < len; i += 2) {
            short s = (short) (((buf[i + 1] & 0xFF) << 8) | (buf[i] & 0xFF));
            if (s >= CLIP_THRESHOLD || s <= -CLIP_THRESHOLD) {
                clipExpiry = System.currentTimeMillis() + CLIP_HOLD_MS;
                break;
            }
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        int w = HudPalette.HUD_METER_SCALE_W + HudPalette.HUD_METER_LIVE_W
                + HudPalette.HUD_METER_COL_GAP + HudPalette.HUD_METER_PEAK_W + 78;
        return new Dimension(w, 260);
    }

    @Override
    public Dimension getMinimumSize() {
        return isMinimumSizeSet() ? super.getMinimumSize()
                : new Dimension(getPreferredSize().width, 160);
    }

    /** Linear interpolation between two colours; {@code t=0} returns {@code a}, {@code t=1} returns {@code b}. */
    private static Color mix(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /**
     * @return zone colour for a segment at {@code level}, per the app's mic legend: red below the
     * gate (closed, not sending), amber in a narrow band straddling the gate (marginal - opens
     * intermittently), green from there up to {@code clip} (open), red again at/above clip (too hot).
     */
    private Color zoneColor(double level, double clip) {
        if (level < gate * MARGINAL_LOW) return HudPalette.HUD_COLOR_ROLE_DANGER;
        if (level < gate * MARGINAL_HIGH) return HudPalette.HUD_COLOR_ROLE_WARNING;
        if (level < clip) return HudPalette.HUD_COLOR_ROLE_SUCCESS;
        return HudPalette.HUD_COLOR_ROLE_DANGER;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            boolean clipping = System.currentTimeMillis() < clipExpiry;

            int scaleW = HudPalette.HUD_METER_SCALE_W;
            int readoutH = HudPalette.HUD_METER_READOUT_H;
            int top = HudPalette.HUD_PADDING_SMALL;
            int bottom = h - readoutH;
            int meterH = Math.max(1, bottom - top);

            int liveX = scaleW;
            int liveW = HudPalette.HUD_METER_LIVE_W;
            int peakX = liveX + liveW + HudPalette.HUD_METER_COL_GAP;
            int peakW = HudPalette.HUD_METER_PEAK_W;

            int n = HudPalette.HUD_METER_SEG_COUNT;
            int segGap = HudPalette.HUD_METER_SEG_GAP;
            int segH = Math.max(1, (meterH - (n - 1) * segGap) / n);

            // Gate-anchored scale: gate at 30%, clip at 85%, max = top - matches the design mockup
            // (gate 300 -> clip 850 -> max 1000). Falls back to a peak scale if gate is uncalibrated.
            double rms = currentRms;
            double peak = peakHold;
            double fullScale = gate > 0 ? gate / GATE_POS : Math.max(peak * 1.15, 1.0);
            double clip = fullScale * CLIP_POS;
            double peakShown = Math.min(peak, fullScale);

            int peakSeg = (int) Math.round(peakShown / fullScale * n) - 1;

            for (int i = 0; i < n; i++) {
                int y = bottom - (i + 1) * segH - i * segGap;
                double segLevel = (i + 0.5) / n * fullScale;
                Color zone = zoneColor(segLevel, clip);

                // Live column: lit in zone colour; unlit a dark slab with a faint zone-tint top
                // edge so the colour scale is hinted even where the level has not reached.
                if (segLevel <= rms) {
                    g2.setColor(zone);
                    g2.fillRect(liveX, y, liveW, segH);
                } else {
                    g2.setColor(HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
                    g2.fillRect(liveX, y, liveW, segH);
                    g2.setColor(mix(zone, HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND, 0.80));
                    g2.fillRect(liveX, y, liveW, Math.min(segH, 2));
                }

                // Peak-trail column: grey trail up to the held peak, bright cap on top
                // (red cap when clipping); dark above the peak.
                Color peakColor;
                if (segLevel <= peakShown) {
                    peakColor = (i == peakSeg)
                            ? ((clipping || peak >= clip) ? HudPalette.HUD_COLOR_ROLE_DANGER : HudPalette.HUD_COLOR_ROLE_BUTTON_TEXT)
                            : PEAK_TRAIL;
                } else {
                    peakColor = HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND;
                }
                g2.setColor(peakColor);
                g2.fillRect(peakX, y, peakW, segH);
            }

            int meterRight = peakX + peakW;

            // Threshold rails + scale labels.
            g2.setFont(getFont().deriveFont(HudPalette.HUD_FONT_READOUT_KEY));
            FontMetrics fmK = g2.getFontMetrics();
            drawRail(g2, fmK, "FLOOR " + (int) noiseFloor, noiseFloor, fullScale,
                    top, bottom, scaleW, meterRight, HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
            drawRail(g2, fmK, "GATE " + (int) gate, gate, fullScale,
                    top, bottom, scaleW, meterRight, HudPalette.HUD_COLOR_ROLE_INFORMATION);
            drawRail(g2, fmK, "CLIP " + (int) clip, clip, fullScale,
                    top, bottom, scaleW, meterRight, HudPalette.HUD_COLOR_ROLE_DANGER);

            // MAX anchor at the top; "0" at the bottom only when the FLOOR label is clear of it
            // (otherwise a near-zero floor rail and the "0" anchor overlap).
            g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
            g2.drawString("MAX", scaleW - fmK.stringWidth("MAX") - HudPalette.HUD_GAP, top + fmK.getAscent());
            int floorY = (int) (bottom - Math.min(1.0, noiseFloor / fullScale) * meterH);
            if (bottom - floorY > fmK.getHeight()) {
                g2.drawString("0", scaleW - fmK.stringWidth("0") - HudPalette.HUD_GAP, bottom - 1);
            }

            // Peak readout tag pinned just right of the peak column, at the cap height.
            int peakY = (int) (bottom - Math.min(1.0, peakShown / fullScale) * meterH);
            g2.setColor(HudPalette.HUD_COLOR_ROLE_BUTTON_TEXT);
            String peakTag = "PEAK " + (int) peak;
            int tagX = Math.min(meterRight + HudPalette.HUD_GAP, w - fmK.stringWidth(peakTag));
            g2.drawString(peakTag, tagX, Math.max(top + fmK.getAscent(), peakY));

            // Big current-value readout + status below the columns.
            Color statusColor;
            String status;
            if (clipping || rms >= clip) { statusColor = HudPalette.HUD_COLOR_ROLE_DANGER; status = "HOT"; }
            else if (rms >= gate * MARGINAL_HIGH) { statusColor = HudPalette.HUD_COLOR_ROLE_SUCCESS; status = "OPEN"; }
            else if (rms >= gate * MARGINAL_LOW) { statusColor = HudPalette.HUD_COLOR_ROLE_WARNING; status = "MARGINAL"; }
            else { statusColor = HudPalette.HUD_COLOR_ROLE_DANGER; status = "CLOSED"; }

            int center = scaleW + (meterRight - scaleW) / 2;
            g2.setFont(getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_STAT_LG));
            FontMetrics fmBig = g2.getFontMetrics();
            String num = String.valueOf((int) rms);
            g2.setColor(statusColor);
            g2.drawString(num, center - fmBig.stringWidth(num) / 2, bottom + fmBig.getAscent());

            g2.setFont(getFont().deriveFont(HudPalette.HUD_FONT_READOUT_KEY));
            FontMetrics fmS = g2.getFontMetrics();
            String sub = "LIVE · " + status;
            g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
            g2.drawString(sub, center - fmS.stringWidth(sub) / 2, bottom + fmBig.getHeight() + fmS.getAscent() - 2);
        } finally {
            g2.dispose();
        }
    }

    /** Draws a horizontal threshold rail across both columns with a left-gutter label. */
    private void drawRail(Graphics2D g2, FontMetrics fm, String label, double level, double fullScale,
                          int top, int bottom, int scaleW, int meterRight, Color color) {
        if (level <= 0) return;
        int y = (int) (bottom - Math.min(1.0, level / fullScale) * (bottom - top));
        g2.setColor(color);
        g2.fillRect(scaleW, y, meterRight - scaleW, HudPalette.HUD_BORDER_THICKNESS);
        g2.drawString(label, scaleW - fm.stringWidth(label) - HudPalette.HUD_GAP, y + fm.getAscent() / 2);
    }
}
