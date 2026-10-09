package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.MasterDevice;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.bindforge.install.GameInstallation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The re-merge, read: one device's labels from the master and from each drifted installation, as the sides of a
 * {@link LabelMerge}. <em>"The same view serves a later re-merge, when an install has drifted and its labels no
 * longer match the master"</em> (alias-designer.md, settled 2026-09-23); built 2026-10-08 (Alan).
 * <p>
 * The master is the side {@link #MASTER}; an installation is the side {@link DeviceDivergence#installKey} names, so
 * the screen's own names for installations name the sides too. As in first setup, a label only one side holds is
 * kept - a label the master holds and an installation's file lacks stays in the master.
 */
public final class LabelReMerge {

    /** The master's side. Never an installation key, which is a number. */
    public static final String MASTER = "master";

    private LabelReMerge() {
    }

    /**
     * Reads the stored master's labels for the device and each named stored installation's {@code .buttonMap}.
     *
     * @throws IOException if a {@code .buttonMap} cannot be read - merging without it could keep the master's
     *                     label over one the user cannot see
     */
    public static LabelMerge stored(String deviceName, List<Long> installIds) throws IOException {
        Map<String, String> master = DeviceFilesPush.storedMaster().stream()
                .filter(device -> device.name().equals(deviceName))
                .map(MasterDevice::labels)
                .findFirst()
                .orElse(Map.of());
        List<Target> targets = DeviceFilesPush.storedInstallations().stream()
                .filter(target -> installIds.contains(target.installId()))
                .toList();
        return read(deviceName, master, targets);
    }

    /**
     * @param master  the master's labels for the device, input to label
     * @param targets the installations whose labels are the other sides, in the order they are shown. One whose
     *                folder or {@code .buttonMap} is not there gives no side
     */
    static LabelMerge read(String deviceName, Map<String, String> master, List<Target> targets) throws IOException {
        Map<String, Map<String, String>> sides = new LinkedHashMap<>();
        sides.put(MASTER, master);
        for (Target target : targets) {
            if (!target.isReachable()) continue;
            Path buttonMap = GameInstallation.buttonMapIn(target.controlSchemes(), deviceName);
            if (!Files.isRegularFile(buttonMap)) continue;
            try {
                sides.put(DeviceDivergence.installKey(target.installId()), ButtonMapReader.read(Files.readAllBytes(buttonMap)));
            } catch (IOException e) {
                throw new IOException(target.storefront() + ": " + buttonMap.getFileName() + " could not be read - "
                        + e.getMessage(), e);
            }
        }
        return new LabelMerge(sides);
    }
}
