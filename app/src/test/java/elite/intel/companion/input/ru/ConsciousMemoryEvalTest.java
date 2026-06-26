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
 * Theme 5 (Russian): deliberate llm_memory writes from Russian "remember" instructions and Russian probes.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsciousMemoryEvalTest {

    private record Probe(String plant, String question, String locator, String keyword) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-conscious-eval-trace.txt", Language.RU);

    private final List<Probe> probes = List.of(
            new Probe("запомни, что наш код авторизации стыковки Sierra Nine Four",
                    "какой у нас код авторизации стыковки", "sierra", "sierra"),
            new Probe("запомни, что я предпочитаю trading и exploration вместо боя",
                    "какие активности я предпочитаю", "trading", "trading"),
            new Probe("запомни, что наш лидер крыла Commander Hale",
                    "кто наш лидер крыла", "hale", "hale"),
            new Probe("запомни, что моя домашняя система Shinrarta Dezhra",
                    "какая у меня домашняя система", "shinrarta", "shinrarta"),
            new Probe("запомни, что наш корабль называется Stardust Drifter",
                    "как называется наш корабль", "stardust", "stardust"),
            new Probe("запомни, что наш контакт на встрече Agent Vasquez",
                    "кто наш контакт на встрече", "vasquez", "vasquez"),
            new Probe("запомни, что наш приоритетный груз medical supplies",
                    "какой у нас приоритетный груз", "medical", "medical"),
            new Probe("запомни, что лимит топливного резерва 20 percent",
                    "какой у нас лимит топливного резерва", "20", "20"),
            new Probe("запомни, что наш аварийный позывной Mayday Seven",
                    "какой у нас аварийный позывной", "mayday", "mayday"),
            new Probe("запомни, что маяк назначения находится на Hutton Orbital",
                    "где находится маяк назначения", "hutton", "hutton"));

    private static final List<String> FILLER = List.of(
            "где мы сейчас находимся", "который час", "какой у нас курс", "есть ли контакты на радаре",
            "какой здесь уровень безопасности", "сколько у нас свободного места в трюме",
            "какой у нас запас топлива", "покажи панель статуса", "есть ли поблизости станции", "что у нас в трюме");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void recallsDeliberatelyRememberedRussianFacts() throws Exception {
        for (Probe p : probes) {
            h.say(p.plant());
        }
        for (String filler : FILLER) {
            h.say(filler);
        }

        List<String> report = new ArrayList<>();
        report.add(String.format("%-10s | %-26s | %-10s | %-6s | %s",
                "keyword", "located tier", "recalled", "hit", "spoken"));
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
            report.add("    calls : " + h.turnToolNames());
        }

        StringBuilder block = new StringBuilder("\n======== RU CONSCIOUS MEMORY / llm_memory (theme 5) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("hits: %d / %d%n", hits, probes.size()));
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
