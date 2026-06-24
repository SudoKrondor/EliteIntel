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
                    "who is our wing leader", "hale", "hale"),
            new Probe("remember that my home system is Shinrarta Dezhra",
                    "what is my home system", "shinrarta", "shinrarta"),
            new Probe("remember that our ship is named the Stardust Drifter",
                    "what is our ship called", "stardust", "stardust"),
            new Probe("remember that our rendezvous contact is Agent Vasquez",
                    "who is our rendezvous contact", "vasquez", "vasquez"),
            new Probe("remember that our priority cargo is medical supplies",
                    "what is our priority cargo", "medical", "medical"),
            new Probe("remember that our fuel reserve limit is 20 percent",
                    "what is our fuel reserve limit", "20", "20"),
            new Probe("remember that our emergency callsign is Mayday Seven",
                    "what is our emergency callsign", "mayday", "mayday"),
            new Probe("remember that our destination beacon is at Hutton Orbital",
                    "where is our destination beacon", "hutton", "hutton"));

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
        report.add(String.format("%-10s | %-26s | %-10s | %-6s | %s", "keyword", "located tier", "recalled", "hit", "spoken"));
        report.add("-".repeat(108));
        int hits = 0;
        for (Probe p : probes) {
            String tier = h.locateTier(p.locator());

            h.say(p.question());
            boolean recalled = h.recalled();
            boolean hit = h.spokenContains(p.keyword());
            if (hit) {
                hits++;
            }
            report.add(String.format("%-10s | %-26s | %-10s | %-6s | %s",
                    p.keyword(), tier, recalled ? "yes" : "no", hit ? "yes" : "no", h.spokenTexts()));
            report.add("    search: query='" + h.recalledQuery() + "' -> " + h.recallResult());
        }

        StringBuilder block = new StringBuilder("\n======== CONSCIOUS MEMORY / llm_memory (theme 5) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("hits: %d / %d%n", hits, probes.size()));
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
