package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Reusable titled HUD section/card for grouping related controls and telemetry.
 */
public class HudSection extends HudPanel {

    /** Shared horizontal inset for header text and decorative dots — mirrors title left ↔ dots right. */
    private static final int HEADER_H_INSET = 8;

    private final JPanel body;
    private final Variant sectionVariant;
    private final JLabel headerLabel;
    private JComponent footer;
    private Color footerBackground = HudPalette.HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND;

    /**
     * Creates a titled section with a supplied body layout.
     *
     * @param title localized section title
     * @param bodyLayout layout manager used by the content body
     */
    public HudSection(String title, LayoutManager bodyLayout) {
        this(title, bodyLayout, Variant.FRAMED);
    }

    /**
     * Creates a titled section with a supplied body layout and visual framing strength.
     *
     * @param title localized section title
     * @param bodyLayout layout manager used by the content body
     * @param variant visual framing strength for the section surface
     */
    public HudSection(String title, LayoutManager bodyLayout, Variant variant) {
        this(title, bodyLayout, variant, HudPalette.HUD_GAP);
    }

    /**
     * Creates a titled section with explicit visual framing and title-to-body gap.
     *
     * @param title localized section title
     * @param bodyLayout layout manager used by the content body
     * @param variant visual framing strength for the section surface
     * @param bodyGap vertical gap between the title and the content body
     */
    public HudSection(String title, LayoutManager bodyLayout, Variant variant, int bodyGap) {
        this(title, bodyLayout, variant, bodyGap, HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION);
    }

    /**
     * Creates a titled section with explicit framing, spacing, and frame colour.
     *
     * @param title localized section title
     * @param bodyLayout layout manager used by the content body
     * @param variant visual framing strength for the section surface
     * @param bodyGap vertical gap between the title and the content body
     * @param borderColor restrained frame colour used for framed sections
     */
    public HudSection(String title, LayoutManager bodyLayout, Variant variant, int bodyGap, Color borderColor) {
        // HudSection owns the titled-card frame; HudPanel only supplies the dark rounded base fill.
        super(new BorderLayout(0, 0), HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION, Variant.FLAT);
        sectionVariant = variant == null ? Variant.FRAMED : variant;
        if (sectionVariant == Variant.FLAT) {
            setPaintBackgroundFill(false);
        }
        setBorder(sectionVariant == Variant.FLAT
                ? AppTheme.hudFlatBorder()
                : BorderFactory.createEmptyBorder(1, 1, 1, 1));
        putClientProperty(AppTheme.HUD_CARD_BORDER_COLOR,
                borderColor == null ? HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION : borderColor);

        headerLabel = AppTheme.hudSectionLabel(title == null ? "" : title.toUpperCase());
        JPanel header = AppTheme.transparentPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(3, HEADER_H_INSET, 4, HEADER_H_INSET));
        header.add(headerLabel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        int topPadding = sectionVariant == Variant.FRAMED ? Math.max(3, bodyGap) : Math.max(0, bodyGap);
        body = AppTheme.transparentPanel(bodyLayout);
        body.setBorder(sectionVariant == Variant.FRAMED
                ? BorderFactory.createEmptyBorder(topPadding, 6, 6, 6)
                : BorderFactory.createEmptyBorder(topPadding, 0, 0, 0));
        add(body, BorderLayout.CENTER);
    }

    /**
     * Creates a titled section without an additional visible card frame for nested HUD layouts.
     */
    public static HudSection flat(String title, LayoutManager bodyLayout) {
        return new HudSection(title, bodyLayout, Variant.FLAT);
    }

    /**
     * Creates a compact flat section for dense cockpit screens and data panels.
     */
    public static HudSection compactFlat(String title, LayoutManager bodyLayout) {
        return new HudSection(title, bodyLayout, Variant.FLAT, 3);
    }

    /**
     * Creates a compact card section with a restrained structural frame for major HUD modules.
     */
    public static HudSection compactCard(String title, LayoutManager bodyLayout) {
        return new HudSection(title, bodyLayout, Variant.FRAMED, 3);
    }

