package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.MasterDevice;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.db.managers.BindForgeDeviceMasterManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The startup check, for the device files: each installation's {@code DeviceMappings.xml} and {@code .buttonMap}
 * files against the <strong>saved master</strong> - never the draft, which is unsaved intent rather than what was
 * pushed.
 * <p>
 * <strong>Each outcome is detected exactly, never scored</strong> (overview, 2026-09-24). Unchanged means Apply
 * would write nothing; wiped means Frontier's stock file or no file at all; anything else is an edit, reported as
 * the changes themselves so the user can judge them. Settled for the device files 2026-10-05 (Alan) - see
 * overview.md, <em>What the startup check actually decides</em>.
 * <p>
 * Reads only. The ways out are {@link DeviceFilesAdopt} and {@link DeviceFilesPush#push(long)}, and neither runs
 * on its own.
 */
public final class DeviceFilesCheck {

    private static final Logger log = LogManager.getLogger(DeviceFilesCheck.class);


    public enum State {
        /** Apply would write nothing: the installation holds what the master says. */
        UNCHANGED,
        /** Its {@code DeviceMappings.xml} is Frontier's stock file, or is gone. The repair is revert. */
        WIPED,
        /** Anything else: edited in the game or by hand. Adopt or revert, never automatically. */
        EDITED,
        /** Its folder is not there. Nothing could be compared. */
        MISSING,
        /** Its {@code DeviceMappings.xml} could not be read. Nothing could be compared. */
        UNREADABLE
    }

    public enum ElementKind {
        /** The master holds the element; the installation does not. */
        MISSING,
        /** The installation holds an element of the user's that the master does not. */
        ADDED,
        /** Both hold it, with a different primary VID/PID. */
        HARDWARE_DIFFERS,
        /** Both hold it as the master says, but not leading the file - so a Frontier entry for the same hardware wins. */
        OUT_OF_PLACE
    }

    /**
     * One element that differs. Frontier's entries are never reported: they are not the master's to compare.
     *
     * @param master       the master's primary pair, or {@code null} for an element only the installation has
     * @param installation the installation's primary pair, or {@code null} for one it lacks
     */
    public record ElementChange(String deviceName, ElementKind kind, HardwareId master, HardwareId installation) {
    }

    public enum LabelKind {
        /** The master has labels for the device; the installation has no {@code .buttonMap} for it. */
        FILE_MISSING,
        /** The {@code .buttonMap} is there and is not well-formed, so its labels are unknown. */
        FILE_UNREADABLE,
        /** A label the installation has and the master does not. */
        ADDED,
        /** A label the master has and the installation does not. */
        REMOVED,
        /** Both have a label for the input, and they differ. */
        CHANGED,
        /** Every label agrees; only the file's layout differs from what Apply writes. */
        LAYOUT_ONLY
    }

    /**
     * One label, or one whole {@code .buttonMap}, that differs.
     *
     * @param inputToken   the input, or {@code null} for a change to the whole file
     * @param master       the master's label, or {@code null}
     * @param installation the installation's label, or {@code null}
     */
    public record LabelChange(String deviceName, LabelKind kind, String inputToken, String master,
                              String installation) {
    }

    /**
     * @param reason    why nothing could be compared, for {@link State#MISSING} and {@link State#UNREADABLE};
     *                  {@code null} otherwise
     * @param fileGone  for {@link State#WIPED}: the installation has no {@code DeviceMappings.xml} at all, rather
     *                  than Frontier's. The game then loads no bindings, where with Frontier's file it loses only
     *                  the user's entries - so the divergence list colours the two differently (Alan, 2026-10-08)
     */
    public record InstallationCheck(Target target, State state, String reason, List<ElementChange> elements,
                                    List<LabelChange> labels, boolean fileGone) {
        public InstallationCheck {
            elements = List.copyOf(elements);
            labels = List.copyOf(labels);
        }

        /** Any check but a wipe that found no file. */
        public InstallationCheck(Target target, State state, String reason, List<ElementChange> elements,
                                 List<LabelChange> labels) {
            this(target, state, reason, elements, labels, false);
        }
    }

    /**
     * @param setUp       whether there is a master to compare against. Without one, first setup has not run, and
     *                    nothing is compared - every custom entry would read as an edit, and nothing is forced on a
     *                    user who has not asked (Alan, 2026-10-05)
     * @param masterNames every device name in the master the installations were compared against
     * @param userNames   those of them Frontier does not ship - what a reset to Frontier's file loses
     */
    public record Report(boolean setUp, Set<String> masterNames, Set<String> userNames,
                         List<InstallationCheck> installations) {
        public Report {
            masterNames = Set.copyOf(masterNames);
            userNames = Set.copyOf(userNames);
            installations = List.copyOf(installations);
        }
    }

    private final Supplier<List<Target>> targets;
    private final Supplier<List<MasterDevice>> master;
    private final Supplier<Set<String>> expectedLeftovers;
    private final FrontierStockDevices stock;

    /** Checks the stored installations against the stored master. */
    public DeviceFilesCheck() {
        this(DeviceFilesPush::storedInstallations,
                DeviceFilesPush::storedMaster,
                DeviceFilesCheck::storedLeftovers,
                FrontierStockDevices.getInstance());
    }

    /**
     * @param expectedLeftovers element names expected to linger in an installation after an Apply, which are not
     *                          the user adding anything: pending removals, and the old name of a rename not yet
     *                          carried out. Apply never removes, so these stay until something does
     */
    DeviceFilesCheck(Supplier<List<Target>> targets, Supplier<List<MasterDevice>> master,
                     Supplier<Set<String>> expectedLeftovers, FrontierStockDevices stock) {
        this.targets = targets;
        this.master = master;
        this.expectedLeftovers = expectedLeftovers;
        this.stock = stock;
    }

    /** Every installation against the master. */
    public Report check() {
        List<MasterDevice> devices = master.get();
        if (devices.isEmpty()) return new Report(false, Set.of(), Set.of(), List.of());

        Set<String> leftovers = expectedLeftovers.get();
        List<InstallationCheck> results = new ArrayList<>();
        for (Target target : targets.get()) {
            InstallationCheck result = checkOne(target, devices, leftovers);
            // WHY: DEBUG when nothing changed. The Alias Designer screen runs this check on every refresh, which
            // includes every ship-profile change, and a line per matching installation each time says nothing.
            Level level = result.state() == State.UNCHANGED ? Level.DEBUG : Level.INFO;
            log.log(level, "Device files for {} ({}) against the master: {}{}", target.storefront(),
                    target.controlSchemes(), result.state(), result.reason() == null ? "" : " - " + result.reason());
            results.add(result);
        }
        Set<String> masterNames = new LinkedHashSet<>();
        devices.forEach(device -> masterNames.add(device.name()));
        Set<String> userNames = new LinkedHashSet<>();
        masterNames.stream().filter(name -> !stock.ships(name)).forEach(userNames::add);
        return new Report(true, masterNames, userNames, results);
    }

    /**
     * One installation against the master - what adopt re-reads, so it acts on the files as they are now rather
     * than as they were when the user was shown them.
     *
     * @return empty when no stored installation has that id, or when there is no master
     */
    public Optional<InstallationCheck> check(long installId) {
        List<MasterDevice> devices = master.get();
        if (devices.isEmpty()) return Optional.empty();
        return targets.get().stream()
                .filter(target -> target.installId() == installId)
                .findFirst()
                .map(target -> checkOne(target, devices, expectedLeftovers.get()));
    }

    private InstallationCheck checkOne(Target target, List<MasterDevice> devices, Set<String> leftovers) {
        if (!target.isReachable()) {
            return nothingCompared(target, State.MISSING, "folder not found: " + target.controlSchemes());
        }
        Path deviceMappings = GameInstallation.deviceMappingsIn(target.controlSchemes());
        boolean pushWouldWrite;
        byte[] current;
        Map<String, HardwareId> inFile;
        try {
            pushWouldWrite = !DeviceFilesPush.plan(target, devices, stock).writes().isEmpty();
            current = readIfPresent(deviceMappings);
            inFile = current == null ? Map.of() : DeviceMappingsParser.primaries(current);
        } catch (IOException e) {
            return nothingCompared(target, State.UNREADABLE, "could not read its device files: " + e.getMessage());
        }

        List<String> userNames = devices.stream().map(MasterDevice::name).filter(name -> !stock.ships(name)).toList();
        List<ElementChange> elements = elementChanges(devices, userNames, inFile, leftovers);
        // WHY: "Apply would write nothing" is not enough on its own. Apply never removes, so an entry the user
        // added by hand leaves the push with nothing to do - and the installation still differs from the master.
        // With the push idle, the only element change left to find is such an addition.
        if (!pushWouldWrite && elements.isEmpty()) {
            return new InstallationCheck(target, State.UNCHANGED, null, List.of(), List.of());
        }
        List<LabelChange> labels = labelChanges(target, devices, elements);
        return new InstallationCheck(target, stateOf(current, userNames), null, elements, labels, current == null);
    }

    /**
     * WIPED when the file is gone, or is byte-for-byte Frontier's and the master puts elements of the user's into
     * it. A master holding only labels on Frontier's own entries expects the stock file, so for it the stock file
     * is not a wipe.
     * <p>
     * Byte identity only (Alan, 2026-10-05). The file carries no version, so a newer stock file after an update
     * reads as EDITED with every user entry listed as missing - which fails safe, since nothing is adopted on its
     * own.
     */
    private static State stateOf(byte[] current, List<String> userNames) {
        if (current == null) return State.WIPED;
        try {
            if (!userNames.isEmpty() && Arrays.equals(current, DeviceFilesPush.stockFile())) return State.WIPED;
        } catch (IOException e) {
            // WHY: the stock file travels in the jar, and DeviceFilesPush.stockFile() throws rather than return
            // nothing when it is absent. A read error here is the jar itself failing, so the honest answer is the
            // one that asks the user rather than the one that offers a repair.
            log.warn("Could not read Frontier's stock DeviceMappings.xml to test for a wipe: {}", e.getMessage());
        }
        return State.EDITED;
    }

    private List<ElementChange> elementChanges(List<MasterDevice> devices, List<String> userNames,
                                               Map<String, HardwareId> inFile, Set<String> leftovers) {
        List<ElementChange> changes = new ArrayList<>();
        Set<String> changed = new HashSet<>();
        Map<String, HardwareId> masterHardware = new LinkedHashMap<>();
        devices.forEach(device -> masterHardware.put(device.name(), new HardwareId(device.vid(), device.pid())));

        for (String name : userNames) {
            HardwareId wanted = masterHardware.get(name);
            HardwareId found = inFile.get(name);
            if (found == null) {
                changes.add(new ElementChange(name, ElementKind.MISSING, wanted, null));
                changed.add(name);
            } else if (!found.equals(wanted)) {
                changes.add(new ElementChange(name, ElementKind.HARDWARE_DIFFERS, wanted, found));
                changed.add(name);
            }
        }

        // WHY: the same test the push makes - the user's elements, in the master's order, are the file's first
        // elements. An element below Frontier's entry for the same VID/PID is never the one the game uses.
        List<String> present = userNames.stream().filter(inFile::containsKey).toList();
        List<String> fileOrder = List.copyOf(inFile.keySet());
        for (int i = 0; i < present.size(); i++) {
            String name = present.get(i);
            if (!name.equals(fileOrder.get(i)) && !changed.contains(name)) {
                changes.add(new ElementChange(name, ElementKind.OUT_OF_PLACE, masterHardware.get(name), inFile.get(name)));
            }
        }

        inFile.forEach((name, hardware) -> {
            if (stock.ships(name) || masterHardware.containsKey(name) || leftovers.contains(name)) return;
            changes.add(new ElementChange(name, ElementKind.ADDED, null, hardware));
        });
        return changes;
    }

    /**
     * Labels for every master device the push writes a {@code .buttonMap} for, and for every element the
     * installation added - adopting one takes its labels with it.
     */
    private static List<LabelChange> labelChanges(Target target, List<MasterDevice> devices,
                                                  List<ElementChange> elements) {
        List<LabelChange> changes = new ArrayList<>();

        for (MasterDevice device : devices) {
            // WHY: a device with no labels gets no file from the push, so whatever sits under its name - often
            // Frontier's own map - is not the master's to compare.
            if (device.labels().isEmpty()) continue;
            Path buttonMap = GameInstallation.buttonMapIn(target.controlSchemes(), device.name());
            changes.addAll(compareLabels(device.name(), device.labels(), buttonMap));
        }
        for (ElementChange element : elements) {
            if (element.kind() != ElementKind.ADDED) continue;
            Path buttonMap = GameInstallation.buttonMapIn(target.controlSchemes(), element.deviceName());
            if (Files.isRegularFile(buttonMap)) changes.addAll(compareLabels(element.deviceName(), Map.of(), buttonMap));
        }
        return changes;
    }

    private static List<LabelChange> compareLabels(String device, Map<String, String> wanted, Path buttonMap) {
        byte[] content;
        Map<String, String> found;
        try {
            content = readIfPresent(buttonMap);
            if (content == null) return List.of(new LabelChange(device, LabelKind.FILE_MISSING, null, null, null));
            if (!wanted.isEmpty() && Arrays.equals(content, ButtonMapWriter.write(wanted))) return List.of();
            found = ButtonMapReader.read(content);
        } catch (IOException e) {
            log.warn("Could not read {}: {}", buttonMap, e.getMessage());
            return List.of(new LabelChange(device, LabelKind.FILE_UNREADABLE, null, null, null));
        }

        List<LabelChange> changes = new ArrayList<>();
        Set<String> tokens = new LinkedHashSet<>(wanted.keySet());
        tokens.addAll(found.keySet());
        for (String token : tokens) {
            String master = wanted.get(token);
            String installation = found.get(token);
            if (Objects.equals(master, installation)) continue;
            LabelKind kind = master == null ? LabelKind.ADDED : installation == null ? LabelKind.REMOVED : LabelKind.CHANGED;
            changes.add(new LabelChange(device, kind, token, master, installation));
        }
        if (changes.isEmpty() && !wanted.isEmpty()) changes.add(new LabelChange(device, LabelKind.LAYOUT_ONLY, null, null, null));
        return changes;
    }

    private static InstallationCheck nothingCompared(Target target, State state, String reason) {
        return new InstallationCheck(target, state, reason, List.of(), List.of());
    }

    private static byte[] readIfPresent(Path file) throws IOException {
        return Files.isRegularFile(file) ? Files.readAllBytes(file) : null;
    }

    private static Set<String> storedLeftovers() {
        BindForgeDeviceMasterManager masterStore = BindForgeDeviceMasterManager.getInstance();
        Set<String> leftovers = new HashSet<>(masterStore.pendingRemovals());
        for (DeviceRow row : masterStore.findAll()) {
            if (row.previousName() != null) leftovers.add(row.previousName());
        }
        return leftovers;
    }
}
