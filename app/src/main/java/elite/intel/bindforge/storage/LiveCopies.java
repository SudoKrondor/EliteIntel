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
 *     <li><strong>A file that already holds the same bytes is not rewritten</strong> (2026-10-24), so a write with
 *     nothing new for an installation writes nothing there.</li>
 *     <li><strong>A {@code .buttonMap} the set lacks is removed</strong>, except Frontier's shipped ones
 *     ({@link FrontierStockButtonMaps#shipsMapFor}).</li>
 *     <li><strong>A read-only file stays read-only</strong> - Windows' flag, or the owner's write permission elsewhere
 *     ({@link ReadOnlyFlag}) - cleared for the write and set again (storage.md rule 10, 2026-10-25). On Windows a
 *     read-only {@code .buttonMap} the set lacks cannot be removed, and is reported with what to do.</li>
 *     <li><strong>Everything it can</strong>: a file or an installation that cannot be written is reported, the rest
 *     are still written, and nothing is rolled back.</li>
 * </ul>
 * A {@code .binds} the set lacks is left in the bindings folder - removing an old-named one is a rename's (Storage
 * S12). Only {@link WritePath} calls this, after the backup. Touches the disk - not for the EDT.
 */
final class LiveCopies {

    private static final Logger log = LogManager.getLogger(LiveCopies.class);

    static final String FOLDER_NOT_FOUND = "folder not found";
    /** A {@code .buttonMap} the set lacks that the user made read-only - English, like every failure line. */
    static final String READ_ONLY_NOT_REMOVED =
            "not removed because it is read-only - clear its read-only flag, and the next write removes it";

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
            return writing.place(WriteReport.Kind.BINDINGS_FOLDER, null, folder);
        }
        // WHY: the one folder Elite-Intel is set to, on Linux too, where each installation has its own - every .binds
        // read and write in BindForge uses it (input-environment.md section 7; storage brief, Known issues).
        writing.writeAll(folder, files.bindings());
        return writing.place(WriteReport.Kind.BINDINGS_FOLDER, null, folder);
    }

    private Place writeInstallation(BackupSources.Installation installation, ConfigurationFiles files) {
        Writing writing = new Writing();
        Path controlSchemes = installation.controlSchemes();
        if (installation.missing() || !Files.isDirectory(controlSchemes)) {
            writing.failures.add(FOLDER_NOT_FOUND);
            return writing.place(WriteReport.Kind.INSTALLATION, installation.storefront(), installation.root());
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
        return writing.place(WriteReport.Kind.INSTALLATION, installation.storefront(), installation.root());
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
            } catch (IOException e) {
                fail(target, e);
                return;
            }
            // WHY: players protect a binds file to stop the game resetting it, and on Windows a file with the flag cannot
            // be replaced; on Linux the new file would come back writable. The write is the user asking, so the
            // protection is lifted, the file written, and the protection put back - the live file's as it is now
            // (storage.md rule 10; Alan, 2026-10-25).
            boolean readOnly;
            try {
                readOnly = ReadOnlyFlag.isSet(target);
                if (readOnly) ReadOnlyFlag.set(target, false);
            } catch (IOException e) {
                fail(target, e);
                return;
            }
            try {
                AtomicFiles.write(target, content);
                written.add(target.getFileName().toString());
            } catch (IOException e) {
                fail(target, e);
            } finally {
                if (readOnly) restoreFlag(target);
            }
        }

        private void restoreFlag(Path target) {
            try {
                ReadOnlyFlag.set(target, true);
            } catch (IOException e) {
                failures.add(target.getFileName() + ": its read-only flag could not be set again: " + reason(e));
                log.warn("Could not set the read-only flag on {} again", target, e);
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
                    if (isReadOnly(file)) {
                        // WHY: the user protected it, so it is not removed against that - they are told how to let it
                        // go, rather than shown the system's "access denied" (principle 9; review, 2026-10-25).
                        failures.add(name + ": " + READ_ONLY_NOT_REMOVED);
                        log.warn("Did not remove {}: it is read-only", file, e);
                    } else {
                        fail(file, e);
                    }
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

        private static boolean isReadOnly(Path file) {
            try {
                return ReadOnlyFlag.isSet(file);
            } catch (IOException e) {
                return false;
            }
        }

        void fail(Path file, IOException e) {
            failures.add(file.getFileName() + ": " + reason(e));
            log.warn("Could not write {}", file, e);
        }

        Place place(WriteReport.Kind kind, String storefront, Path folder) {
            return new Place(kind, storefront, folder, written, removed, failures);
        }
    }

    private static String deviceNameOf(String buttonMapName) {
        return buttonMapName.substring(0, buttonMapName.length() - GameInstallation.BUTTON_MAP_SUFFIX.length());
    }

    private static String reason(IOException e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
