package elite.intel.ui.screen;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Groups bindings, command catalog, and future customCommands under one Action Center tab.
 */
public class ActionCenterTabPanel extends JPanel {

    private final BindingsTabPanel bindingsTabPanel = new BindingsTabPanel();
    private final CommandCatalogTablePanel commandCatalogTablePanel = new CommandCatalogTablePanel();
    private final CustomCommandsTabPanel customCommandsTabPanel = new CustomCommandsTabPanel();

    public ActionCenterTabPanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        setBorder(AppTheme.hudScreenBorder());

        JTabbedPane tabs = AppTheme.makeSectionTabs();
        tabs.setTabPlacement(JTabbedPane.TOP);
        tabs.addTab(getText("actions.tab.bindings"), bindingsTabPanel);
        tabs.addTab(getText("actions.tab.commands"), commandCatalogTablePanel);
        tabs.addTab(getText("actions.tab.customCommands"), customCommandsTabPanel);

        add(tabs, BorderLayout.CENTER);
    }

    public void initData() {
        bindingsTabPanel.initData();
        commandCatalogTablePanel.initData();
        customCommandsTabPanel.initData();
    }

    /** Forwards close-with-draft prompt to the bindings panel. Must be called on the EDT. */
    public void promptCloseWithDraft() {
        bindingsTabPanel.promptCloseWithDraft();
    }

    public void dispose() {
        bindingsTabPanel.dispose();
    }
}
