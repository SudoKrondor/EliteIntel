package elite.intel.gameapi.signals;

import com.google.gson.reflect.TypeToken;
import elite.intel.gameapi.journal.events.FSDJumpEvent;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Which entry of an arrival's Conflicts block is the war worth flying to. The block is read the same way
 * off the commander's own journal and off the EDDN relay, so this is the one place the rule is pinned.
 */
class ActiveWarTest {

    @Test
    void anElectionListedFirstDoesNotHideTheWarBehindIt() {
        ActiveWar war = ActiveWar.firstIn(conflicts("""
                [{"WarType": "election", "Status": "active", "Faction1": {"Name": "Voters"}, "Faction2": {"Name": "Others"}},
                 {"WarType": "civilwar", "Status": "active", "Faction1": {"Name": "Sirius Corporation"}, "Faction2": {"Name": "RSR"}}]
                """));

        assertNotNull(war);
        assertEquals("civilwar", war.warType());
        assertEquals("Sirius Corporation", war.faction1());
        assertEquals("RSR", war.faction2());
    }

    @Test
    void aPendingWarHasNoZonesYet() {
        assertNull(ActiveWar.firstIn(conflicts("""
                [{"WarType": "war", "Status": "pending", "Faction1": {"Name": "A"}, "Faction2": {"Name": "B"}}]
                """)));
    }

    @Test
    void aSideWithoutANameIsNotAWarToFlyTo() {
        assertNull(ActiveWar.firstIn(conflicts("""
                [{"WarType": "war", "Status": "active", "Faction1": {"Name": "A"}}]
                """)));
    }

    @Test
    void noBlockAndAnEmptyBlockBothMeanNoWar() {
        assertNull(ActiveWar.firstIn(null));
        assertNull(ActiveWar.firstIn(List.of()));
    }

    private static List<FSDJumpEvent.Conflict> conflicts(String json) {
        return GsonFactory.getGson().fromJson(json, new TypeToken<List<FSDJumpEvent.Conflict>>() {
        }.getType());
    }
}
