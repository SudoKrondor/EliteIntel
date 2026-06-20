package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.CommanderEvent;
import elite.intel.gameapi.journal.subscribers.CommanderEventSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommanderEventSubscriberTest {

    private final CommanderEventSubscriber subscriber = new CommanderEventSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

    @Test
    void commanderNameIsStoredInPlayerSession() {
        subscriber.onEvent(commanderEvent("Alex Storm"));

        assertEquals("Alex Storm", session.getInGameName());
    }

    @Test
    void commanderNameOverwritesPreviousValue() {
        subscriber.onEvent(commanderEvent("Old Name"));
        subscriber.onEvent(commanderEvent("New Name"));

        assertEquals("New Name", session.getInGameName());
    }

    private static CommanderEvent commanderEvent(String name) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Commander");
        j.addProperty("FID", "F1234567");
        j.addProperty("Name", name);
        return new CommanderEvent(j);
    }
}
