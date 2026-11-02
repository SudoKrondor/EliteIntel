package elite.intel.gameapi.search.spansh.station.shipyard;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchResultDto;
import elite.intel.util.ShipPadSizes;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "Find a shipyard where I can buy a Cobra MkIII" is a station search with a ship filter. Spansh matches nothing
 * against a filter shape or a ship name it does not know - silently - so the request body is asserted against the
 * shape measured live (2026-11-01: a bogus name returned 0 stations, "Anaconda Mk II" 676 within 200 ly of Sol).
 */
class ShipyardSearchTest {

    private static final ShipyardShip COBRA_III = ShipyardCatalogue.bySymbol("CobraMkIII").orElseThrow();

    @Test
    void theShipGoesOutUnderItsSpanshName() {
        JsonObject filters = filtersOf(ShipyardSearch.searchCriteria(COBRA_III, 1, 2, 3, 40, false));

        assertEquals("Cobra MkIII", filters.getAsJsonObject("ships").getAsJsonArray("value").get(0).getAsString());
    }

    /**
     * Fresh by the SHIPYARD's own upload time, as a {@code <=>} range in Spansh's relative-time vocabulary.
     */
    @Test
    void onlyARecentShipyardIsAsked() {
        JsonObject seen = filtersOf(ShipyardSearch.searchCriteria(COBRA_III, 1, 2, 3, 40, false))
                .getAsJsonObject("shipyard_updated_at");

        assertEquals("<=>", seen.get("comparison").getAsString());
        assertEquals(ShipyardSearch.FRESHNESS_WINDOW, seen.getAsJsonArray("value").get(0).getAsString());
        assertEquals("now", seen.getAsJsonArray("value").get(1).getAsString());
    }

    @Test
    void theRadiusGoesOutAsAStringPairAndNearestFirst() {
        JsonObject body = JsonParser.parseString(
                ShipyardSearch.searchCriteria(COBRA_III, 1, 2, 3, 40, false).toJson()).getAsJsonObject();

        JsonObject distance = body.getAsJsonObject("filters").getAsJsonObject("distance");
        assertEquals("0", distance.get("min").getAsString());
        assertEquals("40", distance.get("max").getAsString());
        assertEquals("asc", body.getAsJsonArray("sort").get(0).getAsJsonObject()
                .getAsJsonObject("distance").get("direction").getAsString());
    }

    @Test
    void aLargePadIsAskedOnlyForALargeShip() {
        assertFalse(filtersOf(ShipyardSearch.searchCriteria(COBRA_III, 1, 2, 3, 40, false)).has("large_pads"));
        assertTrue(filtersOf(ShipyardSearch.searchCriteria(COBRA_III, 1, 2, 3, 40, true)).has("large_pads"));
    }

    /**
     * What Spansh sent is re-read rather than trusted: a station whose shipyard does not list the hull is no hit,
     * and the price is the wanted hull's, not the first one on the list.
     */
    @Test
    void onlyAStationListingTheShipIsAHit() {
        List<ShipyardHit> hits = ShipyardSearch.selling(page(), COBRA_III);

        assertEquals(List.of("Daedalus", "Galileo"), hits.stream().map(ShipyardHit::stationName).toList());
        assertEquals(314_748L, hits.getFirst().price());
    }

    /**
     * Spansh lists a hull newly on sale without a price; it is still sold there.
     */
    @Test
    void anUnpricedListingIsStillAHit() {
        ShipyardShip anacondaII = ShipyardCatalogue.bySymbol("AnacondaMkII").orElseThrow();

        List<ShipyardHit> hits = ShipyardSearch.selling(page(), anacondaII);

        assertEquals(1, hits.size());
        assertNull(hits.getFirst().price());
    }

    @Test
    void cheapestFirstPutsAnUnpricedListingLast() {
        List<ShipyardHit> ranked = ShipyardSearch.rank(List.of(
                new ShipyardHit("A", "Unpriced", "Orbis Starport", 3, null),
                new ShipyardHit("B", "Dear", "Orbis Starport", 4, 1_000L),
                new ShipyardHit("C", "Cheap", "Orbis Starport", 9, 850L)), false);

        assertEquals(List.of("Cheap", "Dear", "Unpriced"), ranked.stream().map(ShipyardHit::stationName).toList());
    }

    @Test
    void nearestFirstStillPrefersAnOrbitalToASurfacePort() {
        List<ShipyardHit> ranked = ShipyardSearch.rank(List.of(
                new ShipyardHit("A", "Ground", "Planetary Port", 2, 800L),
                new ShipyardHit("B", "Orbit", "Coriolis Starport", 7, 900L),
                new ShipyardHit("C", "Far Orbit", "Coriolis Starport", 12, 700L)), true);

        assertEquals(List.of("Orbit", "Far Orbit", "Ground"), ranked.stream().map(ShipyardHit::stationName).toList());
    }

    /**
     * Every hull in the catalogue has a pad size of its own, so the commander's ship is never guessed large
     * because this list grew and {@link ShipPadSizes} did not.
     */
    @Test
    void theCatalogueIsInStepWithPadSizes() {
        Set<String> symbols = new HashSet<>();
        for (ShipyardShip ship : ShipyardCatalogue.all()) {
            String symbol = ship.symbol().toLowerCase(Locale.ROOT);
            assertTrue(symbols.add(symbol), "duplicate symbol " + symbol);
            assertTrue(ShipPadSizes.PAD_SIZES.containsKey(symbol), "no pad size for " + symbol);
        }
    }

    private static JsonObject filtersOf(TradeStationSearchCriteria criteria) {
        return JsonParser.parseString(criteria.toJson()).getAsJsonObject().getAsJsonObject("filters");
    }

    /**
     * A captured page, cut down: a station without the Cobra MkIII, one with it, and one listing it beside the
     * unpriced Anaconda Mk II.
     */
    private static List<TradeStationSearchResultDto.StationResult> page() {
        return GsonFactory.getGson().fromJson("""
                {"results":[
                  {"name":"Ehrlich City","system_name":"Sol","type":"Orbis Starport","distance":3.0,
                   "ships":[{"name":"Cobra MkV","price":1790514,"symbol":"CobraMkV"}]},
                  {"name":"Daedalus","system_name":"Sol","type":"Orbis Starport","distance":4.0,
                   "ships":[{"name":"Adder","price":79029,"symbol":"Adder"},
                            {"name":"Cobra MkIII","price":314748,"symbol":"CobraMkIII"}]},
                  {"name":"Galileo","system_name":"Sol","type":"Ocellus Starport","distance":5.0,
                   "ships":[{"name":"Anaconda Mk II","symbol":"AnacondaMkII"},
                            {"name":"Cobra MkIII","price":330000,"symbol":"CobraMkIII"}]},
                  {"name":"No Shipyard","system_name":"Sol","type":"Outpost","distance":6.0}
                ]}
                """, TradeStationSearchResultDto.class).getResults();
    }
}
