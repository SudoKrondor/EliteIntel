package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeIdentityAnswerDao;
import elite.intel.db.dao.BindForgeIdentityAnswerDao.AnswerRow;
import elite.intel.db.util.Database;

import java.util.Collection;
import java.util.List;

/**
 * The identity question's remembered answers - {@code NOT_NOW} and {@code NONE_OF_THESE} - read and written through
 * {@link BindForgeIdentityAnswerDao} (alias-designer.md, <em>What Changes When Hardware Changes</em>; Alan,
 * 2026-10-15, A12). Which question an answer still covers is decided by {@code IdentityQuestions}, which compares the
 * stored candidate set with the one in front of the user.
 */
public class BindForgeIdentityAnswerManager {

    private static volatile BindForgeIdentityAnswerManager instance;

    private BindForgeIdentityAnswerManager() {
        // Private constructor to prevent instantiation
    }

    public static BindForgeIdentityAnswerManager getInstance() {
        if (instance == null) {
            synchronized (BindForgeIdentityAnswerManager.class) {
                if (instance == null) {
                    instance = new BindForgeIdentityAnswerManager();
                }
            }
        }
        return instance;
    }

    public List<AnswerRow> findAll() {
        return Database.withDao(BindForgeIdentityAnswerDao.class, BindForgeIdentityAnswerDao::findAll);
    }

    /** Records an answer for a missing device, replacing the one before. */
    public void put(String deviceName, String candidates, String answer) {
        Database.withDao(BindForgeIdentityAnswerDao.class, dao -> {
            dao.put(deviceName, candidates, answer);
            return Void.TYPE;
        });
    }

    /** Forgets the answers for these devices - each one attached again, or gone from the user's devices. */
    public void forget(Collection<String> deviceNames) {
        if (deviceNames.isEmpty()) return;
        Database.withDao(BindForgeIdentityAnswerDao.class, dao -> {
            deviceNames.forEach(dao::delete);
            return Void.TYPE;
        });
    }
}
