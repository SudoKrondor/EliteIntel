package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeActivationDao;
import elite.intel.db.util.Database;

/**
 * The stored "first-time startup complete" flag (storage.md, <em>First-time startup</em>; Storage S2b, Alan,
 * 2026-10-25), read and written through {@link BindForgeActivationDao}.
 * <p>
 * Set in the step that writes the working copy, and cleared only by Run first-time startup again - which clears it and
 * nothing else. It is not "the working copy exists": a working copy can be there while the flag is cleared.
 */
public class BindForgeActivationManager {

    private static volatile BindForgeActivationManager instance;

    private BindForgeActivationManager() {
        // Private constructor to prevent instantiation
    }

    public static BindForgeActivationManager getInstance() {
        if (instance == null) {
            synchronized (BindForgeActivationManager.class) {
                if (instance == null) {
                    instance = new BindForgeActivationManager();
                }
            }
        }
        return instance;
    }

    public boolean isFirstTimeStartupComplete() {
        return Database.withDao(BindForgeActivationDao.class, BindForgeActivationDao::isComplete);
    }

    public void markFirstTimeStartupComplete() {
        Database.withDao(BindForgeActivationDao.class, dao -> {
            dao.markComplete();
            return Void.TYPE;
        });
    }

    /** Run first-time startup again: clears the flag, and touches nothing else. */
    public void clearFirstTimeStartup() {
        Database.withDao(BindForgeActivationDao.class, dao -> {
            dao.clear();
            return Void.TYPE;
        });
    }
}
