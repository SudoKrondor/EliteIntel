package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.Report;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.db.managers.BindForgeDeviceDraftManager;
import elite.intel.db.managers.BindForgeDeviceDraftManager.StoredDevice;
import elite.intel.db.managers.BindForgeDeviceMasterManager;
import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * First setup, end to end: read every installation, and once the user has answered, back them all up, fill the
 * master and push it out (alias-designer.md, <em>First setup — reconciling the installs</em>).
 * <p>
 * <strong>Nothing is written until {@link #apply}.</strong> Reading can be repeated and abandoned.
 * <p>
 * The write runs in the order the spec gives it: the labelled backup, then the master, then the push - which
 * keeps every file it replaces in Edit History, one copy per unique content, so each installation's own version
 * can be walked back alone. A backup that fails stops everything; a push that fails in one installation is
 * reported for that installation, and the master already holds what the user chose.
 */
public final class FirstSetup {

    private static final Logger log = LogManager.getLogger(FirstSetup.class);
    static final String SNAPSHOT_LABEL = "first-setup";

    /**
     * What first setup found.
     *
     * @param targets     every stored installation, including those it could not reach
     * @param unreachable installations whose folder is gone - not read, and skipped by the push
     */
    public record Reading(List<Target> targets, List<Target> unreachable, FirstSetupPlan plan) {
        public Reading {
            targets = List.copyOf(targets);
            unreachable = List.copyOf(unreachable);
            Objects.requireNonNull(plan, "plan");
        }
    }

    public enum Outcome {
        /** The master is set up and was pushed; the report says how each installation fared. */
        APPLIED,
        /** The master was no longer empty. Nothing was written. */
        ALREADY_SET_UP,
        /** The backup could not be taken. Nothing was written. */
        BACKUP_FAILED
    }

    /**
     * @param reason why nothing was written; {@code null} when applied
     * @param backup the labelled backup's folder, when taken
     * @param report each installation's push, when applied
     */
    public record Result(Outcome outcome, String reason, Path backup, FirstSetupPlan.Resolution resolution,
                         Report report) {
    }

    /** Takes the labelled backup. */
    @FunctionalInterface
    interface Backup {
        Path take(List<Target> targets) throws IOException;
    }

    /** Reads the names the shared {@code .binds} uses. */
    @FunctionalInterface
    interface References {
        Set<String> read() throws IOException;
    }

    /** Reads one installation. */
    @FunctionalInterface
    interface Reader {
        FirstSetupPlan.Installation read(Target target) throws IOException;
    }

    private final Supplier<List<Target>> targets;
    private final Reader reader;
    private final References references;
    private final BooleanSupplier masterEmpty;
    private final Backup backup;
    private final BiPredicate<List<StoredDevice>, List<String>> establish;
    private final Supplier<Report> push;

    /** The stored installations, master and draft. */
    public static FirstSetup stored() throws IOException {
        DeviceFilesSnapshot snapshot = new DeviceFilesSnapshot();
        DeviceFilesPush pushToAll = new DeviceFilesPush();
        FrontierStockDevices stock = FrontierStockDevices.getInstance();
        FrontierStockButtonMaps stockMaps = FrontierStockButtonMaps.getInstance();
        return new FirstSetup(
                DeviceFilesPush::storedInstallations,
                target -> FirstSetupReader.read(target, stock, stockMaps),
                () -> BindsDeviceReferences.referencedEntryNames(PlayerSession.getInstance().getBindingsDir()),
                () -> BindForgeDeviceMasterManager.getInstance().findAll().isEmpty(),
                targets -> snapshot.take(SNAPSHOT_LABEL, targets),
                BindForgeDeviceDraftManager.getInstance()::establish,
                pushToAll::push);
    }

    FirstSetup(Supplier<List<Target>> targets, Reader reader, References references, BooleanSupplier masterEmpty,
               Backup backup, BiPredicate<List<StoredDevice>, List<String>> establish, Supplier<Report> push) {
        this.targets = targets;
        this.reader = reader;
        this.references = references;
        this.masterEmpty = masterEmpty;
        this.backup = backup;
        this.establish = establish;
        this.push = push;
    }

    /** Whether first setup is still to run: the master is empty, which is also what the startup check reads. */
    public boolean isNeeded() {
        return masterEmpty.getAsBoolean();
    }

    /**
     * Reads every reachable installation. Writes nothing.
     *
     * @throws IOException if an installation's files or the {@code .binds} cannot be read - the message names
     *                     which. First setup does not run on a partial picture
     */
    public Reading read() throws IOException {
        List<Target> all = targets.get();
        List<Target> unreachable = new ArrayList<>();
        List<FirstSetupPlan.Installation> installations = new ArrayList<>();
        for (Target target : all) {
            if (target.missing() || !Files.isDirectory(target.controlSchemes())) {
                unreachable.add(target);
                continue;
            }
            try {
                installations.add(reader.read(target));
            } catch (IOException e) {
                throw new IOException(target.storefront() + ": " + e.getMessage(), e);
            }
        }
        return new Reading(all, unreachable, new FirstSetupPlan(installations, references.read()));
    }

    /**
     * Writes what the user chose: the backup, the master, then every installation.
     *
     * @throws IllegalStateException if a question is unanswered
     */
    public Result apply(Reading reading, Map<String, String> answers) {
        FirstSetupPlan.Resolution resolution = reading.plan().resolve(answers);
        Path taken;
        try {
            taken = backup.take(reading.targets());
        } catch (IOException e) {
            log.warn("First setup stopped: the backup could not be taken", e);
            return new Result(Outcome.BACKUP_FAILED, e.getMessage(), null, resolution, null);
        }
        if (!establish.test(resolution.devices(), resolution.pendingRemovals())) {
            return new Result(Outcome.ALREADY_SET_UP, "the master is no longer empty", taken, resolution, null);
        }
        Report report = push.get();
        log.info("First setup: {} devices, {} button maps, {} pending removals; installations matching {} of {}",
                resolution.devices().size(), resolution.buttonMapCount(), resolution.pendingRemovals().size(),
                report.matchingCount(), report.installations().size());
        return new Result(Outcome.APPLIED, null, taken, resolution, report);
    }
}
