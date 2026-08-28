package elite.intel.gameapi.search.spansh.commodity;

import elite.intel.db.managers.StationMarketsManager;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchResultDto;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Weighing a market against a whole shopping list rather than one commodity.
 * <p>
 * The point of the exercise: a colonisation build's long tail is nine or ten commodities of sixty tonnes
 * each. Fetched one at a time that is nine round trips with the hold empty for eight of them, and the data
 * needed to avoid it is already in the response - every Spansh station row carries that station's entire
 * market, and the single-commodity path throws all but one entry away.
 * <p>
 * No live search: these exercise the ranking and hold allocation against captured pages, the same way the
 * single-commodity ranking is asserted.
 */
class CommodityBasketSearchTest {

    /**
     * Nothing was ever seen first-hand, so Spansh's own supply figures stand.
     */
    private static final SpanshCommoditySearch.MarketSightings NEVER_BEEN_THERE =
            (system, station, symbol) -> Optional.empty();

    private static WantedCommodity want(String commodity, int units) {
        return new WantedCommodity(commodity.toLowerCase().replace(" ", ""), commodity, units);
    }

    /**
     * Three markets that all sell the anchor. Bujold is nearest and has only the anchor; Kanwar is further
     * out and stocks four of the wanted goods; Hume is furthest and stocks two.
     */
    private static List<TradeStationSearchResultDto.StationResult> page() {
        String json = """
                {"results":[
                  {"name":"Bujold Terminal","system_name":"Sterope","type":"Coriolis Starport","distance":28.0,
                   "distance_to_arrival":120.0,"has_market":true,
                   "market":[{"commodity":"Ceramic Composites","buy_price":724,"sell_price":700,"supply":900,"demand":0}]},
                  {"name":"Kanwar Gateway","system_name":"Hyades Sector","type":"Coriolis Starport","distance":41.0,
                   "distance_to_arrival":200.0,"has_market":true,
                   "market":[{"commodity":"Ceramic Composites","buy_price":730,"sell_price":700,"supply":900,"demand":0},
                             {"commodity":"Polymers","buy_price":682,"sell_price":650,"supply":900,"demand":0},
                             {"commodity":"Copper","buy_price":1050,"sell_price":1000,"supply":900,"demand":0},
                             {"commodity":"Water","buy_price":662,"sell_price":600,"supply":900,"demand":0}]},
                  {"name":"Hume Dock","system_name":"Maia","type":"Coriolis Starport","distance":52.0,
                   "distance_to_arrival":300.0,"has_market":true,
                   "market":[{"commodity":"Ceramic Composites","buy_price":700,"sell_price":680,"supply":900,"demand":0},
                             {"commodity":"Copper","buy_price":900,"sell_price":880,"supply":900,"demand":0}]}
                ]}""";
        return GsonFactory.getGson().fromJson(json, TradeStationSearchResultDto.class).getResults();
    }

    /**
     * Divis Gateway's outstanding long tail: the small lines that would otherwise be one trip each.
     */
    private static List<WantedCommodity> longTail() {
        return List.of(
                want("Ceramic Composites", 85),
                want("Polymers", 170),
                want("Copper", 85),
                want("Water", 22));
    }

    @Test
    void theMarketThatFillsMostOfTheHoldWinsEvenThoughItIsFurtherOut() {
        List<BasketResult> ranked =
                SpanshCommoditySearch.rankBaskets(page(), longTail(), 512, true, NEVER_BEEN_THERE);

        assertEquals("Kanwar Gateway", ranked.getFirst().stationName(),
                "28 ly for 85 tonnes against 41 ly for 362 is not a close call");
        assertEquals(362, ranked.getFirst().totalUnits());
        assertEquals(4, ranked.getFirst().lines().size());
    }

    @Test
    void everyCandidateStillHasToSellTheAnchor() {
        List<BasketResult> ranked =
                SpanshCommoditySearch.rankBaskets(page(), longTail(), 512, true, NEVER_BEEN_THERE);

        assertEquals(3, ranked.size());
        for (BasketResult market : ranked) {
            assertEquals("Ceramic Composites", market.anchor().commodity(),
                    "the anchor is the reason for the trip and is always the first line");
        }
    }

    /**
     * The whole reason the search is anchored at all: a market with plenty of the small goods and none of
     * the one the commander is actually short of is not where this trip is going.
     */
    @Test
    void aMarketWithoutTheAnchorIsNoCandidateHoweverMuchElseItHas() {
        String json = """
                {"results":[
                  {"name":"Wrong Shop","system_name":"Sterope","type":"Coriolis Starport","distance":5.0,
                   "distance_to_arrival":100.0,"has_market":true,
                   "market":[{"commodity":"Polymers","buy_price":682,"sell_price":650,"supply":900,"demand":0},
                             {"commodity":"Copper","buy_price":1050,"sell_price":1000,"supply":900,"demand":0}]}
                ]}""";
        List<TradeStationSearchResultDto.StationResult> stations =
                GsonFactory.getGson().fromJson(json, TradeStationSearchResultDto.class).getResults();

        assertTrue(SpanshCommoditySearch.rankBaskets(stations, longTail(), 512, true, NEVER_BEEN_THERE).isEmpty());
    }

