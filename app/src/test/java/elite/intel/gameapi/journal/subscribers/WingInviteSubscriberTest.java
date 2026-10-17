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
 * A wing invitation is narrated with the name labelled as a commander's, and nothing else to invent from - a bare name
 * reads to a small model like anything, and the Friends announcement once came back with a friend's name as a place.
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
    void theNameIsLabelledAsTheInvitingCommanders() {
        new WingInviteSubscriber().onWingInvite(invite("Perilous Range"));

        assertEquals(1, narrator.data.size());
        assertEquals("Wing invitation received from another player. Inviting commander's name: Perilous Range",
                narrator.data.getFirst());
        assertTrue(narrator.instructions.getFirst().contains("Commander Perilous Range"));
        assertTrue(narrator.instructions.getFirst().contains("Say the name exactly as written"));
    }

    @Test
    void anInviteWithNoNameIsNotSpoken() {
        new WingInviteSubscriber().onWingInvite(invite(" "));
        new WingInviteSubscriber().onWingInvite(invite(null));

        assertTrue(narrator.data.isEmpty());
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
        private final List<String> instructions = new ArrayList<>();

        @Override
        public void filler(String text, boolean urgent) {
        }

        @Override
        public void narrate(String data, String instructions) {
            this.data.add(data);
            this.instructions.add(instructions);
        }

        @Override
        public void announce(String phrase, boolean urgent) {
        }
    }
}
