package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.EventBus;
import com.google.gson.JsonObject;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.*;
import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.Cypher;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Knowing which power the commander is pledged to, from journal lines in the order the game writes them.
 * <p>
 * The game says so at every load for a pledged commander and says nothing for one who is not, so the
 * cases are loads with and without the {@code Powerplay} line, and the three changes that can happen
 * between loads. Events go through a bus, as they do live and in the pre-scan.
 */
class PowerPlaySubscriberTest {

    private final EventBus bus = new EventBus();

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    PowerPlaySubscriberTest() {
        bus.register(new PowerPlaySubscriber());
    }

    @Test
    void aLoadWithAPowerplayLineIsAPledgeHoweverLongAgoItWasMade() {
        load(true, "Felicia Winters");

        RankAndProgressDto progress = progress();
        assertTrue(progress.isPledged());
        assertEquals("Felicia Winters", progress.getPledgedToPower());
        assertEquals(42, progress.getPowerRank());
    }

    @Test
    void aLoadWithoutOneIsNoPledgeWhateverTheLastLoadSaid() {
        load(true, "Felicia Winters");
        load(false, null);

        assertFalse(progress().isPledged(), "the game writes Powerplay at every load of a pledged commander");
        assertEquals(RankAndProgressDto.NOT_PLEDGED, progress().getPledgedToPower(),
                "known to be none, which is not the same as not known yet");
    }

    @Test
    void joiningLeavingAndDefectingMoveThePledgeBetweenLoads() {
        load(false, null);

        bus.post(new PowerplayJoinEvent(line("PowerplayJoin", "\"Power\": \"Aisling Duval\"")));
        assertEquals("Aisling Duval", progress().getPledgedToPower());

        bus.post(new PowerplayDefectEvent(line("PowerplayDefect",
                "\"FromPower\": \"Aisling Duval\", \"ToPower\": \"Zemina Torval\"")));
        assertEquals("Zemina Torval", progress().getPledgedToPower());
        assertEquals(0, progress().getMerrits(), "a defector starts over with the new power");

        bus.post(new PowerplayLeaveEvent(line("PowerplayLeave", "\"Power\": \"Zemina Torval\"")));
        assertFalse(progress().isPledged());
    }

    @Test
    void beforeAnyLoadThePledgeIsUnknownRatherThanNone() {
        RankAndProgressDto fresh = new RankAndProgressDto();

        assertEquals(RankAndProgressDto.PLEDGE_UNKNOWN, fresh.getPledgedToPower());
        assertFalse(fresh.isPledged());
    }

    /**
     * The opening of a game load as the journal writes it: Commander first, Powerplay after LoadGame and
     * Location when the commander is pledged.
     */
    private void load(boolean pledged, String power) {
        bus.post(new CommanderEvent(line("Commander", "\"FID\": \"F1234567\", \"Name\": \"Test\"")));
        if (pledged) {
            bus.post(new PowerplayEvent(line("Powerplay",
                    "\"Power\": \"" + power + "\", \"Rank\": 42, \"Merits\": 1200, \"TimePledged\": 31536000")));
        }
    }

    private static JsonObject line(String event, String fields) {
        return GsonFactory.getGson().fromJson(
                "{\"timestamp\": \"2026-10-25T20:00:00Z\", \"event\": \"" + event + "\", " + fields + "}", JsonObject.class);
    }

    private static RankAndProgressDto progress() {
        return PlayerSession.getInstance().getRankAndProgressDto();
    }
}
