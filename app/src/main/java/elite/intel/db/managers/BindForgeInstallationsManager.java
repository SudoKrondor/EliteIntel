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
 * <p>
 * <strong>The list is held in memory</strong> and read from the table again only after one of this manager's own
 * writes, which are the only writes the table gets. {@code DeviceFilesMonitor} asks for it every two seconds, for as
 * long as Elite-Intel runs, and the list changes only when the user or a rescan changes it.
 */
public class BindForgeInstallationsManager {

    private static volatile BindForgeInstallationsManager instance;

    /**
     * The rows as last read, or {@code null} after a write. Guarded by {@code this}.
     */
    private List<InstallationRow> cached;

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

    // WHY: synchronized with every write, so a read that started before a write cannot store the old list after
    // that write has dropped it.
    public synchronized List<InstallationRow> findAll() {
        if (cached == null) {
            cached = List.copyOf(Database.withDao(BindForgeInstallationsDao.class, BindForgeInstallationsDao::findAll));
        }
        return cached;
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
    public synchronized InstallationRow record(String storefront, Path root, boolean addedByHand) {
        String rootPath = root.toString();
        cached = null;
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
    public synchronized void setMissing(long id, boolean missing) {
        cached = null;
        Database.withDao(BindForgeInstallationsDao.class, dao -> {
            dao.setMissing(id, missing);
            return Void.TYPE;
        });
    }

    /**
     * Records whether an installation missed a write the others took - what catch-up asks about (A14).
     * <p>
     * Written only when it changes, so the list held in memory - read every two seconds by the device-file monitor -
     * is dropped only then: every push reports every installation, and most reports change nothing here.
     */
    public synchronized void setMissedWrite(long id, boolean missed) {
        int changed = Database.withDao(BindForgeInstallationsDao.class, dao -> dao.setMissedWrite(id, missed));
        if (changed > 0) cached = null;
    }

    /**
     * Repoints an installation at a new folder, keeping its id and everything keyed to it - the
     * reinstalled-to-another-drive case.
     */
    public synchronized void relocate(long id, Path newRoot) {
        cached = null;
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
    public synchronized void remove(long id) {
        cached = null;
        Database.withDao(BindForgeInstallationsDao.class, dao -> {
            dao.delete(id);
            return Void.TYPE;
        });
    }
}
