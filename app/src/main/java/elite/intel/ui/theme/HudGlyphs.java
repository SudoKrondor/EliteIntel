package elite.intel.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * HUD icon and glyph primitives: image loading/tinting helpers, flat vector glyph painters
 * (arrows, info, close, vertical ellipsis, warning, checkbox marker, text caret) and the {@link Icon}
 * factories that wrap them. Split out of {@link AppTheme} so the painting code lives apart
 * from palette tokens and component factories.
 */
public final class HudGlyphs {

    private HudGlyphs() {
    }

    /**
     * A flat vector glyph painter: draws a single glyph tinted {@code color} within the box {@code (x, y, w, h)},
     * without disposing {@code g2}. Every {@code paintHud*Glyph} method matches this shape, so they can be passed
     * as method references (e.g. to {@code HudGlyphButton}) instead of each caller hardcoding a glyph.
     */
    @FunctionalInterface
    public interface Painter {
        void paint(Graphics2D g2, int x, int y, int w, int h, Color color);
    }

    /** Visual anchor used to align the prompt caret with the conversation's chevron marker. */
    private static final String CARET_ALIGNMENT_GLYPH = "»";

    /**
     * Draws the blinking typewriter caret centered on the visual bounds of the conversation prompt marker.
     * This avoids treating the font ascent as the marker's visible height, which would raise the caret above it.
     *
     * @param g2        graphics context (not disposed by this method)
     * @param x         caret left edge
     * @param baselineY text baseline shared with the prompt or message line
     * @param fm        metrics of the font currently used to render the line
     * @param color     caret colour
     */
    public static void paintHudTextCaret(Graphics2D g2, int x, int baselineY, FontMetrics fm, Color color) {
        Font font = fm.getFont();
        GlyphVector glyph = font.createGlyphVector(g2.getFontRenderContext(), CARET_ALIGNMENT_GLYPH);
        Rectangle2D visualBounds = glyph.getVisualBounds();
        int caretHeight = Math.max(1, fm.getAscent() - fm.getDescent());
        int caretY = baselineY + (int) Math.round(visualBounds.getCenterY() - caretHeight / 2.0);
        g2.setColor(color);
        g2.fillRect(x, caretY, HudPalette.HUD_CARET_WIDTH, caretHeight);
    }

    /**
     * Loads and scales an image resource relative to the supplied owner class.
     */
    public static ImageIcon scaledIcon(Class<?> owner, String resource, int size) {
        return scaledIcon(owner, resource, size, size);
    }

    /**
     * Loads and scales an image resource relative to the supplied owner class.
     */
    public static ImageIcon scaledIcon(Class<?> owner, String resource, int width, int height) {
        return new ImageIcon(
                new ImageIcon(java.util.Objects.requireNonNull(owner.getResource(resource)))
                        .getImage()
                        .getScaledInstance(width, height, Image.SCALE_SMOOTH)
        );
    }

