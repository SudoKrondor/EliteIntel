package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.vega.VegaNarrator;
import elite.intel.ai.brain.vega.VegaRuntimeGraph;
import elite.intel.ai.brain.vega.VegaRuntimeTestSupport;
import elite.intel.gameapi.journal.events.ScanOrganicEvent;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The second sample of an organism is only an acknowledgement, and it is spoken as written - never handed to the
 * model, which given the chance added a made-up list of remaining genus ("Fungivora, Fungivora, Fungivora...").
 * <p>
 * Waits on the narrator call itself rather than on a fixed sleep, so it is safe on a slow CI runner.
 */
class ScanOrganicSecondSampleTest {

    private final CapturingNarrator narrator = new CapturingNarrator();
    private final PlayerSession session = PlayerSession.getInstance();
    private Boolean discoveryAnnouncementsBefore;
    private VegaRuntimeGraph runtimeGraph;

    @BeforeEach
    void setUp() {
        discoveryAnnouncementsBefore = session.isDiscoveryAnnouncementOn();
        session.setDiscoveryAnnouncementOn(true);
        runtimeGraph = VegaRuntimeTestSupport.installNarrator(narrator);
    }

    @AfterEach
    void tearDown() {
        VegaRuntimeTestSupport.uninstall(runtimeGraph);
        session.setDiscoveryAnnouncementOn(discoveryAnnouncementsBefore);
    }

    @Test
    void theSecondSampleIsSpokenAsWrittenWithoutTheModel() throws InterruptedException {
        new ScanOrganicSubscriber().onScanOrganicEvent(sample("Sample"));

        assertTrue(narrator.spoke.await(5, TimeUnit.SECONDS), "the second sample was never voiced");
        assertEquals("Sample for genus Fungoida logged.", narrator.announced);
        assertNull(narrator.narrated, "the model must not be asked to rephrase the second sample");
    }

    private static ScanOrganicEvent sample(String scanType) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("event", "ScanOrganic");
        json.addProperty("ScanType", scanType);
        json.addProperty("Genus", "$Codex_Ent_Fungoids_Genus_Name;");
        json.addProperty("Genus_Localised", "Fungoida");
        json.addProperty("Species", "$Codex_Ent_Fungoids_02_Name;");
        json.addProperty("Species_Localised", "Fungoida Stabitis");
        json.addProperty("SystemAddress", 359602885418L);
        json.addProperty("Body", 21);
        return new ScanOrganicEvent(json);
    }

    private static final class CapturingNarrator implements VegaNarrator {
        private final CountDownLatch spoke = new CountDownLatch(1);
        private volatile String announced;
        private volatile String narrated;

        @Override
        public void filler(String text, boolean urgent) {
        }

        @Override
        public void narrate(String data, String instructions) {
            narrated = data;
            spoke.countDown();
        }

        @Override
        public void announce(String phrase, boolean urgent) {
            announced = phrase;
            spoke.countDown();
        }
    }
}
