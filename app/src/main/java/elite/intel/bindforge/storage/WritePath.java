package elite.intel.bindforge.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
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
        Backup take() throws IOException;
    }

    private final WorkingCopy workingCopy;
    private final Supplier<BackupSources> sources;

    /** The stored working copy, and the bindings folder and installations a backup covers. */
    public static WritePath stored() throws IOException {
        return new WritePath(WorkingCopy.stored(), BackupSources::gather);
    }

    WritePath(WorkingCopy workingCopy, Supplier<BackupSources> sources) {
        this.workingCopy = workingCopy;
        this.sources = sources;
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
        Backup backup = backUp.take();
        workingCopy.write(files);
        WriteReport report = new LiveCopies(sources.get()).write(files);
        log(backup, report);
        return report;
    }

    private static void log(Backup backup, WriteReport report) {
        log.info("Wrote the working copy to the live copies after backup {}: the bindings folder {}, installations "
                        + "updated {} of {}", backup.zip(), report.bindings().updated() ? "updated" : "not updated",
                report.installationsUpdated(), report.installations().size());
        for (WriteReport.Place place : report.installations()) {
            if (!place.updated()) log.warn("{} ({}) was not fully updated: {}", place.label(), place.folder(),
                    String.join("; ", place.failures()));
        }
        if (!report.bindings().updated()) {
            log.warn("The bindings folder ({}) was not fully updated: {}", report.bindings().folder(),
                    String.join("; ", report.bindings().failures()));
        }
    }
}
