package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallationProvider;
import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.eventbus.UiBus;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.BindForgeReadOnlyChangedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Read only while an installation is missing (storage.md, <em>An installation missing - read only</em>; principle 13;
 * Storage S14a). Worked out once per Elite-Intel run, at the first ask: an installation in the stored list that fails
 * the one <em>reachable</em> test ({@link BackupSources#reachable(InstallationRow)}) is missing. Nothing checks the disk
 * again while Elite-Intel runs.
 * <p>
 * <strong>Before first-time startup has ever completed it does not apply</strong> - the very first setup runs with the
 * installations that are there. "Ever completed" is <strong>a working copy exists</strong> (Alan, 2026-11-05, S14a Q2):
 * only first-time startup and RESTORE make one, and Run first-time startup again leaves it, so a user who set up before
 * is still read only after clearing the flag.
 * <p>
 * <strong>OVERRIDE</strong> turns editing on until Elite-Intel closes. <strong>Remove</strong> in Game Install Locations
 * takes that installation off the missing list, and read only ends when none is left (Alan, 2026-11-05, S14a Q4).
 * <p>
 * Every change publishes {@link BindForgeReadOnlyChangedEvent}, after the lock is released. {@link Edits} refuses every
 * change and write while {@link #active()}, so a screen that offers one anyway is told rather than obeyed.
 */
public final class ReadOnlyMode {

    private static final Logger log = LogManager.getLogger(ReadOnlyMode.class);

    /** The warning at Elite-Intel start, in the user's language; {@code {0}} names the missing installations. */
    public static final String SPEECH_KEY = "speech.bindforge.readOnly";

    private static final ReadOnlyMode STORED = new ReadOnlyMode(
            () -> new InstallationRegistry(GameInstallationProvider.forThisPlatform(
                    PlayerSession.getInstance().getBindingsDir())).currentWithStartupScan(),
            () -> WorkingCopy.stored().exists(),
            BackupSources::reachable,
            () -> UiBus.publish(new BindForgeReadOnlyChangedEvent()));

    /** Whether a working copy exists - first-time startup, or RESTORE, has completed at least once. */
    @FunctionalInterface
    interface EverSetUp {
        boolean exists() throws IOException;
    }

    private final Supplier<List<InstallationRow>> installations;
    private final EverSetUp everSetUp;
    private final Predicate<InstallationRow> reachable;
    private final Runnable changed;
    private final AtomicBoolean warned = new AtomicBoolean();
    /**
     * One work-out at a time, held across the disk and the database. Apart from this object's own monitor because the
     * EDT takes that one: it must never wait on the work (a stalled drive is what this class is for).
     */
    private final Object workingOut = new Object();

    /**
     * The missing installations, or {@code null} until worked out. Guarded by this object's monitor.
     */
    private List<InstallationRow> missing;
    private boolean overridden;
    /**
     * Removed before {@link #missing} was known: the work-out under way may have read them before they went.
     */
    private final Set<Long> removedBeforeKnown = new HashSet<>();

    /** The one every BindForge screen and {@link Edits} share. */
    public static ReadOnlyMode stored() {
        return STORED;
    }

    ReadOnlyMode(Supplier<List<InstallationRow>> installations, EverSetUp everSetUp,
                 Predicate<InstallationRow> reachable, Runnable changed) {
        this.installations = installations;
        this.everSetUp = everSetUp;
        this.reachable = reachable;
        this.changed = changed;
    }

    /**
     * The installations that make BindForge read only - empty when it is not, before first-time startup has ever
     * completed, or when it could not be worked out. Worked out at the first call in a run; touches the disk and the
     * database then - not for the EDT.
     */
    public List<InstallationRow> missing() {
        boolean first = false;
        synchronized (workingOut) {
            if (!known()) {
                publish(workOut());
                first = true;
            }
        }
        if (first) changed.run();
        return missingIfKnown();
    }

    private synchronized boolean known() {
        return missing != null;
    }

    private synchronized void publish(List<InstallationRow> gone) {
        missing = gone.stream().filter(row -> !removedBeforeKnown.contains(row.id())).toList();
    }

    private List<InstallationRow> workOut() {
        try {
            if (!everSetUp.exists()) return List.of();
            List<InstallationRow> gone = installations.get().stream().filter(reachable.negate()).toList();
            if (!gone.isEmpty()) {
                log.warn("BindForge is read only: installations missing: {}", gone.stream()
                        .map(row -> row.storefront() + " (" + row.rootPath() + ")").toList());
            }
            return gone;
        } catch (IOException | RuntimeException e) {
            // WHY: broad on purpose - a database or disk fault must not lock the user out of BindForge. Every write
            // still backs up first, and the check still compares every installation it can read.
            log.error("Could not work out whether an installation is missing; BindForge is not read only", e);
            return List.of();
        }
    }

    /** Whether every change is off now. Works out {@link #missing()} first if it has not been - not for the EDT. */
    public boolean active() {
        missing();
        return activeIfKnown();
    }

    /**
     * {@link #active()} for the EDT: {@code false} until it has been worked out, and the screens hear
     * {@link BindForgeReadOnlyChangedEvent} once it is.
     */
    public synchronized boolean activeIfKnown() {
        return missing != null && !overridden && !missing.isEmpty();
    }

    /** {@link #missing()} for the EDT: empty until it has been worked out. */
    public synchronized List<InstallationRow> missingIfKnown() {
        return missing == null ? List.of() : missing;
    }

    /** OVERRIDE: editing on until Elite-Intel closes; the next start checks again. */
    public void override() {
        synchronized (this) {
            if (overridden) return;
            overridden = true;
        }
        log.warn("BindForge read only overridden by the user until Elite-Intel closes");
        changed.run();
    }

    /** The user removed this installation in Game Install Locations - it no longer counts as missing (Q4). */
    public void removed(long id) {
        synchronized (this) {
            if (missing == null) {
                removedBeforeKnown.add(id);
                return;
            }
            if (missing.stream().noneMatch(row -> row.id() == id)) return;
            missing = missing.stream().filter(row -> row.id() != id).toList();
        }
        log.info("A missing installation was removed; installations still missing: {}", missingIfKnown().size());
        changed.run();
    }

    /**
     * The warning at Elite-Intel start, once a run, on a thread of its own: {@code warn} is handed the missing
     * installations when BindForge is read only.
     */
    public void warnInBackground(Consumer<List<InstallationRow>> warn) {
        warnOnce(warn, task -> Thread.ofPlatform().name("BindForge-ReadOnlyCheck").start(task));
    }

    /**
     * {@link #warnInBackground}, on the executor given.
     */
    void warnOnce(Consumer<List<InstallationRow>> warn, Executor on) {
        if (!warned.compareAndSet(false, true)) return;
        on.execute(() -> {
            if (active()) warn.accept(missing());
        });
    }
}
