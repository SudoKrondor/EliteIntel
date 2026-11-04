package elite.intel.ui.widget;

import com.formdev.flatlaf.ui.FlatComboBoxUI;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * HUD-styled combo box UI: a text cell and a ▼ cell divided by a dim line, the same segments as the table filter bar
 * ({@link HudSearchField}). A combo inside a table cell keeps the warm look of its grid: a bare ▼ and a warm list.
 * Install via {@link AppTheme#styleComboBox(JComboBox)}.
 */
class HudComboBoxUI extends FlatComboBoxUI {

    @Override
    protected void installDefaults() {
        super.installDefaults();
        // FlatLaf installDefaults resets the border via LookAndFeel.installBorder;
        // restore the HUD warm frame here so it is in place before styleComboBox applies its own call.
        comboBox.setBorder(AppTheme.hudComboBorder());
        // FlatComboBoxUI.update() paints the arrow area with buttonBackground on top of the
        // component-wide fill, producing a visible "button box". Null all four background
        // fields so buttonColor resolves to null and the arrow area is painted by ArrowButton alone.
        // buttonEditableBackground is a separate field read by FlatComboBoxUI in editable mode;
        // without nulling it, editable combos show a bright FlatLaf-default rectangle.
        buttonBackground         = null;
        buttonFocusedBackground  = null;
        buttonEditableBackground = null;
        focusedBackground        = null;
        popupBackground = HudPalette.HUD_COLOR_ROLE_FIELD_BACKGROUND;   // background of the JList and popup window
        // FlatLaf's light-theme defaults, or a disabled combo paints pale around and inside its value (section 0.6).
        disabledBackground = HudPalette.HUD_COLOR_ROLE_FIELD_BACKGROUND;
        disabledForeground = HudPalette.HUD_COLOR_ROLE_DISABLED;
    }

    /**
     * Replaces FlatLaf's default (white/bright) popup border with the frame of the field the list drops from.
     */
    @Override
    protected javax.swing.plaf.basic.ComboPopup createPopup() {
        javax.swing.plaf.basic.ComboPopup popup = super.createPopup();
        if (popup instanceof javax.swing.plaf.basic.BasicComboPopup basic) {
            // WHY on open: DefaultCellEditor marks a table cell's combo after this popup is built.
            basic.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    boolean tableCell = AppTheme.isTableCellCombo(comboBox);
                    basic.setBorder(BorderFactory.createLineBorder(
                            tableCell ? HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION : HudPalette.HUD_COLOR_ROLE_FRAME_BORDER,
                            HudPalette.HUD_BORDER_THICKNESS));
                    basic.getList().setBackground(
                            tableCell ? HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND : HudPalette.HUD_COLOR_ROLE_FIELD_BACKGROUND);
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
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
     * Fills the current-value area with the component's own background,
     * bypassing FlatLaf's focus-ring and palette logic.
     */
    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        g.setColor(comboBox.getBackground());
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /** Draws the ▼ cell; in a table cell, a flat down triangle with no background or border box. */
    private final class ArrowButton extends JButton {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                if (comboBox != null && !AppTheme.isTableCellCombo(comboBox)) {
                    g2.setColor(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER);
                    g2.drawLine(0, 0, 0, getHeight() - 1);
                }
                Color c = (comboBox != null && comboBox.isEnabled()) ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION : HudPalette.HUD_COLOR_ROLE_DISABLED;
                HudGlyphs.paintHudArrowDown(g2, 0, 0, getWidth(), getHeight(), c);
            } finally {
                g2.dispose();
            }
        }
    }
}
