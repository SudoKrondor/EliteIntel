package elite.intel.bindforge.storage;

import elite.intel.bindforge.preseteditor.BindNameEntry;
import elite.intel.bindforge.preseteditor.BindsFileOrigin;
import elite.intel.bindforge.storage.BindingsComparison.Differences;
import elite.intel.bindforge.storage.BindingsComparison.Factory;
import elite.intel.bindforge.storage.BindingsComparison.Side;
import elite.intel.db.managers.BindForgeInstallationsManager;
import elite.intel.session.PlayerSession;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
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
 * row changed meanwhile nothing is written ({@link ChangedMeanwhileException}, carrying what is found now).
 * <p>
 * <strong>The device files are checked too</strong> (Storage S9a and S9b; {@link DeviceCheck}): every reachable
 * installation judged against the working copy, in the same warning, backup and write - the device files the user
 * settled on go to every installation the check read, and an installation whose device files changed meanwhile stops
 * the write as a row does. The reconcile's reading is built here ({@link #reading}), from the binds files the user's
 * answers settle.
 * <p>
 * <strong>A newer version of a working-copy binds file</strong> (Storage S10; {@link BindingsComparison.NewerVersion})
 * is found by the same comparison, its rows answered in the same map, and written in the same backup and write: the
 * newer file becomes the working copy's, the older name goes.
 * <p>
 * <strong>At APPLY</strong> (Storage S8b, S9b) {@link Apply} runs {@link #find()} and {@link #settled} itself, and writes
 * the settled files with the edits on top in its own one write; {@link #settle} is the first click's, with no edits on
 * screen. Touches the disk - not for the EDT.
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

    /** Where the working copy is - for the reconcile's message naming BindForge's side. */
    @FunctionalInterface
    interface WorkingCopyFolder {
        Path get() throws IOException;
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
     * @param differences the rows to ask about - every one that differs, but those the edits change
     * @param edited      how many rows that differ are not asked about because the edits change them - at APPLY, where
     *                    the edits go on top (Storage S8b); none at the first click, which has no edits
     * @param devices     every reachable installation's device files, judged (Storage S9a, S9b)
     */
    public record Found(ConfigurationFiles workingCopy, Map<String, byte[]> live, Differences differences, int edited,
                        DeviceCheck.Found devices) {
        public Found {
            Objects.requireNonNull(workingCopy, "workingCopy");
            live = Map.copyOf(live);
            Objects.requireNonNull(differences, "differences");
            Objects.requireNonNull(devices, "devices");
        }

        /** Whether nothing differs - no binds or preset file, and no installation. */
        public boolean none() {
            return differences.none() && devices.none();
        }

        /** Whether there is anything to ask: a row, or an installation the working copy can be put back to. */
        public boolean anythingToAsk() {
            return !differences.none() || !devices.asked().isEmpty();
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
    private final WorkingCopyFolder workingCopyFolder;
    private final LiveReader liveReader;
    private final FactorySource factory;
    private final Writer writer;
    private final BackUp backUp;
    private final Apply.Reader deviceFiles;
    private final DeviceCheck.Source devices;

    /**
     * The shared edits, the bindings folder Elite-Intel is set to, the working copy's folder, the backup folder, the
     * write path and the stored installations.
     */
    public static BindingsCheck stored() {
        return new BindingsCheck(Edits.stored(), () -> PlayerSession.getInstance().getBindingsDir(),
                AppPaths::getWorkingCopyDir,
                BindingsCheck::readBindingsFolder, BindingsCheck::factoryOf,
                (files, backUp, leaveAlone, remove) -> WritePath.stored().write(files, backUp, leaveAlone, remove),
                (sources, workingCopy) -> Backups.stored().backUpBeforeChange(Backups.Before.CHECK, sources, workingCopy),
                installation -> DeviceFiles.read(installation.root()), DeviceCheck.stored());
    }

    BindingsCheck(Edits edits, Supplier<Path> bindingsFolder, WorkingCopyFolder workingCopyFolder, LiveReader liveReader,
                  FactorySource factory, Writer writer, BackUp backUp, Apply.Reader deviceFiles,
                  DeviceCheck.Source devices) {
        this.edits = edits;
        this.bindingsFolder = bindingsFolder;
        this.workingCopyFolder = workingCopyFolder;
        this.liveReader = liveReader;
        this.factory = factory;
        this.writer = writer;
        this.backUp = backUp;
        this.deviceFiles = deviceFiles;
        this.devices = devices;
    }

    /**
     * Compares the bindings folder with the working copy, leaving out the rows the edits on screen change - at APPLY
     * the edits go on top of whichever side is kept, so nobody is asked about them (Alan, 2026-10-30, Storage S8b).
     * The device files are judged too - every reachable installation's, against the working copy - at the first click
     * (Storage S9a) and at APPLY (Storage S9b). Reads only.
     *
     * @throws NoSuchFileException if there is no working copy, or no bindings folder - nothing can be compared
     * @throws IOException         if either cannot be read, or the working copy's own device files cannot be parsed. An
     *                             installation that cannot be read is judged so, not thrown
     */
    public Found find() throws IOException {
        return find(edits.snapshot(), liveReader.read(bindingsFolder.get()));
    }

    private Found find(Edits.Snapshot snapshot, Map<String, byte[]> live) throws IOException {
        ConfigurationFiles workingCopy = snapshot.applied();
        Differences all = BindingsComparison.compare(workingCopy.bindings(), live, Factory.NONE);
        // WHY: the factory binds files are read only when something differs - most first clicks find nothing.
        if (!all.none()) {
            all = BindingsComparison.compare(workingCopy.bindings(), live, factory.of(workingCopy));
        }
        if (!all.leftOut().isEmpty()) {
            log.warn("The check left as they are a newer version it cannot read line by line, and the file it replaces: "
                    + "{}", String.join(", ", all.leftOut()));
        }
        all.newer().forEach(version -> log.info("The game has a newer version of {}: {}, {} new slots, {} lines that "
                + "differ", version.older(), version.newer(), version.newSlots().size(), version.items().size()));
        Differences asked = BindingsComparison.without(all, edited(all, snapshot));
        DeviceCheck.Found judged = devices.find(workingCopy);
        return new Found(workingCopy, live, asked, all.rows().size() - asked.rows().size(), judged);
    }

    private static List<BindingsComparison.LineItem> edited(Differences found, Edits.Snapshot snapshot) {
        return BindingsComparison.editedAtTheCheck(found, snapshot.applied().bindings(), snapshot.current().bindings());
    }

    /**
     * The binds and preset files the user settled on - after reading the bindings folder again, since nothing watched
     * it while the warning was open. Run inside the write's {@link Edits#whileWriting}: the check's own at the first
     * click, APPLY's at APPLY (Storage S8b).
     *
     * @param snapshot the edits and the working copy, read inside the write
     * @param found    what the warning showed - nothing, when it showed none
     * @param answers  the side each of its rows keeps
     * @throws ChangedMeanwhileException if a row, the working copy, the edits or an installation's device files changed
     *                                   since {@code found} - nothing is written
     * @throws IOException               if the bindings folder cannot be read, or a merge cannot be made
     */
    BindingsComparison.Settled settled(Edits.Snapshot snapshot, Found found, Map<String, Side> answers)
            throws IOException {
        Map<String, byte[]> live = liveReader.read(bindingsFolder.get());
        Differences compared = BindingsComparison.compare(snapshot.applied().bindings(), live, Factory.NONE);
        Differences now = BindingsComparison.without(compared, edited(compared, snapshot));
        if (!snapshot.applied().sameAs(found.workingCopy()) || !now.sameRows(found.differences())
                || !devices.find(snapshot.applied()).sameAs(found.devices())) {
            throw new ChangedMeanwhileException(find(snapshot, live));
        }
        return BindingsComparison.settle(snapshot.applied().bindings(), live, now, answers);
    }

    /**
     * The device files' reconcile, as first setup's reading (storage.md rule 14), once the user has answered the binds
     * rows (Storage S9b, from the S9a review): BindForge's side is the working copy with the edits on top - at APPLY
     * the edits are the user's side, decided in the reconcile like any other difference (Alan, 2026-10-31) - and the
     * binds files are those the write would hold: the rows settled by {@code answers}, the edits on top. So a device
     * only the game's side uses ranks red, and <em>use one installation for all</em> counts its bindings. Reads the
     * edits and parses the device files - not for the EDT.
     *
     * @param answers  the side each binds row keeps - every row answered, or none when there are none
     * @param mineName BindForge's side's name on screen, in the user's language
     * @return empty when no installation's files can be a side - only a missing or unparseable file, for which putting
     *         the working copy back is all there is
     * @throws IOException if a merge cannot be made, or a side's files cannot be read for the plan - the message names
     *                     it
     */
    public Optional<FirstSetupReading> reading(Found found, Map<String, Side> answers, String mineName)
            throws IOException {
        if (found.devices().sides().isEmpty()) return Optional.empty();
        Edits.Snapshot snapshot = edits.snapshot();
        BindingsComparison.Settled settled = BindingsComparison.settle(found.workingCopy().bindings(), found.live(),
                found.differences(), answers);
        Map<String, byte[]> bindings = BindingsComparison.withEditsOnTop(settled.bindings(), settled.renamed(),
                snapshot.applied().bindings(), snapshot.current().bindings());
        return Optional.of(found.devices().reading(snapshot.current(), bindings, bindingsFolder.get(),
                workingCopyFolder.get(), mineName));
    }

    /**
     * Writes what the user settled on: backs up the live copies and the working copy, saves the settled files as the
     * working copy, and writes them to the bindings folder - removing each live-only file the user kept the working
     * copy's side of - and the device files settled on to every installation the check read (Storage S9a). One it
     * could not read at all is left alone, as is one it never read whose device files differ (storage.md rule 1).
     *
     * @param found   what the warning showed
     * @param answers the side each of its rows keeps, by {@link BindingsComparison.LineItem#id()}
     * @param devices the device files settled on - {@link DeviceCheck.Answer#PUT_MINE_BACK} when no installation was
     *                asked about, or the user put the working copy back
     * @throws ChangedMeanwhileException if a row, the working copy or an installation changed since {@code found} -
     *                                   nothing written
     * @throws EditsNotAppliedException  if there are edits not applied - nothing written
     * @throws Edits.WriteRunningException if another write is running
     * @throws IOException               if the files cannot be read again, a merge cannot be made, the backup cannot
     *                                   be taken or the working copy cannot be saved - no live copy was written
     */
    public Written settle(Found found, Map<String, Side> answers, DeviceCheck.Answer devices) throws IOException {
        return edits.whileWriting(() -> {
            // WHY: the write hands its set to the edits, which would drop them unseen.
            if (edits.hasEdits()) throw new EditsNotAppliedException();
            Edits.Snapshot snapshot = edits.snapshot();
            ConfigurationFiles workingCopy = snapshot.applied();
            BindingsComparison.Settled settled = settled(snapshot, found, answers);
            ConfigurationFiles files = DeviceCheck.withDevices(workingCopy.withBindings(settled.bindings()), devices);
            DeviceFilesComparison comparison = DeviceFilesComparison.of(workingCopy);
            AtomicReference<Backup> taken = new AtomicReference<>();
            WriteReport report = writer.write(files, sources -> {
                Backup backup = backUp.take(sources, workingCopy);
                taken.set(backup);
                return backup;
            }, found.devices().leavingAlone(comparison, deviceFiles), settled.removeFromLive());
            log.info("The check settled {} files: {} rows; newer versions taken: {}; removed from the bindings folder: "
                            + "{}; installations written that differed: {}, their device files {}",
                    found.differences().files().size(), found.differences().rows().size(), settled.renamed(),
                    settled.removeFromLive(), found.devices().asked().size(),
                    switch (devices) {
                        case DeviceCheck.PutMineBack putBack -> "the working copy's";
                        case DeviceCheck.Reconciled reconciled -> "reconciled";
                    });
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
