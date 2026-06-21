package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.gameapi.journal.events.CarrierStatsEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.subscribers.CarrierStatsSubscriber;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CarrierStatsSubscriberTest {

    private final CarrierStatsSubscriber subscriber = new CarrierStatsSubscriber();
    private final FleetCarrierManager carrierManager = FleetCarrierManager.getInstance();

    @Test
    void carrierCallsignAndNameAreStoredInDb() {
        subscriber.onCarrierStatsEvent(carrierStatsEvent("X7V-K3B", "ISS Freedom", "Drake-Class", 90));

        CarrierDataDto stored = carrierManager.get();
        assertNotNull(stored);
        assertEquals("X7V-K3B", stored.getCallSign());
        assertEquals("ISS Freedom", stored.getCarrierName());
    }

    @Test
    void carrierFuelLevelIsStored() {
        subscriber.onCarrierStatsEvent(carrierStatsEvent("A1B-C2D", "Deep Space Carrier", "Drake-Class", 42));

        CarrierDataDto stored = carrierManager.get();
        assertEquals(42, stored.getFuelLevel());
    }

    private static CarrierStatsEvent carrierStatsEvent(String callsign, String name, String carrierType, int fuelLevel) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CarrierStats");
        j.addProperty("CarrierID", 3700000001L);
        j.addProperty("Callsign", callsign);
        j.addProperty("Name", name);
        j.addProperty("CarrierType", carrierType);
        j.addProperty("DockingAccess", "all");
        j.addProperty("AllowNotorious", false);
        j.addProperty("FuelLevel", fuelLevel);
        j.addProperty("JumpRangeCurr", 500.0);
        j.addProperty("JumpRangeMax", 500.0);
        j.addProperty("PendingDecommission", false);
        return new CarrierStatsEvent(j);
    }
}
