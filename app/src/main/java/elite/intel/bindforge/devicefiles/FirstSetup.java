package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.Report;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.db.managers.BindForgeDeviceDraftManager.StoredDevice;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;
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
 * <p>
 * <strong>After the push, the bindings follow a name the user gave up</strong> (Alan, 2026-10-17, A16): a losing name
 * {@code .binds} uses hands its bindings to the name that beat it, by Apply's rename move. Left for the next Apply,
 * every installation would lose the name - the push renames it - while the presets still name it.
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
        /** BindForge was already activated, or the master was no longer empty. Nothing was written. */
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
     * @param moves        the bindings each losing name handed over, when applied; {@code null} otherwise
     * @param masterFilled whether BindForge is now activated - first setup is then over, whatever else failed. The
     *                     master may still be empty: a user who owns nothing is activated with an empty master
     */
    public record Result(Outcome outcome, String reason, Path backup, FirstSetupPlan.Resolution resolution,
                         Report report, DeviceApply.SetupMoves moves, boolean masterFilled) {

        /** A failure before anything was written, for a caller that could not even reach {@link #apply}. */
        public static Result failedBeforeWriting(Exception e) {
            return new Result(Outcome.FAILED, StringUtls.messageOf(e), null, null, null, null, false);
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

    /**
     * Fills the master and activates BindForge, only before activation - {@link Activation#establish}, which
     * activation without a prompt uses too.
     */
    @FunctionalInterface
    interface Establish {
        /** @return {@code false}, writing nothing, when BindForge was already activated or the master was not empty */
        boolean intoEmptyMaster(List<StoredDevice> devices, List<String> pendingRemovals);
    }

    /** Moves the bindings of the losing names after the push - {@link DeviceApply#afterFirstSetup} in use. */
    @FunctionalInterface
    interface Moves {
        DeviceApply.SetupMoves after(Report report, Map<String, String> handedOver);
    }

    private final Supplier<List<Target>> targets;
    private final Reader reader;
    private final References references;
    private final Backup backup;
    private final Establish establish;
    private final Supplier<Report> push;
    private final Moves moves;

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
                Activation::establish,
                pushToAll::push,
                FirstSetup::storedMoves);
    }

    /**
     * Built only when first setup reaches its moves - activation calls {@link #stored()} on every attempt, and most
     * never get that far.
     */
    private static DeviceApply.SetupMoves storedMoves(Report report, Map<String, String> handedOver) {
        try {
            return new DeviceApply().afterFirstSetup(report, handedOver);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    FirstSetup(Supplier<List<Target>> targets, Reader reader, References references, Backup backup,
               Establish establish, Supplier<Report> push, Moves moves) {
        this.targets = targets;
        this.reader = reader;
        this.references = references;
        this.backup = backup;
        this.establish = establish;
        this.push = push;
        this.moves = moves;
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
            return new Result(Outcome.BACKUP_FAILED, StringUtls.messageOf(e), null, resolution, null, null, false);
        }
        try {
            if (!establish.intoEmptyMaster(resolution.devices(), resolution.pendingRemovals())) {
                return new Result(Outcome.ALREADY_SET_UP, "BindForge is already activated", taken, resolution, null,
                        null, true);
            }
        } catch (RuntimeException e) {
            // WHY: the master is written in one transaction, so a failure here leaves it empty and first setup can
            // simply run again.
            log.error("First setup stopped: the master could not be saved", e);
            return new Result(Outcome.FAILED, "the master could not be saved: " + StringUtls.messageOf(e), taken, resolution,
                    null, null, false);
        }
        Report report;
        try {
            report = push.get();
        } catch (RuntimeException e) {
            // WHY: the master already holds what the user chose, so first setup is over - the installations that
            // missed the push stop matching it, and Apply retries them, the bindings move with them.
            log.error("First setup saved the master, but the push failed", e);
            return new Result(Outcome.FAILED, "the master is saved, but the installations could not be updated: "
                    + StringUtls.messageOf(e), taken, resolution, null, null, true);
        }
        DeviceApply.SetupMoves moved;
        try {
            moved = moves.after(report, resolution.handedOver());
        } catch (RuntimeException e) {
            // WHY: the installations are written, so first setup is over. An old name's move is retried by the next
            // Apply; a handed-over name's element stays, held back while .binds names it.
            log.error("First setup updated the installations, but the bindings could not be moved", e);
            return new Result(Outcome.FAILED, "the installations are updated, but the bindings could not be moved: "
                    + StringUtls.messageOf(e), taken, resolution, report, null, true);
        }
        log.info("First setup: {} devices, {} button maps, {} pending removals, {} bindings moves; installations "
                        + "matching {} of {}", resolution.devices().size(), resolution.buttonMapCount(),
                resolution.pendingRemovals().size(), moved.moves().size(), report.matchingCount(),
                report.installations().size());
        return new Result(Outcome.APPLIED, null, taken, resolution, report, moved, true);
    }

}
