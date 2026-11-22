package elite.intel.community;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.gson.JsonObject;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.CommunityManager;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.ResourceSiteProfile;
import elite.intel.util.Cypher;
import elite.intel.util.json.GsonFactory;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

/**
 * One sync pass against a local stub standing in for both Firebase and the website, over the in-memory database.
 */
class CommunitySyncTest {

    private static final String COMMIT = "/v1/projects/elite-intel-firebase/databases/(default)/documents:commit";

    @RegisterExtension
    WireMockExtension wm = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private final CommunityManager store = CommunityManager.getInstance();
    private final GlobalSettingsManager settings = GlobalSettingsManager.getInstance();
    private CommunitySync sync;

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        HuntingGroundManager ledger = HuntingGroundManager.getInstance();
        Coordinates provider = new Coordinates("Sync Provider", 70000, 0, 0);
        ledger.recordResourceSites("Sync Target", null, new Coordinates("Sync Target", 70005, 0, 0),
                new ResourceSiteProfile(1, 0, 2, 0), "2026-11-01T00:00:00Z");
        String json = """
                {"timestamp":"2026-11-01T00:00:00Z","event":"MissionAccepted","Faction":"Sync Union",
                 "Name":"Mission_Massacre","TargetType":"$MissionUtil_FactionTag_Pirate;",
                 "TargetFaction":"Sync Cartel","KillCount":4,"DestinationSystem":"Sync Target",
                 "Expiry":"2026-11-02T00:00:00Z","Reward":1000000,"MissionID":97001}
                """;
        ledger.recordContract(new MissionDto(new MissionAcceptedEvent(GsonFactory.getGson().fromJson(json, JsonObject.class))),
                provider, "Sync Dock");
    }

    @BeforeEach
    void setUp() {
        try (Handle h = Database.init()) {
            h.execute("UPDATE community_sync SET uid = NULL, refreshToken = NULL, lastPingAt = NULL, lastReportAt = NULL,"
                    + " lastReportDigest = NULL, lastDownloadAt = NULL, setsEtag = NULL, contributors = NULL WHERE id = 1");
        }
        URI base = URI.create(wm.baseUrl() + "/");
        sync = new CommunitySync(store, settings, new FirebaseClient(base, base, base),
                new PublishedFiles(URI.create(wm.baseUrl() + "/data/massacre-sets.json"),
                        URI.create(wm.baseUrl() + "/data/config.json")));
        wm.stubFor(post(urlPathEqualTo("/v1/accounts:signUp")).willReturn(okJson("""
                {"localId":"uid-sync","idToken":"id-sync","refreshToken":"refresh-sync","expiresIn":"3600"}""")));
        wm.stubFor(post(urlPathEqualTo("/v1/token")).willReturn(okJson("""
                {"user_id":"uid-sync","id_token":"id-sync-2","refresh_token":"refresh-sync","expires_in":"3600"}""")));
        wm.stubFor(post(urlPathEqualTo(COMMIT)).willReturn(okJson("{}")));
        wm.stubFor(get("/data/config.json").willReturn(aResponse().withStatus(404)));
        wm.stubFor(get("/data/massacre-sets.json").willReturn(okJson(PublishedFilesTest.SETS)));
    }

    @AfterEach
    void stopSharing() {
        settings.setShareMassacreSets(false);
    }

    @Test
    void withoutSharingOnlyTheStatsPingGoesOutAndTheSetsStillComeIn() {
        settings.setShareMassacreSets(false);

        sync.runOnce(Instant.now());

        wm.verify(1, postRequestedFor(urlPathEqualTo(COMMIT)));
        wm.verify(postRequestedFor(urlPathEqualTo(COMMIT)).withRequestBody(containing("/documents/installs/uid-sync")));
        assertEquals(1, store.crowdSetCount());
        assertEquals(4, store.state().contributors());
        assertEquals("uid-sync", store.state().uid());
        assertEquals("refresh-sync", store.refreshToken());
    }

    @Test
    void sharingUploadsTheLedgerOnceAndNotAgainUntilItChanges() {
        settings.setShareMassacreSets(true);
        Instant now = Instant.now();

        sync.runOnce(now);
        sync.runOnce(now.plus(Duration.ofDays(2)));

        wm.verify(1, postRequestedFor(urlPathEqualTo(COMMIT)).withRequestBody(containing("/documents/reports/uid-sync")));
        wm.verify(postRequestedFor(urlPathEqualTo(COMMIT)).withRequestBody(containing("Sync Provider")));
        assertNotNull(store.state().lastReportAt());
    }

    @Test
    void turningSharingOffWithdrawsTheReport() {
        settings.setShareMassacreSets(true);
        Instant now = Instant.now();
        sync.runOnce(now);

        settings.setShareMassacreSets(false);
        sync.runOnce(now.plusSeconds(60));

        wm.verify(postRequestedFor(urlPathEqualTo(COMMIT))
                .withRequestBody(matchingJsonPath("$.writes[0].delete", containing("/documents/reports/uid-sync"))));
        assertNull(store.state().lastReportAt());
    }

    @Test
    void theMaintainersSwitchStopsTheUpload() {
        wm.stubFor(get("/data/config.json").willReturn(okJson("{\"reportPairs\":false}")));
        settings.setShareMassacreSets(true);

        sync.runOnce(Instant.now());

        wm.verify(0, postRequestedFor(urlPathEqualTo(COMMIT)).withRequestBody(containing("/documents/reports/")));
    }

    @Test
    void aPingTheRulesRefuseCountsAsDoneForTheDay() {
        wm.stubFor(post(urlPathEqualTo(COMMIT)).willReturn(aResponse().withStatus(403)));

        sync.runOnce(Instant.now());

        assertNotNull(store.state().lastPingAt());
    }
}
