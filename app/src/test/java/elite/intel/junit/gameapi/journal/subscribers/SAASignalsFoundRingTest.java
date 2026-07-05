package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.SAASignalsFoundEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.MaterialDto;
import elite.intel.gameapi.journal.subscribers.SAASignalsFoundSubscriber;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression coverage for the ring-persistence clobber: SAASignalsFound for a ring must land as a
 * PLANETARY_RING carrying its mining reserves, and concurrent writes to the same body (Scan racing
 * SAASignalsFound) must not lose fields.
 */
class SAASignalsFoundRingTest {

    private final SAASignalsFoundSubscriber subscriber = new SAASignalsFoundSubscriber();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Test
    void ringSignalsPersistAsPlanetaryRingWithMaterials() throws InterruptedException {
        long sysAddr = 633_608_901_490L;
        long ringBodyId = 16L;
        String starSystem = "Praea Euq XN-E c13-2";
        String ringName = starSystem + " 2 A Ring";

        // SAASignalsFoundSubscriber reads the primary star for coordinates/name; seed one so the
        // ring record inherits a non-null starName (otherwise LocationManager.save() no-ops).
        LocationDto star = new LocationDto(0L, sysAddr);
        star.setStarName(starSystem);
        star.setLocationType(LocationDto.LocationType.PRIMARY_STAR);
        Database.withDao(LocationDao.class, dao -> {
            dao.upsert(0L, "saa_ring_test_primary_star", starSystem, sysAddr,
                    GsonFactory.getGson().toJson(star));
            return null;
        });

        subscriber.onSAASignalsFound(ringEvent(ringName, sysAddr, ringBodyId));

        awaitTrue(() -> {
            LocationDto r = locationManager.findBySystemAddress(sysAddr, ringBodyId);
            return LocationDto.LocationType.PLANETARY_RING.equals(r.getLocationType());
        });

        LocationDto ring = locationManager.findBySystemAddress(sysAddr, ringBodyId);
        assertEquals(LocationDto.LocationType.PLANETARY_RING, ring.getLocationType());
        assertEquals(ringName, ring.getPlanetName());
        assertEquals(starSystem + " 2", ring.getParentBodyName());
        assertFalse(ring.getMaterials().isEmpty(), "ring mining reserves should be persisted");
        assertFalse(ring.getSaaSignals().isEmpty(), "ring saaSignals should be persisted");
    }

    @Test
    void concurrentUpdateBodyDoesNotLoseFields() throws Exception {
        long sysAddr = 999_111_222L;
        long bodyId = 42L;
        String starSystem = "ConcurrencySys";

        // Seed the body so both writers merge into it (starName present so save() persists).
        LocationDto seed = new LocationDto(bodyId, sysAddr);
        seed.setStarName(starSystem);
        seed.setPlanetName("ConcurrencySys 1");
        Database.withDao(LocationDao.class, dao -> {
            dao.upsert(bodyId, "ConcurrencySys 1", starSystem, sysAddr, GsonFactory.getGson().toJson(seed));
            return null;
        });

        // Two writers touching disjoint fields of the same body, contending hard over many rounds.
        // Under the per-body lock each round is atomic, so the last-written row must carry BOTH the
        // materials (writer A) and the PLANETARY_RING classification (writer B). Without the lock the
        // later blind whole-JSON write would drop one side.
        int rounds = 200;
        for (int i = 0; i < rounds; i++) {
            CyclicBarrier barrier = new CyclicBarrier(2);
            CountDownLatch done = new CountDownLatch(2);
            Runnable a = () -> {
                await(barrier);
                locationManager.updateBody(sysAddr, bodyId, loc -> {
                    List<MaterialDto> mats = new ArrayList<>();
                    mats.add(new MaterialDto("Alexandrite", 100, true));
                    loc.setMaterials(mats);
                });
                done.countDown();
            };
            Runnable b = () -> {
                await(barrier);
                locationManager.updateBody(sysAddr, bodyId, loc -> loc.setLocationType(LocationDto.LocationType.PLANETARY_RING));
                done.countDown();
            };
            Thread.ofVirtual().start(a);
            Thread.ofVirtual().start(b);
            done.await();

            LocationDto merged = locationManager.findBySystemAddress(sysAddr, bodyId);
            assertEquals(LocationDto.LocationType.PLANETARY_RING, merged.getLocationType(), "round " + i);
            assertFalse(merged.getMaterials().isEmpty(), "materials lost in round " + i);
        }
    }

    private static SAASignalsFoundEvent ringEvent(String bodyName, long systemAddress, long bodyId) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "SAASignalsFound");
        j.addProperty("BodyName", bodyName);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("BodyID", bodyId);
        JsonArray signals = new JsonArray();
        for (String type : new String[]{"Serendibite", "Alexandrite", "Monazite"}) {
            JsonObject s = new JsonObject();
            s.addProperty("Type", type);
            s.addProperty("Count", 1);
            signals.add(s);
        }
        j.add("Signals", signals);
        j.add("Genuses", new JsonArray());
        return new SAASignalsFoundEvent(j);
    }

    private static void await(CyclicBarrier b) {
        try {
            b.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 3 seconds");
            Thread.sleep(10);
        }
    }
}
