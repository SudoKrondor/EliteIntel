package elite.intel.bindforge.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The one way the game input configuration files are written (storage.md rules 3, 7-8, 10). First setup, APPLY,
 * RESTORE and the check's reconcile all write through {@link #write}, in one order:
 * <ol>
 *     <li><strong>back up</strong> - nothing is written until the backup exists (principle 3);</li>
 *     <li><strong>save the set as the working copy</strong>, in one step ({@link WorkingCopy});</li>
 *     <li><strong>write it to every live copy</strong>, everything it can, reporting what it could not
 *     ({@link LiveCopies}).</li>
 * </ol>
 * A failure in the first two stops it with nothing after written. The working copy is saved before the live copies so
 * that it always holds what BindForge last wrote: a live copy the write missed then differs from it, and the check or
 * the next write puts it right (storage.md rule 8). Touches the disk - not for the EDT.
 */
public final class WritePath {

    private static final Logger log = LogManager.getLogger(WritePath.class);

    /**
     * Takes the backup a write needs first - a before-changes one for APPLY, RESTORE and the check, first setup's own
     * for first setup ({@link Backups}).
     */
    @FunctionalInterface
    public interface BackUpFirst {
        /**
         * @param sources the bindings folder and installations the write is about to write - back up exactly these
         *                ({@link Backups#backUpBeforeChange(Backups.Before, BackupSources)})
         */
        Backup take(BackupSources sources) throws IOException;
    }

    /**
     * One write at a time, from the backup to the last live copy. Without it, two writes could each save the working
     * copy and then write the live copies in the other order, leaving the working copy holding one set and the game's
     * files the other (review, 2026-10-24).
     */
    private static final Object WRITING = new Object();

    private final WorkingCopy workingCopy;
    private final Supplier<BackupSources> sources;
    private final Consumer<ConfigurationFiles> written;

    /**
     * The stored working copy, and the bindings folder and installations a backup covers. Each set written is handed to
     * {@link Edits}, which then starts from it and drops the edits on screen (Storage S4a).
     */
    public static WritePath stored() throws IOException {
        return new WritePath(WorkingCopy.stored(), BackupSources::gather, Edits.onWritten());
    }

    WritePath(WorkingCopy workingCopy, Supplier<BackupSources> sources) {
        this(workingCopy, sources, files -> {
        });
    }

    WritePath(WorkingCopy workingCopy, Supplier<BackupSources> sources, Consumer<ConfigurationFiles> written) {
        this.workingCopy = workingCopy;
        this.sources = sources;
        this.written = written;
    }

    /**
     * Backs up, saves {@code files} as the working copy, and writes them to every live copy.
     *
     * @param files  the whole set - the working copy becomes exactly this
     * @param backUp the backup to take first
     * @return what each place took; a place that could not be written is in it, not thrown
     * @throws IOException if the backup cannot be taken, or the working copy cannot be saved - no live copy was
     *                     written
     */
    public WriteReport write(ConfigurationFiles files, BackUpFirst backUp) throws IOException {
        return write(files, backUp, LiveCopies.LeaveAlone.NONE);
    }

    /**
     * As {@link #write(ConfigurationFiles, BackUpFirst)}, leaving alone each installation {@code leaveAlone} picks -
     * asked just before that installation would be written, after the backup and the working copy.
     */
    WriteReport write(ConfigurationFiles files, BackUpFirst backUp, LiveCopies.LeaveAlone leaveAlone)
            throws IOException {
        synchronized (WRITING) {
            // WHY: found once, for the backup and the write alike. Found twice, an installation found only the second
            // time - a drive plugged in between - would be written with no backup of it (principle 3; review,
            // 2026-10-24).
            BackupSources places = sources.get();
            Backup backup = backUp.take(places);
            workingCopy.write(files);
            written.accept(files);
            WriteReport report = new LiveCopies(places).write(files, leaveAlone);
            log(backup, report);
            return report;
        }
    }

    private static void log(Backup backup, WriteReport report) {
        log.info("Wrote the working copy to the live copies after backup {}: the bindings folder {}, installations "
                        + "updated {} of {}", backup.zip(), report.bindings().updated() ? "updated" : "not updated",
                report.installationsUpdated(), report.installations().size());
        for (WriteReport.Place place : report.installations()) {
            if (place.differs()) {
                log.warn("Installation {} ({}) was left alone: its device files differ from the working copy",
                        place.storefront(), place.folder());
            } else if (!place.updated()) {
                log.warn("Installation {} ({}) was not fully updated: {}; kept as read-only: {}", place.storefront(),
                        place.folder(), String.join("; ", place.failures()), place.keptReadOnly());
            }
        }
        if (!report.bindings().updated()) {
            log.warn("The bindings folder ({}) was not fully updated: {}", report.bindings().folder(),
                    String.join("; ", report.bindings().failures()));
        }
    }
}
