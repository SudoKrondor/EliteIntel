package elite.intel.bindforge.storage;

import elite.intel.bindforge.preseteditor.BindNameEntry;
import elite.intel.bindforge.preseteditor.BindsFileOrigin;
import elite.intel.bindforge.storage.BindingsComparison.Differences;
import elite.intel.bindforge.storage.BindingsComparison.Factory;
import elite.intel.bindforge.storage.BindingsComparison.Side;
import elite.intel.db.managers.BindForgeInstallationsManager;
import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The check for the binds and preset files (storage.md rule 14; Storage S8a): the bindings folder compared with the
 * working copy by value ({@link BindingsComparison}), then - once the user has chosen - the settled files written
 * through {@link WritePath}, backed up first with the working copy inside the backup (rule 12).
 * <p>
 * <strong>The three choices are answers to every row</strong>: overwriting the live copy keeps the working copy's side
 * of each, overwriting the working copy keeps the game's, and a merge answers row by row (Alan, 2026-10-30).
 * <p>
 * <strong>Nothing watches the files</strong>: just before the write the bindings folder is compared again, and if a
 * row changed meanwhile nothing is written ({@link ChangedMeanwhileException}, carrying what is found now). An
 * installation whose device files differ is left alone, as APPLY leaves it, until Storage S9. Touches the disk - not
 * for the EDT.
 */
public final class BindingsCheck {

    private static final Logger log = LogManager.getLogger(BindingsCheck.class);

    /** Reads the binds and preset files in a bindings folder. */
    @FunctionalInterface
    interface LiveReader {
        Map<String, byte[]> read(Path folder) throws IOException;
    }

    /** Frontier's factory binds files, for a set, from every reachable installation. */
    @FunctionalInterface
    interface FactorySource {
        Factory of(ConfigurationFiles files) throws IOException;
    }

    /** The check's write. */
    @FunctionalInterface
    interface Writer {
        WriteReport write(ConfigurationFiles files, WritePath.BackUpFirst backUp, LiveCopies.LeaveAlone leaveAlone,
                          Set<String> removeFromBindings) throws IOException;
    }

    /** The before-the-check backup: the live copies, and the working copy as it is. */
    @FunctionalInterface
    interface BackUp {
        Backup take(BackupSources sources, ConfigurationFiles workingCopy) throws IOException;
    }

    /**
     * What one check found, and what it compared.
     *
     * @param workingCopy the working copy, as compared
     * @param live        the bindings folder's binds and preset files, as compared
     */
    public record Found(ConfigurationFiles workingCopy, Map<String, byte[]> live, Differences differences) {
        public Found {
            Objects.requireNonNull(workingCopy, "workingCopy");
            live = Map.copyOf(live);
            Objects.requireNonNull(differences, "differences");
        }
    }

    /**
     * The bindings folder or the working copy changed between the warning and the write, so nothing was written.
     * {@link #now()} is what the check finds now; answers to rows that are still the same carry over.
     */
    public static final class ChangedMeanwhileException extends IOException {
        private final transient Found now;

        ChangedMeanwhileException(Found now) {
            super("the control files changed while the check was open - nothing was written");
            this.now = now;
        }

        public Found now() {
            return now;
        }
    }

    /**
     * Edits not applied are on screen, and the check's write would throw them away - nothing was written. The message
     * is for the log.
     */
    public static final class EditsNotAppliedException extends IOException {
        EditsNotAppliedException() {
            super("there are edits not applied - the check would throw them away, so nothing was written");
        }
    }

    private final Edits edits;
    private final Supplier<Path> bindingsFolder;
    private final LiveReader liveReader;
    private final FactorySource factory;
    private final Writer writer;
    private final BackUp backUp;
    private final Apply.Reader deviceFiles;

    /** The shared edits, the bindings folder Elite-Intel is set to, the backup folder and the write path. */
    public static BindingsCheck stored() {
        return new BindingsCheck(Edits.stored(), () -> PlayerSession.getInstance().getBindingsDir(),
                BindingsCheck::readBindingsFolder, BindingsCheck::factoryOf,
                (files, backUp, leaveAlone, remove) -> WritePath.stored().write(files, backUp, leaveAlone, remove),
                (sources, workingCopy) -> Backups.stored().backUpBeforeCheck(sources, workingCopy),
                installation -> DeviceFiles.read(installation.root()));
    }

    BindingsCheck(Edits edits, Supplier<Path> bindingsFolder, LiveReader liveReader, FactorySource factory,
                  Writer writer, BackUp backUp, Apply.Reader deviceFiles) {
        this.edits = edits;
        this.bindingsFolder = bindingsFolder;
        this.liveReader = liveReader;
        this.factory = factory;
        this.writer = writer;
        this.backUp = backUp;
        this.deviceFiles = deviceFiles;
    }

