package elite.intel.gameapi.search.spansh.station;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.gameapi.search.spansh.station.traderandbroker.BrokerType;
import elite.intel.gameapi.search.spansh.station.traderandbroker.TraderAndBrokerSearchCriteria;
import elite.intel.gameapi.search.spansh.station.traderandbroker.TraderAndBrokerSearchDto;
import elite.intel.gameapi.search.spansh.station.traderandbroker.TraderType;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The request has to reach Spansh in the shape the stations endpoint accepts, and it has to bound how old
 * a listing may be.
 *
 * <p>Asserted on the serialized body rather than the object because the wire shape is the whole point:
 * Spansh ignores a filter key it does not recognise and silently matches nothing against a shape it does
 * not expect. That is exactly how these searches went stale - a radius sent as ints and no freshness bound
 * at all - and nothing failed; the commander was just sent to a port whose broker had moved on.
 */
class TradersAndBrokersSearchTest {

    @Test
    void aTraderSearchNamesTheTraderTypeAndNothingElse() {
        JsonObject filters = filtersOf(criteria(TraderType.ENCODED, null, false));

        assertEquals("Encoded", filters.getAsJsonObject("material_trader").getAsJsonArray("value").get(0).getAsString());
        assertFalse(filters.has("technology_broker"), () -> "a trader search must not ask for a broker: " + filters);
    }

    @Test
    void aBrokerSearchNamesTheBrokerTypeAndNothingElse() {
        JsonObject filters = filtersOf(criteria(null, BrokerType.GUARDIAN, false));

        assertEquals("Guardian", filters.getAsJsonObject("technology_broker").getAsJsonArray("value").get(0).getAsString());
        assertFalse(filters.has("material_trader"), () -> "a broker search must not ask for a trader: " + filters);
    }

    @Test
    void onlyRecentlyUpdatedListingsAreCandidates() {
        JsonObject filters = filtersOf(criteria(TraderType.RAW, null, false));

        assertTrue(filters.has("updated_at"), () -> "freshness bound missing from " + filters);
        JsonObject updatedAt = filters.getAsJsonObject("updated_at");
        assertEquals("<=>", updatedAt.get("comparison").getAsString());
        JsonArray window = updatedAt.getAsJsonArray("value");
        assertEquals(TradersAndBrokersSearch.FRESHNESS_WINDOW, window.get(0).getAsString());
        assertEquals("now", window.get(1).getAsString());
    }

    @Test
    void theRadiusIsSentAsTheStringPairTheEndpointWants() {
        JsonObject filters = filtersOf(criteria(TraderType.RAW, null, false));

        JsonObject distance = filters.getAsJsonObject("distance");
        assertTrue(distance.get("min").isJsonPrimitive() && distance.get("min").getAsJsonPrimitive().isString(),
                () -> "radius bounds must be strings, not numbers: " + distance);
        assertEquals("0.00", distance.get("min").getAsString());
        assertEquals("100.00", distance.get("max").getAsString());
    }

    @Test
    void theSupercruiseLegIsBounded() {
        JsonObject filters = filtersOf(criteria(TraderType.RAW, null, false));

        JsonObject arrival = filters.getAsJsonObject("distance_to_arrival");
        assertEquals("<=>", arrival.get("comparison").getAsString());
        assertEquals(0, arrival.getAsJsonArray("value").get(0).getAsInt());
        assertEquals(TradersAndBrokersSearch.MAX_ARRIVAL_LS, arrival.getAsJsonArray("value").get(1).getAsInt());
    }

    @Test
    void aLargeShipAsksForStationsWithALargePad() {
        JsonObject filters = filtersOf(criteria(null, BrokerType.HUMAN, true));

        assertTrue(filters.has("large_pads"), () -> "pad filter missing from " + filters);
        JsonObject largePads = filters.getAsJsonObject("large_pads");
        assertEquals("<=>", largePads.get("comparison").getAsString());
        assertEquals(1, largePads.getAsJsonArray("value").get(0).getAsInt(), "at least one large pad");
        assertTrue(largePads.getAsJsonArray("value").get(1).getAsInt() > 1,
                "upper bound must not exclude stations with several large pads");
    }

