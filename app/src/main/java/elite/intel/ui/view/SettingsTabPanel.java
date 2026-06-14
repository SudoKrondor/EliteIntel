package elite.intel.ui.view;

import elite.intel.ui.view.settings.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class SettingsTabPanel extends JPanel {

    private final CommonSettingsPanel commonPanel = new CommonSettingsPanel();
    private final LocalLlmSettingsPanel localLlmPanel = new LocalLlmSettingsPanel();
    private final AudioSettingsPanel audioPanel = new AudioSettingsPanel();
    private final InputSettingsPanel inputPanel = new InputSettingsPanel();
    private final CloudServicesSettingsPanel cloudPanel = new CloudServicesSettingsPanel();

    private HudUpdateButton updateAppButton;

    public SettingsTabPanel() {
        buildUi();
        cloudPanel.setOnCloudLlmUsed(() -> localLlmPanel.deactivateLocalLlm());
        cloudPanel.setOnCloudTtsUsed(() -> audioPanel.activateCloudTts());
        localLlmPanel.setOnLocalLlmChanged(() -> cloudPanel.syncUseCheckboxes());
        audioPanel.setOnLocalTtsChanged(() -> cloudPanel.syncUseCheckboxes());
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
        tabs.addTab(getText("settings.tab.localLlm"), localLlmPanel);
        tabs.addTab(getText("settings.tab.audio"), audioPanel);
        tabs.addTab(getText("settings.tab.comms"), inputPanel);
        tabs.addTab(getText("settings.tab.cloudServices"), cloudPanel);

        updateAppButton = new HudUpdateButton();

        // Non-modal footer: no BACK, no status — just the update action on the right (shared rail).
        JPanel footer = HudFooter.build(false, null, null, List.of(updateAppButton));

        add(commonPanel, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    public void initData() {
        commonPanel.initData();
        localLlmPanel.initData();
        audioPanel.initData();
        inputPanel.initData();
        cloudPanel.initData();
    }
}
