package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.DockedEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.subscribers.DockedSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class DockedSubscriberTest {

    private final DockedSubscriber subscriber = new DockedSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Test
    void stationNameAndStarSystemAreStoredAfterDocking() throws InterruptedException {
        long sysAddr = 444_111_222L;
        long bodyId = 3L;
        session.setCurrentLocationId(bodyId, sysAddr);

        subscriber.onDockedEvent(dockedEvent("Jameson Memorial", "Orbis", "Shinrarta Dezhra", sysAddr, 128_016_640L));

        awaitTrue(() -> locationManager.findBySystemAddress(sysAddr, bodyId).getStationName() != null);

        LocationDto saved = locationManager.findBySystemAddress(sysAddr, bodyId);
        assertEquals("Jameson Memorial", saved.getStationName());
        assertEquals("Shinrarta Dezhra", saved.getStarName());
    }

    @Test
    void marketIdIsStoredAfterDocking() throws InterruptedException {
        long sysAddr = 444_333_555L;
        long bodyId = 1L;
        session.setCurrentLocationId(bodyId, sysAddr);

        subscriber.onDockedEvent(dockedEvent("Frontier Outpost", "Coriolis", "Sol", sysAddr, 77_777L));

        awaitTrue(() -> locationManager.findBySystemAddress(sysAddr, bodyId).getStationName() != null);

        LocationDto saved = locationManager.findBySystemAddress(sysAddr, bodyId);
        assertEquals(77_777L, saved.getMarketID());
    }

    @Test
    void locationTypeIsSetToStationForNonCarrierDock() throws InterruptedException {
        long sysAddr = 444_666_999L;
        long bodyId = 2L;
        session.setCurrentLocationId(bodyId, sysAddr);

        subscriber.onDockedEvent(dockedEvent("Hutton Orbital", "Coriolis", "Alpha Centauri", sysAddr, 55_000L));

        awaitTrue(() -> locationManager.findBySystemAddress(sysAddr, bodyId).getLocationType()
                == LocationDto.LocationType.STATION);

        LocationDto saved = locationManager.findBySystemAddress(sysAddr, bodyId);
        assertEquals(LocationDto.LocationType.STATION, saved.getLocationType());
    }

    private static DockedEvent dockedEvent(String stationName, String stationType, String starSystem,
                                           long systemAddress, long marketId) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Docked");
        j.addProperty("StationName", stationName);
        j.addProperty("StationType", stationType);
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("MarketID", marketId);
        j.addProperty("StationGovernment", "$government_Democracy;");
        j.addProperty("StationGovernment_Localised", "Democracy");
        j.addProperty("StationEconomy", "$economy_HighTech;");
        j.addProperty("StationEconomy_Localised", "High Tech");

        JsonObject faction = new JsonObject();
        faction.addProperty("Name", "Pilots Federation");
        faction.addProperty("FactionState", "None");
        j.add("StationFaction", faction);

        JsonArray services = new JsonArray();
        services.add("Refuel");
        services.add("Repair");
        j.add("StationServices", services);

        j.addProperty("DistFromStarLS", 100.0);
        return new DockedEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
