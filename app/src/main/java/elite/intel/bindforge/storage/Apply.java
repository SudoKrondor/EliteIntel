package elite.intel.bindforge.storage;

import elite.intel.bindforge.preseteditor.BindNameCatalog;
import elite.intel.bindforge.preseteditor.BindNameList;
import elite.intel.db.managers.BindForgeInstallationsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
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
 * every APPLY, whatever the edits are, since a preset file that names nothing real fails silently in the game.
 * <p>
 * <em>Until Storage S8 APPLY runs without the check</em> (Alan, 2026-10-24); the backup before it is the safety net.
 * Publishes no {@code BindingsUpdatedEvent} - {@code BindingsMonitor} sees the bindings folder change. Touches the disk
 * - not for the EDT.
 */
public final class Apply {

    private static final Logger log = LogManager.getLogger(Apply.class);

    /** Writes a set through the write path, leaving alone the installations it is told to. */
    @FunctionalInterface
    interface Writer {
        WriteReport write(ConfigurationFiles files, WritePath.BackUpFirst backUp, LiveCopies.LeaveAlone leaveAlone)
                throws IOException;
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
     * installation (preset-editor.md, <em>The BindName list</em>).
     */
    @FunctionalInterface
    interface BindNames {
        BindNameList of(ConfigurationFiles files) throws IOException;
    }

    private final Edits edits;
    private final DefaultLabels defaults;
    private final Writer writer;
    private final WritePath.BackUpFirst beforeApply;
    private final Reader reader;
    private final BindNames bindNames;

    /**
     * The shared edits, the write path, the backup folder, and the installations' files on disk.
     *
     * @param defaults works out the default labels as APPLY runs
     */
    public static Apply stored(DefaultLabels defaults) {
        return new Apply(Edits.stored(), defaults,
                (files, backUp, leaveAlone) -> WritePath.stored().write(files, backUp, leaveAlone),
                sources -> Backups.stored().backUpBeforeChange(Backups.Before.APPLY, sources),
                installation -> DeviceFiles.read(installation.root()),
                files -> BindNameCatalog.list(PresetEdits.bindsFiles(files),
                        BindNameCatalog.controlSchemesOf(BindForgeInstallationsManager.getInstance().findAll())));
    }

    Apply(Edits edits, DefaultLabels defaults, Writer writer, WritePath.BackUpFirst beforeApply, Reader reader,
          BindNames bindNames) {
        this.edits = edits;
        this.defaults = defaults;
        this.writer = writer;
        this.beforeApply = beforeApply;
        this.reader = reader;
        this.bindNames = bindNames;
    }

    /**
     * Writes the edits, with no change let in while it runs. Afterwards the edits are the working copy, and the bar
     * goes dark.
     *
     * @return the report and the before-APPLY backup
     * @throws PresetRefusedException if a line of the preset file names no binds file that can be found - nothing was
     *                                 backed up or written, and the edits are still on screen
     * @throws IOException             if the working copy's own device files cannot be read, the backup cannot be
     *                                 taken, or the working copy cannot be saved - no live copy was written, and the
     *                                 edits are still on screen
     */
    public Written apply() throws IOException {
        return edits.whileWriting(() -> {
            Edits.Snapshot snapshot = edits.snapshot();
            PresetEdits.check(snapshot.current(), bindNames.of(snapshot.current()));
            // WHY: before anything is backed up or written. The working copy is BindForge's own, so one it cannot
            // parse is a broken invariant - not a user file to protect by leaving every installation alone.
            DeviceFilesComparison comparison = DeviceFilesComparison.of(snapshot.applied());
            ConfigurationFiles files = withDefaults(snapshot.current(),
                    defaults.of(snapshot.current(), snapshot.applied()));
            AtomicReference<Backup> taken = new AtomicReference<>();
            WriteReport report = writer.write(files, sources -> {
                Backup backup = beforeApply.take(sources);
                taken.set(backup);
                return backup;
            }, installation -> differs(comparison, installation));
            log.info("Applied: {} binds and preset files, {} button maps; installations updated {} of {}",
                    files.bindings().size(), files.buttonMaps().size(), report.installationsUpdated(),
                    report.installations().size());
            return new Written(report, taken.get());
        });
    }

    /** The edits, with the default labels for every device that has no {@code .buttonMap} - one in any spelling wins. */
    private static ConfigurationFiles withDefaults(ConfigurationFiles files, Map<String, byte[]> defaultLabels) {
        Map<String, byte[]> maps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        maps.putAll(files.buttonMaps());
        defaultLabels.forEach(maps::putIfAbsent);
        return files.withButtonMaps(new TreeMap<>(maps));
    }

    private boolean differs(DeviceFilesComparison comparison, BackupSources.Installation installation) {
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