    /**
     * Returns the mutable content body for adding section controls.
     */
    public JPanel body() {
        return body;
    }

    /**
     * Sets an optional full-width footer strip inside the same rounded section card.
     *
     * @param footer component shown as the section footer, or {@code null} to remove it
     */
    public void setFooter(JComponent footer) {
        setFooter(footer, HudPalette.HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND);
    }

    /**
     * Sets an optional full-width footer strip with a dedicated background surface.
     *
     * @param footer component shown as the section footer, or {@code null} to remove it
     * @param background background surface painted behind the footer strip
     */
    public void setFooter(JComponent footer, Color background) {
        if (this.footer != null) {
            remove(this.footer);
        }
        this.footer = footer;
        footerBackground = background == null ? HudPalette.HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND : background;
        if (footer != null) {
            add(footer, BorderLayout.SOUTH);
        }
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (sectionVariant == Variant.FLAT) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                Component header = getComponentCount() > 0 ? getComponent(0) : null;
                if (header != null) {
                    Rectangle bounds = header.getBounds();
                    g2.setColor(HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION);
                    g2.drawLine(1, bounds.y + bounds.height,
                                Math.max(1, w - 2), bounds.y + bounds.height);
                }
            } finally {
                g2.dispose();
            }
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int arc = HudPalette.HUD_PANEL_ARC;

            Shape originalClip = g2.getClip();
            g2.setClip(new RoundRectangle2D.Float(1, 1, Math.max(0, w - 2), Math.max(0, h - 2), arc, arc));

            Component header = getComponentCount() > 0 ? getComponent(0) : null;
            if (header != null) {
                Rectangle bounds = header.getBounds();
                g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND);
                g2.fillRect(1, 1, Math.max(0, w - 2), Math.max(0, bounds.height));
                g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER);
                g2.drawLine(1, bounds.y + bounds.height, Math.max(1, w - 2), bounds.y + bounds.height);

            }

            if (footer != null) {
                Rectangle bounds = footer.getBounds();
                g2.setColor(footerBackground);
                g2.fillRect(1, bounds.y, Math.max(0, w - 2), Math.max(0, h - bounds.y - 1));
                g2.setColor(HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION);
                g2.drawLine(1, bounds.y, Math.max(1, w - 2), bounds.y);
            }
            g2.setClip(originalClip);

            Color borderColor = (Color) getClientProperty(AppTheme.HUD_CARD_BORDER_COLOR);
            g2.setColor(borderColor == null ? HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION : borderColor);
            g2.drawRoundRect(0, 0, Math.max(0, w - 1), Math.max(0, h - 1), arc, arc);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension preferred = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, preferred.height);
    }

    /**
     * Paints the decorative three-dot accent on the right side of the header strip.
     * Suppressed automatically when the section is too narrow or dots would overlap the title.
     */
    private void drawHeaderDots(Graphics2D g2, int panelWidth, Rectangle headerBounds) {
        final int dotD    = 3;
        final int dotGap  = 5;
        final int groupW  = 3 * dotD + 2 * dotGap; // 19 px total
        // mirrors actual title left edge: section border inset + shared header horizontal inset
        final int rightPad = getInsets().left + HEADER_H_INSET;
        final int safetyGap = 12;

        if (panelWidth < 320) return;

        int startX = panelWidth - rightPad - groupW;

        // Hide if dots would collide with the title text.
        int titleRight = headerLabel.getX() + headerLabel.getWidth() + safetyGap;
        if (startX < titleRight) return;

        int centerY = headerBounds.y + headerBounds.height / 2;
        int dotY    = centerY - dotD / 2;

        g2.setColor(headerLabel.getForeground());
        for (int i = 0; i < 3; i++) {
            g2.fillOval(startX + i * (dotD + dotGap), dotY, dotD, dotD);
        }
    }
}
