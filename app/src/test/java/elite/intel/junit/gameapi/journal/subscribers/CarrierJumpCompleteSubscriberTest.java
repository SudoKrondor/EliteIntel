package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.CarrierJumpEvent;
import elite.intel.gameapi.journal.subscribers.CarrierJumpCompleteSubscriber;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class CarrierJumpCompleteSubscriberTest {

    private final CarrierJumpCompleteSubscriber subscriber = new CarrierJumpCompleteSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

    @BeforeEach
    @AfterEach
    void clearDockedStatus() {
        GameEvents.StatusEvent undocked = new GameEvents.StatusEvent();
        undocked.setFlags(0L);
        Status.getInstance().setStatus(undocked);
    }

    @Test
    void dockedWithBodyIdUpdatesLocationId() throws InterruptedException {
        long sysAddr = 70001000L;
        long bodyId = 7L;
        session.setCurrentLocationId(999L, sysAddr);
        setDockedStatus();

        subscriber.onCarrierJumpCompleteEvent(carrierJumpEvent("CarrierTestSystem", sysAddr, bodyId));

        awaitTrue(() -> Long.valueOf(bodyId).equals(session.getLocationData().getInGameId()));

        assertEquals(bodyId, session.getLocationData().getInGameId());
    }

    @Test
    void dockedWithNullBodyIdDoesNotUpdateLocationId() throws InterruptedException {
        long sysAddr = 70002000L;
        long knownBodyId = 42L;
        session.setCurrentLocationId(knownBodyId, sysAddr);
        setDockedStatus();

        subscriber.onCarrierJumpCompleteEvent(carrierJumpEventNullBodyId("CarrierNullSystem", sysAddr));

        Thread.sleep(300);

        LocationData<Long, Long> loc = session.getLocationData();
        assertEquals(knownBodyId, loc.getInGameId(), "null BodyID must not overwrite current_location_id");
    }

    private static void setDockedStatus() {
        GameEvents.StatusEvent docked = new GameEvents.StatusEvent();
        docked.setFlags(1L); // DOCKED bit
        Status.getInstance().setStatus(docked);
    }

    private static CarrierJumpEvent carrierJumpEvent(String starSystem, long systemAddress, long bodyId) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CarrierJump");
        j.addProperty("Docked", true);
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("Body", starSystem + " A");
        j.addProperty("BodyID", bodyId);
        JsonArray starPos = new JsonArray();
        starPos.add(1.0);
        starPos.add(2.0);
        starPos.add(3.0);
        j.add("StarPos", starPos);
        return new CarrierJumpEvent(j);
    }

    private static CarrierJumpEvent carrierJumpEventNullBodyId(String starSystem, long systemAddress) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CarrierJump");
        j.addProperty("Docked", true);
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("Body", starSystem + " A");
        // BodyID intentionally omitted — Gson will deserialise it as null
        JsonArray starPos = new JsonArray();
        starPos.add(1.0);
        starPos.add(2.0);
        starPos.add(3.0);
        j.add("StarPos", starPos);
        return new CarrierJumpEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
