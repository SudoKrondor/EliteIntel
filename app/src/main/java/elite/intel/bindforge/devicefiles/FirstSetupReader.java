package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.bindforge.install.GameInstallation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads one installation for {@link FirstSetupPlan}: its own elements, and the labels worth taking into a master.
 * <p>
 * <strong>Frontier's elements are set aside by name</strong>, as {@link ProvenanceRule} does - every installation
 * runs the same game version, so their shipped entries agree by construction and are never compared. A Frontier
 * element is read only when a {@code .buttonMap} of the user's or BindForge's sits beside it; Frontier's own two
 * maps are recognised and left out ({@link FrontierStockButtonMaps}).
 * <p>
 * <strong>A {@code .buttonMap} with no element behind it is not read.</strong> Nothing resolves through it, so it
 * has nothing to give the master; it stays on disk, reported by the divergence list as it is today.
 */
final class FirstSetupReader {

    private FirstSetupReader() {
    }

    /**
     * @throws IOException if its {@code DeviceMappings.xml} or one of its {@code .buttonMap} files cannot be read.
     *                     First setup does not run on a partial reading: an unread file could hold the one entry
     *                     the master must not lose
     */
    static FirstSetupPlan.Installation read(Target target, FrontierStockDevices stock,
                                            FrontierStockButtonMaps stockMaps) throws IOException {
        Path controlSchemes = target.controlSchemes();
        Path deviceMappings = GameInstallation.deviceMappingsIn(controlSchemes);
        Map<String, HardwareId> primaries = Files.isRegularFile(deviceMappings)
                ? DeviceMappingsParser.primaries(Files.readAllBytes(deviceMappings))
                : Map.of();

        Map<String, HardwareId> elements = new LinkedHashMap<>();
        Map<String, HardwareId> builtIns = new LinkedHashMap<>();
        Map<String, Map<String, String>> labels = new LinkedHashMap<>();
        for (Map.Entry<String, HardwareId> element : primaries.entrySet()) {
            String name = element.getKey();
            boolean frontiers = stock.ships(name);
            if (!frontiers) elements.put(name, element.getValue());

            Map<String, String> found = labelsOf(controlSchemes, name);
            if (found.isEmpty()) continue;
            if (frontiers && stockMaps.isShipped(name, found)) continue;
            labels.put(name, found);
            if (frontiers) builtIns.put(name, element.getValue());
        }
        return new FirstSetupPlan.Installation(target.installId(), target.storefront(), elements, builtIns, labels);
    }

    private static Map<String, String> labelsOf(Path controlSchemes, String deviceName) throws IOException {
        Path buttonMap;
        try {
            buttonMap = GameInstallation.buttonMapIn(controlSchemes, deviceName);
        } catch (InvalidPathException e) {
            // WHY: a name that cannot be a filename inside DeviceButtonMaps has no .buttonMap the game could
            // load, so there are no labels to take - not a reason to stop first setup.
            return Map.of();
        }
        if (!Files.isRegularFile(buttonMap)) return Map.of();
        try {
            return ButtonMapReader.read(Files.readAllBytes(buttonMap));
        } catch (IOException e) {
            throw new IOException(buttonMap.getFileName() + " could not be read: " + e.getMessage(), e);
        }
    }
}
