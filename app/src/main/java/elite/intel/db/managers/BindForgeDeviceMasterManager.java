package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeDeviceMasterDao;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.db.dao.BindForgeDeviceMasterDao.LabelRow;
import elite.intel.db.util.Database;
import org.jdbi.v3.core.Handle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The master device set: the user's own device entries and their button and axis labels.
 * <p>
 * This is intent, not observation. What each installation actually holds is
 * {@link BindForgeDeviceInstallsManager}'s, and the difference between the two is the drift BindForge exists
 * to report.
 * <p>
 * Frontier's shipped entries are never in here. They are read from the stock reference that ships with the
 * application, because this table records only what BindForge might touch.
 */
public class BindForgeDeviceMasterManager {

    private static volatile BindForgeDeviceMasterManager instance;

    private BindForgeDeviceMasterManager() {
        // Private constructor to prevent instantiation
    }

    public static BindForgeDeviceMasterManager getInstance() {
        if (instance == null) {
            synchronized (BindForgeDeviceMasterManager.class) {
                if (instance == null) {
                    instance = new BindForgeDeviceMasterManager();
                }
            }
        }
        return instance;
    }

    public List<DeviceRow> findAll() {
        return Database.withDao(BindForgeDeviceMasterDao.class, BindForgeDeviceMasterDao::findAll);
    }

    /** The device with that alias, or {@code null} when the master holds none. */
    public DeviceRow findByName(String deviceName) {
        return Database.withDao(BindForgeDeviceMasterDao.class, dao -> dao.findByName(deviceName));
    }

    /**
     * The device for a piece of attached hardware, or {@code null} when it has never been named.
     * <p>
     * This is the correlation the device list is built on: hardware the master knows is shown with its alias,
     * and hardware it does not is shown unnamed rather than left out.
     */
    public DeviceRow findByHardware(String vid, String pid) {
        return Database.withDao(BindForgeDeviceMasterDao.class, dao -> dao.findByHardware(vid, pid));
    }

    /**
     * Adds a device to the master, or returns the existing row when that alias is already taken.
     * <p>
     * Deliberately does not enforce the alias-collision rule, which compares ignoring case, {@code -} and
     * {@code _} and must also reject Frontier's built-in names. That rule needs the stock reference and
     * belongs with the naming code, so this is the table's own uniqueness and nothing more.
     */
    public DeviceRow record(String deviceName, String vid, String pid, boolean aliasConfirmed) {
        // WHY: in a transaction, so the look-before-insert is not a race. Without one the row can appear
        // between the check and the insert, turning a repeat call - which is meant to be harmless - into a
        // UNIQUE violation thrown at whatever happened to call it second.
        try (Handle handle = Database.init()) {
            return handle.inTransaction(h -> {
                BindForgeDeviceMasterDao dao = h.attach(BindForgeDeviceMasterDao.class);
                DeviceRow existing = dao.findByName(deviceName);
                if (existing != null) return existing;
                dao.insert(deviceName, vid, pid, aliasConfirmed);
                return dao.findByName(deviceName);
            });
        }
    }

    /**
     * Marks that the user has looked at this device's name, which is what unlocks button and axis naming.
     * <p>
     * One flag for the device rather than one per installation: there is a single record now, so a name
     * cannot be confirmed in one installation and unconfirmed in another.
     */
    public void setAliasConfirmed(long id, boolean confirmed) {
        Database.withDao(BindForgeDeviceMasterDao.class, dao -> {
            dao.setAliasConfirmed(id, confirmed);
            return Void.TYPE;
        });
    }

    /**
     * Renames a device and remembers what it was called.
     * <p>
     * The old name is kept because a rename is not finished when this row changes: every installation still
     * holds a {@code .buttonMap} under the old name, and that file has to be renamed rather than left beside
     * the new one where nothing would ever clean it up. {@link #clearPreviousName(long)} closes the rename
     * once those files have been dealt with.
     */
    public void rename(long id, String deviceName) {
        Database.withDao(BindForgeDeviceMasterDao.class, dao -> {
            dao.rename(id, deviceName);
            return Void.TYPE;
        });
    }

    /** Marks a rename finished, once every installation's {@code .buttonMap} has been renamed with it. */
    public void clearPreviousName(long id) {
        Database.withDao(BindForgeDeviceMasterDao.class, dao -> {
            dao.clearPreviousName(id);
            return Void.TYPE;
        });
    }

    /**
     * Points an entry at different hardware, keeping its name.
     * <p>
     * The repair for a vendor firmware update, which changes a device's PID and silently breaks every binding
     * naming it. Because {@code .binds} references the alias rather than the ID, every binding follows this
     * one field and nothing else has to be touched.
     */
    public void retarget(long id, String vid, String pid) {
        Database.withDao(BindForgeDeviceMasterDao.class, dao -> {
            dao.retarget(id, vid, pid);
            return Void.TYPE;
        });
    }

    /**
     * Removes a device from the master, labels and all.
     * <p>
     * The device itself stays visible in the list while its hardware is attached - the list is driven by what
     * is plugged in, so this returns it to "not added" rather than making it disappear.
     */
    public void remove(long id) {
        Database.withDao(BindForgeDeviceMasterDao.class, dao -> {
            dao.delete(id);
            return Void.TYPE;
        });
    }

    /** This device's labels, keyed by {@code .binds} input token, in token order. */
    public Map<String, String> labelsOf(long deviceId) {
        List<LabelRow> rows = Database.withDao(BindForgeDeviceMasterDao.class, dao -> dao.labelsOf(deviceId));
        Map<String, String> labels = new LinkedHashMap<>();
        for (LabelRow row : rows) {
            labels.put(row.inputToken(), row.label());
        }
        return labels;
    }

    public void putLabel(long deviceId, String inputToken, String label) {
        Database.withDao(BindForgeDeviceMasterDao.class, dao -> {
            dao.putLabel(deviceId, inputToken, label);
            return Void.TYPE;
        });
    }

    /**
     * Replaces this device's labels with the set given.
     * <p>
     * A whole-set replacement rather than a merge, because generating labels for a device whose axis or
     * button count changed must not leave it holding some of the old set beside some of the new.
     * <p>
     * In a transaction, not merely in one call. {@code Database.withDao} borrows a handle in autocommit, so
     * the delete would commit on its own and a failure part way through the inserts would leave exactly the
     * mixture this method exists to prevent - and the device would then write that partial set outward on the
     * next Apply.
     */
    public void replaceLabels(long deviceId, Map<String, String> labels) {
        try (Handle handle = Database.init()) {
            handle.useTransaction(h -> {
                BindForgeDeviceMasterDao dao = h.attach(BindForgeDeviceMasterDao.class);
                dao.deleteLabels(deviceId);
                labels.forEach((token, label) -> dao.putLabel(deviceId, token, label));
            });
        }
    }
}
