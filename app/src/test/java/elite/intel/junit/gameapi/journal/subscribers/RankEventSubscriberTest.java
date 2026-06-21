package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.RankEvent;
import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.gameapi.journal.subscribers.RankEventSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RankEventSubscriberTest {

    private final RankEventSubscriber subscriber = new RankEventSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

    @Test
    void combatRankIsConvertedToStringAndStored() {
        // Combat rank 3 = "Competent"
        subscriber.onRankEvent(rankEvent(3, 0, 0, 0, 0, 0, 0, 0));

        RankAndProgressDto rp = session.getRankAndProgressDto();
        assertEquals("Competent", rp.getCombatRank());
    }

    @Test
    void militaryRanksAreStoredAndHighestIsTracked() {
        // Empire=5 ("Knight"), Federation=2 ("Midshipman") → highest is Imperial "Knight"
        subscriber.onRankEvent(rankEvent(0, 0, 0, 0, 0, 5, 2, 0));

        assertEquals("Knight", session.getPlayerHighestMilitaryRank());
        RankAndProgressDto rp = session.getRankAndProgressDto();
        assertEquals("Knight", rp.getMilitaryRankEmpire());
    }

    @Test
    void explorationRankIsStoredInDto() {
        // Explore rank 4 = "Surveyor" - let's check Ranks map for exploration
        subscriber.onRankEvent(rankEvent(0, 0, 6, 0, 0, 0, 0, 0));

        RankAndProgressDto rp = session.getRankAndProgressDto();
        assertNotNull(rp.getExplorationRank());
        assertNotEquals("unknown", rp.getExplorationRank());
    }

    private static RankEvent rankEvent(int combat, int trade, int explore, int soldier,
                                       int exobiologist, int empire, int federation, int cqc) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Rank");
        j.addProperty("Combat", combat);
        j.addProperty("Trade", trade);
        j.addProperty("Explore", explore);
        j.addProperty("Soldier", soldier);
        j.addProperty("Exobiologist", exobiologist);
        j.addProperty("Empire", empire);
        j.addProperty("Federation", federation);
        j.addProperty("CQC", cqc);
        return new RankEvent(j);
    }
}
