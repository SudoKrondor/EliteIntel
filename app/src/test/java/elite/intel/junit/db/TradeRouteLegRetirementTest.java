package elite.intel.junit.db;

import elite.intel.db.dao.TradeRouteDao;
import elite.intel.db.util.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A sale retires the leg that ended at that market — one leg, not every leg that happens to end there.
 * <p>
 * Spansh routes bounce between the same few stations, so the old "delete everything for this market" wiped
 * most of a route at the first sale. A commander plotted ten legs, sold once, and the overlay showed
 * "LEG 3 OF 6": six legs had been deleted, and the total was being counted from the wreckage.
 */
class TradeRouteLegRetirementTest {

    private static final long MARKET_A = 3223343616L;
    private static final long MARKET_B = 3229009920L;
    private static final long MARKET_C = 3221379840L;

    @BeforeEach
    void clearRoute() {
        Database.withDao(TradeRouteDao.class, dao -> {
            dao.clear();
            return null;
        });
    }

    @Test
    void aSaleRetiresOnlyTheLegThatEndedThere() {
        // A ten-leg loop between three stations: six of its legs end at market A.
        long[] destinations = {MARKET_A, MARKET_B, MARKET_A, MARKET_C, MARKET_A,
                MARKET_B, MARKET_A, MARKET_C, MARKET_A, MARKET_A};
        saveRoute(destinations);

        deleteForMarket(MARKET_A);

        assertEquals(9, legNumbers().size(), "one sale must cost the route exactly one leg");
        assertEquals(2, legNumbers().getFirst(), "the next leg is the one after the leg just flown");
    }

    @Test
    void everyLegIsRetiredInTurnAsTheRouteIsFlown() {
        saveRoute(new long[]{MARKET_A, MARKET_A, MARKET_A});

        deleteForMarket(MARKET_A);
        assertEquals(List.of(2, 3), legNumbers());

        deleteForMarket(MARKET_A);
        assertEquals(List.of(3), legNumbers());

        deleteForMarket(MARKET_A);
        assertEquals(List.of(), legNumbers());
    }

    @Test
    void sellingSomewhereThatIsNotOnTheRouteChangesNothing() {
        saveRoute(new long[]{MARKET_A, MARKET_B});

        deleteForMarket(MARKET_C);

        assertEquals(List.of(1, 2), legNumbers());
    }

    /**
     * A route is a chain, so the only leg a sale can complete is the one being flown. Dumping unrelated cargo
     * at a station a later leg happens to end at must not retire that leg from under the commander.
     */
    @Test
    void sellingAtALaterLegsDestinationDoesNotRetireItEarly() {
        saveRoute(new long[]{MARKET_A, MARKET_B, MARKET_C});

        deleteForMarket(MARKET_C);

        assertEquals(List.of(1, 2, 3), legNumbers());
    }

    /**
     * The recorded length outlives the legs, which is what the overlay needs to stop counting the wreckage.
     */
    @Test
    void theRecordedLengthSurvivesTheLegsBeingFlown() {
        saveRoute(new long[]{MARKET_A, MARKET_B, MARKET_A, MARKET_C, MARKET_A});

        assertEquals(5, totalLegs());

        deleteForMarket(MARKET_A);
        deleteForMarket(MARKET_B);

        assertEquals(5, totalLegs(), "a flown leg does not shorten the route");
        assertEquals(3, legNumbers().size());
    }

    @Test
    void noRouteHasNoLength() {
        assertEquals(0, totalLegs());
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private static void saveRoute(long[] destinationMarketIds) {
        Database.withDao(TradeRouteDao.class, dao -> {
            for (int i = 0; i < destinationMarketIds.length; i++) {
                TradeRouteDao.TradeRoute leg = new TradeRouteDao.TradeRoute();
                leg.setLegNumber(i + 1);
                leg.setTotalLegs(destinationMarketIds.length);
                leg.setJson("{\"stopNumber\":" + (i + 1)
                        + ",\"destinationMarketId\":" + destinationMarketIds[i] + "}");
                dao.save(leg);
            }
            return null;
        });
    }

    private static void deleteForMarket(long marketId) {
        Database.withDao(TradeRouteDao.class, dao -> {
            dao.deleteForMarketId(marketId);
            return null;
        });
    }

    private static List<Integer> legNumbers() {
        return Database.withDao(TradeRouteDao.class, dao -> dao.listAll().stream()
                .map(TradeRouteDao.TradeRoute::getLegNumber)
                .sorted()
                .toList());
    }

    private static int totalLegs() {
        return Database.withDao(TradeRouteDao.class, TradeRouteDao::totalLegs);
    }
}
