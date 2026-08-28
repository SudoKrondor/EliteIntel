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
    void theFirstSearchAsksForAHoldsWorthInStock() {
        JsonObject supply = marketplaceOf(profile(), "Gold").getAsJsonObject("supply");

        assertEquals("<=>", supply.get("comparison").getAsString());
        assertEquals(HOLD, supply.getAsJsonArray("value").get(0).getAsInt(),
                "a full load is what the first attempt looks for; a later one settles for less");
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
        for (String type : stationTypesOf(profile())) {
            assertTrue(SPANSH_STATION_TYPES.contains(type),
                    () -> "'" + type + "' is not in Spansh's station type vocabulary, so it matches nothing");
        }
    }

    @Test
    void everyKindOfMarketThatStaysPutIsSearched() {
        // Measured live: of the 440 goods in our commodities table, 140 are on sale at no starport anywhere
        // in the galaxy. "Micro-weave Cooling Hoses" - a common mission cargo - is stocked by 2,661
        // settlements and by no starport at all, so a search over starports alone can only report a good the
        // commander is looking at in his own transaction panel as being nowhere to be found.
        List<String> types = stationTypesOf(profile());

        assertTrue(types.contains("Settlement"), () -> types.toString());
        assertTrue(types.containsAll(TradeStationSearchCriteria.StationType.PLANETARY_TRADE_TYPES), () -> types.toString());
        assertTrue(types.containsAll(TradeStationSearchCriteria.StationType.ORBITAL_TRADE_TYPES), () -> types.toString());
    }

    @Test
    void theFirstSearchAsksForNoFleetCarrier() {
        // A carrier jumps: its owner can move it hundreds of light years between Spansh's last sync and the
        // commander arriving. So a market that stays put is always preferred, and the carrier is a fallback
        // (see theFallbackAsksForFleetCarriersAlone), never a competitor on the first page.
        List<String> types = stationTypesOf(profile());

        assertFalse(types.contains("Drake-Class Carrier"), () -> types.toString());
    }

    @Test
    void aPartLoadIsPreferredToReportingTheGoodAsNonexistent() {
        // Measured live: 1,726 markets sell "Neofabric Insulation", but only FOUR hold 300 tonnes of it and
        // none at all within 40 ly. Insisting on the ship's whole capacity told a commander that an ordinary
        // industrial good on sale 20 ly away does not exist - and the bigger his ship, the more goods
        // vanished. The widened attempt asks only that the market be selling.
        JsonObject supply = filtersOf(criteriaOf(profile(), "Neofabric Insulation", 60, true,
                TradeStationSearchCriteria.StationType.EVERY_STATIC_TRADE_TYPE, 1))
                .getAsJsonArray("marketplace").get(0).getAsJsonObject().getAsJsonObject("supply");

        assertEquals(1, supply.getAsJsonArray("value").get(0).getAsInt());
    }

    @Test
    void theRadiusDoublesBeforeAnyFleetCarrierIsConsidered() {
        // A carrier is a mobile station and its position is only where Spansh last saw it, so it is the last
        // resort - after the fixed markets have been asked twice, at the stated radius and at double it.
        // "Hardware Diagnostic Sensor" has 3 static markets in the galaxy against 147 carrier listings, and
        // reaching for a carrier while a real starport sat outside the radius sent a commander to empty space.
        List<String> order = SpanshCommoditySearch.attemptsForTest(HOLD, 100).stream()
                .map(attempt -> (attempt.stationTypes().size() == 1 ? "carrier" : "static")
                        + "@" + attempt.maxDistanceLy())
                .toList();

        assertEquals(List.of("static@100", "static@100", "static@200", "static@1000", "carrier@1000"), order,
                "the bubble sweep must come before any carrier: measured live, nothing static sold Hardware "
                        + "Diagnostic Sensor inside 120 ly, but Kanwar Gateway had 5,477 units at 202 ly");
    }

    @Test
    void theFirstAttemptAsksForTheAmountWanted() {
        // A mission still owing 20 tonnes after a part load wants 20, not a hold's worth. Asking for the
        // full 300 first passes over every nearby market holding 25 and answers with a big one further out,
        // and the commander is sent across the bubble for cargo that was two jumps away.
        assertEquals(20, SpanshCommoditySearch.attemptsForTest(20, 100).getFirst().minUnits());
        assertEquals(HOLD, SpanshCommoditySearch.attemptsForTest(HOLD, 100).getFirst().minUnits());
    }

    @Test
    void aPartLoadIsStillOfferedWhenNobodyHoldsTheWholeAmount() {
        // The second rung drops the floor to "selling at all" at the SAME radius, so buying what one market
        // has is a real answer - the next search asks for whatever is left after that stop.
        assertEquals(1, SpanshCommoditySearch.attemptsForTest(20, 100).get(1).minUnits());
        assertEquals(100, SpanshCommoditySearch.attemptsForTest(20, 100).get(1).maxDistanceLy(),
                "the part load is looked for where the commander asked, before the radius widens");
    }

    @Test
    void everyStaticAttemptIsExhaustedBeforeTheCarrierOne() {
        List<SpanshCommoditySearch.Attempt> attempts = SpanshCommoditySearch.attemptsForTest(HOLD, 100);
        int firstCarrier = attempts.indexOf(attempts.stream()
                .filter(a -> a.stationTypes().equals(TradeStationSearchCriteria.StationType.CARRIER_TRADE_TYPES))
                .findFirst().orElseThrow());

        assertEquals(attempts.size() - 1, firstCarrier, "the carrier attempt must be the last one, not a peer");
    }

    @Test
    void aWideAskIsNeverNarrowedByTheBubbleSweep() {
        // A commander who said 2000 ly keeps his 4000 ly sweep; the bubble is a floor on how far the search
        // widens, never a ceiling on what he asked for.
        List<Integer> radii = SpanshCommoditySearch.attemptsForTest(HOLD, 2000).stream()
                .map(SpanshCommoditySearch.Attempt::maxDistanceLy)
                .toList();

        assertEquals(List.of(2000, 2000, 4000, 4000), radii);
    }

    @Test
    void aWidenedRadiusIsAskedForAsDoubleTheStatedOne() {
        JsonObject distance = filtersOf(criteriaOf(profile(), "Gold", 200, false)).getAsJsonObject("distance");

        assertEquals(200, distance.get("max").getAsInt(),
                "each attempt carries its own radius; the doubling happens between attempts");
    }

    @Test
    void onlyRecentlySeenCarriersAreOffered() {
        // A carrier's recorded position is its last sighting, and it jumps. Measured live, 147 carriers are
        // listed as selling "Hardware Diagnostic Sensor" and only 13 were seen within the day - so without
        // this window nine in ten answers send the commander to a system the carrier has already left.
        JsonObject carrierFilters = filtersOf(criteriaOf(profile(), "Gold", 60, true,
                TradeStationSearchCriteria.StationType.CARRIER_TRADE_TYPES, 1));

        assertTrue(carrierFilters.has("updated_at"), () -> carrierFilters.toString());
        assertEquals(2, carrierFilters.getAsJsonObject("updated_at").getAsJsonArray("value").size());
    }

    @Test
    void aStaticMarketIsNotAskedWhenItWasLastSeen() {
        // A starport is where Spansh last recorded it however old the record is; filtering static markets by
        // sighting age would discard real answers for no gain.
        assertFalse(filtersOf(criteriaOf(profile(), "Gold", 60, true)).has("updated_at"));
    }

    @Test
    void theFallbackAsksForFleetCarriersAlone() {
        // Measured live: Alexandrite is stocked by 241 carriers and 0 starports, Thargoid Sensors by 339
        // and 0. Without this second pass those goods are reported as existing nowhere in the galaxy.
        List<String> types = stationTypesOf(profile(), TradeStationSearchCriteria.StationType.CARRIER_TRADE_TYPES);

        assertEquals(List.of("Drake-Class Carrier"), types,
                "the fallback re-asks the SAME question of carriers only; mixing the static types back in "
                        + "would let a distant starport outrank the carrier that actually has the good");
    }

    @Test
    void aCarrierMarketIsFlaggedSoTheCommanderCanBeWarned() {
        // The flag is what the spoken line and the reminder key off. Inferring it from the station type at
        // the call site instead would put Spansh's exact spelling of "Drake-Class Carrier" in two places.
        List<CommoditySearchResult> carrierMarkets = SpanshCommoditySearch.rank(carrierPage(), "Alexandrite", true);

        assertFalse(carrierMarkets.isEmpty(), "the captured carrier page sells Alexandrite");
        assertTrue(carrierMarkets.getFirst().isFleetCarrier(),
                () -> carrierMarkets.getFirst().getStationType() + " must be flagged as a carrier");
    }

    @Test
    void aStaticMarketIsNotFlaggedAsACarrier() {
        List<CommoditySearchResult> markets = SpanshCommoditySearch.rank(page(), "Gold", true);

        assertFalse(markets.isEmpty());
        assertFalse(markets.getFirst().isFleetCarrier(),
                () -> markets.getFirst().getStationType() + " is not a carrier and must not be warned about");
    }

    @Test
    void theTradeProfilesStationRulesDoNotNarrowThisSearch() {
        // They are settings for a route the commander flies repeatedly, reached through the trade route
        // screen, and nothing on it says it also governs "where can I buy this". A commander with planetary
        // ports switched off was told mission cargo did not exist anywhere in the galaxy. (The carrier
        // setting cannot widen this search either - see fleetCarriersAreNeverSearched.)
        TradeRouteSearchCriteria restrictive = profile();
        restrictive.setAllowPlanetary(false);
        restrictive.setAllowFleetCarriers(false);

        TradeRouteSearchCriteria permissive = profile();
        permissive.setAllowPlanetary(true);
        permissive.setAllowFleetCarriers(true);

        assertEquals(stationTypesOf(permissive), stationTypesOf(restrictive));
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
    void whatTheShipCanPhysicallyDoStillNarrowsTheSearch() {
        // The line between the two halves of the profile: a large ship cannot land on a small pad and a
        // hold has a size, whatever the commander asked for - but where he is WILLING to trade is not a
        // limit on where a good can be found.
        TradeRouteSearchCriteria profile = profile();
        profile.setRequiresLargePad(true);
        profile.setMaxLsFromArrival(1500);

        JsonObject filters = filtersOf(criteriaOf(profile, "Micro-weave Cooling Hoses", 60, false));

        assertEquals(1, filters.getAsJsonObject("large_pads").getAsJsonArray("value").get(0).getAsInt());
        assertEquals(1500, filters.getAsJsonObject("distance_to_arrival").getAsJsonArray("value").get(1).getAsInt());
        assertEquals(HOLD, filters.getAsJsonArray("marketplace").get(0).getAsJsonObject()
                .getAsJsonObject("supply").getAsJsonArray("value").get(0).getAsInt());
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
    void anOrbitalStationBeatsASettlementInsideTheSameRadius() {
        // A settlement costs an approach, a descent, a glide and a pad hunt on top of the flying. Ranking
        // on distance alone treats that as the same stop as a docking request and sends the commander
        // down a gravity well to save a jump.
        List<CommoditySearchResult> ranked = SpanshCommoditySearch.rank(mixedTypePage(), "Gold", true);

        assertEquals(List.of("Orbis Starport", "Planetary Port", "Settlement"),
                ranked.stream().map(CommoditySearchResult::getStationType).toList(),
                "orbit first, then a surface port, then a settlement - however the distances fall");
    }

    @Test
    void theStationTypeOutranksThePriceToo() {
        // The settlement is the cheapest on this page. It is still the last stop offered: a few credits a
        // tonne does not buy back the landing.
        assertEquals("Orbis Starport",
                SpanshCommoditySearch.rank(mixedTypePage(), "Gold", false).getFirst().getStationType());
    }

    @Test
    void distanceStillDecidesBetweenStationsOfTheSameKind() {
        assertEquals("Daedalus", SpanshCommoditySearch.rank(page(), "Gold", true).getFirst().getStationName(),
                "both are orbital, so the nearer one wins as it always did");
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
        return criteriaOf(profile, commodity, maxLy, nearest, TradeStationSearchCriteria.StationType.EVERY_STATIC_TRADE_TYPE);
    }

    private static TradeStationSearchCriteria criteriaOf(
            TradeRouteSearchCriteria profile, String commodity, int maxLy, boolean nearest, List<String> stationTypes) {
        return criteriaOf(profile, commodity, maxLy, nearest, stationTypes, profile.getMaxCargo());
    }

    private static TradeStationSearchCriteria criteriaOf(
            TradeRouteSearchCriteria profile, String commodity, int maxLy, boolean nearest,
            List<String> stationTypes, int minSupply) {
        return SpanshCommoditySearch.searchCriteria(commodity, "Sol", profile, nearest,
                new SpanshCommoditySearch.Attempt(stationTypes, minSupply, maxLy,
                        stationTypes.equals(TradeStationSearchCriteria.StationType.CARRIER_TRADE_TYPES)));
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
        return stationTypesOf(profile, TradeStationSearchCriteria.StationType.EVERY_STATIC_TRADE_TYPE);
    }

    private static List<String> stationTypesOf(TradeRouteSearchCriteria profile, List<String> stationTypes) {
        JsonArray types = filtersOf(criteriaOf(profile, "Gold", 60, false, stationTypes))
                .getAsJsonObject("type").getAsJsonArray("value");
        return types.asList().stream().map(com.google.gson.JsonElement::getAsString).toList();
    }

    /**
     * Two stations off a real {@code /api/stations/search/recall} page, trimmed to the fields read here.
     * Daedalus is the nearer, Galileo the cheaper, so distance order and price order disagree.
     */
    /**
     * A fallback page: carriers only, as the second search comes back. Station names are the callsign-style
     * ones Spansh serves, and the type is its exact spelling - the flag is matched against it.
     */
    private static List<TradeStationSearchResultDto.StationResult> carrierPage() {
        String json = """
                {"results":[
                  {"name":"K7Q-BQL","system_name":"Deciat","type":"Drake-Class Carrier","distance":12.4,
                   "distance_to_arrival":0.0,"has_market":true,
                   "market":[{"commodity":"Alexandrite","buy_price":455000,"sell_price":447000,"supply":900,"demand":0}]}
                ]}""";
        return GsonFactory.getGson().fromJson(json, TradeStationSearchResultDto.class).getResults();
    }

    /**
     * One page holding all three docking kinds, with the distances and prices deliberately against the
     * preference: the settlement is nearest AND cheapest, the orbital farthest and dearest.
     */
    private static List<TradeStationSearchResultDto.StationResult> mixedTypePage() {
        String json = """
                {"results":[
                  {"name":"Boldyr Dredging Installation","system_name":"Mat Zemlya","type":"Settlement","distance":2.1,
                   "distance_to_arrival":900.0,"has_market":true,
                   "market":[{"commodity":"Gold","buy_price":30000,"sell_price":29000,"supply":400,"demand":0}]},
                  {"name":"Hutton Orbital","system_name":"Alpha Centauri","type":"Planetary Port","distance":30.0,
                   "distance_to_arrival":200.0,"has_market":true,
                   "market":[{"commodity":"Gold","buy_price":33000,"sell_price":32000,"supply":400,"demand":0}]},
                  {"name":"Daedalus","system_name":"Sol","type":"Orbis Starport","distance":88.0,
                   "distance_to_arrival":157.0,"has_market":true,
                   "market":[{"commodity":"Gold","buy_price":45091,"sell_price":44137,"supply":400,"demand":0}]}
                ]}""";
        return GsonFactory.getGson().fromJson(json, TradeStationSearchResultDto.class).getResults();
    }

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
