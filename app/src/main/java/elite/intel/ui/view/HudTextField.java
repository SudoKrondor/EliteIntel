package elite.intel.ui.view;

import javax.swing.*;
import java.awt.*;
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

    /**
     * Creates an empty HUD text field.
     */
    public HudTextField() {
        AppTheme.styleTextComponent(this);
        setPreferredSize(new Dimension(0, AppTheme.HUD_FIELD_HEIGHT));
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
            int infoZoneW = AppTheme.HUD_TABLE_ROW_HEIGHT_COMPACT;
            int infoZoneX = w - infoZoneW;

            // Separator stripe: HUD_BG between text area and info-zone
            g2.setColor(AppTheme.HUD_BG);
            g2.fillRect(infoZoneX - AppTheme.HUD_SEP_W, 0, AppTheme.HUD_SEP_W, h);

            // Tint by state (text field has no selected state)
            Color tint = !isEnabled() ? AppTheme.HUD_DISABLED
                       : infoHover    ? AppTheme.ACCENT
                       :                AppTheme.HUD_ORANGE_SOFT;

            int gs = AppTheme.HUD_ICON_TABLE;
            int gx = infoZoneX + (infoZoneW - gs) / 2;
            int gy = (h - gs) / 2;
            AppTheme.paintHudInfoGlyph(g2, gx, gy, gs, gs, tint);
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
        return infoAction != null && mouseX >= getWidth() - AppTheme.HUD_TABLE_ROW_HEIGHT_COMPACT;
    }
}
