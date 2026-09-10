package elite.intel.db.managers;

import com.google.gson.JsonObject;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.HuntingGround;
import elite.intel.gameapi.missions.MassacrePair;
import elite.intel.gameapi.missions.ResourceSiteProfile;
import elite.intel.util.Cypher;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The ledger that replaced the crowd-sourced pairing service.
 * <p>
 * A pair is two independent findings joined: a contract taken in one system against another, and
 * resource extraction sites seen in that other system. The join is the whole feature - a contract
 * against a system with no sites is a contract with nowhere to fight it, and the service that used to
 * answer this question handed those out routinely.
 * <p>
 * Each case works in its own corner of the galaxy and searches a one light year radius around it, so
 * the tests share the database without seeing each other's systems.
 */
class HuntingGroundLedgerTest {

    private final HuntingGroundManager ledger = HuntingGroundManager.getInstance();

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @Test
    void aContractAgainstASystemWithNoResourceSitesIsNotAPair() {
        Coordinates provider = at("Ledger Provider A", 1000);
        sites("Ledger Target A", 1010, new ResourceSiteProfile(0, 4, 2, 2));
        contract(9001, provider, "First Union", "Ledger Target A", "A Cartel");
        contract(9002, provider, "First Union", "Ledger Barren A", "Barren Cartel");

        List<String> targets = ledger.bestPairs(provider, 1).stream().map(MassacrePair::targetSystem).toList();

        assertEquals(List.of("Ledger Target A"), targets,
                "the commander has never seen a resource site in Ledger Barren A, so there is nowhere "
                        + "to do the killing and it is not an answer");
    }

    @Test
    void stackDepthCountsFactionsAndNotContracts() {
        Coordinates provider = at("Ledger Provider B", 2000);
        sites("Ledger Target B", 2010, new ResourceSiteProfile(0, 2, 0, 2));
        contract(9101, provider, "Union of B", "Ledger Target B", "B Cartel");
        contract(9102, provider, "Union of B", "Ledger Target B", "B Cartel");
        contract(9103, provider, "B Defence Force", "Ledger Target B", "B Cartel");

        assertEquals(2, onlyPair(provider).stackDepth(),
                "a second contract from a faction already in the pile queues behind its first, so it "
                        + "does not deepen the stack");
    }

    @Test
    void theStationsWalkedAreRememberedAlongWithTheFactions() {
        Coordinates provider = at("Ledger Provider C", 3000);
        sites("Ledger Target C", 3010, new ResourceSiteProfile(1, 1, 0, 0));
        contract(9201, provider, "Union of C", "Ledger Target C", "C Cartel", "Nicholson Dock");
        contract(9202, provider, "C Services", "Ledger Target C", "C Cartel", "Palmer Dock");

        MassacrePair pair = onlyPair(provider);

        assertEquals(List.of("Nicholson Dock", "Palmer Dock"), pair.stations().stream().sorted().toList());
        assertEquals(List.of("C Services", "Union of C"), pair.providerFactions().stream().sorted().toList());
    }

    @Test
    void theSameContractReadTwiceIsStillOneContract() {
        Coordinates provider = at("Ledger Provider D", 4000);
        sites("Ledger Target D", 4010, new ResourceSiteProfile(0, 1, 0, 0));

        assertTrue(contract(9301, provider, "Union of D", "Ledger Target D", "D Cartel"));

        int onFile = ledger.knowledge().contracts();
        assertFalse(contract(9301, provider, "Union of D", "Ledger Target D", "D Cartel"),
                "the game's own MissionID is the key, which is what makes re-reading a journal free");
        assertEquals(onFile, ledger.knowledge().contracts(), "and nothing was added the second time");
    }

