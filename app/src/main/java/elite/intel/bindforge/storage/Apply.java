package elite.intel.bindforge.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * APPLY - the one above BindForge's tabs (storage.md rules 7-8; overview.md, <em>APPLY and DISCARD</em>): back up,
 * save the edits into the working copy, and write the working copy to every live copy, through {@link WritePath}, the
 * one write path. The default labels go with it ({@link Edits#toApply()}).
 * <p>
 * <strong>An installation whose device files differ from the working copy is left alone</strong> and reported, until it
 * is reconciled (Alan, 2026-10-26; storage.md, <em>How the files are written</em>): compared by value
 * ({@link DeviceFilesComparison}) against the working copy as it was before the edits went on top.
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
        InstallationFiles read(BackupSources.Installation installation) throws IOException;
    }

    private final Edits edits;
    private final Writer writer;
    private final WritePath.BackUpFirst beforeApply;
    private final Reader reader;

    /** The shared edits, the write path, the backup folder, and the installations' files on disk. */
    public static Apply stored() {
        return new Apply(Edits.stored(),
                (files, backUp, leaveAlone) -> WritePath.stored().write(files, backUp, leaveAlone),
                sources -> Backups.stored().backUpBeforeChange(Backups.Before.APPLY, sources),
                installation -> InstallationFiles.read(-1, installation.storefront(), installation.root()));
    }

    Apply(Edits edits, Writer writer, WritePath.BackUpFirst beforeApply, Reader reader) {
        this.edits = edits;
        this.writer = writer;
        this.beforeApply = beforeApply;
        this.reader = reader;
    }

    /**
     * Writes the edits, with no change let in while it runs. Afterwards the edits are the working copy, and the bar
     * goes dark.
     *
     * @return the report and the before-APPLY backup
     * @throws IOException if the backup cannot be taken, or the working copy cannot be saved - no live copy was
     *                     written, and the edits are still on screen
     */
    public Written apply() throws IOException {
        return edits.whileWriting(() -> {
            ConfigurationFiles files = edits.toApply();
            ConfigurationFiles before = edits.applied();
            AtomicReference<Backup> taken = new AtomicReference<>();
            WriteReport report = writer.write(files, sources -> {
                Backup backup = beforeApply.take(sources);
                taken.set(backup);
                return backup;
            }, installation -> differs(before, installation));
            log.info("Applied: {} binds and preset files, {} button maps; installations updated {} of {}",
                    files.bindings().size(), files.buttonMaps().size(), report.installationsUpdated(),
                    report.installations().size());
            return new Written(report, taken.get());
        });
    }

    private boolean differs(ConfigurationFiles before, BackupSources.Installation installation) {
        try {
            return DeviceFilesComparison.differs(before, reader.read(installation));
        } catch (IOException e) {
            // WHY: a file there that cannot be read could hold anything of the user's - left alone, as one that
            // differs, never written over unseen.
            log.warn("Could not read {} ({}) to compare it with the working copy, so it is left alone",
                    installation.storefront(), installation.root(), e);
            return true;
        }
    }
}
