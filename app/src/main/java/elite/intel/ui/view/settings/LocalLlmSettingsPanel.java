package elite.intel.ui.view.settings;

import elite.intel.ai.brain.LocalLlmProvider;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.RestartBrainEvent;
import elite.intel.ui.view.HudSection;

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
    private JRadioButton ollamaRadio;
    private JRadioButton lmStudioRadio;

    private LocalLlmProvider currentProvider;
    private Runnable onLocalLlmChanged;

    public void setOnLocalLlmChanged(Runnable r) {
        onLocalLlmChanged = r;
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

        ollamaRadio = new JRadioButton(getText("settings.localLlm.ollama"));
        lmStudioRadio = new JRadioButton(getText("settings.localLlm.lmStudio"));
        styleCheckBox(ollamaRadio);
        styleCheckBox(lmStudioRadio);
        ButtonGroup providerGroup = new ButtonGroup();
        providerGroup.add(ollamaRadio);
        providerGroup.add(lmStudioRadio);
        ollamaRadio.addActionListener(e -> onProviderSelected(LocalLlmProvider.OLLAMA));
        lmStudioRadio.addActionListener(e -> onProviderSelected(LocalLlmProvider.LMSTUDIO));

        HudSection providerSection = new HudSection(getText("settings.localLlm.section.provider"), new FlowLayout(FlowLayout.LEFT, HUD_GAP, 0));
        JPanel providerPanel = providerSection.body();
        providerPanel.add(new JLabel(getText("settings.localLlm.host")));
        providerPanel.add(ollamaRadio);
        providerPanel.add(lmStudioRadio);

        JPanel buttons = transparentPanel(new FlowLayout(FlowLayout.LEFT, HUD_GAP, 0));

        JButton saveButton = makeButton(getText("button.save"));
        saveButton.addActionListener(e -> save());

        JButton restoreButton = makeButton(getText("button.restoreDefaults"));
        restoreButton.addActionListener(e -> SwingUtilities.invokeLater(() -> {
            setDefaults();
        }));

        buttons.add(saveButton);
        buttons.add(restoreButton);

        JPanel content = transparentPanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(fieldsSection);
        content.add(Box.createVerticalStrut(12));
        content.add(providerSection);
        content.add(Box.createVerticalStrut(12));
        content.add(buttons);

        add(content, BorderLayout.NORTH);
    }

    private void setDefaults() {
        currentProvider = LocalLlmProvider.LMSTUDIO;
        lmStudioRadio.setSelected(true);
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
        ollamaRadio.setSelected(provider == LocalLlmProvider.OLLAMA);
        lmStudioRadio.setSelected(provider == LocalLlmProvider.LMSTUDIO);
        loadProviderFieldsIntoUi(provider);
        useLocalCommandLLMCheck.setSelected(systemSession.useLocalCommandLlm());
        useLocalQueryLLMCheck.setSelected(systemSession.useLocalQueryLlm());
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
        LocalLlmProvider provider = lmStudioRadio.isSelected() ? LocalLlmProvider.LMSTUDIO : LocalLlmProvider.OLLAMA;
        saveProviderFields(provider);
        systemSession.setLocalLlmProvider(provider);
        systemSession.setUseLocalCommandLlm(useLocalCommandLLMCheck.isSelected());
        systemSession.setUseLocalQueryLlm(useLocalQueryLLMCheck.isSelected());
        EventBusManager.publish(new AppLogEvent("Local LLM config saved"));
        EventBusManager.publish(new RestartBrainEvent());
        initData();
    }
}
