package elite.intel.ui.view.settings;

import elite.intel.ai.brain.LocalLlmProvider;
import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.RestartBrainEvent;
import elite.intel.ui.event.RestartMouthEvent;
import elite.intel.ui.event.TTSProviderChangedEvent;
import elite.intel.ui.view.HudSection;
import elite.intel.ui.view.HudSegmentedControl;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.view.AppTheme.*;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class LocalLlmSettingsPanel extends JPanel {

    private final SystemSession systemSession = SystemSession.getInstance();

    private JTextField localLlmAddressField;
    private JTextField localLlmModelCommandField;
    private JTextField localLlmModelQueryField;
    private JCheckBox useLocalCommandLLMCheck;
    private JCheckBox useLocalQueryLLMCheck;
    private JCheckBox useLocalTtsCheck;
    private HudSegmentedControl providerControl;

    // Segment indices for the Ollama/LM Studio provider selector.
    private static final int PROVIDER_OLLAMA = 0;
    private static final int PROVIDER_LMSTUDIO = 1;

    private LocalLlmProvider currentProvider;
    private Runnable onLocalLlmChanged;
    private Runnable onLocalTtsChanged;

    public void setOnLocalLlmChanged(Runnable r) {
        onLocalLlmChanged = r;
    }

    public void setOnLocalTtsChanged(Runnable r) {
        onLocalTtsChanged = r;
    }

    public LocalLlmSettingsPanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_BG);

        HudSection fieldsSection = new HudSection(getText("settings.localLlm.section.models"), new GridBagLayout());
        JPanel fields = fieldsSection.body();
        GridBagConstraints gc = baseGbc();

        addLabel(fields, getText("settings.localLlm.address"), gc);
        localLlmAddressField = makeTextField();
        addField(fields, localLlmAddressField, gc, 1, 1.0);

        nextRow(gc);
        addLabel(fields, getText("settings.localLlm.command"), gc);
        localLlmModelCommandField = makeTextField();
        addField(fields, localLlmModelCommandField, gc, 1, 1.0);
        useLocalCommandLLMCheck = makeCheckBox(getText("settings.cloud.use"), false);
        useLocalCommandLLMCheck.addActionListener(e -> onCheckboxToggled());
        addCheck(fields, useLocalCommandLLMCheck, gc);

        nextRow(gc);
        addLabel(fields, getText("settings.localLlm.query"), gc);
        localLlmModelQueryField = makeTextField();
        addField(fields, localLlmModelQueryField, gc, 1, 1.0);
        useLocalQueryLLMCheck = makeCheckBox(getText("settings.cloud.use"), false);
        useLocalQueryLLMCheck.addActionListener(e -> onCheckboxToggled());
        addCheck(fields, useLocalQueryLLMCheck, gc);

        providerControl = new HudSegmentedControl(
                new String[]{getText("settings.localLlm.ollama"), getText("settings.localLlm.lmStudio")},
                PROVIDER_OLLAMA);
        providerControl.addChangeListener(e -> onProviderSelected(
                providerControl.getSelectedIndex() == PROVIDER_OLLAMA
                        ? LocalLlmProvider.OLLAMA
                        : LocalLlmProvider.LMSTUDIO));

        HudSection providerSection = new HudSection(getText("settings.localLlm.section.provider"), new FlowLayout(FlowLayout.LEFT, HUD_GAP, 0));
        JPanel providerPanel = providerSection.body();
        providerPanel.add(new JLabel(getText("settings.localLlm.host")));
        providerPanel.add(providerControl);

        JPanel buttons = transparentPanel(new FlowLayout(FlowLayout.LEFT, HUD_GAP, 0));

        JButton saveButton = makeButton(getText("button.save"));
        saveButton.addActionListener(e -> save());

        JButton restoreButton = makeButton(getText("button.restoreDefaults"));
        restoreButton.addActionListener(e -> SwingUtilities.invokeLater(() -> {
            setDefaults();
        }));

        buttons.add(saveButton);
        buttons.add(restoreButton);

        useLocalTtsCheck = makeCheckBox(getText("settings.audio.useLocalTts"), false);
        useLocalTtsCheck.addActionListener(a -> saveLocalTts());
        JPanel ttsRow = transparentPanel(new FlowLayout(FlowLayout.LEFT, HUD_GAP, 0));
        ttsRow.add(useLocalTtsCheck);

        JPanel content = transparentPanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(ttsRow);
        content.add(Box.createVerticalStrut(12));
        content.add(fieldsSection);
        content.add(Box.createVerticalStrut(12));
        content.add(providerSection);
        content.add(Box.createVerticalStrut(12));
        content.add(buttons);

        add(content, BorderLayout.NORTH);
    }

    private void setDefaults() {
        currentProvider = LocalLlmProvider.LMSTUDIO;
        providerControl.setSelectedIndex(PROVIDER_LMSTUDIO);
        useLocalCommandLLMCheck.setSelected(true);
        useLocalQueryLLMCheck.setSelected(true);
        localLlmAddressField.setText(LocalLlmProvider.LMSTUDIO.getDefaultUrl());
        localLlmModelCommandField.setText("matrixportalx/tulu-3.1-8b-supernova");
        localLlmModelQueryField.setText("matrixportalx/tulu-3.1-8b-supernova");
        save();
    }

    public void initData() {
        LocalLlmProvider provider = systemSession.getLocalLlmProvider();
        currentProvider = provider;
        providerControl.setSelectedIndex(provider == LocalLlmProvider.OLLAMA ? PROVIDER_OLLAMA : PROVIDER_LMSTUDIO);
        loadProviderFieldsIntoUi(provider);
        useLocalCommandLLMCheck.setSelected(systemSession.useLocalCommandLlm());
        useLocalQueryLLMCheck.setSelected(systemSession.useLocalQueryLlm());
        useLocalTtsCheck.setSelected(systemSession.useLocalTTS());
    }

    /**
     * Called by CloudServicesSettingsPanel when the user activates cloud TTS. Delegates to
     * {@link #saveLocalTts()} so the confirmation dialog and voice-reset logic fire identically
     * to the user clicking the checkbox.
     */
    public void activateCloudTts() {
        useLocalTtsCheck.setSelected(false);
        saveLocalTts();
    }

    private void saveLocalTts() {
        boolean newValue = useLocalTtsCheck.isSelected();
        boolean oldValue = systemSession.useLocalTTS();
        if (newValue != oldValue) {
            String defaultVoice = newValue ? KokoroVoices.BELLA.name() : GoogleVoices.EMMA.name();
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    getText("settings.audio.switchTts.message"),
                    getText("settings.audio.switchTts.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                useLocalTtsCheck.setSelected(oldValue);
                return;
            }
            ShipManager.getInstance().resetAllVoicesToDefault(defaultVoice);
        }
        systemSession.setUseLocalTTS(newValue);
        EventBusManager.publish(new TTSProviderChangedEvent());
        EventBusManager.publish(new RestartMouthEvent());
        if (onLocalTtsChanged != null) onLocalTtsChanged.run();
    }

    private void loadProviderFieldsIntoUi(LocalLlmProvider provider) {
        String addr, cmd, qry;
        if (provider == LocalLlmProvider.OLLAMA) {
            addr = systemSession.getOllamaAddress();
            cmd = systemSession.getOllamaCommandModel();
            qry = systemSession.getOllamaQueryModel();
        } else {
            addr = systemSession.getLmStudioAddress();
            cmd = systemSession.getLmStudioCommandModel();
            qry = systemSession.getLmStudioQueryModel();
        }
        localLlmAddressField.setText(addr != null && !addr.isEmpty() ? addr : provider.getDefaultUrl());
        localLlmModelCommandField.setText(cmd != null ? cmd : "");
        localLlmModelQueryField.setText(qry != null ? qry : "");
    }

    private void onProviderSelected(LocalLlmProvider newProvider) {
        if (currentProvider != null && currentProvider != newProvider) {
            saveProviderFields(currentProvider);
        }
        currentProvider = newProvider;
        loadProviderFieldsIntoUi(newProvider);
        save();
        EventBusManager.publish(new AppLogEvent("Local LLM provider set to: " + newProvider.name()));
    }

    private void saveProviderFields(LocalLlmProvider provider) {
        String addr = localLlmAddressField.getText();
        String cmd = localLlmModelCommandField.getText();
        String qry = localLlmModelQueryField.getText();
        if (provider == LocalLlmProvider.OLLAMA) {
            systemSession.setOllamaSettings(addr, cmd, qry);
        } else {
            systemSession.setLmStudioSettings(addr, cmd, qry);
        }
    }

    /**
     * Called by CloudServicesSettingsPanel when the user activates cloud LLM.
     */
    public void deactivateLocalLlm() {
        if (useLocalCommandLLMCheck.isSelected() || useLocalQueryLLMCheck.isSelected()) {
            useLocalCommandLLMCheck.setSelected(false);
            useLocalQueryLLMCheck.setSelected(false);
            onCheckboxToggled();
        }
    }

    private void onCheckboxToggled() {
        save();
        EventBusManager.publish(new AppLogEvent("LLM mode changed: command="
                + (useLocalCommandLLMCheck.isSelected() ? "local" : "cloud")
                + " query=" + (useLocalQueryLLMCheck.isSelected() ? "local" : "cloud")));
        if (onLocalLlmChanged != null) onLocalLlmChanged.run();
    }

    private void save() {
        LocalLlmProvider provider = providerControl.getSelectedIndex() == PROVIDER_LMSTUDIO
                ? LocalLlmProvider.LMSTUDIO : LocalLlmProvider.OLLAMA;
        saveProviderFields(provider);
        systemSession.setLocalLlmProvider(provider);
        systemSession.setUseLocalCommandLlm(useLocalCommandLLMCheck.isSelected());
        systemSession.setUseLocalQueryLlm(useLocalQueryLLMCheck.isSelected());
        EventBusManager.publish(new AppLogEvent("Local LLM config saved"));
        EventBusManager.publish(new RestartBrainEvent());
        initData();
    }
}
