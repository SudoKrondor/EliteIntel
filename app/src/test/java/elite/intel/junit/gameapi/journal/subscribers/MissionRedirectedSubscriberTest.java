package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.MissionRedirectedEvent;
import elite.intel.gameapi.journal.subscribers.MissionAcceptedSubscriber;
import elite.intel.gameapi.journal.subscribers.MissionRedirectedSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class MissionRedirectedSubscriberTest {

    private final MissionRedirectedSubscriber subscriber = new MissionRedirectedSubscriber();
    private final MissionAcceptedSubscriber missionAcceptedSubscriber = new MissionAcceptedSubscriber();
    private final MissionManager missionManager = MissionManager.getInstance();

    @BeforeEach
    void clearMissions() throws InterruptedException {
        Thread.sleep(100);
        missionManager.clear();
    }

    @Test
    void redirectUpdatesDestinationStationOnMission() throws InterruptedException {
        long missionId = 800_001L;
        missionAcceptedSubscriber.onMissionAcceptedEvent(genericMissionAccepted(missionId, "Deciat", "Garay Terminal"));

        subscriber.onMissionRedirectedSubscriber(redirectEvent(missionId, "Cubeo", "Victoria's Gift"));

        awaitTrue(() -> {
            var mission = missionManager.getMission(missionId);
            return mission != null && "Victoria's Gift".equals(mission.getDestinationStation());
        });
        var mission = missionManager.getMission(missionId);
        assertEquals("Victoria's Gift", mission.getDestinationStation());
        assertEquals("Cubeo", mission.getDestinationSystem());
    }

    @Test
    void redirectUpdatesDestinationSystemOnMission() throws InterruptedException {
        long missionId = 800_002L;
        missionAcceptedSubscriber.onMissionAcceptedEvent(genericMissionAccepted(missionId, "Sol", "Galileo"));

        subscriber.onMissionRedirectedSubscriber(redirectEvent(missionId, "Alpha Centauri", "Hutton Orbital"));

        awaitTrue(() -> {
            var mission = missionManager.getMission(missionId);
            return mission != null && "Alpha Centauri".equals(mission.getDestinationSystem());
        });
        assertEquals("Alpha Centauri", missionManager.getMission(missionId).getDestinationSystem());
    }

    private static MissionAcceptedEvent genericMissionAccepted(long missionId, String system, String station) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "MissionAccepted");
        j.addProperty("Faction", "Pilots Federation");
        j.addProperty("Name", "Mission_Delivery");
        j.addProperty("LocalisedName", "Delivery mission");
        j.addProperty("MissionID", missionId);
        j.addProperty("DestinationSystem", system);
        j.addProperty("DestinationStation", station);
        j.addProperty("Reward", 50_000L);
        j.addProperty("Expiry", Instant.now().plusSeconds(3600).toString());
        return new MissionAcceptedEvent(j);
    }

    private static MissionRedirectedEvent redirectEvent(long missionId, String newSystem, String newStation) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "MissionRedirected");
        j.addProperty("MissionID", missionId);
        j.addProperty("Name", "Mission_Delivery");
        j.addProperty("LocalisedName", "Delivery mission");
        j.addProperty("NewDestinationSystem", newSystem);
        j.addProperty("NewDestinationStation", newStation);
        j.addProperty("OldDestinationSystem", "Sol");
        j.addProperty("OldDestinationStation", "Galileo");
        return new MissionRedirectedEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
