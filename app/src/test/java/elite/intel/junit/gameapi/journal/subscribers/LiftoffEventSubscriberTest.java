package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.LiftoffEvent;
import elite.intel.gameapi.journal.subscribers.LiftoffEventSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiftoffEventSubscriberTest {

    private final LiftoffEventSubscriber subscriber = new LiftoffEventSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

    @Test
    void playerControlledLiftoffSetsAutoDepartedFalse() {
        session.setShipAutoDeparted(true);

        subscriber.onLiftoffEvent(liftoffEvent(true));

        assertFalse(session.isShipAutoDeparted());
    }

    @Test
    void npcControlledLiftoffSetsAutoDepartedTrue() {
        session.setShipAutoDeparted(false);

        subscriber.onLiftoffEvent(liftoffEvent(false));

        assertTrue(session.isShipAutoDeparted());
    }

    private static LiftoffEvent liftoffEvent(boolean playerControlled) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Liftoff");
        j.addProperty("PlayerControlled", playerControlled);
        j.addProperty("StarSystem", "Sol");
        j.addProperty("SystemAddress", 10477373803L);
        j.addProperty("Body", "Earth");
        j.addProperty("BodyID", 3);
        j.addProperty("OnStation", false);
        j.addProperty("OnPlanet", true);
        return new LiftoffEvent(j);
    }
}
