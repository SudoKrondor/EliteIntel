package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.SupercruiseEntryEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.subscribers.SupercruiseEntryEventSubscriber;
import elite.intel.session.PlayerSession;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class SupercruiseEntryEventSubscriberTest {

    private final SupercruiseEntryEventSubscriber subscriber = new SupercruiseEntryEventSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Test
    void supercruiseEntryClearsStationFieldsOnCurrentLocation() throws InterruptedException {
        long sysAddr = 777_888_999L;
        long starBodyId = 1L;
        long stationBodyId = 8L;

        // Pre-seed PRIMARY_STAR via DAO with a unique locationName so it doesn't conflict
        // with the station row. LocationManager.save() uses planetName (defaults to "")
        // as locationName, which would cause both rows to share the same "" key.
        LocationDto star = new LocationDto(starBodyId, sysAddr);
        star.setStarName("Deciat");
        star.setLocationType(LocationDto.LocationType.PRIMARY_STAR);
        Database.withDao(LocationDao.class, dao -> {
            dao.upsert(starBodyId, "supercruise_test_primary_star", "Deciat", sysAddr,
                    GsonFactory.getGson().toJson(star));
            return null;
        });

        // Pre-seed station using "" as locationName (the value LocationManager.save() computes
        // for non-planet locations). The subscriber will later save to the same "" key, updating
        // this row's JSON in-place — which is exactly what we want to poll.
        LocationDto station = new LocationDto(stationBodyId, sysAddr);
        station.setStarName("Deciat");
        station.setStationName("Felicia Winter's Retreat");
        station.setStationType("Coriolis");
        station.setStationFaction("Pilots Federation");
        station.setStationEconomy("High Tech");
        station.setStationGovernment("Democracy");
        station.setLocationType(LocationDto.LocationType.STATION);
        Database.withDao(LocationDao.class, dao -> {
            dao.upsert(stationBodyId, "", "Deciat", sysAddr, GsonFactory.getGson().toJson(station));
            return null;
        });

        session.setCurrentLocationId(stationBodyId, sysAddr);
        session.setCurrentPrimaryStarName("Deciat");

        subscriber.onSuperCruiseEntryEvent(supercruiseEntryEvent("Deciat", sysAddr));

        // Subscriber saves with locationName="" — same key as the station row above.
        // ON CONFLICT(locationName) UPDATE overwrites the JSON, clearing station fields.
        // Poll on stationFaction (no FleetCarrier fallback in getter, so null is detectable).
        awaitTrue(() -> locationManager.findBySystemAddress(sysAddr, stationBodyId).getStationFaction() == null);

        LocationDto updated = locationManager.findBySystemAddress(sysAddr, stationBodyId);
        // stationType null is surfaced as "FleetCarrier" by the getter — that's the cleared state
        assertEquals("FleetCarrier", updated.getStationType());
        assertNull(updated.getStationFaction());
        assertNull(updated.getStationEconomy());
        assertNull(updated.getStationGovernment());
    }

    private static SupercruiseEntryEvent supercruiseEntryEvent(String starSystem, long systemAddress) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "SupercruiseEntry");
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("Taxi", false);
        j.addProperty("Multicrew", false);
        return new SupercruiseEntryEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
