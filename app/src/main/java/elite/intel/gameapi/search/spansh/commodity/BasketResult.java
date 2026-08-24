package elite.intel.gameapi.search.spansh.commodity;

import java.util.List;

/**
 * One market weighed against a whole shopping list: everything on the list it stocks, and how much of each
 * would fit in the hold.
 * <p>
 * The sibling of {@link CommoditySearchResult}, which answers about a single good. This one exists because
 * a colonisation build's long tail is nine or ten commodities of sixty tonnes each: fetched one at a time
 * that is nine round trips, and the hold is empty for eight of them.
 *
 * @param lines every wanted good this market can supply, in the order the caller asked for them, so
 *              {@code lines.getFirst()} is always the anchor the Spansh filter was built around
 */
public record BasketResult(
        String starSystem,
        String stationName,
        String stationType,
        boolean fleetCarrier,
        double distanceFromPlayer,
        String marketUpdatedAt,
        List<BasketLine> lines) {

    public BasketResult {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    /**
     * One good this market can supply, and how much of it this trip would take.
     *
     * @param supply     units on sale, already corrected by our own last look at this market
     * @param unitsToBuy what the hold has room for, capped by the supply and by what is still wanted
     */
    public record BasketLine(String symbol, String commodity, long price, long supply, int unitsToBuy) {
    }

    /**
     * The good the search was anchored on - the reason for the trip, and the one every candidate market
     * was required to stock.
     */
    public BasketLine anchor() {
        return lines.getFirst();
    }

    /**
     * Tonnes of the hold this market can fill with goods that are actually wanted. What the markets are
     * ranked by: a station three jumps further out that fills the hold four times over is the shorter job.
     */
    public int totalUnits() {
        return lines.stream().mapToInt(BasketLine::unitsToBuy).sum();
    }

    /**
     * True when this trip is worth describing as a shopping list rather than as one errand.
     */
    public boolean isMultiBuy() {
        return lines.size() > 1;
    }
}
