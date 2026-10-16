package elite.intel.ui.screen;

import elite.intel.ui.screen.bindings.AliasDesignerPanel;
import elite.intel.ui.screen.bindings.BindingManagementPanel;
import elite.intel.ui.screen.bindings.BindingProfilePanel;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Outer BIND FORGE tab shell hosting the "Binding Profile" and "Binding Management" sub-tabs,
 * matching the {@code AppTheme.makeSectionTabs()} pattern already used by
 * {@link ActionsTabPanel} and {@link SettingsTabPanel}.
 */
public class BindingsTabPanel extends JPanel {

    private final BindingProfilePanel bindingProfilePanel = new BindingProfilePanel();
    private final BindingManagementPanel bindingManagementPanel = new BindingManagementPanel();
    private final AliasDesignerPanel aliasDesignerPanel = new AliasDesignerPanel();

    public BindingsTabPanel() {
        buildUi();
        // WHY: activation is the first opening of the BindForge tab - this one, not the Alias Designer sub-tab
        // (Alan, 2026-10-13, A1b). Opening BindForge is the user coming to it; first setup may open over whichever
        // sub-tab is showing.
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !isShowing()) return;
            aliasDesignerPanel.activateOnFirstShowing();
        });
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        setBorder(AppTheme.hudScreenBorder());

        JTabbedPane tabs = AppTheme.makeSectionTabs();
        tabs.setTabPlacement(JTabbedPane.TOP);
        tabs.addTab(getText("bindings.tab.bindingProfile"), bindingProfilePanel);
        tabs.addTab(getText("bindings.tab.bindingManagement"), bindingManagementPanel);
        tabs.addTab(getText("bindings.tab.aliasDesigner"), aliasDesignerPanel);
        // WHY: a retarget keeps bindings by button number, so on another model some land on other buttons - the
        // notice after it offers the bindings for review (A12).
        aliasDesignerPanel.setReviewBindings(() -> tabs.setSelectedComponent(bindingProfilePanel));

        add(tabs, BorderLayout.CENTER);
    }

    public void dispose() {
        bindingProfilePanel.dispose();
        aliasDesignerPanel.dispose();
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
        aliasDesignerPanel.initData();
    }
}
