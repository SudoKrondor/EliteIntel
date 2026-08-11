package elite.intel.ui.screen;

import elite.intel.ui.screen.bindings.BindingManagementPanel;
import elite.intel.ui.screen.bindings.BindingProfilePanel;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Outer BIND FORGE tab shell hosting the "Binding Profile" and "Binding Management" sub-tabs,
 * matching the {@code AppTheme.makeSectionTabs()} pattern already used by
 * {@link ActionsTabPanel} and {@link SettingsTabPanel}.
 */
public class BindingsTabPanel extends JPanel {

    private final BindingProfilePanel bindingProfilePanel = new BindingProfilePanel();
    private final BindingManagementPanel bindingManagementPanel = new BindingManagementPanel();

    public BindingsTabPanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        setBorder(AppTheme.hudScreenBorder());

        JTabbedPane tabs = AppTheme.makeSectionTabs();
        tabs.setTabPlacement(JTabbedPane.TOP);
        tabs.addTab(getText("bindings.tab.bindingProfile"), bindingProfilePanel);
        tabs.addTab(getText("bindings.tab.bindingManagement"), bindingManagementPanel);

        add(tabs, BorderLayout.CENTER);
    }

    public void dispose() {
        bindingProfilePanel.dispose();
    }

    /**
     * Shows a modal dialog when the user closes the application with an unapplied
     * binding draft. Delegates to the Binding Profile sub-tab, the only one with state to lose.
     */
    public void promptCloseWithDraft() {
        bindingProfilePanel.promptCloseWithDraft();
    }

    public void initData() {
        bindingProfilePanel.initData();
        bindingManagementPanel.initData();
    }
}
