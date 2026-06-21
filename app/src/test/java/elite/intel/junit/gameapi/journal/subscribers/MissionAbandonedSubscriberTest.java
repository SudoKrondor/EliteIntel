package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.MissionAbandonedEvent;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.subscribers.MissionAbandonedSubscriber;
import elite.intel.gameapi.journal.subscribers.MissionAcceptedSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class MissionAbandonedSubscriberTest {

    private final MissionAbandonedSubscriber subscriber = new MissionAbandonedSubscriber();
    private final MissionAcceptedSubscriber acceptedSubscriber = new MissionAcceptedSubscriber();
    private final MissionManager missionManager = MissionManager.getInstance();

    @BeforeEach
    void clearMissions() {
        missionManager.clear();
        PlayerSession.getInstance().setCurrentPrimaryStarName("Sol");
    }

    @Test
    void missionIsRemovedFromDbAfterAbandonment() throws InterruptedException {
        acceptedSubscriber.onMissionAcceptedEvent(acceptedEvent(600L, "Mission_Courier", "Deliver cargo"));
        assertNotNull(missionManager.getMission(600L));

        subscriber.onMissionAbandonedEvent(abandonedEvent(600L, "Mission_Courier"));

        awaitTrue(() -> missionManager.getMission(600L) == null);
        assertNull(missionManager.getMission(600L));
    }

    @Test
    void abandoningMissionNotInDbExitsSilentlyWithoutException() {
        assertDoesNotThrow(() -> {
            subscriber.onMissionAbandonedEvent(abandonedEvent(999L, "Mission_Courier"));
            Thread.sleep(200);
        });
    }

    @Test
    void onlyAbandonedMissionIsRemovedOthersMissionsSurvive() throws InterruptedException {
        acceptedSubscriber.onMissionAcceptedEvent(acceptedEvent(601L, "Mission_Courier", "Mission one"));
        acceptedSubscriber.onMissionAcceptedEvent(acceptedEvent(602L, "Mission_Delivery", "Mission two"));

        subscriber.onMissionAbandonedEvent(abandonedEvent(601L, "Mission_Courier"));

        awaitTrue(() -> missionManager.getMission(601L) == null);
        assertNull(missionManager.getMission(601L));
        assertNotNull(missionManager.getMission(602L));
    }

    private static MissionAcceptedEvent acceptedEvent(long id, String name, String localised) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "MissionAccepted");
        j.addProperty("MissionID", id);
        j.addProperty("Name", name);
        j.addProperty("LocalisedName", localised);
        j.addProperty("Faction", "SomeFaction");
        j.addProperty("Reward", 20_000L);
        j.addProperty("Expiry", Instant.now().plusSeconds(3600).toString());
        return new MissionAcceptedEvent(j);
    }

    private static MissionAbandonedEvent abandonedEvent(long id, String name) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "MissionAbandoned");
        j.addProperty("MissionID", id);
        j.addProperty("Name", name);
        return new MissionAbandonedEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
