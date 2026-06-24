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
 * Theme 5 (English): filling and using CONSCIOUS memory (llm_memory) - facts the companion deliberately
 * remembers. Each fact is given with an explicit "remember" instruction, then, after the prompt timeline has
 * moved on, probed. llm_memory is not inlined into the prompt (only a count is), so a correct answer requires
 * calling recall(scope=llm_memory). Scores, per fact: did it land in llm_memory, did the model recall it, and
 * does the spoken answer carry the detail. Opt-in; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsciousMemoryEvalTest {

    private record Probe(String plant, String question, String locator, String keyword) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-conscious-eval-trace.txt");

    private final List<Probe> probes = List.of(
            new Probe("remember that our docking authorization code is Sierra Nine Four",
                    "what is our docking authorization code", "sierra", "sierra"),
            new Probe("remember that I prefer trading and exploration over combat",
                    "what kind of activities do I prefer", "trading", "trading"),
            new Probe("remember that our wing leader is Commander Hale",
                    "who is our wing leader", "hale", "hale"));

    private static final List<String> FILLER = List.of(
            "what is our current location", "what's the time", "what's our heading", "are there contacts on radar",
            "what's the security level here", "how much cargo space do we have", "what's our fuel status",
            "show me the status panel", "are there stations nearby", "what's in our cargo hold");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void recallsDeliberatelyRememberedFacts() throws Exception {
        for (Probe p : probes) {
            h.say(p.plant());
        }
        for (String filler : FILLER) {
            h.say(filler);
        }

        List<String> report = new ArrayList<>();
        report.add(String.format("%-10s | %-26s | %-12s | %-6s | %s", "keyword", "located tier", "recall llm", "hit", "spoken"));
        report.add("-".repeat(110));
        int hits = 0;
        for (Probe p : probes) {
            String tier = h.locateTier(p.locator());

            h.say(p.question());
            boolean recalledLlm = h.recalled("llm_memory");
            boolean hit = h.spokenContains(p.keyword());
            if (hit) {
                hits++;
            }
            report.add(String.format("%-10s | %-26s | %-12s | %-6s | %s",
                    p.keyword(), tier, recalledLlm ? "yes" : "no", hit ? "yes" : "no", h.spokenTexts()));
        }

        StringBuilder block = new StringBuilder("\n======== CONSCIOUS MEMORY / llm_memory (theme 5) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("hits: %d / %d%n", hits, probes.size()));
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
