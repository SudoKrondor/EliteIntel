package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.LaunchFighterEvent;
import elite.intel.gameapi.journal.subscribers.LaunchFighterEventSubscriber;
import elite.intel.session.Status;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchFighterEventSubscriberTest {

    private final LaunchFighterEventSubscriber subscriber = new LaunchFighterEventSubscriber();
    private final Status status = Status.getInstance();

    @Test
    void launchFighterSetsFighterOutTrue() {
        status.setFighterOut(false);

        subscriber.onLaunchFighterEvent(launchFighterEvent(true));

        assertTrue(status.isFighterOut());
    }

    @Test
    void launchFighterDisablesLoadoutAnnouncement() {
        status.setOkToAnnounceLoadout(true);

        subscriber.onLaunchFighterEvent(launchFighterEvent(true));

        assertFalse(status.isOkToAnnounceLoadout());
    }

    private static LaunchFighterEvent launchFighterEvent(boolean playerControlled) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "LaunchFighter");
        j.addProperty("Loadout", "starter");
        j.addProperty("ID", 1);
        j.addProperty("PlayerControlled", playerControlled);
        return new LaunchFighterEvent(j);
    }
}
