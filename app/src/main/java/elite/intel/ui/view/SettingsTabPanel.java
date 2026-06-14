package elite.intel.ui.view;

import elite.intel.ui.view.settings.AudioSettingsPanel;
import elite.intel.ui.view.settings.CloudServicesSettingsPanel;
import elite.intel.ui.view.settings.InputSettingsPanel;
import elite.intel.ui.view.settings.LocalLlmSettingsPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class SettingsTabPanel extends JPanel {

    private final LocalLlmSettingsPanel localLlmPanel = new LocalLlmSettingsPanel();
    private final AudioSettingsPanel audioPanel = new AudioSettingsPanel();
    private final InputSettingsPanel inputPanel = new InputSettingsPanel();
    private final CloudServicesSettingsPanel cloudPanel = new CloudServicesSettingsPanel();
    private final CustomSettingsTabPanel customPanel = new CustomSettingsTabPanel();

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

        JTabbedPane tabs = AppTheme.makeStandardTabs();
        tabs.setTabPlacement(JTabbedPane.TOP);
        tabs.addTab(getText("settings.tab.localLlm"), scaledIcon("/images/local-llm.png"), localLlmPanel);
        tabs.addTab(getText("settings.tab.audio"), scaledIcon("/images/audio.png"), audioPanel);
        tabs.addTab(getText("settings.tab.comms"), scaledIcon("/images/communications.png"), inputPanel);
        tabs.addTab(getText("settings.tab.cloudServices"), scaledIcon("/images/cloud.png"), cloudPanel);
        // TODO: replace controller.png with a dedicated custom-settings icon
        tabs.addTab(getText("settings.tab.custom"), scaledIcon("/images/controller.png"), customPanel);

        updateAppButton = new HudUpdateButton();

        // Non-modal footer: no BACK, no status — just the update action on the right (shared rail).
        JPanel footer = HudFooter.build(false, null, null, List.of(updateAppButton));

        HudSection section = new HudSection(getText("settings.section.systemConfiguration"), new BorderLayout());
        section.body().add(tabs, BorderLayout.CENTER);
        add(section, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    public void initData() {
        localLlmPanel.initData();
        audioPanel.initData();
        inputPanel.initData();
        cloudPanel.initData();
        customPanel.initData();
    }

    private ImageIcon scaledIcon(String resource) {
        return AppTheme.scaledIcon(getClass(), resource, AppTheme.HUD_ICON_MAIN);
    }
}
