package elite.intel.bindforge.storage;

import elite.intel.db.managers.BindForgeSettingsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The backup taken silently every time Elite-Intel starts, then retention (storage.md rules 11-12).
 * <p>
 * <strong>Whether or not the user ever opens BindForge, and before first-time startup has ever run.</strong> Started
 * from {@code App.main} once the database is open, on a thread of its own, so it never holds startup up and does not
 * wait for the services - which need credentials to start. The user is not told. It cannot be switched off.
 * <p>
 * <strong>A failure is logged and tried again at the next start.</strong> Nothing is pruned after a failure: today's
 * backup is missing, so the older ones are the newest there are (2026-10-24).
 * <p>
 * <strong>Nothing else BindForge does at startup writes to a game file before it is done</strong> - such a write asks
 * {@link #awaitTaken()} first, and writes nothing that run if the backup failed (principle 3; Alan, 2026-10-24).
 */
public final class StartupBackup {

    private static final Logger log = LogManager.getLogger(StartupBackup.class);
    private static final StartupBackup INSTANCE = new StartupBackup(Backups::stored,
            () -> BindForgeSettingsManager.getInstance().getBackupRetentionDays());

    private final Supplier<Backups> backups;
    private final IntSupplier retentionDays;
    private final AtomicBoolean started = new AtomicBoolean();
    private final CountDownLatch finished = new CountDownLatch(1);
    private volatile boolean taken;

    StartupBackup(Supplier<Backups> backups, IntSupplier retentionDays) {
        this.backups = backups;
        this.retentionDays = retentionDays;
    }

    public static StartupBackup getInstance() {
        return INSTANCE;
    }

    /** Takes the backup on a thread of its own. Only the first call in a run does anything. */
    public void startInBackground() {
        if (!started.compareAndSet(false, true)) return;
        Thread.ofVirtual().name("bindforge-startup-backup").start(this::run);
    }

    /**
     * Waits for the startup backup. Blocks - never on the EDT or the device bus.
     *
     * @return {@code true} once it has been taken; {@code false} if it failed, or the wait was interrupted
     */
    public boolean awaitTaken() {
        try {
            finished.await();
            return taken;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** The backup, then retention. On the caller's thread; {@link #startInBackground()} is what production uses. */
    void run() {
        Backups backupSystem;
        try {
            backupSystem = backups.get();
            Backup backup = backupSystem.backUpEverything();
            taken = true;
            log.info("Startup backup taken at {}", backup.folder());
        } catch (Backups.NothingToBackUpException e) {
            // WHY: a warning, not an error - a new player, or a game not yet installed, has no files yet, and an error
            // line at every start would send whoever reads a support bundle looking for a fault (review, 2026-10-24).
            log.warn("Nothing to back up at Elite-Intel start: {}. {}", String.join("; ", e.notRead()),
                    Backups.NothingToBackUpException.ADVICE);
            return;
        } catch (Exception e) {
            // WHY: broad on purpose - whatever stopped it, it is logged and tried again at the next start, and startup
            // goes on. Nothing is pruned.
            log.error("The backup taken when Elite-Intel starts failed; it is tried again at the next start", e);
            return;
        } finally {
            finished.countDown();
        }
        prune(backupSystem);
    }

    private void prune(Backups backupSystem) {
        try {
            backupSystem.prune(retentionDays.getAsInt());
        } catch (Exception e) {
            // WHY: broad on purpose - a backup kept too long costs disk, never the user's files; the next start
            // tries again.
            log.error("Deleting backups older than the retention setting failed; tried again at the next start", e);
        }
    }
}
