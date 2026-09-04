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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Docking files the station under its own name. It used to be written onto the record of the body the ship
 * dropped at - Docked carries no BodyID, so that record was the only one to hand - which re-labelled the body
 * as whatever was parked next to it, right down to storing a moon as a FLEET_CARRIER.
 */
class DockedSubscriberTest {

    private static final AtomicInteger RUN = new AtomicInteger();

    private final DockedSubscriber subscriber = new DockedSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Test
    void theStationIsStoredUnderItsOwnName() throws InterruptedException {
        int run = RUN.incrementAndGet();
        long sysAddr = 444_111_222L + run;
        long marketId = 128_016_640L + run;
        session.setCurrentLocationId(3L, sysAddr);

        subscriber.onDockedEvent(dockedEvent("Jameson Memorial " + run, "Orbis", "Shinrarta Dezhra", sysAddr, marketId));

        awaitTrue(() -> locationManager.findByMarketId(marketId).getStationName() != null);

        LocationDto station = locationManager.findByMarketId(marketId);
        assertEquals("Jameson Memorial " + run, station.getStationName());
        assertEquals("Shinrarta Dezhra", station.getStarName());
        assertEquals(marketId, station.getMarketID());
        assertEquals("Orbis", station.getStationType());
        assertEquals(LocationDto.LocationType.STATION, station.getLocationType());
        assertEquals(sysAddr, station.getSystemAddress());
    }

    @Test
    void theBodyWeDroppedAtIsLeftAlone() throws InterruptedException {
        int run = RUN.incrementAndGet();
        long sysAddr = 444_333_555L + run;
        long bodyId = 25L;
        long marketId = 77_777L + run;
        String star = "Docked Body Test " + run;
        String planet = star + " 7 a";

        LocationDto body = new LocationDto(bodyId, sysAddr);
        body.setStarName(star);
        body.setPlanetName(planet);
        body.setLocationType(LocationDto.LocationType.MOON);
        body.setGravity(0.08);
        locationManager.save(body);

        session.setCurrentLocationId(bodyId, sysAddr);

        // A carrier parked at that moon: the case that used to store the moon as a FLEET_CARRIER.
        subscriber.onDockedEvent(dockedEvent("GHY-L8X " + run, "FleetCarrier", star, sysAddr, marketId));

        awaitTrue(() -> locationManager.findByMarketId(marketId).getStationName() != null);

        LocationDto stillTheMoon = locationManager.findBySystemAddress(sysAddr, planet);
        assertEquals(LocationDto.LocationType.MOON, stillTheMoon.getLocationType());
        assertEquals(0.08, stillTheMoon.getGravity(), 1e-9);
        assertNull(stillTheMoon.getStationName(), "the carrier is not the moon it is parked at");

        LocationDto carrier = locationManager.findByMarketId(marketId);
        assertEquals(LocationDto.LocationType.FLEET_CARRIER, carrier.getLocationType());
        assertEquals(marketId, carrier.getBodyId(), "a station with no body of its own is filed under its MarketID");
        assertEquals(0.0, carrier.getGravity(), 1e-9, "the body's scan data must not be copied onto the station");
    }

    @Test
    void aSecondDockingUpdatesTheSameRecord() throws InterruptedException {
        int run = RUN.incrementAndGet();
        long sysAddr = 444_666_999L + run;
        long marketId = 55_000L + run;
        session.setCurrentLocationId(2L, sysAddr);

        subscriber.onDockedEvent(dockedEvent("Hutton Orbital " + run, "Coriolis", "Alpha Centauri", sysAddr, marketId));
        awaitTrue(() -> locationManager.findByMarketId(marketId).getStationName() != null);

        session.setCurrentLocationId(99L, sysAddr); // a different drop body: it must make no difference
        subscriber.onDockedEvent(dockedEvent("Hutton Orbital " + run, "Coriolis", "Alpha Centauri", sysAddr, marketId));
        Thread.sleep(300);

        assertEquals(1, locationManager.findAllBySystemAddress(sysAddr).stream()
                .filter(l -> ("Hutton Orbital " + run).equals(l.getStationName()))
                .count(), "one station, one record");
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
