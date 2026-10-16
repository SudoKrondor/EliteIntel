package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.Report;
import elite.intel.bindforge.io.DeviceReferenceRewrite;
import elite.intel.bindforge.io.DeviceRewriteResult;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.db.managers.BindForgeDeviceDraftManager;
import elite.intel.db.managers.BindForgeDeviceMasterManager;
import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * APPLY, for the device files: the draft becomes the master, then the master is pushed out and the pending
 * removals are taken out of every installation.
 * <p>
 * <strong>The draft is promoted before anything is written</strong> (Alan, 2026-10-04). The master records what
 * the user intends, so a push that cannot reach one installation still leaves the edits safe in the master -
 * that installation stops matching it, the install strip shows which, and the next Apply tries again. Nothing
 * the user saved is lost to a failed write.
 * <p>
 * <strong>A removal never strands a binding</strong> (Alan, 2026-10-11, A10). An entry {@code .binds} still names
 * is held back and stays pending: without it the game rejects the whole preset. Moving those bindings is a
 * {@code .binds} rewrite, which is not built. When {@code .binds} cannot be read, every such removal is held back.
 * <strong>One of Frontier's names is never held back</strong> (review, 2026-10-11): RESET LABELS on a built-in removes
 * only its {@code .buttonMap}, never Frontier's element, and {@code .binds} names inputs by token, never by label -
 * so nothing a binding needs is lost, and holding it back would leave the reset pending forever.
 * <p>
 * <strong>A removal stays pending until every installation has taken it</strong>, so one on an unplugged drive is
 * retried by the next Apply rather than forgotten.
 * <p>
 * With no draft, Apply still pushes the master. That is the retry after a partial Apply.
 * <p>
 * <strong>Bindings follow a device to its name</strong> once the push has put that name in at least one installation
 * - those naming it by hardware id (Alan, 2026-10-14, A6b), and those naming it by the old name of a rename (A11). The
 * user's Apply comes first: an installation that missed the name does not hold the move back, it is reported and caught
 * up later. When no installation took it, nothing was applied for the bindings to follow, and they stay as they are
 * for the next Apply. One of Frontier's names is never a target. Both moves are derived from the files each time, so a
 * move that stopped is retried by the next Apply with no record of its own.
 * <p>
 * <strong>Catch-up</strong> ({@link #catchUp(Collection)}, Alan, 2026-10-15, A14) is Apply for an installation that
 * missed one and is found again: the same push, removals and moves, to that installation alone, and never the draft -
 * what the user saved and has not applied stays theirs to apply. What it settles is read from every installation's
 * files ({@link Holdings}), since its own report covers only the one it wrote.
 */
public final class DeviceApply {

    private static final Logger log = LogManager.getLogger(DeviceApply.class);

    /** The names {@code .binds} gives devices, read when Apply runs. */
    @FunctionalInterface
    interface BindsNames {
        Set<String> read() throws IOException;
    }

    /**
     * Moves bindings from one device reference to another - a hardware id to a name, or an old name to a new one -
     * across {@code .binds}; {@link BindsDeviceRewrite} in use.
     */
    interface BindsRewrite {
        /** As {@link BindsDeviceReferences#hardwareIdCounts(java.nio.file.Path)}: strict. */
        Map<String, Integer> hardwareIdCounts() throws IOException;

        /** As {@link BindsDeviceReferences#referenceCounts(java.nio.file.Path)}: strict, keyed as written. */
        Map<String, Integer> nameCounts() throws IOException;

        /** Every reference the rewrite matches moves to its new value, all or nothing. */
        Rewritten rewrite(DeviceReferenceRewrite rewrite);
    }

    /**
     * @param status   how the rewrite ended
     * @param bindings how many bindings in the game's own {@code .binds} it moved - working copies not counted, as
     *                 they hold the same bindings
     */
    record Rewritten(DeviceRewriteResult.Status status, int bindings) {
    }

    /**
     * The two pushes Apply and catch-up make - {@link DeviceFilesPush} in use. One seam rather than two, since both
     * come from one push (review, 2026-10-15).
     */
    interface Pushes {
        /** Apply's: every installation, with the removals. */
        Report push(Set<String> removals);

        /** Catch-up's: the installations named only, with the removals. */
        Report push(Collection<Long> installIds, Set<String> removals);
    }

    /**
     * Whether any installation may still hold a name - what catch-up settles a removal or a rename by, since its push
     * reaches only the installation that missed them. {@link InstallationHoldings} in use.
     */
    public interface Holdings {
        /** As the push removes it. */
        boolean mayHoldRemoval(String name);

        /** As the push renames it. */
        boolean mayHoldOldName(String oldName, String newName);
    }

    /** What became of the bindings a move was for. */
    public enum MoveOutcome {
        /** They name the device now. */
        MOVED,
        /** No installation took the device's name, so they were left as they were, for the next Apply. */
        NOT_APPLIED,
        /** Gone by the time the rewrite ran - the game saved {@code .binds} in between - so nothing was written. */
        NOT_FOUND,
        /** The rewrite stopped and put everything back - the move's status says why. */
        FAILED
    }

    /**
     * The bindings that named a master device by hardware id.
     *
     * @param bindings how many bindings moved, when they did; otherwise how many named the device by hardware id
     * @param status   the rewrite's status when it ran, else {@code null}
     */
    public record Move(String name, int bindings, MoveOutcome outcome, DeviceRewriteResult.Status status) {
    }

    /**
     * The bindings that named a renamed device by its old name (A11).
     *
     * @param bindings how many bindings moved, when they did; otherwise how many named the old name
     * @param status   the rewrite's status when it ran, else {@code null}
     */
    public record RenameMove(String from, String to, int bindings, MoveOutcome outcome,
                             DeviceRewriteResult.Status status) {
    }

    /**
     * @param removed       devices this Apply took out of the master - CLEAR and RESET LABELS, applied
     * @param report        what happened in each installation
     * @param cleanedUp     pending removals every installation has now lost, and which are pending no more
     * @param notEverywhere pending removals some installation did not take - unreachable, or failed - left pending
     *                      for the next Apply
     * @param heldBack      pending removals left in the installations because {@code .binds} names them, or could
     *                      not be read
     * @param bindsUnread   whether {@code .binds} could not be read and an entry of the user's was held back for it
     * @param moves           the bindings that named a master device by hardware id, and what became of them
     * @param renameMoves     the bindings that named a renamed device by its old name, and what became of them
     * @param movesUnread     whether {@code .binds} could not be read to find bindings to move, so none was moved
     * @param renamesFinished the devices whose rename every installation and {@code .binds} have now taken, by new
     *                        name - their old name is forgotten
     */
    public record Result(List<String> removed, Report report, List<String> cleanedUp, List<String> notEverywhere,
                         List<String> heldBack, boolean bindsUnread, List<Move> moves, List<RenameMove> renameMoves,
                         boolean movesUnread, List<String> renamesFinished) {
        public Result {
            removed = List.copyOf(removed);
            Objects.requireNonNull(report, "report");
            cleanedUp = List.copyOf(cleanedUp);
            notEverywhere = List.copyOf(notEverywhere);
            heldBack = List.copyOf(heldBack);
            moves = List.copyOf(moves);
            renameMoves = List.copyOf(renameMoves);
            renamesFinished = List.copyOf(renamesFinished);
        }
    }

    private final Supplier<List<String>> promote;
    private final Pushes pushes;
    private final BindsNames bindsNames;
    private final FrontierStockDevices stock;
    private final BindForgeDeviceMasterManager master;
    private final BindsRewrite binds;
    private final Holdings holdings;

    /**
     * Promotes the stored draft and pushes to the stored installations.
     *
     * @throws IOException if Elite-Intel's data folder cannot be created for Edit History
     */
    public DeviceApply() throws IOException {
        this(new DeviceFilesPush());
    }

    private DeviceApply(DeviceFilesPush push) throws IOException {
        this(BindForgeDeviceDraftManager.getInstance()::promote, push,
                () -> BindsDeviceReferences.referenceCounts(PlayerSession.getInstance().getBindingsDir()).keySet(),
                FrontierStockDevices.getInstance(), BindForgeDeviceMasterManager.getInstance(),
                BindsDeviceRewrite.stored(PlayerSession.getInstance().getBindingsDir()),
                InstallationHoldings.stored());
    }

    /** With nothing for {@code .binds} to move - for the tests that are about removals. */
    DeviceApply(Supplier<List<String>> promote, Function<Set<String>, Report> push, BindsNames bindsNames,
                FrontierStockDevices stock, BindForgeDeviceMasterManager master) {
        this(promote, push, bindsNames, stock, master, NOTHING_TO_MOVE);
    }

    /** @param bindsNames must throw rather than answer when it cannot be sure - a missing folder is not "none" */
    DeviceApply(Supplier<List<String>> promote, Function<Set<String>, Report> push, BindsNames bindsNames,
                FrontierStockDevices stock, BindForgeDeviceMasterManager master, BindsRewrite binds) {
        this(promote, applyOnly(push), bindsNames, stock, master, binds, NOTHING_SETTLED);
    }

    /** @param holdings what settles a removal or rename after catch-up */
    DeviceApply(Supplier<List<String>> promote, Pushes pushes, BindsNames bindsNames, FrontierStockDevices stock,
                BindForgeDeviceMasterManager master, BindsRewrite binds, Holdings holdings) {
        this.promote = promote;
        this.pushes = pushes;
        this.bindsNames = bindsNames;
        this.stock = stock;
        this.master = master;
        this.binds = binds;
        this.holdings = holdings;
    }

    /** Apply's push alone, for the tests that are about Apply - catch-up is not theirs to call. */
    private static Pushes applyOnly(Function<Set<String>, Report> push) {
        return new Pushes() {
            @Override
            public Report push(Set<String> removals) {
                return push.apply(removals);
            }

            @Override
            public Report push(Collection<Long> installIds, Set<String> removals) {
                throw new UnsupportedOperationException("built for Apply only");
            }
        };
    }

    /** Every name may still be somewhere, so catch-up settles nothing. */
    private static final Holdings NOTHING_SETTLED = new Holdings() {
        @Override
        public boolean mayHoldRemoval(String name) {
            return true;
        }

        @Override
        public boolean mayHoldOldName(String oldName, String newName) {
            return true;
        }
    };

    private static final BindsRewrite NOTHING_TO_MOVE = new BindsRewrite() {
        @Override
        public Map<String, Integer> hardwareIdCounts() {
            return Map.of();
        }

        @Override
        public Map<String, Integer> nameCounts() {
            return Map.of();
        }

        @Override
        public Rewritten rewrite(DeviceReferenceRewrite rewrite) {
            throw new IllegalStateException("nothing is named for a move");
        }
    };

    public Result apply() {
        List<String> removed = promote.get();
        Split split = pendingRemovals();

        Report report = pushes.push(split.removals());
        List<String> cleanedUp = List.of();
        List<String> notEverywhere = List.of();
        if (!split.removals().isEmpty()) {
            if (everyInstallationTookIt(report)) {
                split.removals().forEach(master::clearPendingRemoval);
                cleanedUp = List.copyOf(split.removals());
            } else {
                notEverywhere = List.copyOf(split.removals());
            }
        }
        boolean everywhere = everyInstallationTookIt(report);
        Renames renames = moveRenamedReferences(report, row -> everywhere);
        Moves moves = moveHardwareIds(report);
        return new Result(removed, report, cleanedUp, notEverywhere, split.heldBack(), split.bindsUnread(),
                moves.moves(), renames.moves(), moves.unread() || renames.unread(), renames.finished());
    }

    /**
     * Catch-up: the installations named, which missed a write and are found again, brought in line as Apply would -
     * the master, the pending removals {@code .binds} does not hold back, the renames, and the bindings that follow
     * them. The draft is left alone (Alan, 2026-10-15, A14).
     * <p>
     * <strong>Settled from the files.</strong> A removal stops being pending, and a rename's old name is forgotten,
     * once no installation may hold it ({@link Holdings}) - not from this push's report, which covers only the
     * installations it wrote.
     */
    public Result catchUp(Collection<Long> installIds) {
        Split split = pendingRemovals();

        Report report = pushes.push(installIds, split.removals());
        List<String> cleanedUp = new ArrayList<>();
        List<String> notEverywhere = new ArrayList<>();
        for (String name : split.removals()) {
            if (holdings.mayHoldRemoval(name)) {
                notEverywhere.add(name);
            } else {
                master.clearPendingRemoval(name);
                cleanedUp.add(name);
            }
        }
        Renames renames = moveRenamedReferences(report,
                row -> !holdings.mayHoldOldName(row.previousName(), row.deviceName()));
        Moves moves = moveHardwareIds(report);
        return new Result(List.of(), report, cleanedUp, notEverywhere, split.heldBack(), split.bindsUnread(),
                moves.moves(), renames.moves(), moves.unread() || renames.unread(), renames.finished());
    }

    /**
     * The pending removals of devices no longer in the master, split into those this write carries out and those
     * {@code .binds} holds back.
     */
    private Split pendingRemovals() {
        Set<String> inMaster = master.findAll().stream().map(DeviceRow::deviceName).collect(Collectors.toSet());
        List<String> pending = master.pendingRemovals().stream().filter(name -> !inMaster.contains(name)).toList();

        // WHY: read only when it can decide something. Frontier's names never consult it, and most Applies remove
        // nothing - reading it then is wasted, and its warning would describe a hold-back that never happened.
        boolean anyUsersName = pending.stream().anyMatch(name -> !stock.ships(name));
        return split(pending, anyUsersName ? readBindsNames() : Optional.empty());
    }

    /**
     * The bindings naming a renamed device by its old name, moved to the new one - or left, when no installation took
     * the new name (Alan, 2026-10-14, A11). One rewrite per device, back to back, as the hardware-id moves are.
     * <p>
     * <strong>A rename that changes only case moves nothing</strong>: a {@code Device=} name finds its element
     * ignoring case, and rewriting would only risk two spellings in one preset, which the game rejects (format §4.3,
     * measured 2026-10-14).
     * <p>
     * <strong>A rename is finished</strong> - its old name forgotten - once every installation took the new name and
     * no binding names the old one. Until then the push renames wherever the old name is still found, and the next
     * Apply moves what is left.
     *
     * @param lostOldName whether every installation has lost a device's old name - from Apply's report, or for
     *                    catch-up from the files
     */
    private Renames moveRenamedReferences(Report report, Predicate<DeviceRow> lostOldName) {
        List<DeviceRow> renamedDevices = master.findAll().stream()
                .filter(row -> row.previousName() != null && !row.previousName().equals(row.deviceName()))
                .filter(row -> !stock.ships(row.deviceName()) && !stock.ships(row.previousName()))
                .toList();
        if (renamedDevices.isEmpty()) return new Renames(List.of(), false, List.of());

        Map<String, Integer> counts;
        try {
            counts = binds.nameCounts();
        } catch (IOException e) {
            log.warn("Could not read .binds, so no binding naming a renamed device by its old name was moved: {}",
                    e.getMessage());
            return new Renames(List.of(), true, List.of());
        }

        boolean applied = report.matchingCount() > 0;
        List<RenameMove> moves = new ArrayList<>();
        List<String> finished = new ArrayList<>();
        for (DeviceRow row : renamedDevices) {
            String from = row.previousName();
            String to = row.deviceName();
            boolean caseOnly = from.equalsIgnoreCase(to);
            int bindings = caseOnly ? 0 : BindsDeviceReferences.bindingsNaming(List.of(from), counts);
            boolean bindsSettled = bindings == 0;
            if (bindings > 0) {
                if (!applied) {
                    moves.add(new RenameMove(from, to, bindings, MoveOutcome.NOT_APPLIED, null));
                } else {
                    Rewritten result = binds.rewrite(DeviceReferenceRewrite.rename(from, to));
                    MoveOutcome outcome = outcomeOf(result, from + " to " + to);
                    moves.add(new RenameMove(from, to, reported(result, bindings), outcome, result.status()));
                    bindsSettled = outcome != MoveOutcome.FAILED;
                }
            }
            if (bindsSettled && lostOldName.test(row)) {
                master.clearPreviousName(row.id());
                finished.add(to);
                log.info("Rename of {} to {} finished in every installation and in .binds", from, to);
            }
        }
        return new Renames(moves, false, finished);
    }

    private record Renames(List<RenameMove> moves, boolean unread, List<String> finished) {
    }

    /**
     * What became of a rewrite that ran: moved, found nothing to move - the game saved {@code .binds} in between - or
     * stopped and put everything back, which is logged.
     *
     * @param what the move, for the log - {@code "LVWAP to LeftStick"}
     */
    private static MoveOutcome outcomeOf(Rewritten result, String what) {
        return switch (result.status()) {
            case REWRITTEN -> MoveOutcome.MOVED;
            case NO_REFERENCES -> MoveOutcome.NOT_FOUND;
            default -> {
                log.warn("Could not move the bindings {}: {}", what, result.status());
                yield MoveOutcome.FAILED;
            }
        };
    }

    /** How many bindings a move reports: those it moved, when it did; otherwise those found before it ran. */
    private static int reported(Rewritten result, int found) {
        return result.status() == DeviceRewriteResult.Status.REWRITTEN ? result.bindings() : found;
    }

    /**
     * The bindings naming a master device of the user's by hardware id, moved to its name - or left, when no
     * installation took the master. Where two master devices claim one VID/PID, the first in the master's order is
     * the one the push leads each file with, and so the one the game resolves.
     * <p>
     * One rewrite per device, back to back (Alan, 2026-10-14). Each ends on the game's {@code .binds}, which the
     * bindings monitor reloads, so a second device's rewrite can meet a working copy mid-reload and stop as
     * {@code STALE_FILE}. That device is reported as failed and, its bindings still on the hardware id, moved by the
     * next Apply. One pass for several devices would need a rewrite with several targets, which {@code bindforge.io}
     * does not offer.
     */
    private Moves moveHardwareIds(Report report) {
        Map<String, Integer> counts;
        try {
            counts = binds.hardwareIdCounts();
        } catch (IOException e) {
            log.warn("Could not read .binds, so no binding naming a device by hardware id was moved: {}",
                    e.getMessage());
            return new Moves(List.of(), true);
        }
        if (counts.isEmpty()) return new Moves(List.of(), false);

        boolean applied = report.matchingCount() > 0;
        Set<String> claimed = new HashSet<>();
        List<Move> moves = new ArrayList<>();
        for (DeviceRow row : master.findAll()) {
            if (stock.ships(row.deviceName()) || row.vid() == null || row.pid() == null) continue;
            Map<String, Integer> found =
                    new LinkedHashMap<>(BindsDeviceReferences.hardwareIdsFor(row.vid(), row.pid(), counts));
            found.keySet().removeIf(id -> !claimed.add(id.toUpperCase(Locale.ROOT)));
            if (found.isEmpty()) continue;
            int bindings = found.values().stream().mapToInt(Integer::intValue).sum();
            if (!applied) {
                moves.add(new Move(row.deviceName(), bindings, MoveOutcome.NOT_APPLIED, null));
                continue;
            }
            Rewritten result = binds.rewrite(new DeviceReferenceRewrite(found.keySet(), row.deviceName()));
            MoveOutcome outcome = outcomeOf(result, "naming " + row.deviceName() + " by hardware id " + found.keySet());
            moves.add(new Move(row.deviceName(), reported(result, bindings), outcome, result.status()));
        }
        return new Moves(moves, false);
    }

    private record Moves(List<Move> moves, boolean unread) {
    }

    /** The names {@code .binds} gives devices, folded to lower case - or empty when they cannot be read. */
    private Optional<Set<String>> readBindsNames() {
        try {
            return Optional.of(folded(bindsNames.read()));
        } catch (IOException e) {
            log.warn("Could not read .binds, so no entry of the user's is removed by this Apply: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Which pending removals this Apply can carry out, and which wait for a {@code .binds} rewrite. Frontier's names
     * go either way: their element stays, and a label is nothing a binding needs.
     *
     * @param named the names {@code .binds} gives devices, or empty when they could not be read - or were not read,
     *              because no name in {@code pending} is the user's
     */
    private Split split(List<String> pending, Optional<Set<String>> named) {
        Set<String> removals = new TreeSet<>();
        List<String> heldBack = new ArrayList<>();
        boolean bindsUnread = false;
        for (String name : pending) {
            if (stock.ships(name)) {
                removals.add(name);
            } else if (named.isEmpty()) {
                heldBack.add(name);
                bindsUnread = true;
            } else if (named.get().contains(name.toLowerCase(Locale.ROOT))) {
                // WHY: compared ignoring case. A hand-edited .binds can spell a name otherwise, and holding back an
                // entry that might be in use costs a leftover; removing one that is costs the whole preset.
                heldBack.add(name);
            } else {
                removals.add(name);
            }
        }
        return new Split(removals, heldBack, bindsUnread);
    }

    private record Split(Set<String> removals, List<String> heldBack, boolean bindsUnread) {
    }

    private static boolean everyInstallationTookIt(Report report) {
        return !report.installations().isEmpty() && report.installations().stream()
                .allMatch(result -> result.outcome().landed());
    }

    private static Set<String> folded(Set<String> names) {
        return names.stream().map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }
}
