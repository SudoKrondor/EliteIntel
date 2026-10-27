package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * One installation's device files as they are on disk: its {@code DeviceMappings.xml} and every {@code .buttonMap}, as
 * bytes. What first-time startup reads into {@link InstallationFiles}, and what APPLY compares with the working copy
 * ({@link DeviceFilesComparison}) - read straight from the installation's folder, with no stored id (Storage S4a review,
 * 2026-10-27).
 *
 * @param deviceMappings its {@code DeviceMappings.xml}, or {@code null} when it has none
 * @param buttonMaps     every {@code .buttonMap} in its {@code DeviceButtonMaps} folder, by file name
 */
public record DeviceFiles(byte[] deviceMappings, Map<String, byte[]> buttonMaps) {

    public DeviceFiles {
        buttonMaps = Collections.unmodifiableMap(new TreeMap<>(buttonMaps));
    }

    /**
     * Reads the device files of the installation at {@code root}.
     *
     * @throws IOException if a file that is there cannot be read - the message names it. An unread file could hold the
     *                     one entry the working copy must not lose (alias-designer.md, <em>Activation</em>)
     */
    public static DeviceFiles read(Path root) throws IOException {
        Path controlSchemes = GameInstallation.controlSchemesUnder(root);
        Path mappings = GameInstallation.deviceMappingsIn(controlSchemes);
        byte[] deviceMappings = Files.isRegularFile(mappings) ? readNamed(mappings) : null;

        Map<String, byte[]> buttonMaps = new TreeMap<>();
        Path folder = GameInstallation.deviceButtonMapsIn(controlSchemes);
        if (Files.isDirectory(folder)) {
            List<Path> found;
            try (Stream<Path> files = Files.list(folder)) {
                found = files.filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(GameInstallation.BUTTON_MAP_SUFFIX))
                        .sorted().toList();
            } catch (IOException e) {
                throw new IOException(GameInstallation.DEVICE_BUTTON_MAPS + " could not be listed: " + e.getMessage(),
                        e);
            }
            for (Path file : found) buttonMaps.put(file.getFileName().toString(), readNamed(file));
        }
        return new DeviceFiles(deviceMappings, buttonMaps);
    }

    /** Whether the installation holds a {@code DeviceMappings.xml}. */
    public boolean hasDeviceMappings() {
        return deviceMappings != null;
    }

    private static byte[] readNamed(Path file) throws IOException {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new IOException(file.getFileName() + " could not be read: " + e.getMessage(), e);
        }
    }
}
