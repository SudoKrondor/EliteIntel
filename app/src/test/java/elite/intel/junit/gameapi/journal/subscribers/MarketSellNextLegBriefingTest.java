package elite.intel.junit.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.dao.TradeRouteDao;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.DockedEvent;
import elite.intel.gameapi.journal.events.MarketSellEvent;
import elite.intel.gameapi.journal.subscribers.MarketSellEventSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The next-leg briefing is spoken once per docking, however many commodities the hold was emptied of.
 * <p>
 * The sales are debounced into a batch, but selling is manual: a commander clearing three commodities takes
 * seconds between each, which is longer than the debounce window. Each gap therefore closed one batch and
 * opened another, and every batch briefed the next leg - so the commander heard what to buy three times.
 */
class MarketSellNextLegBriefingTest {

    private static final long MARKET_A = 3223343616L;
    private static final long MARKET_B = 3229009920L;
    // Scaled down from the shipped 2000 / 8000 so the suite does not sit out real quiet periods. The ratio
    // is what matters: the briefing must outlast a gap between sales that the sale debounce does not.
    private static final int DEBOUNCE_MS = 200;
    private static final int BRIEFING_QUIET_MS = 800;

    /**
     * Longer than the sale debounce, shorter than the briefing quiet period - the gap that broke this.
     */
    private static final int GAP_BETWEEN_SALES_MS = 400;

    private final MarketSellEventSubscriber subscriber =
            new MarketSellEventSubscriber(DEBOUNCE_MS, BRIEFING_QUIET_MS);
    private final SpeechRecorder speech = new SpeechRecorder();

    @BeforeEach
    void setUp() {
        GameEventBus.register(speech);
        Database.withDao(TradeRouteDao.class, dao -> {
            dao.clear();
            return null;
        });
    }

    @AfterEach
    void tearDown() {
        // Before unregistering: a timer this test armed must not fire into the next one.
        subscriber.shutdown();
        GameEventBus.unregister(speech);
    }

    /**
     * The regression: three sales spaced further apart than the debounce window. Each lands in its own batch,
     * so the guard - not the batching - is what keeps the briefing to one.
     */
    @Test
    void sellingCommoditiesOneAtATimeBriefsTheNextLegOnce() throws InterruptedException {
        saveRoute(MARKET_A, MARKET_B);

        sellSlowly(MARKET_A, "gold", "palladium", "silver");

        assertEquals(1, briefings(), "next leg briefed once per docking, got: " + speech.spoken);
        // Each sale still gets its own confirmation - only the briefing is deduplicated.
        assertEquals(3, sellConfirmations(), "every sale is confirmed: " + speech.spoken);
        // And it comes last: the commander hears what to buy once the hold is empty, not part way through.
        assertEquals(3, speech.spoken.indexOf(briefing()), "briefing follows every sale: " + speech.spoken);
    }

    /**
     * Undocking and coming back is a new docking, so the same market briefs again - the shape a loop route
     * bouncing between two stations produces.
     */
    @Test
    void returningToTheSameMarketBriefsAgain() throws InterruptedException {
        // Three legs, so there is still a leg left to brief on the second visit.
        saveRoute(MARKET_A, MARKET_A, MARKET_B);

        sellSlowly(MARKET_A, "gold");
        assertEquals(1, briefings());

        subscriber.onDockedEvent(docked(MARKET_A));
        sellSlowly(MARKET_A, "silver");

        assertEquals(2, briefings(), "a second docking briefs again: " + speech.spoken);
    }

    @Test
    void sellingAtTheNextMarketBriefsThatLeg() throws InterruptedException {
        saveRoute(MARKET_A, MARKET_B, MARKET_A);

        sellSlowly(MARKET_A, "gold");
        sellSlowly(MARKET_B, "silver");

        assertEquals(2, briefings(), "each market briefs its own leg: " + speech.spoken);
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    /**
     * Sells one commodity per debounced batch, the way a commander clicking through a market does: each sale
     * lands in its own batch, and the run then goes quiet long enough for the briefing to be spoken.
     */
    private void sellSlowly(long marketId, String... commodities) throws InterruptedException {
        long briefingsBefore = briefings();
        for (String commodity : commodities) {
            int spokenBefore = speech.spoken.size();
            subscriber.onMarketSellEvent(sale(marketId, commodity));
            awaitTrue(() -> speech.spoken.size() > spokenBefore);
            Thread.sleep(GAP_BETWEEN_SALES_MS);
        }
        awaitTrue(() -> briefings() > briefingsBefore);
    }

    private String briefing() {
        return speech.spoken.stream().filter(MarketSellNextLegBriefingTest::isBriefing).findFirst().orElseThrow();
    }

    private long briefings() {
        return speech.spoken.stream().filter(MarketSellNextLegBriefingTest::isBriefing).count();
    }

    private long sellConfirmations() {
        return speech.spoken.stream().filter(text -> !isBriefing(text)).count();
    }

    /**
     * The two briefing phrasings both name where to sell; the sale confirmations never do.
     */
    private static boolean isBriefing(String text) {
        return text.contains("Hutton Orbital");
    }

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

    private static DockedEvent docked(long marketId) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("event", "Docked");
        json.addProperty("MarketID", marketId);
        json.addProperty("StationName", "Abraham Lincoln");
        json.addProperty("StationType", "Orbis");
        json.addProperty("StarSystem", "Sol");
        return new DockedEvent(json);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 5 seconds");
            Thread.sleep(25);
        }
    }

    private static class SpeechRecorder {
        private final List<String> spoken = new CopyOnWriteArrayList<>();

        @Subscribe
        public void onVox(AiVoxResponseEvent event) {
            spoken.add(event.getText());
        }
    }
}