    @Test
    void gradeCountsRiseToTheFullestSweepAndNeverFall() {
        sites("Ledger Ground E", 5000, new ResourceSiteProfile(0, 4, 2, 2));
        sites("Ledger Ground E", 5000, new ResourceSiteProfile(0, 1, 0, 0));

        assertEquals(new ResourceSiteProfile(0, 4, 2, 2), ledger.sitesIn("Ledger Ground E"),
                "a sweep flown at the edge of scanner range reports fewer sites than the system holds");
    }

    @Test
    void theBestFightingWithinRangeIsOfferedAheadOfTheNearestRing() {
        sites("Ledger Ground F Near", 6001, new ResourceSiteProfile(0, 5, 0, 0));
        sites("Ledger Ground F Far", 6002, new ResourceSiteProfile(0, 0, 0, 3));

        List<HuntingGround> grounds = ledger.bestHuntingGrounds(at("Ledger Here F", 6000), 5);

        assertEquals("Ledger Ground F Far", grounds.getFirst().starSystem(),
                "within a range the commander named, the question is where the good fighting is");
    }

    @Test
    void forgettingAGroundDropsItsPairsAndSurvivesTheNextSighting() {
        Coordinates provider = at("Ledger Provider G", 7000);
        sites("Ledger Target G", 7010, new ResourceSiteProfile(0, 2, 0, 1));
        contract(9401, provider, "Union of G", "Ledger Target G", "G Cartel");

        HuntingGroundManager.ForgetResult result = ledger.forget("Ledger Target G");

        assertTrue(result.wasKnown());
        assertEquals(1, result.contractsForgotten());
        assertTrue(ledger.bestPairs(provider, 1).isEmpty());

        sites("Ledger Target G", 7010, new ResourceSiteProfile(0, 2, 0, 1));

        assertNull(ledger.sitesIn("Ledger Target G"),
                "flying back through must not resurrect a ground the commander judged not worth it");
        assertTrue(ledger.bestHuntingGrounds(at("Ledger Target G", 7010), 1).isEmpty(),
                "and it must not come back as a bounty-hunting suggestion either");
    }

    @Test
    void forgettingASystemNobodyRecordedSaysSo() {
        assertFalse(ledger.forget("Ledger Never Visited").wasKnown());
    }

    // ---------------------------------------------------------------- fixtures

    private static Coordinates at(String starSystem, double x) {
        return new Coordinates(starSystem, x, 0, 0);
    }

    private void sites(String starSystem, double x, ResourceSiteProfile profile) {
        ledger.recordResourceSites(starSystem, null, at(starSystem, x), profile, "2026-09-01T00:00:00Z");
    }

    private boolean contract(long missionId, Coordinates provider, String providerFaction,
                             String target, String targetFaction) {
        return contract(missionId, provider, providerFaction, target, targetFaction, "Some Dock");
    }

    private boolean contract(long missionId, Coordinates provider, String providerFaction,
                             String target, String targetFaction, String station) {
        String json = """
                {"timestamp":"2026-09-01T00:00:00Z","event":"MissionAccepted","Faction":"%s",
                 "Name":"Mission_Massacre","LocalisedName":"Kill %s faction Pirates",
                 "TargetType":"$MissionUtil_FactionTag_Pirate;","TargetType_Localised":"Pirates",
                 "TargetFaction":"%s","KillCount":4,"DestinationSystem":"%s",
                 "Expiry":"2026-09-02T00:00:00Z","Wing":false,"Reward":1000000,"MissionID":%d}
                """.formatted(providerFaction, targetFaction, targetFaction, target, missionId);

        MissionAcceptedEvent event =
                new MissionAcceptedEvent(GsonFactory.getGson().fromJson(json, JsonObject.class));
        return ledger.recordContract(new MissionDto(event), provider, station);
    }

    private MassacrePair onlyPair(Coordinates provider) {
        List<MassacrePair> pairs = ledger.bestPairs(provider, 1);
        assertEquals(1, pairs.size(), "one provider and one target faction is one pairing");
        return pairs.getFirst();
    }
}
