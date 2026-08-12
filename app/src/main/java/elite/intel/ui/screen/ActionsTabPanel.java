package elite.intel.ui.screen;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Groups the built-in commands and customCommands under one Actions tab.
 */
public class ActionsTabPanel extends JPanel {

    private final BuiltInCommandsTabPanel builtInCommandsTabPanel = new BuiltInCommandsTabPanel();
    private final CustomCommandsTabPanel customCommandsTabPanel = new CustomCommandsTabPanel();

    public ActionsTabPanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        setBorder(AppTheme.hudScreenBorder());

        JTabbedPane tabs = AppTheme.makeSectionTabs();
        tabs.setTabPlacement(JTabbedPane.TOP);
        tabs.addTab(getText("actions.tab.commands"), builtInCommandsTabPanel);
        tabs.addTab(getText("actions.tab.customCommands"), customCommandsTabPanel);

        add(tabs, BorderLayout.CENTER);
    }

    public void initData() {
        builtInCommandsTabPanel.initData();
        customCommandsTabPanel.initData();
    }

    /**
     * Releases the built-in commands tab's event-bus subscriptions; safe to call more than once.
     */
    public void dispose() {
        builtInCommandsTabPanel.dispose();
    }
}
