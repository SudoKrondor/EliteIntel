package elite.intel.db.managers;

import com.google.gson.JsonObject;
import elite.intel.community.CrowdSet;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.MassacrePair;
import elite.intel.gameapi.missions.ResourceSiteProfile;
import elite.intel.util.Cypher;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The sets other commanders reported, merged into the commander's own pair search. Each case works far out on the
 * x axis, clear of the other ledger tests, and searches a small radius around its own provider.
 */
class CrowdMassacreSetsTest {

    private final HuntingGroundManager ledger = HuntingGroundManager.getInstance();
    private final CommunityManager community = CommunityManager.getInstance();
    private final GlobalSettingsManager settings = GlobalSettingsManager.getInstance();

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @AfterEach
    void reset() {
        settings.setShareMassacreSets(false);
        community.replaceCrowdSets(List.of());
    }

    @Test
    void aReportedSetIsOfferedOnlyWhileTheCommanderShares() {
        Coordinates here = at("Crowd Provider A", 80000);
        sites("Crowd Target A", 80005);
        community.replaceCrowdSets(List.of(set("Crowd Provider A", 80000, "Crowd Target A", 3, 2)));

        assertTrue(ledger.bestPairs(here, 10).isEmpty(), "not sharing, so nothing from others (give to get)");
        assertTrue(ledger.othersReportedNear(here, 10), "but VEGA can say that sets exist nearby");

        settings.setShareMassacreSets(true);
        MassacrePair pair = ledger.bestPairs(here, 10).getFirst();

        assertEquals("Crowd Target A", pair.targetSystem());
        assertEquals(3, pair.stackDepth());
        assertEquals(2, pair.reporters());
        assertTrue(pair.reportedByOthers());
    }

    @Test
    void aReportedSetIsHiddenUntilThisCommanderKnowsTheTargetHasResourceSites() {
        Coordinates here = at("Crowd Provider B", 81000);
        community.replaceCrowdSets(List.of(set("Crowd Provider B", 81000, "Crowd Target B", 2, 1)));
        settings.setShareMassacreSets(true);

        assertTrue(ledger.bestPairs(here, 10).isEmpty(),
                "the nightly job cannot check resource sites, so the commander's own ledger must vouch for them");

        sites("Crowd Target B", 81005);
        assertEquals(1, ledger.bestPairs(here, 10).size());
    }

    @Test
    void theCommandersOwnPairReplacesAReportOfTheSameTwoSystems() {
        Coordinates here = at("Crowd Provider C", 82000);
        sites("Crowd Target C", 82005);
        contract(98001, here, "C Union", "Crowd Target C", "C Cartel");
        community.replaceCrowdSets(List.of(set("Crowd Provider C", 82000, "Crowd Target C", 1, 5)));
        settings.setShareMassacreSets(true);

        List<MassacrePair> pairs = ledger.bestPairs(here, 10);

        assertEquals(1, pairs.size());
        assertFalse(pairs.getFirst().reportedByOthers(), "the commander has flown it, the report is someone else's word");
    }

    @Test
    void aDeeperReportedStackRanksAboveAShallowOwnPair() {
        Coordinates here = at("Crowd Provider D", 83000);
        sites("Crowd Target D", 83005);
        sites("Crowd Target D2", 83006);
        contract(98101, here, "D Union", "Crowd Target D", "D Cartel");
        community.replaceCrowdSets(List.of(set("Crowd Provider D", 83000, "Crowd Target D2", 4, 1)));
        settings.setShareMassacreSets(true);

        List<String> targets = ledger.bestPairs(here, 10).stream().map(MassacrePair::targetSystem).toList();

        assertEquals(List.of("Crowd Target D2", "Crowd Target D"), targets);
    }

    // ---------------------------------------------------------------- fixtures

    private static Coordinates at(String starSystem, double x) {
        return new Coordinates(starSystem, x, 0, 0);
    }

    private void sites(String starSystem, double x) {
        ledger.recordResourceSites(starSystem, null, at(starSystem, x), new ResourceSiteProfile(0, 2, 1, 1), "2026-11-01T00:00:00Z");
    }

    private static CrowdSet set(String provider, double x, String target, int depth, int reporters) {
        List<String> factions = java.util.stream.IntStream.range(0, depth).mapToObj(i -> provider + " Faction " + i).toList();
        return new CrowdSet(provider, x, 0, 0, factions, List.of("Some Dock"), target, target + " Raiders", depth, reporters, "2026-11-10");
    }

    private void contract(long missionId, Coordinates provider, String providerFaction, String target, String targetFaction) {
        String json = """
                {"timestamp":"2026-11-01T00:00:00Z","event":"MissionAccepted","Faction":"%s",
                 "Name":"Mission_Massacre","TargetType":"$MissionUtil_FactionTag_Pirate;",
                 "TargetFaction":"%s","KillCount":4,"DestinationSystem":"%s",
                 "Expiry":"2026-11-02T00:00:00Z","Reward":1000000,"MissionID":%d}
                """.formatted(providerFaction, targetFaction, target, missionId);
        ledger.recordContract(new MissionDto(new MissionAcceptedEvent(GsonFactory.getGson().fromJson(json, JsonObject.class))),
                provider, "Some Dock");
    }
}
