package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

/**
 * A labelled copy of every installation's device files, taken immediately before an operation that rewrites them
 * all - first setup's <em>"just before I standardised"</em> restore point (alias-designer.md, <em>Two ways
 * back</em>).
 * <p>
 * Each installation's {@code DeviceMappings.xml} and its whole {@code DeviceButtonMaps} folder, as found, under
 * {@code <player backups>/<yyyy-MM-dd_HH-mm-ss>_<label>/<installation id>-<storefront>/}.
 * <p>
 * <strong>Always the default player backups folder</strong> (Alan, 2026-10-07). A destination the user chose is
 * resolved only inside {@code PlayerBackupService}, which is shared with V1.1; honouring it here waits for that
 * resolver to be exposed. The folder name sorts with the player backups because it starts the same way.
 */
public final class DeviceFilesSnapshot {

    private static final Logger log = LogManager.getLogger(DeviceFilesSnapshot.class);
    // Matches PlayerBackupService's folder names, so the two sort together.
    private static final DateTimeFormatter FOLDER_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Path root;
    private final Clock clock;

    /** @throws IOException if Elite-Intel's player backups folder cannot be created */
    public DeviceFilesSnapshot() throws IOException {
        this(AppPaths.getPlayerBackupsDir(), Clock.systemDefaultZone());
    }

    DeviceFilesSnapshot(Path root, Clock clock) {
        this.root = root;
        this.clock = clock;
    }

    /**
     * Copies every reachable installation's device files. An installation whose folder is gone has nothing to
     * copy and is left out; the operation that follows skips it too.
     *
     * @param label names the snapshot - {@code first-setup}
     * @return the snapshot's folder
     * @throws IOException if any copy fails. The operation it protects must not go ahead
     */
    public Path take(String label, List<Target> targets) throws IOException {
        Path folder = uniqueFolder(ZonedDateTime.now(clock).format(FOLDER_TIMESTAMP) + "_" + label);
        Files.createDirectories(folder);
        for (Target target : targets) {
            if (target.missing() || !Files.isDirectory(target.controlSchemes())) continue;
            Path into = folder.resolve(target.installId() + "-" + safe(target.storefront()));
            Files.createDirectories(into);

            Path deviceMappings = GameInstallation.deviceMappingsIn(target.controlSchemes());
            if (Files.isRegularFile(deviceMappings)) {
                Files.copy(deviceMappings, into.resolve(GameInstallation.DEVICE_MAPPINGS),
                        StandardCopyOption.COPY_ATTRIBUTES);
            }
            Path buttonMaps = GameInstallation.deviceButtonMapsIn(target.controlSchemes());
            if (!Files.isDirectory(buttonMaps)) continue;
            Path mapsInto = into.resolve(GameInstallation.DEVICE_BUTTON_MAPS);
            Files.createDirectories(mapsInto);
            try (Stream<Path> files = Files.list(buttonMaps)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    Files.copy(file, mapsInto.resolve(file.getFileName().toString()),
                            StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
        log.info("Device files backed up before {} at {}", label, folder);
        return folder;
    }

    private Path uniqueFolder(String name) {
        Path folder = root.resolve(name);
        for (int n = 2; Files.exists(folder); n++) {
            folder = root.resolve(name + "-" + n);
        }
        return folder;
    }

    /** A storefront is a label, not a filename; anything that could not be one is replaced. */
    private static String safe(String storefront) {
        return storefront.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
