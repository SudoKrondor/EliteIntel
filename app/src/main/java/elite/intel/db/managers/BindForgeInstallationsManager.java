package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeInstallationsDao;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.util.Database;

import java.nio.file.Path;
import java.util.List;

/**
 * The game installations BindForge knows about.
 * <p>
 * Stores and reads rows; it does not decide what the list should contain. Reconciling a fresh detection
 * against these rows is the caller's job, because that needs the filesystem and a provider, and neither
 * belongs in a manager.
 */
public class BindForgeInstallationsManager {

    private static volatile BindForgeInstallationsManager instance;

    private BindForgeInstallationsManager() {
        // Private constructor to prevent instantiation
    }

    public static BindForgeInstallationsManager getInstance() {
        if (instance == null) {
            synchronized (BindForgeInstallationsManager.class) {
                if (instance == null) {
                    instance = new BindForgeInstallationsManager();
                }
            }
        }
        return instance;
    }

    public List<InstallationRow> findAll() {
        return Database.withDao(BindForgeInstallationsDao.class, BindForgeInstallationsDao::findAll);
    }

    /**
     * The installation at {@code root}, or {@code null} when no row holds that folder.
     * <p>
     * A folder belongs to at most one installation - the table enforces it - so callers can ask before
     * writing rather than reading the answer out of a constraint violation.
     */
    public InstallationRow findByPath(Path root) {
        String rootPath = root.toString();
        return Database.withDao(BindForgeInstallationsDao.class, dao -> dao.findByPath(rootPath));
    }

    /**
     * Records an installation, or leaves the existing row alone when that folder is already known.
     * <p>
     * Doing nothing on a repeat is what makes a rescan safe to run as often as the user likes: the row keeps
     * its id, so everything keyed to it survives, and keeps {@code addedByHand} rather than being demoted to
     * auto-detected because detection has now also found it.
     *
     * @return the row for that folder, whether it was just created or already there
     */
    public InstallationRow record(String storefront, Path root, boolean addedByHand) {
        String rootPath = root.toString();
        return Database.withDao(BindForgeInstallationsDao.class, dao -> {
            InstallationRow existing = dao.findByPath(rootPath);
            if (existing != null) return existing;
            dao.insert(storefront, rootPath, addedByHand, false);
            return dao.findByPath(rootPath);
        });
    }

    /**
     * Marks whether an installation's folder is there right now.
     * <p>
     * Missing is a state the user acts on, never something to clean up automatically: an unmounted drive and
     * an uninstall are indistinguishable from here, and dropping the row would take its device records away
     * with it.
     */
    public void setMissing(long id, boolean missing) {
        Database.withDao(BindForgeInstallationsDao.class, dao -> {
            dao.setMissing(id, missing);
            return Void.TYPE;
        });
    }

    /**
     * Repoints an installation at a new folder, keeping its id and everything keyed to it - the
     * reinstalled-to-another-drive case.
     */
    public void relocate(long id, Path newRoot) {
        Database.withDao(BindForgeInstallationsDao.class, dao -> {
            dao.relocate(id, newRoot.toString());
            return Void.TYPE;
        });
    }

    /**
     * Drops an installation from the list.
     * <p>
     * Deliberately discards what was held against it. There is no orphan-retention scheme and no undo beyond
     * adding the folder again: device records are cheap to rebuild, and backups are not kept here.
     */
    public void remove(long id) {
        Database.withDao(BindForgeInstallationsDao.class, dao -> {
            dao.delete(id);
            return Void.TYPE;
        });
    }
}
