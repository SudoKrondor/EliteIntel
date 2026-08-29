package elite.intel.gameapi.colonisation;

import elite.intel.gameapi.colonisation.CarrierStockpile.Stash;
import elite.intel.gameapi.colonisation.ConstructionCargo.Outstanding;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * A build's manifest read as a shopping list: what is wanted, what is already bought, and whether the
 * market in front of the commander has anything left to give.
 * <p>
 * <b>Why this is not inside the HUD card.</b> The screen and the voice have to agree about when a shop is
 * finished - the card stops offering goods that cannot be bought here in the same moment the companion says
 * so out loud. Two implementations of "covered" would drift apart on the first rounding, and the commander
 * would be told one thing and shown another.
 * <p>
 * <b>Bought is bought, wherever it sits.</b> Tonnes in the ship's hold and tonnes on the carrier are both
 * paid for and both bound for the depot, so they count as one figure against what the site still wants.
 */
public final class ConstructionShopping {

    private ConstructionShopping() {
    }

    /**
     * One good measured against the build.
     *
     * @param good  the manifest line, for the caller that needs to name it
     * @param owned tonnes we have already bought of it - carrier and hold together
     */
    public record Line(Outstanding good, int owned) {

        /**
         * Tonnes the site still wants of it, whoever ends up delivering them.
         */
        public int needed() {
            return good.outstanding();
        }

        public String symbol() {
            return good.symbol();
        }

        /**
         * Nothing left to buy of this one.
         */
        public boolean isCovered() {
            return owned >= needed();
        }
    }

    /**
     * What this market sells that the build still wants, largest requirement first.
     * <p>
     * The order is the build's own requirement rather than the deficit, because the deficit moves as the
     * commander buys and a list that reshuffles under their hands is not a list. Empty means this market is
     * no part of the trip - it sells nothing the build wants - which is a different thing from a market that
     * has been bought out.
     * <p>
     * <b>Part-bought goods do NOT lead here</b>, though they do in the card's loading order. The two lists
     * answer different questions. The loading order is read on a delivery run, where a part-bought good means
     * a job under way and finishing it beats opening a second front. This one is read while STOCKING UP,
     * where the commander buys a little of everything and promoting each good as it is touched would shuffle
     * the list under their hands - the exact restlessness ordering by deficit caused, arrived at by another
     * route. What is part bought is already legible here without moving anything, because every row carries
     * it: {@code 1.760/3.963 T}, owned over needed.
     */
    public static List<Line> soldHere(List<Outstanding> manifest, Set<String> onTheShelves, Stash stash) {
        if (manifest == null || onTheShelves == null || onTheShelves.isEmpty()) return List.of();
        return manifest.stream()
                .filter(line -> onTheShelves.contains(line.symbol()))
                .map(line -> line(line, stash))
                .sorted(byRequirement())
                .toList();
    }

    /**
     * Everything the build still wants that we have not bought yet, wherever it is sold - the answer to
     * "what now" once a market has been bought out.
     */
    public static List<Line> stillToAcquire(List<Outstanding> manifest, Stash stash) {
        if (manifest == null) return List.of();
        return manifest.stream()
                .map(line -> line(line, stash))
                .filter(line -> !line.isCovered())
                .sorted(byRequirement())
                .toList();
    }

    /**
     * The whole job as a delivery run reads it: every line the site still wants, each measured against what
     * is already bought, a good under way ahead of one untouched and the largest requirement first after
     * that.
     * <p>
     * <b>Nothing is filtered out for being bought.</b> Unlike {@link #stillToAcquire}, which answers "what is
     * left to BUY", a good sitting complete on the carrier is the most relevant line on a delivery run -
     * dropping it would hide the very cargo the commander is at the depot to move. The only lines missing are
     * the ones the site no longer wants, and those never reach here: {@code ConstructionCargo.outstanding}
     * drops a fully delivered line before this sees it.
     * <p>
     * The requirement here is the site's own, ignoring the hold, so it reads as the same number the game's
     * construction panel puts in its REQUIRED column - the commander is usually looking at both at once.
     */
    public static List<Line> toDeliver(List<Outstanding> manifest, Stash stash) {
        if (manifest == null) return List.of();
        return manifest.stream()
                .map(line -> line(line, stash))
                .sorted(byDeliveryOrder())
                .toList();
    }

    /**
     * True when this market had something for the build and we now hold all of it.
     * <p>
     * A market that never sold anything the build wants is NOT bought out: there was nothing to buy, and
     * announcing it as finished would say the commander had achieved something by flying past.
     */
    public static boolean isBoughtOut(List<Line> soldHere) {
        return !soldHere.isEmpty() && soldHere.stream().allMatch(Line::isCovered);
    }

    /**
     * The goods on this list still short of what the build wants.
     */
    public static List<Line> stillShort(List<Line> lines) {
        return lines.stream().filter(line -> !line.isCovered()).toList();
    }

    private static Line line(Outstanding good, Stash stash) {
        int aboardTheCarrier = stash == null ? 0 : stash.stockOf(good.symbol());
        return new Line(good, aboardTheCarrier + good.held());
    }

    private static Comparator<Line> byRequirement() {
        return Comparator.comparingInt(Line::needed).reversed()
                // A stable tie-break, so two goods wanted in equal tonnage do not swap places between polls.
                .thenComparing(Line::symbol);
    }

    /**
     * Part bought first, then largest requirement. Safe from the restlessness that rules out ordering the
     * SHOPPING list this way: a delivery run buys nothing, so no row is promoted under the commander's hands
     * while they read it.
     */
    private static Comparator<Line> byDeliveryOrder() {
        return Comparator.comparing((Line line) -> line.owned() > 0).reversed()
                .thenComparing(byRequirement());
    }
}
