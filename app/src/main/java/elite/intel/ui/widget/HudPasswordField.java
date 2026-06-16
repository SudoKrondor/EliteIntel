package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

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
        setPreferredSize(new Dimension(0, HudPalette.HUD_FIELD_HEIGHT));
    }
}
