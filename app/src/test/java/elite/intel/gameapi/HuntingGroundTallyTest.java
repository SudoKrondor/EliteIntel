package elite.intel.gameapi;

import com.google.gson.JsonParser;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.missions.ResourceSiteProfile;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The tally's own rules, the ones the folder backfill ({@link HuntingGroundJournalScannerTest}) never reaches: how it
 * picks the event out of a raw line, and the import's journals whose commander is not known, which teach the galaxy's
 * resource sites but file no contract under anyone.
 */
class HuntingGroundTallyTest {

    private static final String LOW = "$MULTIPLAYER_SCENARIO77_TITLE;";

    private final HuntingGroundManager ledger = HuntingGroundManager.getInstance();

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @Test
    void theEventIsReadOutOfTheRawLine() {
        assertEquals("FSDJump", HuntingGroundTally.eventNameOf("{ \"timestamp\":\"t\", \"event\":\"FSDJump\", \"x\":1 }"));
        assertNull(HuntingGroundTally.eventNameOf("{ \"timestamp\":\"t\" }"), "a line with no event names none");
        assertNull(HuntingGroundTally.eventNameOf("{ \"event\":\"FSDJu"), "a line cut off mid-name names none");
    }

    @Test
    void onlyTheEventsItLearnsFromAreWanted() {
        HuntingGroundTally tally = new HuntingGroundTally(ledger, true);

        for (String wanted : List.of("FSDJump", "Location", "CarrierJump", "Docked", "Undocked",
                "FSSSignalDiscovered", "MissionAccepted", "MissionCompleted")) {
            assertTrue(tally.wants(wanted), wanted);
        }
        assertFalse(tally.wants("Scan"));
        assertFalse(tally.wants("Music"));
    }

    @Test
    void aKnownCommandersContractIsFiled() {
        HuntingGroundTally tally = new HuntingGroundTally(ledger, true);

        fly(tally, "Tally Ground Known", 9101, 900, "Tally Provider Known", 9102, 901, 8101);

        assertEquals(1, ledger.bestPairs(at("Tally Provider Known", 901), 1).size());
    }

    @Test
    void aJournalWithNoKnownCommanderTeachesTheSitesButFilesNoContract() {
        HuntingGroundTally tally = new HuntingGroundTally(ledger, false);

        fly(tally, "Tally Ground Unknown", 9201, 950, "Tally Provider Unknown", 9202, 951, 8201);

        assertEquals(new ResourceSiteProfile(0, 2, 0, 0), ledger.sitesIn("Tally Ground Unknown"),
                "resource sites are facts about the galaxy and belong to everyone");
        assertTrue(ledger.bestPairs(at("Tally Provider Unknown", 951), 1).isEmpty(),
                "a contract has to be filed under the commander who took it, and nobody is known");
    }

    /**
     * Sweeps two low sites in the ground, docks at the provider and takes a pirate massacre contract against the
     * ground, then writes the sites.
     */
    private static void fly(HuntingGroundTally tally, String ground, long groundAddress, double groundX,
                            String provider, long providerAddress, double providerX, long missionId) {
        feed(tally, arrival("2026-09-10T10:00:00Z", ground, groundAddress, groundX));
        feed(tally, signal("2026-09-10T10:00:05Z", groundAddress));
        feed(tally, signal("2026-09-10T10:00:05Z", groundAddress));
        feed(tally, arrival("2026-09-10T11:00:00Z", provider, providerAddress, providerX));
        feed(tally, "{ \"timestamp\":\"2026-09-10T11:05:00Z\", \"event\":\"Docked\", \"StationName\":\"Tally Dock\", "
                + "\"MarketID\":1 }");
        feed(tally, ("{ \"timestamp\":\"2026-09-10T11:06:00Z\", \"event\":\"MissionAccepted\", \"Faction\":\"Tally Union\", "
                + "\"Name\":\"Mission_Massacre\", \"TargetType\":\"$MissionUtil_FactionTag_Pirate;\", "
                + "\"TargetFaction\":\"Tally Cartel\", \"KillCount\":4, \"DestinationSystem\":\"%s\", \"Wing\":false, "
                + "\"Reward\":1000000, \"MissionID\":%d }").formatted(ground, missionId));
        tally.persistResourceSites();
    }

    private static void feed(HuntingGroundTally tally, String line) {
        String event = HuntingGroundTally.eventNameOf(line);
        assertTrue(tally.wants(event), event);
        tally.accept(event, JsonParser.parseString(line).getAsJsonObject());
    }

    private static String arrival(String timestamp, String starSystem, long address, double x) {
        return ("{ \"timestamp\":\"%s\", \"event\":\"FSDJump\", \"StarSystem\":\"%s\", "
                + "\"SystemAddress\":%d, \"StarPos\":[%s,0.0,0.0] }").formatted(timestamp, starSystem, address, x);
    }

    private static String signal(String timestamp, long address) {
        return ("{ \"timestamp\":\"%s\", \"event\":\"FSSSignalDiscovered\", \"SystemAddress\":%d, "
                + "\"SignalName\":\"%s\", \"SignalType\":\"ResourceExtraction\" }").formatted(timestamp, address, LOW);
    }

    private static Coordinates at(String starSystem, double x) {
        return new Coordinates(starSystem, x, 0, 0);
    }
}
