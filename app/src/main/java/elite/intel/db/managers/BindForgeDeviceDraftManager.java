package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeActivationDao;
import elite.intel.db.dao.BindForgeDeviceDraftDao;
import elite.intel.db.dao.BindForgeDeviceDraftDao.DraftRow;
import elite.intel.db.dao.BindForgeDeviceMasterDao;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.db.dao.BindForgeDeviceMasterDao.LabelRow;
import elite.intel.db.util.Database;
import org.jdbi.v3.core.Handle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    /** Points a draft device at different hardware, keeping its name - the repair for a firmware update. */
    public void retarget(long id, String vid, String pid) {
        update(daos -> daos.draft().retarget(id, vid, pid));
    }

    /**
     * The identity question's answer, <em>this is the same controller</em>: the device keeps its name and labels and
     * takes the attached controller's VID/PID, in one transaction with starting the draft (Alan, 2026-10-15, A12).
     * Bindings name the device, so they follow it; Apply writes the new VID/PID to every installation.
     *
     * @return {@code false}, writing nothing, when the device is gone, no longer has the hardware the question showed,
     *         or another draft device already has the new hardware - the caller read a list that has changed since
     */
    public boolean retargetByName(String deviceName, String wasVid, String wasPid, String vid, String pid) {
        return inTransaction(daos -> {
            startIfNone(daos);
            BindForgeDeviceDraftDao draft = daos.draft();
            DraftRow row = draft.findByName(deviceName);
            if (row == null || !sameHardware(row, wasVid, wasPid)) return false;
            if (draft.findAll().stream().anyMatch(other -> other.id() != row.id() && sameHardware(other, vid, pid))) {
                return false;
            }
            draft.retarget(row.id(), vid, pid);
            return true;
        });
    }

    // WHY: VID/PID hex has no case convention, even inside Frontier's own file.
    private static boolean sameHardware(DraftRow row, String vid, String pid) {
        return row.vid() != null && row.pid() != null && row.vid().equalsIgnoreCase(vid)
                && row.pid().equalsIgnoreCase(pid);
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
     * The Device Editor's SAVE: the device's whole label set into the draft, in one transaction with starting the
     * draft (Alan, 2026-10-10, A9). A device the draft does not hold yet - a built-in the master has no labels for -
     * is added under the name and hardware given, as a name the user has seen.
     *
     * @return the draft row the labels were saved on
     */
    public DraftRow saveLabels(String deviceName, String vid, String pid, Map<String, String> labels) {
        return inTransaction(daos -> {
            startIfNone(daos);
            BindForgeDeviceDraftDao draft = daos.draft();
            DraftRow row = draft.findByName(deviceName);
            if (row == null) {
                draft.insert(null, deviceName, vid, pid, true, null);
                row = draft.findByName(deviceName);
            }
            long id = row.id();
            draft.deleteLabels(id);
            labels.forEach((token, label) -> draft.putLabel(id, token, label));
            return row;
        });
    }

    /**
     * The Device Editor's SAVE with the name changed: the rename and the whole label set, in one transaction with
     * starting the draft (Alan, 2026-10-14, A11). A device under its old name keeps that name as its previous name,
     * so Apply can find its element and {@code .buttonMap} in each installation; see the DAO's rename for when it
     * keeps none.
     * <p>
     * Only a device the draft already holds can be renamed - a built-in the master has no labels for is not one of
     * the user's to rename (A15).
     *
     * @return the draft row saved on, under its new name - or {@code null}, writing nothing, when the device is gone
     *         or the new name is already a draft device's: the caller read a list that has changed since
     */
    public DraftRow saveDevice(String currentName, String newName, Map<String, String> labels) {
        return inTransaction(daos -> {
            startIfNone(daos);
            BindForgeDeviceDraftDao draft = daos.draft();
            DraftRow row = draft.findByName(currentName);
            if (row == null) return null;
            if (!currentName.equals(newName)) {
                DraftRow holder = draft.findByName(newName);
                if (holder != null && holder.id() != row.id()) return null;
                draft.rename(row.id(), newName);
            }
            long id = row.id();
            draft.deleteLabels(id);
            labels.forEach((token, label) -> draft.putLabel(id, token, label));
            return draft.findByName(newName);
        });
    }

    /**
     * Onboarding's names, in one transaction (Alan, 2026-10-14, A6b).
     * <p>
     * <strong>Named now</strong> - a controller no binding names by hardware id - goes into the master, since its
     * entry is written to every installation straight away and an installation entry the master lacks is a
     * divergence. <strong>And into a started draft</strong>, as a copy of the master row: Apply promotes the draft,
     * so a name left out of it would leave the master again and be stored as a pending removal.
     * <p>
     * <strong>Named at Apply</strong> - a controller {@code .binds} names by hardware id - goes into the draft only,
     * starting it, because naming it moves bindings, which only Apply does.
     *
     * @param now     controllers named now, with their generated labels. Ids are ignored
     * @param atApply controllers that wait for Apply, with their generated labels. Ids are ignored
     * @return {@code false}, writing nothing, when BindForge is not activated or a name is already in the master or
     *         the draft - the caller read a list that has changed since
     */
    public boolean onboard(List<StoredDevice> now, List<StoredDevice> atApply) {
        return inTransaction(daos -> {
            BindForgeDeviceMasterDao master = daos.master();
            BindForgeDeviceDraftDao draft = daos.draft();
            if (!daos.activation().isActivated()) return false;
            for (StoredDevice device : concat(now, atApply)) {
                if (master.findByName(device.deviceName()) != null || draft.findByName(device.deviceName()) != null) {
                    return false;
                }
            }
            boolean draftStarted = draft.exists();
            for (StoredDevice device : now) {
                master.insert(device.deviceName(), device.vid(), device.pid(), true);
                DeviceRow row = master.findByName(device.deviceName());
                device.labels().forEach((token, label) -> master.putLabel(row.id(), token, label));
                master.clearPendingRemoval(device.deviceName());
                if (draftStarted) copyIntoDraft(draft, row);
            }
            if (!atApply.isEmpty()) {
                // WHY: after the master rows above, so a draft started here copies them in with the rest.
                startIfNone(daos);
                for (StoredDevice device : atApply) {
                    draft.insert(null, device.deviceName(), device.vid(), device.pid(), true, null);
                    long draftId = draft.findByName(device.deviceName()).id();
                    device.labels().forEach((token, label) -> draft.putLabel(draftId, token, label));
                }
            }
            return true;
        });
    }

    private static List<StoredDevice> concat(List<StoredDevice> first, List<StoredDevice> second) {
        List<StoredDevice> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
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

    /**
     * CLEAR, or RESET LABELS on a built-in: takes the device out of the draft, labels and all, starting the draft
     * first (Alan, 2026-10-11, A10). Applying it makes the name a pending removal, which is what takes its entry
     * and {@code .buttonMap} out of every installation.
     *
     * @return {@code false} when the draft holds no device of that name - nothing was changed
     */
    public boolean removeByName(String deviceName) {
        return inTransaction(daos -> {
            startIfNone(daos);
            DraftRow row = daos.draft().findByName(deviceName);
            if (row == null) return false;
            daos.draft().delete(row.id());
            return true;
        });
    }

    /**
     * UNDO CLEAR and UNDO RESET: puts a master device back into the draft exactly as the master holds it - {@link
     * #revert(long)}, by name, since the dialog that asks knows the device by name.
     *
     * @return {@code false} when the master holds no device of that name - nothing was changed
     * @throws IllegalStateException as {@link #revert(long)} does
     */
    public boolean restoreFromMaster(String deviceName) {
        DeviceRow row = Database.withDao(BindForgeDeviceMasterDao.class, dao -> dao.findByName(deviceName));
        if (row == null) return false;
        revert(row.id());
        return true;
    }

    /**
     * The master and the draft, read in one transaction - what telling a draft with edits from an untouched one
     * compares ({@code DraftChanges}).
     */
    public Stored stored() {
        return inTransaction(daos -> new Stored(storedMaster(daos.master()), storedDraft(daos.draft())));
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

            List<PromotedDevice> devices = new ArrayList<>();
            Set<String> draftNames = new HashSet<>();
            for (DraftRow row : draft.findAll()) {
                devices.add(new PromotedDevice(row, labelMap(draft.labelsOf(row.id()))));
                draftNames.add(row.deviceName());
            }
            // WHY: a name the draft holds is never removed, whichever row holds it. A device cleared and then saved
            // again comes back as a new draft row with no master row behind it, so its master row reads as removed
            // - and stored as pending, the cleanup would take a device out of every installation while it was in the
            // master (found 2026-10-11, A10).
            List<String> removed = draft.masterNamesNotInDraft().stream()
                    .filter(name -> !draftNames.contains(name))
                    .toList();
            // WHY: a device that leaves the master mid-rename leaves its old name in the installations that missed the
            // rename, and perhaps in .binds. Kept pending, that name is removed - or held back while .binds names it -
            // like any other, rather than forgotten with the row (review, 2026-10-15, A11).
            List<String> oldNames = master.findAll().stream()
                    .filter(row -> removed.contains(row.deviceName()))
                    .map(DeviceRow::previousName)
                    .filter(old -> old != null && !removed.contains(old) && !draftNames.contains(old))
                    .toList();

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
            oldNames.forEach(master::addPendingRemoval);
            return removed;
        });
    }

    /**
     * A device as the master or the draft holds it, labels included - what adopting an installation plans with.
     *
     * @param masterId for a master device its own id; for a draft device the master row it was copied from, or
     *                 {@code null} for one added in the draft
     */
    public record StoredDevice(Long masterId, String deviceName, String vid, String pid, boolean aliasConfirmed,
                               String previousName, Map<String, String> labels) {
        public StoredDevice {
            Objects.requireNonNull(deviceName, "deviceName");
            labels = Collections.unmodifiableMap(new LinkedHashMap<>(labels));
        }
    }

    /** @param draft the draft's devices, or {@code null} when no draft is started */
    public record Stored(List<StoredDevice> master, List<StoredDevice> draft) {
    }

    /**
     * A draft device to write, and the master device it belongs to by name - master ids are only known once the
     * master has been written.
     *
     * @param masterName the master device this draft device was copied from, or {@code null} for one the draft
     *                   added
     */
    public record DraftWrite(String masterName, StoredDevice device) {
    }

    /**
     * @param master  the whole master as it is to be. Ids are ignored: a device keeps its row when its name is
     *                already in the master, and gets a new one otherwise
     * @param removed devices leaving the master, recorded as pending removals - their entries are still in the
     *                other installations
     * @param draft   the whole draft as it is to be, or {@code null} to leave the draft alone
     */
    public record AdoptionWrite(List<StoredDevice> master, List<String> removed, List<DraftWrite> draft) {
    }

    /**
     * Adopts what an installation holds: reads the master and the draft, lets {@code plan} decide, and writes
     * both, in one transaction (Alan, 2026-10-05). A failure leaves the master and the draft as they were, and an
     * edit saved between the read and the write cannot be overwritten by a plan made without it.
     * <p>
     * Master devices that survive keep their rows and ids, so nothing else pointing at them is disturbed.
     *
     * @param plan returns what to write, or {@code null} to write nothing
     */
    public void adopt(Function<Stored, AdoptionWrite> plan) {
        inTransaction(daos -> {
            BindForgeDeviceMasterDao master = daos.master();
            BindForgeDeviceDraftDao draft = daos.draft();
            AdoptionWrite write = plan.apply(new Stored(storedMaster(master), storedDraft(draft)));
            if (write == null) return null;

            // WHY: the draft is emptied first, as promote does, so no draft row is left pointing at a master row
            // this adoption removes.
            if (write.draft() != null) draft.deleteAll();
            for (String name : write.removed()) {
                DeviceRow row = master.findByName(name);
                if (row != null) master.delete(row.id());
                master.addPendingRemoval(name);
            }
            for (StoredDevice device : write.master()) {
                DeviceRow row = master.findByName(device.deviceName());
                if (row == null) {
                    master.insert(device.deviceName(), device.vid(), device.pid(), device.aliasConfirmed());
                    row = master.findByName(device.deviceName());
                } else if (!Objects.equals(row.vid(), device.vid()) || !Objects.equals(row.pid(), device.pid())) {
                    master.retarget(row.id(), device.vid(), device.pid());
                }
                master.deleteLabels(row.id());
                long masterId = row.id();
                device.labels().forEach((token, label) -> master.putLabel(masterId, token, label));
            }
            if (write.draft() != null) {
                for (DraftWrite row : write.draft()) {
                    StoredDevice device = row.device();
                    Long masterId = row.masterName() == null ? null : master.findByName(row.masterName()).id();
                    draft.insert(masterId, device.deviceName(), device.vid(), device.pid(), device.aliasConfirmed(),
                            device.previousName());
                    long draftId = draft.findByName(device.deviceName()).id();
                    device.labels().forEach((token, label) -> draft.putLabel(draftId, token, label));
                }
            }
            return null;
        });
    }

    /**
     * Activation: the master is filled for the first time, and BindForge is activated, in one transaction - by
     * first setup (Alan, 2026-10-07), or with no prompt when the installations agree (Alan, 2026-10-13, A1b).
     * <p>
     * Only once - a BindForge already activated, or a master that filled meanwhile, is not overwritten. The master
     * may be filled with nothing: a user who owns nothing is still activated.
     * <strong>Any draft is thrown away</strong> in the same transaction: before activation it can hold no
     * edits of the user's, and left started it would replace the new master with its empty copy on the next
     * Apply.
     *
     * @param devices         the master as it is to be, possibly none. Ids are ignored
     * @param pendingRemovals names whose entries are in the installations but not the master
     * @return {@code false}, writing nothing, when BindForge was already activated or the master was not empty
     */
    public boolean establish(List<StoredDevice> devices, List<String> pendingRemovals) {
        return inTransaction(daos -> {
            BindForgeDeviceMasterDao master = daos.master();
            if (daos.activation().isActivated() || !master.findAll().isEmpty()) return false;
            daos.draft().deleteAll();
            daos.draft().clearStarted();
            for (StoredDevice device : devices) {
                master.insert(device.deviceName(), device.vid(), device.pid(), device.aliasConfirmed());
                long masterId = master.findByName(device.deviceName()).id();
                device.labels().forEach((token, label) -> master.putLabel(masterId, token, label));
                master.clearPendingRemoval(device.deviceName());
            }
            pendingRemovals.forEach(master::addPendingRemoval);
            daos.activation().activate();
            return true;
        });
    }

    private static List<StoredDevice> storedMaster(BindForgeDeviceMasterDao master) {
        List<StoredDevice> devices = new ArrayList<>();
        for (DeviceRow row : master.findAll()) {
            devices.add(new StoredDevice(row.id(), row.deviceName(), row.vid(), row.pid(), row.aliasConfirmed(),
                    row.previousName(), labelMap(master.labelsOf(row.id()))));
        }
        return devices;
    }

    private static List<StoredDevice> storedDraft(BindForgeDeviceDraftDao draft) {
        if (!draft.exists()) return null;
        List<StoredDevice> devices = new ArrayList<>();
        for (DraftRow row : draft.findAll()) {
            devices.add(new StoredDevice(row.masterId(), row.deviceName(), row.vid(), row.pid(), row.aliasConfirmed(),
                    row.previousName(), labelMap(draft.labelsOf(row.id()))));
        }
        return devices;
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
                    new Daos(h.attach(BindForgeDeviceDraftDao.class), h.attach(BindForgeDeviceMasterDao.class),
                            h.attach(BindForgeActivationDao.class))));
        }
    }

    private record Daos(BindForgeDeviceDraftDao draft, BindForgeDeviceMasterDao master,
                        BindForgeActivationDao activation) {
    }

    private record PromotedDevice(DraftRow row, Map<String, String> labels) {
    }
}
