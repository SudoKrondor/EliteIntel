package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeHardwareIdMoveDao;
import elite.intel.db.dao.BindForgeHardwareIdMoveDao.MoveRow;
import elite.intel.db.util.Database;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Bindings waiting to move from a controller's old hardware id to its name, read and written through
 * {@link BindForgeHardwareIdMoveDao} (alias-designer.md, <em>What Changes When Hardware Changes</em>; Alan, 2026-10-17,
 * A17).
 * <p>
 * A row is written with the controller's name, in the draft's transaction ({@link BindForgeDeviceDraftManager#onboard},
 * through {@link #put}); Apply reads it and forgets it once the bindings are moved; the screen's refresh forgets the rest, when the device
 * has gone or the old controller is attached again. <strong>Keyed by the hardware the controller has now</strong>, so
 * a rename does not lose it.
 */
public class BindForgeHardwareIdMoveManager {

    private static volatile BindForgeHardwareIdMoveManager instance;

    private BindForgeHardwareIdMoveManager() {
        // Private constructor to prevent instantiation
    }

    public static BindForgeHardwareIdMoveManager getInstance() {
        if (instance == null) {
            synchronized (BindForgeHardwareIdMoveManager.class) {
                if (instance == null) {
                    instance = new BindForgeHardwareIdMoveManager();
                }
            }
        }
        return instance;
    }

    public List<MoveRow> findAll() {
        return Database.withDao(BindForgeHardwareIdMoveDao.class, BindForgeHardwareIdMoveDao::findAll);
    }

    /**
     * Records that the controller with this hardware is the one {@code .binds} still names by the old id, on a DAO in
     * the caller's transaction - the draft's, so the name and the record land together. The old id leaves any other
     * controller it was recorded for.
     */
    static void put(BindForgeHardwareIdMoveDao dao, String vid, String pid, String oldVid, String oldPid) {
        dao.deleteByOldId(upper(oldVid), upper(oldPid));
        dao.put(upper(vid), upper(pid), upper(oldVid), upper(oldPid));
    }

    /** Forgets the rows for these controllers, by the hardware they have now. */
    public void forget(Collection<MoveRow> rows) {
        if (rows.isEmpty()) return;
        Database.withDao(BindForgeHardwareIdMoveDao.class, dao -> {
            rows.forEach(row -> dao.delete(upper(row.vid()), upper(row.pid())));
            return Void.TYPE;
        });
    }

    // WHY: VID/PID hex has no case convention, even inside Frontier's own file, so the rows hold one.
    private static String upper(String hex) {
        return hex.toUpperCase(Locale.ROOT);
    }
}
