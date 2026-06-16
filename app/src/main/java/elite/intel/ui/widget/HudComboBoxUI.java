package elite.intel.ui.widget;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.theme.HudGlyphs;

import com.formdev.flatlaf.ui.FlatComboBoxUI;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * HUD-styled combo box UI: flat ▼ glyph in {@link AppTheme#HUD_COLOR_ROLE_PRIMARY_ACTION}, warm dark field background.
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
        // component-wide fill, producing a visible "button box". Null all four background
        // fields so buttonColor resolves to null and the arrow area keeps HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND.
        // buttonEditableBackground is a separate field read by FlatComboBoxUI in editable mode;
        // without nulling it, editable combos show a bright FlatLaf-default rectangle.
        buttonBackground         = null;
        buttonFocusedBackground  = null;
        buttonEditableBackground = null;
        focusedBackground        = null;
        popupBackground         = HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND;   // тёплый фон JList и popup-окна
    }

    /** Replaces FlatLaf's default (white/bright) popup border with the HUD warm accent frame. */
    @Override
    protected javax.swing.plaf.basic.ComboPopup createPopup() {
        javax.swing.plaf.basic.ComboPopup popup = super.createPopup();
        if (popup instanceof javax.swing.plaf.basic.BasicComboPopup basic) {
            basic.setBorder(javax.swing.BorderFactory.createLineBorder(
                    HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION, HudPalette.HUD_BORDER_THICKNESS));
        }
        JScrollPane scroller = (JScrollPane) SwingUtilities.getAncestorOfClass(
                JScrollPane.class, popup.getList());
        if (scroller != null) {
            AppTheme.styleScrollBar(scroller.getVerticalScrollBar());
        }
        return popup;
    }

    /**
     * Subdues the FlatLaf-managed editor of an editable combo to the HUD canon. Runs after FlatLaf's
     * own editor configuration so the HUD border is in place when {@link AppTheme#styleComboEditor}
     * neutralises FlatLaf's injected {@code JTextField.padding}. Called on every editor (re)configuration
     * (initial install, look-and-feel update), keeping the editor styling from going stale.
     */
    @Override
    protected void configureEditor() {
        super.configureEditor();
        Component ec = comboBox.getEditor().getEditorComponent();
        if (ec instanceof JTextComponent editor) {
            AppTheme.styleComboEditor(editor);
        }
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
     * Fills the current-value area with the component's own background (HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND),
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
                Color c = (comboBox != null && comboBox.isEnabled()) ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION : HudPalette.HUD_COLOR_ROLE_DISABLED;
                HudGlyphs.paintHudArrowDown(g2, 0, 0, getWidth(), getHeight(), c);
            } finally {
                g2.dispose();
            }
        }
    }
}
