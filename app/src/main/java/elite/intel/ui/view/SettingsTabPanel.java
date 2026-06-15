package elite.intel.ui.view;

import elite.intel.ui.view.settings.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class SettingsTabPanel extends JPanel {

    private static final int AI_TAB_INDEX = 0;

    private final CommonSettingsPanel commonPanel = new CommonSettingsPanel();
    private final AiServicesSettingsPanel aiServicesPanel = new AiServicesSettingsPanel();
    private final AudioSettingsPanel audioPanel = new AudioSettingsPanel();
    private final InputSettingsPanel inputPanel = new InputSettingsPanel();

    private HudUpdateButton updateAppButton;

    // Tracks the previously-selected section tab so the unsaved-changes guard can revert.
    private int lastTabIndex = AI_TAB_INDEX;
    private boolean tabGuardActive = false;

    public SettingsTabPanel() {
        buildUi();
    }

    public void dispose() {
        inputPanel.dispose();
        if (updateAppButton != null) updateAppButton.dispose();
    }

    private void buildUi() {
        setLayout(new BorderLayout(AppTheme.HUD_GAP, AppTheme.HUD_GAP));
        setBackground(AppTheme.HUD_BG);
        setBorder(AppTheme.hudScreenBorder());

        JTabbedPane tabs = AppTheme.makeSectionTabs();
        tabs.setTabPlacement(JTabbedPane.TOP);
        tabs.addTab(getText("settings.tab.aiServices"), aiServicesPanel);
        tabs.addTab(getText("settings.tab.audio"), audioPanel);
        tabs.addTab(getText("settings.tab.comms"), inputPanel);
        tabs.addChangeListener(e -> guardAiServicesTab(tabs));

        updateAppButton = new HudUpdateButton(false);

        // Non-modal footer: no BACK, no status — just the update action on the right (shared rail).
        JPanel footer = HudFooter.build(false, null, null, List.of(updateAppButton));

        add(commonPanel, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    public void initData() {
        commonPanel.initData();
        aiServicesPanel.initData();
        audioPanel.initData();
        inputPanel.initData();
    }

    /**
     * When leaving the AI Services tab with unsaved edits, prompts to Save / Discard / Keep editing.
     * Save or Discard let the switch proceed; Keep editing (or an aborted save) reverts the selection.
     */
    private void guardAiServicesTab(JTabbedPane tabs) {
        if (tabGuardActive) return;
        int newIndex = tabs.getSelectedIndex();
        if (lastTabIndex == AI_TAB_INDEX && newIndex != AI_TAB_INDEX && aiServicesPanel.isDirty()) {
            HudConfirmDialog.Result choice = HudConfirmDialog.show(
                    this,
                    getText("settings.ai.unsaved.title"),
                    getText("settings.ai.unsaved.message"),
                    getText("button.save"),                 // primary
                    getText("settings.ai.discardChanges"),  // extra
                    getText("settings.ai.keepEditing"));    // dismiss
            if (choice == HudConfirmDialog.Result.PRIMARY) {        // Save
                if (!aiServicesPanel.save()) {
                    revertToAiTab(tabs);
                    return;
                }
            } else if (choice == HudConfirmDialog.Result.EXTRA) {   // Discard
                aiServicesPanel.reload();
            } else {                                                // Keep editing / closed
                revertToAiTab(tabs);
                return;
            }
        }
        lastTabIndex = tabs.getSelectedIndex();
    }

    private void revertToAiTab(JTabbedPane tabs) {
        tabGuardActive = true;
        tabs.setSelectedIndex(AI_TAB_INDEX);
        tabGuardActive = false;
        lastTabIndex = AI_TAB_INDEX;
    }
}
