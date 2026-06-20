package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.managers.ReputationManager;
import elite.intel.gameapi.journal.events.ReputationEvent;
import elite.intel.gameapi.journal.subscribers.ReputationSubscriber;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReputationSubscriberTest {

    private final ReputationSubscriber subscriber = new ReputationSubscriber();
    private final ReputationManager reputationManager = ReputationManager.getInstance();

    @Test
    void reputationIsSavedAndRetrievableFromDb() {
        subscriber.onReputationEvent(reputationEvent(75.0, 50.0, 25.0, 10.0));

        ReputationEvent stored = reputationManager.get();
        assertNotNull(stored);
        assertEquals(75.0, stored.getEmpire(), 0.01);
        assertEquals(50.0, stored.getFederation(), 0.01);
        assertEquals(25.0, stored.getIndependent(), 0.01);
    }

    @Test
    void reputationIsOverwrittenOnNextEvent() {
        subscriber.onReputationEvent(reputationEvent(10.0, 20.0, 30.0, 40.0));
        subscriber.onReputationEvent(reputationEvent(90.0, 80.0, 70.0, 60.0));

        ReputationEvent stored = reputationManager.get();
        assertEquals(90.0, stored.getEmpire(), 0.01);
        assertEquals(80.0, stored.getFederation(), 0.01);
    }

    private static ReputationEvent reputationEvent(double empire, double federation, double independent, double alliance) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Reputation");
        j.addProperty("Empire", empire);
        j.addProperty("Federation", federation);
        j.addProperty("Independent", independent);
        j.addProperty("Alliance", alliance);
        return new ReputationEvent(j);
    }
}
