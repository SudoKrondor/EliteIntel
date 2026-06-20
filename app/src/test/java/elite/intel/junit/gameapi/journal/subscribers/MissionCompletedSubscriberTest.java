package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.MissionCompletedEvent;
import elite.intel.gameapi.journal.subscribers.MissionAcceptedSubscriber;
import elite.intel.gameapi.journal.subscribers.MissionCompletedSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class MissionCompletedSubscriberTest {

    private final MissionCompletedSubscriber subscriber = new MissionCompletedSubscriber();
    private final MissionAcceptedSubscriber acceptedSubscriber = new MissionAcceptedSubscriber();
    private final MissionManager missionManager = MissionManager.getInstance();
    private final PlayerSession session = PlayerSession.getInstance();

    @BeforeEach
    void clearMissions() {
        missionManager.clear();
        session.setCurrentPrimaryStarName("Sol");
    }

    @Test
    void genericMissionIsRemovedFromDbAfterCompletion() throws InterruptedException {
        acceptedSubscriber.onMissionAcceptedEvent(acceptedEvent(500L, "Mission_Courier", "Deliver", "GoodFaction", null, null));
        assertNotNull(missionManager.getMission(500L));

        subscriber.onMissionCompletedEvent(completedEvent(500L, "Mission_Courier", "Deliver"));

        awaitTrue(() -> missionManager.getMission(500L) == null);
        assertNull(missionManager.getMission(500L));
    }

    @Test
    void completingUnknownMissionExitsSilentlyWithoutException() {
        assertDoesNotThrow(() -> {
            subscriber.onMissionCompletedEvent(completedEvent(999L, "Mission_Courier", "Unknown mission"));
            Thread.sleep(200);
        });
    }

    @Test
    void pirateMissionIsRemovedAfterCompletion() throws InterruptedException {
        acceptedSubscriber.onMissionAcceptedEvent(acceptedEvent(501L, "Mission_Massacre", "Kill pirates", "EmpireFaction", "Deciat", "SpacePirates"));
        assertNotNull(session.getMission(501L));

        subscriber.onMissionCompletedEvent(completedEvent(501L, "Mission_Massacre", "Kill pirates"));

        awaitTrue(() -> session.getMission(501L) == null);
        assertNull(session.getMission(501L));
    }

    private static MissionAcceptedEvent acceptedEvent(long id, String name, String localised,
                                                      String faction, String destSystem, String targetFaction) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "MissionAccepted");
        j.addProperty("MissionID", id);
        j.addProperty("Name", name);
        j.addProperty("LocalisedName", localised);
        j.addProperty("Faction", faction);
        j.addProperty("Reward", 50_000L);
        j.addProperty("Expiry", Instant.now().plusSeconds(3600).toString());
        if (destSystem != null) j.addProperty("DestinationSystem", destSystem);
        if (targetFaction != null) j.addProperty("TargetFaction", targetFaction);
        return new MissionAcceptedEvent(j);
    }

    private static MissionCompletedEvent completedEvent(long id, String name, String localised) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "MissionCompleted");
        j.addProperty("MissionID", id);
        j.addProperty("Name", name);
        j.addProperty("LocalisedName", localised);
        j.addProperty("Reward", 50_000L);
        return new MissionCompletedEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
