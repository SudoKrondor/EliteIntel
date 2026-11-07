package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.io.FileNames;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final Logger log = LogManager.getLogger(DeviceFiles.class);

    public DeviceFiles {
        Map<String, byte[]> sorted = new TreeMap<>(FileNames.ONE_FILE);
        sorted.putAll(buttonMaps);
        buttonMaps = Collections.unmodifiableMap(sorted);
    }

    /**
     * Reads the device files of the installation at {@code root}.
     *
     * @throws IOException if a file that is there cannot be read - the message names it. An unread file could hold the
     *                     one entry the working copy must not lose (alias-designer.md, <em>Activation</em>)
     */
    public static DeviceFiles read(Path root) throws IOException {
        Path controlSchemes = GameInstallation.controlSchemesUnder(root);
        // WHY: found ignoring case, as the game finds it (Storage S13a D5).
        Optional<Path> mappings;
        try {
            mappings = GameInstallation.findDeviceMappingsIn(controlSchemes);
        } catch (IOException e) {
            throw new IOException(controlSchemes.getFileName() + " could not be listed: " + e.getMessage(), e);
        }
        byte[] deviceMappings = mappings.isPresent() ? readNamed(mappings.get()) : null;

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
        // WHY: two spellings of one map - only where the file system heeds case - are one file to the game; the write
        // removes the other (Storage S13a D3).
        FileNames.OneSpelling<byte[]> one = FileNames.oneSpelling(buttonMaps, List.of());
        if (!one.leftOut().isEmpty()) {
            log.warn("{} holds two spellings of one .buttonMap; left out: {}", folder, one.leftOut());
        }
        return new DeviceFiles(deviceMappings, one.files());
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
