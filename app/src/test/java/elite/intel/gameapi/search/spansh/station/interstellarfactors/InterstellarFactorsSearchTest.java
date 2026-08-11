package elite.intel.gameapi.search.spansh.station.interstellarfactors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The pad requirement has to reach Spansh, and it has to reach it in the shape the stations endpoint accepts.
 *
 * <p>Spansh exposes no boolean "large pad required" on {@code /api/stations/search}: the pad constraint is a
 * range over the station's large-pad count, so "requires a large pad" is the range {@code [1, MANY]}. Asserted
 * on the serialized body rather than the object because the wire shape is the whole point - a filter Gson
 * emits under a different key, or with the wrong comparison, is silently ignored by the API and the commander
 * gets sent to an outpost their Cutter cannot dock at.
 */
class InterstellarFactorsSearchTest {

    @Test
    void aLargeShipAsksForStationsWithALargePad() {
        JsonObject filters = filtersOf(InterstellarFactorsSearch.buildCriteria(1, 2, 3, 100, 6000, true));

        assertTrue(filters.has("large_pads"), () -> "pad filter missing from " + filters);
        JsonObject largePads = filters.getAsJsonObject("large_pads");
        assertEquals("<=>", largePads.get("comparison").getAsString());
        assertEquals(1, largePads.getAsJsonArray("value").get(0).getAsInt(),
                "at least one large pad");
        assertTrue(largePads.getAsJsonArray("value").get(1).getAsInt() > 1,
                "upper bound must not exclude stations with several large pads");
    }

    @Test
    void aSmallShipLeavesThePadSizeUnconstrained() {
        JsonObject filters = filtersOf(InterstellarFactorsSearch.buildCriteria(1, 2, 3, 100, 6000, false));

        assertFalse(filters.has("large_pads"),
                () -> "a medium or small ship must still see outposts: " + filters);
    }

    @Test
    void thePadFilterDoesNotDisplaceTheServiceFilter() {
        JsonObject filters = filtersOf(InterstellarFactorsSearch.buildCriteria(1, 2, 3, 100, 6000, true));

        assertEquals(InterstellarFactorsSearch.INTERSTELLAR_FACTORS_CONTACT,
                filters.getAsJsonArray("services").get(0).getAsJsonObject()
                        .getAsJsonArray("name").get(0).getAsString());
        assertTrue(filters.has("distance"));
        assertTrue(filters.has("distance_to_arrival"));
    }

    private static JsonObject filtersOf(InterstellarFactorsSearchCriteria criteria) {
        return JsonParser.parseString(criteria.toJson()).getAsJsonObject().getAsJsonObject("filters");
    }
}
