package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.storage.WriteReport.Place;
import elite.intel.io.AtomicFiles;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Writes one set of files to every live copy: the {@code .binds} and {@code StartPreset.N.start} to the bindings
 * folder, and the device files to every installation (storage.md rules 7-8; alias-designer.md, <em>What APPLY does to
 * each installation's device files</em>):
 * <ul>
 *     <li><strong>Device files whole, to every installation</strong> - a missing {@code DeviceMappings.xml} is
 *     simply written.</li>
 *     <li><strong>A file that already holds the same bytes is not rewritten</strong> (2026-10-25), so a write with
 *     nothing new for an installation writes nothing there.</li>
 *     <li><strong>A {@code .buttonMap} the set lacks is removed</strong>, except Frontier's shipped ones
 *     ({@link FrontierStockButtonMaps#shipsMapFor}).</li>
 *     <li><strong>Everything it can</strong>: a file or an installation that cannot be written is reported, the rest
 *     are still written, and nothing is rolled back.</li>
 * </ul>
 * A {@code .binds} the set lacks is left in the bindings folder - removing an old-named one is a rename's (Storage
 * S12). Only {@link WritePath} calls this, after the backup. Touches the disk - not for the EDT.
 */
final class LiveCopies {

    private static final Logger log = LogManager.getLogger(LiveCopies.class);

    static final String FOLDER_NOT_FOUND = "folder not found";

    private final BackupSources sources;

    LiveCopies(BackupSources sources) {
        this.sources = sources;
    }

    /** The bindings folder first, then each installation in the stored list's order. */
    WriteReport write(ConfigurationFiles files) {
        Place bindings = writeBindings(files);
        List<Place> installations = sources.installations().stream()
                .map(installation -> writeInstallation(installation, files))
                .toList();
        return new WriteReport(bindings, installations);
    }

    private Place writeBindings(ConfigurationFiles files) {
        Path folder = sources.bindingsFolder();
        Writing writing = new Writing();
        // WHY: not created. The game makes this folder; one that is not there is a wrong setting or an unplugged
        // drive, and creating it would put the user's files where the game never reads them.
        if (!Files.isDirectory(folder)) {
            writing.failures.add(FOLDER_NOT_FOUND);
            return writing.place("the bindings folder", folder);
        }
        // WHY: the one folder Elite-Intel is set to, on Linux too, where each installation has its own - every .binds
        // read and write in BindForge uses it (input-environment.md section 7; storage brief, Known issues).
        writing.writeAll(folder, files.bindings());
        return writing.place("the bindings folder", folder);
    }

    private Place writeInstallation(BackupSources.Installation installation, ConfigurationFiles files) {
        Writing writing = new Writing();
        Path controlSchemes = installation.controlSchemes();
        if (installation.missing() || !Files.isDirectory(controlSchemes)) {
            writing.failures.add(FOLDER_NOT_FOUND);
            return writing.place(installation.storefront(), installation.root());
        }
        if (files.hasDeviceMappings()) {
            writing.writeIfDifferent(GameInstallation.deviceMappingsIn(controlSchemes), files.deviceMappings());
        }
        Path buttonMaps = GameInstallation.deviceButtonMapsIn(controlSchemes);
        try {
            Files.createDirectories(buttonMaps);
            writing.writeAll(buttonMaps, files.buttonMaps());
            writing.removeOthers(buttonMaps, files.buttonMaps());
        } catch (IOException e) {
            writing.fail(buttonMaps, e);
        }
        return writing.place(installation.storefront(), installation.root());
    }

    /** What one place's write did so far. */
    private static final class Writing {
        final List<String> written = new ArrayList<>();
        final List<String> removed = new ArrayList<>();
        final List<String> failures = new ArrayList<>();

        void writeAll(Path folder, Map<String, byte[]> files) {
            files.forEach((name, content) -> writeIfDifferent(folder.resolve(name), content));
        }

        void writeIfDifferent(Path target, byte[] content) {
            try {
                if (Files.isRegularFile(target) && Arrays.equals(Files.readAllBytes(target), content)) return;
                AtomicFiles.write(target, content);
                written.add(target.getFileName().toString());
            } catch (IOException e) {
                fail(target, e);
            }
        }

        /** Removes every {@code .buttonMap} in {@code folder} the set lacks, except Frontier's shipped ones. */
        void removeOthers(Path folder, Map<String, byte[]> kept) throws IOException {
            List<Path> present;
            try (Stream<Path> listing = Files.list(folder)) {
                present = listing.filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(GameInstallation.BUTTON_MAP_SUFFIX))
                        .toList();
            }
            for (Path file : present) {
                String name = file.getFileName().toString();
                if (kept.containsKey(name) || isOneKept(file, folder, kept)) continue;
                if (FrontierStockButtonMaps.shipsMapFor(deviceNameOf(name))) continue;
                try {
                    Files.delete(file);
                    removed.add(name);
                } catch (IOException e) {
                    fail(file, e);
                }
            }
        }

        // WHY: on a file system that ignores case, lvwap.buttonMap on disk is the LVWAP.buttonMap just written - one
        // file, which deleting would lose. Asked of the file system rather than assumed, so on one that heeds case the
        // old spelling is a file of its own and goes.
        private static boolean isOneKept(Path file, Path folder, Map<String, byte[]> kept) throws IOException {
            for (String name : kept.keySet()) {
                if (!name.equalsIgnoreCase(file.getFileName().toString())) continue;
                Path keptFile = folder.resolve(name);
                if (Files.exists(keptFile) && Files.isSameFile(file, keptFile)) return true;
            }
            return false;
        }

        void fail(Path file, IOException e) {
            failures.add(file.getFileName() + ": " + reason(e));
            log.warn("Could not write {}", file, e);
        }

        Place place(String label, Path folder) {
            return new Place(label, folder, written, removed, failures);
        }
    }

    private static String deviceNameOf(String buttonMapName) {
        return buttonMapName.substring(0, buttonMapName.length() - GameInstallation.BUTTON_MAP_SUFFIX.length());
    }

    private static String reason(IOException e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
