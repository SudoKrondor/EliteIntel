package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.ElementChange;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.ElementKind;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.InstallationCheck;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.LabelChange;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.LabelKind;
import elite.intel.db.managers.BindForgeDeviceDraftManager;
import elite.intel.db.managers.BindForgeDeviceDraftManager.AdoptionWrite;
import elite.intel.db.managers.BindForgeDeviceDraftManager.DraftWrite;
import elite.intel.db.managers.BindForgeDeviceDraftManager.Stored;
import elite.intel.db.managers.BindForgeDeviceDraftManager.StoredDevice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Predicate;

/**
 * <strong>Adopt</strong>, for the device files: what one installation holds becomes the master. The way out of the
 * startup check for an installation edited in the game or by hand; the other is revert,
 * {@link DeviceFilesPush#push(long)}.
 * <p>
 * Settled 2026-10-05 (Alan) - overview.md, <em>What the startup check actually decides</em>:
 * <ul>
 *     <li><strong>Adopt changes the master, never a game file.</strong> The other installations stop matching it,
 *     and the user's next Apply brings them in line.</li>
 *     <li>An entry the installation no longer has <strong>leaves the master as a pending removal</strong>, since it
 *     is still in the others.</li>
 *     <li><strong>With a draft, device by device and label by label</strong> against the master before the adopt:
 *     what the draft left alone takes the adopted value, what only the draft changed keeps the draft's, and what
 *     both changed differently keeps the draft's and is reported.</li>
 *     <li><strong>A wiped installation is refused</strong>: adopting it would empty the master into Frontier's
 *     file. Its repair is revert.</li>
 * </ul>
 * From the divergence list it runs <strong>per device</strong>, {@link #adopt(long, String)}, and the label re-merge
 * is {@link #adoptLabels} (Alan, 2026-10-08 - alias-designer.md, <em>The ways out</em>).
 */
public final class DeviceFilesAdopt {

    private static final Logger log = LogManager.getLogger(DeviceFilesAdopt.class);

    public enum Outcome {
        /** The master now holds what the installation held. */
        ADOPTED,
        /** The installation already says what the master says, in everything adopt can take. */
        NOTHING_TO_ADOPT,
        /** Nothing was written; the reason says why. */
        REFUSED
    }

    public enum ConflictKind {
        /** The draft and the installation each changed the device's VID/PID, differently. */
        HARDWARE,
        /** The draft and the installation each changed one label, differently. */
        LABEL,
        /** The installation lost the device; the draft changed it. The draft keeps it. */
        REMOVED_IN_INSTALLATION,
        /** The draft removed the device; the installation changed it. It stays removed from the draft. */
        REMOVED_IN_DRAFT,
        /** The installation added a device under a name a draft device was renamed to. The draft keeps its own. */
        NAME_TAKEN
    }

    /**
     * Something the draft and the adopted installation both changed. The draft's value is kept, and the user
     * decides - adopt never picks a winner for an edit the user saved.
     *
     * @param inputToken the input, for {@link ConflictKind#LABEL}; {@code null} otherwise
     */
    public record Conflict(String deviceName, ConflictKind kind, String inputToken) {
    }

    /**
     * @param reason    why nothing was written, for {@link Outcome#REFUSED}; {@code null} otherwise
     * @param removed   devices that left the master, now pending removals
     * @param added     devices the master took from the installation
     * @param conflicts what the draft keeps against the adopted values
     */
    public record Result(Outcome outcome, String reason, List<String> removed, List<String> added,
                         List<Conflict> conflicts) {
        public Result {
            removed = List.copyOf(removed);
            added = List.copyOf(added);
            conflicts = List.copyOf(conflicts);
        }

        static Result refused(String reason) {
            return new Result(Outcome.REFUSED, reason, List.of(), List.of(), List.of());
        }
    }

    private final LongFunction<Optional<InstallationCheck>> check;
    private final Consumer<Function<Stored, AdoptionWrite>> store;

    /** Adopts from the stored installations into the stored master and draft. */
    public DeviceFilesAdopt() {
        this(new DeviceFilesCheck()::check, BindForgeDeviceDraftManager.getInstance()::adopt);
    }

    /**
     * @param check one installation against the master, read afresh
     * @param store runs the plan against the stored master and draft, and writes what it returns, in one
     *              transaction
     */
    DeviceFilesAdopt(LongFunction<Optional<InstallationCheck>> check, Consumer<Function<Stored, AdoptionWrite>> store) {
        this.check = check;
        this.store = store;
    }

