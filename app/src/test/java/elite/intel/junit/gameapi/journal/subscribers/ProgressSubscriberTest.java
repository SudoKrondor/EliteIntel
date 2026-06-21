package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.ProgressEvent;
import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.gameapi.journal.subscribers.ProgressSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressSubscriberTest {

    private final ProgressSubscriber subscriber = new ProgressSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

    @Test
    void combatProgressIsStoredInDto() {
        subscriber.onProgressEvent(progressEvent(67, 0, 0, 0, 0, 0, 0, 0));

        RankAndProgressDto rp = session.getRankAndProgressDto();
        assertEquals(67, rp.getCombatProgressToNextRankInPercent());
    }

    @Test
    void empireAndFederationMilitaryProgressAreStoredIndependently() {
        subscriber.onProgressEvent(progressEvent(0, 0, 0, 0, 0, 45, 80, 0));

        RankAndProgressDto rp = session.getRankAndProgressDto();
        assertEquals(45, rp.getEmpireMilitaryRankProgressToNextRankInPercent());
        assertEquals(80, rp.getFederationMilitaryRankProgressToNextRankInPercent());
    }

    @Test
    void explorationProgressIsStoredInDto() {
        subscriber.onProgressEvent(progressEvent(0, 0, 33, 0, 0, 0, 0, 0));

        RankAndProgressDto rp = session.getRankAndProgressDto();
        assertEquals(33, rp.getExplorationProgressToNextRankInPercent());
    }

    private static ProgressEvent progressEvent(int combat, int trade, int explore, int soldier,
                                               int exobiologist, int empire, int federation, int cqc) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Progress");
        j.addProperty("Combat", combat);
        j.addProperty("Trade", trade);
        j.addProperty("Explore", explore);
        j.addProperty("Soldier", soldier);
        j.addProperty("Exobiologist", exobiologist);
        j.addProperty("Empire", empire);
        j.addProperty("Federation", federation);
        j.addProperty("CQC", cqc);
        return new ProgressEvent(j);
    }
}
