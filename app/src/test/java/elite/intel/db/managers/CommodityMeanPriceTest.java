package elite.intel.db.managers;

import elite.intel.db.util.Database;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.util.Cypher;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A price on its own says nothing. 57,844 credits a tonne for Tritium is an excellent sale or a poor one
 * depending entirely on the 51,294 it is measured against, and the game puts that figure on every line of
 * every {@code Market.json} - which the app was already storing for other reasons and never reading.
 *
 * <p>The averages are harvested where every market board already passes through, so they accumulate as the
 * commander flies rather than needing a fetch of their own.
 */
class CommodityMeanPriceTest {

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    private static final String LOVE_HUB = """
            {"timestamp":"2026-08-28T21:07:10Z","event":"Market","MarketID":4335542787,
             "StationName":"Love Hub","StationType":"Dodec","StarSystem":"Col 285 Sector IB-X d1-32",
             "Items":[
               {"Name":"$tritium_name;","Name_Localised":"Tritium","BuyPrice":0,"SellPrice":57844,
                "MeanPrice":51294,"Stock":0,"Demand":212636},
               {"Name":"$liquidoxygen_name;","Name_Localised":"Liquid oxygen","BuyPrice":0,"SellPrice":2443,
                "MeanPrice":1375,"Stock":0,"Demand":11729512},
               {"Name":"$hydrogenfuel_name;","Name_Localised":"Hydrogen Fuel","BuyPrice":127,"SellPrice":120,
                "MeanPrice":0,"Stock":1381903,"Demand":1}]}""";

    @Test
    void everyAverageOnTheBoardIsLearnedFromOneVisit() {
        harvest(LOVE_HUB);

        assertEquals(51294, CommodityMeanPriceManager.getInstance().meanPrice("tritium").orElseThrow());
        assertEquals(1375, CommodityMeanPriceManager.getInstance().meanPrice("liquidoxygen").orElseThrow());
    }

    @Test
    void theRawJournalNameIsAcceptedAsWellAsTheBareSymbol() {
        harvest(LOVE_HUB);

        assertEquals(51294, CommodityMeanPriceManager.getInstance().meanPrice("$tritium_name;").orElseThrow(),
                "the caller may be holding either form; normalising is this manager's job");
    }

    @Test
    void aBoardThatReportsNoAverageTeachesNothing() {
        harvest(LOVE_HUB);

        // Fleet carriers report MeanPrice 0, and zero is not an average - storing it would answer a later
        // question wrongly rather than not at all.
        assertTrue(CommodityMeanPriceManager.getInstance().meanPrice("hydrogenfuel").isEmpty());
    }

    @Test
    void aGoodWeHaveNeverSeenListedHasNoAverage() {
        assertTrue(CommodityMeanPriceManager.getInstance().meanPrice("nonexistentwidget").isEmpty());
    }

    @Test
    void nullAndBlankSymbolsAreAnswerableWithoutBlowingUp() {
        assertTrue(CommodityMeanPriceManager.getInstance().meanPrice(null).isEmpty());
        assertTrue(CommodityMeanPriceManager.getInstance().meanPrice("  ").isEmpty());
    }

    @Test
    void theNewestSightingWins() {
        harvest(LOVE_HUB);
        harvest(LOVE_HUB.replace("\"MeanPrice\":51294", "\"MeanPrice\":52000")
                .replace("2026-08-28T21:07:10Z", "2026-08-29T09:00:00Z"));

        assertEquals(52000, CommodityMeanPriceManager.getInstance().meanPrice("tritium").orElseThrow(),
                "the average is a galaxy-wide constant, so a later look is the better number");
    }

    @Test
    void anEmptyOrAbsentBoardIsIgnored() {
        int before = CommodityMeanPriceManager.getInstance().known();
        CommodityMeanPriceManager.getInstance().harvest(null);
        harvest("""
                {"timestamp":"2026-08-28T21:07:10Z","event":"Market","MarketID":1,"Items":[]}""");

        assertEquals(before, CommodityMeanPriceManager.getInstance().known());
    }

    private static void harvest(String marketJson) {
        CommodityMeanPriceManager.getInstance().harvest(
                GsonFactory.getGson().fromJson(marketJson, GameEvents.MarketEvent.class));
    }
}
