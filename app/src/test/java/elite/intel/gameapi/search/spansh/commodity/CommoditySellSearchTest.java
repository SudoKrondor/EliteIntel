package elite.intel.gameapi.search.spansh.commodity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.db.managers.StationMarketsManager;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchResultDto;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "Where can I sell this?" is the buy search read off the other side of the counter, and every way of
 * getting that wrong is silent.
 *
 * <p>A market entry carries both halves of every pair - {@code supply}/{@code demand},
 * {@code buy_price}/{@code sell_price} - so reading the wrong half never fails, it answers a different
 * question: a sell search filtered on supply returns the markets that already HAVE the good, which are
 * generally the ones that will pay least for it. Spansh compounds this by ignoring a filter key it does
 * not know and matching nothing against a value it does not know.
 *
 * <p>The wire shape is asserted on the serialized body for that reason, and the ranking against a captured
 * page, because Spansh hands the page back in distance order whatever was asked of it.
 */
class CommoditySellSearchTest {

    private static final int HOLD = 300;

    @Test
    void theMarketMustWantTheGoodRatherThanStockIt() {
        JsonObject marketplace = marketplaceOf(HOLD);

        assertTrue(marketplace.has("demand"), "a sell search is a search for demand");
        assertFalse(marketplace.has("supply"),
                "asking for supply would return the markets already holding it, not the ones that want it");
    }

    @Test
    void thePriceAskedAboutIsTheOneTheCommanderIsPaid() {
        JsonObject marketplace = marketplaceOf(HOLD);

        assertTrue(marketplace.has("sell_price"), "sell_price is the station's bid, which is our income");
        assertFalse(marketplace.has("buy_price"), "buy_price is the ask, and we are not buying");
        assertTrue(marketplace.getAsJsonObject("sell_price").getAsJsonArray("value").get(0).getAsInt() >= 1,
                "a bid of zero is a market that lists the good and will not take it");
    }

    @Test
    void theFirstSearchAsksForEnoughDemandToTakeTheWholeLoad() {
        JsonObject demand = marketplaceOf(HOLD).getAsJsonObject("demand");

        // Both ends set: measured live, {"comparison": ">=", ...} is accepted and then matches nothing.
        assertEquals("<=>", demand.get("comparison").getAsString());
        assertEquals(HOLD, demand.getAsJsonArray("value").get(0).getAsInt());
        assertEquals(2, demand.getAsJsonArray("value").size());
        assertTrue(demand.getAsJsonArray("value").get(1).getAsInt() > HOLD,
                "the upper bound must not exclude a market that wants plenty");
    }

    @Test
    void aPartSaleIsPreferredToReportingThatNobodyWantsIt() {
        // The same ladder the buy search climbs: ask for the whole load, then for any buyer at all.
        assertEquals(1, SpanshCommoditySearch.attemptsForTest(HOLD, 100).get(1).minUnits());
    }

    @Test
    void theBestPayingMarketLeadsABestPriceSearch() {
        List<CommoditySearchResult> ranked =
                SpanshCommoditySearch.rank(page(), "Tritium", false, TradeSide.SELL);

        assertEquals("Galileo", ranked.getFirst().getStationName(),
                "selling, the BEST price is the highest - the opposite end of the same sort");
        assertEquals(55000, ranked.getFirst().getPrice());
    }

    @Test
    void theNearestBuyerLeadsANearestSearch() {
        List<CommoditySearchResult> ranked =
                SpanshCommoditySearch.rank(page(), "Tritium", true, TradeSide.SELL);

        assertEquals("Daedalus", ranked.getFirst().getStationName());
    }

    @Test
    void theCheapestMarketStillLeadsABuySearchOfTheSamePage() {
        // The direction is the whole difference: the same page, ranked to buy, answers the other way round.
        assertEquals("Daedalus",
                SpanshCommoditySearch.rank(page(), "Tritium", false, TradeSide.BUY).getFirst().getStationName());
    }

    @Test
    void theUnitsReportedAreTheTonnesTheMarketWants() {
        CommoditySearchResult best = SpanshCommoditySearch.rank(page(), "Tritium", true, TradeSide.SELL).getFirst();

        assertEquals(4200, best.getSupply(),
                "on a sell result the units figure is demand - stock would be the tonnage we cannot sell them");
    }

    @Test
    void aMarketThatOnlySellsTheGoodIsNoBuyer() {
        // Kanwar stocks Tritium and bids nothing for it: a market to buy from, never one to sell to.
        assertTrue(SpanshCommoditySearch.rank(sellerOnlyPage(), "Tritium", true, TradeSide.SELL).isEmpty());
        assertFalse(SpanshCommoditySearch.rank(sellerOnlyPage(), "Tritium", true, TradeSide.BUY).isEmpty());
    }

