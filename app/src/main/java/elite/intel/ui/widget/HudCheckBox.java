package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * HUD-styled checkbox: a rounded square marker (Graphics2D primitives) and a CAPS label, painted straight on
 * whatever panel it sits on. No LAF rendering is used; super.paintComponent is not called.
 * <p>
 * Only the marker and label are live, however wide a layout stretches the control: nothing marks the rest of
 * the cell as part of it, so a click there must not flip a setting.
 * <p>
 * Optional info-zone: call {@link #setInfoAction(Runnable)} to append a square
 * info-glyph (info) zone after the label. Clicking it runs the action without toggling
 * the checkbox state.
 */
public class HudCheckBox extends JCheckBox {

    private static final int MARKER_SIZE = HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT - 2 * HudPalette.HUD_PADDING_SMALL;
    private static final int LABEL_X = MARKER_SIZE + HudPalette.HUD_PADDING_SMALL;
    private static final int INFO_ZONE_WIDTH = HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT;

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
     * info-glyph appears after the label; clicking it runs {@code action} without toggling
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
        int infoExtra = infoAction != null ? INFO_ZONE_WIDTH : 0;
        return new Dimension(LABEL_X + labelWidth() + infoExtra, HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT);
    }

    /**
     * Limits the live area to what is painted, so the mouse passes through the stretched remainder of a cell.
     */
    @Override
    public boolean contains(int x, int y) {
        return x < getPreferredSize().width && super.contains(x, y);
    }

    /**
     * The width of what {@link #paintLabel} paints.
     */
    protected int labelWidth() {
        FontMetrics fm = getFontMetrics(getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_CHECKBOX));
        return fm != null ? fm.stringWidth(labelText) : 120;
    }

    /**
     * Pins the minimum size to the preferred size: this control paints its marker and label itself
     * within its bounds, so a layout that shrinks it below this would clip the label vertically.
     */
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
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

            int h = getHeight();
            boolean on      = isSelected();
            boolean enabled = isEnabled();

            Color markerColor;
            Color textColor;

            // The label only brightens with the state, so a page of checked rows stays calm: the
            // filled marker is what says "on".
            if (!enabled) {
                markerColor = HudPalette.HUD_COLOR_ROLE_DISABLED;
                textColor   = HudPalette.HUD_COLOR_ROLE_DISABLED;
            } else if (on) {
                markerColor = HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION;
                textColor = HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT;
            } else {
                markerColor = HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION;
                textColor   = HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT;
            }

            // Pass `on` rather than `on && enabled`: a disabled toggle must still show whether it is
            // checked, or a setting that cannot be changed right now renders as an empty box and reads
            // as "off" when it is on. Greying is already carried by markerColor.
            HudGlyphs.paintHudCheckMarker(g2, 0, (h - MARKER_SIZE) / 2, MARKER_SIZE,
                    markerColor, HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT, on);

            paintLabel(g2, LABEL_X, h, textColor);

            // Info-zone (optional)
            if (infoAction != null) {
                int infoZoneX = infoZoneX();

                // Glyph tint: brightens for hover on the zone itself and for the checked state alike.
                Color infoTint;
                if (!enabled) {
                    infoTint = HudPalette.HUD_COLOR_ROLE_DISABLED;
                } else if (infoHover || on) {
                    infoTint = HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION;
                } else {
                    infoTint = HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION;
                }

                // Glyph box centred inside the info zone, sized to HUD_ICON_TABLE role
                int gs  = HudPalette.HUD_ICON_TABLE;
                int gx = infoZoneX + (INFO_ZONE_WIDTH - gs) / 2;
                int gy  = (h - gs) / 2;
                HudGlyphs.paintHudInfoGlyph(g2, gx, gy, gs, gs, infoTint);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Paints the label in the text zone, vertically centred.
     *
     * @param x left edge of the label
     * @param h height of the control
     */
    protected void paintLabel(Graphics2D g2, int x, int h, Color textColor) {
        g2.setFont(getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_CHECKBOX));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(textColor);
        g2.drawString(labelText, x, (h - fm.getHeight()) / 2 + fm.getAscent());
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
        return mouseX >= infoZoneX();
    }

    private int infoZoneX() {
        return LABEL_X + labelWidth();
    }

    private void clearInfoHover() {
        if (infoHover) {
            infoHover = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            repaint();
        }
    }
}
