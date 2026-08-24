package elite.intel.gameapi.carrier;

import com.google.gson.JsonObject;
import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.db.managers.SquadronCarrierManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.CarrierStatsEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Which carrier a {@code CarrierStats} reading is filed under.
 * <p>
 * A commander can own one fleet carrier and one squadron carrier, and the game reports both through the
 * same event. Every reading used to go to the fleet carrier, so opening the squadron carrier's panel
 * overwrote the other ship's callsign, tank and balances - and the squadron carrier never acquired a
 * callsign of its own, which is what makes it recognisable as the commander's later on.
 */
class CarrierStatsRoutingTest {

    private static final long FLEET_ID = 3712500736L;
    private static final long SQUADRON_ID = 3712500999L;

    private final PlayerSession session = PlayerSession.getInstance();

    @BeforeEach
    @AfterEach
    void clean() {
        FleetCarrierManager.getInstance().save(new CarrierDataDto());
        SquadronCarrierManager.getInstance().save(new CarrierDataDto());
    }

    @Test
    void aFleetCarrierReadingGoesToTheFleetCarrier() {
        session.setCarrierStats(stats(FLEET_ID, "FleetCarrier", "GHY-L8X", 956));

        assertEquals("GHY-L8X", session.getFleetCarrierData().getCallSign());
        assertEquals(956, session.getFleetCarrierData().getFuelLevel());
        assertNull(SquadronCarrierManager.getInstance().get().getCallSign());
    }

    @Test
    void aSquadronCarrierReadingGoesToTheSquadronCarrier() {
        session.setCarrierStats(stats(SQUADRON_ID, "SquadronCarrier", "SQD-001", 400));

        assertEquals("SQD-001", SquadronCarrierManager.getInstance().get().getCallSign());
        assertEquals(400, SquadronCarrierManager.getInstance().get().getFuelLevel());
    }

    /**
     * The bug this fixes: one ship's panel must not rewrite the other's record.
     */
    @Test
    void aSquadronReadingLeavesTheFleetCarrierAlone() {
        session.setCarrierStats(stats(FLEET_ID, "FleetCarrier", "GHY-L8X", 956));

        session.setCarrierStats(stats(SQUADRON_ID, "SquadronCarrier", "SQD-001", 400));

        assertEquals("GHY-L8X", session.getFleetCarrierData().getCallSign(),
                "the fleet carrier still has its own callsign");
        assertEquals(956, session.getFleetCarrierData().getFuelLevel(),
                "and its own tank");
    }

    /**
     * Squadron carriers are the newcomer. A journal that says nothing about the kind of carrier predates
     * them, so an absent type is the fleet carrier - guessing the other way would move a commander's whole
     * carrier record on the strength of a missing field.
     */
    @Test
    void aReadingWithNoCarrierTypeIsTheFleetCarriers() {
        session.setCarrierStats(stats(FLEET_ID, null, "GHY-L8X", 956));

        assertEquals("GHY-L8X", session.getFleetCarrierData().getCallSign());
        assertNull(SquadronCarrierManager.getInstance().get().getCallSign());
    }

    /**
     * The id is the handle every other carrier event uses - a fuel deposit, a trade order - and this reading
     * is the only place it is ever stated.
     */
    @Test
    void theReadingIsWhereWeLearnTheCarriersId() {
        session.setCarrierStats(stats(SQUADRON_ID, "SquadronCarrier", "SQD-001", 400));

        assertEquals(SQUADRON_ID, SquadronCarrierManager.getInstance().get().getCarrierId());
        assertTrue(OurCarriers.byId(SQUADRON_ID).isPresent());
        assertEquals("SQD-001", OurCarriers.byId(SQUADRON_ID).orElseThrow().data().getCallSign());
    }

    /**
     * Zero is what an id reads as before any panel has been opened, so it must never match a carrier -
     * otherwise every unidentified carrier would be the same carrier.
     */
    @Test
    void anUnknownIdMatchesNoCarrier() {
        assertTrue(OurCarriers.byId(0).isEmpty());
    }

    /**
     * With the squadron carrier finally holding its own callsign, the cargo ledger can tell the two apart -
     * which is what the whole per-carrier hold depends on.
     */
    @Test
    void aSquadronCarriersMarketSeedsItsOwnLedger() {
        session.setCarrierStats(stats(FLEET_ID, "FleetCarrier", "GHY-L8X", 956));
        session.setCarrierStats(stats(SQUADRON_ID, "SquadronCarrier", "SQD-001", 400));

        CarrierHoldLedger.seedFrom(marketOf("SQD-001", SQUADRON_ID, Map.of("tritium", 800)));

        assertEquals(800, SquadronCarrierManager.getInstance().get().getFuelReserve());
        assertEquals(0, session.getFleetCarrierData().getFuelReserve(),
                "the fleet carrier's fuel is not the squadron carrier's");
    }

    private static GameEvents.MarketEvent marketOf(String stationName, long marketId, Map<String, Integer> stock) {
        GameEvents.MarketEvent market = new GameEvents.MarketEvent();
        market.setEvent("Market");
        market.setTimestamp(Instant.now().toString());
        market.setMarketID(marketId);
        market.setStationName(stationName);
        market.setStationType("FleetCarrier");
        market.setStarSystem("Hyades Sector NR-V b2-2");
        List<GameEvents.MarketEvent.MarketItem> items = new ArrayList<>();
        stock.forEach((symbol, units) -> {
            GameEvents.MarketEvent.MarketItem item = new GameEvents.MarketEvent.MarketItem();
            item.setName("$" + symbol + "_name;");
            item.setStock(units);
            items.add(item);
        });
        market.setItems(items);
        return market;
    }

    private static CarrierStatsEvent stats(long carrierId, String carrierType, String callsign, int fuel) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CarrierStats");
        j.addProperty("CarrierID", carrierId);
        if (carrierType != null) j.addProperty("CarrierType", carrierType);
        j.addProperty("Callsign", callsign);
        j.addProperty("Name", "LONE WOLF");
        j.addProperty("DockingAccess", "all");
        j.addProperty("FuelLevel", fuel);
        return new CarrierStatsEvent(j);
    }
}
