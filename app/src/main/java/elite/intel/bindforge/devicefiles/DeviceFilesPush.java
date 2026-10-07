package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceMappingsMerge.UserEntry;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.db.managers.BindForgeDeviceMasterManager;
import elite.intel.db.managers.BindForgeInstallationsManager;
import elite.intel.db.managers.BindForgeSettingsManager;
import elite.intel.io.AtomicFiles;
import elite.intel.io.TimestampedBackups;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
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
 * Adds and updates only. Removing an entry or a {@code .buttonMap} - CLEAR, a rename, RESET LABELS - is not
 * done here.
 */
public final class DeviceFilesPush {

    private static final Logger log = LogManager.getLogger(DeviceFilesPush.class);


    /**
     * One installation as the push sees it.
     *
     * @param storefront     names the installation in a report
     * @param controlSchemes the folder holding its {@code DeviceMappings.xml}
     * @param missing        whether the stored list already knows its folder is gone
     */
    public record Target(long installId, String storefront, Path controlSchemes, boolean missing) {
    }

    /**
     * One device from the master.
     *
     * @param name   the element tag, and the {@code .buttonMap} filename stem
     * @param labels label by input token; empty when the device has none, in which case no {@code .buttonMap}
     *               is written for it
     */
    public record MasterDevice(String name, String vid, String pid, Map<String, String> labels) {
        public MasterDevice {
            Objects.requireNonNull(name, "name");
            labels = Map.copyOf(labels);
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
        FAILED
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
                    .filter(result -> result.outcome() == Outcome.WRITTEN || result.outcome() == Outcome.UNCHANGED)
                    .count();
        }
    }

    private final Supplier<List<Target>> targets;
    private final Supplier<List<MasterDevice>> master;
    private final FrontierStockDevices stock;
    private final TimestampedBackups backups;
    private final Path historyRoot;
    private final IntSupplier retention;
    private final Consumer<Target> afterWrite;

    /**
     * Pushes the stored master to the stored installations, keeping Edit History under Elite-Intel's own data
     * folder and re-reading each installation it writes, so the "matches the master" markers are right
     * straight away.
     *
     * @throws IOException if Elite-Intel's data folder cannot be created
     */
    public DeviceFilesPush() throws IOException {
        this(DeviceFilesPush::storedInstallations,
                DeviceFilesPush::storedMaster,
                FrontierStockDevices.getInstance(),
                new TimestampedBackups(),
                AppPaths.getBindingsWorkingDir().resolve("history"),
                BindForgeSettingsManager.getInstance()::getEditHistoryRetention,
                rescanWith(new InstallationDeviceScanner()));
    }

    DeviceFilesPush(Supplier<List<Target>> targets, Supplier<List<MasterDevice>> master,
                    FrontierStockDevices stock, TimestampedBackups backups, Path historyRoot,
                    IntSupplier retention, Consumer<Target> afterWrite) {
        this.targets = targets;
        this.master = master;
        this.stock = stock;
        this.backups = backups;
        this.historyRoot = historyRoot;
        this.retention = retention;
        this.afterWrite = afterWrite;
    }

    /** Pushes the master to every installation and reports what happened to each. */
    public Report push() {
        List<MasterDevice> devices = master.get();
        int keep = retention.getAsInt();
        Set<String> keptThisPush = new HashSet<>();
        List<InstallationResult> results = new ArrayList<>();
        for (Target target : targets.get()) {
            InstallationResult result = pushTo(target, devices, keep, keptThisPush);
            log.info("Device files for {} ({}): {}{}", target.storefront(), target.controlSchemes(),
                    result.outcome(), result.reason() == null ? "" : " - " + result.reason());
            results.add(result);
        }
        return new Report(results);
    }

    /**
     * Pushes the master to one installation: <strong>revert</strong>, for an installation the startup check
     * found wiped or edited (Alan, 2026-10-05). The same push as Apply, so it adds and updates only, and an entry
     * hand-added in that installation stays.
     *
     * @return the report, holding that installation alone - or nothing, when no stored installation has that id
     */
    public Report push(long installId) {
        List<MasterDevice> devices = master.get();
        List<InstallationResult> results = new ArrayList<>();
        for (Target target : targets.get()) {
            if (target.installId() != installId) continue;
            InstallationResult result = pushTo(target, devices, retention.getAsInt(), new HashSet<>());
            log.info("Device files for {} ({}), reverted to the master: {}{}", target.storefront(),
                    target.controlSchemes(), result.outcome(), result.reason() == null ? "" : " - " + result.reason());
            results.add(result);
        }
        return new Report(results);
    }

