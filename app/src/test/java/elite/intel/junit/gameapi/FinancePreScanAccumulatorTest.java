package elite.intel.junit.gameapi;

import com.google.gson.JsonObject;
import elite.intel.gameapi.FinancePreScanAccumulator;
import elite.intel.gameapi.journal.events.LoadGameEvent;
import elite.intel.gameapi.journal.events.MarketBuyEvent;
import elite.intel.gameapi.journal.events.MarketSellEvent;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the mid-session-start reconstruction. Events use an old timestamp so
 * {@code isReplay()} is true (their timestamp predates APP_START, captured when
 * BaseEvent loaded in this JVM) - that is the condition the accumulator counts.
 */
class FinancePreScanAccumulatorTest {

    /**
     * Well before APP_START, so isReplay() == true.
     */
    private static final String REPLAY_TS = "2020-01-01T00:00:00Z";
    private static final long SENTINEL = 123L;

    private final PlayerSession session = PlayerSession.getInstance();

    @BeforeEach
    void setSentinelBalance() {
        session.setPersonalCreditsAvailable(SENTINEL);
    }

    @Test
    void reconstructsBalanceFromAnchorPlusReplayDeltas() {
        FinancePreScanAccumulator acc = new FinancePreScanAccumulator();
        acc.onLoadGame(loadGame(1_000_000L, REPLAY_TS));
        acc.onMarketSell(marketSell(50_000L, REPLAY_TS));   // +50k
        acc.onMarketBuy(marketBuy(20_000L, REPLAY_TS));     // -20k
        acc.persist();

        assertEquals(1_030_000L, session.getPersonalCredits());
    }

    @Test
    void laterLoadGameResetsRunningTally() {
        FinancePreScanAccumulator acc = new FinancePreScanAccumulator();
        acc.onLoadGame(loadGame(1L, REPLAY_TS));
        acc.onMarketSell(marketSell(999L, REPLAY_TS));      // belongs to the stale anchor
        acc.onLoadGame(loadGame(2_000_000L, REPLAY_TS));    // newer absolute truth resets
        acc.onMarketBuy(marketBuy(500L, REPLAY_TS));        // -500 against the new anchor
        acc.persist();

        assertEquals(2_000_000L - 500L, session.getPersonalCredits());
    }

    @Test
    void noAnchorLeavesBalanceUntouched() {
        FinancePreScanAccumulator acc = new FinancePreScanAccumulator();
        acc.onMarketSell(marketSell(50_000L, REPLAY_TS));   // no LoadGame seen
        acc.persist();

        assertEquals(SENTINEL, session.getPersonalCredits());
    }

    @Test
    void eventsAtOrAfterAppStartAreIgnored() {
        FinancePreScanAccumulator acc = new FinancePreScanAccumulator();
        acc.onLoadGame(loadGame(1_000_000L, REPLAY_TS));
        // timestamp = now -> not a replay -> left for the live FinanceSubscriber
        acc.onMarketSell(marketSell(50_000L, Instant.now().toString()));
        acc.persist();

        assertEquals(1_000_000L, session.getPersonalCredits());
    }

    // --- builders ---

    private static JsonObject base(String event, String timestamp) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", timestamp);
        j.addProperty("event", event);
        return j;
    }

    private static LoadGameEvent loadGame(long credits, String ts) {
        JsonObject j = base("LoadGame", ts);
        j.addProperty("Commander", "CMDR Test");
        j.addProperty("Ship", "cobra");
        j.addProperty("Credits", credits);
        return new LoadGameEvent(j);
    }

    private static MarketSellEvent marketSell(long totalSale, String ts) {
        JsonObject j = base("MarketSell", ts);
        j.addProperty("Type", "gold");
        j.addProperty("Count", 1);
        j.addProperty("SellPrice", totalSale);
        j.addProperty("TotalSale", totalSale);
        return new MarketSellEvent(j);
    }

    private static MarketBuyEvent marketBuy(long totalCost, String ts) {
        JsonObject j = base("MarketBuy", ts);
        j.addProperty("Type", "gold");
        j.addProperty("Count", 1);
        j.addProperty("BuyPrice", totalCost);
        j.addProperty("TotalCost", totalCost);
        return new MarketBuyEvent(j);
    }
}
