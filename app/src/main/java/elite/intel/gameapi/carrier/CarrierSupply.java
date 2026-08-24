package elite.intel.gameapi.carrier;

import elite.intel.gameapi.carrier.OwnCarrierHold.Held;
import elite.intel.gameapi.search.spansh.commodity.WantedCommodity;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * How much of a shopping list the commander's own carrier could fill.
 * <p>
 * The workflow this exists for: a commander stockpiles construction materials on their carrier, moves the
 * carrier out to the build, and then shuttles between the two. Sending them to a market to buy steel they
 * already own, parked in the next orbit, is the wrong answer - and it is the answer they got before this,
 * because the search only ever looked outward.
 */
public final class CarrierSupply {

    /**
     * How much of the ship's hold the carrier must be able to fill before it is worth flying to one that is
     * in another system.
     * <p>
     * A carrier in the CURRENT system is always worth using - the cargo is already bought and it is right
     * there. One that is somewhere else is a journey, and a journey across the bubble to collect four tonnes
     * of a good sold at a station two jumps away is a worse answer than the market run. A quarter of the
     * hold is the line: enough that the trip carries real weight, low enough that a part load still counts.
     */
    private static final double WORTH_A_JUMP = 0.25;

    private CarrierSupply() {
    }

    /**
     * What one trip to the carrier would load, in the caller's priority order.
     *
     * @param wanted       what is still needed, anchor first
     * @param holdCapacity tonnes the ship can carry
     */
    public static List<Line> loadable(Held carrier, List<WantedCommodity> wanted, int holdCapacity) {
        List<Line> lines = new ArrayList<>();
        if (carrier == null || wanted == null) return lines;

        int remaining = Math.max(0, holdCapacity);
        for (WantedCommodity want : wanted) {
            if (remaining <= 0) break;
            if (want == null || want.unitsWanted() <= 0) continue;
            int aboard = carrier.stockOf(want.symbol());
            if (aboard <= 0) continue;
            int units = Math.min(Math.min(want.unitsWanted(), remaining), aboard);
            if (units <= 0) continue;
            lines.add(new Line(want.symbol(), want.commodity(), units, aboard));
            remaining -= units;
        }
        return lines;
    }

    /**
     * How old a carrier snapshot may be before the commander is warned it might have moved on.
     * <p>
     * Longer than the construction manifest's hour, because the two go stale for different reasons. A
     * depot's numbers change because OTHER commanders are delivering to it, constantly and invisibly. A
     * carrier's change only through the owner's own hauling - or someone buying off its sell orders - so
     * within a session the commander already knows what they loaded. Across sessions they may well not,
     * which is what this window is sized to catch.
     */
    public static final int SNAPSHOT_STALE_AFTER_HOURS = 6;

    /**
     * True when the last look at this carrier's shelves is old enough to be worth a caveat.
     * <p>
     * The game tells a third-party tool what a carrier holds exactly once, when the commander stands in its
     * market. Nothing reports cargo moved on or off afterwards, so an old snapshot is a claim about the past
     * and stating it flatly is how a commander flies somewhere for cargo that is not there any more.
     */
    public static boolean snapshotIsStale(Held carrier) {
        if (carrier == null || carrier.seenAt() == null) return false;
        return Duration.between(carrier.seenAt(), Instant.now()).toHours() >= SNAPSHOT_STALE_AFTER_HOURS;
    }

    /**
     * The carrier best placed to supply the list, or empty when none of them holds any of it.
     * <p>
     * One in the system the commander is already in wins outright: its cargo is bought and paid for and
     * needs no journey at all, so even a small load beats a bigger one somewhere else. Between two equally
     * placed carriers, the one that fills more of the hold.
     */
    public static Optional<Loaded> best(List<Held> carriers, List<WantedCommodity> wanted,
                                        int holdCapacity, String currentSystem) {
        Loaded best = null;
        for (Held carrier : carriers == null ? List.<Held>of() : carriers) {
            List<Line> loadable = loadable(carrier, wanted, holdCapacity);
            if (loadable.isEmpty()) continue;
            Loaded candidate = new Loaded(carrier, loadable, isInSystem(carrier, currentSystem));
            if (best == null || candidate.beats(best)) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * One carrier weighed against the shopping list.
     *
     * @param here true when the carrier is in the system the commander is standing in
     */
    public record Loaded(Held carrier, List<Line> loadable, boolean here) {

        public int tonnes() {
            return loadable.stream().mapToInt(Line::unitsToLoad).sum();
        }

        private boolean beats(Loaded other) {
            if (here != other.here) return here;
            return tonnes() > other.tonnes();
        }
    }

    /**
     * Whether the carrier is the better answer than a market run.
     * <p>
     * Always yes when it is in the system the commander is already in; otherwise only when it can fill
     * enough of the hold to justify the journey - see {@link #WORTH_A_JUMP}.
     *
     * @param currentSystem the system the commander is in right now
     */
    public static boolean worthGoing(Held carrier, List<Line> loadable, int holdCapacity, String currentSystem) {
        if (carrier == null || loadable.isEmpty()) return false;
        if (isInSystem(carrier, currentSystem)) return true;

        int tonnes = loadable.stream().mapToInt(Line::unitsToLoad).sum();
        return tonnes >= Math.max(1, (int) Math.ceil(holdCapacity * WORTH_A_JUMP));
    }

    /**
     * True when the carrier is in the system the commander is standing in, so there is no route to plot -
     * only a short hop in supercruise.
     */
    public static boolean isInSystem(Held carrier, String currentSystem) {
        return carrier != null && currentSystem != null
                && currentSystem.equalsIgnoreCase(carrier.starSystem());
    }

    /**
     * One good to collect from the carrier.
     *
     * @param unitsToLoad what this trip would take, capped by the hold and by what is still wanted
     * @param aboard      what the carrier was holding when we last looked
     */
    public record Line(String symbol, String commodity, int unitsToLoad, int aboard) {
    }
}
