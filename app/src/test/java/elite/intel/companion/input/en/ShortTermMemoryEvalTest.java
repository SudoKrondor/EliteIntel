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
 * Theme 3 (English): filling and using SHORT-TERM memory. Each fact is stated and then probed on the very
 * next turn, while it is still in the hot session timeline that is inlined into the prompt - so a correct
 * answer needs no recall at all. Scores, per fact, the located tier at probe time and whether the spoken
 * answer carries the planted detail. Opt-in via the local-integration tag; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShortTermMemoryEvalTest {

    /** A fact stated, then immediately probed; {@code keyword} is the planted detail expected in the answer. */
    private record Probe(String plant, String question, String keyword) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-shortterm-eval-trace.txt");

    private final List<Probe> probes = List.of(
            new Probe("our callsign for this operation is Nightingale", "what's our callsign for this operation", "nightingale"),
            new Probe("we are escorting a passenger named Doctor Vance", "who are we escorting", "vance"),
            new Probe("the cargo we must protect is in rack four", "which rack holds the cargo we must protect", "four"));

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void usesFactsStillInTheHotTimeline() throws Exception {
        List<String> report = new ArrayList<>();
        report.add(String.format("%-26s | %-22s | %-6s | %s", "probe keyword", "located tier", "hit", "spoken"));
        report.add("-".repeat(100));

        int hits = 0;
        for (Probe p : probes) {
            h.say(p.plant());           // state the fact (lands in short-term)
            String tier = h.locateTier(p.keyword());

            h.beginTurn();
            h.say(p.question());        // probe on the next turn, fact still hot
            boolean hit = h.spokenContains(p.keyword());
            if (hit) {
                hits++;
            }
            report.add(String.format("%-26s | %-22s | %-6s | %s", p.keyword(), tier, hit ? "yes" : "no", h.spokenTexts()));
            report.add(h.memoryDeltaBlock()); // what the plant + probe wrote to memory, this probe
        }

        StringBuilder block = new StringBuilder("\n======== SHORT-TERM MEMORY (theme 3) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("score: %d / %d%n", hits, probes.size()));
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
