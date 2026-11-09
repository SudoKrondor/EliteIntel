package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.FileNames;
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
 * <strong>What does not belong in the bindings folder is moved to Leftover User Files in the same write</strong>
 * (Storage S13d; {@link Leftovers}): found here, compared again before the write as a row is, a plain binds file judged
 * against the set the write would hold first ({@link PlainBinds}) - and converted into a binds file of the set when it
 * is neither a twin nor a factory file. <strong>A plain file holding lines its twin lacks</strong> (Storage S13e) gives
 * rows of the warning ({@link Found#plainRows}), answered in the same map: each kept in-game goes into the twin.
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

    /** Reads what does not belong in a bindings folder (Storage S13d). */
    @FunctionalInterface
    interface LeftoverReader {
        Leftovers.Found read(Path folder) throws IOException;
    }

    /** Frontier's factory binds files by BindName, for a set - what a plain file is judged against (Storage S13b). */
    @FunctionalInterface
    interface PlainFactory {
        Map<String, byte[]> of(ConfigurationFiles files) throws IOException;
    }

    /** The check's write. */
    @FunctionalInterface
    interface Writer {
        WriteReport write(ConfigurationFiles files, WritePath.BackUpFirst backUp, LiveCopies.LeaveAlone leaveAlone,
                          Set<String> removeFromBindings, Set<String> moveFromBindings) throws IOException;
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
     * @param leftovers   what does not belong in the bindings folder, to move to Leftover User Files (Storage S13d)
     * @param plainRows   the lines each plain file holds and its twin lacks, asked as rows of the warning (Storage S13e)
     */
    public record Found(ConfigurationFiles workingCopy, Map<String, byte[]> live, Differences differences, int edited,
                        DeviceCheck.Found devices, Leftovers.Found leftovers, List<PlainBinds.Rows> plainRows) {
        public Found {
            Objects.requireNonNull(workingCopy, "workingCopy");
            live = Map.copyOf(live);
            Objects.requireNonNull(differences, "differences");
            Objects.requireNonNull(devices, "devices");
            Objects.requireNonNull(leftovers, "leftovers");
            plainRows = List.copyOf(plainRows);
        }

        /** The warning's rows, file by file - the binds and preset files', then the plain files' (Storage S13e). */
        public List<BindingsComparison.LineItem> items() {
            List<BindingsComparison.LineItem> items = new ArrayList<>(differences.items());
            plainRows.forEach(plain -> items.addAll(plain.rows().items()));
            return items;
        }

        /** Every row to answer: the warning's, then each newer version's. */
        public List<BindingsComparison.LineItem> rows() {
            List<BindingsComparison.LineItem> rows = new ArrayList<>(items());
            differences.newer().forEach(version -> rows.addAll(version.items()));
            return rows;
        }

        /** Whether the warning has a row to answer - a binds or preset file's, or a plain file's. */
        public boolean anyRows() {
            return !differences.files().isEmpty() || !plainRows.isEmpty();
        }

        /** Whether nothing differs - no binds or preset file, no installation, and nothing to move. */
        public boolean none() {
            return differences.none() && devices.none() && leftovers.none();
        }

        /**
         * Whether there is anything to ask: a row, an installation the working copy can be put back to, or a file to
         * move to Leftover User Files.
         */
        public boolean anythingToAsk() {
            return !differences.none() || !devices.asked().isEmpty() || !leftovers.none();
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
    private final LeftoverReader leftoverReader;
    private final PlainFactory plainFactory;

    /**
     * The shared edits, the bindings folder Elite-Intel is set to, the working copy's folder, the backup folder, the
     * write path and the stored installations.
     */
    public static BindingsCheck stored() {
        return new BindingsCheck(Edits.stored(), () -> PlayerSession.getInstance().getBindingsDir(),
                AppPaths::getWorkingCopyDir,
                BindingsCheck::readBindingsFolder, BindingsCheck::factoryOf,
                (files, backUp, leaveAlone, remove, move) -> WritePath.stored().write(files, backUp, leaveAlone, remove,
                        move),
                (sources, workingCopy) -> Backups.stored().backUpBeforeChange(Backups.Before.CHECK, sources, workingCopy),
                installation -> DeviceFiles.read(installation.root()), DeviceCheck.stored(),
                Leftovers::read, BindingsCheck::factoryByName);
    }

    BindingsCheck(Edits edits, Supplier<Path> bindingsFolder, WorkingCopyFolder workingCopyFolder, LiveReader liveReader,
                  FactorySource factory, Writer writer, BackUp backUp, Apply.Reader deviceFiles,
                  DeviceCheck.Source devices, LeftoverReader leftoverReader, PlainFactory plainFactory) {
        this.edits = edits;
        this.bindingsFolder = bindingsFolder;
        this.workingCopyFolder = workingCopyFolder;
        this.liveReader = liveReader;
        this.factory = factory;
        this.writer = writer;
        this.backUp = backUp;
        this.deviceFiles = deviceFiles;
        this.devices = devices;
        this.leftoverReader = leftoverReader;
        this.plainFactory = plainFactory;
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
        Path folder = bindingsFolder.get();
        return find(edits.snapshot(), liveReader.read(folder), leftoverReader.read(folder));
    }

    private Found find(Edits.Snapshot snapshot, Map<String, byte[]> live, Leftovers.Found leftovers)
            throws IOException {
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
        if (!leftovers.none()) {
            log.info("The bindings folder holds files that do not belong there: {}; of them plain binds files to judge: "
                    + "{}", String.join(", ", leftovers.files()), leftovers.plain().keySet());
        }
        List<PlainBinds.Rows> plainRows = plainRows(snapshot, live, leftovers);
        plainRows.forEach(plain -> log.info("The plain binds file {} holds {} lines {} lacks", plain.plain(),
                plain.rows().items().size(), plain.twin()));
        return new Found(workingCopy, live, asked, all.rows().size() - asked.rows().size(), judged, leftovers,
                plainRows);
    }

    /**
     * The lines each plain file holds and its twin lacks (Storage S13e, Q4): the twin as the edits hold it - the working
     * copy's, with the edits on top at APPLY - else, when only the bindings folder holds it, the live one.
     */
    private static List<PlainBinds.Rows> plainRows(Edits.Snapshot snapshot, Map<String, byte[]> live,
                                                   Leftovers.Found leftovers) {
        if (leftovers.plain().isEmpty()) return List.of();
        Map<String, byte[]> twins = new TreeMap<>(FileNames.ONE_FILE);
        twins.putAll(live);
        // WHY: the edits' spelling and bytes win over the bindings folder's.
        snapshot.current().bindings().forEach((name, content) -> {
            twins.remove(name);
            twins.put(name, content);
        });
        return PlainBinds.rows(leftovers.plain(), twins);
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
     * @throws ChangedMeanwhileException if a row, the working copy, the edits, an installation's device files or the
     *                                   files to move changed since {@code found} - nothing is written
     * @throws IOException               if the bindings folder cannot be read, or a merge cannot be made
     */
    BindingsComparison.Settled settled(Edits.Snapshot snapshot, Found found, Map<String, Side> answers)
            throws IOException {
        Path folder = bindingsFolder.get();
        Map<String, byte[]> live = liveReader.read(folder);
        Leftovers.Found leftovers = leftoverReader.read(folder);
        Differences compared = BindingsComparison.compare(snapshot.applied().bindings(), live, Factory.NONE);
        Differences now = BindingsComparison.without(compared, edited(compared, snapshot));
        if (!snapshot.applied().sameAs(found.workingCopy()) || !now.sameRows(found.differences())
                || !devices.find(snapshot.applied()).sameAs(found.devices())
                || !leftovers.sameAs(found.leftovers())
                || !plainRows(snapshot, live, leftovers).equals(found.plainRows())) {
            throw new ChangedMeanwhileException(find(snapshot, live, leftovers));
        }
        return BindingsComparison.settle(snapshot.applied().bindings(), live, now, answers);
    }

    /**
     * What the write does with what {@code found} holds that does not belong in the bindings folder (Storage S13d): each
     * plain binds file judged against {@code settled} - the set the write would hold, the check's settled files at the
     * first click, with the edits on top at APPLY (Storage S13b) - and everything moved to Leftover User Files. Lists the
     * factory files only when there is a plain file to judge.
     *
     * @param workingCopy the set the factory files are listed for
     * @param answers     the side each row keeps - a plain file's rows against its twin among them (Storage S13e)
     * @throws IOException if the factory files cannot be listed, or a twin merged into could not be read back
     */
    Leftovers.Plan leftoversPlan(Found found, ConfigurationFiles workingCopy, Map<String, byte[]> settled,
                                 Map<String, Side> answers) throws IOException {
        if (found.leftovers().none()) return Leftovers.Plan.NONE;
        Map<String, byte[]> factory = found.leftovers().plain().isEmpty() ? Map.of() : plainFactory.of(workingCopy);
        return Leftovers.plan(found.leftovers(), settled, factory, answers);
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
     * copy's side of, and moving what does not belong there to Leftover User Files (Storage S13d) - and the device
     * files settled on to every installation the check read (Storage S9a). One it could not read at all is left alone,
     * as is one it never read whose device files differ (storage.md rule 1).
     *
     * @param found   what the warning showed
     * @param answers the side each of its rows keeps, by {@link BindingsComparison.LineItem#id()}
     * @param devices the device files settled on - {@link DeviceCheck.Answer#PUT_MINE_BACK} when no installation was
     *                asked about, or the user put the working copy back
     * @throws ChangedMeanwhileException if a row, the working copy or an installation changed since {@code found} -
     *                                   nothing written
     * @throws EditsNotAppliedException  if there are edits not applied - nothing written
     * @throws Edits.RefusedException    if another write is running, or BindForge is read only - nothing written
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
            // WHY: before the device files, so a reconcile's moves reach a converted file too (Storage S13b).
            Leftovers.Plan leftovers = leftoversPlan(found, workingCopy, settled.bindings(), answers);
            ConfigurationFiles files = DeviceCheck.withDevices(
                    workingCopy.withBindings(leftovers.plain().appliedTo(settled.bindings())), devices);
            Set<String> remove = new TreeSet<>(settled.removeFromLive());
            Set<String> move = leftovers.move();
            DeviceFilesComparison comparison = DeviceFilesComparison.of(workingCopy);
            AtomicReference<Backup> taken = new AtomicReference<>();
            WriteReport report = writer.write(files, sources -> {
                Backup backup = backUp.take(sources, workingCopy);
                taken.set(backup);
                return backup;
            }, found.devices().leavingAlone(comparison, deviceFiles), remove, move);
            Leftovers.log(leftovers);
            log.info("The check settled {} files: {} rows; newer versions taken: {}; removed from the bindings folder: "
                            + "{}; moved to Leftover User Files: {}; installations written that differed: {}, their "
                            + "device files {}",
                    found.differences().files().size(), found.differences().rows().size(), settled.renamed(),
                    remove, report.bindings().moved(), found.devices().asked().size(),
                    switch (devices) {
                        case DeviceCheck.PutMineBack putBack -> "the working copy's";
                        case DeviceCheck.Reconciled reconciled -> "reconciled";
                    });
            return new Written(report, taken.get(), leftovers);
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

    /**
     * The factory binds files of every reachable installation, by BindName - the first readable file of each, since every
     * installation runs one game version (Storage S11, Q2). One that cannot be read is logged and left out: a plain file
     * of its name is then converted, which keeps every byte.
     */
    private static Map<String, byte[]> factoryByName(ConfigurationFiles files) {
        Apply.Offered offered = Apply.offered(files, BindForgeInstallationsManager.getInstance().findAll());
        Map<String, byte[]> byName = new TreeMap<>(FileNames.ONE_FILE);
        for (BindNameEntry entry : offered.list().entries()) {
            if (entry.origin() != BindsFileOrigin.FACTORY) continue;
            for (Path file : entry.files()) {
                try {
                    byName.putIfAbsent(entry.bindName(), Files.readAllBytes(file));
                    break;
                } catch (IOException e) {
                    log.warn("Could not read the factory binds file {} to judge a plain binds file", file, e);
                }
            }
        }
        return byName;
    }
}
