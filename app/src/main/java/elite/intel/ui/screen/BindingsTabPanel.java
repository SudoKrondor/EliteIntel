package elite.intel.ui.screen;

import elite.intel.ui.screen.bindings.AliasDesignerPanel;
import elite.intel.ui.screen.bindings.ApplyBar;
import elite.intel.ui.screen.bindings.BindingManagementPanel;
import elite.intel.ui.screen.bindings.BindingProfilePanel;
import elite.intel.ui.screen.bindings.FirstTimeStartupFlow;
import elite.intel.ui.screen.bindings.LeavePrompt;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Outer BIND FORGE tab shell hosting the "Binding Profile", "Binding Management" and "Alias Designer" sub-tabs,
 * matching the {@code AppTheme.makeSectionTabs()} pattern already used by
 * {@link ActionsTabPanel} and {@link SettingsTabPanel}.
 * <p>
 * <strong>The one APPLY / DISCARD bar sits above the sub-tabs</strong>, beneath Elite-Intel's main tabs, where the mouse
 * goes to leave BindForge (overview.md, <em>APPLY and DISCARD - one pair</em>; Storage S4a). Leaving it with edits not
 * applied asks first ({@link #leave}; Storage S7).
 */
public class BindingsTabPanel extends JPanel {

    private final BindingProfilePanel bindingProfilePanel = new BindingProfilePanel();
    private final BindingManagementPanel bindingManagementPanel = new BindingManagementPanel();
    private final AliasDesignerPanel aliasDesignerPanel = new AliasDesignerPanel();
    private final FirstTimeStartupFlow firstTimeStartup = new FirstTimeStartupFlow(this);
    private final ApplyBar applyBar = new ApplyBar();
    private final LeavePrompt leavePrompt = applyBar.leavePrompt(this);

    public BindingsTabPanel() {
        buildUi();
        // WHY: first-time startup runs from a click on the BindForge tab - this one, not a sub-tab (storage.md rule 1) -
        // every time it is shown while the flag is not set, so one abandoned is offered again (Alan, 2026-10-25). It may
        // open over whichever sub-tab is showing.
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !isShowing()) return;
            firstTimeStartup.offerIfNeeded();
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

        add(applyBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    /**
     * Leaving BindForge - another main tab, or closing Elite-Intel: runs {@code leave} now, later or never, as the user
     * chooses when there are edits not applied (storage.md rule 9). On the EDT.
     */
    public void leave(LeavePrompt.Leaving leaving, Runnable leave) {
        leavePrompt.request(leaving, leave);
    }

    public void dispose() {
        bindingProfilePanel.dispose();
        aliasDesignerPanel.dispose();
        applyBar.dispose();
    }

    public void initData() {
        bindingProfilePanel.initData();
        bindingManagementPanel.initData();
        aliasDesignerPanel.initData();
    }
}
