package elite.intel.ui.view;

import com.formdev.flatlaf.ui.FlatComboBoxUI;

import javax.swing.*;
import java.awt.*;

/**
 * HUD-styled combo box UI: flat ▼ glyph in {@link AppTheme#ACCENT}, warm dark field background.
 * Replaces FlatLaf's arrow-button box with a plain filled triangle — no gradient, no rounded box.
 * Install via {@link AppTheme#styleComboBox(JComboBox)}.
 */
class HudComboBoxUI extends FlatComboBoxUI {

    @Override
    protected void installDefaults() {
        super.installDefaults();
        // FlatLaf installDefaults resets the border via LookAndFeel.installBorder;
        // restore the HUD warm frame here so it is in place before styleComboBox applies its own call.
        comboBox.setBorder(AppTheme.hudFieldBorder());
        // FlatComboBoxUI.update() paints the arrow area with buttonBackground on top of the
        // component-wide fill, producing a visible "button box". Null all three background
        // fields so buttonColor resolves to null and the arrow area keeps HUD_TABLE_ROW.
        buttonBackground        = null;
        buttonFocusedBackground = null;
        focusedBackground       = null;
        popupBackground         = AppTheme.HUD_TABLE_ROW;   // тёплый фон JList и popup-окна
    }

    /** Replaces FlatLaf's default (white/bright) popup border with the HUD warm accent frame. */
    @Override
    protected javax.swing.plaf.basic.ComboPopup createPopup() {
        javax.swing.plaf.basic.ComboPopup popup = super.createPopup();
        if (popup instanceof javax.swing.plaf.basic.BasicComboPopup basic) {
            basic.setBorder(javax.swing.BorderFactory.createLineBorder(
                    AppTheme.HUD_ORANGE_SOFT, AppTheme.HUD_BORDER_THICKNESS));
        }
        return popup;
    }

    @Override
    protected JButton createArrowButton() {
        ArrowButton btn = new ArrowButton();
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusable(false);
        btn.setOpaque(false);
        return btn;
    }

    /**
     * Fills the current-value area with the component's own background (HUD_TABLE_ROW),
     * bypassing FlatLaf's focus-ring and palette logic.
     */
    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        g.setColor(comboBox.getBackground());
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /** Draws a flat ▼ triangle with no background or border box. */
    private final class ArrowButton extends JButton {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Color c = (comboBox != null && comboBox.isEnabled()) ? AppTheme.ACCENT : AppTheme.HUD_DISABLED;
                AppTheme.paintHudArrowDown(g2, 0, 0, getWidth(), getHeight(), c);
            } finally {
                g2.dispose();
            }
        }
    }
}
