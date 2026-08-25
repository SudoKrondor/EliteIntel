package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonParser;
import elite.intel.gameapi.journal.events.DockedEvent;
import elite.intel.gameapi.journal.events.UndockedEvent;
import elite.intel.session.DockedMarket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
