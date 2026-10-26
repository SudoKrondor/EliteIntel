package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * RESTORE (storage.md rule 10; file-manager.md, <em>Restore</em>): choose a backup, and the whole of it goes back - every
 * {@code .binds} and {@code StartPreset.4.start} to the bindings folder, and one set of device files to every
 * installation on this machine now, one the backup does not hold included - and it replaces the working copy. No
 * choices, except a reconcile when the backup's installations hold different device files (rule 2).
 * <p>
 * In two steps, so nothing is written until the user confirms: {@link #prepare} reads and checks, and {@link #write}
 * backs up what is there now (before RESTORE) and writes through {@link WritePath}, the one write path.
 * <p>
 * <strong>Refused before anything is backed up or written</strong> (Alan, 2026-10-26): with no bindings folder - it is
 * never created, and half a restore would follow; and a backup missing a half - no binds or preset file, or no device
 * files while this machine has installations - which would leave the working copy without binds, or remove every
 * {@code .buttonMap} the user has.
 * <p>
 * <strong>Publishes no {@code BindingsUpdatedEvent}</strong>: {@code BindingsMonitor} sees the bindings folder change
 * (overview.md). Touches the disk and the database - not for the EDT.
 */
public final class Restore {

    private static final Logger log = LogManager.getLogger(Restore.class);

    /** Which half of a backup is missing. */
    public enum Half {
        /** No {@code .binds} and no {@code StartPreset.N.start}. */
        BINDINGS,
        /** No installation's device files, while this machine has installations. */
        DEVICE_FILES
    }

    /** Thrown by {@link #prepare} when the backup lacks a half - nothing is backed up or written. */
    public static final class MissingHalfException extends IOException {
        private final Half half;

        MissingHalfException(Half half) {
            super(half == Half.BINDINGS ? "the backup holds no binds or preset file"
                    : "the backup holds no device files, and this machine has installations");
            this.half = half;
        }

        /** What it lacks. */
        public Half half() {
            return half;
        }
    }

    /**
     * What a restore will do, read and checked, and nothing written yet.
     *
     * @param contents what the backup holds
     * @param places   the bindings folder and every installation on this machine now - what its confirmation names
     * @param reading  the backup's installations, compared as first-time startup compares them - the reconcile's input
     *                 when they differ
     */
    public record Prepared(BackupContents contents, BackupSources places, FirstSetupReading reading) {
        public Prepared {
            Objects.requireNonNull(contents, "contents");
            Objects.requireNonNull(places, "places");
            Objects.requireNonNull(reading, "reading");
        }

        /** Whether the backup's installations hold the same device files - then nothing is asked but the confirmation. */
        public boolean identical() {
            return reading.identical();
        }

        /**
         * The set to write when they are {@linkplain #identical() identical}: every binds and preset file and the one
         * set of device files.
         */
        public ConfigurationFiles files() {
            return FirstSetupFiles.identical(reading);
        }
    }

    /**
     * What a restore did.
     *
     * @param report each place, as the write path reports it
     * @param backup the before-RESTORE backup of what was there
     */
    public record Written(WriteReport report, Backup backup) {
        public Written {
            Objects.requireNonNull(report, "report");
            Objects.requireNonNull(backup, "backup");
        }
    }

    /** Takes the before-RESTORE backup of what the write is about to write. */
    @FunctionalInterface
    interface BackupTaker {
        Backup take(BackupSources sources) throws IOException;
    }

    private final Supplier<BackupSources> places;
    private final FirstTimeStartup.Writer writer;
    private final BackupTaker beforeRestore;
    private final FirstTimeStartup.Flag flag;

    /** Detection and the stored installations, the write path, the backup folder and the stored flag. */
    public static Restore stored() {
        return new Restore(BackupSources::gather, (files, backUp) -> WritePath.stored().write(files, backUp),
                sources -> Backups.stored().backUpBeforeChange(Backups.Before.RESTORE, sources),
                FirstTimeStartup.storedFlag());
    }

    Restore(Supplier<BackupSources> places, FirstTimeStartup.Writer writer, BackupTaker beforeRestore,
            FirstTimeStartup.Flag flag) {
        this.places = places;
        this.writer = writer;
        this.beforeRestore = beforeRestore;
        this.flag = flag;
    }

    /**
     * Checks a backup can be restored here, and compares its installations. Writes nothing.
     *
     * @throws FirstTimeStartup.NoBindingsYetException if the bindings folder is not there
     * @throws MissingHalfException                    if the backup lacks a half
     * @throws IOException                             if its device files cannot be compared - the message names the
     *                                                 installation and the file
     */
    public Prepared prepare(BackupContents contents) throws IOException {
        BackupSources now = places.get();
        if (!Files.isDirectory(now.bindingsFolder())) {
            throw new FirstTimeStartup.NoBindingsYetException(now.bindingsFolder());
        }
        if (!contents.hasBindings()) throw new MissingHalfException(Half.BINDINGS);
        if (!contents.hasDeviceFiles() && !now.installations().isEmpty()) {
            throw new MissingHalfException(Half.DEVICE_FILES);
        }
        FirstSetupReading reading = FirstSetupReading.of(now.bindingsFolder(), contents.bindings(),
                contents.installations(), List.of(), FrontierStockDevices.getInstance(),
                FrontierStockButtonMaps.getInstance());
        return new Prepared(contents, now, reading);
    }

    /**
     * Backs up what is there now, then writes {@code files} - the backup's, or the set the user reconciled - as the
     * working copy and to every live copy. Marks first-time startup complete if it was not: RESTORE leaves the working
     * copy and the live copies matching, as first-time startup does (Alan, 2026-10-26).
     *
     * @throws IOException if the backup cannot be taken, or the working copy cannot be saved - nothing was written
     */
    public Written write(Prepared prepared, ConfigurationFiles files) throws IOException {
        Backup[] taken = new Backup[1];
        WriteReport report = writer.write(files, sources -> {
            taken[0] = beforeRestore.take(sources);
            return taken[0];
        });
        markFirstTimeStartupComplete();
        log.info("Restored {} ({}): {} binds and preset files, {}, {} button maps; installations updated {} of {}",
                prepared.contents().zip(), prepared.contents().taken(), files.bindings().size(),
                files.hasDeviceMappings() ? "a DeviceMappings.xml" : "no DeviceMappings.xml",
                files.buttonMaps().size(), report.installationsUpdated(), report.installations().size());
        return new Written(report, taken[0]);
    }

    private void markFirstTimeStartupComplete() {
        try {
            if (!flag.complete()) flag.markComplete();
        } catch (RuntimeException e) {
            // WHY: the restore itself is done and must be reported as done. Unset, the next click runs first-time
            // startup over the restored files, which now match the working copy - nothing is lost.
            log.error("The restore is written, but first-time startup could not be marked complete", e);
        }
    }
}
