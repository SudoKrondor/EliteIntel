package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeDeviceDraftDao;
import elite.intel.db.dao.BindForgeDeviceDraftDao.DraftRow;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.db.dao.BindForgeDeviceMasterDao.LabelRow;
import elite.intel.db.util.Database;
import org.jdbi.v3.core.Handle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The device draft: what the user has saved and not yet applied.
 * <p>
 * <strong>SAVE writes here; APPLY promotes it.</strong> The master ({@link BindForgeDeviceMasterManager}) is what
 * was last applied and what every installation is compared against, so an edit that has only been saved must
 * not be in it - the install strip and the startup check would then report differences nobody has applied.
 * <p>
 * <strong>The draft is a whole copy of the master's set</strong>, taken by the first edit. Applying it is a plain
 * copy back, and afterwards the master holds exactly the set the user was looking at. No draft means no saved
 * edits: {@link #findAll()} is then empty, and the device list reads the master.
 * <p>
 * Every change runs in one transaction with starting the draft, so a failed edit never leaves a draft holding
 * part of the master.
 */
public class BindForgeDeviceDraftManager {

    private static volatile BindForgeDeviceDraftManager instance;

    private BindForgeDeviceDraftManager() {
        // Private constructor to prevent instantiation
    }

    public static BindForgeDeviceDraftManager getInstance() {
        if (instance == null) {
            synchronized (BindForgeDeviceDraftManager.class) {
                if (instance == null) {
                    instance = new BindForgeDeviceDraftManager();
                }
            }
        }
        return instance;
    }

    /** Whether there are saved edits that have not been applied. */
    public boolean exists() {
        return Database.withDao(BindForgeDeviceDraftDao.class, BindForgeDeviceDraftDao::exists);
    }

    /**
     * Starts a draft from the master, unless one already exists. Editing an existing device needs its draft row,
     * so the editor calls this before reading the rows it will change.
     */
    public void start() {
        inTransaction(dao -> {
            startIfNone(dao);
            return null;
        });
    }

    /** The draft's devices, or an empty list when there is no draft. */
    public List<DraftRow> findAll() {
        return Database.withDao(BindForgeDeviceDraftDao.class, BindForgeDeviceDraftDao::findAll);
    }

    /** The draft device with that alias, or {@code null}. */
    public DraftRow findByName(String deviceName) {
        return Database.withDao(BindForgeDeviceDraftDao.class, dao -> dao.findByName(deviceName));
    }

    /**
     * Adds a device to the draft, or returns the existing row when that alias is already taken - starting the
     * draft first if there is none. Like the master's {@code record}, this is the table's uniqueness and nothing
     * more: the alias-collision rule belongs with the naming code.
     */
    public DraftRow record(String deviceName, String vid, String pid, boolean aliasConfirmed) {
        return inTransaction(dao -> {
            startIfNone(dao);
            DraftRow existing = dao.findByName(deviceName);
            if (existing != null) return existing;
            dao.insert(null, deviceName, vid, pid, aliasConfirmed, null);
            return dao.findByName(deviceName);
        });
    }

    public void setAliasConfirmed(long id, boolean confirmed) {
        update(dao -> dao.setAliasConfirmed(id, confirmed));
    }

    /**
     * Renames a draft device. The name the installations still hold is kept, so that whatever applies the rename
     * can find the old {@code .buttonMap} in each of them.
     */
    public void rename(long id, String deviceName) {
        update(dao -> dao.rename(id, deviceName));
    }

    /** Points a draft device at different hardware, keeping its name - the repair for a firmware update. */
    public void retarget(long id, String vid, String pid) {
        update(dao -> dao.retarget(id, vid, pid));
    }

    /**
     * Removes a device from the draft, labels and all. Applying the draft then reports the device as removed -
     * see {@link #promote()} - because Apply itself never removes anything from a game file.
     */
    public void remove(long id) {
        update(dao -> dao.delete(id));
    }

    /** This draft device's labels, keyed by {@code .binds} input token, in token order. */
    public Map<String, String> labelsOf(long deviceId) {
        List<LabelRow> rows = Database.withDao(BindForgeDeviceDraftDao.class, dao -> dao.labelsOf(deviceId));
        Map<String, String> labels = new LinkedHashMap<>();
        for (LabelRow row : rows) {
            labels.put(row.inputToken(), row.label());
        }
        return labels;
    }

    public void putLabel(long deviceId, String inputToken, String label) {
        update(dao -> dao.putLabel(deviceId, inputToken, label));
    }

    /** Replaces this draft device's labels with the set given - a whole set, in one transaction. */
    public void replaceLabels(long deviceId, Map<String, String> labels) {
        update(dao -> {
            dao.deleteLabels(deviceId);
            labels.forEach((token, label) -> dao.putLabel(deviceId, token, label));
        });
    }

    /**
     * Puts one master device back into the draft as the master holds it, undoing every saved edit to it -
     * including its removal. A master device that no longer exists is simply left out of the draft.
     */
    public void revert(long masterId) {
        update(dao -> {
            dao.deleteByMasterId(masterId);
            DeviceRow row = dao.findMasterById(masterId);
            if (row != null) copyIntoDraft(dao, row);
        });
    }

    /** Throws the draft away. The master, and every installation, are as they were. */
    public void discard() {
        inTransaction(dao -> {
            dao.deleteAll();
            dao.clearStarted();
            return null;
        });
    }

    /**
     * Makes the draft the master and empties the draft, in one transaction. Does nothing when there is no draft.
     * <p>
     * The master is emptied and refilled rather than updated row by row, so two devices swapping names cannot
     * trip the unique alias between the two updates. Nothing refers to a master row's id except its labels,
     * which are copied with it.
     *
     * @return the master devices the draft no longer holds, by name. Apply adds and updates only, so these
     *         entries are still in every installation's files - whatever removes them works from this list.
     */
    public List<String> promote() {
        return inTransaction(dao -> {
            if (!dao.exists()) return List.of();
            List<String> removed = dao.masterNamesNotInDraft();
            dao.deleteAllMaster();
            for (DraftRow row : dao.findAll()) {
                dao.insertMaster(row.deviceName(), row.vid(), row.pid(), row.aliasConfirmed(), row.previousName());
                long masterId = dao.findMasterByName(row.deviceName()).id();
                dao.copyLabelsToMaster(masterId, row.id());
            }
            dao.deleteAll();
            dao.clearStarted();
            return removed;
        });
    }

    private static void startIfNone(BindForgeDeviceDraftDao dao) {
        if (dao.exists()) return;
        dao.markStarted();
        for (DeviceRow row : dao.findAllMaster()) {
            copyIntoDraft(dao, row);
        }
    }

    private static void copyIntoDraft(BindForgeDeviceDraftDao dao, DeviceRow row) {
        dao.insert(row.id(), row.deviceName(), row.vid(), row.pid(), row.aliasConfirmed(), row.previousName());
        dao.copyLabelsFromMaster(dao.findByName(row.deviceName()).id(), row.id());
    }

    /** A change to the draft, starting it first so the change lands in a whole copy of the master. */
    private static void update(Consumer<BindForgeDeviceDraftDao> change) {
        inTransaction(dao -> {
            startIfNone(dao);
            change.accept(dao);
            return null;
        });
    }

    // WHY: a handle in a transaction, not Database.withDao, which borrows one in autocommit - under that, a
    // draft started by an edit that then failed would be left holding the master copy with the edit missing.
    private static <R> R inTransaction(Function<BindForgeDeviceDraftDao, R> work) {
        try (Handle handle = Database.init()) {
            return handle.inTransaction(h -> work.apply(h.attach(BindForgeDeviceDraftDao.class)));
        }
    }
}
