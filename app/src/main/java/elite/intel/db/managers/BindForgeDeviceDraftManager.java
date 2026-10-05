package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeDeviceDraftDao;
import elite.intel.db.dao.BindForgeDeviceDraftDao.DraftRow;
import elite.intel.db.dao.BindForgeDeviceMasterDao;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.db.dao.BindForgeDeviceMasterDao.LabelRow;
import elite.intel.db.util.Database;
import org.jdbi.v3.core.Handle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The device draft: what the user has saved and not yet applied.
 * <p>
 * <strong>SAVE writes here; APPLY promotes it.</strong> The master ({@link BindForgeDeviceMasterManager}) is what
 * was last applied and what every installation is compared against, so an edit that has only been saved must
 * not be in it - the install strip and the startup check would then report differences nobody has applied.
 * <p>
 * <strong>The draft is a whole copy of the master's set</strong>, taken when one is started. Applying it is a
 * plain copy back, and afterwards the master holds exactly the set the user was looking at. With no draft,
 * {@link #findAll()} is empty and the device list reads the master.
 * <p>
 * <strong>A started draft may hold no edits.</strong> The editor starts one before reading the rows it will
 * change, so {@link #isStarted()} is not "the user changed something". Telling real edits apart is for whoever
 * needs to - the unsaved-work prompt, the Apply button - to decide.
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

    /** Whether a draft has been started and not yet applied or discarded. It may hold no edits. */
    public boolean isStarted() {
        return Database.withDao(BindForgeDeviceDraftDao.class, BindForgeDeviceDraftDao::exists);
    }

    /**
     * Starts a draft from the master, unless one already exists. Editing an existing device needs its draft row,
     * so the editor calls this before reading the rows it will change.
     */
    public void start() {
        update(daos -> { });
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
        return inTransaction(daos -> {
            startIfNone(daos);
            DraftRow existing = daos.draft().findByName(deviceName);
            if (existing != null) return existing;
            daos.draft().insert(null, deviceName, vid, pid, aliasConfirmed, null);
            return daos.draft().findByName(deviceName);
        });
    }

    public void setAliasConfirmed(long id, boolean confirmed) {
        update(daos -> daos.draft().setAliasConfirmed(id, confirmed));
    }

    /**
     * Renames a draft device. The name the installations still hold is kept, so that whatever applies the rename
     * can find the old {@code .buttonMap} in each of them.
     */
    public void rename(long id, String deviceName) {
        update(daos -> daos.draft().rename(id, deviceName));
    }

    /** Points a draft device at different hardware, keeping its name - the repair for a firmware update. */
    public void retarget(long id, String vid, String pid) {
        update(daos -> daos.draft().retarget(id, vid, pid));
    }

    /**
     * Removes a device from the draft, labels and all. Applying the draft records the device as a pending
     * removal - see {@link #promote()} - because Apply itself never removes anything from a game file.
     */
    public void remove(long id) {
        update(daos -> daos.draft().delete(id));
    }

    /** This draft device's labels, keyed by {@code .binds} input token, in token order. */
    public Map<String, String> labelsOf(long deviceId) {
        return Database.withDao(BindForgeDeviceDraftDao.class, dao -> labelMap(dao.labelsOf(deviceId)));
    }

    public void putLabel(long deviceId, String inputToken, String label) {
        update(daos -> daos.draft().putLabel(deviceId, inputToken, label));
    }

    /** Replaces this draft device's labels with the set given - a whole set, in one transaction. */
    public void replaceLabels(long deviceId, Map<String, String> labels) {
        update(daos -> {
            daos.draft().deleteLabels(deviceId);
            labels.forEach((token, label) -> daos.draft().putLabel(deviceId, token, label));
        });
    }

    /**
     * Puts one master device back into the draft as the master holds it, undoing every saved edit to it -
     * including its removal. A master device that no longer exists is simply left out of the draft.
     * <p>
     * The reverted device comes back as a new draft row with a new id, so re-read it rather than keep the old id.
     *
     * @throws IllegalStateException if another draft device has taken the master's name since - the user has to
     *                               rename or remove that one first, and is told which it is
     */
    public void revert(long masterId) {
        update(daos -> {
            DeviceRow row = daos.master().findById(masterId);
            if (row != null) {
                DraftRow holder = daos.draft().findByName(row.deviceName());
                if (holder != null && !Objects.equals(holder.masterId(), masterId)) {
                    throw new IllegalStateException("Cannot put " + row.deviceName() + " back: another device in "
                            + "the draft has that name now (draft id " + holder.id() + ")");
                }
            }
            daos.draft().deleteByMasterId(masterId);
            if (row != null) copyIntoDraft(daos.draft(), row);
        });
    }

    /** Throws the draft away. The master, and every installation, are as they were. */
    public void discard() {
        inTransaction(daos -> {
            daos.draft().deleteAll();
            daos.draft().clearStarted();
            return null;
        });
    }

    /**
     * Makes the draft the master and ends the draft, in one transaction. Does nothing when there is no draft.
     * <p>
     * The master is emptied and refilled rather than updated row by row, so two devices swapping names cannot
     * trip the unique alias between the two updates. <strong>So every master device gets a new id</strong> - see
     * {@link BindForgeDeviceMasterManager}.
     * <p>
     * A device the draft removed leaves the master but not the installations, since Apply adds and updates only.
     * Its name is kept as a pending removal ({@link BindForgeDeviceMasterManager#pendingRemovals()}) in this same
     * transaction, so a crash before the files are cleaned up forgets nothing. A name the draft puts back in the
     * master is no longer pending, so a cleanup never removes an entry in use again.
     *
     * @return the devices this Apply removed from the master, by name
     */
    public List<String> promote() {
        return inTransaction(daos -> {
            BindForgeDeviceDraftDao draft = daos.draft();
            BindForgeDeviceMasterDao master = daos.master();
            if (!draft.exists()) return List.of();

            List<String> removed = draft.masterNamesNotInDraft();
            List<PromotedDevice> devices = new ArrayList<>();
            for (DraftRow row : draft.findAll()) {
                devices.add(new PromotedDevice(row, labelMap(draft.labelsOf(row.id()))));
            }

            // WHY: the draft is emptied before the master. Its rows point at master rows, and emptying the master
            // first would work only because master_id happens to be ON DELETE SET NULL.
            draft.deleteAll();
            draft.clearStarted();
            master.deleteAll();
            for (PromotedDevice device : devices) {
                DraftRow row = device.row();
                master.insertWithPreviousName(row.deviceName(), row.vid(), row.pid(), row.aliasConfirmed(),
                        row.previousName());
                long masterId = master.findByName(row.deviceName()).id();
                device.labels().forEach((token, label) -> master.putLabel(masterId, token, label));
                master.clearPendingRemoval(row.deviceName());
            }
            removed.forEach(master::addPendingRemoval);
            return removed;
        });
    }

    private static void startIfNone(Daos daos) {
        if (daos.draft().exists()) return;
        daos.draft().markStarted();
        for (DeviceRow row : daos.master().findAll()) {
            copyIntoDraft(daos.draft(), row);
        }
    }

    private static void copyIntoDraft(BindForgeDeviceDraftDao draft, DeviceRow row) {
        draft.insert(row.id(), row.deviceName(), row.vid(), row.pid(), row.aliasConfirmed(), row.previousName());
        draft.copyLabelsFromMaster(draft.findByName(row.deviceName()).id(), row.id());
    }

    private static Map<String, String> labelMap(List<LabelRow> rows) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (LabelRow row : rows) {
            labels.put(row.inputToken(), row.label());
        }
        return labels;
    }

    /** A change to the draft, starting it first so the change lands in a whole copy of the master. */
    private static void update(Consumer<Daos> change) {
        inTransaction(daos -> {
            startIfNone(daos);
            change.accept(daos);
            return null;
        });
    }

    // WHY: a handle in a transaction, not Database.withDao, which borrows one in autocommit - under that, a
    // draft started by an edit that then failed would be left holding the master copy with the edit missing.
    // Both DAOs are attached to the one handle, so the draft and the master change in the same transaction.
    private static <R> R inTransaction(Function<Daos, R> work) {
        try (Handle handle = Database.init()) {
            return handle.inTransaction(h -> work.apply(
                    new Daos(h.attach(BindForgeDeviceDraftDao.class), h.attach(BindForgeDeviceMasterDao.class))));
        }
    }

    private record Daos(BindForgeDeviceDraftDao draft, BindForgeDeviceMasterDao master) {
    }

    private record PromotedDevice(DraftRow row, Map<String, String> labels) {
    }
}
