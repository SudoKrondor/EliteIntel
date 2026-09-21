package elite.intel.gameapi.search.spansh.station.outfitting;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchResultDto;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "Where can I buy a size 6 class B fuel scoop?" is a station search with a module filter, and the whole of
 * the answer's correctness is in the shape of the request and in how the module list that comes back is read.
 *
 * <p>Spansh ignores a filter key it does not recognise and matches nothing against a shape it does not
 * expect, so either mistake narrows the search silently instead of failing it - and the commander is told
 * nobody sells the module. Hence the assertions on the serialized body, against the shape the site's own
 * search form posts.
 */
class OutfittingSearchTest {

    private static final WantedModule FUEL_SCOOP_6B = new WantedModule(List.of("Fuel Scoop"), 6, "B", null);
    private static final WantedModule ANY_FUEL_SCOOP_6 = new WantedModule(List.of("Fuel Scoop"), 6, null, null);

    /**
     * The pilot's "size" is Spansh's "class" and the pilot's "class" is Spansh's "rating"; each goes out inside
     * a {@code value} wrapper, as one nested module and not three loose filters.
     */
    @Test
    void theModuleGoesOutInSpanshsOwnVocabulary() {
        JsonObject module = filtersOf(OutfittingSearch.searchCriteria(FUEL_SCOOP_6B, 1, 2, 3, 40, false))
                .getAsJsonArray("modules").get(0).getAsJsonObject();

        assertEquals("Fuel Scoop", module.getAsJsonObject("name").getAsJsonArray("value").get(0).getAsString());
        assertEquals(6, module.getAsJsonObject("class").getAsJsonArray("value").get(0).getAsInt());
        assertEquals("B", module.getAsJsonObject("rating").getAsJsonArray("value").get(0).getAsString());
    }

    /**
     * Every spelling the catalogue holds is sent: a search for "Mk II Business Class Passenger Cabin" alone
     * misses every station Spansh filed under "MkII".
     */
    @Test
    void everySpellingIsAsked() {
        WantedModule cabin = new WantedModule(
                List.of("Mk II Business Class Passenger Cabin", "MkII Business Class Passenger Cabin"), null, null, null);

        JsonObject module = filtersOf(OutfittingSearch.searchCriteria(cabin, 1, 2, 3, 40, false))
                .getAsJsonArray("modules").get(0).getAsJsonObject();

        assertEquals(2, module.getAsJsonObject("name").getAsJsonArray("value").size());
    }

    /**
     * A size or class the commander did not name is left off the wire entirely, not sent as an empty list -
     * Spansh matches nothing against an empty list.
     */
    @Test
    void anUnstatedDesignationIsNotSent() {
        JsonObject module = filtersOf(OutfittingSearch.searchCriteria(ANY_FUEL_SCOOP_6, 1, 2, 3, 40, false))
                .getAsJsonArray("modules").get(0).getAsJsonObject();

        assertTrue(module.has("class"));
        assertFalse(module.has("rating"));
    }

    /**
     * The radius is a min/max pair of STRINGS on this endpoint, unlike every other range filter, and is
     * silently ignored when sent as a {@code <=>} range.
     */
    @Test
    void theRadiusGoesOutAsAStringPair() {
        JsonObject distance = filtersOf(OutfittingSearch.searchCriteria(FUEL_SCOOP_6B, 1, 2, 3, 40, false))
                .getAsJsonObject("distance");

        assertEquals("0", distance.get("min").getAsString());
        assertEquals("40", distance.get("max").getAsString());
    }

    @Test
    void onlyRecentlySeenOutfittingIsTrusted() {
        JsonObject updatedAt = filtersOf(OutfittingSearch.searchCriteria(FUEL_SCOOP_6B, 1, 2, 3, 40, false))
                .getAsJsonObject("updated_at");

        assertEquals("<=>", updatedAt.get("comparison").getAsString());
        assertEquals(OutfittingSearch.FRESHNESS_WINDOW, updatedAt.getAsJsonArray("value").get(0).getAsString());
        assertEquals("now", updatedAt.getAsJsonArray("value").get(1).getAsString());
    }

