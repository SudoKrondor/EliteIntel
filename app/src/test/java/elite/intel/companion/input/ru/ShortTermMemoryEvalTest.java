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
 * Theme 3 (Russian): filling and using SHORT-TERM memory from Russian commander input. Each fact is stated in
 * Russian and probed on the very next turn, while it is still in the hot session timeline that is inlined into
 * the prompt - so a correct answer needs no recall at all. Scores, per fact, the located tier at probe time and
 * whether the spoken answer carries the planted detail. Opt-in via the local-integration tag; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShortTermMemoryEvalTest {

    private record Probe(String plant, String question, String keyword) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-shortterm-eval-trace.txt", Language.RU);

    // Russian details on purpose: a Russian session's STT transcribes in lower-case Cyrillic and does not
    // emit clean Latin words, so English keywords would not survive real speech. Keywords are word stems that
    // also survive Russian declension (сокол -> Сокола, соколов -> Соколова).
    private final List<Probe> probes = List.of(
            // Pure session facts with no game-query twin: a fact that overlaps a query (e.g. cargo/hold ->
            // query_cargo_hold_contents) makes the model query live state instead of recalling, so these stay
            // codewords / names / passwords the game has no function for.
            new Probe("наш позывной для этой операции — сокол",
                    "какой у нас позывной для этой операции", "сокол"),
            new Probe("мы сопровождаем пассажира по имени доктор соколов",
                    "кого мы сопровождаем", "соколов"),
            new Probe("кодовое слово для отмены операции — гранит",
                    "какое кодовое слово для отмены операции", "гранит"),
            new Probe("наш связной — механик борис",
                    "как зовут нашего связного", "борис"),
            new Probe("пароль доступа на сегодня — восход",
                    "какой у нас пароль доступа на сегодня", "восход"),
            new Probe("наша зона сбора называется утёс",
                    "как называется наша зона сбора", "утёс"),
            new Probe("позывной нашего ведущего — ястреб",
                    "какой позывной у нашего ведущего", "ястреб"),
            new Probe("кодовое имя точки эвакуации — дельта",
                    "какое кодовое имя у точки эвакуации", "дельта"),
            new Probe("наша запасная база называется редут",
                    "как называется наша запасная база", "редут"),
            new Probe("название нашего отряда — гроза",
                    "как называется наш отряд", "гроза"));

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
            report.add(h.memoryDeltaBlock()); // what the plant + probe wrote to memory, this probe
        }

        StringBuilder block = new StringBuilder("\n======== RU SHORT-TERM MEMORY (theme 3) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("score: %d / %d%n", hits, probes.size()));
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
