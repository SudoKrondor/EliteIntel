package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.preseteditor.BindNameCatalog;
import elite.intel.bindforge.preseteditor.BindNameList;
import elite.intel.bindforge.storage.BindingsComparison.Side;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.managers.BindForgeInstallationsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * APPLY - the one above BindForge's tabs (storage.md rules 7-8; overview.md, <em>APPLY and DISCARD</em>): back up,
 * save the edits into the working copy, and write the working copy to every live copy, through {@link WritePath}, the
 * one write path.
 * <p>
 * <strong>The default labels are worked out here, as APPLY runs</strong> ({@link DefaultLabels}; Alan, 2026-10-27), from
 * the edits and the controllers attached now - not pushed earlier by a screen, which could leave them stale after a
 * rename or a controller plugged in while the screen was not showing.
 * <p>
 * <strong>An installation whose device files differ from the working copy is left alone</strong> and reported, until it
 * is reconciled (Alan, 2026-10-26; storage.md, <em>How the files are written</em>): compared by value
 * ({@link DeviceFilesComparison}) against the working copy as it was before the edits went on top.
 * <p>
 * <strong>The preset file is checked first</strong>, before anything is backed up or written (preset-editor.md,
 * <em>What APPLY checks and writes for the preset file</em>; Storage S6, 2026-10-28): every one of its four lines must
 * name a binds file the BindName list offers, else APPLY refuses as a whole ({@link PresetRefusedException}). It runs on
 * every APPLY, whatever the edits are, since a preset file that names nothing real fails silently in the game. The
 * factory binds files come only from the installations that are reachable ({@link BackupSources.Installation#reachable});
 * when one is not, the refusal names it, since its files may be what a line names (Storage S6R, 2026-10-29).
 * <p>
 * <strong>Every APPLY runs the check</strong> for the binds and preset files (storage.md rule 14, <em>At APPLY</em>;
 * Storage S8b): {@link #check()} after the confirmation, then {@link #apply} with what the user settled on. The check
 * settles the working copy first and the edits go on top - an edit wins on every line item it changed - in one backup
 * and one write. The rows the edits change are never asked about. Device files are Storage S9's.
 * Publishes no {@code BindingsUpdatedEvent} - {@code BindingsMonitor} sees the bindings folder change. Touches the disk
 * - not for the EDT.
 */
public final class Apply {

    private static final Logger log = LogManager.getLogger(Apply.class);

    /**
     * Writes a set through the write path, leaving alone the installations it is told to and removing from the bindings
     * folder the files named - a live-only file the check kept the working copy's side of.
     */
    @FunctionalInterface
    interface Writer {
        WriteReport write(ConfigurationFiles files, WritePath.BackUpFirst backUp, LiveCopies.LeaveAlone leaveAlone,
                          Set<String> removeFromBindings) throws IOException;
    }

    /** APPLY's before-changes backup. */
    @FunctionalInterface
    interface BackUp {
        /**
         * @param sources     the bindings folder and installations the write is about to write
         * @param workingCopy the working copy, to hold as well when the check settled something (Alan, 2026-10-30,
         *                    Storage S8b) - {@code null} when it settled nothing
         */
        Backup take(BackupSources sources, ConfigurationFiles workingCopy) throws IOException;
    }

    /** Reads one installation's device files as they are now. */
    @FunctionalInterface
    interface Reader {
        DeviceFiles read(BackupSources.Installation installation) throws IOException;
    }

    /**
     * The default labels (alias-designer.md, <em>Default labels</em>): a {@code .buttonMap} for every connected, named
     * controller that has none, by file name. Not the user's edits, so they never light the bar.
     */
    @FunctionalInterface
    public interface DefaultLabels {
        /**
         * @param current the edits APPLY is about to write
         * @param applied the working copy they started from
         */
        Map<String, byte[]> of(ConfigurationFiles current, ConfigurationFiles applied) throws IOException;
    }

    /**
     * The BindNames a line of the preset file can hold, for a set: its own binds files, and the factory ones of every
     * reachable installation (preset-editor.md, <em>The BindName list</em>).
     */
    @FunctionalInterface
    interface BindNames {
        Offered of(ConfigurationFiles files) throws IOException;
    }

    /**
     * The BindName list, and the installations whose factory binds files are not in it because they could not be
     * reached.
     */
    record Offered(BindNameList list, List<InstallationRow> unreachable) {
        Offered {
            unreachable = List.copyOf(unreachable);
        }
    }

    private final Edits edits;
    private final DefaultLabels defaults;
    private final Writer writer;
    private final BackUp backUp;
    private final Reader reader;
    private final BindNames bindNames;
    private final BindingsCheck check;

    /**
     * The shared edits, the write path, the backup folder, the installations' files on disk and the check.
     *
     * @param defaults works out the default labels as APPLY runs
     */
    public static Apply stored(DefaultLabels defaults) {
        return new Apply(Edits.stored(), defaults,
                (files, backUp, leaveAlone, remove) -> WritePath.stored().write(files, backUp, leaveAlone, remove),
                (sources, workingCopy) -> workingCopy == null
                        ? Backups.stored().backUpBeforeChange(Backups.Before.APPLY, sources)
                        : Backups.stored().backUpBeforeApply(sources, workingCopy),
                installation -> DeviceFiles.read(installation.root()),
                files -> offered(files, BindForgeInstallationsManager.getInstance().findAll()),
                BindingsCheck.stored());
    }

    /**
     * The BindName list for {@code files}: its binds files, and the factory ones of every installation in {@code rows}
     * that is reachable - the one test (storage.md, <em>Settled from Krondor's review</em>). The others are returned as
     * unreachable. Touches the disk.
     */
    static Offered offered(ConfigurationFiles files, List<InstallationRow> rows) {
        List<Path> controlSchemes = new ArrayList<>();
        List<InstallationRow> unreachable = new ArrayList<>();
        for (InstallationRow row : rows) {
            if (BackupSources.reachable(row)) {
                controlSchemes.add(GameInstallation.controlSchemesUnder(Path.of(row.rootPath())));
            } else {
                unreachable.add(row);
            }
        }
        return new Offered(BindNameCatalog.list(PresetEdits.bindsFiles(files), controlSchemes), unreachable);
    }

    Apply(Edits edits, DefaultLabels defaults, Writer writer, BackUp backUp, Reader reader, BindNames bindNames,
          BindingsCheck check) {
        this.edits = edits;
        this.defaults = defaults;
        this.writer = writer;
        this.backUp = backUp;
        this.reader = reader;
        this.bindNames = bindNames;
        this.check = check;
    }

    /**
     * APPLY's first step, once the user has confirmed: the preset file the edits hold, then the check - the game's binds
     * and preset files compared with the working copy, leaving out the rows the edits change (Alan, 2026-10-30, Storage
     * S8b). Reads only; the user then answers what it found, if anything, and {@link #apply} writes.
     *
     * @throws PresetRefusedException if a line of the preset file names no binds file that can be found - before the
     *                                 warning, so no merge is answered for nothing
     * @throws IOException             if the game's files or the working copy cannot be read - APPLY stops, nothing
     *                                 written, since an unread file could hold the user's work
     */
    public BindingsCheck.Found check() throws IOException {
        checkPresetFile(edits.snapshot().current());
        return check.find();
    }

    /**
     * Writes the edits on top of what the check settled on, with no change let in while it runs. Afterwards the edits
     * are the working copy, and the bar goes dark.
     *
     * @param found   what {@link #check()} found
     * @param answers the side each of its rows keeps - none when it found nothing
     * @return the report and the before-APPLY backup
     * @throws BindingsCheck.ChangedMeanwhileException if the game's files changed since {@code found} - nothing was
     *                                                 written; it carries what is there now
     * @throws PresetRefusedException                  if a line of the preset file APPLY would write names no binds
     *                                                 file that can be found - nothing was backed up or written, and
     *                                                 the edits are still on screen. It names the installations that
     *                                                 could not be reached, if any
     * @throws IOException                             if the game's files or the working copy's own device files cannot
     *                                                 be read, a merge cannot be made, the backup cannot be taken, or
     *                                                 the working copy cannot be saved - no live copy was written, and
     *                                                 the edits are still on screen
     */
    public Written apply(BindingsCheck.Found found, Map<String, Side> answers) throws IOException {
        return edits.whileWriting(() -> {
            Edits.Snapshot snapshot = edits.snapshot();
            BindingsComparison.Settled settled = check.settled(snapshot, found, answers);
            boolean checkSettled = !found.differences().none();
            // WHY: with nothing settled the edits go out byte for byte, as they always have.
            ConfigurationFiles onTop = checkSettled
                    ? snapshot.current().withBindings(BindingsComparison.withEditsOnTop(settled.bindings(),
                    snapshot.applied().bindings(), snapshot.current().bindings()))
                    : snapshot.current();
            // WHY: again, on the set written - a merge can take the game's preset line.
            checkPresetFile(onTop);
            // WHY: before anything is backed up or written. The working copy is BindForge's own, so one it cannot
            // parse is a broken invariant - not a user file to protect by leaving every installation alone.
            DeviceFilesComparison comparison = DeviceFilesComparison.of(snapshot.applied());
            ConfigurationFiles files = withDefaults(onTop, defaults.of(onTop, snapshot.applied()));
            Set<String> remove = new TreeSet<>(settled.removeFromLive());
            // WHY: a file the edits hold is written, never removed - the edit wins.
            remove.removeAll(files.bindings().keySet());
            AtomicReference<Backup> taken = new AtomicReference<>();
            WriteReport report = writer.write(files, sources -> {
                Backup backup = backUp.take(sources, checkSettled ? snapshot.applied() : null);
                taken.set(backup);
                return backup;
            }, leavingAloneWhereDifferent(comparison, reader), remove);
            log.info("Applied: {} binds and preset files, {} button maps; the check settled {} rows; installations "
                            + "updated {} of {}", files.bindings().size(), files.buttonMaps().size(),
                    found.differences().items().size(), report.installationsUpdated(), report.installations().size());
            return new Written(report, taken.get());
        });
    }

    /**
     * {@link PresetEdits#check}, naming the installations that could not be reached when it refuses - only when a
     * refused line names something, since no drive explains an empty line (Storage S6R2, the S4R-S6R review, D2).
     */
    private void checkPresetFile(ConfigurationFiles current) throws IOException {
        Offered offered = bindNames.of(current);
        try {
            PresetEdits.check(current, offered.list());
        } catch (PresetRefusedException e) {
            if (offered.unreachable().isEmpty() || everyRefusedLineIsEmpty(current, e)) throw e;
            throw e.withUnreachable(offered.unreachable());
        }
    }

    private static boolean everyRefusedLineIsEmpty(ConfigurationFiles current, PresetRefusedException refused) {
        Optional<StartPresetSections> lines = PresetEdits.sections(current);
        return lines.isEmpty() || refused.sections().stream()
                .allMatch(section -> lines.get().forSection(section).isEmpty());
    }

    /** The edits, with the default labels for every device that has no {@code .buttonMap} - one in any spelling wins. */
    private static ConfigurationFiles withDefaults(ConfigurationFiles files, Map<String, byte[]> defaultLabels) {
        Map<String, byte[]> maps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        maps.putAll(files.buttonMaps());
        defaultLabels.forEach(maps::putIfAbsent);
        return files.withButtonMaps(new TreeMap<>(maps));
    }

    /**
     * Leaves alone each installation whose device files differ from the working copy - APPLY's, and the check's, until
     * Storage S9 reconciles them (Alan, 2026-10-26).
     */
    static LiveCopies.LeaveAlone leavingAloneWhereDifferent(DeviceFilesComparison comparison, Reader reader) {
        return installation -> differs(comparison, reader, installation);
    }

    private static boolean differs(DeviceFilesComparison comparison, Reader reader,
                                   BackupSources.Installation installation) {
        try {
            return comparison.differs(reader.read(installation));
        } catch (IOException e) {
            // WHY: a file there that cannot be read or parsed could hold anything of the user's - left alone, as one
            // that differs, never written over unseen.
            log.warn("Could not read {} ({}) to compare it with the working copy, so it is left alone",
                    installation.storefront(), installation.root(), e);
            return true;
        }
    }
}
