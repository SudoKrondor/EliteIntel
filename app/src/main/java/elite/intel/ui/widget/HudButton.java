package elite.intel.ui.widget;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

/**
 * HUD-styled Swing button with consistent cockpit fill, border, and hover states.
 * <p>
 * An optional info-zone can be appended inside the right edge through
 * {@link #setInfoAction(Runnable)}. Mouse interaction in that zone runs the info action without
 * activating the button's main action; keyboard activation remains attached to the main action.
 * Use {@link #setMainActionEnabled(boolean)} when the main action should be unavailable while the
 * informational zone remains interactive.
 */
public class HudButton extends JButton {

    private final boolean primary;

    /** Non-null when the optional right-side info-zone is active. */
    private Runnable infoAction;
    /** True while the pointer is inside the info-zone. */
    private boolean infoHover;
    /** Availability of the main action; kept separate from the component for an interactive info-zone. */
    private boolean mainActionEnabled = true;

    /** When > 0, the button uses a fixed square footprint instead of the default action sizing. */
    private int squareSide = 0;

    /**
     * Creates a reusable HUD button.
     *
     * @param label   visible button text
     * @param primary true for the orange primary treatment, false for a subdued treatment
     */
    public HudButton(String label, boolean primary) {
        super(label != null ? label.toUpperCase() : "");
        this.primary = primary;
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setForeground(primary ? HudPalette.HUD_COLOR_ROLE_BUTTON_TEXT : HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
        setFont(getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_BUTTON));
        updateBorder();
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Prevent applyDarkPalette from overriding the state-driven foreground colour.
        putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        getModel().addChangeListener(e -> updateForeground());
        addPropertyChangeListener("enabled", event -> {
            updateForeground();
            updateCursor(infoHover);
        });
    }

    /**
     * Attaches an internal info-zone to the right edge of this button. Clicking the zone runs
     * {@code action} without firing the button's main action. Pass {@code null} to remove the zone
     * and restore the button's original footprint and mouse behaviour.
     *
     * @param action info action, or {@code null} to remove the info-zone
     */
    public void setInfoAction(Runnable action) {
        this.infoAction = action;
        infoHover = false;
        if (action != null) {
            enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }
        updateCursor(false);
        updateBorder();
        revalidate();
        repaint();
    }

    /**
     * Returns whether this button currently reserves and paints an internal info-zone.
     *
     * @return {@code true} when an info action is attached
     */
    public boolean hasInfoZone() {
        return infoAction != null;
    }

    /**
     * Enables or disables only the button's main action and its visual treatment. Unlike
     * {@link #setEnabled(boolean)}, disabling the main action this way leaves an attached info-zone
     * interactive so contextual help and training phrases remain discoverable.
     *
     * @param enabled whether mouse/keyboard activation may fire the main action
     */
    public void setMainActionEnabled(boolean enabled) {
        mainActionEnabled = enabled;
        if (!enabled) {
            cancelMainPress();
            getModel().setRollover(false);
        }
        updateForeground();
        updateCursor(infoHover);
        repaint();
    }

    /** Returns whether the main button action is currently available. */
    public boolean isMainActionEnabled() {
        return mainActionEnabled && isEnabled();
    }

    /**
     * Switches the main action to a fixed {@code side}x{@code side} footprint, used for compact
     * trailing field actions (e.g. pickers) that must align with a field's height and stay
     * narrow. An optional info-zone is appended to that footprint. Pass 0 to restore the default
     * action-button sizing.
     */
    public void setSquareSide(int side) {
        this.squareSide = side;
        updateBorder();
        revalidate();
    }

