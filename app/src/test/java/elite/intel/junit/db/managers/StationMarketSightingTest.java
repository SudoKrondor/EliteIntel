package elite.intel.junit.db.managers;

import elite.intel.db.managers.StationMarketsManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reading our own last look at a market back out of the store. The app has been keeping every
 * {@code Market.json} it sees all along; this is what makes that pile answer a question.
 */
class StationMarketSightingTest {

    /**
     * Trimmed from the real Market.json for Boldyr Dredging Installation: Silver is listed with a
     * price and no stock at all, which is exactly what Spansh disagreed with.
     */
    private static final String BOLDYR = """
            { "timestamp":"2026-08-21T10:21:53Z", "event":"Market", "MarketID":3803845888,
              "StationName":"Boldyr Dredging Installation", "StationType":"OnFootSettlement",
              "StarSystem":"Mat Zemlya", "Items":[
              { "id":128049155, "Name":"$silver_name;", "Name_Localised":"Silver", "BuyPrice":25961,
                "SellPrice":25959, "MeanPrice":4885, "StockBracket":0, "DemandBracket":0, "Stock":0,
                "Demand":0, "Consumer":false, "Producer":false, "Rare":false },
              { "id":128049177, "Name":"$bertrandite_name;", "Name_Localised":"Bertrandite", "BuyPrice":2470,
                "SellPrice":2468, "MeanPrice":2374, "StockBracket":3, "DemandBracket":0, "Stock":1079,
                "Demand":0, "Consumer":false, "Producer":true, "Rare":false },
              { "id":128049202, "Name":"$harmasilverseasrum_name;", "Name_Localised":"Harma Silver Sea Rum",
                "BuyPrice":1221, "SellPrice":1220, "MeanPrice":1200, "StockBracket":0, "DemandBracket":0,
                "Stock":0, "Demand":0, "Consumer":false, "Producer":false, "Rare":true } ] }
            """;

    private final StationMarketsManager markets = StationMarketsManager.getInstance();

    @BeforeEach
    void clean() {
        markets.clear();
        markets.save(GsonFactory.getGson().fromJson(BOLDYR, GameEvents.MarketEvent.class));
    }

    @AfterEach
    void tidy() {
        markets.clear();
    }

    @Test
    @DisplayName("a listed price with no stock reads as no stock")
    void listedButNotStocked() {
        StationMarketsManager.Sighting seen =
                markets.lastSeen("Mat Zemlya", "Boldyr Dredging Installation", "silver").orElseThrow();

        assertEquals(0, seen.stock());
        assertEquals(Instant.parse("2026-08-21T10:21:53Z"), seen.seenAt());
    }

    @Test
    @DisplayName("what the market does stock comes back with its real count")
    void stockedGoodsCarryTheirCount() {
        assertEquals(1079,
                markets.lastSeen("Mat Zemlya", "Boldyr Dredging Installation", "bertrandite").orElseThrow().stock());
    }

    @Test
    @DisplayName("a good the market does not list at all is no stock, not no sighting")
    void unlistedGoodIsZeroNotAbsent() {
        // "we were there and it had none" and "we were there and it did not even list it" are the same
        // answer to the only question being asked.
        assertEquals(0,
                markets.lastSeen("Mat Zemlya", "Boldyr Dredging Installation", "tritium").orElseThrow().stock());
    }

    @Test
    @DisplayName("a similarly named good is not mistaken for the one asked about")
    void rumIsNotSilver() {
        assertEquals(0,
                markets.lastSeen("Mat Zemlya", "Boldyr Dredging Installation", "harmasilverseasrum").orElseThrow().stock());
    }

    @Test
    @DisplayName("the same station name in another system is not this market")
    void stationNamesAreNotUniqueAcrossTheGalaxy() {
        assertTrue(markets.lastSeen("Deciat", "Boldyr Dredging Installation", "silver").isEmpty());
    }

    @Test
    @DisplayName("a market never visited has nothing to say")
    void neverVisited() {
        assertEquals(Optional.empty(), markets.lastSeen("Deciat", "Garay Terminal", "silver"));
    }
}
