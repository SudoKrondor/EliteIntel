package elite.intel.db.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.db.dao.LocationDao;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteSearchCriteria;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The station a trade route is plotted FROM has to satisfy the route search's own rules, and the request
 * that finds it has to reach Spansh in the shape the stations endpoint accepts.
 *
 * <p>Asserted on the serialized body rather than the object because the wire shape is the whole point:
 * Spansh ignores a filter key it does not recognise, and a station type name it does not know matches no
 * station at all. Either mistake narrows the search silently instead of failing it, and the commander just
 * hears "no trade route found".
 */
class TradeAnchorSearchTest {

    /**
     * Spansh's station type vocabulary, from {@code GET /api/stations/field_values/type}. Snapshotted here so
     * a typo in the filter is caught by a build rather than by an empty result set out in the black.
     */
    private static final Set<String> SPANSH_STATION_TYPES = Set.of(
            "Asteroid base", "Coriolis Starport", "Dodec Starport", "Drake-Class Carrier", "Mega ship",
            "Ocellus Starport", "Orbis Starport", "Outpost", "Planetary Construction Depot",
            "Planetary Outpost", "Planetary Port", "Settlement", "Space Construction Depot",
            "Surface Settlement"
    );

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
    void outpostsAreEligibleAnchors() {
        // 115k of them, and for a small or medium hauler often the only trade station in reach.
        assertTrue(stationTypesOf(profile()).contains("Outpost"));
    }

    @Test
    void surfacePortsAreOfferedOnlyWhenTheProfileAllowsThem() {
        assertFalse(stationTypesOf(profile()).contains("Planetary Port"),
                "a profile that forbids planetary ports must not be anchored at one");

        TradeRouteSearchCriteria allowsPlanetary = profile();
        allowsPlanetary.setAllowPlanetary(true);
        assertTrue(stationTypesOf(allowsPlanetary).contains("Planetary Port"));
    }

    @Test
    void carriersAreOfferedOnlyWhenTheProfileAllowsThem() {
        assertFalse(stationTypesOf(profile()).contains("Drake-Class Carrier"));

        TradeRouteSearchCriteria allowsCarriers = profile();
        allowsCarriers.setAllowFleetCarriers(true);
        assertTrue(stationTypesOf(allowsCarriers).contains("Drake-Class Carrier"));
    }

    @Test
    void theAnchorMustHaveACommodityMarket() {
        JsonArray services = filtersOf(profile()).getAsJsonArray("services");

        assertEquals("Market", services.get(0).getAsJsonObject().getAsJsonArray("name").get(0).getAsString(),
                "a route cannot start where there is no cargo to buy");
    }

    @Test
    void aLargeShipAsksForStationsWithALargePad() {
        TradeRouteSearchCriteria profile = profile();
        profile.setRequiresLargePad(true);

        JsonObject largePads = filtersOf(profile).getAsJsonObject("large_pads");
        assertEquals("<=>", largePads.get("comparison").getAsString());
        assertEquals(1, largePads.getAsJsonArray("value").get(0).getAsInt(), "at least one large pad");
        assertTrue(largePads.getAsJsonArray("value").get(1).getAsInt() > 1,
                "upper bound must not exclude stations with several large pads");
    }

    @Test
    void aSmallShipLeavesThePadSizeUnconstrained() {
        assertFalse(filtersOf(profile()).has("large_pads"),
                "a medium or small ship must still see outposts");
    }

    @Test
    void theNearestStationComesBackFirst() {
        // Spansh returns index order for an empty sort, which is not distance order: the anchor would be
        // whichever row came back first, hundreds of light years out while a neighbour sat on the same page.
        JsonArray sort = JsonParser.parseString(criteriaOf(profile()).toJson()).getAsJsonObject()
                .getAsJsonArray("sort");

        assertEquals(1, sort.size(), () -> "no sort means no nearest: " + sort);
        assertEquals("asc", sort.get(0).getAsJsonObject().getAsJsonObject("distance").get("direction").getAsString());
    }

    @Test
    void theAnchorIsHeldToTheProfilesOwnArrivalDistance() {
        TradeRouteSearchCriteria profile = profile();
        profile.setMaxLsFromArrival(1500);

        JsonArray range = filtersOf(profile).getAsJsonObject("distance_to_arrival").getAsJsonArray("value");
        assertEquals(0, range.get(0).getAsInt());
        assertEquals(1500, range.get(1).getAsInt(),
                "an anchor further out than the route's own limit is a station the route will not use");
    }

    @Test
    void anUnsetArrivalDistanceLeavesTheAnchorSearchPermissive() {
        // 0 means "not configured yet". The calling command says so in its own words, so failing the anchor
        // search first would replace a useful message with a useless one.
        JsonArray range = filtersOf(profile()).getAsJsonObject("distance_to_arrival").getAsJsonArray("value");

        assertTrue(range.get(1).getAsInt() > 0, () -> "arrival cap collapsed to " + range);
    }

    @Test
    void theSearchRadiusIsSentAsStringsWithoutAComparison() {
        // Spansh takes the system-distance filter in a different shape from every other range filter.
        JsonObject distance = filtersOf(profile()).getAsJsonObject("distance");

        assertFalse(distance.has("comparison"));
        assertEquals("0", distance.get("min").getAsString());
        assertEquals(String.valueOf(TradeProfileManager.MAX_DISTANCE_TO_INITIAL_STATION),
                distance.get("max").getAsString());
    }

    @Test
    void aLargeShipIsNotAnchoredAtAnOutpostFromOurOwnRecords() {
        TradeRouteSearchCriteria profile = profile();
        profile.setRequiresLargePad(true);

        assertFalse(TradeProfileManager.canAnchorRoute(profile, station("Outpost")));
        assertTrue(TradeProfileManager.canAnchorRoute(profile, station("Coriolis")));
    }

    @Test
    void aSurfacePortFromOurOwnRecordsObeysThePlanetaryFlag() {
        assertFalse(TradeProfileManager.canAnchorRoute(profile(), station("CraterPort")));

        TradeRouteSearchCriteria allowsPlanetary = profile();
        allowsPlanetary.setAllowPlanetary(true);
        assertTrue(TradeProfileManager.canAnchorRoute(allowsPlanetary, station("CraterPort")));
    }

    @Test
    void aStationTypeWeNeverRecordedIsStillUsable() {
        // Older station rows predate the type being captured. Discarding them would throw away the local
        // records the whole first pass exists to use.
        TradeRouteSearchCriteria profile = profile();
        profile.setRequiresLargePad(true);

        assertTrue(TradeProfileManager.canAnchorRoute(profile, station(null)));
    }

    private static LocationDto station(String stationType) {
        LocationDto station = new LocationDto(1L, "Somewhere");
        station.setStationName("Somewhere Hub");
        station.setStationType(stationType);
        return station;
    }

    private static TradeRouteSearchCriteria profile() {
        return new TradeRouteSearchCriteria();
    }

    private static TradeStationSearchCriteria criteriaOf(TradeRouteSearchCriteria profile) {
        return TradeProfileManager.anchorSearchCriteria(profile, new LocationDao.Coordinates("Somewhere", 1, 2, 3));
    }

    private static JsonObject filtersOf(TradeRouteSearchCriteria profile) {
        return JsonParser.parseString(criteriaOf(profile).toJson()).getAsJsonObject().getAsJsonObject("filters");
    }

    private static List<String> stationTypesOf(TradeRouteSearchCriteria profile) {
        JsonArray types = filtersOf(profile).getAsJsonObject("type").getAsJsonArray("value");
        return types.asList().stream().map(element -> element.getAsString()).toList();
    }
}
