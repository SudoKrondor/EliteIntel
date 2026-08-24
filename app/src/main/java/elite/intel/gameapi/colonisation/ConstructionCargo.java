package elite.intel.gameapi.colonisation;

import elite.intel.db.dao.ConstructionSiteDao.Requirement;
import elite.intel.gameapi.gamestate.dtos.GameEvents;

import java.util.*;

/**
 * What the commander still has to buy for a construction site, measured against the hold they are
 * already carrying.
 * <p>
 * The sibling of {@code MissionCargo}, and the same shape of problem: a list of commodities and counts
 * that the hold partly covers. The differences are worth stating, because they are why this is not that
 * class with another argument:
 * <ul>
 *   <li>The manifest already knows what has been delivered. A mission gives no progress signal at all,
 *       so {@code MissionCargo} has to infer everything from the hold; here {@code ProvidedAmount} is
 *       authoritative and the hold only says what is on its way.</li>
 *   <li>The deliveries are not only ours. Anyone can haul to any depot, so a line can close while the
 *       commander is away and the shortfall can grow no further than the manifest says.</li>
 *   <li>There is no expiry to sort by. A build waits.</li>
 * </ul>
 * The order is largest shortfall first: Steel at 2542 tonnes is many trips in any ship, while the nine
 * tonnes of Fruit and Vegetables at the bottom of the list will fit in the corner of a hold on some
 * later run. Working the long pole first is what shortens the whole job.
 */
public final class ConstructionCargo {

    private ConstructionCargo() {
    }

    /**
     * One line of the manifest measured against the hold.
     *
     * @param symbol   bare journal symbol of the commodity
     * @param gameName the commodity as the game named it, for a good with no entry in the commodities table
     * @param required tonnes the site wants in total
     * @param provided tonnes already delivered, by anyone
     * @param held     tonnes of it in the hold right now, capped at what is still wanted
     * @param payment  credits per tonne the depot pays
     */
    public record Outstanding(String symbol, String gameName, int required, int provided, int held, long payment) {

        /**
         * Tonnes still to deliver after the hold is emptied into the build - what a shopping trip is for.
         */
        public int shortfall() {
            return Math.max(0, required - provided - held);
        }

        /**
         * Tonnes the site still wants, ignoring what is in the hold.
         */
        public int outstanding() {
            return Math.max(0, required - provided);
        }

        public boolean isSatisfied() {
            return shortfall() <= 0;
        }
    }

    /**
     * Every line the site still wants, largest shortfall first, each measured against the hold.
     * <p>
     * Lines the manifest lists as fully delivered are dropped: they are not shopping, and leaving them in
     * would make "seventeen commodities outstanding" the answer at a site with two left to go.
     */
    public static List<Outstanding> outstanding(Collection<Requirement> manifest, Map<String, Integer> heldBySymbol) {
        Map<String, Integer> pool = new HashMap<>(heldBySymbol == null ? Map.of() : heldBySymbol);
        List<Outstanding> result = new ArrayList<>();
        for (Requirement line : manifest == null ? List.<Requirement>of() : manifest) {
            if (line == null || line.getSymbol() == null) continue;
            int stillWanted = line.outstanding();
            if (stillWanted <= 0) continue;
            int available = pool.getOrDefault(line.getSymbol(), 0);
            int claimed = Math.min(available, stillWanted);
            pool.put(line.getSymbol(), available - claimed);
            result.add(new Outstanding(line.getSymbol(), line.getGameName(), line.getRequiredAmount(),
                    line.getProvidedAmount(), claimed, line.getPayment()));
        }
        result.sort(Comparator
                .comparingInt(Outstanding::shortfall).reversed()
                // A stable tie-break so the same manifest always names the same commodity. Without it the
                // card and the spoken answer can disagree about what is next while nothing has changed.
                .thenComparing(Outstanding::symbol));
        return result;
    }

    /**
     * The next commodity to go shopping for: the largest shortfall the hold does not already cover.
     * <p>
     * Empty means one of two things the caller has to tell apart - the build is finished, or the hold
     * already covers everything left and the commander should be flying, not shopping - which is why
     * {@link #outstanding} is public too.
     */
    public static Optional<Outstanding> nextToSource(Collection<Requirement> manifest, Map<String, Integer> heldBySymbol) {
        return outstanding(manifest, heldBySymbol).stream()
                .filter(line -> !line.isSatisfied())
                .findFirst();
    }

    /**
     * The hold as a symbol-to-tonnes map. The journal writes inventory names bare and lower-cased, which
     * is what {@code JournalSymbol} normalises a manifest line to.
     */
    public static Map<String, Integer> heldBySymbol(GameEvents.CargoEvent cargo) {
        Map<String, Integer> held = new HashMap<>();
        if (cargo == null || cargo.getInventory() == null) return held;
        for (GameEvents.Inventory item : cargo.getInventory()) {
            if (item == null || item.getName() == null) continue;
            held.merge(item.getName().toLowerCase(Locale.ROOT), (int) item.getCount(), Integer::sum);
        }
        return held;
    }
}
