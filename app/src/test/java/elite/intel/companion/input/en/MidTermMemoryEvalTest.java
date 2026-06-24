package elite.intel.companion.input.en;

import elite.intel.companion.input.CompanionEvalHarness;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Theme 4 (English): filling MID-TERM topic memory across several topics, the quality of topic detection,
 * and the use of topic recall. Facts are planted under different topics (each pinned with a topic switch),
 * then pushed out of short-term by topic-spread filler. For each probe it scores: which topic actually
 * holds the fact (topic-detection quality), whether the model called recall(scope=topic_memory) for that
 * topic, and whether the spoken answer carries the planted detail. Opt-in; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MidTermMemoryEvalTest {

    /** A fact pinned to a topic, then probed after eviction; {@code expectedTopic} is where it should land. */
    private record Probe(String topicCue, String plant, String question, String expectedTopic, String locator, String keyword) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-midterm-eval-trace.txt");

    private final List<Probe> probes = List.of(
            new Probe("let's talk about trade", "for the record, we cached the painite in cargo rack seven",
                    "which cargo rack holds the painite", "trade", "painite", "seven"),
            new Probe("let's talk about navigation", "the rendezvous point is the third moon of Maia",
                    "where is the rendezvous point", "navigation", "maia", "maia"),
            new Probe("let's talk about mining", "our mining target this run is low temperature diamonds",
                    "what is our mining target this run", "mining", "diamonds", "diamonds"));

    // Topic-spread filler so eviction is not concentrated in one topic (which would overflow that topic and
    // push its own older facts past mid-term).
    private static final List<String> FILLER = List.of(
            "what is our current location", "what's the time", "what's our heading", "are there contacts on radar",
            "what's the security level here", "how much cargo space do we have", "what's our fuel status",
            "show me the status panel", "are there stations nearby", "what's the nearest scoopable star",
            "how many jumps to the bubble", "what's in our cargo hold");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void recallsFactsByTopicAfterEviction() throws Exception {
        for (Probe p : probes) {
            h.say(p.topicCue());
            h.say(p.plant());
        }
        for (String filler : FILLER) {
            h.say(filler);
        }

        List<String> report = new ArrayList<>();
        report.add(String.format("%-14s | %-26s | %-12s | %-10s | %-6s | %s", "expected topic", "located tier", "topic ok", "recalled", "hit", "spoken"));
        report.add("-".repeat(120));
        int hits = 0;
        for (Probe p : probes) {
            String tier = h.locateTier(p.locator());
            String actualTopic = h.midTermTopic(p.locator());
            boolean topicOk = p.expectedTopic().equals(actualTopic);

            h.say(p.question());
            boolean recalled = h.recalled();
            boolean hit = h.spokenContains(p.keyword());
            if (hit) {
                hits++;
            }
            report.add(String.format("%-14s | %-26s | %-12s | %-10s | %-6s | %s",
                    p.expectedTopic(), tier, topicOk ? "yes" : "(" + actualTopic + ")",
                    recalled ? "yes" : "no", hit ? "yes" : "no", h.spokenTexts()));
        }

        StringBuilder block = new StringBuilder("\n======== MID-TERM MEMORY BY TOPIC (theme 4) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("hits: %d / %d%n", hits, probes.size()));
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
