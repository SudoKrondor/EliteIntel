package elite.intel.ui.screen.settings;

import elite.intel.ui.support.AudioDeviceCombo;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudMicMeter;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudSlider;
import elite.intel.ui.widget.StatusBadge;

import elite.intel.gameapi.EventBusManager;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudPalette.*;
import static elite.intel.ui.theme.HudForms.*;

public class AudioSettingsPanel extends JPanel {

    private final SystemSession systemSession = SystemSession.getInstance();

    /** Shared left label-column width so device and level controls start at the same x (fits the longest label). */
    private static final int LABEL_COL_WIDTH = 170;

    private HudSlider voiceVolumeSlider;
    private HudSlider beepVolumeSlider;
    private HudSlider speechSpeedSlider;
    private HudSlider sttThreadsSlider;

    private HudComboBox<String> inputCombo;
    private HudComboBox<String> outputCombo;
    /** Guards the combo listeners from persisting while we programmatically re-sync the selection. */
    private boolean syncingDevices;

    public AudioSettingsPanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        JPanel columns = transparentPanel(new GridBagLayout());
        columns.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridy = 0;
        cc.weighty = 1.0;
        cc.fill = GridBagConstraints.BOTH;
        cc.anchor = GridBagConstraints.NORTHWEST;

        // Left: settings column takes all the slack.
        cc.gridx = 0;
        cc.weightx = 1.0;
        cc.insets = new Insets(0, 0, 0, HUD_GAP);
        columns.add(buildSettingsColumn(), cc);

        // Right: microphone monitor stays at its natural (narrow) width - no horizontal stretch.
        cc.gridx = 1;
        cc.weightx = 0;
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

    /** Left column: AUDIO DEVICES over AUDIO LEVELS, with the help block beneath. All FLAT (section 9). */
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
        sizeFieldLabel(inLabel, LABEL_COL_WIDTH);
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
        sizeFieldLabel(outLabel, LABEL_COL_WIDTH);
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

    /** AUDIO LEVELS: four full-width HUD sliders stacked in one column, plus the local-TTS toggle. */
    private HudSection buildLevelsSection() {
        HudSection section = HudSection.flat(getText("settings.audio.section.levels"), new GridBagLayout());
        JPanel grid = section.body();
        GridBagConstraints ag = baseGbc();

        voiceVolumeSlider = makeSlider(0, 100, systemSession.getVoiceVolume());
        voiceVolumeSlider.addChangeListener(e -> EventBusManager.publish(new SttVolumeChangedEvent(voiceVolumeSlider.getValue())));
        addLevelRow(grid, ag, 0, getText("settings.audio.speechVolume"), voiceVolumeSlider);

        speechSpeedSlider = makeSlider(0, 100, (int) (systemSession.getSpeechSpeed() * 100));
        speechSpeedSlider.addChangeListener(e -> EventBusManager.publish(new SpeechSpeedChangeEvent(speechSpeedSlider.getValue() / 100f)));
        addLevelRow(grid, ag, 1, getText("settings.audio.ttsVoiceSpeed"), speechSpeedSlider);

        beepVolumeSlider = makeSlider(0, 100, (int) (systemSession.getBeepVolume() * 100));
        beepVolumeSlider.addChangeListener(e -> EventBusManager.publish(new NotificationVolumeChangedEvent(beepVolumeSlider.getValue() / 100f)));
        addLevelRow(grid, ag, 2, getText("settings.audio.beepVolume"), beepVolumeSlider);

        sttThreadsSlider = makeSlider(4, 11, systemSession.getSttThreads());
        sttThreadsSlider.addChangeListener(e -> EventBusManager.publish(new SttThreadsChangedEvent(sttThreadsSlider.getValue())));
        addLevelRow(grid, ag, 3, getText("settings.audio.sttThreads"), sttThreadsSlider);

        return section;
    }

    /** Right column: MICROPHONE MONITOR - a FRAMED accent card holding the full-height level meter. */
    private JComponent buildMicColumn() {
        HudSection section = new HudSection(getText("settings.audio.section.microphoneMonitor"), new BorderLayout());
        section.body().add(new HudMicMeter(), BorderLayout.CENTER);
        return section;
    }

    public void initData() {
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

    /** Adds one full-width level row: label in the shared column + slider stretched to panel width. */
    private static void addLevelRow(JPanel grid, GridBagConstraints ag, int row, String label, HudSlider slider) {
        ag.gridy = row;
        ag.gridx = 0;
        ag.gridwidth = 1;
        ag.weightx = 0;
        ag.fill = GridBagConstraints.NONE;
        ag.insets = new Insets(6, 6, 6, 6);
        grid.add(sliderLabel(label, LABEL_COL_WIDTH), ag);
        ag.gridx = 1;
        ag.weightx = 1.0;
        ag.fill = GridBagConstraints.HORIZONTAL;
        grid.add(slider, ag);
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
