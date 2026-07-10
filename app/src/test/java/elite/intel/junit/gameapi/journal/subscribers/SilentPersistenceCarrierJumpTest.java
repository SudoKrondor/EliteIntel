package elite.intel.junit.gameapi.journal.subscribers;

import com.google.common.eventbus.EventBus;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.gameapi.journal.events.CarrierJumpEvent;
import elite.intel.gameapi.journal.events.CarrierLocationEvent;
import elite.intel.gameapi.journal.events.FSDJumpEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.subscribers.SilentPersistenceSubscriber;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The startup pre-scan replays the journal into the DB. A carrier jump moves the commander without
 * an FSDJump or Location event, so without a CarrierJump handler the pre-scan leaves the
 * current-location pointer at the system he flew to under his own power — arbitrarily far away.
 *
 * <p>Unlike its sibling {@code PreScanRingPersistenceTest}, this test posts through a real EventBus
 * rather than calling the subscriber directly. The original defect was a <em>missing</em>
 * {@code @Subscribe}: the event was constructed and published, and nothing consumed it. A direct
 * method call cannot detect that. Do not "simplify" this into a direct call.
 */
class SilentPersistenceCarrierJumpTest {

    private final PlayerSession session = PlayerSession.getInstance();

    @Test
    void carrierJumpAdvancesCurrentLocationPointerDuringPreScan() {
        EventBus privateBus = new EventBus("pre-scan-test");
        privateBus.register(new SilentPersistenceSubscriber());

        long fsdSysAddr = 91704628386L;
        long carrierSysAddr = 362891416506L;

        // Commander flies to a system under his own power...
        privateBus.post(fsdJump("Phylurn CW-S c18-0", fsdSysAddr, 2L, 4674.31, 441.25, 2300.5));
        assertEquals(fsdSysAddr, session.getLocationData().getSystemAddress());

        // ...then his carrier jumps while he is on foot on the deck. Docked is false here.
        privateBus.post(onFootCarrierJump("Pro Eurl FV-M c21-1", carrierSysAddr, 1L, 2492.40, 279.65, 1144.5));

        LocationData<Long, Long> loc = session.getLocationData();
        assertEquals(carrierSysAddr, loc.getSystemAddress(), "pre-scan must follow the carrier, not the last FSD jump");
        assertEquals(1L, loc.getInGameId());
    }

    @Test
    void carrierJumpWithoutCommanderAboardLeavesPointerAlone() {
        EventBus privateBus = new EventBus("pre-scan-test");
        privateBus.register(new SilentPersistenceSubscriber());

        long fsdSysAddr = 91771737242L;
        privateBus.post(fsdJump("Phylurn ZP-U c17-0", fsdSysAddr, 1L, 4700.0, 450.0, 2310.0));

        JsonObject j = carrierJumpJson("Somewhere Else", 999999L, 1L, 1.0, 2.0, 3.0);
        j.addProperty("Docked", false);
        j.addProperty("OnFoot", false);
        privateBus.post(new CarrierJumpEvent(j));

        assertEquals(fsdSysAddr, session.getLocationData().getSystemAddress());
    }

    /**
     * The carrier can jump while the commander is elsewhere, which produces CarrierLocation and no
     * CarrierJump at all. Without replaying it, the carrier's last known system is stale on startup.
     */
    @Test
    void carrierLocationRestoresLastKnownCarrierSystemDuringPreScan() {
        EventBus privateBus = new EventBus("pre-scan-test");
        privateBus.register(new SilentPersistenceSubscriber());

        privateBus.post(carrierLocation("Phylurn TR-D b28-0", 728401193201L));

        assertEquals("Phylurn TR-D b28-0", session.getLastKnownCarrierLocation());
    }

    @Test
    void preScanCarrierLocationNeverDecrementsFuel() {
        EventBus privateBus = new EventBus("pre-scan-test");
        privateBus.register(new SilentPersistenceSubscriber());

        FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
        route.clear();
        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setFuelLevel(500);
        session.setFleetCarrierData(carrier);

        // The journal is replayed on every start, so a decrement here would compound each launch.
        privateBus.post(carrierLocation("Phylurn CW-S c18-0", 91704628386L));
        privateBus.post(carrierLocation("Phylurn CW-S c18-0", 91704628386L));

        assertEquals(500, session.getFleetCarrierData().getFuelLevel());
    }

    private static CarrierLocationEvent carrierLocation(String starSystem, long systemAddress) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CarrierLocation");
        j.addProperty("CarrierType", "FleetCarrier");
        j.addProperty("CarrierID", 3712500736L);
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("BodyID", 1);
        return new CarrierLocationEvent(j);
    }

    private static FSDJumpEvent fsdJump(String starSystem, long systemAddress, long bodyId,
                                        double x, double y, double z) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "FSDJump");
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("Body", starSystem + " A");
        j.addProperty("BodyID", bodyId);
        j.addProperty("BodyType", "Star");
        j.add("StarPos", starPos(x, y, z));
        return new FSDJumpEvent(j);
    }

    private static CarrierJumpEvent onFootCarrierJump(String starSystem, long systemAddress, long bodyId,
                                                      double x, double y, double z) {
        return new CarrierJumpEvent(carrierJumpJson(starSystem, systemAddress, bodyId, x, y, z));
    }

    private static JsonObject carrierJumpJson(String starSystem, long systemAddress, long bodyId,
                                              double x, double y, double z) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CarrierJump");
        j.addProperty("Docked", false);
        j.addProperty("OnFoot", true);
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("Body", starSystem + " A");
        j.addProperty("BodyID", bodyId);
        j.addProperty("BodyType", "Star");
        j.add("StarPos", starPos(x, y, z));
        return j;
    }

    private static JsonArray starPos(double x, double y, double z) {
        JsonArray starPos = new JsonArray();
        starPos.add(x);
        starPos.add(y);
        starPos.add(z);
        return starPos;
    }
}
