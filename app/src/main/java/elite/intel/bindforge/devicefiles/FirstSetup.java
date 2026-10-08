package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.Report;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.db.managers.BindForgeDeviceDraftManager;
import elite.intel.db.managers.BindForgeDeviceDraftManager.StoredDevice;
import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        BACKUP_FAILED,
        /** A later step failed; the reason names it, and {@link Result#masterFilled()} says whether it got that far. */
        FAILED
    }

    /**
     * @param reason       what went wrong; {@code null} when applied
     * @param backup       the labelled backup's folder, when taken
     * @param resolution   the master that was to be written; {@code null} when the answers could not be resolved
     * @param report       each installation's push, when applied
     * @param masterFilled whether the master now holds anything - first setup is then over, whatever else failed
     */
    public record Result(Outcome outcome, String reason, Path backup, FirstSetupPlan.Resolution resolution,
                         Report report, boolean masterFilled) {

        /** A failure before anything was written, for a caller that could not even reach {@link #apply}. */
        public static Result failedBeforeWriting(Exception e) {
            return new Result(Outcome.FAILED, messageOf(e), null, null, null, false);
        }
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

    /** Fills the master, only while it is empty - {@link BindForgeDeviceDraftManager#establish}. */
    @FunctionalInterface
    interface Establish {
        /** @return {@code false}, writing nothing, when the master was not empty */
        boolean intoEmptyMaster(List<StoredDevice> devices, List<String> pendingRemovals);
    }

    private final Supplier<List<Target>> targets;
    private final Reader reader;
    private final References references;
    private final Backup backup;
    private final Establish establish;
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
                targets -> snapshot.take(SNAPSHOT_LABEL, targets),
                BindForgeDeviceDraftManager.getInstance()::establish,
                pushToAll::push);
    }

    FirstSetup(Supplier<List<Target>> targets, Reader reader, References references, Backup backup,
               Establish establish, Supplier<Report> push) {
        this.targets = targets;
        this.reader = reader;
        this.references = references;
        this.backup = backup;
        this.establish = establish;
        this.push = push;
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
            if (!target.isReachable()) {
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
     * Writes what the user chose: the backup, the master, then every installation. Each step that fails is named
     * in the result, and nothing after it runs.
     *
     * @throws IllegalStateException if a question is unanswered
     */
    public Result apply(Reading reading, Map<String, String> answers) {
        FirstSetupPlan.Resolution resolution = reading.plan().resolve(answers);
        Path taken;
        try {
            taken = backup.take(reading.targets());
        } catch (IOException | RuntimeException e) {
            log.warn("First setup stopped: the backup could not be taken", e);
            return new Result(Outcome.BACKUP_FAILED, messageOf(e), null, resolution, null, false);
        }
        try {
            if (!establish.intoEmptyMaster(resolution.devices(), resolution.pendingRemovals())) {
                return new Result(Outcome.ALREADY_SET_UP, "the master is no longer empty", taken, resolution, null,
                        true);
            }
        } catch (RuntimeException e) {
            // WHY: the master is written in one transaction, so a failure here leaves it empty and first setup can
            // simply run again.
            log.error("First setup stopped: the master could not be saved", e);
            return new Result(Outcome.FAILED, "the master could not be saved: " + messageOf(e), taken, resolution,
                    null, false);
        }
        Report report;
        try {
            report = push.get();
        } catch (RuntimeException e) {
            // WHY: the master already holds what the user chose, so first setup is over - the installations that
            // missed the push stop matching it, and Apply retries them.
            log.error("First setup saved the master, but the push failed", e);
            return new Result(Outcome.FAILED, "the master is saved, but the installations could not be updated: "
                    + messageOf(e), taken, resolution, null, true);
        }
        log.info("First setup: {} devices, {} button maps, {} pending removals; installations matching {} of {}",
                resolution.devices().size(), resolution.buttonMapCount(), resolution.pendingRemovals().size(),
                report.matchingCount(), report.installations().size());
        return new Result(Outcome.APPLIED, null, taken, resolution, report, true);
    }

    /** An exception's message, or its kind when it carries none - never the word "null" on screen. */
    static String messageOf(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