    /** Adopts everything the installation differs from the master on - the startup check's way out. */
    public Result adopt(long installId) {
        return adopt(installId, name -> true);
    }

    /**
     * Adopts one device as the installation holds it - its element and its labels - and nothing else the
     * installation differs on. The way out of one row of the divergence list (Alan, 2026-10-08): adopting a whole
     * installation to fix one stick's PID would also drop from the master every entry that installation lacks.
     */
    public Result adopt(long installId, String deviceName) {
        Objects.requireNonNull(deviceName, "deviceName");
        return adopt(installId, deviceName::equals);
    }

    private Result adopt(long installId, Predicate<String> devices) {
        // WHY: the files are read again rather than taken from the check the user was shown. Adopt acts on the
        // installation as it is now; one edited again since is what gets adopted, not a picture of it.
        Optional<InstallationCheck> found = check.apply(installId);
        if (found.isEmpty()) return Result.refused("no such installation, or no master to adopt into");
        InstallationCheck installation = narrowed(found.get(), devices);

        switch (installation.state()) {
            case UNCHANGED -> {
                return new Result(Outcome.NOTHING_TO_ADOPT, null, List.of(), List.of(), List.of());
            }
            case WIPED -> {
                return Result.refused("its device files are Frontier's stock files or missing - revert it instead");
            }
            case MISSING, UNREADABLE -> {
                return Result.refused(installation.reason());
            }
            case EDITED -> {
            }
        }
        Optional<String> unreadable = installation.labels().stream()
                .filter(change -> change.kind() == LabelKind.FILE_UNREADABLE)
                .map(change -> change.deviceName() + ".buttonMap could not be read")
                .findFirst();
        if (unreadable.isPresent()) return Result.refused(unreadable.get());

        Result result = write(master -> adoptedMaster(master, installation), null);
        log.info("Adopted installation {} into the master: {}", installId, result);
        return result;
    }

    /**
     * The label re-merge: one master device's labels become {@code labels}, the answer the user gave in the merge
     * view with the master as one side. Like adopt it writes the master, and the draft three-way, and never a game
     * file - the installations then differ from the master until they are reverted (Alan, 2026-10-08).
     *
     * @param labels input token to label, the whole set the device is to hold
     */
    public Result adoptLabels(String deviceName, Map<String, String> labels) {
        Objects.requireNonNull(deviceName, "deviceName");
        Map<String, String> wanted = Map.copyOf(labels);
        Result result = write(master -> {
            if (master.stream().noneMatch(device -> device.deviceName().equals(deviceName))) return null;
            return master.stream()
                    .map(device -> device.deviceName().equals(deviceName)
                            ? new StoredDevice(device.masterId(), device.deviceName(), device.vid(), device.pid(),
                            device.aliasConfirmed(), device.previousName(), new TreeMap<>(wanted))
                            : device)
                    .toList();
        }, deviceName + " is not in the master");
        log.info("Re-merged the labels of {} into the master: {}", deviceName, result);
        return result;
    }

    /**
     * Plans against the stored master and draft and writes the result, in one transaction.
     *
     * @param adopting    the master as it is to be, from the master as it is; {@code null} to refuse
     * @param refusedWhen the reason given when {@code adopting} refuses
     */
    private Result write(Function<List<StoredDevice>, List<StoredDevice>> adopting, String refusedWhen) {
        Result[] result = {null};
        store.accept(stored -> {
            List<StoredDevice> adopted = adopting.apply(stored.master());
            if (adopted == null) {
                result[0] = Result.refused(refusedWhen);
                return null;
            }
            Set<String> before = names(stored.master());
            Set<String> after = names(adopted);
            List<String> removed = before.stream().filter(name -> !after.contains(name)).toList();
            List<String> added = after.stream().filter(name -> !before.contains(name)).toList();

            if (removed.isEmpty() && added.isEmpty() && sameDevices(stored.master(), adopted)) {
                result[0] = new Result(Outcome.NOTHING_TO_ADOPT, null, List.of(), List.of(), List.of());
                return null;
            }
            List<Conflict> conflicts = new ArrayList<>();
            List<DraftWrite> draft = stored.draft() == null
                    ? null
                    : mergeDraft(stored.master(), adopted, stored.draft(), conflicts);
            result[0] = new Result(Outcome.ADOPTED, null, removed, added, conflicts);
            return new AdoptionWrite(adopted, removed, draft);
        });
        return result[0];
    }

