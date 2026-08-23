package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.MissionFailedEvent;
import elite.intel.gameapi.journal.subscribers.MissionAcceptedSubscriber;
import elite.intel.gameapi.journal.subscribers.MissionFailedSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A failed mission is gone, and the commonest way to fail one is to let it expire.
 *
 * <p>Nothing listened to {@code MissionFailed} at all, so an expired courier run stayed in the database and
 * on the HUD card indefinitely. Shaped after {@code MissionAbandonedSubscriberTest}, because the two
 * subscribers answer the same question about the same record.
 */
class MissionFailedSubscriberTest {

    private final MissionFailedSubscriber subscriber = new MissionFailedSubscriber();
    private final MissionAcceptedSubscriber acceptedSubscriber = new MissionAcceptedSubscriber();
    private final MissionManager missionManager = MissionManager.getInstance();

    @BeforeEach
    void clearMissions() {
        missionManager.clear();
        PlayerSession.getInstance().setCurrentPrimaryStarName("Sol");
    }

    @Test
    void anExpiredMissionIsRemovedFromTheDatabase() throws InterruptedException {
        acceptedSubscriber.onMissionAcceptedEvent(acceptedEvent(700L, "Mission_Courier", "Deliver cargo"));
        assertNotNull(missionManager.getMission(700L));

        subscriber.onMissionFailedEvent(failedEvent(700L, "Mission_Courier"));

        awaitTrue(() -> missionManager.getMission(700L) == null);
        assertNull(missionManager.getMission(700L));
    }

    @Test
    void failingAMissionWeNeverHeldExitsSilentlyWithoutException() {
        assertDoesNotThrow(() -> {
            subscriber.onMissionFailedEvent(failedEvent(999L, "Mission_Courier"));
            Thread.sleep(200);
        });
    }

    @Test
    void onlyTheFailedMissionIsRemoved() throws InterruptedException {
        acceptedSubscriber.onMissionAcceptedEvent(acceptedEvent(701L, "Mission_Courier", "Mission one"));
        acceptedSubscriber.onMissionAcceptedEvent(acceptedEvent(702L, "Mission_Delivery", "Mission two"));

        subscriber.onMissionFailedEvent(failedEvent(701L, "Mission_Courier"));

        awaitTrue(() -> missionManager.getMission(701L) == null);
        assertNull(missionManager.getMission(701L));
        assertNotNull(missionManager.getMission(702L));
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

    private static MissionFailedEvent failedEvent(long id, String name) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "MissionFailed");
        j.addProperty("MissionID", id);
        j.addProperty("Name", name);
        return new MissionFailedEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
