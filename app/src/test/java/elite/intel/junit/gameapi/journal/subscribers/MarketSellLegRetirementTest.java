package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.dao.TradeRouteDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.MarketSellEvent;
import elite.intel.gameapi.journal.subscribers.MarketSellEventSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A docking costs the route one leg, however many commodities were sold.
 * <p>
 * MarketSell fires once per commodity, so emptying a hold of three goods raised three sales. Each used to
 * retire a leg, and on a loop whose next legs end at the same station two of them took legs the commander had
 * not flown yet - part of why a ten-leg route was reporting six.
 */
class MarketSellLegRetirementTest {

    private static final long MARKET_A = 3223343616L;
    private static final long MARKET_B = 3229009920L;

    private final MarketSellEventSubscriber subscriber = new MarketSellEventSubscriber();

    @BeforeEach
    void clearRoute() {
        Database.withDao(TradeRouteDao.class, dao -> {
            dao.clear();
            return null;
        });
    }

    @AfterEach
    void stopTimers() {
        // This subscriber narrates too; a briefing it armed must not fire into a sibling suite's recorder.
        subscriber.shutdown();
    }

    /**
     * Consecutive legs ending at the same station is the shape that made this destructive: without the
     * per-docking rule the second and third sales walk straight down the route.
     */
    @Test
    void sellingThreeCommoditiesAtOneStopRetiresOneLeg() throws InterruptedException {
        saveRoute(MARKET_A, MARKET_A, MARKET_B);

        subscriber.onMarketSellEvent(sale(MARKET_A, "gold"));
        subscriber.onMarketSellEvent(sale(MARKET_A, "silver"));
        subscriber.onMarketSellEvent(sale(MARKET_A, "painite"));

        awaitLegs(List.of(2, 3));
    }

    @Test
    void sellingAtAStopThatIsNotTheLegBeingFlownLeavesTheRouteAlone() throws InterruptedException {
        saveRoute(MARKET_A, MARKET_B);

        subscriber.onMarketSellEvent(sale(MARKET_B, "gold"));

        // Nothing should ever be retired; give the debounce time to prove it.
        Thread.sleep(2500);
        assertEquals(List.of(1, 2), legNumbers());
    }

    @Test
    void eachDockingRetiresTheLegThatEndedThere() throws InterruptedException {
        saveRoute(MARKET_A, MARKET_B);

        subscriber.onMarketSellEvent(sale(MARKET_A, "gold"));
        awaitLegs(List.of(2));

        subscriber.onMarketSellEvent(sale(MARKET_B, "silver"));
        awaitLegs(List.of());
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private static void saveRoute(long... destinationMarketIds) {
        Database.withDao(TradeRouteDao.class, dao -> {
            for (int i = 0; i < destinationMarketIds.length; i++) {
                TradeRouteDao.TradeRoute leg = new TradeRouteDao.TradeRoute();
                leg.setLegNumber(i + 1);
                leg.setTotalLegs(destinationMarketIds.length);
                leg.setJson("{\"stopNumber\":" + (i + 1)
                        + ",\"sourceSystem\":\"Sol\",\"sourceStation\":\"Abraham Lincoln\""
                        + ",\"destinationSystem\":\"Alpha Centauri\",\"destinationStation\":\"Hutton Orbital\""
                        + ",\"commodities\":[{\"name\":\"Gold\",\"amount\":100}]"
                        + ",\"destinationMarketId\":" + destinationMarketIds[i] + "}");
                dao.save(leg);
            }
            return null;
        });
    }

    private static MarketSellEvent sale(long marketId, String commodity) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("event", "MarketSell");
        json.addProperty("MarketID", marketId);
        json.addProperty("Type", commodity);
        json.addProperty("Count", 10);
        json.addProperty("SellPrice", 1000);
        json.addProperty("TotalSale", 10000);
        json.addProperty("AvgPricePaid", 500);
        return new MarketSellEvent(json);
    }

    private static List<Integer> legNumbers() {
        return Database.withDao(TradeRouteDao.class, dao -> dao.listAll().stream()
                .map(TradeRouteDao.TradeRoute::getLegNumber)
                .sorted()
                .toList());
    }

    /**
     * The retirement happens on the debounced flush, so it is awaited rather than assumed.
     */
    private static void awaitLegs(List<Integer> expected) throws InterruptedException {
        awaitTrue(() -> legNumbers().equals(expected));
        assertEquals(expected, legNumbers());
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 5 seconds");
            Thread.sleep(50);
        }
    }
}
