package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceMappingsMerge.UserEntry;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.db.managers.BindForgeDeviceMasterManager;
import elite.intel.db.managers.BindForgeInstallationsManager;
import elite.intel.io.AtomicFiles;
import elite.intel.io.TimestampedBackups;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Pushes the master device set out to every installation: Apply, for {@code DeviceMappings.xml} and the
 * {@code .buttonMap} files.
 * <p>
 * <strong>Reported per installation, and partial success is allowed</strong> (Alan, 2026-10-04). An
 * installation whose folder is gone, or whose files cannot be read or written, is left as it was and named with
 * the reason - <em>"updated 2 of 3 - Epic: folder not found"</em> - while the others are brought up to date.
 * Silent partial success is the drift this model exists to remove, so every installation gets a result.
 * <p>
 * <strong>One installation is all or nothing.</strong> Its {@code DeviceMappings.xml} and its {@code .buttonMap}
 * files change together: if one write fails, the ones already written in that installation are put back, so no
 * installation is left with a new entry and old labels.
 * <p>
 * <strong>Before a file is replaced, its content is kept in Edit History</strong> - unless identical content was
 * already kept during this push. After standardisation every installation holds the same files, so an entry per
 * installation would be several identical copies of one file.
 * <p>
 * <strong>Adds and updates; removes only what it is handed.</strong> The master alone cannot tell a cleared device
 * from an entry BindForge never owned, so {@link #push(Set)} takes the names to remove - Apply passes the pending
 * removals (Alan, 2026-10-11, A10). A user's name loses its element and its {@code .buttonMap}; one of Frontier's
 * names loses only the {@code .buttonMap}, and where Frontier shipped that file it is put back instead. Revert
 * removes nothing.
 */
public final class DeviceFilesPush implements DeviceApply.Pushes {

    private static final Logger log = LogManager.getLogger(DeviceFilesPush.class);


    /**
     * One installation as the push sees it.
     *
     * @param storefront     names the installation in a report
     * @param controlSchemes the folder holding its {@code DeviceMappings.xml}
     * @param missing        whether the stored list already knows its folder is gone
     * @param missedWrite    whether it missed a write the other installations took, and has not caught up (A14)
     */
    public record Target(long installId, String storefront, Path controlSchemes, boolean missing,
                         boolean missedWrite) {

        /** An installation that has missed no write. */
        public Target(long installId, String storefront, Path controlSchemes, boolean missing) {
            this(installId, storefront, controlSchemes, missing, false);
        }

        /**
         * Whether the installation can be read and written now: not known to be gone, and its folder is there.
         * <p>
         * One definition for the push, the check, the backup, first setup and generation - if they disagreed, a
         * backup and the write it protects could cover different installations.
         */
        public boolean isReachable() {
            return !missing && Files.isDirectory(controlSchemes);
        }
    }

    /**
     * One device from the master.
     *
     * @param name   the element tag, and the {@code .buttonMap} filename stem
     * @param labels       label by input token; empty when the device has none, in which case no {@code .buttonMap}
     *                     is written for it
     * @param previousName what the device was called before a rename still being carried out - the name some
     *                     installation may still hold - or {@code null}
     */
    public record MasterDevice(String name, String vid, String pid, Map<String, String> labels, String previousName) {
        public MasterDevice {
            Objects.requireNonNull(name, "name");
            labels = Map.copyOf(labels);
        }

        /** A device with no rename under way. */
        public MasterDevice(String name, String vid, String pid, Map<String, String> labels) {
            this(name, vid, pid, labels, null);
        }

        /** Whether a rename of this device is still being carried out - its old name may be in some installation. */
        boolean isRenamed() {
            return previousName != null && !previousName.equals(name);
        }
    }

    public enum Outcome {
        /** At least one file was written, and the installation now matches the master. */
        WRITTEN,
        /** It already matched the master; nothing was written. */
        UNCHANGED,
        /** Its folder is not there. Nothing was written. */
        SKIPPED_MISSING,
        /** A file could not be read, kept or written. The installation is as it was before the push. */
        FAILED;

        /**
         * Whether the installation now holds what was pushed. Asked rather than spelled out at each caller, so an
         * outcome added later is counted the same way everywhere - as a miss, until it says otherwise.
         */
        public boolean landed() {
            return this == WRITTEN || this == UNCHANGED;
        }
    }

    /**
     * @param reason          why the installation was skipped or failed, and {@code null} otherwise
     * @param seededFromStock its {@code DeviceMappings.xml} was missing and was created from Frontier's
     * @param written         the files written, in the order they were written
     */
    public record InstallationResult(Target target, Outcome outcome, String reason, boolean seededFromStock,
                                     List<Path> written) {
        public InstallationResult {
            written = List.copyOf(written);
        }
    }

    public record Report(List<InstallationResult> installations) {
        public Report {
            installations = List.copyOf(installations);
        }

        /** How many installations match the master after the push - the "2" in "updated 2 of 3". */
        public long matchingCount() {
            return installations.stream()
                    .filter(result -> result.outcome().landed())
                    .count();
        }
    }

    private final Supplier<List<Target>> targets;
    private final Supplier<List<MasterDevice>> master;
    private final FrontierStockDevices stock;
    private final EditHistory history;
    private final Consumer<Target> afterWrite;
    private final MissedWrites missedWrites;

    /**
     * Pushes the stored master to the stored installations, keeping Edit History under Elite-Intel's own data
     * folder and re-reading each installation it writes, so the "matches the master" markers are right
     * straight away.
     *
     * @throws IOException if Elite-Intel's data folder cannot be created
     */
    public DeviceFilesPush() throws IOException {
        this(DeviceFilesPush::storedInstallations, DeviceFilesPush::storedMaster, FrontierStockDevices.getInstance(),
                EditHistory.stored(), rescanWith(new InstallationDeviceScanner()), MissedWrites.stored());
    }

    DeviceFilesPush(Supplier<List<Target>> targets, Supplier<List<MasterDevice>> master,
                    FrontierStockDevices stock, TimestampedBackups backups, Path historyRoot,
                    IntSupplier retention, Consumer<Target> afterWrite) {
        this(targets, master, stock, backups, historyRoot, retention, afterWrite, MissedWrites.NONE);
    }

    DeviceFilesPush(Supplier<List<Target>> targets, Supplier<List<MasterDevice>> master,
                    FrontierStockDevices stock, TimestampedBackups backups, Path historyRoot,
                    IntSupplier retention, Consumer<Target> afterWrite, MissedWrites missedWrites) {
        this(targets, master, stock, new EditHistory(backups, historyRoot, retention), afterWrite, missedWrites);
    }

    private DeviceFilesPush(Supplier<List<Target>> targets, Supplier<List<MasterDevice>> master,
                            FrontierStockDevices stock, EditHistory history, Consumer<Target> afterWrite,
                            MissedWrites missedWrites) {
        this.targets = targets;
        this.master = master;
        this.stock = stock;
        this.history = history;
        this.afterWrite = afterWrite;
        this.missedWrites = missedWrites;
    }

    /** Pushes the master to every installation, removing nothing, and reports what happened to each. */
    public Report push() {
        return push(Set.of());
    }

    /**
     * Apply: pushes the master to every installation and removes the devices named, in the same per-installation
     * unit - so an installation's removals and its updates land together or not at all.
     *
     * @param removals devices to take out of every installation. The caller makes sure none is in the master and
     *                 none is still named by {@code .binds}
     */
    @Override
    public Report push(Set<String> removals) {
        return pushWhere(target -> true, removals, "", true);
    }

    /**
     * <strong>Catch-up</strong>: Apply's push, to the installations named only (Alan, 2026-10-15, A14) - the master,
     * the renames and the removals, for an installation that missed them and is found again. The others are left
     * alone: one edited in the game meanwhile is not this question's to revert.
     *
     * @param removals as {@link #push(Set)}: none in the master, none still named by {@code .binds}
     */
    @Override
    public Report push(Collection<Long> installIds, Set<String> removals) {
        return pushWhere(target -> installIds.contains(target.installId()), removals, ", caught up with the master",
                true);
    }

    /**
     * Pushes the master to one installation: <strong>revert</strong>, for an installation the startup check
     * found wiped or edited (Alan, 2026-10-05). The same push as Apply, so it adds and updates only, and an entry
     * hand-added in that installation stays.
     *
     * @return the report, holding that installation alone - or nothing, when no stored installation has that id
     */
    public Report push(long installId) {
        // WHY: not carrying everything - a miss marks the installation, but landing does not clear it. Revert removes
        // nothing, so it can land on an installation that still holds a device the others lost to a CLEAR.
        return pushWhere(target -> target.installId() == installId, Set.of(), ", reverted to the master", false);
    }

    /**
     * The one push every kind runs through - Apply, catch-up and revert - so a rule for every push is written once.
     *
     * @param logTag            what the log line says this push was, after the installation
     * @param carriesEverything whether it carries all Apply does, so landing means the installation has caught up
     *                          ({@link MissedWrites})
     */
    private Report pushWhere(Predicate<Target> which, Set<String> removals, String logTag,
                             boolean carriesEverything) {
        List<MasterDevice> devices = master.get();
        int keep = history.retention();
        Set<String> keptThisPush = new HashSet<>();
        List<InstallationResult> results = new ArrayList<>();
        for (Target target : targets.get()) {
            if (!which.test(target)) continue;
            InstallationResult result = pushTo(target, devices, removals, keep, keptThisPush);
            log.info("Device files for {} ({}){}: {}{}", target.storefront(), target.controlSchemes(), logTag,
                    result.outcome(), result.reason() == null ? "" : " - " + result.reason());
            results.add(result);
        }
        Report report = new Report(results);
        missedWrites.afterPush(report, carriesEverything);
        return report;
    }

    private InstallationResult pushTo(Target target, List<MasterDevice> devices, Set<String> removals, int keep,
                                      Set<String> kept) {
        if (!target.isReachable()) {
            return result(target, Outcome.SKIPPED_MISSING, "folder not found: " + target.controlSchemes(), false);
        }
        Plan plan;
        try {
            plan = plan(target, devices, removals, stock, FrontierStockButtonMaps.getInstance());
        } catch (IOException e) {
            return result(target, Outcome.FAILED, "could not read its device files: " + e.getMessage(), false);
        }
        if (plan.writes().isEmpty()) return result(target, Outcome.UNCHANGED, null, false);

        // WHY: the history copies are taken before anything is written, and a failure to take one stops the
        // installation. A write that could not be walked back is exactly what Edit History exists to prevent.
        try {
            keepHistory(target, plan.writes(), keep, kept);
        } catch (IOException e) {
            return result(target, Outcome.FAILED, "could not keep an Edit History copy: " + e.getMessage(),
                    plan.seeded());
        }
        String failure = writeAllOrRestore(plan.writes());
        if (failure != null) return result(target, Outcome.FAILED, failure, plan.seeded());

        afterWrite.accept(target);
        List<Path> written = plan.writes().stream().map(FileWrite::path).toList();
        return new InstallationResult(target, Outcome.WRITTEN, null, plan.seeded(), written);
    }

    /**
     * Everything the installation needs written, worked out before anything is touched.
     * <p>
     * Also what the startup check means by "matches the master": no writes. One definition, so the check and
     * Apply cannot disagree about whether an installation is up to date.
     */
    static Plan plan(Target target, List<MasterDevice> devices, FrontierStockDevices stock) throws IOException {
        return plan(target, devices, Set.of(), stock, FrontierStockButtonMaps.getInstance());
    }

    /** As {@link #plan(Target, List, FrontierStockDevices)}, with the devices Apply removes. */
    static Plan plan(Target target, List<MasterDevice> devices, Set<String> removals, FrontierStockDevices stock,
                     FrontierStockButtonMaps stockMaps) throws IOException {
        List<FileWrite> writes = new ArrayList<>();

        Path deviceMappings = GameInstallation.deviceMappingsIn(target.controlSchemes());
        byte[] current = readIfPresent(deviceMappings);
        boolean seeded = current == null;
        byte[] mappings = seeded ? stockFile() : current;
        boolean changed = false;
        // WHY: renamed in place before the merge, so the element keeps every child it has. The merge alone would add
        // a bare element under the new name and leave the old one, and its <Alternative> pairs, behind it.
        for (MasterDevice device : renamedUserDevices(devices, stock)) {
            DeviceMappingsMerge.Result renamed = DeviceMappingsMerge.rename(mappings, device.previousName(),
                    device.name());
            mappings = renamed.content();
            changed |= renamed.changed();
        }
        DeviceMappingsMerge.Result merged = DeviceMappingsMerge.merge(mappings, userEntries(devices, stock));
        mappings = merged.content();
        changed |= merged.changed();
        // WHY: one of Frontier's names keeps its element. On a built-in the element is Frontier's definition - RESET
        // LABELS took the labels away, never the entry.
        Set<String> userRemovals = new TreeSet<>();
        for (String name : removals) {
            if (!stock.ships(name)) userRemovals.add(name);
        }
        if (!userRemovals.isEmpty()) {
            DeviceMappingsMerge.Result removed = DeviceMappingsMerge.remove(mappings, userRemovals);
            mappings = removed.content();
            changed |= removed.changed();
        }
        if (seeded || changed) writes.add(new FileWrite(deviceMappings, current, mappings));

        for (MasterDevice device : devices) {
            if (device.labels().isEmpty()) continue;
            Path buttonMap = buttonMapOf(target, device.name());
            byte[] content = ButtonMapWriter.write(device.labels());
            byte[] previous = readIfPresent(buttonMap);
            if (!Arrays.equals(previous, content)) writes.add(new FileWrite(buttonMap, previous, content));
        }
        for (MasterDevice device : renamedUserDevices(devices, stock)) {
            writes.addAll(renameButtonMap(target, device));
        }
        for (String name : new TreeSet<>(removals)) {
            FileWrite write = removeButtonMap(target, name, stockMaps);
            if (write != null) writes.add(write);
        }
        return new Plan(writes, seeded);
    }

    /**
     * The {@code .buttonMap} the master writes for a device.
     *
     * @throws IOException when the name cannot be a filename inside {@code DeviceButtonMaps} - an XML tag may hold a
     *                     colon, which Windows refuses. Checked, so the push reports that installation as failed and
     *                     goes on to the next, and the startup check reports it unreadable, rather than either stopping
     */
    private static Path buttonMapOf(Target target, String name) throws IOException {
        try {
            return GameInstallation.buttonMapIn(target.controlSchemes(), name);
        } catch (InvalidPathException e) {
            throw new IOException("\"" + name + "\" cannot be a filename for its .buttonMap", e);
        }
    }

    /**
     * A rename's {@code .buttonMap}: the file under the old name goes, so none is left where nothing would ever clean it
     * up (Alan, 2026-10-14, A11). The master's labels are written under the new name above; a device the master holds
     * no labels for takes the old file's own content to the new name instead - renamed, never copied.
     * <p>
     * <strong>A rename that changes only case</strong> on a file system that ignores it - Windows - finds the old and
     * the new name to be one file. Then nothing is deleted: the file is the device's, under either spelling, and the
     * game finds it so (format §4.3, measured 2026-10-14).
     *
     * @return the writes - none when the installation holds no file under the old name
     */
    private static List<FileWrite> renameButtonMap(Target target, MasterDevice device) throws IOException {
        Path oldMap;
        try {
            oldMap = GameInstallation.buttonMapIn(target.controlSchemes(), device.previousName());
        } catch (InvalidPathException e) {
            // WHY: an old name that cannot be a filename has no file to rename.
            return List.of();
        }
        byte[] oldContent = readIfPresent(oldMap);
        if (oldContent == null) return List.of();
        Path newMap = buttonMapOf(target, device.name());
        if (Files.exists(newMap) && Files.isSameFile(oldMap, newMap)) return List.of();

        List<FileWrite> writes = new ArrayList<>();
        if (device.labels().isEmpty() && !Files.exists(newMap)) {
            writes.add(new FileWrite(newMap, null, oldContent));
        }
        writes.add(new FileWrite(oldMap, oldContent, null));
        return writes;
    }

    /**
     * The master's devices of the user's whose rename is still being carried out. A built-in is never among them:
     * renaming one of Frontier's elements is slice A15's, and its {@code .buttonMap} is not renamed here either.
     */
    private static List<MasterDevice> renamedUserDevices(List<MasterDevice> devices, FrontierStockDevices stock) {
        return devices.stream()
                .filter(MasterDevice::isRenamed)
                .filter(device -> !stock.ships(device.name()) && !stock.ships(device.previousName()))
                .toList();
    }

    /**
     * What removing a device does to its {@code .buttonMap}: the file is deleted, unless Frontier ships one under
     * that name - {@code VPCPanel}, {@code VPCThrottle} - which is put back as Frontier shipped it, since a file
     * Frontier shipped is never removed (Alan, 2026-10-11).
     *
     * @return the write, or {@code null} when the file is already as it should be
     */
    private static FileWrite removeButtonMap(Target target, String name, FrontierStockButtonMaps stockMaps)
            throws IOException {
        Path buttonMap;
        try {
            buttonMap = GameInstallation.buttonMapIn(target.controlSchemes(), name);
        } catch (InvalidPathException e) {
            // WHY: a name that cannot be a filename in DeviceButtonMaps has no file there to remove.
            return null;
        }
        byte[] previous = readIfPresent(buttonMap);
        Optional<byte[]> shipped = stockMaps.shippedContent(name);
        if (shipped.isPresent()) {
            return Arrays.equals(previous, shipped.get()) ? null : new FileWrite(buttonMap, previous, shipped.get());
        }
        return previous == null ? null : new FileWrite(buttonMap, previous, null);
    }

    /**
     * The elements the merge places. A master device under one of Frontier's names is left out: an unrenamed
     * built-in carries only labels, and its element is Frontier's definition - {@code <GamePad>} covers eighty
     * controllers, and writing one controller's VID/PID over its primary pair would break the rest. Its
     * {@code .buttonMap} is still written.
     */
    private static List<UserEntry> userEntries(List<MasterDevice> devices, FrontierStockDevices stock) {
        return devices.stream()
                .filter(device -> !stock.ships(device.name()))
                .map(device -> new UserEntry(device.name(), device.vid(), device.pid()))
                .toList();
    }

    private void keepHistory(Target target, List<FileWrite> writes, int keep, Set<String> kept) throws IOException {
        for (FileWrite write : writes) {
            history.keep(target.installId(), write.path(), write.previous(), keep, kept);
        }
    }

    /**
     * Writes every file, or puts back the ones already written and says why.
     *
     * @return {@code null} when every write landed, otherwise what failed - and anything that could not be put
     *         back, which leaves that installation needing attention
     */
    private static String writeAllOrRestore(List<FileWrite> writes) {
        List<FileWrite> done = new ArrayList<>();
        for (FileWrite write : writes) {
            try {
                if (write.content() == null) {
                    Files.deleteIfExists(write.path());
                } else {
                    Files.createDirectories(write.path().getParent());
                    AtomicFiles.write(write.path(), write.content());
                }
                done.add(write);
            } catch (IOException e) {
                return (write.content() == null ? "could not remove " : "could not write ")
                        + write.path().getFileName() + ": " + e.getMessage() + restore(done);
            }
        }
        return null;
    }

    /**
     * Puts back, newest first, writes that already landed - each file restored to what it held, or removed when it
     * did not exist.
     *
     * @return what could not be put back, worded for a report; empty when everything was
     */
    static String restore(List<FileWrite> done) {
        StringBuilder notRestored = new StringBuilder();
        for (FileWrite write : done.reversed()) {
            try {
                if (write.previous() == null) {
                    Files.deleteIfExists(write.path());
                } else {
                    AtomicFiles.write(write.path(), write.previous());
                }
            } catch (IOException e) {
                log.error("Could not put {} back after a failed write", write.path(), e);
                notRestored.append("; and could not put back ").append(write.path().getFileName())
                        .append(": ").append(e.getMessage());
            }
        }
        return notRestored.toString();
    }

    private static InstallationResult result(Target target, Outcome outcome, String reason, boolean seeded) {
        return new InstallationResult(target, outcome, reason, seeded, List.of());
    }

    /**
     * A file's bytes, or {@code null} when there is no such file.
     */
    static byte[] readIfPresent(Path file) throws IOException {
        return Files.isRegularFile(file) ? Files.readAllBytes(file) : null;
    }

    // WHY: a missing resource throws rather than seeding an empty file. A DeviceMappings.xml without
    // Frontier's entries leaves every Frontier-recognised controller unresolvable, so a packaging mistake must
    // be loud - the same reason FrontierStockDevices refuses to load without it.
    static byte[] stockFile() throws IOException {
        try (InputStream xml = DeviceFilesPush.class.getResourceAsStream(FrontierStockDevices.RESOURCE)) {
            if (xml == null) {
                throw new IllegalStateException("Frontier stock reference is missing from the jar: "
                        + FrontierStockDevices.RESOURCE);
            }
            return xml.readAllBytes();
        }
    }

    static List<Target> storedInstallations() {
        return BindForgeInstallationsManager.getInstance().findAll().stream()
                .map(row -> new Target(row.id(), row.storefront(),
                        GameInstallation.controlSchemesUnder(Path.of(row.rootPath())), row.missing(),
                        row.missedWrite()))
                .toList();
    }

    static List<MasterDevice> storedMaster() {
        BindForgeDeviceMasterManager masterStore = BindForgeDeviceMasterManager.getInstance();
        List<MasterDevice> devices = new ArrayList<>();
        for (DeviceRow row : masterStore.findAll()) {
            devices.add(new MasterDevice(row.deviceName(), row.vid(), row.pid(), masterStore.labelsOf(row.id()),
                    row.previousName()));
        }
        return devices;
    }

    private static Consumer<Target> rescanWith(InstallationDeviceScanner scanner) {
        return target -> scanner.scan(target.installId(), target.controlSchemes());
    }

    /**
     * @param previous what the file held before, or {@code null} when it did not exist - which is also what
     *                 putting it back means
     * @param content  what to write, or {@code null} to remove the file
     */
    record FileWrite(Path path, byte[] previous, byte[] content) {
    }

    record Plan(List<FileWrite> writes, boolean seeded) {
    }
}
