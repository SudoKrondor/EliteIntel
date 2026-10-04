package elite.intel.db.managers;

import elite.intel.bindforge.devicefiles.Provenance;
import elite.intel.db.dao.BindForgeDeviceInstallsDao;
import elite.intel.db.dao.BindForgeDeviceInstallsDao.InstallDeviceRow;
import elite.intel.db.util.Database;
import org.jdbi.v3.core.Handle;

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

    public void record(long installId, String deviceName, String vid, String pid, Provenance provenance,
                       boolean hasButtonMap) {
        Database.withDao(BindForgeDeviceInstallsDao.class, dao -> {
            dao.record(installId, deviceName, vid, pid, provenance.stored(), hasButtonMap);
            return Void.TYPE;
        });
    }

    /**
     * Replaces everything recorded for one installation with what a scan just found.
     * <p>
     * A replacement rather than an upsert per row, because an entry the user deleted outside BindForge has to
     * disappear from these rows too - upserting what was found would leave it behind and report a device the
     * installation no longer has.
     * <p>
     * <strong>In a transaction, and this one matters more than most.</strong> {@code Database.withDao} borrows
     * a handle in autocommit, so the delete would commit on its own: a failure before the rows were refilled
     * would leave the installation with no device rows at all. That is not a blank slate - it is the exact
     * signature of the incident this table exists to detect, a game patch having wiped the file, and BindForge
     * would be manufacturing it. An installation with no rows cannot be told from a device that was never
     * configured.
     */
    public void replaceForInstall(long installId, Collection<FoundDevice> found) {
        try (Handle handle = Database.init()) {
            handle.useTransaction(h -> {
                BindForgeDeviceInstallsDao dao = h.attach(BindForgeDeviceInstallsDao.class);
                dao.deleteForInstall(installId);
                for (FoundDevice device : found) {
                    dao.record(installId, device.deviceName(), device.vid(), device.pid(),
                            device.provenance().stored(), device.hasButtonMap());
                }
            });
        }
    }

    /**
     * One device as a scan found it, with no installation on it.
     * <p>
     * Deliberately not {@link InstallDeviceRow}, which carries an {@code installId} because a row read back
     * from the database knows which installation it came from. On the way in that field is a second copy of
     * something the call already states, and a caller that assembled rows for one installation and named
     * another would be silently moving devices between them - with the result looking exactly like real
     * drift for the user to resolve. The field is absent here rather than checked, so the mistake cannot be
     * written down.
     *
     * @param deviceName   the element tag exactly as that installation's file spells it
     * @param provenance   whether BindForge may touch this entry
     * @param hasButtonMap whether a {@code .buttonMap} exists under this name in that installation
     */
    public record FoundDevice(String deviceName, String vid, String pid, Provenance provenance,
                              boolean hasButtonMap) {
    }

    public void remove(long installId, String deviceName) {
        Database.withDao(BindForgeDeviceInstallsDao.class, dao -> {
            dao.delete(installId, deviceName);
            return Void.TYPE;
        });
    }
}
