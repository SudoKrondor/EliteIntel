package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.vega.VegaNarrator;
import elite.intel.ai.brain.vega.VegaRuntimeGraph;
import elite.intel.ai.brain.vega.VegaRuntimeTestSupport;
import elite.intel.gameapi.journal.events.WingInviteEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A wing invitation is announced as a fixed, localized line naming the commander - never phrased by the LLM, which once
 * turned a bare friend's name into a place (review, 2026-10-17).
 */
class WingInviteSubscriberTest {

    private final CapturingNarrator narrator = new CapturingNarrator();
    private VegaRuntimeGraph runtimeGraph;

    @BeforeEach
    void setUp() {
        runtimeGraph = VegaRuntimeTestSupport.installNarrator(narrator);
    }

    @AfterEach
    void tearDown() {
        VegaRuntimeTestSupport.uninstall(runtimeGraph);
    }

    @Test
    void theInviteIsAFixedLineNamingTheCommander() {
        new WingInviteSubscriber().onWingInvite(invite("Perilous Range"));

        assertEquals(1, narrator.announced.size());
        assertTrue(narrator.announced.getFirst().contains("Perilous Range"), narrator.announced.getFirst());
        assertTrue(narrator.data.isEmpty(), "nothing is left for the LLM to phrase");
    }

    @Test
    void anInviteWithNoNameIsNotSpoken() {
        new WingInviteSubscriber().onWingInvite(invite(" "));
        new WingInviteSubscriber().onWingInvite(invite(null));

        assertTrue(narrator.announced.isEmpty());
    }

    private static WingInviteEvent invite(String name) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("event", "WingInvite");
        if (name != null) json.addProperty("Name", name);
        return new WingInviteEvent(json);
    }

    private static final class CapturingNarrator implements VegaNarrator {
        private final List<String> data = new ArrayList<>();
        private final List<String> announced = new ArrayList<>();

        @Override
        public void filler(String text, boolean urgent) {
        }

        @Override
        public void narrate(String data, String instructions) {
            this.data.add(data);
        }

        @Override
        public void announce(String phrase, boolean urgent) {
            announced.add(phrase);
        }

    }
}
