package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.io.FileNames;
import elite.intel.bindforge.storage.WriteReport.Place;
import elite.intel.io.AtomicFiles;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
 *     ({@link ReadOnlyFlag}) - cleared for the write and set again (storage.md rule 10, 2026-10-25). A read-only
 *     {@code .buttonMap} the set lacks is kept, on every system, and reported with what to do.</li>
 *     <li><strong>Everything it can</strong>: a file or an installation that cannot be written is reported, the rest
 *     are still written, and nothing is rolled back. <strong>In the bindings folder the binds files go first</strong>:
 *     if one fails, the preset file is not written and nothing is removed, so the game is never left on a preset line
 *     naming a file that is not there (review, 2026-11-03).</li>
 *     <li><strong>One spelling of each file</strong> (Krondor and Alan, 2026-11-07, Storage S13a): names that differ
 *     only in case are one file ({@link FileNames#sameFile}). A file is written under the set's spelling - another
 *     spelling already there is renamed to it first, and any further spelling, which only a file system that heeds
 *     case allows, is removed and reported. So the working copy's spelling is the one left, a {@code .buttonMap} and
 *     {@code DeviceMappings.xml} included.</li>
 * </ul>
 * A {@code .binds} the set lacks is left in the bindings folder - removing an old-named one is a rename's (Storage
 * S12). <strong>What does not belong in the bindings folder is moved, never deleted</strong>, when the check names it
 * ({@link Leftovers}; Storage S13d): into a date-stamped folder of its own in Leftover User Files, after the write,
 * whatever its read-only flag, which the move does not change (Storage S13eR, S13eR2). Only {@link WritePath} calls
 * this, after the backup. Touches the disk - not for the EDT.
 */
final class LiveCopies {

    private static final Logger log = LogManager.getLogger(LiveCopies.class);

    static final String FOLDER_NOT_FOUND = "folder not found";

    /**
     * A failure line: a file a write would have removed - a {@code .buttonMap} the set lacks, a binds file named for
     * removal, a further spelling - was kept because its read-only flag could not be read.
     */
    static final String NOT_REMOVED_FLAG_UNREADABLE = "not removed: whether it is read-only could not be read";

    /** The date stamp of a folder in Leftover User Files - a backup's (Storage S13d). */
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * A failure line: a file held back because an earlier file in the bindings folder could not be written.
     */
    static final String HELD_BACK = "left as it is, because another file could not be written";

    /** Added to a file's name for the middle step of changing its spelling. */
    static final String RESPELLING = ".respelling";

    /**
     * Decides, just before an installation is written, whether to leave it alone - APPLY's for an installation whose
     * device files differ from the working copy (Alan, 2026-10-26).
     */
    @FunctionalInterface
    interface LeaveAlone {
        LeaveAlone NONE = installation -> false;

        boolean leaveAlone(BackupSources.Installation installation);
    }

    /**
     * Moves a file: {@link Files#move}, or in a test a move that fails, so a respelling that fails can be seen on any
     * file system (Storage S13aR, D1).
     */
    @FunctionalInterface
    interface Mover {
        Mover FILES = Files::move;

        void move(Path from, Path to) throws IOException;
    }

    /** Leftover User Files - asked only when there is a file to move ({@code AppPaths.getLeftoverUserFilesDir}). */
    @FunctionalInterface
    interface LeftoverFolder {
        /** For a write that moves nothing. */
        LeftoverFolder NONE = () -> {
            throw new IOException("no Leftover User Files folder was given");
        };

        Path get() throws IOException;
    }

    private final BackupSources sources;
    private final Mover mover;
    private final LeftoverFolder leftovers;

    LiveCopies(BackupSources sources) {
        this(sources, Mover.FILES, LeftoverFolder.NONE);
    }

    LiveCopies(BackupSources sources, Mover mover) {
        this(sources, mover, LeftoverFolder.NONE);
    }

    LiveCopies(BackupSources sources, Mover mover, LeftoverFolder leftovers) {
        this.sources = sources;
        this.mover = mover;
        this.leftovers = leftovers;
    }

    /**
     * The bindings folder first, then each installation in the stored list's order, leaving alone the installations
     * {@code leaveAlone} picks - asked only of one that is there; a missing one is reported as not found. Removes from
     * the bindings folder the binds and preset files named - the check's, for a live-only file the user kept the
     * working copy's side of (Alan, 2026-10-30, Storage S8), and a renamed one's old name; a read-only one is kept, and
     * reported, as a {@code .buttonMap} is.
     */
    WriteReport write(ConfigurationFiles files, LeaveAlone leaveAlone, Set<String> removeFromBindings) {
        return write(files, leaveAlone, removeFromBindings, Set.of());
    }

    /**
     * {@link #write(ConfigurationFiles, LeaveAlone, Set)}, then moving {@code moveFromBindings} - files that do not
     * belong in the bindings folder - to Leftover User Files (Storage S13d).
     */
    WriteReport write(ConfigurationFiles files, LeaveAlone leaveAlone, Set<String> removeFromBindings,
                      Set<String> moveFromBindings) {
        requireNamesToTakeAway(removeFromBindings, moveFromBindings);
        Place bindings = writeBindings(files, removeFromBindings, moveFromBindings);
        List<Place> installations = sources.installations().stream()
                .map(installation -> {
                    boolean present = installation.reachable();
                    if (present && leaveAlone.leaveAlone(installation)) {
                        return new Place(WriteReport.Kind.INSTALLATION, installation.storefront(), installation.root(),
                                List.of(), List.of(), List.of(), List.of(), true);
                    }
                    return writeInstallation(installation, files);
                })
                .toList();
        return new WriteReport(bindings, installations);
    }

    /**
     * Refuses a name to remove that is not a binds or preset file's, and a name to move that is the game's own or a
     * path - a bug in the caller, so it is thrown before anything is written, never half way through the write.
     */
    private static void requireNamesToTakeAway(Set<String> remove, Set<String> move) {
        for (String name : remove) {
            if (!FileNames.plain(name) || !FileNames.isBindingsFile(Path.of(name))) {
                throw new IllegalArgumentException("not a binds or preset file name: " + name);
            }
        }
        for (String name : move) {
            if (!FileNames.plain(name) || FileNames.staysInBindingsFolder(name)) {
                throw new IllegalArgumentException("not a file to move from the bindings folder: " + name);
            }
        }
    }

    private Place writeBindings(ConfigurationFiles files, Set<String> remove, Set<String> move) {
        Path folder = sources.bindingsFolder();
        Writing writing = new Writing(mover);
        // WHY: not created. The game makes this folder; one that is not there is a wrong setting or an unplugged
        // drive, and creating it would put the user's files where the game never reads them.
        if (!Files.isDirectory(folder)) {
            writing.failures.add(FOLDER_NOT_FOUND);
            return writing.place(WriteReport.Kind.BINDINGS_FOLDER, null, folder);
        }
        // WHY: the one folder Elite-Intel is set to, on Linux too, where each installation has its own - every .binds
        // read and write in BindForge uses it (input-environment.md section 7; storage brief, Known issues).
        Map<String, byte[]> presetFiles = new TreeMap<>(files.bindings());
        presetFiles.keySet().removeIf(name -> !FileNames.isPresetFile(name));
        Map<String, byte[]> bindsFiles = new TreeMap<>(files.bindings());
        bindsFiles.keySet().removeAll(presetFiles.keySet());
        writing.writeAll(folder, bindsFiles);
        if (writing.failures.isEmpty()) {
            writing.writeAll(folder, presetFiles);
        } else {
            writing.heldBack(presetFiles.keySet());
        }
        if (!writing.failures.isEmpty()) {
            writing.heldBack(remove);
            // WHY: a converted plain file's binds file may be the one that failed, so the plain file stays too.
            writing.heldBack(move);
            return writing.place(WriteReport.Kind.BINDINGS_FOLDER, null, folder);
        }
        for (String name : remove) {
            // WHY: a name the set holds in any spelling is the file just written - the edit wins (Storage S13a).
            if (files.bindings().keySet().stream().anyMatch(written -> FileNames.sameFile(written, name))) continue;
            writing.spellingsIn(folder, name).forEach(spelling -> writing.remove(folder.resolve(spelling)));
        }
        for (String name : move) writing.moveAway(folder.resolve(name), leftovers);
        return writing.place(WriteReport.Kind.BINDINGS_FOLDER, null, folder);
    }

    private Place writeInstallation(BackupSources.Installation installation, ConfigurationFiles files) {
        Writing writing = new Writing(mover);
        Path controlSchemes = installation.controlSchemes();
        if (!installation.reachable()) {
            writing.failures.add(FOLDER_NOT_FOUND);
            return writing.place(WriteReport.Kind.INSTALLATION, installation.storefront(), installation.root());
        }
        if (files.hasDeviceMappings()) {
            // WHY: through writeAll, so a devicemappings.xml there is that file and leaves under the game's spelling
            // (Alan, 2026-11-07, Storage S13a D5).
            writing.writeAll(controlSchemes, Map.of(GameInstallation.DEVICE_MAPPINGS, files.deviceMappings()));
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
        private final Mover mover;
        final List<String> written = new ArrayList<>();
        final List<String> removed = new ArrayList<>();
        final List<String> keptReadOnly = new ArrayList<>();
        final List<String> failures = new ArrayList<>();
        final List<String> moved = new ArrayList<>();
        Path movedTo;

        Writing(Mover mover) {
            this.mover = mover;
        }

        void heldBack(Set<String> names) {
            names.forEach(name -> failures.add(name + ": " + HELD_BACK));
        }

        /**
         * Writes each file under its own spelling. Another spelling of it in the folder is renamed to it first, so the
         * file is replaced rather than a second one written beside it; any further spelling - two or more, which only a
         * file system that heeds case allows - is removed after the write (Krondor and Alan, 2026-11-07, Storage S13a).
         * A rename that fails is reported and the file written under its old spelling, so the next write tries the
         * rename again (Storage S13aR, D1).
         */
        void writeAll(Path folder, Map<String, byte[]> files) {
            List<String> present = namesIn(folder);
            files.forEach((name, content) -> {
                List<String> others = present.stream()
                        .filter(found -> FileNames.sameFile(found, name) && !found.equals(name)).toList();
                Path target = folder.resolve(name);
                boolean respelled = false;
                List<String> extra = others;
                if (!present.contains(name) && !others.isEmpty()) {
                    Path old = folder.resolve(others.getFirst());
                    respelled = respell(old, target);
                    // WHY: where the file system heeds case, the new spelling would be a second file beside the old -
                    // the state BindForge never leaves (Storage S13aR, D1).
                    if (!respelled) target = old;
                    extra = others.subList(1, others.size());
                }
                writeIfDifferent(target, content);
                if (respelled && !written.contains(name)) written.add(name);
                extra.forEach(other -> remove(folder.resolve(other)));
            });
        }

        /** The spellings of {@code name} the folder holds - none, one, or on a file system that heeds case, several. */
        List<String> spellingsIn(Path folder, String name) {
            return namesIn(folder).stream().filter(found -> FileNames.sameFile(found, name)).toList();
        }

        /** The names of the files in {@code folder}, as spelled on disk; none, logged, when it cannot be listed. */
        private static List<String> namesIn(Path folder) {
            try (Stream<Path> listing = Files.list(folder)) {
                return listing.filter(Files::isRegularFile).map(file -> file.getFileName().toString()).toList();
            } catch (IOException e) {
                // WHY: the writes that follow say what failed; without a listing a file is only written, not respelled.
                log.warn("Could not list {} to find other spellings of the files written", folder, e);
                return List.of();
            }
        }

        /**
         * Renames {@code from} to {@code to}, names that differ only in case, in two steps through a name of its own:
         * on Windows a move to a name the file system takes for the same file does nothing. On a failure the file is
         * put back and reported, and the write goes on under the old spelling.
         */
        private boolean respell(Path from, Path to) {
            Path step = to.resolveSibling(to.getFileName() + RESPELLING);
            try {
                mover.move(from, step);
            } catch (IOException e) {
                fail(from, e);
                return false;
            }
            try {
                mover.move(step, to);
                log.info("Renamed {} to {}", from.getFileName(), to.getFileName());
                return true;
            } catch (IOException e) {
                fail(to, e);
                try {
                    mover.move(step, from);
                } catch (IOException back) {
                    failures.add(from.getFileName() + ": left as " + step.getFileName() + ": " + reason(back));
                    log.warn("Could not put {} back as {}", step, from, back);
                }
                return false;
            }
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
                if (kept.keySet().stream().anyMatch(keptName -> FileNames.sameFile(keptName, name))) continue;
                if (FrontierStockButtonMaps.shipsMapFor(deviceNameOf(name))) continue;
                remove(file);
            }
        }

        /** Removes one file; a read-only one is kept and reported, never unprotected to delete it. */
        void remove(Path file) {
            if (!Files.isRegularFile(file) || kept(file)) return;
            try {
                Files.delete(file);
                removed.add(file.getFileName().toString());
            } catch (IOException e) {
                fail(file, e);
            }
        }

        /**
         * Moves one file into this write's folder in Leftover User Files, made at the first move and stamped as a backup
         * is (Storage S13d); a first move that fails takes the folder away again. <strong>Moved whatever its read-only
         * flag</strong> (Alan, 2026-11-09, Storage S13eR): the flag protects a file from being deleted, and a leftover is
         * kept, in Leftover User Files. <strong>The move changes no flag</strong> (Alan, 2026-11-09, Storage S13eR2), and
         * the flag does not stop it: on Windows both folders are under LOCALAPPDATA, one drive, so the move is a rename;
         * on Linux a move to another drive copies and unlinks, which a read-only file allows. One that cannot be moved
         * stays where it is, reported.
         */
        void moveAway(Path file, LeftoverFolder leftovers) {
            if (!Files.isRegularFile(file)) return;
            String name = file.getFileName().toString();
            try {
                if (movedTo == null) {
                    Path folder = uniqueFolder(leftovers.get(), LocalDateTime.now().withNano(0).format(STAMP));
                    Files.createDirectories(folder);
                    movedTo = folder;
                }
                // WHY: Files.move copies and deletes when the two folders are on different drives (Storage S13d, Q7).
                mover.move(file, movedTo.resolve(name));
                moved.add(name);
                log.info("Moved {} from the bindings folder to {}", name, movedTo);
            } catch (IOException e) {
                failures.add(name + ": not moved to Leftover User Files: " + reason(e));
                log.warn("Could not move {} to Leftover User Files", file, e);
                if (moved.isEmpty()) discardEmptyFolder();
            }
        }

        /**
         * Takes this write's folder in Leftover User Files away again while nothing has gone into it, so a first move
         * that fails leaves no empty folder - the file stays, and every check after tries it again.
         */
        private void discardEmptyFolder() {
            if (movedTo == null) return;
            try {
                Files.deleteIfExists(movedTo);
                movedTo = null;
            } catch (IOException e) {
                // WHY: something is in it - what a failed copy-and-delete left behind - so it stays, for the user to
                // find, and the failure line already names the file.
                log.warn("Could not take away the folder {} in Leftover User Files that nothing was moved to", movedTo, e);
            }
        }

        /**
         * Whether {@code file} is kept because the user made it read-only - or because whether they did cannot be read,
         * reported as a failure. A protected file is never unprotected to delete it.
         */
        private boolean kept(Path file) {
            String name = file.getFileName().toString();
            // WHY: the user protected it, so it is not taken away against that - asked first, because on Linux the
            // delete would succeed whatever the file's own permission. They are told how to let it go, rather than shown
            // the system's "access denied" (principle 9; Alan, 2026-10-25, from the review).
            try {
                if (!ReadOnlyFlag.isSet(file)) return false;
            } catch (IOException e) {
                // WHY: a file whose protection cannot be read may be protected, so it is kept, not taken away, and
                // reported (Krondor's review, 2026-10-27). Taken for unprotected, it was deleted.
                failures.add(name + ": " + NOT_REMOVED_FLAG_UNREADABLE + " (" + reason(e) + ")");
                log.warn("Left {} where it is: whether it is read-only could not be read", file, e);
                return true;
            }
            keptReadOnly.add(name);
            log.warn("Left {} where it is: it is read-only", file);
            return true;
        }

        void fail(Path file, IOException e) {
            failures.add(file.getFileName() + ": " + reason(e));
            log.warn("Could not write {}", file, e);
        }

        Place place(WriteReport.Kind kind, String storefront, Path folder) {
            // WHY: the folder is made at the first move, so one whose move failed is no place anything went.
            return new Place(kind, storefront, folder, written, removed, keptReadOnly, failures, false, moved,
                    moved.isEmpty() ? null : movedTo);
        }
    }

    /** {@code stamp} in {@code root}, or {@code stamp-2}, {@code -3} and on when two writes come in one second. */
    private static Path uniqueFolder(Path root, String stamp) {
        Path candidate = root.resolve(stamp);
        for (int n = 2; Files.exists(candidate); n++) {
            candidate = root.resolve(stamp + "-" + n);
        }
        return candidate;
    }

    private static String deviceNameOf(String buttonMapName) {
        return buttonMapName.substring(0, buttonMapName.length() - GameInstallation.BUTTON_MAP_SUFFIX.length());
    }

    private static String reason(IOException e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
