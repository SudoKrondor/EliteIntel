package elite.intel.gameapi.search.spansh.station.refuel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchResultDto;
import elite.intel.util.ShipPadSizes;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "Where can I refuel?" is a station-service search, and the whole of the answer's correctness is in the
 * shape of the request and in the pad rule applied to what comes back.
 *
 * <p>Spansh ignores a filter key it does not recognise and matches nothing at all against a service or
 * station type it does not know, so either mistake narrows the search silently instead of failing it - and a
 * commander short of fuel is told there is nowhere to refuel. Hence the assertions on the serialized body.
 *
 * <p>The pad half is asserted against a captured page, because the API cannot answer "medium OR large" and
 * the merge of two searches is exactly where a station neither of them qualifies could slip through.
 */
class RefuelStationSearchTest {

    @Test
    void theRefuelServiceIsWhatIsAskedFor() {
        JsonObject filters = filtersOf(RefuelStationSearch.searchCriteria(
                1, 2, 3, 40, RefuelStationSearch.PadFilter.ANY));

        assertEquals(RefuelStationSearch.REFUEL_SERVICE,
                filters.getAsJsonArray("services").get(0).getAsJsonObject()
                        .getAsJsonArray("name").get(0).getAsString());
    }

    /**
     * The radius is a min/max pair of STRINGS on this endpoint, unlike every other range filter, and is
     * silently ignored when sent as a {@code <=>} range.
     */
    @Test
    void theRadiusGoesOutAsAStringPair() {
        JsonObject distance = filtersOf(RefuelStationSearch.searchCriteria(
                1, 2, 3, 40, RefuelStationSearch.PadFilter.ANY)).getAsJsonObject("distance");

        assertEquals("0", distance.get("min").getAsString());
        assertEquals("40", distance.get("max").getAsString());
    }

    /**
     * A fleet carrier is not offered: its recorded position is only where Spansh last saw it, and its owner
     * can switch refuelling off. The type filter is what keeps them out.
     */
    @Test
    void onlyStationsThatStayWhereTheyAreAreSearched() {
        JsonObject filters = filtersOf(RefuelStationSearch.searchCriteria(
                1, 2, 3, 40, RefuelStationSearch.PadFilter.ANY));

        List<String> types = filters.getAsJsonObject("type").getAsJsonArray("value").asList().stream()
                .map(element -> element.getAsString()).toList();
        assertFalse(types.isEmpty());
        assertTrue(types.contains("Coriolis Starport"));
        assertFalse(types.contains("Drake-Class Carrier"), () -> "carriers move: " + types);
    }

    @Test
    void aLargeShipSearchesOnceAndOnlyForLargePads() {
        assertEquals(List.of(RefuelStationSearch.PadFilter.LARGE),
                RefuelStationSearch.padFilters(ShipPadSizes.LARGE));

        JsonObject filters = filtersOf(RefuelStationSearch.searchCriteria(
                1, 2, 3, 40, RefuelStationSearch.PadFilter.LARGE));
        assertEquals(1, filters.getAsJsonObject("large_pads").getAsJsonArray("value").get(0).getAsInt());
        assertFalse(filters.has("medium_pads"));
    }

    /**
     * Spansh ANDs its three pad counts, so "medium OR large" cannot be one request. Measured live, stations
     * with a large pad and no medium pad exist in the tens of thousands - asking only about medium pads
     * would hide every one of them from a medium ship.
     */
    @Test
    void aMediumShipSearchesTwiceOverTwoDifferentPadCounts() {
        assertEquals(List.of(RefuelStationSearch.PadFilter.MEDIUM, RefuelStationSearch.PadFilter.LARGE),
                RefuelStationSearch.padFilters(ShipPadSizes.MEDIUM));

        JsonObject medium = filtersOf(RefuelStationSearch.searchCriteria(
                1, 2, 3, 40, RefuelStationSearch.PadFilter.MEDIUM));
        assertTrue(medium.has("medium_pads"));
        assertFalse(medium.has("large_pads"));

        JsonObject large = filtersOf(RefuelStationSearch.searchCriteria(
                1, 2, 3, 40, RefuelStationSearch.PadFilter.LARGE));
        assertTrue(large.has("large_pads"));
        assertFalse(large.has("medium_pads"));
    }

