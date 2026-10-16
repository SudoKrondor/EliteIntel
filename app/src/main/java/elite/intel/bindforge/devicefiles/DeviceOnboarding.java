package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.FileWrite;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.bindforge.devicefiles.DeviceMappingsMerge.UserEntry;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.db.managers.BindForgeDeviceDraftManager;
import elite.intel.db.managers.BindForgeDeviceDraftManager.StoredDevice;
import elite.intel.db.managers.BindForgeDeviceMasterManager;
import elite.intel.io.AtomicFiles;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Onboarding's writes: the names the user gave their controllers, put where they belong (spec, alias-designer.md,
 * <em>What onboarding writes, and when</em>; Alan, 2026-10-14, A6b).
 * <p>
 * <strong>A controller no binding names by hardware id is named now.</strong> Its name goes into the master (and a
 * started draft), then its entry and a generated {@code .buttonMap} into every installation. This is one of the
 * writes outside Apply, and it is safe for the reason that list gives: it adds an element beside the others and
 * creates a file that was not there, so nothing the user has is changed. Each installation is all or nothing - its
 * {@code DeviceMappings.xml} and new {@code .buttonMap} files land together, or are put back - and one that cannot be
 * written is reported and left for the user to catch up, as after Apply.
 * <p>
 * <strong>A controller {@code .binds} names by hardware id waits for Apply</strong>, held in the draft: naming it
 * moves those bindings, and only Apply moves bindings ({@link DeviceApply}).
 * <p>
 * <strong>The database is written first</strong>, as Apply promotes before it pushes: the master records what the
 * user chose, so an installation the write misses differs from the master and shows as a row to revert.
 */
public final class DeviceOnboarding {

    private static final Logger log = LogManager.getLogger(DeviceOnboarding.class);

    /**
     * One controller as the user named it.
     *
     * @param labels   the generated labels, by input token
     * @param bindings how many bindings name it by hardware id - more than none means it waits for Apply
     */
    public record Naming(String name, String vid, String pid, Map<String, String> labels, int bindings) {
        public Naming {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(vid, "vid");
            Objects.requireNonNull(pid, "pid");
            labels = Map.copyOf(labels);
        }

        public boolean waitsForApply() {
            return bindings > 0;
        }
    }

    public enum Outcome {
        /** The entries, and the {@code .buttonMap} files that were not there, were written. */
        WRITTEN,
        /** Everything was already there; nothing was written. */
        UNCHANGED,
        /** Its folder is not there. Nothing was written. */
        SKIPPED_MISSING,
        /** A file could not be read, kept or written. The installation is as it was. */
        FAILED;

        /** Whether the installation now holds every name given - as {@link DeviceFilesPush.Outcome#landed()}. */
        public boolean landed() {
            return this == WRITTEN || this == UNCHANGED;
        }
    }

    /**
     * @param reason          why it was skipped or failed, else {@code null}
     * @param keptButtonMaps  names whose {@code .buttonMap} was already there and was left as it is
     */
    public record InstallationResult(Target target, Outcome outcome, String reason, List<String> keptButtonMaps) {
        public InstallationResult {
            keptButtonMaps = List.copyOf(keptButtonMaps);
        }
    }

    /**
     * @param recorded      whether the names were stored - {@code false} when BindForge is not activated, or a name was
     *                      taken meanwhile, in which case nothing was written anywhere
     * @param installations what happened in each installation; empty when nothing was named now
     * @param atApply       the controllers that wait for Apply
     */
    public record Result(boolean recorded, List<InstallationResult> installations, List<Naming> atApply) {
        public Result {
            installations = List.copyOf(installations);
            atApply = List.copyOf(atApply);
        }

        /** How many installations hold every name given now - the "2" in "updated 2 of 3". */
        public long matchingCount() {
            return installations.stream()
                    .filter(result -> result.outcome().landed())
                    .count();
        }
    }

    /** Where the names are stored. */
    interface Store {
        /** As {@link BindForgeDeviceDraftManager#onboard(List, List)}. */
        boolean onboard(List<StoredDevice> now, List<StoredDevice> atApply);

        /** Every name in the master, in the order the push leads each file with. */
        List<String> masterOrder();
    }

    private final Supplier<List<Target>> targets;
    private final Store store;
    private final EditHistory history;
    private final Consumer<Target> afterWrite;
    private final MissedWrites missedWrites;

    /**
     * Stores in the master and draft, writes to the stored installations, keeps Edit History where the push does,
     * and re-reads each installation it writes, as the push does.
     *
     * @throws IOException if Elite-Intel's data folder cannot be created
     */
    public static DeviceOnboarding stored() throws IOException {
        Store store = new Store() {
            @Override
            public boolean onboard(List<StoredDevice> now, List<StoredDevice> atApply) {
                return BindForgeDeviceDraftManager.getInstance().onboard(now, atApply);
            }

            @Override
            public List<String> masterOrder() {
                return BindForgeDeviceMasterManager.getInstance().findAll().stream().map(DeviceRow::deviceName).toList();
            }
        };
        InstallationDeviceScanner scanner = new InstallationDeviceScanner();
        return new DeviceOnboarding(DeviceFilesPush::storedInstallations, store,
                EditHistory.stored(),
                target -> scanner.scan(target.installId(), target.controlSchemes()), MissedWrites.stored());
    }

