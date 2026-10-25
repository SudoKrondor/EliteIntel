package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * One installation's device files as first-time startup read them: its {@code DeviceMappings.xml} and every
 * {@code .buttonMap}, as bytes. Read once, so what the user reconciles is exactly what is written.
 *
 * @param installId      the stored list's id
 * @param storefront     the stored list's label
 * @param root           the folder holding {@code Products}
 * @param deviceMappings its {@code DeviceMappings.xml}, or {@code null} when it has none
 * @param buttonMaps     every {@code .buttonMap} in its {@code DeviceButtonMaps} folder, by file name
 */
public record InstallationFiles(long installId, String storefront, Path root, byte[] deviceMappings,
                                Map<String, byte[]> buttonMaps) {

    public InstallationFiles {
        Objects.requireNonNull(storefront, "storefront");
        Objects.requireNonNull(root, "root");
        buttonMaps = Collections.unmodifiableMap(new TreeMap<>(buttonMaps));
    }

    /**
     * Reads a stored installation's device files.
     *
     * @throws IOException if a file that is there cannot be read - the message names it. An unread file could hold
     *                     the one entry the working copy must not lose (alias-designer.md, <em>Activation</em>)
     */
    public static InstallationFiles read(InstallationRow row) throws IOException {
        Path root = Path.of(row.rootPath());
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
        return new InstallationFiles(row.id(), row.storefront(), root, deviceMappings, buttonMaps);
    }

    /** Whether this installation holds a {@code DeviceMappings.xml} - "use one installation for all" needs one. */
    public boolean hasDeviceMappings() {
        return deviceMappings != null;
    }

    /** Whether the other holds exactly the same device files, byte for byte. */
    public boolean sameFilesAs(InstallationFiles other) {
        if (!Arrays.equals(deviceMappings, other.deviceMappings)) return false;
        if (!buttonMaps.keySet().equals(other.buttonMaps.keySet())) return false;
        for (Map.Entry<String, byte[]> file : buttonMaps.entrySet()) {
            if (!Arrays.equals(file.getValue(), other.buttonMaps.get(file.getKey()))) return false;
        }
        return true;
    }

    private static byte[] readNamed(Path file) throws IOException {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new IOException(file.getFileName() + " could not be read: " + e.getMessage(), e);
        }
    }
}