    /**
     * Tints a monochrome glyph icon to the given colour using {@link AlphaComposite#SRC_IN},
     * preserving per-pixel alpha. Only correct for single-colour masks on a transparent background.
     *
     * @param src   source icon (any size)
     * @param w     output width in pixels
     * @param h     output height in pixels
     * @param color replacement colour
     * @return new {@link ImageIcon} backed by a {@link BufferedImage}
     */
    public static ImageIcon tintIcon(ImageIcon src, int w, int h, Color color) {
        BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buf.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(src.getImage(), 0, 0, w, h, null);
            g2.setComposite(AlphaComposite.SrcIn);
            g2.setColor(color);
            g2.fillRect(0, 0, w, h);
        } finally {
            g2.dispose();
        }
        return new ImageIcon(buf);
    }

    /**
     * Returns a copy of {@code src} composited at the given alpha (0 = transparent, 1 = opaque).
     * Use to produce a visually receded version of a colourful icon without recolouring it.
     *
     * @param src   source icon (not modified)
     * @param alpha opacity, clamped to [0, 1] by the compositing pipeline
     * @return new {@link ImageIcon} backed by a {@link BufferedImage}
     */
    public static ImageIcon dimIcon(ImageIcon src, float alpha) {
        int w = src.getIconWidth();
        int h = src.getIconHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        try {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(src.getImage(), 0, 0, w, h, null);
        } finally {
            g2.dispose();
        }
        return new ImageIcon(result);
    }

    /**
     * Draws the flat down triangle used by HUD combo boxes, centred within (x, y, w, h).
     * Shared with the combo box UI delegate arrow button and table cell renderers.
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color fill colour for the triangle
     */
    public static void paintHudArrowDown(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Local geometry - not a colour/font/height token.
        int aw = 8;
        int ah = 5;
        int ax = x + (w - aw) / 2;
        int ay = y + (h - ah) / 2;
        g2.setColor(color);
        g2.fillPolygon(
                new int[]{ax, ax + aw, ax + aw / 2},
                new int[]{ay, ay,      ay + ah},
                3);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Draws a left-pointing filled triangle centred within the box (x, y, w, h).
     * Pair to {@link #paintHudArrowDown}; used by the discrete stepper ({@link HudStepper}).
     */
    public static void paintHudArrowLeft(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int aw = 5;
        int ah = 8;
        int ax = x + (w - aw) / 2;
        int ay = y + (h - ah) / 2;
        g2.setColor(color);
        g2.fillPolygon(
                new int[]{ax, ax + aw, ax + aw},
                new int[]{ay + ah / 2, ay, ay + ah},
                3);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Draws a right-pointing filled triangle centred within the box (x, y, w, h).
     * Pair to {@link #paintHudArrowDown}; used by the discrete stepper ({@link HudStepper}).
     */
    public static void paintHudArrowRight(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int aw = 5;
        int ah = 8;
        int ax = x + (w - aw) / 2;
        int ay = y + (h - ah) / 2;
        g2.setColor(color);
        g2.fillPolygon(
                new int[]{ax, ax + aw, ax},
                new int[]{ay, ay + ah / 2, ay + ah},
                3);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Draws an up-pointing filled triangle centred within the box (x, y, w, h).
     * Pair to {@link #paintHudArrowDown}; used for move-up affordances.
     */
    public static void paintHudArrowUp(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int aw = 8;
        int ah = 5;
        int ax = x + (w - aw) / 2;
        int ay = y + (h - ah) / 2;
        g2.setColor(color);
        g2.fillPolygon(
                new int[]{ax, ax + aw, ax + aw / 2},
                new int[]{ay + ah, ay + ah, ay},
                3);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Draws a lowercase "i" glyph (dot + stem) centred within the box (x, y, w, h).
     * All geometry is relative - no hardcoded pixel sizes except proportional formulas.
     * Suitable for info-affording controls; caller chooses colour based on component state.
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color fill colour for both dot and stem
     */
    public static void paintHudInfoGlyph(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int stemW = Math.max(2, w / 5);
        int stemH = (int) Math.round(h * 0.55);
        int gap   = Math.max(1, stemW / 2);
        int totalH = stemW + gap + stemH;
        int gx = x + (w - stemW) / 2;
        int gy = y + (h - totalH) / 2;
        g2.setColor(color);
        g2.fillRect(gx, gy,              stemW, stemW); // dot
        g2.fillRect(gx, gy + stemW + gap, stemW, stemH); // stem
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Draws an x glyph (two crossing diagonals) centred within the box (x, y, w, h).
     * All geometry is proportional - no hardcoded pixel sizes except the proportional formulas.
     * Suitable for close/dismiss affordances; caller chooses colour based on hover state.
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color fill colour for both diagonals
     */
    public static void paintHudCloseGlyph(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Stroke oldStroke = g2.getStroke();
        float strokeW = Math.max(2f, w / 8f);
        g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        int pad = (int) (w * 0.28);
        int x1 = x + pad,     y1 = y + pad;
        int x2 = x + w - pad, y2 = y + h - pad;
        g2.setColor(color);
        g2.drawLine(x1, y1, x2, y2); // top-left -> bottom-right
        g2.drawLine(x1, y2, x2, y1); // bottom-left -> top-right
        g2.setStroke(oldStroke);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Draws the window "minimize" glyph within (x, y, w, h): one horizontal bar across the lower part of the box,
     * at the close glyph's padding and stroke so the three window buttons read as a set.
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color stroke colour
     */
    public static void paintHudMinimizeGlyph(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Stroke oldStroke = g2.getStroke();
        float strokeW = Math.max(2f, w / 8f);
        g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        int pad = (int) (w * 0.28);
        int barY = y + h - pad;
        g2.setColor(color);
        g2.drawLine(x + pad, barY, x + w - pad, barY);
        g2.setStroke(oldStroke);
    }

    /**
     * Draws the window "maximize" glyph within (x, y, w, h): one square outline, at the close glyph's padding.
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color stroke colour
     */
    public static void paintHudMaximizeGlyph(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(Math.max(1.5f, w / 10f), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        int pad = (int) (w * 0.28);
        g2.setColor(color);
        g2.drawRect(x + pad, y + pad, w - 2 * pad, h - 2 * pad);
        g2.setStroke(oldStroke);
    }

    /**
     * Draws the window "restore" glyph within (x, y, w, h): two overlapping square outlines, the back one up and to
     * the right, showing only its two edges that are not behind the front one.
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color stroke colour
     */
    public static void paintHudRestoreGlyph(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(Math.max(1.5f, w / 10f), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        int pad = (int) (w * 0.28);
        // WHY min: the squares stay square in a box that is not.
        int side = Math.min(w, h) - 2 * pad;
        int offset = Math.max(2, side / 4);
        int front = side - offset;
        int fx = x + pad, fy = y + pad + offset;
        int bx = fx + offset, by = y + pad;
        g2.setColor(color);
        g2.drawRect(fx, fy, front, front);
        g2.drawLine(bx, by, bx + front, by);               // back square, top edge
        g2.drawLine(bx + front, by, bx + front, by + front); // back square, right edge
        g2.setStroke(oldStroke);
    }

    /**
     * Paints the "report a bug" glyph centred within (x, y, w, h): {@code images/bug.png}, tinted {@code color} the
     * way the vector glyphs are, so it follows the theme and the hover state.
     * <p>
     * WHY a raster: the bug's legs and body do not reduce to a few proportional strokes that still read as a bug
     * at header size. The image is a single-colour mask, which is what {@link #tintIcon} needs.
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color tint for the bug
     */
    public static void paintHudBugGlyph(Graphics2D g2, int x, int y, int w, int h, Color color) {
        int size = Math.min(w, h);
        Image bug = BUG_GLYPHS.computeIfAbsent(new TintedGlyph(size, color.getRGB()),
                key -> tintIcon(scaledIcon(HudGlyphs.class, BUG_IMAGE, size), size, size, color).getImage());
        g2.drawImage(bug, x + (w - size) / 2, y + (h - size) / 2, null);
    }

    private static final String BUG_IMAGE = "/images/bug.png";

    /** One tinted bug per size and colour, painted on the EDT only: a theme has a handful of each. */
    private static final Map<TintedGlyph, Image> BUG_GLYPHS = new HashMap<>();

    private record TintedGlyph(int size, int rgb) {
    }

    /**
     * Draws the flat "copy" glyph centred within (x, y, w, h): two overlapping document outlines. Geometry is
     * proportional so it scales with the box; caller chooses colour by state.
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color stroke colour for both document outlines
     */
    public static void paintHudCopyGlyph(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Stroke oldStroke = g2.getStroke();
        float strokeW = Math.max(2f, w / 9f);
        g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(color);

        int backX = x + Math.round(w * 0.30f);
        int backY = y + Math.round(h * 0.14f);
        int backW = Math.round(w * 0.52f);
        int backH = Math.round(h * 0.62f);
        int frontX = x + Math.round(w * 0.14f);
        int frontY = y + Math.round(h * 0.30f);
        int frontW = Math.round(w * 0.52f);
        int frontH = Math.round(h * 0.58f);
        g2.drawRect(backX, backY, backW, backH);
        g2.drawRect(frontX, frontY, frontW, frontH);

        g2.setStroke(oldStroke);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Draws the flat "clear / delete" (trash can) glyph centred within (x, y, w, h): a lid with a small handle
     * tab above a slightly tapered open can body (no fill slats, to keep the same light stroke weight as the
     * neighbouring save glyph). All geometry is proportional so it scales with the box; caller chooses colour by
     * state.
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color stroke colour for the can
     */
    public static void paintHudTrashGlyph(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Stroke oldStroke = g2.getStroke();
        float strokeW = Math.max(2f, w / 9f);
        g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(color);

        int cx = x + w / 2;
        int lidY = y + Math.round(h * 0.30f);
        g2.drawLine(x + Math.round(w * 0.18f), lidY, x + w - Math.round(w * 0.18f), lidY); // lid

        int handleHalf = Math.round(w * 0.12f);
        int handleTop = y + Math.round(h * 0.19f);
        g2.drawLine(cx - handleHalf, handleTop, cx + handleHalf, handleTop);               // handle top
        g2.drawLine(cx - handleHalf, handleTop, cx - handleHalf, lidY);                    // handle left
        g2.drawLine(cx + handleHalf, handleTop, cx + handleHalf, lidY);                    // handle right

        int bodyTop = lidY + Math.round(h * 0.05f);
        int bodyBottom = y + Math.round(h * 0.82f);
        int topInset = Math.round(w * 0.24f);
        int botInset = Math.round(w * 0.30f);
        g2.drawLine(x + topInset, bodyTop, x + botInset, bodyBottom);                       // left wall (tapered)
        g2.drawLine(x + w - topInset, bodyTop, x + w - botInset, bodyBottom);               // right wall (tapered)
        g2.drawLine(x + botInset, bodyBottom, x + w - botInset, bodyBottom);                // bottom

        g2.setStroke(oldStroke);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Draws a vertical three-dot "more/options" glyph (three stacked squares) centred within
     * the box (x, y, w, h). All geometry is proportional. Flat squares (not rounded dots) per
     * the HUD flat-style rule; caller chooses colour based on component state.
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color fill colour for the three dots
     */
    public static void paintHudVerticalEllipsis(Graphics2D g2, int x, int y, int w, int h, Color color) {
        // Small dots with generous vertical breathing room - proportional so they scale with the box.
        int dot = Math.max(3, Math.round(h / 7f));
        int gap = Math.max(2, Math.round(h / 12f));
        int totalH = dot * 3 + gap * 2;
        int dx = x + (w - dot) / 2;
        int dy = y + (h - totalH) / 2;
        g2.setColor(color);
        for (int i = 0; i < 3; i++) {
            g2.fillRect(dx, dy + i * (dot + gap), dot, dot);
        }
    }

    /**
     * Returns an icon that paints a vertical three-dot glyph using the host component's
     * foreground colour, so it follows the button's state-driven colour. Use for compact
     * field-trailing "more/options/pick" buttons instead of a Unicode "vertical-ellipsis" (see HUD section 13).
     *
     * @param boxSize square icon side in px (typically the field/button height)
     */
    public static Icon verticalEllipsisIcon(int boxSize) {
        return new Icon() {
            @Override public int getIconWidth() { return boxSize; }
            @Override public int getIconHeight() { return boxSize; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                paintHudVerticalEllipsis((Graphics2D) g, x, y, boxSize, boxSize, c.getForeground());
            }
        };
    }

    /**
     * Returns an icon that paints an up-pointing triangle in the host component's foreground
     * colour (so it follows button state). Use for move-up affordances on buttons (see HUD section 13).
     */
    public static Icon arrowUpIcon(int boxSize) {
        return new Icon() {
            @Override public int getIconWidth() { return boxSize; }
            @Override public int getIconHeight() { return boxSize; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                paintHudArrowUp((Graphics2D) g, x, y, boxSize, boxSize, c.getForeground());
            }
        };
    }

    /**
     * Returns an icon that paints a down-pointing triangle in the host component's foreground
     * colour (so it follows button state). Use for move-down affordances on buttons (see HUD section 13).
     */
    public static Icon arrowDownIcon(int boxSize) {
        return new Icon() {
            @Override public int getIconWidth() { return boxSize; }
            @Override public int getIconHeight() { return boxSize; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                paintHudArrowDown((Graphics2D) g, x, y, boxSize, boxSize, c.getForeground());
            }
        };
    }

    /**
     * Draws the HUD warning glyph - a triangle outline with a centred exclamation - within
     * (x, y, w, h). Primitive replacement for the Unicode "warning" (HUD section 13); the caller chooses the
     * colour (state-driven, typically {@link #HUD_COLOR_ROLE_WARNING}).
     *
     * @param g2    graphics context (not disposed by this method)
     * @param x     left edge of the available area
     * @param y     top edge of the available area
     * @param w     width of the available area
     * @param h     height of the available area
     * @param color stroke/fill colour for the glyph
     */
    public static void paintHudWarningGlyph(Graphics2D g2, int x, int y, int w, int h, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Stroke oldStroke = g2.getStroke();
        int size = Math.min(w, h);
        int inset = Math.max(1, Math.round(size * 0.10f));
        int left = x + (w - size) / 2 + inset;
        int right = x + (w + size) / 2 - inset;
        int top = y + (h - size) / 2 + inset;
        int bottom = y + (h + size) / 2 - inset;
        int cx = (left + right) / 2;
        float stroke = Math.max(1.5f, size / 11f);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawPolygon(new int[]{left, right, cx}, new int[]{bottom, bottom, top}, 3);
        // Exclamation mark: vertical bar + dot, centred in the triangle.
        int barTop = top + Math.round((bottom - top) * 0.40f);
        int barBot = top + Math.round((bottom - top) * 0.66f);
        g2.drawLine(cx, barTop, cx, barBot);
        int gap = Math.max(2, Math.round((bottom - top) * 0.08f));
        int dot = Math.max(2, Math.round(stroke));
        g2.fillRect(cx - dot / 2, barBot + gap, dot, dot);
        g2.setStroke(oldStroke);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Returns an icon that paints the HUD warning glyph using the host component's foreground
     * colour. Use as a leading glyph on {@link HudBanner} warnings instead of a Unicode "warning" (section 13).
     *
     * @param boxSize square icon side in px
     */
    public static Icon warningGlyphIcon(int boxSize) {
        return new Icon() {
            @Override public int getIconWidth() { return boxSize; }
            @Override public int getIconHeight() { return boxSize; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                paintHudWarningGlyph((Graphics2D) g, x, y, boxSize, boxSize, c.getForeground());
            }
        };
    }

    /** Base length of a focus corner-mark leg; lengthens by 2 px while focused. */
    private static final int FOCUS_CORNER_MARK = 6;
    /**
     * Corner rounding of the checkbox marker, in px.
     */
    private static final int CHECK_MARKER_ARC = 4;

    /**
     * Paints the diagonal HUD focus corner accent: short 1 px L-marks one pixel inside the top-left
     * and bottom-right corners of the box (0, 0, w, h). The legs lengthen slightly while focused.
     * Used by {@link HudSearchField} framed search variants; the caller chooses the colour and
     * disposes the graphics context.
     *
     * @param g2      graphics context (not disposed by this method)
     * @param w       component width in px
     * @param h       component height in px
     * @param focused whether the host input holds focus (lengthens the marks)
     * @param color   mark colour
     */
    public static void paintHudFocusCornerMarks(Graphics2D g2, int w, int h, boolean focused, Color color) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        // Axis-aligned 1 px lines: AA off so they render as crisp full-brightness pixels.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        int m = focused ? FOCUS_CORNER_MARK + 2 : FOCUS_CORNER_MARK;
        g2.setColor(color);
        g2.drawLine(1, 1, 1 + m, 1);       g2.drawLine(1, 1, 1, 1 + m);           // top-left
        g2.drawLine(w-2-m, h-2, w-2, h-2); g2.drawLine(w-2, h-2-m, w-2, h-2);    // bottom-right
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Paints the HUD checkbox marker: a rounded square, outlined when off and filled with a tick when on.
     * <p>
     * The tick is built in floating point and stroked, rather than snapped to whole pixels, so it
     * stays optically centred at any size. The tick has a colour of its own because it must read
     * against the fill, and a selected table row inverts the marker's colours.
     *
     * @param g2          graphics context (not disposed by this method)
     * @param x           left edge of the marker box
     * @param y           top edge of the marker box
     * @param size        outer marker size in px (e.g. {@code HUD_TABLE_ROW_HEIGHT_COMPACT - 2*HUD_PADDING_SMALL})
     * @param markerColor colour of the outline, and of the fill when {@code checked}
     * @param tickColor   colour of the tick
     * @param checked     fill the box and draw the tick when true
     */
    public static void paintHudCheckMarker(Graphics2D g2, int x, int y, int size,
                                           Color markerColor, Color tickColor, boolean checked) {
        Graphics2D g = (Graphics2D) g2.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setColor(markerColor);

            if (!checked) {
                // Half-pixel inset: a 1 px stroke centred on the box edge would straddle two pixel rows and blur.
                g.setStroke(new BasicStroke(1f));
                g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, size - 1, size - 1,
                        CHECK_MARKER_ARC, CHECK_MARKER_ARC));
                return;
            }

            g.fill(new RoundRectangle2D.Float(x, y, size, size, CHECK_MARKER_ARC, CHECK_MARKER_ARC));

            // Tick proportions are expressed as fractions of the box so the mark scales with it.
            float w = size;
            Path2D.Float tick = new Path2D.Float();
            tick.moveTo(x + w * 0.25f, y + w * 0.52f);
            tick.lineTo(x + w * 0.43f, y + w * 0.70f);
            tick.lineTo(x + w * 0.76f, y + w * 0.32f);

            g.setColor(tickColor);
            g.setStroke(new BasicStroke(Math.max(2f, size / 7f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(tick);
        } finally {
            g.dispose();
        }
    }
}
