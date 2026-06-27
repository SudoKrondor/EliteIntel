package elite.intel.companion.input.en;

import elite.intel.companion.input.CompanionEvalHarness;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Theme 6 (English): filling and using LONG-TERM memory. A long, single-topic (navigation) conversation
 * overflows that topic's mid-term memory past its cap, fills the consolidation buffer and triggers the
 * LLM compression into the long-term summary (which is inlined into the prompt, not recalled). The test
 * reports whether consolidation fired, whether a distinctive early fact survived the lossy compression,
 * and whether the companion can answer a question about the session from the summary. Opt-in; LM Studio up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LongTermMemoryEvalTest {

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-longterm-eval-trace.txt");

    private static final String EARLY_FACT_TOKEN = "deciat";

    // Long single-topic run: overflows mid-term[navigation] into the consolidation buffer -> long-term summary.
    private static final List<String> NAVIGATION = List.of(
            "plot a route to Sol", "how far is the next star system", "what's our current fuel level",
            "find the nearest scoopable star", "how many jumps to the destination", "what's the distance remaining",
            "engage the next jump when ready", "are there any neutron stars on our path", "what's our maximum jump range",
            "recalculate the route in economic mode", "how long until we reach the destination", "is the destination a planetary base",
            "what's the security level of the system ahead", "scan for nearby points of interest", "what's our current heading",
            "align us with the next jump target", "how much fuel does the next jump cost", "are we in supercruise or normal space",
            "what's the closest inhabited system", "does the route pass through any anarchy systems",
            "what are the coordinates of our destination", "drop us out of supercruise near the station",
            "how many light years have we travelled", "set the next waypoint on the galaxy map",
            "what is our average jump time", "is there a fuel star on the way", "how many systems remain unscanned",
            "what is the population of the destination", "are there any tourist beacons nearby", "what's the next station's pad size");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void consolidatesAndUsesTheLongTermSummary() throws Exception {
        h.say("let's talk about navigation");
        h.say("note that our journey began in the Deciat system"); // distinctive early fact
        for (String turn : NAVIGATION) {
            h.say(turn);
        }
        // The consolidator runs on a background thread; give it a moment to flush a triggered batch.
        Thread.sleep(6000);

        String summary = h.memory().longTermSummary();
        boolean consolidated = summary != null && !summary.isBlank();
        boolean earlyFactSurvived = consolidated && summary.toLowerCase(java.util.Locale.ROOT).contains(EARLY_FACT_TOKEN);

        h.say("what have we been working on this session");
        boolean answered = !h.spokenTexts().isEmpty();

        h.say("where did our journey begin");
        boolean originRecalled = h.spokenContains(EARLY_FACT_TOKEN);

        StringBuilder block = new StringBuilder("\n======== LONG-TERM MEMORY (theme 6) ========\n");
        block.append("consolidation fired (long-term summary present): ").append(consolidated).append("\n");
        block.append("early fact '").append(EARLY_FACT_TOKEN).append("' survived into summary: ").append(earlyFactSurvived).append("\n");
        block.append("answered a session-summary question: ").append(answered).append("\n");
        block.append("origin recalled at probe ('").append(EARLY_FACT_TOKEN).append("'): ").append(originRecalled).append("\n");
        block.append("long-term summary:\n").append(consolidated ? summary : "(none - the run was not long enough to consolidate)").append("\n");
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
