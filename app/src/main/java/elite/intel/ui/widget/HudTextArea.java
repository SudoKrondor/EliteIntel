package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;

import javax.swing.*;

/**
 * HUD-styled multi-line text area for logs, diagnostics, and details panels.
 */
public class HudTextArea extends JTextArea {

    /**
     * Creates a HUD text area with the requested size hint.
     *
     * @param rows preferred text rows
     * @param columns preferred text columns
     */
    public HudTextArea(int rows, int columns) {
        super(rows, columns);
        setLineWrap(true);
        setWrapStyleWord(true);
        AppTheme.styleTextComponent(this);
    }
}