    /** The check with only the changes to the devices {@code devices} accepts; an UNCHANGED check stays as it is. */
    private static InstallationCheck narrowed(InstallationCheck installation, Predicate<String> devices) {
        if (installation.state() != DeviceFilesCheck.State.EDITED) return installation;
        List<ElementChange> elements = installation.elements().stream()
                .filter(change -> devices.test(change.deviceName())).toList();
        List<LabelChange> labels = installation.labels().stream()
                .filter(change -> devices.test(change.deviceName())).toList();
        return new InstallationCheck(installation.target(), installation.state(), installation.reason(), elements,
                labels, installation.fileGone());
    }

    /**
     * The master as it is to be: the stored master with the installation's changes taken in. Element order and
     * layout are not adopted - they are where Apply puts things, not something the user chose.
     */
    static List<StoredDevice> adoptedMaster(List<StoredDevice> master, InstallationCheck installation) {
        Map<String, ElementChange> elements = new LinkedHashMap<>();
        installation.elements().forEach(change -> elements.put(change.deviceName(), change));
        Map<String, List<LabelChange>> labels = new LinkedHashMap<>();
        installation.labels().forEach(change ->
                labels.computeIfAbsent(change.deviceName(), name -> new ArrayList<>()).add(change));

        List<StoredDevice> adopted = new ArrayList<>();
        for (StoredDevice device : master) {
            ElementChange element = elements.get(device.deviceName());
            if (element != null && element.kind() == ElementKind.MISSING) continue;
            String vid = device.vid();
            String pid = device.pid();
            if (element != null && element.kind() == ElementKind.HARDWARE_DIFFERS) {
                vid = element.installation().vid();
                pid = element.installation().pid();
            }
            adopted.add(new StoredDevice(device.masterId(), device.deviceName(), vid, pid, device.aliasConfirmed(),
                    device.previousName(), withChanges(device.labels(), labels.get(device.deviceName()))));
        }
        for (ElementChange element : elements.values()) {
            if (element.kind() != ElementKind.ADDED) continue;
            // WHY: confirmed, because the user wrote this name themselves and has just been shown it, by name,
            // in the change list they chose to adopt. The confirm gate exists so no device is configured under a
            // name nobody looked at - which this one is not.
            adopted.add(new StoredDevice(null, element.deviceName(), element.installation().vid(),
                    element.installation().pid(), true, null,
                    withChanges(Map.of(), labels.get(element.deviceName()))));
        }
        return adopted;
    }

    private static Map<String, String> withChanges(Map<String, String> labels, List<LabelChange> changes) {
        Map<String, String> result = new TreeMap<>(labels);
        if (changes == null) return result;
        for (LabelChange change : changes) {
            switch (change.kind()) {
                case FILE_MISSING -> result.clear();
                case ADDED, CHANGED -> result.put(change.inputToken(), change.installation());
                case REMOVED -> result.remove(change.inputToken());
                case FILE_UNREADABLE, LAYOUT_ONLY -> {
                }
            }
        }
        return result;
    }