    private InstallationResult pushTo(Target target, List<MasterDevice> devices, int keep, Set<String> kept) {
        if (target.missing() || !Files.isDirectory(target.controlSchemes())) {
            return result(target, Outcome.SKIPPED_MISSING, "folder not found: " + target.controlSchemes(), false);
        }
        Plan plan;
        try {
            plan = plan(target, devices, stock);
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
        List<FileWrite> writes = new ArrayList<>();

        Path deviceMappings = GameInstallation.deviceMappingsIn(target.controlSchemes());
        byte[] current = readIfPresent(deviceMappings);
        boolean seeded = current == null;
        DeviceMappingsMerge.Result merged =
                DeviceMappingsMerge.merge(seeded ? stockFile() : current, userEntries(devices, stock));
        if (seeded || merged.changed()) writes.add(new FileWrite(deviceMappings, current, merged.content()));

        for (MasterDevice device : devices) {
            if (device.labels().isEmpty()) continue;
            Path buttonMap = GameInstallation.buttonMapIn(target.controlSchemes(), device.name());
            byte[] content = ButtonMapWriter.write(device.labels());
            byte[] previous = readIfPresent(buttonMap);
            if (!Arrays.equals(previous, content)) writes.add(new FileWrite(buttonMap, previous, content));
        }
        return new Plan(writes, seeded);
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
            if (write.previous() == null) continue;
            if (!kept.add(sha256(write.previous()))) continue;
            Path folder = historyRoot.resolve(String.valueOf(target.installId()));
            if (write.path().getFileName().toString().endsWith(GameInstallation.BUTTON_MAP_SUFFIX)) {
                folder = folder.resolve(GameInstallation.DEVICE_BUTTON_MAPS);
            }
            backups.create(write.path(), folder);
            backups.prune(folder, write.path().getFileName().toString(), keep);
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
                Files.createDirectories(write.path().getParent());
                AtomicFiles.write(write.path(), write.content());
                done.add(write);
            } catch (IOException e) {
                return "could not write " + write.path().getFileName() + ": " + e.getMessage() + restore(done);
            }
        }
        return null;
    }

    private static String restore(List<FileWrite> done) {
        StringBuilder notRestored = new StringBuilder();
        for (FileWrite write : done.reversed()) {
            try {
                if (write.previous() == null) {
                    Files.deleteIfExists(write.path());
                } else {
                    AtomicFiles.write(write.path(), write.previous());
                }
            } catch (IOException e) {
                log.error("Could not put {} back after a failed push", write.path(), e);
                notRestored.append("; and could not put back ").append(write.path().getFileName())
                        .append(": ").append(e.getMessage());
            }
        }
        return notRestored.toString();
    }

    private static InstallationResult result(Target target, Outcome outcome, String reason, boolean seeded) {
        return new InstallationResult(target, outcome, reason, seeded, List.of());
    }

    private static byte[] readIfPresent(Path file) throws IOException {
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

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required of every Java platform", e);
        }
    }

    static List<Target> storedInstallations() {
        return BindForgeInstallationsManager.getInstance().findAll().stream()
                .map(row -> new Target(row.id(), row.storefront(),
                        GameInstallation.controlSchemesUnder(Path.of(row.rootPath())), row.missing()))
                .toList();
    }

    static List<MasterDevice> storedMaster() {
        BindForgeDeviceMasterManager masterStore = BindForgeDeviceMasterManager.getInstance();
        List<MasterDevice> devices = new ArrayList<>();
        for (DeviceRow row : masterStore.findAll()) {
            devices.add(new MasterDevice(row.deviceName(), row.vid(), row.pid(), masterStore.labelsOf(row.id())));
        }
        return devices;
    }

    private static Consumer<Target> rescanWith(InstallationDeviceScanner scanner) {
        return target -> scanner.scan(target.installId(), target.controlSchemes());
    }

    /**
     * @param previous what the file held before, or {@code null} when it did not exist - which is also what
     *                 putting it back means
     */
    record FileWrite(Path path, byte[] previous, byte[] content) {
    }

    record Plan(List<FileWrite> writes, boolean seeded) {
    }
}
