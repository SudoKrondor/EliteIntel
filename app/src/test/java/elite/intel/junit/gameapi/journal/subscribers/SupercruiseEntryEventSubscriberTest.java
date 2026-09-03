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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Leaving for supercruise clears the station fields from the record of the body we were at - docking writes
 * them onto it - but it does not erase the station's own record. A station is still a station once we leave,
 * and what we saw of its market and services is first-hand data worth more than anything a lookup can offer.
 */
class SupercruiseEntryEventSubscriberTest {

    private static final AtomicInteger RUN = new AtomicInteger();

    private final SupercruiseEntryEventSubscriber subscriber = new SupercruiseEntryEventSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Test
    void supercruiseEntryClearsStationFieldsFromTheBodyWeDroppedAt() throws InterruptedException {
        int run = RUN.incrementAndGet();
        long sysAddr = 777_888_999L + run;
        long bodyId = 8L;
        String star = "Deciat Entry " + run;
        seedPrimaryStar(sysAddr, star, run);

        // The body's record after docking at a port on it: the planet, wearing the station's fields.
        LocationDto body = new LocationDto(bodyId, sysAddr);
        body.setStarName(star);
        body.setPlanetName(star + " 1 a");
        body.setLocationType(LocationDto.LocationType.PLANET);
        body.setStationName("Felicia Winter's Retreat");
        body.setStationType("Coriolis");
        body.setStationFaction("Pilots Federation");
        body.setStationEconomy("High Tech");
        body.setStationGovernment("Democracy");
        locationManager.save(body);

        session.setCurrentLocationId(bodyId, sysAddr);
        session.setCurrentPrimaryStarName(star);

        subscriber.onSuperCruiseEntryEvent(supercruiseEntryEvent(star, sysAddr));

        awaitTrue(() -> locationManager.findBySystemAddress(sysAddr, bodyId).getStationFaction() == null);

        LocationDto updated = locationManager.findBySystemAddress(sysAddr, bodyId);
        assertNull(updated.getStationFaction());
        assertNull(updated.getStationEconomy());
        assertNull(updated.getStationGovernment());
        assertNull(updated.getStationName());
        assertEquals(star + " 1 a", updated.getPlanetName(), "the body keeps its own identity");
    }

    @Test
    void aStationsOwnRecordIsNotErasedByLeaving() throws InterruptedException {
        int run = RUN.incrementAndGet();
        long sysAddr = 777_888_999L + run;
        long bodyId = 9L;
        String star = "Deciat Entry " + run;
        seedPrimaryStar(sysAddr, star, run);

        LocationDto station = new LocationDto(bodyId, sysAddr);
        station.setStarName(star);
        station.setStationName("Garden Ring " + run);
        station.setStationType("Coriolis");
        station.setStationFaction("Pilots Federation");
        station.setLocationType(LocationDto.LocationType.STATION);
        locationManager.save(station);

        session.setCurrentLocationId(bodyId, sysAddr);
        session.setCurrentPrimaryStarName(star);

        subscriber.onSuperCruiseEntryEvent(supercruiseEntryEvent(star, sysAddr));

        // Nothing to wait on: the assertion is that nothing happens, so give the handler its moment first.
        Thread.sleep(300);
        LocationDto stored = locationManager.findBySystemAddress(sysAddr, bodyId);
        assertEquals("Garden Ring " + run, stored.getStationName());
        assertEquals("Pilots Federation", stored.getStationFaction());
        assertEquals(LocationDto.LocationType.STATION, stored.getLocationType());
    }

    /**
     * The subscriber reads the system's star name off the PRIMARY_STAR record, and a record with no star name
     * is never saved - so without one seeded here the handler would appear to do nothing at all.
     */
    private void seedPrimaryStar(long sysAddr, String star, int run) {
        LocationDto primary = new LocationDto(1L, sysAddr);
        primary.setStarName(star);
        primary.setLocationType(LocationDto.LocationType.PRIMARY_STAR);
        Database.withDao(LocationDao.class, dao -> {
            dao.upsert(1L, "supercruise_test_primary_star_" + run, star, sysAddr, GsonFactory.getGson().toJson(primary));
            return null;
        });
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
