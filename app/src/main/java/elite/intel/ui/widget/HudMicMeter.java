package elite.intel.ui.widget;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.ears.AudioMonitorEvent;
import elite.intel.eventbus.AudioMonitorBus;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;

/**
 * Segmented vertical mic-level meter in the HUD visual language (HUD section 4).
 * <p>
 * Two columns: a <b>LIVE</b> column whose segments light up to the current RMS, coloured by zone
 * ({@link HudPalette#HUD_COLOR_ROLE_DANGER} below the noise floor, {@link HudPalette#HUD_COLOR_ROLE_WARNING} between floor and
 * gate, {@link HudPalette#HUD_COLOR_ROLE_SUCCESS} above the gate), and a slim <b>PEAK-trail</b> column holding the
 * decaying maximum with a bright {@link HudPalette#HUD_COLOR_ROLE_BUTTON_TEXT} cap ({@link HudPalette#HUD_COLOR_ROLE_DANGER} when
 * the input is clipping = too hot). Floor and gate thresholds are drawn as labelled rails
 * ({@link HudPalette#HUD_COLOR_ROLE_SECONDARY_TEXT} / {@link HudPalette#HUD_COLOR_ROLE_INFORMATION}); the current value and gate status are
 * read out below the columns.
 * <p>
 * Data comes from {@link AudioMonitorBus} (one {@link AudioMonitorEvent} per ~100 ms capture
 * frame): {@code rms} drives the live level and {@code noiseFloor}/{@code rmsHigh} are the
 * floor/gate, while the sample peak and the clipping state are both scanned out of the raw PCM
 * buffer, the peak being held and decayed across frames. The frames are raw capture, taken ahead of
 * {@code Amplifier} and noise reduction, so the meter reads the microphone as the operating system
 * hands it over. The bus runs off-EDT, so frame state is held in volatile fields and a repaint
 * is marshalled to the EDT. Registration is tied to {@link #addNotify()}/{@link #removeNotify()}.
 * <p>
 * <b>Units.</b> The meter is a standard {@code dBFS} instrument: 0 dBFS is digital full scale and
 * the scale is linear in decibels from {@link #SCALE_MIN_DBFS} up to 0, so it reads the same as any
 * other meter pointed at the same microphone. Levels arrive on the bus as linear RMS amplitudes in
 * 16-bit sample units (0..32767) and are converted once, on entry, by {@link #dbfs(double)}.
 * <p>
 * The two columns deliberately measure different things, as on a DAW meter. <b>LIVE</b> is the
 * <i>RMS</i> of the frame - the level the voice gate actually compares against, hence the FLOOR and
 * GATE rails. <b>PEAK</b> is the true <i>sample peak</i> of the frame, which is what Reaper and OBS
 * display. For speech the two differ by the crest factor, typically 12-18 dB, so PEAK reading well
 * above LIVE is correct and expected, not a fault.
 */
public class HudMicMeter extends JComponent {

    /** 16-bit samples at/above this magnitude are treated as hardware clipping (~97.7% of full scale). */
    private static final short CLIP_THRESHOLD = (short) 32000;
    /**
     * Amplitude of digital full scale for 16-bit PCM; the 0 dBFS reference.
     */
    private static final double FULL_SCALE_AMPLITUDE = 32768.0;
    /**
     * Bottom of the scale. Below this a signal is inaudible next to any realistic noise floor.
     */
    private static final double SCALE_MIN_DBFS = -60.0;
    /**
     * CLIP ("too hot") rail: the conventional headroom warning for a peak level.
     */
    private static final double CLIP_DBFS = -3.0;
    /**
     * Silence readout, and its fallback for fonts that cannot render the glyph. Written as a
     * codepoint rather than a literal: the build sets no source encoding, so javac reads sources in
     * the platform default charset and a literal U+221E INFINITY would mangle on a non-UTF-8 machine.
     */
    private static final char INFINITY = 0x221E;
    private static final String MINUS_INFINITY = "-" + INFINITY;
    private static final String MINUS_INFINITY_FALLBACK = "-INF";
    /** Keep the clip ("HOT") state for this long after the last saturated sample. */
    private static final long CLIP_HOLD_MS = 1500;
    /**
     * Per-frame fall-back of the held peak, in dB (0.985 linear, preserved from the linear meter).
     */
    private static final double PEAK_DECAY_DB = 0.13;
    /**
     * Half-width, in dB, of the marginal band straddling the gate (gate opens intermittently here).
     */
    private static final double MARGINAL_BAND_DB = 1.2;

    /** Grey peak-hold trail (dimmed white toward the background). */
    private static final Color PEAK_TRAIL = mix(HudPalette.HUD_COLOR_ROLE_BUTTON_TEXT, HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND, 0.60);

