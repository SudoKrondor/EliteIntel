package elite.intel.ai.ears;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

public class AudioDeviceEnumerator {

    private static final Logger log = LogManager.getLogger(AudioDeviceEnumerator.class);

    /**
     * Retrieves a list of input audio devices available on the system.
     * The method scans the system for audio devices that support
     * {@code TargetDataLine}, which are typically used for capturing audio input.
     *
     * @return a list of {@code Mixer.Info} objects representing the input audio devices
     * that support {@code TargetDataLine}. If no such devices are found, the
     * returned list will be empty.
     */
    public static List<Mixer.Info> getInputDevices() {
        List<Mixer.Info> result = new ArrayList<>();
        Line.Info probe = new Line.Info(TargetDataLine.class);
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            try {
                if (AudioSystem.getMixer(info).isLineSupported(probe)) {
                    result.add(info);
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    /**
     * Retrieves a list of output audio devices available on the system.
     * The method scans the system for audio devices that support {@code SourceDataLine},
     * which are typically used for playback of audio output.
     *
     * @return a list of {@code Mixer.Info} objects representing the output audio devices
     * that support {@code SourceDataLine}. If no such devices are found, the
     * returned list will be empty.
     */
    public static List<Mixer.Info> getOutputDevices() {
        List<Mixer.Info> result = new ArrayList<>();
        Line.Info probe = new Line.Info(SourceDataLine.class);
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            try {
                if (AudioSystem.getMixer(info).isLineSupported(probe)) {
                    result.add(info);
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    /**
     * Resolves an input audio device by name from the list of available input devices.
     * Searches the available input devices for a match with the specified name.
     * If a device with the given name is found, it returns the corresponding {@code Mixer.Info}.
     * Otherwise, it logs a warning if the device is not found and falls back to returning {@code null}.
     *
     * @param name the name of the input audio device to resolve; may not be {@code null} or blank.
     * @return the {@code Mixer.Info} representing the resolved input device if found;
     * otherwise returns {@code null}.
     */
    public static Mixer.Info resolveInputDevice(String name) {
        if (name == null || name.isBlank()) return null;
        for (Mixer.Info info : getInputDevices()) {
            if (info.getName().equals(name)) return info;
        }
        log.warn("Saved input device '{}' not found; falling back to system default", name);
        return null;
    }

    /**
     * Resolves an output audio device by its name from the list of available output devices.
     * Searches the available output devices for a match with the specified name. If a device with the
     * given name is found, it returns the corresponding {@code Mixer.Info}. If no such device is found,
     * it logs a warning and falls back to returning {@code null}.
     *
     * @param name the name of the output audio device to resolve; may not be {@code null} or blank.
     * @return the {@code Mixer.Info} representing the resolved output device if found;
     * otherwise returns {@code null}.
     */
    public static Mixer.Info resolveOutputDevice(String name) {
        if (name == null || name.isBlank()) return null;
        for (Mixer.Info info : getOutputDevices()) {
            if (info.getName().equals(name)) return info;
        }
        log.warn("Saved output device '{}' not found; falling back to system default", name);
        return null;
    }

    /**
     * Opens an input audio line for capturing audio data. The method attempts to retrieve a
     * {@code TargetDataLine} using the provided {@code DataLine.Info} and optionally a
     * specific {@code Mixer.Info}. If the specified mixer is unavailable or results in an
     * exception, the system default mixer is used.
     *
     * @param info      the {@code DataLine.Info} describing the desired audio line; must not be null.
     * @param mixerInfo the {@code Mixer.Info} of the specific mixer to use, or null to use the system default mixer.
     * @return the {@code TargetDataLine} for capturing audio data.
     * @throws LineUnavailableException if no suitable line is available or cannot be opened.
     */
    public static TargetDataLine openInputLine(DataLine.Info info, Mixer.Info mixerInfo) throws LineUnavailableException {
        if (mixerInfo == null) {
            return (TargetDataLine) AudioSystem.getLine(info);
        }
        try {
            return (TargetDataLine) AudioSystem.getMixer(mixerInfo).getLine(info);
        } catch (Exception e) {
            // Do NOT silently fall back to the system default device. Switching mics behind
            // the user's back calibrates/captures against a device they did not select and,
            // when that default cannot honour the requested format, surfaces as a misleading
            // "format not supported" error against the wrong line. Fail on the chosen device
            // so callers can retry it (transient contention) or report it truthfully.
            log.warn("Cannot open input on '{}': {}", mixerInfo.getName(), e.getMessage());
            if (e instanceof LineUnavailableException lue) {
                throw lue;
            }
            LineUnavailableException ex = new LineUnavailableException(
                    "Cannot open input on '" + mixerInfo.getName() + "': " + e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    /**
     * Opens an output audio line for playback of audio data.
     * The method attempts to retrieve a {@code SourceDataLine} using the provided {@code DataLine.Info}
     * and optionally a specific {@code Mixer.Info}. If the specified mixer is unavailable or results
     * in an exception, the system default mixer is used as a fallback.
     *
     * @param info      the {@code DataLine.Info} describing the desired audio line; must not be null.
     * @param mixerInfo the {@code Mixer.Info} of the specific mixer to use, or null to use the system default mixer.
     * @return the {@code SourceDataLine} for playback of audio data.
     * @throws LineUnavailableException if no suitable line is available or cannot be opened.
     */
    public static SourceDataLine openOutputLine(DataLine.Info info, Mixer.Info mixerInfo) throws LineUnavailableException {
        if (mixerInfo == null) {
            return (SourceDataLine) AudioSystem.getLine(info);
        }
        try {
            return (SourceDataLine) AudioSystem.getMixer(mixerInfo).getLine(info);
        } catch (Exception e) {
            log.warn("Cannot open output on '{}', using system default: {}", mixerInfo.getName(), e.getMessage());
            return (SourceDataLine) AudioSystem.getLine(info);
        }
    }
}
