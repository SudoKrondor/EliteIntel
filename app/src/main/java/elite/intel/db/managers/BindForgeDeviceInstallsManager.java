package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeDeviceInstallsDao;
import elite.intel.db.dao.BindForgeDeviceInstallsDao.InstallDeviceRow;
import elite.intel.db.util.Database;

import java.util.Collection;
import java.util.List;

/**
 * What each installation's {@code DeviceMappings.xml} holds, as last read from disk.
 * <p>
 * This is observation, not intent. The user's own choices are {@link BindForgeDeviceMasterManager}'s, and
 * comparing the two is what detects drift - including the case these rows exist for, where a game patch
 * replaces an installation's file and the wipe would otherwise look exactly like a device that was never
 * configured.
 * <p>
 * Stores and reads rows only. Deciding what a scan should find needs the filesystem and a parser, and neither
 * belongs in a manager.
 */
public class BindForgeDeviceInstallsManager {

    private static volatile BindForgeDeviceInstallsManager instance;

    private BindForgeDeviceInstallsManager() {
        // Private constructor to prevent instantiation
    }

    public static BindForgeDeviceInstallsManager getInstance() {
        if (instance == null) {
            synchronized (BindForgeDeviceInstallsManager.class) {
                if (instance == null) {
                    instance = new BindForgeDeviceInstallsManager();
                }
            }
        }
        return instance;
    }

    public List<InstallDeviceRow> findAll() {
        return Database.withDao(BindForgeDeviceInstallsDao.class, BindForgeDeviceInstallsDao::findAll);
    }

    public List<InstallDeviceRow> findByInstall(long installId) {
        return Database.withDao(BindForgeDeviceInstallsDao.class, dao -> dao.findByInstall(installId));
    }

    /** That installation's entry for a device, or {@code null} when it holds none - which is itself a finding. */
    public InstallDeviceRow findOne(long installId, String deviceName) {
        return Database.withDao(BindForgeDeviceInstallsDao.class, dao -> dao.findOne(installId, deviceName));
    }

    public void record(long installId, String deviceName, String vid, String pid, String provenance,
                       boolean hasButtonMap) {
        Database.withDao(BindForgeDeviceInstallsDao.class, dao -> {
            dao.record(installId, deviceName, vid, pid, provenance, hasButtonMap);
            return Void.TYPE;
        });
    }

    /**
     * Replaces everything recorded for one installation with what a scan just found.
     * <p>
     * A replacement rather than an upsert per row, because an entry the user deleted outside BindForge has to
     * disappear from these rows too - upserting what was found would leave it behind and report a device the
     * installation no longer has. Done in one DAO call so a failure cannot empty an installation's rows
     * without refilling them.
     */
    public void replaceForInstall(long installId, Collection<InstallDeviceRow> found) {
        Database.withDao(BindForgeDeviceInstallsDao.class, dao -> {
            dao.deleteForInstall(installId);
            for (InstallDeviceRow row : found) {
                dao.record(installId, row.deviceName(), row.vid(), row.pid(), row.provenance(),
                        row.hasButtonMap());
            }
            return Void.TYPE;
        });
    }

    public void remove(long installId, String deviceName) {
        Database.withDao(BindForgeDeviceInstallsDao.class, dao -> {
            dao.delete(installId, deviceName);
            return Void.TYPE;
        });
    }
}
