package elite.intel.ui.view;

import javax.swing.*;
import java.awt.*;

/**
 * HUD-styled checkbox rendered as a full-width cockpit control:
 * a square marker (Graphics2D primitives) + a narrow separator gap + a CAPS label
 * in one solid slab. No LAF rendering is used; super.paintComponent is not called.
 */
public class HudCheckBox extends JCheckBox {

    private final String labelText;

    /**
     * Creates a HUD checkbox.
     *
     * @param label    visible checkbox text (rendered in upper case)
     * @param selected initial selected state
     */
    public HudCheckBox(String label, boolean selected) {
        super(label, selected);
        this.labelText = label != null ? label.toUpperCase() : "";
        super.setText("");
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setBorder(null);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        getModel().addChangeListener(e -> repaint());
    }

    @Override
    public Dimension getPreferredSize() {
        Font f = getFont().deriveFont(Font.BOLD, AppTheme.HUD_FONT_CHECKBOX);
        FontMetrics fm = getFontMetrics(f);
        int markerSize = AppTheme.HUD_TABLE_ROW_HEIGHT_COMPACT - 2 * AppTheme.HUD_PADDING_SMALL;
        int markerZoneW = markerSize + 2 * AppTheme.HUD_PADDING_SMALL;
        int textW = AppTheme.HUD_PADDING
                + (fm != null ? fm.stringWidth(labelText) : 120)
                + AppTheme.HUD_PADDING;
        return new Dimension(markerZoneW + 3 + textW, AppTheme.HUD_TABLE_ROW_HEIGHT_COMPACT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Marker geometry: square sized to row height minus vertical padding
            int markerSize = AppTheme.HUD_TABLE_ROW_HEIGHT_COMPACT - 2 * AppTheme.HUD_PADDING_SMALL;
            int markerZoneW = markerSize + 2 * AppTheme.HUD_PADDING_SMALL;
            int markerX = AppTheme.HUD_PADDING_SMALL;
            int markerY = (h - markerSize) / 2;

            boolean on = isSelected();
            boolean enabled = isEnabled();

            Color fill;
            Color markerColor;
            Color textColor;

            if (!enabled) {
                fill = AppTheme.HUD_TABLE_ROW_HOVER;
                markerColor = AppTheme.HUD_DISABLED;
                textColor = AppTheme.HUD_DISABLED;
            } else if (on) {
                fill = AppTheme.ACCENT;
                markerColor = AppTheme.SEL_FG;
                textColor = AppTheme.SEL_FG;
            } else {
                fill = AppTheme.HUD_TABLE_ROW_HOVER;
                markerColor = AppTheme.HUD_ORANGE_SOFT;
                textColor = AppTheme.FG_MUTED;
            }

            // Slab fill — no border outline on the control itself
            g2.setColor(fill);
            g2.fillRect(0, 0, w, h);

            // Separator: 3-px HUD_BG stripe cutting between marker zone and text zone
            g2.setColor(AppTheme.HUD_BG);
            g2.fillRect(markerZoneW, 0, 3, h);

            // Marker: 2-px square border (two nested drawRects)
            g2.setColor(markerColor);
            g2.drawRect(markerX, markerY, markerSize - 1, markerSize - 1);
            g2.drawRect(markerX + 1, markerY + 1, markerSize - 3, markerSize - 3);

            // Inner filled square — ON state only
            if (on && enabled) {
                int innerSize = markerSize / 2;
                int innerX = markerX + (markerSize - innerSize) / 2;
                int innerY = markerY + (markerSize - innerSize) / 2;
                g2.fillRect(innerX, innerY, innerSize, innerSize);
            }

            // Label text in the text zone, vertically centred
            Font f = getFont().deriveFont(Font.BOLD, AppTheme.HUD_FONT_CHECKBOX);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(textColor);
            g2.drawString(labelText, markerZoneW + 3 + AppTheme.HUD_PADDING, baseline);
        } finally {
            g2.dispose();
        }
    }
}
