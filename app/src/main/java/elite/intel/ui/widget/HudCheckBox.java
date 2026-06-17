package elite.intel.ui.widget;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.theme.HudGlyphs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * HUD-styled checkbox rendered as a full-width cockpit control:
 * a square marker (Graphics2D primitives) + a narrow separator gap + a CAPS label
 * in one solid slab. No LAF rendering is used; super.paintComponent is not called.
 * <p>
 * Optional info-zone: call {@link #setInfoAction(Runnable)} to append a square
 * info-glyph (info) zone on the right. Clicking it runs the action without toggling
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
        Font f = getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_CHECKBOX);
        FontMetrics fm = getFontMetrics(f);
        int markerSize  = HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT - 2 * HudPalette.HUD_PADDING_SMALL;
        int markerZoneW = markerSize + 2 * HudPalette.HUD_PADDING_SMALL;
        int textW = HudPalette.HUD_PADDING
                + (fm != null ? fm.stringWidth(labelText) : 120)
                + HudPalette.HUD_PADDING;
        int infoExtra = infoAction != null ? (HudPalette.HUD_SEP_W + HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT) : 0;
        return new Dimension(markerZoneW + HudPalette.HUD_SEP_W + textW + infoExtra,
                HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT);
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
            int markerSize  = HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT - 2 * HudPalette.HUD_PADDING_SMALL;
            int markerZoneW = markerSize + 2 * HudPalette.HUD_PADDING_SMALL;
            int markerX     = HudPalette.HUD_PADDING_SMALL;
            int markerY     = (h - markerSize) / 2;

            boolean on      = isSelected();
            boolean enabled = isEnabled();

            Color fill;
            Color markerColor;
            Color textColor;

            if (!enabled) {
                fill        = HudPalette.HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND;
                markerColor = HudPalette.HUD_COLOR_ROLE_DISABLED;
                textColor   = HudPalette.HUD_COLOR_ROLE_DISABLED;
            } else if (on) {
                fill        = HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION;
                markerColor = HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT;
                textColor   = HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT;
            } else {
                fill        = HudPalette.HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND;
                markerColor = HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION;
                textColor   = HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT;
            }

            // Slab fill - no border outline on the control itself
            g2.setColor(fill);
            g2.fillRect(0, 0, w, h);

            // Left separator: HUD_COLOR_ROLE_APPLICATION_BACKGROUND stripe between marker zone and text zone
            g2.setColor(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
            g2.fillRect(markerZoneW, 0, HudPalette.HUD_SEP_W, h);

            HudGlyphs.paintHudCheckMarker(g2, markerX, markerY, markerSize, markerColor, on && enabled);

            // Label text in the text zone, vertically centred
            Font f = getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_CHECKBOX);
            g2.setFont(f);
            FontMetrics fm   = g2.getFontMetrics();
            int baseline     = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(textColor);
            g2.drawString(labelText, markerZoneW + HudPalette.HUD_SEP_W + HudPalette.HUD_PADDING, baseline);

            // Info-zone (optional)
            if (infoAction != null) {
                int infoZoneW = HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT;
                int infoZoneX = w - infoZoneW;

                // Right separator: same width/colour as the left one
                g2.setColor(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
                g2.fillRect(infoZoneX - HudPalette.HUD_SEP_W, 0, HudPalette.HUD_SEP_W, h);

                // Glyph tint: follows row state; hover on the zone itself brightens to HUD_COLOR_ROLE_PRIMARY_ACTION
                Color infoTint;
                if (!enabled) {
                    infoTint = HudPalette.HUD_COLOR_ROLE_DISABLED;
                } else if (on) {
                    infoTint = HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT;          // visible on HUD_COLOR_ROLE_PRIMARY_ACTION fill
                } else if (infoHover) {
                    infoTint = HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION;           // hover highlight
                } else {
                    infoTint = HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION;
                }

                // Glyph box centred inside the info zone, sized to HUD_ICON_TABLE role
                int gs  = HudPalette.HUD_ICON_TABLE;
                int gx  = infoZoneX + (infoZoneW - gs) / 2;
                int gy  = (h - gs) / 2;
                HudGlyphs.paintHudInfoGlyph(g2, gx, gy, gs, gs, infoTint);
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
        return mouseX >= getWidth() - HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT;
    }

    private void clearInfoHover() {
        if (infoHover) {
            infoHover = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            repaint();
        }
    }
}