    @Test
    void aLargeShipIsOnlyShownLargePads() {
        assertTrue(filtersOf(OutfittingSearch.searchCriteria(FUEL_SCOOP_6B, 1, 2, 3, 40, true)).has("large_pads"));
        assertFalse(filtersOf(OutfittingSearch.searchCriteria(FUEL_SCOOP_6B, 1, 2, 3, 40, false)).has("large_pads"));
    }

    @Test
    void carriersAreNotOffered() {
        List<String> types = filtersOf(OutfittingSearch.searchCriteria(FUEL_SCOOP_6B, 1, 2, 3, 40, false))
                .getAsJsonObject("type").getAsJsonArray("value").asList().stream()
                .map(element -> element.getAsString()).toList();

        assertTrue(types.contains("Coriolis Starport"));
        assertFalse(types.contains("Drake-Class Carrier"), () -> "carriers move: " + types);
    }

    /**
     * What a station stocks is read from the page, every listing that fits, cheapest first - so a commander
     * who named a size but no class hears "6D at 449,430 credits, 6E at 107,860 credits".
     */
    @Test
    void everyListingThatFitsIsReported() {
        List<OutfittingHit> hits = OutfittingSearch.stocking(page(), ANY_FUEL_SCOOP_6);

        OutfittingHit chandola = hits.stream().filter(hit -> hit.stationName().equals("Chandola Legacy")).findFirst().orElseThrow();
        assertEquals(List.of("6E", "6D"), chandola.modules().stream().map(StockedModule::designation).toList());
        assertEquals(107_860, chandola.cheapest());
    }

    @Test
    void aStatedClassNarrowsTheListing() {
        List<OutfittingHit> hits = OutfittingSearch.stocking(page(), new WantedModule(List.of("Fuel Scoop"), 6, "D", null));

        OutfittingHit chandola = hits.stream().filter(hit -> hit.stationName().equals("Chandola Legacy")).findFirst().orElseThrow();
        assertEquals(List.of("6D"), chandola.modules().stream().map(StockedModule::designation).toList());
    }

    /**
     * The mount is not a filter Spansh has, so a station that answered the name may stock only the wrong
     * mount - it is dropped here, and a station with none of the module at all never becomes a hit.
     */
    @Test
    void theMountIsCheckedAgainstWhatCameBack() {
        List<OutfittingHit> gimbal = OutfittingSearch.stocking(page(), new WantedModule(List.of("Beam Laser"), null, null, "Gimbal"));
        List<OutfittingHit> turret = OutfittingSearch.stocking(page(), new WantedModule(List.of("Beam Laser"), null, null, "Turret"));

        assertEquals(List.of("Ehrlich City"), gimbal.stream().map(OutfittingHit::stationName).toList());
        assertTrue(turret.isEmpty(), () -> "no station on the page turrets a beam laser: " + turret);
    }

    /**
     * Module prices are fixed by the game, so a cheaper station is one with a Power's discount - and that is
     * the station worth hearing about first. At the same price, nearest wins.
     */
    @Test
    void theCheapestListingWinsAndThenTheNearest() {
        List<OutfittingHit> ranked = OutfittingSearch.rank(OutfittingSearch.stocking(page(), ANY_FUEL_SCOOP_6), false);

        assertEquals(List.of("Ehrlich City", "Chandola Legacy", "Walz Depot"),
                ranked.stream().map(OutfittingHit::stationName).toList());
    }

    @Test
    void askedForTheNearestThePriceIsIgnored() {
        List<OutfittingHit> ranked = OutfittingSearch.rank(OutfittingSearch.stocking(page(), ANY_FUEL_SCOOP_6), true);

        assertEquals(List.of("Chandola Legacy", "Ehrlich City", "Walz Depot"),
                ranked.stream().map(OutfittingHit::stationName).toList());
    }

