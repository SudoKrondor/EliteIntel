package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.ButtonMapReader;
import elite.intel.bindforge.devicefiles.DeviceEntry;
import elite.intel.bindforge.devicefiles.DeviceMappingsParser;
import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.install.GameInstallation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Whether an installation's device files hold what the working copy holds, <strong>by value</strong> (storage.md,
 * <em>How the files are written</em>; Alan, 2026-10-26): the device elements in file order - names and every VID/PID,
 * since the first element claiming a VID/PID is the one the game uses - and each {@code .buttonMap}'s labels. Bytes,
 * line endings and dates do not count ({@code overview.md}, <em>Freshness Checks</em>). APPLY leaves an installation
 * that differs alone, for the check to reconcile (Storage S9 reuses this). Pure.
 * <p>
 * <strong>Not a difference:</strong> an installation with no {@code DeviceMappings.xml} - nothing of the user's is in it,
 * and APPLY simply writes one (alias-designer.md, <em>What APPLY does</em>); a working copy with none, which the write
 * leaves every installation's alone for; Frontier's own {@code VPCPanel} and {@code VPCThrottle} maps the working copy
 * lacks, which no write removes; and <strong>an element's children</strong>, such as {@code <SupportsIcons>} - they are
 * Frontier's, and the user does not edit them (Alan, 2026-10-27).
 * <p>
 * The working copy's side is parsed once, by {@link #of}, so a working copy that cannot be read stops APPLY before
 * anything is written rather than leaving every installation alone as "differs" (Storage S4a review, 2026-10-27).
 */
public final class DeviceFilesComparison {

    /** The working copy's elements, or {@code null} when it holds no {@code DeviceMappings.xml}. */
    private final List<DeviceEntry> elements;
    private final Map<String, Map<String, String>> labels;

    private DeviceFilesComparison(List<DeviceEntry> elements, Map<String, Map<String, String>> labels) {
        this.elements = elements;
        this.labels = labels;
    }

    /**
     * Reads the working copy's side.
     *
     * @param workingCopy the working copy - what the last write left, before the edits on screen
     * @throws IOException if its own {@code DeviceMappings.xml} or a {@code .buttonMap} cannot be parsed
     */
    public static DeviceFilesComparison of(ConfigurationFiles workingCopy) throws IOException {
        return new DeviceFilesComparison(
                workingCopy.hasDeviceMappings() ? elements(workingCopy.deviceMappings()) : null,
                labelsByDevice(workingCopy.buttonMaps()));
    }

    /**
     * @param installation the installation's device files as they are now
     * @return whether they differ from the working copy
     * @throws IOException if one of the installation's files cannot be parsed - what it holds is unknown, so the caller
     *                     treats it as differing
     */
    public boolean differs(DeviceFiles installation) throws IOException {
        return !sameDeviceMappings(installation) || !sameButtonMaps(installation);
    }

    private boolean sameDeviceMappings(DeviceFiles installation) throws IOException {
        if (elements == null || !installation.hasDeviceMappings()) return true;
        return elements.equals(elements(installation.deviceMappings()));
    }

    private static List<DeviceEntry> elements(byte[] deviceMappings) throws IOException {
        return DeviceMappingsParser.parse(new ByteArrayInputStream(deviceMappings), GameInstallation.DEVICE_MAPPINGS);
    }

    private boolean sameButtonMaps(DeviceFiles installation) throws IOException {
        Map<String, Map<String, String>> theirs = labelsByDevice(installation.buttonMaps());
        // WHY: a shipped map the working copy lacks is never removed by a write, so it is no difference - but one the
        // working copy holds must match.
        for (String device : List.copyOf(theirs.keySet())) {
            if (!labels.containsKey(device) && FrontierStockButtonMaps.shipsMapFor(device)) theirs.remove(device);
        }
        return labels.equals(theirs);
    }

    /** Labels by device name, the name folded to lower case - the game finds a map ignoring case. */
    private static Map<String, Map<String, String>> labelsByDevice(Map<String, byte[]> maps)
            throws IOException {
        Map<String, Map<String, String>> labels = new TreeMap<>();
        for (Map.Entry<String, byte[]> map : maps.entrySet()) {
            String name = map.getKey();
            String device = name.substring(0, name.length() - GameInstallation.BUTTON_MAP_SUFFIX.length());
            labels.put(device.toLowerCase(Locale.ROOT), ButtonMapReader.read(map.getValue()));
        }
        return labels;
    }
}
