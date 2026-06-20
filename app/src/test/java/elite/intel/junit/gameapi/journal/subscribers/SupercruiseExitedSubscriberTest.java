package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.SupercruiseExitEvent;
import elite.intel.gameapi.journal.subscribers.SupercruiseExitedSubscriber;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class SupercruiseExitedSubscriberTest {

    private final SupercruiseExitedSubscriber subscriber = new SupercruiseExitedSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

    @Test
    void supercruiseExitUpdatesCurrentLocationIdInSession() throws InterruptedException {
        long sysAddr = 555_444_333L;
        long bodyId = 12L;
        session.setCurrentPrimaryStarName("Wolf 359");

        subscriber.onSupercruiseExited(supercruiseExitEvent(sysAddr, bodyId, "Wolf 359 A 1", "Planet"));

        awaitTrue(() -> {
            LocationData<Long, Long> loc = session.getLocationData();
            return Long.valueOf(bodyId).equals(loc.getInGameId()) && sysAddr == loc.getSystemAddress();
        });

        LocationData<Long, Long> loc = session.getLocationData();
        assertEquals(sysAddr, loc.getSystemAddress());
        assertEquals(bodyId, loc.getInGameId());
    }

    @Test
    void supercruiseExitToStationUpdatesLocationId() throws InterruptedException {
        long sysAddr = 555_111_222L;
        long bodyId = 5L;
        session.setCurrentPrimaryStarName("Sol");

        subscriber.onSupercruiseExited(supercruiseExitEvent(sysAddr, bodyId, "Galileo", "Station"));

        awaitTrue(() -> Long.valueOf(bodyId).equals(session.getLocationData().getInGameId()));

        assertEquals(sysAddr, session.getLocationData().getSystemAddress());
    }

    private static SupercruiseExitEvent supercruiseExitEvent(long systemAddress, long bodyId,
                                                             String body, String bodyType) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "SupercruiseExit");
        j.addProperty("StarSystem", "Sol");
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("Body", body);
        j.addProperty("BodyID", bodyId);
        j.addProperty("BodyType", bodyType);
        j.addProperty("Taxi", false);
        j.addProperty("Multicrew", false);
        return new SupercruiseExitEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