    /**
     * Easiest to dock at first, ahead of both price and distance: a surface port in the next system is a worse
     * errand than a starport a few light years further on.
     */
    @Test
    void anOrbitalStationBeatsANearerSurfacePort() {
        List<OutfittingHit> ranked = OutfittingSearch.rank(OutfittingSearch.stocking(surfacePage(), ANY_FUEL_SCOOP_6), true);

        assertEquals("Ehrlich City", ranked.getFirst().stationName());
    }

    private static JsonObject filtersOf(TradeStationSearchCriteria criteria) {
        return JsonParser.parseString(criteria.toJson()).getAsJsonObject().getAsJsonObject("filters");
    }

    private static List<TradeStationSearchResultDto.StationResult> stations(String json) {
        return GsonFactory.getGson().fromJson(json, TradeStationSearchResultDto.class).getResults();
    }

    /**
     * A captured page, cut down to the listings that matter: three orbitals at 5, 9 and 12 ly, the middle one
     * discounted, the last stocking a size 6 scoop in class E only.
     */
    private static List<TradeStationSearchResultDto.StationResult> page() {
        return stations("""
                {"results":[
                  {"name":"Chandola Legacy","system_name":"Pru Eurk SX-L d7-11","type":"Orbis Starport","distance":5.0,
                   "modules":[
                     {"category":"internal","class":5,"ed_symbol":"Int_FuelScoop_Size5_Class1","name":"Fuel Scoop","price":34030,"rating":"E"},
                     {"category":"internal","class":6,"ed_symbol":"Int_FuelScoop_Size6_Class1","name":"Fuel Scoop","price":107860,"rating":"E"},
                     {"category":"internal","class":6,"ed_symbol":"Int_FuelScoop_Size6_Class2","name":"Fuel Scoop","price":449430,"rating":"D"},
                     {"category":"hardpoint","class":1,"ed_symbol":"Hpt_BeamLaser_Fixed_Small","name":"Beam Laser","price":37430,"rating":"E","weapon_mode":"Fixed"}
                   ]},
                  {"name":"Ehrlich City","system_name":"Alpha Centauri","type":"Coriolis Starport","distance":9.7,
                   "modules":[
                     {"category":"internal","class":6,"ed_symbol":"Int_FuelScoop_Size6_Class1","name":"Fuel Scoop","price":91681,"rating":"E"},
                     {"category":"hardpoint","class":1,"ed_symbol":"Hpt_BeamLaser_Gimbal_Small","name":"Beam Laser","price":74650,"rating":"E","weapon_mode":"Gimbal"}
                   ]},
                  {"name":"Walz Depot","system_name":"Sol","type":"Coriolis Starport","distance":12.0,
                   "modules":[
                     {"category":"internal","class":6,"ed_symbol":"Int_FuelScoop_Size6_Class1","name":"Fuel Scoop","price":107860,"rating":"E"}
                   ]},
                  {"name":"Aithal Garrison","system_name":"Sol","type":"Settlement","distance":12.0,
                   "modules":[]}
                ]}
                """);
    }

    private static List<TradeStationSearchResultDto.StationResult> surfacePage() {
        return stations("""
                {"results":[
                  {"name":"Chandola Legacy","system_name":"Pru Eurk SX-L d7-11","type":"Planetary Outpost","distance":5.0,
                   "modules":[
                     {"category":"internal","class":6,"ed_symbol":"Int_FuelScoop_Size6_Class1","name":"Fuel Scoop","price":107860,"rating":"E"}
                   ]},
                  {"name":"Ehrlich City","system_name":"Alpha Centauri","type":"Coriolis Starport","distance":9.7,
                   "modules":[
                     {"category":"internal","class":6,"ed_symbol":"Int_FuelScoop_Size6_Class1","name":"Fuel Scoop","price":107860,"rating":"E"}
                   ]}
                ]}
                """);
    }
}
