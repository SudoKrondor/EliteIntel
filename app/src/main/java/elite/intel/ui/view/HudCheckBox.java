package elite.intel.ui.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * HUD-styled checkbox rendered as a full-width cockpit control:
 * a square marker (Graphics2D primitives) + a narrow separator gap + a CAPS label
 * in one solid slab. No LAF rendering is used; super.paintComponent is not called.
 * <p>
 * Optional info-zone: call {@link #setInfoAction(Runnable)} to append a square
 * info-glyph (ⓘ) zone on the right. Clicking it runs the action without toggling
 * the checkbox state.
 */
public class HudCheckBox extends JCheckBox {

    private final String labelText;

    /** Non-null when an info-zone is active. */
    private Runnable infoAction;
    /** True while the pointer is inside the info-zone. */
    private boolean infoHover;

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

    /**
     * Attaches an info-zone to this checkbox. When {@code action} is non-null a square
     * info-glyph appears on the right; clicking it runs {@code action} without toggling
     * the checkbox. Pass {@code null} to remove the info-zone and restore default behaviour.
     */
    public void setInfoAction(Runnable action) {
        this.infoAction = action;
        if (action != null) {
            // Motion events are not enabled by default in AbstractButton.
            enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
        } else {
            infoHover = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        revalidate();
        repaint();
    }

    // -------------------------------------------------------------------------
    // Size
    // -------------------------------------------------------------------------

    @Override
    public Dimension getPreferredSize() {
        Font f = getFont().deriveFont(Font.BOLD, AppTheme.HUD_FONT_CHECKBOX);
        FontMetrics fm = getFontMetrics(f);
        int markerSize  = AppTheme.HUD_TABLE_ROW_HEIGHT_COMPACT - 2 * AppTheme.HUD_PADDING_SMALL;
        int markerZoneW = markerSize + 2 * AppTheme.HUD_PADDING_SMALL;
        int textW = AppTheme.HUD_PADDING
                + (fm != null ? fm.stringWidth(labelText) : 120)
                + AppTheme.HUD_PADDING;
        int infoExtra = infoAction != null ? (AppTheme.HUD_SEP_W + AppTheme.HUD_TABLE_ROW_HEIGHT_COMPACT) : 0;
        return new Dimension(markerZoneW + AppTheme.HUD_SEP_W + textW + infoExtra,
                AppTheme.HUD_TABLE_ROW_HEIGHT_COMPACT);
    }

    // -------------------------------------------------------------------------
    // Paint
    // -------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Marker geometry: square sized to row height minus vertical padding
            int markerSize  = AppTheme.HUD_TABLE_ROW_HEIGHT_COMPACT - 2 * AppTheme.HUD_PADDING_SMALL;
            int markerZoneW = markerSize + 2 * AppTheme.HUD_PADDING_SMALL;
            int markerX     = AppTheme.HUD_PADDING_SMALL;
            int markerY     = (h - markerSize) / 2;

            boolean on      = isSelected();
            boolean enabled = isEnabled();

            Color fill;
            Color markerColor;
            Color textColor;

            if (!enabled) {
                fill        = AppTheme.HUD_TABLE_ROW_HOVER;
                markerColor = AppTheme.HUD_DISABLED;
                textColor   = AppTheme.HUD_DISABLED;
            } else if (on) {
                fill        = AppTheme.ACCENT;
                markerColor = AppTheme.SEL_FG;
                textColor   = AppTheme.SEL_FG;
            } else {
                fill        = AppTheme.HUD_TABLE_ROW_HOVER;
                markerColor = AppTheme.HUD_ORANGE_SOFT;
                textColor   = AppTheme.FG_MUTED;
            }

            // Slab fill — no border outline on the control itself
            g2.setColor(fill);
            g2.fillRect(0, 0, w, h);

            // Left separator: HUD_BG stripe between marker zone and text zone
            g2.setColor(AppTheme.HUD_BG);
            g2.fillRect(markerZoneW, 0, AppTheme.HUD_SEP_W, h);

            // Marker: 2-px square border (two nested drawRects)
            g2.setColor(markerColor);
            g2.drawRect(markerX,     markerY,     markerSize - 1, markerSize - 1);
            g2.drawRect(markerX + 1, markerY + 1, markerSize - 3, markerSize - 3);

            // Inner filled square — ON state only
            if (on && enabled) {
                int innerSize = markerSize / 2;
                int innerX    = markerX + (markerSize - innerSize) / 2;
                int innerY    = markerY + (markerSize - innerSize) / 2;
                g2.fillRect(innerX, innerY, innerSize, innerSize);
            }

            // Label text in the text zone, vertically centred
            Font f = getFont().deriveFont(Font.BOLD, AppTheme.HUD_FONT_CHECKBOX);
            g2.setFont(f);
            FontMetrics fm   = g2.getFontMetrics();
            int baseline     = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(textColor);
            g2.drawString(labelText, markerZoneW + AppTheme.HUD_SEP_W + AppTheme.HUD_PADDING, baseline);

            // Info-zone (optional)
            if (infoAction != null) {
                int infoZoneW = AppTheme.HUD_TABLE_ROW_HEIGHT_COMPACT;
                int infoZoneX = w - infoZoneW;

                // Right separator: same width/colour as the left one
                g2.setColor(AppTheme.HUD_BG);
                g2.fillRect(infoZoneX - AppTheme.HUD_SEP_W, 0, AppTheme.HUD_SEP_W, h);

                // Glyph tint: follows row state; hover on the zone itself brightens to ACCENT
                Color infoTint;
                if (!enabled) {
                    infoTint = AppTheme.HUD_DISABLED;
                } else if (on) {
                    infoTint = AppTheme.SEL_FG;          // visible on ACCENT fill
                } else if (infoHover) {
                    infoTint = AppTheme.ACCENT;           // hover highlight
                } else {
                    infoTint = AppTheme.HUD_ORANGE_SOFT;
                }

                // Glyph box centred inside the info zone, sized to HUD_ICON_TABLE role
                int gs  = AppTheme.HUD_ICON_TABLE;
                int gx  = infoZoneX + (infoZoneW - gs) / 2;
                int gy  = (h - gs) / 2;
                AppTheme.paintHudInfoGlyph(g2, gx, gy, gs, gs, infoTint);
            }
        } finally {
            g2.dispose();
        }
    }

    // -------------------------------------------------------------------------
    // Mouse handling
    // -------------------------------------------------------------------------

    /**
     * Intercepts clicks landing in the info-zone so they run {@link #infoAction}
     * without toggling the checkbox. All other events pass through to the ButtonModel.
     */
    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (infoAction != null) {
            if (e.getID() == MouseEvent.MOUSE_EXITED) {
                clearInfoHover();
                // Fall through to super so ButtonModel clears its rollover state.
            } else if (isInInfoZone(e.getX())) {
                if (e.getID() == MouseEvent.MOUSE_CLICKED) {
                    infoAction.run();
                }
                e.consume();
                return; // Block toggle
            }
        }
        super.processMouseEvent(e);
    }

    /**
     * Tracks pointer position to update info-zone hover state and cursor.
     */
    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (infoAction != null) {
            boolean nowHover = isInInfoZone(e.getX());
            if (nowHover != infoHover) {
                infoHover = nowHover;
                setCursor(Cursor.getPredefinedCursor(
                        nowHover ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                repaint();
            }
        }
        super.processMouseMotionEvent(e);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns true when {@code mouseX} falls within the info-zone column. */
    private boolean isInInfoZone(int mouseX) {
        return mouseX >= getWidth() - AppTheme.HUD_TABLE_ROW_HEIGHT_COMPACT;
    }

    private void clearInfoHover() {
        if (infoHover) {
            infoHover = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            repaint();
        }
    }
}
