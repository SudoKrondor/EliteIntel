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
 * Theme 4 (Russian): filling mid-term topic memory through Russian topic cues and Russian probes.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MidTermMemoryEvalTest {

    private record Probe(String topicCue, String plant, String question,
                         String expectedTopic, String locator, String keyword) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-midterm-eval-trace.txt", Language.RU);

    private final List<Probe> probes = List.of(
            new Probe("давай поговорим о торговле",
                    "для записи: painite лежит в грузовой секции seven",
                    "в какой секции лежит painite", "trade", "painite", "seven"),
            new Probe("давай поговорим о навигации",
                    "точка встречи находится у третьей луны Maia",
                    "где находится точка встречи", "navigation", "maia", "maia"),
            new Probe("давай поговорим о добыче ресурсов",
                    "наша цель добычи на этот рейс - low temperature diamonds",
                    "какая у нас цель добычи на этот рейс", "mining", "diamonds", "diamonds"));

    private static final List<String> FILLER = List.of(
            "где мы сейчас находимся", "который час", "какой у нас курс", "есть ли контакты на радаре",
            "какой здесь уровень безопасности", "сколько у нас свободного места в трюме",
            "какой у нас запас топлива", "покажи панель статуса", "есть ли поблизости станции",
            "какая ближайшая звезда для заправки", "сколько прыжков до пузыря", "что у нас в трюме");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void recallsRussianFactsByTopicAfterEviction() throws Exception {
        for (Probe p : probes) {
            h.say(p.topicCue());
            h.say(p.plant());
        }
        for (String filler : FILLER) {
            h.say(filler);
        }

        List<String> report = new ArrayList<>();
        report.add(String.format("%-14s | %-26s | %-12s | %-10s | %-6s | %s",
                "expected topic", "located tier", "topic ok", "recalled", "hit", "spoken"));
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
            report.add(h.memoryDeltaBlock()); // what the probe turn wrote to memory
        }

        StringBuilder block = new StringBuilder("\n======== RU MID-TERM MEMORY BY TOPIC (theme 4) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("hits: %d / %d%n", hits, probes.size()));
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