    // Frame state, all in dBFS - written on the audio-monitor bus thread, read on the EDT.
    private volatile double currentRmsDb = Double.NEGATIVE_INFINITY;
    private volatile double noiseFloorDb = Double.NEGATIVE_INFINITY;
    private volatile double gateDb = Double.NEGATIVE_INFINITY;
    private volatile double peakHoldDb = Double.NEGATIVE_INFINITY;
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
        currentRmsDb = dbfs(event.getRms());
        noiseFloorDb = dbfs(event.getNoiseFloor());
        gateDb = dbfs(event.getRmsHigh());

        // One pass over the frame for both the true sample peak (what a DAW meter shows) and the
        // saturation check. Math.abs on Short.MIN_VALUE is a no-op, so compare magnitudes as ints.
        byte[] buf = event.getBuffer();
        int len = event.getLength();
        int framePeak = 0;
        for (int i = 0; i + 1 < len; i += 2) {
            int magnitude = Math.abs((int) (short) (((buf[i + 1] & 0xFF) << 8) | (buf[i] & 0xFF)));
            if (magnitude > framePeak) framePeak = magnitude;
        }
        if (framePeak >= CLIP_THRESHOLD) {
            clipExpiry = System.currentTimeMillis() + CLIP_HOLD_MS;
        }
        // The hold falls back toward the bottom of the scale rather than toward negative infinity,
        // so a long silence parks the cap on the floor segment instead of running off the scale.
        peakHoldDb = Math.max(dbfs(framePeak), Math.max(peakHoldDb - PEAK_DECAY_DB, SCALE_MIN_DBFS));

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
     * Converts a linear amplitude in 16-bit sample units to decibels relative to full scale.
     *
     * @return {@code 20*log10(amplitude / 32768)}, or {@link Double#NEGATIVE_INFINITY} at silence.
     */
    private static double dbfs(double amplitude) {
        return amplitude <= 0 ? Double.NEGATIVE_INFINITY
                : 20.0 * Math.log10(amplitude / FULL_SCALE_AMPLITUDE);
    }

    /**
     * @return fraction of the meter height for {@code db}, clamped to the drawn scale.
     */
    private static double scalePos(double db) {
        if (db <= SCALE_MIN_DBFS) return 0;
        return Math.min(1.0, (db - SCALE_MIN_DBFS) / -SCALE_MIN_DBFS);
    }

    /**
     * @return {@code db} rendered as a whole-number dBFS readout, e.g. {@code "-22"}.
     */
    private String formatDbfs(double db) {
        if (Double.isInfinite(db)) {
            return getFont().canDisplay(INFINITY) ? MINUS_INFINITY : MINUS_INFINITY_FALLBACK;
        }
        return String.valueOf(Math.round(db));
    }

    /**
     * @return zone colour for a segment at {@code db}, per the app's mic legend: red below the
     * gate (closed, not sending), amber in a narrow band straddling the gate (marginal - opens
     * intermittently), green from there up to CLIP (open), red again at/above CLIP (too hot).
     */
    private Color zoneColor(double db) {
        if (db < gateDb - MARGINAL_BAND_DB) return HudPalette.HUD_COLOR_ROLE_DANGER;
        if (db < gateDb + MARGINAL_BAND_DB) return HudPalette.HUD_COLOR_ROLE_WARNING;
        if (db < CLIP_DBFS) return HudPalette.HUD_COLOR_ROLE_SUCCESS;
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

            // Absolute dBFS scale, linear in decibels: SCALE_MIN_DBFS at the bottom, 0 dBFS (digital
            // full scale) at the top. Fixed, so the meter reads the same as any other dBFS meter
            // regardless of how the voice gate happens to be calibrated.
            double rmsDb = currentRmsDb;
            double peakDb = peakHoldDb;
            double peakPos = scalePos(peakDb);

            int peakSeg = (int) Math.round(peakPos * n) - 1;

            for (int i = 0; i < n; i++) {
                int y = bottom - (i + 1) * segH - i * segGap;
                double segDb = SCALE_MIN_DBFS + (i + 0.5) / n * -SCALE_MIN_DBFS;
                Color zone = zoneColor(segDb);

                // Live column (frame RMS): lit in zone colour; unlit a dark slab with a faint
                // zone-tint top edge so the colour scale is hinted even where the level has not reached.
                if (segDb <= rmsDb) {
                    g2.setColor(zone);
                    g2.fillRect(liveX, y, liveW, segH);
                } else {
                    g2.setColor(HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
                    g2.fillRect(liveX, y, liveW, segH);
                    g2.setColor(mix(zone, HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND, 0.80));
                    g2.fillRect(liveX, y, liveW, Math.min(segH, 2));
                }

                // Peak-trail column (true sample peak): grey trail up to the held peak, bright cap
                // on top (red cap when clipping); dark above the peak.
                Color peakColor;
                if (i <= peakSeg) {
                    peakColor = (i == peakSeg)
                            ? ((clipping || peakDb >= CLIP_DBFS) ? HudPalette.HUD_COLOR_ROLE_DANGER : HudPalette.HUD_COLOR_ROLE_BUTTON_TEXT)
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
            drawRail(g2, fmK, "FLOOR " + formatDbfs(noiseFloorDb), noiseFloorDb,
                    top, bottom, scaleW, meterRight, HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
            drawRail(g2, fmK, "GATE " + formatDbfs(gateDb), gateDb,
                    top, bottom, scaleW, meterRight, HudPalette.HUD_COLOR_ROLE_INFORMATION);
            drawRail(g2, fmK, "CLIP " + formatDbfs(CLIP_DBFS), CLIP_DBFS,
                    top, bottom, scaleW, meterRight, HudPalette.HUD_COLOR_ROLE_DANGER);

            // Scale end anchors, each drawn only when the nearest rail label is clear of it: the CLIP
            // rail sits close under the top and the FLOOR rail can sit close over the bottom.
            g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
            int clipY = (int) (bottom - scalePos(CLIP_DBFS) * meterH);
            if (clipY - top > fmK.getHeight()) {
                String maxTag = "0";
                g2.drawString(maxTag, scaleW - fmK.stringWidth(maxTag) - HudPalette.HUD_GAP, top + fmK.getAscent());
            }
            int floorY = (int) (bottom - scalePos(noiseFloorDb) * meterH);
            if (bottom - floorY > fmK.getHeight()) {
                String minTag = formatDbfs(SCALE_MIN_DBFS);
                g2.drawString(minTag, scaleW - fmK.stringWidth(minTag) - HudPalette.HUD_GAP, bottom - 1);
            }

            // Peak readout tag pinned just right of the peak column, at the cap height.
            int peakY = (int) (bottom - peakPos * meterH);
            g2.setColor(HudPalette.HUD_COLOR_ROLE_BUTTON_TEXT);
            String peakTag = "PEAK " + formatDbfs(peakDb);
            int tagX = Math.min(meterRight + HudPalette.HUD_GAP, w - fmK.stringWidth(peakTag));
            g2.drawString(peakTag, tagX, Math.max(top + fmK.getAscent(), peakY));

            // Big current-value readout + status below the columns. HOT is driven by the sample peak
            // (that is what saturates); the gate states are driven by the RMS the gate itself sees.
            Color statusColor;
            String status;
            if (clipping || peakDb >= CLIP_DBFS) {
                statusColor = HudPalette.HUD_COLOR_ROLE_DANGER;
                status = "HOT";
            } else if (rmsDb >= gateDb + MARGINAL_BAND_DB) {
                statusColor = HudPalette.HUD_COLOR_ROLE_SUCCESS;
                status = "OPEN";
            } else if (rmsDb >= gateDb - MARGINAL_BAND_DB) { statusColor = HudPalette.HUD_COLOR_ROLE_WARNING; status = "MARGINAL"; } else {
                statusColor = HudPalette.HUD_COLOR_ROLE_DANGER;
                status = "CLOSED";
            }

            // Big value + its unit, laid out as one centred group so the pair stays centred whatever
            // the digit count. The unit lives here because the top of the scale is too crowded.
            int center = scaleW + (meterRight - scaleW) / 2;
            Font bigFont = getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_STAT_LG);
            Font smallFont = getFont().deriveFont(HudPalette.HUD_FONT_READOUT_KEY);
            FontMetrics fmBig = g2.getFontMetrics(bigFont);
            FontMetrics fmS = g2.getFontMetrics(smallFont);

            String num = formatDbfs(rmsDb);
            String unit = "dBFS";
            int groupW = fmBig.stringWidth(num) + HudPalette.HUD_GAP_TIGHT + fmS.stringWidth(unit);
            int numX = center - groupW / 2;
            int baseline = bottom + fmBig.getAscent();

            g2.setFont(bigFont);
            g2.setColor(statusColor);
            g2.drawString(num, numX, baseline);

            g2.setFont(smallFont);
            g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
            g2.drawString(unit, numX + fmBig.stringWidth(num) + HudPalette.HUD_GAP_TIGHT, baseline);

            String sub = "RMS · " + status;
            g2.drawString(sub, center - fmS.stringWidth(sub) / 2, bottom + fmBig.getHeight() + fmS.getAscent() - 2);
        } finally {
            g2.dispose();
        }
    }

    /** Draws a horizontal threshold rail across both columns with a left-gutter label. */
    private void drawRail(Graphics2D g2, FontMetrics fm, String label, double db,
                          int top, int bottom, int scaleW, int meterRight, Color color) {
        if (db <= SCALE_MIN_DBFS) return;
        int y = (int) (bottom - scalePos(db) * (bottom - top));
        g2.setColor(color);
        g2.fillRect(scaleW, y, meterRight - scaleW, HudPalette.HUD_BORDER_THICKNESS);
        g2.drawString(label, scaleW - fm.stringWidth(label) - HudPalette.HUD_GAP, y + fm.getAscent() / 2);
    }
}
