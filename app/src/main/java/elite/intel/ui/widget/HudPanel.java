package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Reusable Swing container for cockpit/HUD surfaces with a dark panel fill and accent edge.
 */
public class HudPanel extends JPanel {

    /**
     * Controls how strongly a HUD surface is framed in nested screen compositions.
     */
    public enum Variant {
        FRAMED,
        FLAT
    }

    private final Color accentColor;
    private final Variant variant;
    private boolean paintBackgroundFill = true;

    /**
     * Creates a HUD panel with the standard accent colour.
     *
     * @param layout layout manager used by the panel content
     */
    public HudPanel(LayoutManager layout) {
        this(layout, AppTheme.ACCENT);
    }

    /**
     * Creates a HUD panel with an explicit accent colour for status-specific surfaces.
     *
     * @param layout layout manager used by the panel content
     * @param accentColor colour used for the top accent edge
     */
    public HudPanel(LayoutManager layout, Color accentColor) {
        this(layout, accentColor, Variant.FRAMED);
    }

    /**
     * Creates a HUD panel with explicit accent colour and visual weight.
     *
     * @param layout layout manager used by the panel content
     * @param accentColor colour used for the top accent edge on framed panels
     * @param variant visual framing strength for the surface
     */
    public HudPanel(LayoutManager layout, Color accentColor, Variant variant) {
        super(layout);
        this.accentColor = accentColor == null ? AppTheme.ACCENT : accentColor;
        this.variant = variant == null ? Variant.FRAMED : variant;
        setOpaque(false);
        setBorder(this.variant == Variant.FLAT ? AppTheme.hudFlatBorder() : AppTheme.hudBorder());
    }

    /** Allows subclasses to suppress the dark panel fill (e.g. transparent FLAT sections). */
    protected void setPaintBackgroundFill(boolean value) {
        this.paintBackgroundFill = value;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (paintBackgroundFill) {
                g2.setColor(AppTheme.HUD_PANEL_BG);
                g2.fillRoundRect(0, 0, Math.max(0, w - 1), Math.max(0, h - 1),
                        AppTheme.HUD_PANEL_ARC, AppTheme.HUD_PANEL_ARC);
            }

            if (variant == Variant.FLAT) {
                return;
            }

            g2.setColor(AppTheme.HUD_BORDER_DIM);
            g2.drawRoundRect(0, 0, Math.max(0, w - 1), Math.max(0, h - 1),
                    AppTheme.HUD_PANEL_ARC, AppTheme.HUD_PANEL_ARC);

            // Keep the cockpit treatment lightweight: a single accent edge avoids repaint-heavy effects.
            g2.setColor(accentColor);
            g2.fillRoundRect(AppTheme.HUD_PADDING, 0,
                    Math.max(0, w - AppTheme.HUD_PADDING * 2), 2,
                    AppTheme.HUD_PANEL_ARC, AppTheme.HUD_PANEL_ARC);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
