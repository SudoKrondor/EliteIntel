package elite.intel.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * HUD icon and glyph primitives: image loading/tinting helpers, flat vector glyph painters
 * (arrows, info, close, vertical ellipsis, warning, checkbox marker) and the {@link Icon}
 * factories that wrap them. Split out of {@link AppTheme} so the painting code lives apart
 * from palette tokens and component factories.
 */
public final class HudGlyphs {

    private HudGlyphs() {
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
     * Paints the HUD checkbox marker: a 2-px double-outline square box, with a centred
     * filled inner square when {@code filled} is true. Geometry matches the legacy inline
     * drawing in {@link HudCheckBox}. Caller is responsible for antialiasing hints.
     *
     * @param g2          graphics context (not disposed by this method)
     * @param x           left edge of the marker box
     * @param y           top edge of the marker box
     * @param size        outer marker size in px (e.g. {@code HUD_TABLE_ROW_HEIGHT_COMPACT - 2*HUD_PADDING_SMALL})
     * @param markerColor colour of outline and inner fill
     * @param filled      draw the inner filled square (ON state) when true
     */
    public static void paintHudCheckMarker(Graphics2D g2, int x, int y, int size,
                                           Color markerColor, boolean filled) {
        g2.setColor(markerColor);
        g2.drawRect(x,     y,     size - 1, size - 1);
        g2.drawRect(x + 1, y + 1, size - 3, size - 3);
        if (filled) {
            int innerSize = size / 2;
            int innerX = x + (size - innerSize) / 2;
            int innerY = y + (size - innerSize) / 2;
            g2.fillRect(innerX, innerY, innerSize, innerSize);
        }
    }
}
