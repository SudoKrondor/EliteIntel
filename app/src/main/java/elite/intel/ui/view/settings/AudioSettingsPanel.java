package elite.intel.ui.view.settings;

import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.*;
import elite.intel.ui.view.AudioInterfaceDialog;
import elite.intel.ui.view.HudSection;
import elite.intel.ui.view.HudSlider;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.view.AppTheme.*;

public class AudioSettingsPanel extends JPanel {

    private final SystemSession systemSession = SystemSession.getInstance();

    private HudSlider voiceVolumeSlider;
    private HudSlider beepVolumeSlider;
    private HudSlider speechSpeedSlider;
    private HudSlider sttThreadsSlider;
    private JCheckBox useLocalTTSCheck;

    private Runnable onLocalTtsChanged;

    public void setOnLocalTtsChanged(Runnable r) {
        onLocalTtsChanged = r;
    }

    public AudioSettingsPanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_BG);

        HudSection controlsSection = new HudSection(getText("settings.audio.section.levels"), new GridBagLayout());
        JPanel grid = controlsSection.body();
        GridBagConstraints ag = baseGbc();

        // Row 0: Speech Volume | Beep Volume
        ag.gridy = 0;
        ag.gridx = 0;
        ag.weightx = 0;
        ag.fill = GridBagConstraints.NONE;
        JLabel lblSpeechVol = sliderLabel(getText("settings.audio.speechVolume"), 140);
        grid.add(lblSpeechVol, ag);

        voiceVolumeSlider = makeSlider(0, 100, systemSession.getVoiceVolume());
        voiceVolumeSlider.addChangeListener(e -> EventBusManager.publish(new SttVolumeChangedEvent(voiceVolumeSlider.getValue())));
        ag.gridx = 1;
        ag.weightx = 1.0;
        ag.fill = GridBagConstraints.HORIZONTAL;
        grid.add(voiceVolumeSlider, ag);

        ag.gridx = 2;
        ag.weightx = 0;
        ag.fill = GridBagConstraints.NONE;
        ag.insets = new Insets(6, 24, 6, 6);
        JLabel lblBeepVol = sliderLabel(getText("settings.audio.beepVolume"), 120);
        grid.add(lblBeepVol, ag);

        beepVolumeSlider = makeSlider(0, 100, (int) (systemSession.getBeepVolume() * 100));
        beepVolumeSlider.addChangeListener(e -> EventBusManager.publish(new NotificationVolumeChangedEvent(beepVolumeSlider.getValue() / 100f)));
        ag.gridx = 3;
        ag.weightx = 1.0;
        ag.fill = GridBagConstraints.HORIZONTAL;
        ag.insets = new Insets(6, 6, 6, 6);
        grid.add(beepVolumeSlider, ag);

        // Row 1: TTS Voice Speed | STT Threads
        ag.gridy = 1;
        ag.gridx = 0;
        ag.weightx = 0;
        ag.fill = GridBagConstraints.NONE;
        JLabel lblTtsSpeed = sliderLabel(getText("settings.audio.ttsVoiceSpeed"), 140);
        grid.add(lblTtsSpeed, ag);

        speechSpeedSlider = makeSlider(0, 100, (int) (systemSession.getSpeechSpeed() * 100));
        speechSpeedSlider.addChangeListener(e -> EventBusManager.publish(new SpeechSpeedChangeEvent(speechSpeedSlider.getValue() / 100f)));
        ag.gridx = 1;
        ag.weightx = 1.0;
        ag.fill = GridBagConstraints.HORIZONTAL;
        grid.add(speechSpeedSlider, ag);

        ag.gridx = 2;
        ag.weightx = 0;
        ag.fill = GridBagConstraints.NONE;
        ag.insets = new Insets(6, 24, 6, 6);
        JLabel lblSttThreads = sliderLabel(getText("settings.audio.sttThreads"), 120);
        grid.add(lblSttThreads, ag);

        sttThreadsSlider = makeSlider(4, 11, systemSession.getSttThreads());
        sttThreadsSlider.addChangeListener(e -> EventBusManager.publish(new SttThreadsChangedEvent(sttThreadsSlider.getValue())));
        ag.gridx = 3;
        ag.weightx = 1.0;
        ag.fill = GridBagConstraints.HORIZONTAL;
        ag.insets = new Insets(6, 6, 6, 6);
        grid.add(sttThreadsSlider, ag);

        // Row 2: Use Local TTS
        ag.gridy = 2;
        ag.gridx = 0;
        ag.gridwidth = 4;
        ag.weightx = 0;
        ag.fill = GridBagConstraints.NONE;
        useLocalTTSCheck = makeCheckBox(getText("settings.audio.useLocalTts"), false);
        useLocalTTSCheck.addActionListener(a -> saveLocalTts());
        grid.add(useLocalTTSCheck, ag);

        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel content = transparentPanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(controlsSection);

        content.add(Box.createVerticalStrut(8));
        HudSection monitorSection = new HudSection(getText("settings.audio.section.microphoneMonitor"), new BorderLayout(0, HUD_GAP));
        JPanel monitorBody = monitorSection.body();
        AudioWaveformPanel waveformPanel = new AudioWaveformPanel();
        waveformPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        waveformPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, AudioWaveformPanel.PANEL_HEIGHT));
        monitorBody.add(waveformPanel, BorderLayout.NORTH);

        JButton audioDevicesBtn = makeButton(getText("button.audioDevices"));
        audioDevicesBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        audioDevicesBtn.addActionListener(e -> new AudioInterfaceDialog(this).setVisible(true));
        JPanel deviceRow = transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        deviceRow.add(audioDevicesBtn);
        monitorBody.add(deviceRow, BorderLayout.CENTER);

        JPanel helpLabels = transparentPanel(null);
        helpLabels.setLayout(new BoxLayout(helpLabels, BoxLayout.PAGE_AXIS));
        helpLabels.setAlignmentX(Component.LEFT_ALIGNMENT);
        helpLabels.add(Box.createVerticalStrut(18));
        JLabel graphLabel = new JLabel(getText("settings.audio.micHelp"));
        graphLabel.setForeground(FG_MUTED);
        helpLabels.add(graphLabel);
        monitorBody.add(helpLabels, BorderLayout.SOUTH);
        content.add(monitorSection);

        add(content, BorderLayout.NORTH);
    }

    public void initData() {
        useLocalTTSCheck.setSelected(systemSession.useLocalTTS());
    }

    /**
     * Called by CloudServicesSettingsPanel when the user activates cloud TTS.
     * Delegates to saveLocalTts() so the confirmation dialog and voice-reset
     * logic fire identically to the user clicking the checkbox directly.
     */
    public void activateCloudTts() {
        useLocalTTSCheck.setSelected(false);
        saveLocalTts();
    }

    private void saveLocalTts() {
        boolean newValue = useLocalTTSCheck.isSelected();
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
                useLocalTTSCheck.setSelected(oldValue);
                return;
            }
            ShipManager.getInstance().resetAllVoicesToDefault(defaultVoice);
        }
        systemSession.setUseLocalTTS(newValue);
        EventBusManager.publish(new TTSProviderChangedEvent());
        EventBusManager.publish(new RestartMouthEvent());
        if (newValue != oldValue) EventBusManager.publish(new RestartMouthEvent());
        if (onLocalTtsChanged != null) onLocalTtsChanged.run();
    }

    /** Creates a HUD slider snapping to integer steps; the value is shown above the thumb. */
    private static HudSlider makeSlider(int min, int max, int value) {
        return new HudSlider(min, max, 1, value);
    }

    /**
     * Builds a slider-row key label aligned to the slider track. The top inset of
     * {@link AppTheme#HUD_SLIDER_VALUE_AREA} drops the text from the row top to the track level,
     * matching the slider's internal layout (value above, track below).
     */
    private static JLabel sliderLabel(String text, int width) {
        JLabel lbl = hudReadoutLabel(text);
        lbl.setPreferredSize(new Dimension(width, HUD_SLIDER_HEIGHT));
        lbl.setBorder(BorderFactory.createEmptyBorder(HUD_SLIDER_VALUE_AREA, 0, 0, 0));
        return lbl;
    }
}