    @Test
    void aSmallShipConstrainsNoPadAtAll() {
        assertEquals(List.of(RefuelStationSearch.PadFilter.ANY),
                RefuelStationSearch.padFilters(ShipPadSizes.SMALL));

        JsonObject filters = filtersOf(RefuelStationSearch.searchCriteria(
                1, 2, 3, 40, RefuelStationSearch.PadFilter.ANY));
        assertFalse(filters.has("small_pads"));
        assertFalse(filters.has("medium_pads"));
        assertFalse(filters.has("large_pads"));
    }

    @Test
    void theRadiusWidensTwiceBeforeGivingUp() {
        assertEquals(List.of(40, 80, 1000), RefuelStationSearch.radiiToTry(40));
    }

    /**
     * A commander who already asked for a wide sweep keeps it: the bubble rung must never narrow the search.
     */
    @Test
    void anAlreadyWideAskIsNeverNarrowed() {
        assertEquals(List.of(2000, 4000), RefuelStationSearch.radiiToTry(2000));
    }

    @Test
    void aStationTheShipCannotLandOnIsNotAnAnswer() {
        List<RefuelStation> found = RefuelStationSearch.rank(page(), ShipPadSizes.LARGE);

        assertTrue(found.stream().noneMatch(station -> station.stationName().equals("Aithal Garrison")),
                () -> "small-pad settlement offered to a large ship: " + found);
        assertTrue(found.stream().anyMatch(station -> station.stationName().equals("Ehrlich City")));
    }

    /**
     * Easiest to dock at first, ahead of distance: a settlement in the next system is a worse errand than a
     * starport a few light years further on, however the raw light years compare.
     */
    @Test
    void anOrbitalStationBeatsANearerSettlement() {
        List<RefuelStation> found = RefuelStationSearch.rank(page(), ShipPadSizes.SMALL);

        assertEquals("Ehrlich City", found.getFirst().stationName());
    }

    /**
     * Same system, so the light years cannot separate them: the shorter supercruise wins.
     */
    @Test
    void withinOneSystemTheShorterSupercruiseWins() {
        List<RefuelStation> found = RefuelStationSearch.rank(sameSystemPage(), ShipPadSizes.SMALL);

        assertEquals("Walz Depot", found.getFirst().stationName());
    }

    private static List<TradeStationSearchResultDto.StationResult> stations(String json) {
        return GsonFactory.getGson().fromJson(json, TradeStationSearchResultDto.class).getResults();
    }

    private static JsonObject filtersOf(TradeStationSearchCriteria criteria) {
        return JsonParser.parseString(criteria.toJson()).getAsJsonObject().getAsJsonObject("filters");
    }

    /**
     * A captured page: an orbital starport further out, a small-pad settlement nearer.
     */
    private static List<TradeStationSearchResultDto.StationResult> page() {
        return stations("""
                {"results":[
                  {"name":"Aithal Garrison","system_name":"Sol","type":"Settlement","distance":4.4,
                   "distance_to_arrival":4734.0,"small_pads":2,"medium_pads":0,"large_pads":0},
                  {"name":"Ehrlich City","system_name":"Alpha Centauri","type":"Coriolis Starport","distance":9.7,
                   "distance_to_arrival":153.0,"small_pads":8,"medium_pads":8,"large_pads":8}
                ]}
                """);
    }

    private static List<TradeStationSearchResultDto.StationResult> sameSystemPage() {
        return stations("""
                {"results":[
                  {"name":"Haberlandt Survey","system_name":"Sol","type":"Coriolis Starport","distance":0.0,
                   "distance_to_arrival":2623.0,"small_pads":4,"medium_pads":4,"large_pads":4},
                  {"name":"Walz Depot","system_name":"Sol","type":"Coriolis Starport","distance":0.0,
                   "distance_to_arrival":153.0,"small_pads":4,"medium_pads":2,"large_pads":2}
                ]}
                """);
    }
}
