package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonParser;
import elite.intel.gameapi.journal.events.ApproachBodyEvent;
import elite.intel.gameapi.journal.events.DockedEvent;
import elite.intel.gameapi.journal.events.LocationEvent;
import elite.intel.gameapi.journal.events.UndockedEvent;
import elite.intel.session.DockedMarket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Which port the ship is standing on, taken from the journal rather than inferred from the location tables.
 * <p>
 * The inference is what failed in the field: those rows are keyed by {@code (systemAddress, bodyId)},
 * {@code Docked} carries no bodyId, and the miss returned a fresh row with MarketID zero - so a command
 * gated on it was never once offered at a depot the app was reading the manifest of at the same moment.
 * <p>
 * The fixtures are the real lines from the commander's journal.
 */
class DockedMarketSubscriberTest {

    private static final long DIVIS_GATEWAY = 3967232514L;

    private final DockedMarketSubscriber subscriber = new DockedMarketSubscriber();

    @BeforeEach
    @AfterEach
    void clearMarker() {
        DockedMarket.getInstance().departed();
    }

    private static DockedEvent docked() {
        String json = """
                { "timestamp":"2026-08-23T19:11:53Z", "event":"Docked",
                  "StationName":"Orbital Construction Site: Divis Gateway", "StationType":"SpaceConstructionDepot",
                  "Taxi":false, "Multicrew":false, "StarSystem":"Hyades Sector NR-V b2-2",
                  "SystemAddress":5070074422609, "MarketID":3967232514,
                  "StationFaction":{ "Name":"Brewer Corporation" }, "DistFromStarLS":1127.704896 }
                """;
        return new DockedEvent(JsonParser.parseString(json).getAsJsonObject());
    }

    private static UndockedEvent undocked(long marketId) {
        String json = """
                { "timestamp":"2026-08-23T12:10:03Z", "event":"Undocked",
                  "StationName":"Orbital Construction Site: Vespucci Landing",
                  "StationType":"SpaceConstructionDepot", "MarketID":%d, "Taxi":false, "Multicrew":false }
                """.formatted(marketId);
        return new UndockedEvent(JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    void dockingRecordsTheMarketTheJournalNamed() {
        subscriber.onDocked(docked());

        assertEquals(DIVIS_GATEWAY, DockedMarket.getInstance().marketId());
    }

    @Test
    void undockingClearsIt() {
        subscriber.onDocked(docked());
        subscriber.onUndocked(undocked(DIVIS_GATEWAY));

        assertEquals(0, DockedMarket.getInstance().marketId(),
                "off the pad, and anything gated on standing there has to close");
    }

    /**
     * Nothing has said we are anywhere yet, which callers must read the same way as "not docked".
     */
    @Test
    void beforeAnyDockingTheAnswerIsNowhere() {
        assertEquals(0, DockedMarket.getInstance().marketId());
    }

    /**
     * A journal line without a MarketID says nothing about where we are, and must not erase what does.
     */
    @Test
    void aDockingWithoutAMarketIdLeavesTheMarkerAlone() {
        subscriber.onDocked(docked());

        String json = """
                { "timestamp":"2026-08-23T19:11:53Z", "event":"Docked", "StationName":"Nowhere",
                  "StationType":"Outpost", "StarSystem":"Sol", "SystemAddress":10477373803 }
                """;
        subscriber.onDocked(new DockedEvent(JsonParser.parseString(json).getAsJsonObject()));

        assertEquals(DIVIS_GATEWAY, DockedMarket.getInstance().marketId());
    }

    @Test
    void movingFromOnePortToAnotherFollowsTheShip() {
        subscriber.onDocked(docked());
        subscriber.onUndocked(undocked(DIVIS_GATEWAY));

        String json = """
                { "timestamp":"2026-08-23T12:16:00Z", "event":"Docked", "StationName":"Borlaug Gateway",
                  "StationType":"Outpost", "StarSystem":"Sol", "SystemAddress":10477373803,
                  "MarketID":4224953347 }
                """;
        subscriber.onDocked(new DockedEvent(JsonParser.parseString(json).getAsJsonObject()));

        assertEquals(4224953347L, DockedMarket.getInstance().marketId());
    }

    // --- the planet a port on the ground stands on (lines from the commander's 2026-10-09 journal) ---

    private static ApproachBodyEvent approach(String body, long systemAddress) {
        String json = """
                { "timestamp":"2026-10-09T18:34:53Z", "event":"ApproachBody", "StarSystem":"Beta-3 Tucani",
                  "SystemAddress":%d, "Body":"%s", "BodyID":16 }
                """.formatted(systemAddress, body);
        return new ApproachBodyEvent(JsonParser.parseString(json).getAsJsonObject());
    }

    private static DockedEvent dockedAtTheBeach(String stationType) {
        String json = """
                { "timestamp":"2026-10-09T18:37:12Z", "event":"Docked", "StationName":"The Beach",
                  "StationType":"%s", "Taxi":false, "Multicrew":false, "StarSystem":"Beta-3 Tucani",
                  "SystemAddress":2827992680811, "MarketID":128674951, "StationFaction":{ "Name":"The Sarge" } }
                """.formatted(stationType);
        return new DockedEvent(JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    void aCraterOutpostStandsOnThePlanetTheShipLastApproached() {
        subscriber.onApproachBody(approach("Beta-3 Tucani 2 b a", 2827992680811L));
        subscriber.onDocked(dockedAtTheBeach("CraterOutpost"));

        assertEquals("Beta-3 Tucani 2 b a", DockedMarket.getInstance().surfaceBody());
    }

    @Test
    void aStationInSpaceStandsOnNothingWhateverWasApproachedBefore() {
        subscriber.onApproachBody(approach("Beta-3 Tucani 2 b a", 2827992680811L));
        subscriber.onDocked(dockedAtTheBeach("Coriolis"));

        assertNull(DockedMarket.getInstance().surfaceBody());
    }

    @Test
    void aPlanetApproachedInAnotherSystemIsNotThePortsPlanet() {
        subscriber.onApproachBody(approach("Achenar 4a", 164098653L));
        subscriber.onDocked(dockedAtTheBeach("CraterOutpost"));

        assertNull(DockedMarket.getInstance().surfaceBody());
    }

    @Test
    void undockingForgetsThePlanet() {
        subscriber.onApproachBody(approach("Beta-3 Tucani 2 b a", 2827992680811L));
        subscriber.onDocked(dockedAtTheBeach("CraterOutpost"));
        subscriber.onUndocked(undocked(128674951L));

        assertNull(DockedMarket.getInstance().surfaceBody());
    }

    @Test
    void aRestartOnASurfacePadTakesThePlanetFromLocation() {
        String json = """
                { "timestamp":"2026-10-09T19:10:00Z", "event":"Location", "Docked":true, "StationName":"The Beach",
                  "StationType":"CraterOutpost", "MarketID":128674951, "Taxi":false, "Multicrew":false,
                  "StarSystem":"Beta-3 Tucani", "SystemAddress":2827992680811,
                  "Body":"Beta-3 Tucani 2 b a", "BodyID":16, "BodyType":"Planet" }
                """;
        subscriber.onLocation(new LocationEvent(JsonParser.parseString(json).getAsJsonObject()));

        assertEquals("Beta-3 Tucani 2 b a", DockedMarket.getInstance().surfaceBody());
    }
}
