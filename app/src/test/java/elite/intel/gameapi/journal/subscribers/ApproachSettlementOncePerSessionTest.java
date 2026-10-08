package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.ApproachSettlementEvent;
import elite.intel.gameapi.journal.events.FileheaderEvent;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A commander working a settlement for materials lands there again and again. VEGA describes it on
 * the first approach of a game session and stays quiet on the returns, until a new journal file
 * starts a new session.
 */
class ApproachSettlementOncePerSessionTest {

    private static ApproachSettlementEvent approach(String name, long systemAddress) {
        String json = """
                {"timestamp":"2026-10-07T12:00:00Z","event":"ApproachSettlement","Name":"%s",
                 "MarketID":3908321536,"SystemAddress":%d,"BodyID":31}
                """.formatted(name, systemAddress);
        return new ApproachSettlementEvent(GsonFactory.getGson().fromJson(json, JsonObject.class));
    }

    private static FileheaderEvent header(int part) {
        String json = """
                {"timestamp":"2026-10-07T12:00:00Z","event":"Fileheader","part":%d,
                 "language":"English/UK","Odyssey":true,"gameversion":"4.1","build":"r1"}
                """.formatted(part);
        return new FileheaderEvent(GsonFactory.getGson().fromJson(json, JsonObject.class));
    }

    @Test
    @DisplayName("a settlement is announced once per session, not on every return")
    void returnsAreSilent() {
        ApproachSettlementSubscriber subscriber = new ApproachSettlementSubscriber();

        assertTrue(subscriber.firstApproachThisSession(approach("Bogacki Prospecting Facility", 1L)));
        assertFalse(subscriber.firstApproachThisSession(approach("Bogacki Prospecting Facility", 1L)));
        assertTrue(subscriber.firstApproachThisSession(approach("Bogacki Prospecting Facility", 2L)),
                "the same name in another system is another settlement");
        assertTrue(subscriber.firstApproachThisSession(approach("The Beach", 1L)));
    }

    @Test
    @DisplayName("a new game session announces the settlement again; a continued file does not")
    void newSessionForgets() {
        ApproachSettlementSubscriber subscriber = new ApproachSettlementSubscriber();
        subscriber.firstApproachThisSession(approach("The Beach", 1L));

        subscriber.onFileheaderEvent(header(2));
        assertFalse(subscriber.firstApproachThisSession(approach("The Beach", 1L)));

        subscriber.onFileheaderEvent(header(1));
        assertTrue(subscriber.firstApproachThisSession(approach("The Beach", 1L)));
    }
}
