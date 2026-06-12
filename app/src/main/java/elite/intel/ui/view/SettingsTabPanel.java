package elite.intel.ui.view;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.EventBusManager;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.SystemShutDownEvent;
import elite.intel.ui.event.UpdateAvailableEvent;
import elite.intel.ui.view.settings.AudioSettingsPanel;
import elite.intel.ui.view.settings.CloudServicesSettingsPanel;
import elite.intel.ui.view.settings.InputSettingsPanel;
import elite.intel.ui.view.settings.LocalLlmSettingsPanel;
import elite.intel.util.Updater;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static elite.intel.ui.view.AppTheme.makeButtonSubtle;
import static elite.intel.ui.view.AppTheme.styleTabbedPane;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class SettingsTabPanel extends JPanel {

    private final LocalLlmSettingsPanel localLlmPanel = new LocalLlmSettingsPanel();
    private final AudioSettingsPanel audioPanel = new AudioSettingsPanel();
    private final InputSettingsPanel inputPanel = new InputSettingsPanel();
    private final CloudServicesSettingsPanel cloudPanel = new CloudServicesSettingsPanel();

    private JButton updateAppButton;

    public SettingsTabPanel() {
        EventBusManager.register(this);
        buildUi();
        cloudPanel.setOnCloudLlmUsed(() -> localLlmPanel.deactivateLocalLlm());
        cloudPanel.setOnCloudTtsUsed(() -> audioPanel.activateCloudTts());
        localLlmPanel.setOnLocalLlmChanged(() -> cloudPanel.syncUseCheckboxes());
        audioPanel.setOnLocalTtsChanged(() -> cloudPanel.syncUseCheckboxes());
    }

    public void dispose() {
        inputPanel.dispose();
        EventBusManager.unregister(this);
    }

    private void buildUi() {
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setTabPlacement(JTabbedPane.TOP);
        styleTabbedPane(tabs);
        tabs.addTab(getText("settings.tab.localLlm"), scaledIcon("/images/local-llm.png"), localLlmPanel);
        tabs.addTab(getText("settings.tab.audio"), scaledIcon("/images/audio.png"), audioPanel);
        tabs.addTab(getText("settings.tab.input"), scaledIcon("/images/controller.png"), inputPanel);
        tabs.addTab(getText("settings.tab.cloudServices"), scaledIcon("/images/cloud.png"), cloudPanel);

        updateAppButton = makeButtonSubtle(getText("settings.update.upToDate"));
        updateAppButton.setEnabled(false);
        updateAppButton.setIcon(scaledIcon("/images/update.png"));
        updateAppButton.addActionListener(e -> {
            updateAppButton.setEnabled(false);
            updateAppButton.setText(getText("settings.update.updating"));
            Updater.performUpdateAsync().thenAccept(launched -> {
                if (launched) {
                    EventBusManager.publish(new SystemShutDownEvent());
                } else {
                    SwingUtilities.invokeLater(() -> {
                        updateAppButton.setEnabled(true);
                        updateAppButton.setText(getText("settings.update.available"));
                    });
                    EventBusManager.publish(new AppLogEvent(
                            "Could not launch updater - is elite_intel_updater.jar present?"));
                }
            });
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        footer.setOpaque(false);
        footer.add(updateAppButton);

        add(tabs, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    public void initData() {
        localLlmPanel.initData();
        audioPanel.initData();
        inputPanel.initData();
        cloudPanel.initData();
    }

    @Subscribe
    public void onUpdateAvailableEvent(UpdateAvailableEvent event) {
        SwingUtilities.invokeLater(() -> {
            updateAppButton.setEnabled(true);
            updateAppButton.setText(getText("settings.update.available"));
        });
    }

    private ImageIcon scaledIcon(String resource) {
        return new ImageIcon(
                new ImageIcon(Objects.requireNonNull(getClass().getResource(resource)))
                        .getImage().getScaledInstance(42, 42, Image.SCALE_SMOOTH));
    }
}
