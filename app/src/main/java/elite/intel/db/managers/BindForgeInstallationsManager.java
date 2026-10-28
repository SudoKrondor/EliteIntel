package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeInstallationsDao;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.util.Database;

import java.nio.file.Path;
import java.util.List;

/**
 * The game installations BindForge knows about.
 * <p>
 * Stores and reads rows; it does not decide what the list should contain. <strong>A removed installation is not in
 * the list</strong> - {@link #findAll()} leaves it out - so nothing backs it up or writes to it.
 * <p>
 * It does not decide what the list should contain otherwise. Reconciling a fresh detection
 * against these rows is the caller's job, because that needs the filesystem and a provider, and neither
 * belongs in a manager.
 * <p>
 * <strong>The list is held in memory</strong> and read from the table again only after one of this manager's own
 * writes, which are the only writes the table gets: every BindForge read and write asks for it, and the list changes
 * only when the user or a rescan changes it.
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
     * Every installation, the ones the user removed included - for the backup folder check, which refuses every game
     * folder BindForge knows of (Alan, 2026-10-25). Read from the table each time; nothing else should use it.
     */
    public List<InstallationRow> findAllIncludingRemoved() {
        return List.copyOf(Database.withDao(BindForgeInstallationsDao.class,
                BindForgeInstallationsDao::findAllIncludingRemoved));
    }

    /**
     * Records an installation detection found, or leaves the existing row alone when that folder is already known -
     * <strong>a removed one included, which stays removed</strong> (file-manager.md, <em>Game Install Locations</em>;
     * Storage S2b). Detection runs at every start, and would otherwise put back an installation the user removed.
     * <p>
     * Doing nothing on a repeat is what makes detection safe to run at every start: the row keeps its id and keeps
     * {@code addedByHand} rather than being demoted to auto-detected because detection has now also found it.
     */
    public synchronized void recordDetected(String storefront, Path root) {
        String rootPath = root.toString();
        Database.withDao(BindForgeInstallationsDao.class, dao -> {
            if (dao.findByPath(rootPath) != null) return Void.TYPE;
            cached = null;
            dao.insert(storefront, rootPath, false, false);
            return Void.TYPE;
        });
    }

    /**
     * Records an installation the user added - Add missing - or brings back one they removed at that folder.
     *
     * @return the row for that folder, in the list again
     */
    public synchronized InstallationRow addByHand(String storefront, Path root) {
        String rootPath = root.toString();
        cached = null;
        return Database.withDao(BindForgeInstallationsDao.class, dao -> {
            InstallationRow existing = dao.findByPath(rootPath);
            if (existing == null) {
                dao.insert(storefront, rootPath, true, false);
            } else if (Boolean.TRUE.equals(dao.isRemoved(rootPath))) {
                dao.setRemoved(existing.id(), false);
            }
            return dao.findByPath(rootPath);
        });
    }

    /**
     * Marks whether an installation's folder is there right now.
     * <p>
     * Missing is a state the user acts on, never something to clean up automatically: an unmounted drive and
     * an uninstall are indistinguishable from here.
     */
    public synchronized void setMissing(long id, boolean missing) {
        cached = null;
        Database.withDao(BindForgeInstallationsDao.class, dao -> {
            dao.setMissing(id, missing);
            return Void.TYPE;
        });
    }

    /**
     * Removes an installation from the list - Remove on Game Install Locations. <strong>It stays removed</strong>:
     * the row is kept and marked, so detection at the next start does not put it back, and it is neither backed up
     * nor written (file-manager.md, <em>Game Install Locations</em>; Storage S2b, 2026-10-25). Add missing on its
     * folder brings it back.
     */
    public synchronized void remove(long id) {
        cached = null;
        Database.withDao(BindForgeInstallationsDao.class, dao -> {
            dao.setRemoved(id, true);
            return Void.TYPE;
        });
    }

    /**
     * Drops the list held in memory - for a test that empties the table by SQL, since {@link #remove} keeps the row.
     */
    synchronized void forgetCached() {
        cached = null;
    }
}