    @Test
    void theHoldIsNeverOverfilled() {
        List<BasketResult> ranked =
                SpanshCommoditySearch.rankBaskets(page(), longTail(), 100, true, NEVER_BEEN_THERE);

        BasketResult best = ranked.getFirst();
        assertEquals(100, best.totalUnits(), "a 100 tonne hold cannot carry 362 tonnes");
        assertEquals("Ceramic Composites", best.lines().getFirst().commodity());
        assertEquals(85, best.lines().getFirst().unitsToBuy(), "the anchor gets its full shortfall first");
        assertEquals(15, best.lines().get(1).unitsToBuy(), "and the next good takes what is left");
    }

    @Test
    void aHoldWithRoomForOnlyTheAnchorReadsAsASingleErrand() {
        BasketResult best = SpanshCommoditySearch
                .rankBaskets(page(), longTail(), 85, true, NEVER_BEEN_THERE).getFirst();

        assertEquals(1, best.lines().size());
        assertFalse(best.isMultiBuy());
    }

    @Test
    void neverMoreOfAGoodThanIsStillWanted() {
        BasketResult best = SpanshCommoditySearch
                .rankBaskets(page(), List.of(want("Ceramic Composites", 85)), 512, true, NEVER_BEEN_THERE)
                .getFirst();

        assertEquals(85, best.totalUnits(), "the site wants 85 tonnes; a 512 tonne hold does not change that");
    }

    @Test
    void neverMoreOfAGoodThanTheMarketHas() {
        String json = """
                {"results":[
                  {"name":"Thin Shelves","system_name":"Sterope","type":"Coriolis Starport","distance":5.0,
                   "distance_to_arrival":100.0,"has_market":true,
                   "market":[{"commodity":"Ceramic Composites","buy_price":724,"sell_price":700,"supply":20,"demand":0}]}
                ]}""";
        List<TradeStationSearchResultDto.StationResult> stations =
                GsonFactory.getGson().fromJson(json, TradeStationSearchResultDto.class).getResults();

        BasketResult best = SpanshCommoditySearch
                .rankBaskets(stations, longTail(), 512, true, NEVER_BEEN_THERE).getFirst();

        assertEquals(20, best.anchor().unitsToBuy(), "a part load is still a real answer");
    }

    /**
     * Spansh is crowd-sourced and {@code Market.json} is the game speaking. Applied per line rather than per
     * station: one emptied shelf should cost the commander that good, not the whole trip.
     */
    @Test
    void ourOwnLookAtAnEmptyShelfDropsThatGoodAndKeepsTheRest() {
        SpanshCommoditySearch.MarketSightings sawNoCopper = (system, station, symbol) ->
                "copper".equals(symbol) && "Kanwar Gateway".equals(station)
                        ? Optional.of(new StationMarketsManager.Sighting(system, station, Instant.now(), 0, 0, 0, 0))
                        : Optional.empty();

        BasketResult best = SpanshCommoditySearch
                .rankBaskets(page(), longTail(), 512, true, sawNoCopper).getFirst();

        assertEquals("Kanwar Gateway", best.stationName());
        assertTrue(best.lines().stream().noneMatch(line -> line.commodity().equals("Copper")));
        assertEquals(277, best.totalUnits(), "362 less the 85 tonnes of copper that are not there");
    }

    /**
     * The same veto against the anchor is fatal to the station, because the anchor is why the trip exists.
     */
    @Test
    void ourOwnLookAtAnEmptyAnchorShelfDropsTheStation() {
        SpanshCommoditySearch.MarketSightings sawNoCeramics = (system, station, symbol) ->
                "ceramiccomposites".equals(symbol) && "Kanwar Gateway".equals(station)
                        ? Optional.of(new StationMarketsManager.Sighting(system, station, Instant.now(), 0, 0, 0, 0))
                        : Optional.empty();

        List<BasketResult> ranked =
                SpanshCommoditySearch.rankBaskets(page(), longTail(), 512, true, sawNoCeramics);

        assertTrue(ranked.stream().noneMatch(market -> market.stationName().equals("Kanwar Gateway")));
        assertEquals("Hume Dock", ranked.getFirst().stationName(), "the next fullest takes over");
    }

    /**
     * Two missions for the same good are two rows on the board and one good at the market. Left unmerged,
     * the allocator would sell the commander the same tonnes twice.
     */
    @Test
    void aStackOfMissionsForOneGoodIsMergedIntoASingleLine() {
        List<WantedCommodity> merged = SpanshCommoditySearch.mergeDuplicates(List.of(
                want("Ceramic Composites", 18),
                want("Polymers", 40),
                want("Ceramic Composites", 72)));

        assertEquals(2, merged.size());
        assertEquals("Ceramic Composites", merged.getFirst().commodity(), "the first appearance keeps its place");
        assertEquals(90, merged.getFirst().unitsWanted());
    }

    @Test
    void aLineWantingNothingIsNotShopping() {
        assertTrue(SpanshCommoditySearch.mergeDuplicates(List.of(want("Copper", 0))).isEmpty());
    }

    @Test
    void anEmptyOrNullListIsQuietRatherThanFatal() {
        assertTrue(SpanshCommoditySearch.mergeDuplicates(null).isEmpty());
        assertTrue(SpanshCommoditySearch.rankBaskets(page(), List.of(), 512, true, NEVER_BEEN_THERE).isEmpty());
    }
}
