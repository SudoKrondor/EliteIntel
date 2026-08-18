package elite.intel.gameapi.search.spansh.commodity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchResultDto;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "Where can I buy this?" is one Spansh stations search, and the whole of the answer's correctness is in
 * the shape of the request: Spansh ignores a filter key it does not recognise, matches nothing against a
 * value it does not know, and accepts at least one comparison operator that then matches nothing either.
 * Every one of those mistakes narrows the search silently instead of failing it, and the commander simply
 * hears that the commodity is nowhere to be found.
 *
 * <p>Asserted on the serialized body, because the wire shape is the behaviour. The ranking half is
 * asserted against a captured response, because Spansh hands the page back in distance order whatever was
 * asked of it - a best-price search that never re-sorted would look perfectly healthy in the log.
 */
class CommodityMarketSearchTest {

    private static final int HOLD = 300;

    /**
     * Spansh's station type vocabulary, from {@code GET /api/stations/field_values/type}.
     */
    private static final Set<String> SPANSH_STATION_TYPES = Set.of(
            "Asteroid base", "Coriolis Starport", "Dodec Starport", "Drake-Class Carrier", "Mega ship",
            "Ocellus Starport", "Orbis Starport", "Outpost", "Planetary Construction Depot",
            "Planetary Outpost", "Planetary Port", "Settlement", "Space Construction Depot",
            "Surface Settlement"
    );

    @Test
    void theCommodityIsAskedForOnTheMarketplaceFilter() {
        // Not "market": that key IS accepted and does nothing, which is how this search came to walk
        // every station's market in Java instead.
        JsonObject marketplace = marketplaceOf(profile(), "Gold");

        assertEquals("Gold", marketplace.getAsJsonArray("commodity").get(0).getAsString());
    }

    @Test
    void theMarketMustHaveAHoldsWorthInStock() {
        JsonObject supply = marketplaceOf(profile(), "Gold").getAsJsonObject("supply");

        assertEquals("<=>", supply.get("comparison").getAsString());
        assertEquals(HOLD, supply.getAsJsonArray("value").get(0).getAsInt(),
                "a market with less than a hold's worth sends the commander out for a part load");
        assertTrue(supply.getAsJsonArray("value").get(1).getAsInt() > HOLD,
                "the upper bound must not exclude a market with plenty");
    }

    @Test
    void everyRangeIsSentWithBothEndsSet() {
        // Measured against the live search: {"comparison": ">=", "value": [500]} is accepted with a 200 and
        // then matches NOTHING, where the same question as a "<=>" range returned 550 stations.
        JsonObject marketplace = marketplaceOf(profile(), "Gold");

        for (String range : List.of("supply", "buy_price")) {
            JsonArray value = marketplace.getAsJsonObject(range).getAsJsonArray("value");
            assertEquals("<=>", marketplace.getAsJsonObject(range).get("comparison").getAsString(), range);
            assertEquals(2, value.size(), () -> range + " must carry both ends of the range");
        }
    }

    @Test
    void onlyMarketsActuallySellingItQualify() {
        JsonArray buyPrice = marketplaceOf(profile(), "Gold").getAsJsonObject("buy_price").getAsJsonArray("value");

        assertTrue(buyPrice.get(0).getAsInt() >= 1,
                "a price of zero is a market that lists the good and does not sell it");
    }

    @Test
    void theLightYearRadiusIsSentAsAMinMaxPair() {
        /// Spansh accepts the radius as a "<=>" range like every other filter here and then IGNORES it:
        /// measured live, a 50 ly search came back with stations 400 ly out.
        JsonObject distance = filtersOf(criteriaOf(profile(), "Gold", 60, false)).getAsJsonObject("distance");

        assertFalse(distance.has("comparison"), "the radius is the one filter that takes min/max instead");
        assertEquals(0, distance.get("min").getAsInt());
        assertEquals(60, distance.get("max").getAsInt());
    }

    @Test
    void theArrivalDistanceIsTheProfilesOwn() {
        TradeRouteSearchCriteria profile = profile();
        profile.setMaxLsFromArrival(1500);

        JsonArray range = filtersOf(criteriaOf(profile, "Gold", 60, false))
                .getAsJsonObject("distance_to_arrival").getAsJsonArray("value");

        assertEquals(0, range.get(0).getAsInt());
        assertEquals(1500, range.get(1).getAsInt(),
                "light seconds from the arrival star, which is not the light year radius");
    }

    @Test
    void thePageComesBackNearestFirst() {
        // Spansh returns index order for an empty sort, and cannot sort by one commodity's price at all
        // (a buy_price sort is accepted and quietly ignored), so distance order is what there is to work
        // with - and what the nearest-market answer is read straight off.
        JsonArray sort = body(criteriaOf(profile(), "Gold", 60, true)).getAsJsonArray("sort");

        assertEquals(1, sort.size(), () -> "no sort means no nearest: " + sort);
        assertEquals("asc", sort.get(0).getAsJsonObject().getAsJsonObject("distance").get("direction").getAsString());
    }

