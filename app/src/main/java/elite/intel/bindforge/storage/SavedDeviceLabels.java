package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.DeviceEntry;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.install.GameInstallation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * The controllers' names and button labels as the Bind Editor shows them (Storage S4b, Alan, 2026-10-28). Read once, for
 * a screen that shows many devices' inputs at a time.
 * <ul>
 *     <li><strong>Names are the working copy's</strong> - what the installations and {@code .binds} hold now
 *     (bind-editor.md, <em>Which name a controller's tab writes</em>). Until the Bind Editor writes through the edits
 *     (Storage S5) it writes the live {@code .binds}, so a name not applied yet would name a device the installations
 *     do not have. A device is found by its hardware as the game resolves it: the first element claiming the VID/PID.</li>
 *     <li><strong>Labels are the edits'</strong>, so a label SAVEd in the Alias Designer shows at once. A device renamed
 *     in the edits has its labels under its new name, and is found by its hardware.</li>
 * </ul>
 * Names are found ignoring case, as the game finds an element.
 */
public final class SavedDeviceLabels {

    private static final Logger log = LogManager.getLogger(SavedDeviceLabels.class);

    /**
     * No devices and no labels: every input reads as its token.
     */
    public static final SavedDeviceLabels NONE = new SavedDeviceLabels(List.of(), List.of(), Map.of());

    private final List<DeviceEntry> applied;
    private final List<DeviceEntry> edited;
    private final Map<String, Map<String, String>> byDevice;

    private SavedDeviceLabels(List<DeviceEntry> applied, List<DeviceEntry> edited,
                              Map<String, Map<String, String>> byDevice) {
        this.applied = List.copyOf(applied);
        this.edited = List.copyOf(edited);
        this.byDevice = byDevice;
    }

    /**
     * The edits as they are now. When the working copy cannot be read - before first-time startup, or a damaged one -
     * there is nothing to show, and every input reads as its token; the BindForge tab says why.
     */
    public static SavedDeviceLabels load() {
        try {
            Edits.Snapshot snapshot = Edits.stored().snapshot();
            return of(snapshot.current(), snapshot.applied());
        } catch (IOException e) {
            log.warn("Could not read the working copy for the controllers' labels: {}", e.getMessage());
            return NONE;
        }
    }

    /**
     * @param edited  the working copy with the edits on it - the labels
     * @param applied the working copy - the names
     * @throws IOException if a {@code DeviceMappings.xml} or a {@code .buttonMap} cannot be read
     */
    public static SavedDeviceLabels of(ConfigurationFiles edited, ConfigurationFiles applied) throws IOException {
        Map<String, Map<String, String>> byDevice = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String file : edited.buttonMaps().keySet()) {
            String device = file.substring(0, file.length() - GameInstallation.BUTTON_MAP_SUFFIX.length());
            byDevice.putIfAbsent(device, DeviceEdits.labels(edited, device));
        }
        return new SavedDeviceLabels(DeviceEdits.elements(applied), DeviceEdits.elements(edited), byDevice);
    }

    /**
     * Every element's name in the working copy, Frontier's included, in file order.
     */
    public List<String> names() {
        return applied.stream().map(DeviceEntry::name).toList();
    }

    /**
     * The name the working copy gives this hardware, as the game resolves it: the first element claiming it, primary
     * or {@code <Alternative>}.
     */
    public Optional<String> nameOf(String vid, String pid) {
        return firstClaiming(applied, new HardwareId(vid, pid)).map(DeviceEntry::name);
    }

    /**
     * One device's labels by input token, from the edits - empty when it has none; a blank one included, read trimmed.
     *
     * @param deviceName the device's name in the working copy, or in the edits
     */
    public Map<String, String> labelsOf(String deviceName) {
        if (deviceName == null) return Map.of();
        Map<String, String> labels = byDevice.get(deviceName);
        if (labels != null) return labels;
        // WHY: renamed in the edits and not applied - its labels are under the new name, and its hardware is the link.
        return applied.stream()
                .filter(element -> element.name().equalsIgnoreCase(deviceName) && element.primary() != null)
                .findFirst()
                .flatMap(element -> firstClaiming(edited, element.primary()))
                .map(element -> byDevice.getOrDefault(element.name(), Map.of()))
                .orElse(Map.of());
    }

    private static Optional<DeviceEntry> firstClaiming(List<DeviceEntry> elements, HardwareId hardware) {
        return elements.stream().filter(element -> element.hardware().contains(hardware)).findFirst();
    }

    /**
     * The name the user gave this input on this device, if any - never a blank one.
     */
    public Optional<String> label(String deviceName, String inputToken) {
        if (inputToken == null) return Optional.empty();
        String label = labelsOf(deviceName).get(inputToken);
        return label == null || label.isBlank() ? Optional.empty() : Optional.of(label);
    }
}
