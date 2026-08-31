package elite.intel.ui.widget;

import elite.intel.ui.theme.HudGlyphs;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * The media transport artwork: rewind, play, pause, stop and forward, sized and tinted for a HUD button.
 * <p>
 * The four supplied images are discs with the symbol knocked out of them - the triangle, the square and the
 * chevrons are holes, not paint - so the disc takes the tint and the symbol shows the button's own fill
 * through it. That is why they are tinted rather than drawn as supplied: a fixed orange disc would sink into
 * the primary button's orange fill the moment it was pressed.
 * <p>
 * WHY pause is drawn rather than loaded: there is no pause image, and a transport row that fell back to the
 * word PAUSE between four discs would break the row every time playback started. It is drawn at the same
 * 512px the artwork ships at and reduced the same way, so it carries the same weight as its neighbours.
 */
public final class HudTransportIcons {

    /**
     * The side the supplied artwork is drawn at, and so the side the pause disc is drawn at.
     */
    private static final int ARTWORK_SIDE = 512;

    /**
     * Pause bar geometry, as a fraction of the disc. Measured off the supplied stop artwork so the two bars
     * fill the same window in the disc that its square does.
     */
    private static final float BAR_WIDTH = 0.1175f;
    private static final float BAR_GAP = 0.125f;
    private static final float BAR_HEIGHT = 0.345f;

    private static final String REWIND = "/images/rewind-button.png";
    private static final String PLAY = "/images/play-button.png";
    private static final String STOP = "/images/stop-button.png";
    private static final String FORWARD = "/images/forward-button.png";

    private HudTransportIcons() {
    }

    public static ImageIcon rewind(int side, Color tint) {
        return tinted(REWIND, side, tint);
    }

    public static ImageIcon play(int side, Color tint) {
        return tinted(PLAY, side, tint);
    }

    public static ImageIcon stop(int side, Color tint) {
        return tinted(STOP, side, tint);
    }

    public static ImageIcon forward(int side, Color tint) {
        return tinted(FORWARD, side, tint);
    }

    public static ImageIcon pause(int side, Color tint) {
        Image reduced = pauseArtwork().getScaledInstance(side, side, Image.SCALE_SMOOTH);
        return HudGlyphs.tintIcon(new ImageIcon(reduced), side, side, tint);
    }

    /**
     * Reduces one of the supplied discs to {@code side} and recolours it.
     * <p>
     * The reduction goes through {@code SCALE_SMOOTH} first: these are 512px images landing in a button
     * around twenty pixels across, and the tint pass alone would sample rather than average them, which
     * frays the disc's edge.
     */
    private static ImageIcon tinted(String resource, int side, Color tint) {
        return HudGlyphs.tintIcon(HudGlyphs.scaledIcon(HudTransportIcons.class, resource, side), side, side, tint);
    }

    /**
     * The pause disc at full artwork size: a filled circle with two bars cleared out of it, which is the
     * same figure-as-hole construction the supplied images use.
     */
    private static BufferedImage pauseArtwork() {
        BufferedImage disc = new BufferedImage(ARTWORK_SIDE, ARTWORK_SIDE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = disc.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Any opaque colour will do - the tint pass replaces it and keeps only the alpha.
            g2.setColor(Color.WHITE);
            g2.fillOval(0, 0, ARTWORK_SIDE, ARTWORK_SIDE);

            int barWidth = Math.round(ARTWORK_SIDE * BAR_WIDTH);
            int barHeight = Math.round(ARTWORK_SIDE * BAR_HEIGHT);
            int gap = Math.round(ARTWORK_SIDE * BAR_GAP);
            int left = (ARTWORK_SIDE - (2 * barWidth + gap)) / 2;
            int top = (ARTWORK_SIDE - barHeight) / 2;

            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(left, top, barWidth, barHeight);
            g2.fillRect(left + barWidth + gap, top, barWidth, barHeight);
        } finally {
            g2.dispose();
        }
        return disc;
    }
}