    @Test
    void distancesAreMeasuredFromTheSystemWeAreIn() {
        JsonObject body = body(criteriaOf(profile(), "Gold", 60, true));

        assertEquals("Sol", body.get("reference_system").getAsString());
        assertFalse(body.has("reference_coords"), "two references would leave which one wins to Spansh");
    }

    @Test
    void aBestPriceSearchWeighsMoreMarketsThanANearestSearch() {
        // Every result carries the station's entire market, so the wider page is paid for in megabytes -
        // but the cheapest of ten neighbours is barely a price search at all.
        int nearest = body(criteriaOf(profile(), "Gold", 60, true)).get("size").getAsInt();
        int bestPrice = body(criteriaOf(profile(), "Gold", 60, false)).get("size").getAsInt();

        assertTrue(bestPrice > nearest, "best price: " + bestPrice + ", nearest: " + nearest);
    }

    @Test
    void everyStationTypeWeAskForIsOneSpanshKnows() {
        TradeRouteSearchCriteria profile = profile();
        profile.setAllowPlanetary(true);
        profile.setAllowFleetCarriers(true);

        for (String type : stationTypesOf(profile)) {
            assertTrue(SPANSH_STATION_TYPES.contains(type),
                    () -> "'" + type + "' is not in Spansh's station type vocabulary, so it matches nothing");
        }
    }

    @Test
    void surfacePortsAndCarriersAreOfferedOnlyWhenTheProfileAllowsThem() {
        assertFalse(stationTypesOf(profile()).contains("Planetary Port"));
        assertFalse(stationTypesOf(profile()).contains("Drake-Class Carrier"),
                "a carrier can jump away between the search and the arrival");

        TradeRouteSearchCriteria permissive = profile();
        permissive.setAllowPlanetary(true);
        permissive.setAllowFleetCarriers(true);
        assertTrue(stationTypesOf(permissive).containsAll(List.of("Planetary Port", "Drake-Class Carrier")));
    }

    @Test
    void aLargeShipIsOnlySentWhereItCanLand() {
        TradeRouteSearchCriteria profile = profile();
        profile.setRequiresLargePad(true);

        JsonObject largePads = filtersOf(criteriaOf(profile, "Gold", 60, false)).getAsJsonObject("large_pads");
        assertEquals(1, largePads.getAsJsonArray("value").get(0).getAsInt(), "at least one large pad");
        assertTrue(largePads.getAsJsonArray("value").get(1).getAsInt() > 1,
                "the upper bound must not exclude stations with several large pads");

        assertFalse(filtersOf(criteriaOf(profile(), "Gold", 60, false)).has("large_pads"),
                "a medium or small ship must still see outposts");
    }

    @Test
    void theNameWeWereGivenIsAskedForFirst() {
        // Right for 389 of the 398 goods Spansh sells; the variants behind it are guesses at the rest.
        assertEquals("Agri-Medicines", SpanshCommoditySearch.spellings("Agri-Medicines").getFirst());
    }

    @Test
    void aNameThatIsAlreadyRightIsNeverReCasedAwayFromSpansh() {
        // Title-casing these on the way out is what broke the search: "Agri-medicines", "H.e. Suits" and
        // "Cmm Composite" each return zero stations from a search that has just told the commander it is
        // looking for them. A wrong spelling may ride ALONG as a variant - the filter ignores what it does
        // not know - but the right one has to be in the list, and first.
        for (String spanshSpelling : List.of("Agri-Medicines", "H.E. Suits", "CMM Composite", "AI Relics", "Fruit and Vegetables")) {
            assertEquals(spanshSpelling, SpanshCommoditySearch.spellings(spanshSpelling).getFirst());
        }
    }

    @Test
    void spellingsCoverTheCasingsSpanshUsesWhereOurTableDisagrees() {
        // Our commodities table and Spansh disagree on these, and Spansh's spelling is the one that
        // matches. Sending both is free: the filter ORs the list and ignores a name it does not know.
        assertTrue(SpanshCommoditySearch.spellings("Liquid Oxygen").contains("Liquid oxygen"));
        assertTrue(SpanshCommoditySearch.spellings("The Waters Of Shintara").contains("The Waters of Shintara"));
        assertTrue(SpanshCommoditySearch.spellings("Eden Apples Of Aerial").contains("Eden Apples of Aerial"));
        assertTrue(SpanshCommoditySearch.spellings("AZ Cancri Formula 42").contains("Az Cancri Formula 42"));
    }

