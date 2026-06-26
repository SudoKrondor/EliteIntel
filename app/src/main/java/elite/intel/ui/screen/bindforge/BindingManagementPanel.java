package elite.intel.ui.screen.bindforge;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * The "Binding Management" sub-tab of BIND FORGE. Placeholder until the backup/restore
 * feature lands as a separate piece of work.
 */
public class BindingManagementPanel extends JPanel {

    public BindingManagementPanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBorder(AppTheme.hudSubtabContentBorder());
        setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        JLabel comingSoon = new JLabel(getText("bindForge.bindingManagement.comingSoon"), SwingConstants.CENTER);
        comingSoon.setForeground(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        add(comingSoon, BorderLayout.CENTER);
    }
}