    @Override
    public Dimension getPreferredSize() {
        int infoExtra = infoExtraWidth();
        if (squareSide > 0) {
            return new Dimension(squareSide + infoExtra, squareSide);
        }
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(90 + infoExtra, d.width), HudPalette.HUD_BUTTON_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return squareSide > 0 ? getPreferredSize() : super.getMinimumSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return squareSide > 0 ? getPreferredSize() : super.getMaximumSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth();
            int h = getHeight();
            ButtonModel model = getModel();

            if (!isEnabled() || !mainActionEnabled) {
                // Unified disabled appearance for primary and secondary: warm fill + dim cold border.
                g2.setColor(HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
                g2.fillRect(0, 0, w - 1, h - 1);
                g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER);
                g2.drawRect(0, 0, w - 1, h - 1);
            } else if (primary) {
                paintPrimary(g2, w, h, model);
            } else {
                paintSecondary(g2, w, h, model);
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
        paintInfoZone(g);
    }

    private void paintPrimary(Graphics2D g2, int w, int h, ButtonModel model) {
        Color fill = model.isPressed() ? HudPalette.HUD_COLOR_ROLE_PRIMARY_BUTTON_PRESSED_BACKGROUND
                   : model.isRollover() ? HudPalette.HUD_COLOR_ROLE_PRIMARY_BUTTON_HOVER_BACKGROUND
                   : HudPalette.HUD_COLOR_ROLE_PRIMARY_BUTTON_BACKGROUND;
        g2.setColor(fill);
        g2.fillRect(0, 0, w - 1, h - 1);
        g2.setColor(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
        g2.drawRect(0, 0, w - 1, h - 1);
        Color glow = new Color(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION.getRed(), HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION.getGreen(),
                HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION.getBlue(), 70);
        g2.setColor(glow);
        g2.drawRect(1, 1, w - 3, h - 3);
    }

    private void paintSecondary(Graphics2D g2, int w, int h, ButtonModel model) {
        Color fill = model.isPressed() ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION
                   : model.isRollover() ? HudPalette.HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND
                   : HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND;
        Color border = HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION;
        g2.setColor(fill);
        g2.fillRect(0, 0, w - 1, h - 1);
        g2.setColor(border);
        g2.drawRect(0, 0, w - 1, h - 1);
    }

    private void paintInfoZone(Graphics g) {
        if (infoAction == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth();
            int h = getHeight();
            int infoZoneW = HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT;
            int infoZoneX = w - infoZoneW;

            g2.setColor(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
            int borderInset = HudPalette.HUD_BORDER_THICKNESS;
            g2.fillRect(
                    infoZoneX - HudPalette.HUD_SEP_W,
                    borderInset,
                    HudPalette.HUD_SEP_W,
                    Math.max(0, h - borderInset * 2));

            ButtonModel model = getModel();
            Color tint = model.isPressed() ? HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT
                    : infoHover ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION
                    : !isEnabled() ? HudPalette.HUD_COLOR_ROLE_DISABLED
                    : !mainActionEnabled ? HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION
                    : primary ? HudPalette.HUD_COLOR_ROLE_BUTTON_TEXT
                    : HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION;
            int glyphSize = HudPalette.HUD_ICON_TABLE;
            int glyphX = infoZoneX + (infoZoneW - glyphSize) / 2;
            int glyphY = (h - glyphSize) / 2;
            HudGlyphs.paintHudInfoGlyph(g2, glyphX, glyphY, glyphSize, glyphSize, tint);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Consumes mouse interaction inside the info-zone so Swing's button model cannot arm or fire
     * the main action there. The info-zone remains usable when only the main action was disabled via
     * {@link #setMainActionEnabled(boolean)}; fully disabling the component blocks both actions.
     */
    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (infoAction != null) {
            if (e.getID() == MouseEvent.MOUSE_EXITED) {
                clearInfoHover();
            } else if (isInInfoZone(e.getX())) {
                setInfoHover(true);
                if (e.getID() == MouseEvent.MOUSE_PRESSED
                        || e.getID() == MouseEvent.MOUSE_RELEASED) {
                    cancelMainPress();
                }
                if (e.getID() == MouseEvent.MOUSE_CLICKED && isEnabled()) {
                    infoAction.run();
                }
                e.consume();
                return;
            }
        }
        if (!mainActionEnabled && e.getID() != MouseEvent.MOUSE_EXITED) {
            cancelMainPress();
            e.consume();
            return;
        }
        super.processMouseEvent(e);
    }

    /** Tracks pointer transitions between the main action and the internal info-zone. */
    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (infoAction != null) {
            boolean nowHover = isInInfoZone(e.getX());
            setInfoHover(nowHover);
            if (nowHover) {
                cancelMainPress();
                e.consume();
                return;
            }
        }
        if (!mainActionEnabled) {
            cancelMainPress();
            e.consume();
            return;
        }
        super.processMouseMotionEvent(e);
    }

    @Override
    protected void fireActionPerformed(ActionEvent event) {
        if (mainActionEnabled && isEnabled()) {
            super.fireActionPerformed(event);
        }
    }

    private void updateBorder() {
        int infoExtra = infoExtraWidth();
        if (squareSide > 0) {
            // Keep the main action square and append the optional info-zone to its right.
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, infoExtra));
        } else {
            setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14 + infoExtra));
        }
    }

    private int infoExtraWidth() {
        return infoAction != null
                ? HudPalette.HUD_SEP_W + HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT
                : 0;
    }

    private boolean isInInfoZone(int mouseX) {
        return infoAction != null && mouseX >= getWidth() - HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT;
    }

    private void setInfoHover(boolean hover) {
        if (infoHover == hover) {
            return;
        }
        infoHover = hover;
        updateCursor(hover);
        if (isRolloverEnabled()) {
            getModel().setRollover(!hover && isEnabled() && mainActionEnabled);
        }
        repaint();
    }

    private void clearInfoHover() {
        if (infoHover) {
            infoHover = false;
            updateCursor(false);
            repaint();
        }
    }

    private void updateCursor(boolean hoveringInfo) {
        boolean clickable = isEnabled() && (mainActionEnabled || (infoAction != null && hoveringInfo));
        setCursor(Cursor.getPredefinedCursor(clickable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void updateForeground() {
        Color color;
        if (!isEnabled() || !mainActionEnabled) {
            color = HudPalette.HUD_COLOR_ROLE_DISABLED;
        } else if (!primary && getModel().isPressed()) {
            color = HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT;
        } else {
            color = primary
                    ? HudPalette.HUD_COLOR_ROLE_BUTTON_TEXT
                    : HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION;
        }
        setForeground(color);
    }

    private void cancelMainPress() {
        ButtonModel model = getModel();
        // Disarm first: releasing a still-armed model would fire the main action.
        model.setArmed(false);
        model.setPressed(false);
    }
}
