package elite.intel.ui.view.settings;

import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.*;
import elite.intel.ui.view.AudioDeviceCombo;
import elite.intel.ui.view.HudBanner;
import elite.intel.ui.view.HudComboBox;
import elite.intel.ui.view.HudMicMeter;
import elite.intel.ui.view.HudSection;
import elite.intel.ui.view.HudSlider;
import elite.intel.ui.view.StatusBadge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.view.AppTheme.*;

public class AudioSettingsPanel extends JPanel {

    private final SystemSession systemSession = SystemSession.getInstance();

    private HudSlider voiceVolumeSlider;
    private HudSlider beepVolumeSlider;
    private HudSlider speechSpeedSlider;
    private HudSlider sttThreadsSlider;
    private JCheckBox useLocalTTSCheck;

    private HudComboBox<String> inputCombo;
    private HudComboBox<String> outputCombo;
    /** Guards the combo listeners from persisting while we programmatically re-sync the selection. */
    private boolean syncingDevices;

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

        JPanel columns = transparentPanel(new GridBagLayout());
        columns.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridy = 0;
        cc.weighty = 1.0;
        cc.fill = GridBagConstraints.BOTH;
        cc.anchor = GridBagConstraints.NORTHWEST;

        // Left: wide settings column (devices over levels, help below).
        cc.gridx = 0;
        cc.weightx = 0.72;
        cc.insets = new Insets(0, 0, 0, HUD_GAP);
        columns.add(buildSettingsColumn(), cc);

        // Right: narrow full-height microphone monitor.
        cc.gridx = 1;
        cc.weightx = 0.28;
        cc.insets = new Insets(0, 0, 0, 0);
        columns.add(buildMicColumn(), cc);

        add(columns, BorderLayout.CENTER);

        // Device selection lives in SystemSession and can be changed elsewhere (the AI-tab dialog),
        // so re-sync the pickers whenever this long-lived panel becomes visible again.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                syncDevices();
            }
        });
    }

    /** Left column: AUDIO DEVICES over AUDIO LEVELS, with the help block beneath. All FLAT (§9). */
    private JComponent buildSettingsColumn() {
        JPanel column = transparentPanel(null);
        column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));

        HudSection devices = buildDevicesSection();
        devices.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(devices);

        column.add(Box.createVerticalStrut(HUD_GAP));

        HudSection levels = buildLevelsSection();
        levels.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(levels);

        column.add(Box.createVerticalGlue());
        return column;
    }

    /** Inlined audio device pickers; selection is persisted immediately on change. */
    private HudSection buildDevicesSection() {
        HudSection section = HudSection.flat(getText("audio.devices.section.devices"), new GridBagLayout());
        JPanel form = section.body();

        inputCombo = AudioDeviceCombo.input(systemSession.getAudioInputDevice());
        outputCombo = AudioDeviceCombo.output(systemSession.getAudioOutputDevice());
        inputCombo.addActionListener(e -> {
            if (!syncingDevices) {
                systemSession.setAudioInputDevice(AudioDeviceCombo.normalize((String) inputCombo.getSelectedItem()));
            }
        });
        outputCombo.addActionListener(e -> {
            if (!syncingDevices) {
                systemSession.setAudioOutputDevice(AudioDeviceCombo.normalize((String) outputCombo.getSelectedItem()));
            }
        });

        GridBagConstraints gbc = baseGbc();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel inLabel = hudReadoutLabel(getText("audio.devices.input"));
        inLabel.setPreferredSize(new Dimension(170, HUD_FIELD_HEIGHT));
        form.add(inLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(inputCombo, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel outLabel = hudReadoutLabel(getText("audio.devices.output"));
        outLabel.setPreferredSize(new Dimension(170, HUD_FIELD_HEIGHT));
        form.add(outLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(outputCombo, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(HUD_GAP, gbc.insets.left, gbc.insets.bottom, gbc.insets.right);
        form.add(new HudBanner(getText("audio.devices.note"), StatusBadge.State.STANDBY, true), gbc);

        return section;
    }

    /** AUDIO LEVELS: four HUD sliders plus the local-TTS toggle. */
    private HudSection buildLevelsSection() {
        HudSection section = HudSection.flat(getText("settings.audio.section.levels"), new GridBagLayout());
        JPanel grid = section.body();
        GridBagConstraints ag = baseGbc();

        // Row 0: Speech Volume | Beep Volume
        ag.gridy = 0;
        ag.gridx = 0;
        ag.weightx = 0;
        ag.fill = GridBagConstraints.NONE;
        grid.add(sliderLabel(getText("settings.audio.speechVolume"), 140), ag);

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
        grid.add(sliderLabel(getText("settings.audio.beepVolume"), 120), ag);

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
        grid.add(sliderLabel(getText("settings.audio.ttsVoiceSpeed"), 140), ag);

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
        grid.add(sliderLabel(getText("settings.audio.sttThreads"), 120), ag);

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

        return section;
    }

    /** Right column: MICROPHONE MONITOR holding the full-height segmented level meter. */
    private JComponent buildMicColumn() {
        HudSection section = HudSection.flat(getText("settings.audio.section.microphoneMonitor"), new BorderLayout());
        section.body().add(new HudMicMeter(), BorderLayout.CENTER);
        return section;
    }

    public void initData() {
        useLocalTTSCheck.setSelected(systemSession.useLocalTTS());
        syncDevices();
    }

    /** Re-reads the persisted device selection into the pickers without re-triggering a save. */
    private void syncDevices() {
        syncingDevices = true;
        try {
            selectDevice(inputCombo, systemSession.getAudioInputDevice());
            selectDevice(outputCombo, systemSession.getAudioOutputDevice());
        } finally {
            syncingDevices = false;
        }
    }

    private static void selectDevice(HudComboBox<String> combo, String savedName) {
        combo.setSelectedItem(savedName == null || savedName.isBlank()
                ? AudioDeviceCombo.SYSTEM_DEFAULT_LABEL : savedName);
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
