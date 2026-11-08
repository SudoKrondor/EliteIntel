package elite.intel.ui.screen;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.storage.ReadOnlyMode;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.BindForgeReadOnlyChangedEvent;
import elite.intel.ui.screen.bindings.*;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Outer BIND FORGE tab shell hosting the "Binding Profile", "Binding Management" and "Alias Designer" sub-tabs,
 * matching the {@code AppTheme.makeSectionTabs()} pattern already used by
 * {@link ActionsTabPanel} and {@link ServicesTabPanel}.
 * <p>
 * <strong>The one APPLY / DISCARD bar sits above the sub-tabs</strong>, beneath Elite-Intel's main tabs, where the mouse
 * goes to leave BindForge (overview.md, <em>APPLY and DISCARD - one pair</em>; Storage S4a). Leaving it with edits not
 * applied asks first ({@link #leave}; Storage S7).
 * <p>
 * <strong>While the user has no binds file of their own, the editors are greyed</strong> until CREATE is pressed on the
 * bar; File Manager stays usable, so a returning player can restore a backup instead (storage.md rule 16; Storage S11).
 */
public class BindingsTabPanel extends JPanel {

    private final BindingProfilePanel bindingProfilePanel = new BindingProfilePanel();
    private final BindingManagementPanel bindingManagementPanel = new BindingManagementPanel();
    private final AliasDesignerPanel aliasDesignerPanel = new AliasDesignerPanel();
    private final FirstTimeStartupFlow firstTimeStartup = new FirstTimeStartupFlow(this);
    // WHY: the binds files are in this folder, and Game Install Locations names it - a new one is read again there.
    private final BindingsFolderField bindingsFolder =
            new BindingsFolderField(bindingManagementPanel::refreshInstallLocations);
    private final ApplyBar applyBar = new ApplyBar(bindingsFolder);
    private final LeavePrompt leavePrompt = applyBar.leavePrompt(this);
    /** Whether read only was on when last heard. On the EDT. */
    private boolean readOnly;

    public BindingsTabPanel() {
        buildUi();
        // WHY: first-time startup runs from a click on the BindForge tab - this one, not a sub-tab (storage.md rule 1) -
        // every time it is shown while the flag is not set, so one abandoned is offered again (Alan, 2026-10-25). It may
        // open over whichever sub-tab is showing.
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !isShowing()) return;
            firstTimeStartup.offerIfNeeded();
            applyBar.refreshCreate();
        });
        UiBus.register(this);
        // WHY after registering: read only may have been worked out before this panel existed, and its event missed.
        readOnly = ReadOnlyMode.stored().activeIfKnown();
    }

    /**
     * Read only ending - OVERRIDE, or the last missing installation removed in Game Install Locations - runs what it held
     * back straight away, on screen: the check, first-time startup, the offer of the newest backup (Storage S14a Q3;
     * S14c, Alan, 2026-11-06). Not at the first work-out, which only finds out whether it is on.
     */
    @Subscribe
    public void onReadOnlyChanged(BindForgeReadOnlyChangedEvent event) {
        SwingUtilities.invokeLater(() -> {
            boolean now = ReadOnlyMode.stored().activeIfKnown();
            boolean ended = readOnly && !now;
            readOnly = now;
            if (ended && isShowing()) firstTimeStartup.offerIfNeeded();
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
        // WHY: the binds file is chosen in File Manager and shown in the Bind Editor - LOAD opens it there, so the press
        // shows its result (Storage S15, D1, Alan, 2026-11-04).
        bindingManagementPanel.setBindEditor(new BindsFileSection.BindEditor() {
            @Override
            public String shown() {
                return bindingProfilePanel.shownFile();
            }

            @Override
            public void load(String file) {
                bindingProfilePanel.load(file);
                tabs.setSelectedComponent(bindingProfilePanel);
            }

            @Override
            public void showRenamed(String file) {
                // WHY no tab switch: the rename was pressed in File Manager, and its result is shown there (Storage
                // S16, Q1, Alan, 2026-11-04).
                bindingProfilePanel.load(file);
            }
        });

        add(applyBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        applyBar.onCreateState(state -> editorsUsable(tabs, state.editors()));
    }

    /**
     * Greys the editor tabs, or brings them back. A greyed tab that is showing gives way to File Manager, so no editor
     * is left on screen with nothing of the user's to edit (Alan, 2026-11-02, Storage S11). The Preset Editor's tab,
     * when it is built (P4), is greyed with these.
     */
    private void editorsUsable(JTabbedPane tabs, boolean usable) {
        for (Component editor : new Component[]{bindingProfilePanel, aliasDesignerPanel}) {
            int index = tabs.indexOfComponent(editor);
            tabs.setEnabledAt(index, usable);
            tabs.setToolTipTextAt(index, usable ? null : getText("bindings.create.tabLocked"));
        }
        // WHY: File Manager's Bindings File section opens a file in an editor, so it is greyed with them (S15, D3).
        bindingManagementPanel.setEditorsUsable(usable);
        if (!usable && tabs.getSelectedComponent() != bindingManagementPanel) {
            tabs.setSelectedComponent(bindingManagementPanel);
        }
    }

    /**
     * Leaving BindForge - another main tab, or closing Elite-Intel: runs {@code leave} now, later or never, as the user
     * chooses when there are edits not applied (storage.md rule 9). On the EDT.
     */
    public void leave(LeavePrompt.Leaving leaving, Runnable leave) {
        leavePrompt.request(leaving, leave);
    }

    public void dispose() {
        UiBus.unregister(this);
        bindingProfilePanel.dispose();
        bindingManagementPanel.dispose();
        aliasDesignerPanel.dispose();
        applyBar.dispose();
    }

    public void initData() {
        bindingsFolder.refresh();
        bindingProfilePanel.initData();
        bindingManagementPanel.initData();
        aliasDesignerPanel.initData();
    }
}