    @Test
    void aCommodityHeardInAnyCaseStillReachesTheRightMarkets() {
        // "gold" and "GOLD" both return zero stations; only "Gold" is a search.
        assertTrue(SpanshCommoditySearch.spellings("gold").contains("Gold"));
        assertTrue(SpanshCommoditySearch.spellings("fruit and vegetables").contains("Fruit and Vegetables"));
    }

    @Test
    void theSameSpellingIsNeverSentTwice() {
        List<String> spellings = SpanshCommoditySearch.spellings("Gold");

        assertEquals(spellings.stream().distinct().count(), spellings.size(), () -> spellings.toString());
    }

    @Test
    void theCheapestMarketLeadsAPriceSearch() {
        List<CommoditySearchResult> results = SpanshCommoditySearch.rank(page(), "Gold", false);

        assertEquals(44824, results.getFirst().getPrice());
        assertEquals("Galileo", results.getFirst().getStationName());
    }

    @Test
    void theNearestMarketLeadsANearestSearch() {
        List<CommoditySearchResult> results = SpanshCommoditySearch.rank(page(), "Gold", true);

        assertEquals("Daedalus", results.getFirst().getStationName());
        assertEquals(0.0, results.getFirst().getDistanceFromPlayer());
    }

    @Test
    void thePriceReportedIsTheOneTheCommanderPays() {
        // Spansh names the fields from the station's side of the counter: buy_price is the ask, sell_price
        // the bid. Reading the wrong one quotes a price the commander can never buy at.
        CommoditySearchResult daedalus = SpanshCommoditySearch.rank(page(), "Gold", true).getFirst();

        assertEquals(45091, daedalus.getPrice(), "buy_price, not the 44137 the station would pay us");
        assertEquals("Gold", daedalus.getCommodity());
        assertEquals("Sol", daedalus.getStarSystem());
        assertEquals("Orbis Starport", daedalus.getStationType());
    }

    @Test
    void aMarketThatDoesNotSellItIsNotAnAnswer() {
        // The marketplace filter has already answered this - but a row that arrives without the commodity,
        // or listing it at no price, must be dropped rather than reported at a price of zero.
        assertTrue(SpanshCommoditySearch.rank(page(), "Painite", true).isEmpty());
    }

    // === helpers ===

    private static TradeRouteSearchCriteria profile() {
        TradeRouteSearchCriteria profile = new TradeRouteSearchCriteria();
        profile.setMaxCargo(HOLD);
        profile.setMaxLsFromArrival(6000);
        return profile;
    }

    private static TradeStationSearchCriteria criteriaOf(TradeRouteSearchCriteria profile, String commodity, int maxLy, boolean nearest) {
        return SpanshCommoditySearch.searchCriteria(commodity, "Sol", maxLy, profile, nearest);
    }

    private static JsonObject body(TradeStationSearchCriteria criteria) {
        return JsonParser.parseString(criteria.toJson()).getAsJsonObject();
    }

    private static JsonObject filtersOf(TradeStationSearchCriteria criteria) {
        return body(criteria).getAsJsonObject("filters");
    }

    private static JsonObject marketplaceOf(TradeRouteSearchCriteria profile, String commodity) {
        return filtersOf(criteriaOf(profile, commodity, 60, false))
                .getAsJsonArray("marketplace").get(0).getAsJsonObject();
    }

    private static List<String> stationTypesOf(TradeRouteSearchCriteria profile) {
        JsonArray types = filtersOf(criteriaOf(profile, "Gold", 60, false))
                .getAsJsonObject("type").getAsJsonArray("value");
        return types.asList().stream().map(com.google.gson.JsonElement::getAsString).toList();
    }

    /**
     * Two stations off a real {@code /api/stations/search/recall} page, trimmed to the fields read here.
     * Daedalus is the nearer, Galileo the cheaper, so distance order and price order disagree.
     */
    private static List<TradeStationSearchResultDto.StationResult> page() {
        String json = """
                {"results":[
                  {"name":"Daedalus","system_name":"Sol","type":"Orbis Starport","distance":0.0,
                   "distance_to_arrival":157.0,"has_market":true,
                   "market":[{"commodity":"Gold","buy_price":45091,"sell_price":44137,"supply":61339,"demand":1},
                             {"commodity":"Silver","buy_price":5000,"sell_price":4900,"supply":100,"demand":0}]},
                  {"name":"Galileo","system_name":"Sol","type":"Ocellus Starport","distance":9.7,
                   "distance_to_arrival":496.0,"has_market":true,
                   "market":[{"commodity":"Gold","buy_price":44824,"sell_price":43876,"supply":75095,"demand":1},
                             {"commodity":"Painite","buy_price":0,"sell_price":40000,"supply":0,"demand":900}]}
                ]}""";
        return GsonFactory.getGson().fromJson(json, TradeStationSearchResultDto.class).getResults();
    }
}
