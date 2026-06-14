package elite.intel.ui.view;

import elite.intel.ai.ears.AudioDeviceEnumerator;

import javax.sound.sampled.Mixer;
import javax.swing.DefaultComboBoxModel;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Shared factory for the audio input/output device pickers used by both the inline AUDIO DEVICES
 * settings panel and the AI-tab {@link AudioInterfaceDialog}. Centralises the "(System Default)"
 * sentinel, the combo model build (sentinel first, then enumerated devices), and the
 * sentinel→{@code null} normalization applied before persisting a selection.
 */
public final class AudioDeviceCombo {

    /** Display label for "no explicit device / use the OS default"; persisted as {@code null}. */
    public static final String SYSTEM_DEFAULT_LABEL = getText("audio.devices.systemDefault");

    private AudioDeviceCombo() {
    }

    /** Builds the microphone (input) picker, preselecting {@code savedName} when present. */
    public static HudComboBox<String> input(String savedName) {
        return build(AudioDeviceEnumerator.getInputDevices(), savedName);
    }

    /** Builds the speaker (output) picker, preselecting {@code savedName} when present. */
    public static HudComboBox<String> output(String savedName) {
        return build(AudioDeviceEnumerator.getOutputDevices(), savedName);
    }

    /** Maps the "(System Default)" sentinel to {@code null} for persistence. */
    public static String normalize(String selected) {
        return SYSTEM_DEFAULT_LABEL.equals(selected) ? null : selected;
    }

    private static HudComboBox<String> build(List<Mixer.Info> devices, String savedName) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement(SYSTEM_DEFAULT_LABEL);
        for (Mixer.Info info : devices) {
            model.addElement(info.getName());
        }
        HudComboBox<String> combo = new HudComboBox<>(model);
        if (savedName != null && !savedName.isBlank()) {
            combo.setSelectedItem(savedName);
        }
        return combo;
    }
}
