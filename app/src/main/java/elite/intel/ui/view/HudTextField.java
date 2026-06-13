package elite.intel.ui.view;

import javax.swing.*;
import java.awt.*;

/**
 * HUD-styled single-line text field with shared dark input styling.
 */
public class HudTextField extends JTextField {

    /**
     * Creates an empty HUD text field.
     */
    public HudTextField() {
        AppTheme.styleTextComponent(this);
        setPreferredSize(new Dimension(0, AppTheme.HUD_FIELD_HEIGHT));
    }
}
