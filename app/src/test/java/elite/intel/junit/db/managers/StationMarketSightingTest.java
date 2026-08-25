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

import static org.junit.jupiter.api.Assertions.*;

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

    /**
     * Another port of the same name, three hundred light years away, holding nothing Boldyr holds. Station
     * names are not unique in the galaxy - the id is what makes one of them THIS one.
     */
    private static final String NAMESAKE = """
            { "timestamp":"2026-08-22T09:00:00Z", "event":"Market", "MarketID":3999999999,
              "StationName":"Boldyr Dredging Installation", "StationType":"OnFootSettlement",
              "StarSystem":"Sirius", "Items":[
              { "id":128049204, "Name":"$titanium_name;", "Name_Localised":"Titanium", "BuyPrice":1100,
                "SellPrice":1098, "MeanPrice":1200, "StockBracket":3, "DemandBracket":0, "Stock":4200,
                "Demand":0, "Consumer":false, "Producer":true, "Rare":false } ] }
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

    @Test
    @DisplayName("the whole shelf comes back for the market the ship is standing on")
    void stockedAtByMarketId() {
        StationMarketsManager.MarketSnapshot shelf = markets.stockedAt(3803845888L).orElseThrow();

        assertEquals("Mat Zemlya", shelf.starSystem());
        assertEquals(1079, shelf.stockBySymbol().get("bertrandite"));
        assertFalse(shelf.stockBySymbol().containsKey("silver"), "a good listed at zero is not on the shelf");
    }

    /**
     * The lookup a card standing on a pad has to make. By name it is a coin toss between two ports; by id
     * it is this one.
     */
    @Test
    @DisplayName("a namesake in another system cannot answer for this market")
    void marketIdBeatsASharedStationName() {
        markets.save(GsonFactory.getGson().fromJson(NAMESAKE, GameEvents.MarketEvent.class));

        assertEquals("Sirius", markets.stockedAt(3999999999L).orElseThrow().starSystem());
        assertEquals("Mat Zemlya", markets.stockedAt(3803845888L).orElseThrow().starSystem());
    }

    /**
     * Two lookups share one memo, so the second must not be served the first one's answer.
     */
    @Test
    @DisplayName("asking by id and by name in turn answers each on its own terms")
    void theMemoDoesNotConfuseTheTwoLookups() {
        assertEquals(Optional.empty(), markets.stockedAt(1L));
        assertEquals("Mat Zemlya",
                markets.stockedAt("Boldyr Dredging Installation").orElseThrow().starSystem());
        assertEquals("Mat Zemlya", markets.stockedAt(3803845888L).orElseThrow().starSystem());
    }

    @Test
    @DisplayName("a market we have never opened is no shelf at all")
    void anUnknownMarketIdIsEmpty() {
        assertTrue(markets.stockedAt(4200000000L).isEmpty());
        assertTrue(markets.stockedAt(0L).isEmpty());
    }
}
