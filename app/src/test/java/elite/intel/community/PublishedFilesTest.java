package elite.intel.community;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import elite.intel.community.PublishedFiles.SetsDownload;
import elite.intel.community.PublishedFiles.Switches;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class PublishedFilesTest {

    /**
     * The shape the nightly job writes: one checked set and one it could not check against the galaxy index.
     */
    static final String SETS = """
            {"format":1,"indexDate":"2026-03-02","contributors":4,"sets":[
             {"provider":{"system":"Bean","systemAddress":1,"coords":[-83.687,-41.812,-100.344],
                          "factions":["Earls of Bean","Bean Organisation"],"stations":["Bruce Dock"]},
              "target":{"system":"Bellite","systemAddress":2,"coords":[-79.719,-35.531,-95.937],"factions":["Bellite Raiders"]},
              "distanceLy":8.64,"stackDepth":2,"reporters":3,"lastSeen":"2026-11-10","indexed":true},
             {"provider":{"system":"New Colony","factions":["Settlers"]},
              "target":{"system":"Bellite","factions":["Bellite Raiders"]},
              "stackDepth":1,"reporters":1,"indexed":false}]}
            """;

    @RegisterExtension
    WireMockExtension wm = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private PublishedFiles files;

    @BeforeEach
    void setUp() {
        files = new PublishedFiles(URI.create(wm.baseUrl() + "/data/massacre-sets.json"),
                URI.create(wm.baseUrl() + "/data/config.json"));
    }

    @Test
    void onlySetsTheNightlyJobCheckedAreKept() throws IOException {
        PublishedFiles.SetsFile file = PublishedFiles.parseSets(SETS);
        List<CrowdSet> sets = file.sets();

        assertEquals(4, file.contributors());
        assertEquals(1, sets.size());
        CrowdSet bean = sets.getFirst();
        assertEquals("Bean", bean.providerSystem());
        assertEquals(-83.687, bean.x());
        assertEquals(List.of("Earls of Bean", "Bean Organisation"), bean.providerFactions());
        assertEquals("Bellite Raiders", bean.targetFaction());
        assertEquals(3, bean.reporters());
    }

    @Test
    void aFormatThisBuildDoesNotKnowIsRefused() {
        assertThrows(IOException.class, () -> PublishedFiles.parseSets("{\"format\":2,\"sets\":[]}"));
        assertThrows(IOException.class, () -> PublishedFiles.parseSets("{\"format\":1,\"sets\":[{\"indexed\":true}]}"));
    }

    @Test
    void anUnchangedFileIsNotDownloadedAgain() throws Exception {
        wm.stubFor(get("/data/massacre-sets.json").withHeader("If-None-Match", equalTo("\"abc\""))
                .willReturn(aResponse().withStatus(304)));
        wm.stubFor(get("/data/massacre-sets.json").withHeader("If-None-Match", absent())
                .willReturn(okJson(SETS).withHeader("ETag", "\"abc\"")));

        SetsDownload first = files.downloadSets(null);
        SetsDownload again = files.downloadSets(first.etag());

        assertEquals(1, first.sets().size());
        assertEquals("\"abc\"", first.etag());
        assertNull(again.sets());
    }

    @Test
    void noSwitchFileMeansEverythingIsOn() throws Exception {
        wm.stubFor(get("/data/config.json").willReturn(aResponse().withStatus(404)));

        assertEquals(Switches.DEFAULT, files.switches());
    }

    @Test
    void aSwitchTurnedOffIsHonouredAndAMissingOneIsOn() throws Exception {
        wm.stubFor(get("/data/config.json").willReturn(okJson("{\"reportPairs\":false}")));

        assertEquals(new Switches(true, false), files.switches());
    }

    @Test
    void anUnreadableSwitchFileTurnsNothingBackOn() {
        wm.stubFor(get("/data/config.json").willReturn(okJson("{not json")));

        assertThrows(IOException.class, files::switches);
    }
}
