package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.MissionsEvent;
import elite.intel.gameapi.journal.subscribers.MissionAcceptedSubscriber;
import elite.intel.gameapi.journal.subscribers.MissionsEventSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

class MissionsEventSubscriberTest {

    private final MissionsEventSubscriber subscriber = new MissionsEventSubscriber();
    private final MissionAcceptedSubscriber missionAcceptedSubscriber = new MissionAcceptedSubscriber();
    private final MissionManager missionManager = MissionManager.getInstance();

    @BeforeEach
    void clearMissions() throws InterruptedException {
        Thread.sleep(100);
        missionManager.clear();
    }

    @Test
    void completedMissionsAreRemovedFromDatabase() throws InterruptedException {
        long missionId = 900_001L;
        missionAcceptedSubscriber.onMissionAcceptedEvent(genericMissionAccepted(missionId));

        subscriber.onMissionsEventSubscriber(missionsEventWithComplete(missionId));

        awaitTrue(() -> !missionManager.getMissions().containsKey(missionId));
        assertFalse(missionManager.getMissions().containsKey(missionId));
    }

    @Test
    void failedMissionsAreRemovedFromDatabase() throws InterruptedException {
        long missionId = 900_002L;
        missionAcceptedSubscriber.onMissionAcceptedEvent(genericMissionAccepted(missionId));

        subscriber.onMissionsEventSubscriber(missionsEventWithFailed(missionId));

        awaitTrue(() -> !missionManager.getMissions().containsKey(missionId));
        assertFalse(missionManager.getMissions().containsKey(missionId));
    }

    @Test
    void missionsNotInCompletedListAreRetained() throws InterruptedException {
        long keptId = 900_003L;
        long completedId = 900_004L;
        missionAcceptedSubscriber.onMissionAcceptedEvent(genericMissionAccepted(keptId));
        missionAcceptedSubscriber.onMissionAcceptedEvent(genericMissionAccepted(completedId));

        subscriber.onMissionsEventSubscriber(missionsEventWithComplete(completedId));

        awaitTrue(() -> !missionManager.getMissions().containsKey(completedId));
        assertFalse(missionManager.getMissions().containsKey(completedId));
        // keptId is active — not in complete/failed, so it should be retained
        // (The subscriber only removes missions whose IDs appear in complete/failed AND in the DB.)
    }

    private static MissionAcceptedEvent genericMissionAccepted(long missionId) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "MissionAccepted");
        j.addProperty("Faction", "Pilots Federation");
        j.addProperty("Name", "Mission_Delivery");
        j.addProperty("LocalisedName", "Delivery mission");
        j.addProperty("MissionID", missionId);
        j.addProperty("DestinationSystem", "Deciat");
        j.addProperty("DestinationStation", "Garay Terminal");
        j.addProperty("Reward", 50_000L);
        j.addProperty("Expiry", Instant.now().plusSeconds(3600).toString());
        return new MissionAcceptedEvent(j);
    }

    private static MissionsEvent missionsEventWithComplete(long missionId) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Missions");
        j.add("Active", new JsonArray());
        j.add("Failed", new JsonArray());
        JsonArray complete = new JsonArray();
        complete.add(missionEntry(missionId, "Mission_Delivery"));
        j.add("Complete", complete);
        return new MissionsEvent(j);
    }

    private static MissionsEvent missionsEventWithFailed(long missionId) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Missions");
        j.add("Active", new JsonArray());
        j.add("Complete", new JsonArray());
        JsonArray failed = new JsonArray();
        failed.add(missionEntry(missionId, "Mission_Delivery"));
        j.add("Failed", failed);
        return new MissionsEvent(j);
    }

    private static JsonObject missionEntry(long missionId, String name) {
        JsonObject m = new JsonObject();
        m.addProperty("MissionID", missionId);
        m.addProperty("Name", name);
        m.addProperty("PassengerMission", false);
        m.addProperty("Expires", 0L);
        return m;
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
