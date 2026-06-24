package elite.intel.ui.widget;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;

/**
 * Toggle variant of the HUD button used for service and on/off controls.
 */
public class HudToggleButton extends JToggleButton {

    /**
     * Creates a HUD toggle button with selected-state accent styling.
     *
     * @param label visible button text
     */
    public HudToggleButton(String label) {
        super(label);
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        setPreferredSize(new Dimension(Math.max(112, getPreferredSize().width), HudPalette.HUD_BUTTON_HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Prevent applyDarkPalette from overriding the state-driven foreground colour.
        putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        // Keep text readable against bright HUD_COLOR_ROLE_PRIMARY_ACTION fill when selected.
        addItemListener(e -> setForeground(isSelected() ? HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT : HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT));
        setForeground(isSelected() ? HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT : HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ButtonModel model = getModel();
            Color border = isSelected() ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION : HudPalette.HUD_COLOR_ROLE_FRAME_BORDER;
            Color fill = isSelected() ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION : HudPalette.HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND;
            if (!isEnabled()) {
                border = HudPalette.HUD_COLOR_ROLE_DISABLED;
                fill = HudPalette.HUD_COLOR_ROLE_PANEL_BACKGROUND;
            } else if (model.isPressed()) {
                fill = fill.darker();
            } else if (model.isRollover()) {
                fill = isSelected() ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION.darker() : HudPalette.HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND;
            }
            g2.setColor(fill);
            g2.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
            g2.setColor(border);
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
