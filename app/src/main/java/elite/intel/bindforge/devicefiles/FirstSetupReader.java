package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.install.GameInstallation;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reads one installation's device files for {@link FirstSetupPlan}: its own elements, and the labels worth taking into
 * the working copy. Works on the bytes first-time startup read once, so what is judged is what is written
 * (Storage S2b, 2026-10-25).
 * <p>
 * <strong>Frontier's elements are set aside by name</strong> ({@link FrontierStockDevices#ships}) - every installation
 * runs the same game version, so their shipped entries agree by construction and are never compared. A Frontier
 * element is read only when a {@code .buttonMap} of the user's or BindForge's sits beside it; Frontier's own two
 * maps are recognised and left out ({@link FrontierStockButtonMaps}).
 * <p>
 * <strong>A {@code .buttonMap} with no element behind it is not read.</strong> Nothing resolves through it, so it
 * has nothing to give the working copy.
 */
public final class FirstSetupReader {

    private FirstSetupReader() {
    }

    /**
     * @param deviceMappings the installation's {@code DeviceMappings.xml}, or {@code null} when it has none
     * @param buttonMaps     every {@code .buttonMap} in its {@code DeviceButtonMaps} folder, by file name
     * @throws IOException if its {@code DeviceMappings.xml} or one of its {@code .buttonMap} files cannot be read.
     *                     First setup does not run on a partial reading: an unread file could hold the one entry
     *                     the working copy must not lose. The message names the file
     */
    public static FirstSetupPlan.Installation read(long installId, String storefront, byte[] deviceMappings,
                                                   Map<String, byte[]> buttonMaps, FrontierStockDevices stock,
                                                   FrontierStockButtonMaps stockMaps) throws IOException {
        Map<String, HardwareId> primaries;
        try {
            primaries = deviceMappings == null ? Map.of() : DeviceMappingsParser.primaries(deviceMappings);
        } catch (IOException e) {
            throw new IOException(GameInstallation.DEVICE_MAPPINGS + " could not be read: " + e.getMessage(), e);
        }

        Map<String, HardwareId> elements = new LinkedHashMap<>();
        Map<String, HardwareId> builtIns = new LinkedHashMap<>();
        Map<String, Map<String, String>> labels = new LinkedHashMap<>();
        for (Map.Entry<String, HardwareId> element : primaries.entrySet()) {
            String name = element.getKey();
            boolean frontiers = stock.ships(name);
            if (!frontiers) elements.put(name, element.getValue());

            Map<String, String> found = labelsOf(buttonMaps, name);
            if (found.isEmpty()) continue;
            if (frontiers && stockMaps.isShipped(name, found)) continue;
            labels.put(name, found);
            if (frontiers) builtIns.put(name, element.getValue());
        }
        return new FirstSetupPlan.Installation(installId, storefront, elements, builtIns, labels);
    }

    /**
     * The file name holding a device's labels among {@code buttonMaps}: spelled exactly as the device, or - as Windows
     * finds it, ignoring case - the first other spelling. {@code null} when there is none.
     */
    public static String buttonMapFileOf(Map<String, byte[]> buttonMaps, String deviceName) {
        String exact = deviceName + GameInstallation.BUTTON_MAP_SUFFIX;
        if (buttonMaps.containsKey(exact)) return exact;
        String folded = exact.toLowerCase(Locale.ROOT);
        for (String file : buttonMaps.keySet()) {
            if (file.toLowerCase(Locale.ROOT).equals(folded)) return file;
        }
        return null;
    }

    private static Map<String, String> labelsOf(Map<String, byte[]> buttonMaps, String deviceName) throws IOException {
        String file = buttonMapFileOf(buttonMaps, deviceName);
        if (file == null) return Map.of();
        try {
            return ButtonMapReader.read(buttonMaps.get(file));
        } catch (IOException e) {
            throw new IOException(file + " could not be read: " + e.getMessage(), e);
        }
    }
}
