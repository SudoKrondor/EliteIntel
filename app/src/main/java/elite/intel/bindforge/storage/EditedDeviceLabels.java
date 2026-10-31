package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.DeviceEntry;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.install.GameInstallation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * The controllers' names and button labels as the Bind Editor shows them, both from the edits not applied - so a device
 * renamed or a label SAVEd in the Alias Designer shows at once, and the capture tab writes the name the edits' binds
 * files use (bind-editor.md, <em>Which name a controller's tab writes</em>; Storage S5, 2026-10-28). Read once, for a
 * screen that shows many devices' inputs at a time.
 * <p>
 * <em>Until Storage S5 the names were the working copy's</em>, since the Bind Editor wrote the live {@code .binds},
 * where a name not applied would have named a device no installation had. It now writes the edits, and a rename
 * moves every binding in them (Storage S4a), so the edits' name is the one their binds files hold.
 * <p>
 * A device is found by its hardware as the game resolves it: the first element claiming the VID/PID. Names are found
 * ignoring case, as the game finds an element.
 */
public final class EditedDeviceLabels {

    private static final Logger log = LogManager.getLogger(EditedDeviceLabels.class);

    /**
     * No devices and no labels: every input reads as its token.
     */
    public static final EditedDeviceLabels NONE = new EditedDeviceLabels(List.of(), Map.of());

    private final List<DeviceEntry> elements;
    private final Map<String, Map<String, String>> byDevice;

    private EditedDeviceLabels(List<DeviceEntry> elements, Map<String, Map<String, String>> byDevice) {
        this.elements = List.copyOf(elements);
        this.byDevice = byDevice;
    }

    /**
     * The edits as they are now - {@link #NONE} when they cannot be read. Touches the disk when there are no edits: not
     * for the EDT.
     */
    public static EditedDeviceLabels load() {
        try {
            return of(Edits.stored().snapshot().current());
        } catch (IOException e) {
            // WHY: before first-time startup, or with a damaged working copy, there is nothing to show - every input
            // reads as its token rather than the screen failing, and the BindForge tab says why.
            log.warn("Could not read the working copy for the controllers' labels: {}", e.getMessage());
            return NONE;
        }
    }

    /**
     * @param edited the working copy with the edits on it
     * @throws IOException if its {@code DeviceMappings.xml} or a {@code .buttonMap} cannot be read
     */
    public static EditedDeviceLabels of(ConfigurationFiles edited) throws IOException {
        Map<String, Map<String, String>> byDevice = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String file : edited.buttonMaps().keySet()) {
            String device = file.substring(0, file.length() - GameInstallation.BUTTON_MAP_SUFFIX.length());
            byDevice.putIfAbsent(device, DeviceEdits.labels(edited, device));
        }
        return new EditedDeviceLabels(DeviceEdits.elements(edited), byDevice);
    }

    /**
     * Every element's name in the edits, Frontier's included, in file order.
     */
    public List<String> names() {
        return elements.stream().map(DeviceEntry::name).toList();
    }

    /**
     * The name the edits give this hardware, as the game resolves it: the first element claiming it, primary or
     * {@code <Alternative>}.
     */
    public Optional<String> nameOf(String vid, String pid) {
        HardwareId hardware = new HardwareId(vid, pid);
        return elements.stream().filter(element -> element.hardware().contains(hardware)).findFirst()
                .map(DeviceEntry::name);
    }

    /**
     * One device's labels by input token - empty when it has none; a blank one included, read trimmed.
     */
    public Map<String, String> labelsOf(String deviceName) {
        if (deviceName == null) return Map.of();
        return byDevice.getOrDefault(deviceName, Map.of());
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