    @Test
    void aSmallShipLeavesThePadSizeUnconstrained() {
        JsonObject filters = filtersOf(criteria(null, BrokerType.HUMAN, false));

        assertFalse(filters.has("large_pads"), () -> "a medium or small ship must still see outposts: " + filters);
    }

    @Test
    void thePageIsSortedNearestFirstOnTheServer() {
        JsonObject body = bodyOf(criteria(TraderType.MANUFACTURED, null, false));

        JsonArray sort = body.getAsJsonArray("sort");
        assertEquals(1, sort.size(), () -> "one sort clause expected: " + body);
        assertEquals("asc", sort.get(0).getAsJsonObject().getAsJsonObject("distance").get("direction").getAsString());
        assertEquals(0, body.get("page").getAsInt());
        assertTrue(List.of(5, 10, 25, 50, 100, 500).contains(body.get("size").getAsInt()),
                "page size must be one Spansh accepts");
        JsonObject reference = body.getAsJsonObject("reference_coords");
        assertEquals(1.0, reference.get("x").getAsDouble());
        assertEquals(2.0, reference.get("y").getAsDouble());
        assertEquals(3.0, reference.get("z").getAsDouble());
    }

    @Test
    void theNearestHitWinsWhateverOrderThePageCameIn() {
        // Measured live: a page asked for with no sort came back with the 35 ly hit LAST.
        List<TraderAndBrokerSearchDto.Result> page = List.of(
                hit("Cet", "McKay Ring", 42.4),
                hit("LP 906-9", "Hertz Orbital", 50.9),
                hit("Bolg", "Moxon's Mojo", 35.7));

        TraderAndBrokerSearchDto.Result chosen = TradersAndBrokersSearch.nearest(page, "Sol");

        assertNotNull(chosen);
        assertEquals("Bolg", chosen.getSystemName());
    }

    @Test
    void aHitInTheCurrentSystemIsNeverTheAnswer() {
        List<TraderAndBrokerSearchDto.Result> page = List.of(
                hit("Sol", "Daedalus", 0.0),
                hit("Cet", "McKay Ring", 42.4));

        TraderAndBrokerSearchDto.Result chosen = TradersAndBrokersSearch.nearest(page, "Sol");

        assertNotNull(chosen);
        assertEquals("Cet", chosen.getSystemName());
    }

    @Test
    void noHitsMeansNoAnswer() {
        assertNull(TradersAndBrokersSearch.nearest(null, "Sol"));
        assertNull(TradersAndBrokersSearch.nearest(List.of(), "Sol"));
        assertNull(TradersAndBrokersSearch.nearest(List.of(hit("Sol", "Daedalus", 0.0)), "Sol"),
                "the only hit sits in the current system");
    }

    private static TraderAndBrokerSearchCriteria criteria(TraderType trader, BrokerType broker, boolean largePad) {
        return TradersAndBrokersSearch.buildCriteria(1, 2, 3, trader, broker, 100, largePad);
    }

    private static JsonObject bodyOf(TraderAndBrokerSearchCriteria criteria) {
        return JsonParser.parseString(criteria.toJson()).getAsJsonObject();
    }

    private static JsonObject filtersOf(TraderAndBrokerSearchCriteria criteria) {
        return bodyOf(criteria).getAsJsonObject("filters");
    }

    /**
     * A hit as Spansh would send it, built through Gson because the result type has no setters.
     */
    private static TraderAndBrokerSearchDto.Result hit(String system, String station, double distance) {
        JsonObject json = new JsonObject();
        json.addProperty("system_name", system);
        json.addProperty("name", station);
        json.addProperty("distance", distance);
        json.addProperty("updated_at", "2026-09-20T18:04:35Z");
        return GsonFactory.getGson().fromJson(json, TraderAndBrokerSearchDto.Result.class);
    }
}
