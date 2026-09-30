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
 * Detection runs again every time, but the list is never rebuilt from it. Rows persist because things hang
 * off them - device records are keyed by installation - and because a row carries facts detection cannot
 * supply: that the user added it by hand, or moved it.
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

    private final GameInstallationProvider provider;
    private final BindForgeInstallationsManager installations;

    public InstallationRegistry(GameInstallationProvider provider) {
        this(provider, BindForgeInstallationsManager.getInstance());
    }

    InstallationRegistry(GameInstallationProvider provider, BindForgeInstallationsManager installations) {
        this.provider = provider;
        this.installations = installations;
    }

    /**
     * Runs detection and folds the result into the stored list: anything new is added, and every row is
     * marked present or missing according to whether its folder is there.
     *
     * @return the list as it now stands, including rows detection did not produce
     */
    /** Test seam: lets a test start from "detection has not run yet" rather than inheriting a sibling's run. */
    static void forgetStartupScan() {
        DETECTED_THIS_RUN.set(false);
    }

    public List<InstallationRow> rescan() {
        for (GameInstallation detected : provider.findInstallations()) {
            installations.record(detected.storefront().name(), detected.root(), false);
        }
        refreshMissingFlags();
        return installations.findAll();
    }

    /**
     * Adds an installation the user chose themselves.
     * <p>
     * For a Frontier-launcher install this is the only way in: nothing on the machine records where one went,
     * so detection cannot find it once it is off the published defaults.
     *
     * @throws IllegalArgumentException if the folder is not an Elite Dangerous installation
     */
    public InstallationRow addByHand(Path root) {
        // WHY: validated before it is accepted, because an arbitrary folder is not an installation and a bad
        // row would send every backup, restore and apply at the wrong place.
        requireAnInstallation(root);
        return installations.record(Storefront.MANUAL.name(), root, true);
    }

    /**
     * Repoints an installation at a folder the user picked, keeping its id and everything keyed to it.
     *
     * @throws IllegalArgumentException if the folder is not an Elite Dangerous installation, or if another
     *                                  installation already occupies it
     */
    public void relocate(long id, Path newRoot) {
        // WHY: validated exactly as a hand-added folder is. Relocate is the same act - the user naming where
        // the game is - so accepting something here that addByHand would refuse makes no sense.
        requireAnInstallation(newRoot);
        // WHY: asked before writing rather than left to the table's UNIQUE constraint. The constraint is
        // right and stays, but it surfaces as an opaque wrapped SQL failure that the caller cannot tell
        // apart from a broken database - and that reached the user as nothing happening at all.
        InstallationRow occupant = installations.findByPath(newRoot);
        if (occupant != null && occupant.id() != id) {
            throw new AlreadyListedException(newRoot, occupant);
        }
        installations.relocate(id, newRoot);
    }

    /** Thrown when the folder the user picked is already held by a different installation. */
    public static class AlreadyListedException extends IllegalArgumentException {
        private final transient InstallationRow occupant;

        AlreadyListedException(Path folder, InstallationRow occupant) {
            super("Already listed as " + occupant.storefront() + ": " + folder);
            this.occupant = occupant;
        }

        /** The installation already at that folder, so the user can be told which one it is. */
        public InstallationRow occupant() {
            return occupant;
        }
    }

    /**
     * Drops an installation from the list, discarding what was held against it.
     */
    public void remove(long id) {
        installations.remove(id);
    }

    private static void requireAnInstallation(Path root) {
        if (!GameInstallation.looksLikeAnInstall(root)) {
            throw new IllegalArgumentException("Not an Elite Dangerous installation: " + root
                    + " (expected " + GameInstallation.controlSchemesUnder(root) + ")");
        }
    }

    public List<InstallationRow> current() {
        return installations.findAll();
    }

    /**
     * The list, having run detection once since the application started.
     * <p>
     * Detection belongs at startup: a storefront installed since the last run is otherwise invisible until
     * the user happens to press Rescan, and they have no reason to - the screen shows one installation and
     * looks correct. Everything downstream inherits that staleness, including a launch backup that would then
     * cover fewer installations than it claims.
     * <p>
     * Once per run, not per call: this is reached from a refresh that also fires on every ship-profile
     * change, and re-running the registry, VDF and manifest reads each time would be waste.
     */
    public List<InstallationRow> currentWithStartupScan() {
        return DETECTED_THIS_RUN.compareAndSet(false, true) ? rescan() : current();
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
