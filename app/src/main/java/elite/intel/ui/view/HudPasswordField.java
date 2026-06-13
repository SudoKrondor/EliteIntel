package elite.intel.ui.view;

import javax.swing.*;
import java.awt.*;

/**
 * HUD-styled password field for API-key and secret inputs.
 */
public class HudPasswordField extends JPasswordField {

    /**
     * Creates an empty HUD password field.
     */
    public HudPasswordField() {
        AppTheme.styleTextComponent(this);
        setPreferredSize(new Dimension(0, AppTheme.HUD_FIELD_HEIGHT));
    }
}
