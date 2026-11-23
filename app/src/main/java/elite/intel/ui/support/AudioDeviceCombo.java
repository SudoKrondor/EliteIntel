package elite.intel.ui.support;

import elite.intel.ui.dialog.AudioInterfaceDialog;
import elite.intel.ui.widget.HudComboBox;

import elite.intel.ai.ears.AudioDeviceEnumerator;

import javax.sound.sampled.Mixer;
import javax.swing.DefaultComboBoxModel;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Shared factory for the audio input/output device pickers used by both the inline AUDIO DEVICES
 * settings panel and the AI-tab {@link AudioInterfaceDialog}. Centralises the "(System Default)"
 * sentinel, the combo model build (sentinel first, then enumerated devices), and the
 * sentinel->{@code null} normalization applied before persisting a selection.
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

    /**
     * Re-enumerates the microphones into an existing picker and selects {@code savedName}. A long-lived
     * picker must call this rather than only re-selecting: a device that appeared after the picker was
     * built (a VR headset powered on after app start) is not in its list, and a non-editable combo
     * silently ignores a selection of an item it does not hold - it would keep showing "(System Default)"
     * while the saved setting names the device.
     */
    public static void refreshInput(HudComboBox<String> combo, String savedName) {
        refresh(combo, AudioDeviceEnumerator.getInputDevices(), savedName);
    }

    /** Re-enumerates the speakers into an existing picker; see {@link #refreshInput}. */
    public static void refreshOutput(HudComboBox<String> combo, String savedName) {
        refresh(combo, AudioDeviceEnumerator.getOutputDevices(), savedName);
    }

    /** Maps the "(System Default)" sentinel to {@code null} for persistence. */
    public static String normalize(String selected) {
        return SYSTEM_DEFAULT_LABEL.equals(selected) ? null : selected;
    }

    private static HudComboBox<String> build(List<Mixer.Info> devices, String savedName) {
        return new HudComboBox<>(model(devices, savedName));
    }

    private static void refresh(HudComboBox<String> combo, List<Mixer.Info> devices, String savedName) {
        combo.setModel(model(devices, savedName));
    }

    /**
     * Sentinel first, then the enumerated devices, with the saved one selected. A saved device that is
     * not connected right now is still listed, so the picker shows what is saved rather than pretending
     * the setting is "(System Default)"; the engines fall back to the default on their own while it is
     * absent and pick it up again on their next start.
     */
    private static DefaultComboBoxModel<String> model(List<Mixer.Info> devices, String savedName) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement(SYSTEM_DEFAULT_LABEL);
        for (Mixer.Info info : devices) {
            model.addElement(info.getName());
        }
        boolean hasSaved = savedName != null && !savedName.isBlank();
        if (hasSaved && model.getIndexOf(savedName) < 0) {
            model.addElement(savedName);
        }
        model.setSelectedItem(hasSaved ? savedName : SYSTEM_DEFAULT_LABEL);
        return model;
    }
}
