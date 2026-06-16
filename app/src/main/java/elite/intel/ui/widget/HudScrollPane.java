package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Shared scroll pane wrapper for HUD panels, tables, logs, and settings sections.
 */
public class HudScrollPane extends JScrollPane {

    /**
     * Creates a HUD scroll pane around the supplied view component.
     *
     * @param view component displayed in the viewport
     */
    public HudScrollPane(Component view) {
        super(view);
        AppTheme.styleScrollPane(this);
    }
}
