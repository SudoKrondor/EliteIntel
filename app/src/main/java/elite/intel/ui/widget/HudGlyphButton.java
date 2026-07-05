package elite.intel.ui.widget;

import elite.intel.ui.theme.HudGlyphs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

import static elite.intel.ui.theme.HudPalette.HUD_ICON_TABLE;
import static elite.intel.ui.theme.HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT;

/**
 * Icon-only clickable HUD affordance: paints one {@link HudGlyphs.Painter} vector glyph over a transparent
 * background, tinted {@code restTint} at rest and {@code hoverTint} while hovered, and fires {@code onClick} on
 * a left press-release that ends inside the button. The single owner of the small "glyph button in a header or
 * toolbar" pattern (the dialog close {@code x}, a section header action) - no component paints its own copy.
 * <p>
 * Footprint is the compact control square ({@code HUD_TABLE_ROW_HEIGHT_COMPACT}); the glyph is drawn at
 * {@code HUD_ICON_TABLE}, centred. All sizes are palette tokens, per the HUD canon (§3/§13).
 */
public final class HudGlyphButton extends JComponent {

    private final HudGlyphs.Painter painter;
    private final Color restTint;
    private final Color hoverTint;
    private final Runnable onClick;

    private boolean hover;
    private boolean armed;

    /**
     * @param painter   glyph to paint (a {@code HudGlyphs::paintHud*Glyph} method reference)
     * @param restTint  glyph colour at rest
     * @param hoverTint glyph colour while hovered
     * @param tooltip   localized tooltip, or {@code null} for none
     * @param onClick   action run on a completed left click
     */
    public HudGlyphButton(HudGlyphs.Painter painter, Color restTint, Color hoverTint, String tooltip, Runnable onClick) {
        // Fail fast on the paint-critical collaborators so a null surfaces at construction, not later inside
        // paintComponent. onClick stays optional (a decorative glyph is valid); its use site null-guards it.
        this.painter = Objects.requireNonNull(painter, "painter");
        this.restTint = Objects.requireNonNull(restTint, "restTint");
        this.hoverTint = Objects.requireNonNull(hoverTint, "hoverTint");
        this.onClick = onClick;
        setOpaque(false);
        setPreferredSize(new Dimension(HUD_TABLE_ROW_HEIGHT_COMPACT, HUD_TABLE_ROW_HEIGHT_COMPACT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (tooltip != null) {
            setToolTipText(tooltip);
        }
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                armed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    armed = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                boolean fire = armed && SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint());
                armed = false;
                if (fire && onClick != null) {
                    onClick.run();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int gs = HUD_ICON_TABLE;
            int gx = (getWidth() - gs) / 2;
            int gy = (getHeight() - gs) / 2;
            painter.paint(g2, gx, gy, gs, gs, hover ? hoverTint : restTint);
        } finally {
            g2.dispose();
        }
    }
}
