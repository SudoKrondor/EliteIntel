package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.SAASignalsFoundEvent;
import elite.intel.gameapi.journal.events.ScanEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.subscribers.SilentPersistenceSubscriber;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The pre-scan replay (runs on every startup) must persist rings correctly too - otherwise it
 * re-clobbers live data and the recovery migration back to MOON with no signals on each launch.
 * These calls are synchronous (pre-scan runs single-threaded on a private bus), so no polling.
 */
class PreScanRingPersistenceTest {

    private final SilentPersistenceSubscriber subscriber = new SilentPersistenceSubscriber();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Test
    void preScanScanAndSaaPersistRingWithSignals() {
        long sysAddr = 5_306_532_762_362L;
        long ringBodyId = 55L;
        String starSystem = "Col 285 Sector ZQ-C c13-19";
        String ringName = starSystem + " 8 B Ring";

        LocationDto star = new LocationDto(0L, sysAddr);
        star.setStarName(starSystem);
        star.setLocationType(LocationDto.LocationType.PRIMARY_STAR);
        Database.withDao(LocationDao.class, dao -> {
            dao.upsert(0L, "prescan_ring_test_primary_star", starSystem, sysAddr,
                    GsonFactory.getGson().toJson(star));
            return null;
        });

        // A ring auto-scan classifies as PLANETARY_RING (not MOON) and records its parent.
        subscriber.onScan(ringScanEvent(ringName, sysAddr, ringBodyId));
        LocationDto afterScan = locationManager.findBySystemAddress(sysAddr, ringBodyId);
        assertEquals(LocationDto.LocationType.PLANETARY_RING, afterScan.getLocationType());
        assertEquals(starSystem + " 8", afterScan.getParentBodyName());

        // The SAA signals for that ring persist the mining reserves and stay classified as a ring.
        subscriber.onSAASignalsFound(ringSaaEvent(ringName, sysAddr, ringBodyId));
        LocationDto afterSaa = locationManager.findBySystemAddress(sysAddr, ringBodyId);
        assertEquals(LocationDto.LocationType.PLANETARY_RING, afterSaa.getLocationType());
        assertFalse(afterSaa.getMaterials().isEmpty(), "ring mining reserves should persist in pre-scan");
        assertFalse(afterSaa.getSaaSignals().isEmpty(), "ring saaSignals should persist in pre-scan");
    }

    private static ScanEvent ringScanEvent(String bodyName, long systemAddress, long bodyId) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Scan");
        j.addProperty("ScanType", "AutoScan");
        j.addProperty("BodyName", bodyName);
        j.addProperty("BodyID", bodyId);
        j.addProperty("StarSystem", bodyName.substring(0, bodyName.length() - " 8 B Ring".length()));
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("DistanceFromArrivalLS", 1037.5);
        JsonArray parents = new JsonArray();
        JsonObject planet = new JsonObject();
        planet.addProperty("Planet", 14);
        parents.add(planet);
        j.add("Parents", parents);
        return new ScanEvent(j);
    }

    private static SAASignalsFoundEvent ringSaaEvent(String bodyName, long systemAddress, long bodyId) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "SAASignalsFound");
        j.addProperty("BodyName", bodyName);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("BodyID", bodyId);
        JsonArray signals = new JsonArray();
        for (String type : new String[]{"Alexandrite", "Monazite", "Musgravite"}) {
            JsonObject s = new JsonObject();
            s.addProperty("Type", type);
            s.addProperty("Count", 1);
            signals.add(s);
        }
        j.add("Signals", signals);
        j.add("Genuses", new JsonArray());
        return new SAASignalsFoundEvent(j);
    }
}
