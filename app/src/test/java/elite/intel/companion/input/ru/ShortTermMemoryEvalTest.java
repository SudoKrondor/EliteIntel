package elite.intel.companion.input.ru;

import elite.intel.companion.input.CompanionEvalHarness;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Theme 3 (Russian): filling and using short-term memory from Russian commander input.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShortTermMemoryEvalTest {

    private record Probe(String plant, String question, String keyword) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-shortterm-eval-trace.txt", Language.RU);

    private final List<Probe> probes = List.of(
            new Probe("наш позывной для этой операции Nightingale",
                    "какой у нас позывной для этой операции", "nightingale"),
            new Probe("мы сопровождаем пассажира по имени Doctor Vance",
                    "кого мы сопровождаем", "vance"),
            new Probe("груз, который нужно защитить, лежит в секции rack four",
                    "в какой секции лежит груз, который нужно защитить", "four"));

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void usesRussianFactsStillInTheHotTimeline() throws Exception {
        List<String> report = new ArrayList<>();
        report.add(String.format("%-26s | %-22s | %-6s | %s", "probe keyword", "located tier", "hit", "spoken"));
        report.add("-".repeat(100));

        int hits = 0;
        for (Probe p : probes) {
            h.say(p.plant());
            String tier = h.locateTier(p.keyword());

            h.beginTurn();
            h.say(p.question());
            boolean hit = h.spokenContains(p.keyword());
            if (hit) {
                hits++;
            }
            report.add(String.format("%-26s | %-22s | %-6s | %s",
                    p.keyword(), tier, hit ? "yes" : "no", h.spokenTexts()));
        }

        StringBuilder block = new StringBuilder("\n======== RU SHORT-TERM MEMORY (theme 3) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("score: %d / %d%n", hits, probes.size()));
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
