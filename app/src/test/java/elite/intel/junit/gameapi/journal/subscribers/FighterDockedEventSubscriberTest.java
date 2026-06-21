package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.DockFighterEvent;
import elite.intel.gameapi.journal.subscribers.FighterDockedEventSubscriber;
import elite.intel.session.Status;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;

class FighterDockedEventSubscriberTest {

    private final FighterDockedEventSubscriber subscriber = new FighterDockedEventSubscriber();
    private final Status status = Status.getInstance();

    @Test
    void dockFighterClearsFighterOutFlag() {
        status.setFighterOut(true);

        subscriber.onFighterDockedEvent(dockFighterEvent());

        assertFalse(status.isFighterOut());
    }

    @Test
    void dockFighterIsIdempotentWhenFighterAlreadyDocked() {
        status.setFighterOut(false);

        subscriber.onFighterDockedEvent(dockFighterEvent());

        assertFalse(status.isFighterOut());
    }

    private static DockFighterEvent dockFighterEvent() {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "DockFighter");
        j.addProperty("ID", 1);
        return new DockFighterEvent(j);
    }
}