    DeviceOnboarding(Supplier<List<Target>> targets, Store store, EditHistory history, Consumer<Target> afterWrite) {
        this(targets, store, history, afterWrite, MissedWrites.NONE);
    }

    DeviceOnboarding(Supplier<List<Target>> targets, Store store, EditHistory history, Consumer<Target> afterWrite,
                     MissedWrites missedWrites) {
        this.targets = targets;
        this.store = store;
        this.history = history;
        this.afterWrite = afterWrite;
        this.missedWrites = missedWrites;
    }

    /** Stores every name, then writes the ones named now to every installation. */
    public Result name(List<Naming> namings) {
        List<Naming> now = namings.stream().filter(naming -> !naming.waitsForApply()).toList();
        List<Naming> atApply = namings.stream().filter(Naming::waitsForApply).toList();
        if (!store.onboard(stored(now), stored(atApply))) {
            log.warn("Onboarding wrote nothing: BindForge is not activated, or one of {} was named meanwhile",
                    namings.stream().map(Naming::name).toList());
            return new Result(false, List.of(), List.of());
        }
        if (now.isEmpty()) return new Result(true, List.of(), atApply);

        List<String> masterOrder = store.masterOrder();
        List<UserEntry> entries = now.stream().map(naming -> new UserEntry(naming.name(), naming.vid(), naming.pid()))
                .toList();
        int keep = history.retention();
        Set<String> keptThisWrite = new HashSet<>();
        List<InstallationResult> results = new ArrayList<>();
        for (Target target : targets.get()) {
            InstallationResult result = writeTo(target, now, entries, masterOrder, keep, keptThisWrite);
            log.info("Onboarding for {} ({}): {}{}", target.storefront(), target.controlSchemes(), result.outcome(),
                    result.reason() == null ? "" : " - " + result.reason());
            results.add(result);
        }
        missedWrites.afterOnboarding(results);
        return new Result(true, results, atApply);
    }

    private InstallationResult writeTo(Target target, List<Naming> now, List<UserEntry> entries,
                                       List<String> masterOrder, int keep, Set<String> keptThisWrite) {
        if (!target.isReachable()) {
            return new InstallationResult(target, Outcome.SKIPPED_MISSING,
                    "folder not found: " + target.controlSchemes(), List.of());
        }
        Path deviceMappings = GameInstallation.deviceMappingsIn(target.controlSchemes());
        byte[] current;
        DeviceMappingsMerge.Result inserted;
        try {
            current = DeviceFilesPush.readIfPresent(deviceMappings);
            // WHY: seeded from Frontier's, as the push seeds it - without the file the game loads no bindings.
            inserted = DeviceMappingsMerge.insert(current == null ? DeviceFilesPush.stockFile() : current, entries,
                    masterOrder);
        } catch (IOException e) {
            // WHY: IOException only. insert's IllegalArgumentException means the store and the master's order
            // disagree - a bug, not the user's file - and fails fast rather than sending them to a healthy file.
            return failed(target, "could not read its DeviceMappings.xml: " + e.getMessage());
        }
        boolean mappingsWrite = current == null || inserted.changed();
        try {
            if (mappingsWrite) history.keep(target.installId(), deviceMappings, current, keep, keptThisWrite);
        } catch (IOException e) {
            return failed(target, "could not keep an Edit History copy: " + e.getMessage());
        }

        // WHY: every write that lands is recorded as the push records its own, so a failure puts this installation
        // back the same way (DeviceFilesPush.restore): new .buttonMap files removed, DeviceMappings.xml restored.
        List<FileWrite> done = new ArrayList<>();
        List<String> kept = new ArrayList<>();
        try {
            if (mappingsWrite) {
                AtomicFiles.write(deviceMappings, inserted.content());
                done.add(new FileWrite(deviceMappings, current, inserted.content()));
            }
            for (Naming naming : now) {
                if (naming.labels().isEmpty()) continue;
                Path buttonMap = GameInstallation.buttonMapIn(target.controlSchemes(), naming.name());
                byte[] content = ButtonMapWriter.write(naming.labels());
                // WHY: create-only, as startup generation is. A map already under the name - one the user made by hand,
                // or one left by a device cleared outside BindForge - is theirs, and the divergence list shows it.
                if (CreateOnlyFiles.createNew(buttonMap, content)) {
                    done.add(new FileWrite(buttonMap, null, content));
                } else {
                    kept.add(naming.name());
                }
            }
        } catch (IOException | InvalidPathException e) {
            // WHY: InvalidPathException beside the IOException - a name the push could not turn into a .buttonMap
            // filename fails this installation, reported, as any write that cannot land does. Anything else is a bug
            // and is not caught here.
            return failed(target, "could not write: " + e.getMessage() + DeviceFilesPush.restore(done));
        }

        if (done.isEmpty()) {
            return new InstallationResult(target, Outcome.UNCHANGED, null, kept);
        }
        afterWrite.accept(target);
        return new InstallationResult(target, Outcome.WRITTEN, null, kept);
    }

    private static InstallationResult failed(Target target, String reason) {
        return new InstallationResult(target, Outcome.FAILED, reason, List.of());
    }

    private static List<StoredDevice> stored(List<Naming> namings) {
        return namings.stream()
                .map(naming -> new StoredDevice(null, naming.name(), naming.vid(), naming.pid(), true, null,
                        naming.labels()))
                .toList();
    }
}