    /**
     * The draft as it is to be, after the master moves from {@code base} to {@code adopted} underneath it.
     * Compared per device - paired through the master row a draft device was copied from, so a device renamed in
     * the draft is still the same device - and per label.
     */
    static List<DraftWrite> mergeDraft(List<StoredDevice> base, List<StoredDevice> adopted, List<StoredDevice> draft,
                                       List<Conflict> conflicts) {
        Map<Long, StoredDevice> adoptedById = new LinkedHashMap<>();
        List<StoredDevice> adoptedNew = new ArrayList<>();
        for (StoredDevice device : adopted) {
            if (device.masterId() == null) adoptedNew.add(device);
            else adoptedById.put(device.masterId(), device);
        }
        Map<Long, StoredDevice> draftByMasterId = new LinkedHashMap<>();
        Map<String, StoredDevice> draftAdded = new LinkedHashMap<>();
        for (StoredDevice device : draft) {
            if (device.masterId() == null) draftAdded.put(device.deviceName(), device);
            else draftByMasterId.put(device.masterId(), device);
        }

        List<DraftWrite> merged = new ArrayList<>();
        for (StoredDevice was : base) {
            StoredDevice nowAdopted = adoptedById.get(was.masterId());
            StoredDevice inDraft = draftByMasterId.get(was.masterId());

            if (nowAdopted == null) {
                if (inDraft == null || sameDevice(inDraft, was)) continue;
                conflicts.add(new Conflict(was.deviceName(), ConflictKind.REMOVED_IN_INSTALLATION, null));
                merged.add(new DraftWrite(null, inDraft));
            } else if (inDraft == null) {
                if (!sameDevice(nowAdopted, was)) {
                    conflicts.add(new Conflict(was.deviceName(), ConflictKind.REMOVED_IN_DRAFT, null));
                }
            } else {
                merged.add(new DraftWrite(nowAdopted.deviceName(), threeWay(was, inDraft, nowAdopted, conflicts)));
            }
        }

        Set<String> draftNames = new HashSet<>();
        draft.forEach(device -> draftNames.add(device.deviceName()));
        for (StoredDevice added : adoptedNew) {
            StoredDevice inDraft = draftAdded.remove(added.deviceName());
            if (inDraft != null) {
                StoredDevice none = new StoredDevice(null, added.deviceName(), null, null, false, null, Map.of());
                merged.add(new DraftWrite(added.deviceName(), threeWay(none, inDraft, added, conflicts)));
            } else if (draftNames.contains(added.deviceName())) {
                conflicts.add(new Conflict(added.deviceName(), ConflictKind.NAME_TAKEN, null));
            } else {
                merged.add(new DraftWrite(added.deviceName(), added));
            }
        }
        draftAdded.values().forEach(device -> merged.add(new DraftWrite(null, device)));
        return merged;
    }

    /**
     * One device the draft holds and the adopted master holds, against what the master held before. The draft
     * keeps its own name, confirmation and pending rename - adopt never renames anything.
     */
    private static StoredDevice threeWay(StoredDevice base, StoredDevice draft, StoredDevice adopted,
                                         List<Conflict> conflicts) {
        HardwareId hardware = pick(hardwareOf(base), hardwareOf(draft), hardwareOf(adopted),
                () -> conflicts.add(new Conflict(draft.deviceName(), ConflictKind.HARDWARE, null)));

        Set<String> tokens = new LinkedHashSet<>(base.labels().keySet());
        tokens.addAll(draft.labels().keySet());
        tokens.addAll(adopted.labels().keySet());
        Map<String, String> labels = new TreeMap<>();
        for (String token : tokens) {
            String label = pick(base.labels().get(token), draft.labels().get(token), adopted.labels().get(token),
                    () -> conflicts.add(new Conflict(draft.deviceName(), ConflictKind.LABEL, token)));
            if (label != null) labels.put(token, label);
        }
        // WHY: the draft's own spelling of the VID/PID when the pair did not move, so a value that differs only in
        // case is not rewritten for nothing.
        boolean keepDraftSpelling = Objects.equals(hardware, hardwareOf(draft));
        return new StoredDevice(draft.masterId(), draft.deviceName(),
                keepDraftSpelling ? draft.vid() : adopted.vid(),
                keepDraftSpelling ? draft.pid() : adopted.pid(),
                draft.aliasConfirmed(), draft.previousName(), labels);
    }

    /** The adopted value where the draft left it alone, otherwise the draft's - reporting when both moved apart. */
    private static <T> T pick(T base, T draft, T adopted, Runnable conflict) {
        if (Objects.equals(draft, base)) return adopted;
        if (!Objects.equals(adopted, base) && !Objects.equals(adopted, draft)) conflict.run();
        return draft;
    }

    private static HardwareId hardwareOf(StoredDevice device) {
        return device.vid() == null && device.pid() == null ? null : new HardwareId(device.vid(), device.pid());
    }

    private static boolean sameDevice(StoredDevice one, StoredDevice other) {
        return one.deviceName().equals(other.deviceName())
                && Objects.equals(hardwareOf(one), hardwareOf(other))
                && one.labels().equals(other.labels());
    }

    private static boolean sameDevices(List<StoredDevice> master, List<StoredDevice> adopted) {
        for (int i = 0; i < master.size(); i++) {
            if (!sameDevice(master.get(i), adopted.get(i))) return false;
        }
        return true;
    }

    private static Set<String> names(List<StoredDevice> devices) {
        Set<String> names = new LinkedHashSet<>();
        devices.forEach(device -> names.add(device.deviceName()));
        return names;
    }
}
