package elite.intel.bindforge.devicefiles;

import elite.intel.db.dao.BindForgeDeviceDraftDao.DraftRow;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.db.managers.BindForgeDeviceDraftManager;
import elite.intel.db.managers.BindForgeDeviceMasterManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The button names the user gave a controller in the Device Editor, as saved: <strong>the draft's when it holds
 * the device, else the master's</strong>. So a label shows as soon as SAVE is pressed, the way the Device Editor
 * itself shows it, and the game catches up at APPLY like every other draft edit.
 * <p>
 * A device is found by its name and, while a rename waits for APPLY, by the name it had - the {@code .binds}
 * file still says the old one until then.
 */
public final class SavedDeviceLabels {

    /**
     * No labels at all: every input reads as its token.
     */
    public static final SavedDeviceLabels NONE = new SavedDeviceLabels(Map.of());

    private final Map<String, Map<String, String>> byDevice;

    private SavedDeviceLabels(Map<String, Map<String, String>> byDevice) {
        this.byDevice = byDevice;
    }

    /**
     * Every saved device's labels, read once - for a screen that shows many devices' inputs at a time.
     */
    public static SavedDeviceLabels load() {
        Map<String, Map<String, String>> byDevice = new HashMap<>();
        BindForgeDeviceMasterManager master = BindForgeDeviceMasterManager.getInstance();
        Layer masterLayer = new Layer();
        for (DeviceRow row : master.findAll()) {
            masterLayer.add(row.deviceName(), row.previousName(), master.labelsOf(row.id()));
        }
        masterLayer.into(byDevice);
        BindForgeDeviceDraftManager draft = BindForgeDeviceDraftManager.getInstance();
        if (draft.isStarted()) {
            // WHY: the draft overrides the master under every name it answers to. A device renamed in the draft
            // is still under its old name in the master and in the .binds file, and its saved names are the
            // draft's.
            Layer draftLayer = new Layer();
            for (DraftRow row : draft.findAll()) {
                draftLayer.add(row.deviceName(), row.previousName(), draft.labelsOf(row.id()));
            }
            draftLayer.into(byDevice);
        }
        return new SavedDeviceLabels(Map.copyOf(byDevice));
    }

    /**
     * One store's devices by name, where a current name always wins over an old one.
     */
    private static final class Layer {
        private final Map<String, Map<String, String>> byName = new HashMap<>();
        private final Map<String, Map<String, String>> byPreviousName = new HashMap<>();

        void add(String deviceName, String previousName, Map<String, String> labels) {
            byName.put(deviceName, labels);
            if (previousName != null) byPreviousName.put(previousName, labels);
        }

        void into(Map<String, Map<String, String>> byDevice) {
            // WHY: two renames can leave one device's old name equal to another's new one; the new one is real.
            byPreviousName.forEach(byName::putIfAbsent);
            byDevice.putAll(byName);
        }
    }

    /**
     * One device's saved labels by input token, or empty when it has none - the Device Editor's starting set.
     */
    public static Map<String, String> of(String deviceName) {
        BindForgeDeviceDraftManager draft = BindForgeDeviceDraftManager.getInstance();
        if (draft.isStarted()) {
            DraftRow row = draft.findByName(deviceName);
            if (row != null) return draft.labelsOf(row.id());
        }
        BindForgeDeviceMasterManager master = BindForgeDeviceMasterManager.getInstance();
        DeviceRow row = master.findByName(deviceName);
        return row == null ? Map.of() : master.labelsOf(row.id());
    }

    /**
     * The name the user gave this input on this device, if any - never a blank one.
     */
    public Optional<String> label(String deviceName, String inputToken) {
        if (deviceName == null || inputToken == null) return Optional.empty();
        String label = byDevice.getOrDefault(deviceName, Map.of()).get(inputToken);
        return label == null || label.isBlank() ? Optional.empty() : Optional.of(label);
    }
}