    @Test
    void ourOwnLookAtTheMarketCorrectsTheDemand() {
        List<CommoditySearchResult> kept = SpanshCommoditySearch.correctWithFirstHandData(
                List.of(result("Deciat", "Daedalus", 4200)), "tritium",
                sighting(0, 90), TradeSide.SELL);

        assertEquals(90, kept.getFirst().getSupply(), "the demand we saw, not the stock");
    }

    @Test
    void aMarketWeSawWantingNoneOfItIsDropped() {
        assertTrue(SpanshCommoditySearch.correctWithFirstHandData(
                        List.of(result("Deciat", "Daedalus", 4200)), "tritium",
                        sighting(0, 0), TradeSide.SELL).isEmpty(),
                "we stood in it and it wanted none");
    }

    @Test
    void anEmptyShelfDoesNotDisqualifyABuyer() {
        // The trap: a market that WANTS a good normally holds none of it, so reading stock here would
        // drop every buyer worth flying to.
        List<CommoditySearchResult> kept = SpanshCommoditySearch.correctWithFirstHandData(
                List.of(result("Deciat", "Daedalus", 4200)), "tritium",
                sighting(0, 5000), TradeSide.SELL);

        assertEquals(1, kept.size());
        assertEquals(5000, kept.getFirst().getSupply());
    }

    // === helpers ===

    private static SpanshCommoditySearch.MarketSightings sighting(int stock, int demand) {
        return (system, station, symbol) -> Optional.of(new StationMarketsManager.Sighting(
                system, station, Instant.now().plus(1, ChronoUnit.DAYS), stock, demand, 0, 0));
    }

    private static CommoditySearchResult result(String system, String station, long units) {
        CommoditySearchResult result = new CommoditySearchResult();
        result.setStarSystem(system);
        result.setStationName(station);
        result.setSupply(units);
        result.setMarketUpdatedAt(Instant.now().minus(30, ChronoUnit.DAYS).toString());
        return result;
    }

    private static JsonObject marketplaceOf(int wanted) {
        TradeRouteSearchCriteria profile = new TradeRouteSearchCriteria();
        profile.setMaxCargo(HOLD);
        profile.setMaxLsFromArrival(6000);
        TradeStationSearchCriteria criteria = SpanshCommoditySearch.searchCriteria(
                "Tritium", "Sol", profile, false,
                new SpanshCommoditySearch.Attempt(
                        TradeStationSearchCriteria.StationType.EVERY_STATIC_TRADE_TYPE, wanted, 60, false),
                TradeSide.SELL);
        return JsonParser.parseString(criteria.toJson()).getAsJsonObject()
                .getAsJsonObject("filters").getAsJsonArray("marketplace").get(0).getAsJsonObject();
    }

    /**
     * Two markets that both buy Tritium. Daedalus is nearer and bids less, Galileo is farther and bids
     * more, so distance order and price order disagree - and both are Coriolis, so docking effort cannot
     * decide it either.
     */
    private static List<TradeStationSearchResultDto.StationResult> page() {
        String json = """
                {"results":[
                  {"name":"Daedalus","system_name":"Sol","type":"Coriolis Starport","distance":0.0,
                   "distance_to_arrival":500.0,"has_market":true,
                   "market":[{"commodity":"Tritium","buy_price":51000,"sell_price":49500,"supply":120,"demand":4200}]},
                  {"name":"Galileo","system_name":"Sol","type":"Coriolis Starport","distance":18.6,
                   "distance_to_arrival":500.0,"has_market":true,
                   "market":[{"commodity":"Tritium","buy_price":53000,"sell_price":55000,"supply":0,"demand":9100}]}
                ]}""";
        return GsonFactory.getGson().fromJson(json, TradeStationSearchResultDto.class).getResults();
    }

    /**
     * A market with Tritium on the shelf and no bid for it.
     */
    private static List<TradeStationSearchResultDto.StationResult> sellerOnlyPage() {
        String json = """
                {"results":[
                  {"name":"Kanwar Gateway","system_name":"Deciat","type":"Coriolis Starport","distance":4.0,
                   "distance_to_arrival":300.0,"has_market":true,
                   "market":[{"commodity":"Tritium","buy_price":50000,"sell_price":0,"supply":5477,"demand":0}]}
                ]}""";
        return GsonFactory.getGson().fromJson(json, TradeStationSearchResultDto.class).getResults();
    }
}