    /**
     * Compares the bindings folder with the working copy. Reads only.
     *
     * @throws NoSuchFileException if there is no working copy, or no bindings folder - nothing can be compared
     * @throws IOException         if either cannot be read
     */
    public Found find() throws IOException {
        ConfigurationFiles workingCopy = edits.snapshot().applied();
        return find(workingCopy, liveReader.read(bindingsFolder.get()));
    }

    private Found find(ConfigurationFiles workingCopy, Map<String, byte[]> live) throws IOException {
        Differences differences = BindingsComparison.compare(workingCopy.bindings(), live, Factory.NONE);
        // WHY: the factory binds files are read only when something differs - most first clicks find nothing.
        if (!differences.none()) {
            differences = BindingsComparison.compare(workingCopy.bindings(), live, factory.of(workingCopy));
        }
        if (!differences.leftOut().isEmpty()) {
            log.info("The check left out until a newer game version is reconciled (Storage S10): {}",
                    String.join(", ", differences.leftOut()));
        }
        return new Found(workingCopy, live, differences);
    }

    /**
     * Writes what the user settled on: backs up the live copies and the working copy, saves the settled files as the
     * working copy, and writes them to the bindings folder - removing each live-only file the user kept the working
     * copy's side of. The device files are written as the working copy holds them, leaving alone an installation whose
     * own differ.
     *
     * @param found   what the warning showed
     * @param answers the side each of its rows keeps, by {@link BindingsComparison.LineItem#id()}
     * @throws ChangedMeanwhileException if a row, or the working copy, changed since {@code found} - nothing written
     * @throws EditsNotAppliedException  if there are edits not applied - nothing written
     * @throws Edits.WriteRunningException if another write is running
     * @throws IOException               if the files cannot be read again, a merge cannot be made, the backup cannot
     *                                   be taken or the working copy cannot be saved - no live copy was written
     */
    public Written settle(Found found, Map<String, Side> answers) throws IOException {
        return edits.whileWriting(() -> {
            // WHY: the write hands its set to the edits, which would drop them unseen.
            if (edits.hasEdits()) throw new EditsNotAppliedException();
            ConfigurationFiles workingCopy = edits.snapshot().applied();
            Map<String, byte[]> live = liveReader.read(bindingsFolder.get());
            Differences now = BindingsComparison.compare(workingCopy.bindings(), live, Factory.NONE);
            if (!workingCopy.sameAs(found.workingCopy()) || !now.items().equals(found.differences().items())) {
                throw new ChangedMeanwhileException(find(workingCopy, live));
            }
            BindingsComparison.Settled settled = BindingsComparison.settle(workingCopy.bindings(), live, now, answers);
            ConfigurationFiles files = new ConfigurationFiles(settled.bindings(),
                    workingCopy.hasDeviceMappings() ? workingCopy.deviceMappings() : null, workingCopy.buttonMaps());
            DeviceFilesComparison comparison = DeviceFilesComparison.of(workingCopy);
            AtomicReference<Backup> taken = new AtomicReference<>();
            WriteReport report = writer.write(files, sources -> {
                Backup backup = backUp.take(sources, workingCopy);
                taken.set(backup);
                return backup;
            }, Apply.leavingAloneWhereDifferent(comparison, deviceFiles), settled.removeFromLive());
            log.info("The check settled {} files: {} rows; removed from the bindings folder: {}",
                    now.files().size(), now.items().size(), settled.removeFromLive());
            return new Written(report, taken.get());
        });
    }

    /**
     * The binds and preset files in a bindings folder, by name - nothing else in it.
     *
     * @throws NoSuchFileException if the folder is not there
     */
    static Map<String, byte[]> readBindingsFolder(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) throw new NoSuchFileException(folder.toString(), null, "no bindings folder");
        List<Path> found;
        try (Stream<Path> listing = Files.list(folder)) {
            found = listing.filter(Files::isRegularFile).filter(FileNames::isBindingsFile).toList();
        }
        Map<String, byte[]> files = new HashMap<>();
        for (Path file : found) {
            files.put(file.getFileName().toString(), Files.readAllBytes(file));
        }
        return files;
    }

    /**
     * The factory binds files of every reachable installation - their BindNames and bytes. One that cannot be read is
     * logged and left out: it only names a reset.
     */
    private static Factory factoryOf(ConfigurationFiles files) {
        Apply.Offered offered = Apply.offered(files, BindForgeInstallationsManager.getInstance().findAll());
        Set<String> names = new HashSet<>();
        List<byte[]> contents = new ArrayList<>();
        for (BindNameEntry entry : offered.list().entries()) {
            if (entry.origin() != BindsFileOrigin.FACTORY) continue;
            names.add(entry.bindName());
            for (Path file : entry.files()) {
                try {
                    contents.add(Files.readAllBytes(file));
                } catch (IOException e) {
                    log.warn("Could not read the factory binds file {} to tell a reset", file, e);
                }
            }
        }
        return new Factory(names, contents);
    }
}
