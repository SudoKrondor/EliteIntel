package elite.intel.db.managers;

import elite.intel.db.dao.QuietedNoticesDao;
import elite.intel.db.util.Database;

import java.util.Objects;

/**
 * The notices the user has chosen not to see again - a dialog's <em>"Don't show this again"</em> box - read and written
 * through {@link QuietedNoticesDao} (Alan, 2026-11-14, Storage S21; storage.md rule 13). App-wide, not BindForge's:
 * Reset warnings sits on Services' Push To Talk and Other Settings tab, and any dialog can join with a name of its own.
 */
public class QuietedNoticesManager {

    private static volatile QuietedNoticesManager instance;

    private QuietedNoticesManager() {
        // Private constructor to prevent instantiation
    }

    public static QuietedNoticesManager getInstance() {
        if (instance == null) {
            synchronized (QuietedNoticesManager.class) {
                if (instance == null) {
                    instance = new QuietedNoticesManager();
                }
            }
        }
        return instance;
    }

    /** Whether the user ticked this notice's box, and has not unticked it or reset the warnings since. */
    public boolean isQuieted(String notice) {
        Objects.requireNonNull(notice, "notice");
        return Database.withDao(QuietedNoticesDao.class, dao -> dao.count(notice) > 0);
    }

    /** Whether any notice is quieted - Reset warnings is greyed while none is. */
    public boolean anyQuieted() {
        return Database.withDao(QuietedNoticesDao.class, dao -> dao.countAll() > 0);
    }

    /**
     * The box ticked ({@code true}) or unticked ({@code false}), stored the moment it changes - the dialog's buttons
     * have no say in it (Alan, 2026-11-14).
     */
    public void setQuieted(String notice, boolean quieted) {
        Objects.requireNonNull(notice, "notice");
        Database.withDao(QuietedNoticesDao.class, dao -> {
            if (quieted) {
                dao.quiet(notice);
            } else {
                dao.unquiet(notice);
            }
            return Void.TYPE;
        });
    }

    /** Reset warnings: every notice shows again. */
    public void resetAll() {
        Database.withDao(QuietedNoticesDao.class, dao -> {
            dao.deleteAll();
            return Void.TYPE;
        });
    }
}
