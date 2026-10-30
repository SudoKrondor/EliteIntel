package elite.intel.bindforge.install;

import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.managers.BindForgeInstallationsManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps the stored installation list in step with what is on disk.
 * <p>
 * Detection runs again every time, but the list is never rebuilt from it. Rows persist because a row carries
 * facts detection cannot supply: that the user added it by hand, or removed it - a removed installation stays
 * removed (Storage S2b, 2026-10-25).
 */
public class InstallationRegistry {

    /**
     * Whether detection has run since the application started.
     * <p>
     * Static because "once per run" is a property of the run rather than of any one registry: the panels hold
     * their own instances today, and two of them scanning at startup would do the same work twice. It becomes
     * an instance field the moment they share a registry.
     */
    private static final AtomicBoolean DETECTED_THIS_RUN = new AtomicBoolean();
    private static final Object DETECTING = new Object();

    private final GameInstallationProvider provider;
    private final BindForgeInstallationsManager installations;

    public InstallationRegistry(GameInstallationProvider provider) {
        this(provider, BindForgeInstallationsManager.getInstance());
    }

    InstallationRegistry(GameInstallationProvider provider, BindForgeInstallationsManager installations) {
        this.provider = provider;
        this.installations = installations;
    }

    /** Test seam: lets a test start from "detection has not run yet" rather than inheriting a sibling's run. */
    static void forgetStartupScan() {
        DETECTED_THIS_RUN.set(false);
    }

    /**
     * Runs detection and folds the result into the stored list: anything new is added, and every row is
     * marked present or missing according to whether its folder is there.
     *
     * @return the list as it now stands, including rows detection did not produce
     */
    public List<InstallationRow> rescan() {
        // WHY: one detection at a time, across every registry - the Alias Designer and Install Locations screens each
        // hold one. Recording a folder is check-then-insert, so two detections at once can both find it absent, and
        // the second insert then breaks the path's UNIQUE constraint (review, 2026-10-13).
        synchronized (DETECTING) {
            return record(provider.findInstallations());
        }
    }

    /**
     * As {@link #rescan()}, with a detection the caller already ran - so a caller that also needs what detection
     * found (the backup, for each installation's bindings folder) detects once, not twice (review, 2026-10-24).
     *
     * @param detected a full detection, {@link GameInstallationProvider#findInstallations()}
     * @return the list as it now stands, including rows detection did not produce
     */
    public List<InstallationRow> rescan(List<GameInstallation> detected) {
        synchronized (DETECTING) {
            return record(detected);
        }
    }

    /** Folds a detection into the stored list. Holds {@code DETECTING}. */
    private List<InstallationRow> record(List<GameInstallation> detected) {
        for (GameInstallation installation : detected) {
            installations.recordDetected(installation.storefront().name(), installation.root());
        }
        refreshMissingFlags();
        // WHY: a full detection has run, so the run's startup detection has nothing left to do - the startup backup's
        // detection serves the screens' first refresh too (review, 2026-10-24).
        DETECTED_THIS_RUN.set(true);
        return installations.findAll();
    }

    /**
     * Adds an installation the user chose themselves - Add missing - or brings back one they removed.
     * <p>
     * For a Frontier-launcher install this is the only way in: nothing on the machine records where one went,
     * so detection cannot find it once it is off the published defaults.
     *
     * @throws IllegalArgumentException if the folder is not an Elite Dangerous installation
     */
    public InstallationRow addByHand(Path root) {
        // WHY: validated before it is accepted, because an arbitrary folder is not an installation and a bad
        // row would send every backup and write at the wrong place.
        if (!GameInstallation.confirmsAnInstall(root)) {
            throw new IllegalArgumentException("Not an Elite Dangerous installation: " + root + " (expected "
                    + GameInstallation.controlSchemesUnder(root) + " and " + GameInstallation.GAME_EXECUTABLE + ")");
        }
        return installations.addByHand(Storefront.MANUAL.name(), root);
    }

    /**
     * Removes an installation from the list. It stays removed: detection skips it, and nothing backs it up or writes
     * to it, until Add missing brings it back (Storage S2b, 2026-10-25).
     */
    public void remove(long id) {
        installations.remove(id);
    }

    public List<InstallationRow> current() {
        return installations.findAll();
    }

    /**
     * The list, having run detection once since the application started.
     * <p>
     * Detection belongs at startup: a storefront installed since the last run is otherwise invisible until
     * something runs detection, and nothing else does. Everything downstream inherits that staleness, including a launch backup that would then
     * cover fewer installations than it claims.
     * <p>
     * Once per run, not per call: this is reached from a refresh that also fires on every ship-profile
     * change, and re-running the registry, VDF and manifest reads each time would be waste.
     * <p>
     * <strong>A caller that arrives while that detection runs waits for it</strong> (review, 2026-10-15, A14). It
     * used to get the list straight away, before the detection had marked a folder that came back as present - so
     * a screen's first refresh read it as still missing.
     */
    public List<InstallationRow> currentWithStartupScan() {
        if (DETECTED_THIS_RUN.get()) return current();
        synchronized (DETECTING) {
            // WHY: set only once detection has finished, so the second caller cannot slip past one still running. A
            // detection that throws leaves it unset, and the next caller tries again.
            if (DETECTED_THIS_RUN.get()) return current();
            List<InstallationRow> rows = rescan();
            DETECTED_THIS_RUN.set(true);
            return rows;
        }
    }

    /**
     * Marks each row present or missing by looking for its folder.
     * <p>
     * Judged by the filesystem rather than by whether detection returned the row. A hand-added Frontier
     * install is never produced by detection, so treating absence from that list as missing would mark it
     * missing on every single rescan.
     */
    private void refreshMissingFlags() {
        for (InstallationRow row : installations.findAll()) {
            boolean missing = !Files.isDirectory(Path.of(row.rootPath()));
            if (missing != row.missing()) {
                installations.setMissing(row.id(), missing);
            }
        }
    }
}
