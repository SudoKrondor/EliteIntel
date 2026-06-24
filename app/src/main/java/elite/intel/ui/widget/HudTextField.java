package elite.intel.ui.widget;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.theme.HudGlyphs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;

/**
 * HUD-styled single-line text field with shared dark input styling.
 * <p>
 * Optional info-zone: call {@link #setInfoAction(Runnable)} to paint an info-glyph
 * in the right margin. Clicking the zone runs the action without affecting the caret
 * or text selection.
 */
public class HudTextField extends JTextField {

    /** Non-null when an info-zone is active. */
    private Runnable infoAction;
    /** True while the pointer is inside the info-zone. */
    private boolean infoHover;
    /** True while the field holds keyboard focus; drives the focus-frame and text accent. */
    private boolean focused;
    /** Foreground colour captured on focus gain, restored on focus loss. */
    private Color restingForeground;

    /**
     * Creates an empty HUD text field.
     */
    public HudTextField() {
        AppTheme.styleTextComponent(this);
        setPreferredSize(new Dimension(0, HudPalette.HUD_FIELD_HEIGHT));
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                focused = true;
                restingForeground = getForeground();
                setForeground(HudPalette.HUD_COLOR_ROLE_INPUT_FOCUS);
                repaint();
            }
            @Override public void focusLost(FocusEvent e) {
                focused = false;
                if (restingForeground != null) {
                    setForeground(restingForeground);
                }
                repaint();
            }
        });
    }

    /**
     * Attaches an info-zone to this field. When {@code action} is non-null an info-glyph
     * is painted in the right margin; clicking it runs {@code action} without moving the
     * caret. Pass {@code null} to remove the info-zone and restore default behaviour.
     */
    /** @return true when an info-zone is active and the field needs the wider info border. */
    public boolean hasInfoZone() {
        return infoAction != null;
    }

    public void setInfoAction(Runnable action) {
        this.infoAction = action;
        setBorder(action != null ? AppTheme.hudFieldBorderWithInfo() : AppTheme.hudFieldBorder());
        if (action != null) {
            enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
            infoHover = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        } else {
            infoHover = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        }
        revalidate();
        repaint();
    }

    // -------------------------------------------------------------------------
    // Paint
    // -------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // live text, caret, selection
        if (infoAction == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth();
            int h = getHeight();
            int infoZoneW = HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT;
            int infoZoneX = w - infoZoneW;

            // Separator stripe: HUD_COLOR_ROLE_APPLICATION_BACKGROUND between text area and info-zone
            g2.setColor(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
            g2.fillRect(infoZoneX - HudPalette.HUD_SEP_W, 0, HudPalette.HUD_SEP_W, h);

            // Tint by state (text field has no selected state)
            Color tint = !isEnabled() ? HudPalette.HUD_COLOR_ROLE_DISABLED
                       : infoHover    ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION
                       :                HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION;

            int gs = HudPalette.HUD_ICON_TABLE;
            int gx = infoZoneX + (infoZoneW - gs) / 2;
            int gy = (h - gs) / 2;
            HudGlyphs.paintHudInfoGlyph(g2, gx, gy, gs, gs, tint);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Overlays the HUD focus accent on top of the standard field border: while focused, the frame
     * is repainted in {@code HUD_COLOR_ROLE_INPUT_FOCUS}. Drawn after {@code super.paint} so it sits
     * over the border; the disabled field keeps its dim border untouched. The field border/insets
     * and base colours are unchanged.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (!isEnabled() || !focused) return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // Crisp 1 px frame: AA off so the right/bottom edges render at full brightness.
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setColor(HudPalette.HUD_COLOR_ROLE_INPUT_FOCUS);
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        } finally {
            g2.dispose();
        }
    }

    // -------------------------------------------------------------------------
    // Mouse handling
    // -------------------------------------------------------------------------

    /**
     * Intercepts events in the info-zone: CLICKED runs the action, PRESSED/RELEASED/CLICKED
     * are consumed to prevent caret placement. All other events pass through normally.
     */
    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (infoAction != null) {
            if (e.getID() == MouseEvent.MOUSE_EXITED) {
                if (infoHover) {
                    infoHover = false;
                    repaint();
                }
                // Fall through so the field can clear its own rollover state.
            } else if (isInInfoZone(e.getX())) {
                if (e.getID() == MouseEvent.MOUSE_CLICKED) {
                    infoAction.run();
                }
                e.consume();
                return; // Block caret placement
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
                        nowHover ? Cursor.HAND_CURSOR : Cursor.TEXT_CURSOR));
                repaint();
            }
        }
        super.processMouseMotionEvent(e);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isInInfoZone(int mouseX) {
        return infoAction != null && mouseX >= getWidth() - HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT;
    }
}
