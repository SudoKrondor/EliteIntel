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
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Theme 7 (Russian): behaviour when the companion cannot directly satisfy a Russian request.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehavioralEvalTest {

    private static final List<String> MISS_CUES = List.of(
            "не знаю", "не могу", "нет данных", "нет информации", "не уверен", "не уверена",
            "не найден", "не найдено", "недоступ", "отсутств", "не располагаю", "не имею");
    private static final List<String> CLARIFY_CUES = List.of(
            "уточни", "уточните", "подроб", "что именно", "какой именно", "какую именно",
            "нужно больше", "можешь уточнить", "можете уточнить");

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-behavioral-eval-trace.txt", Language.RU);

    private final List<String> asks = List.of(
            "какая у нас максимальная дальность прыжка",
            "вычисли орбитальный период ближайшей планеты",
            "какая родная планета командира",
            "включи маскировочное устройство",
            "сколько членов экипажа на станции, к которой мы приближаемся");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void handlesRussianUnknownRequestsWithoutFabricating() throws Exception {
        List<String> report = new ArrayList<>();
        report.add(String.format("%-64s | %-8s | %-12s | %s", "ask", "clarify", "outcome", "spoken"));
        report.add("-".repeat(145));

        int good = 0;
        for (String ask : asks) {
            h.say(ask);
            boolean clarify = h.called("clarify") || cued(h.spokenTexts(), CLARIFY_CUES);
            boolean honestMiss = cued(h.spokenTexts(), MISS_CUES);
            boolean handledWell = clarify || honestMiss;
            if (handledWell) {
                good++;
            }
            String outcome = clarify ? "clarify" : honestMiss ? "honest" : h.spokenTexts().isEmpty() ? "silent" : "FABRICATED?";
            report.add(String.format("%-64s | %-8s | %-12s | %s",
                    ask, clarify ? "yes" : "no", outcome, h.spokenTexts()));
            report.add(h.memoryDeltaBlock()); // what this ask wrote to memory
        }

        StringBuilder block = new StringBuilder("\n======== RU BEHAVIORAL / not-knowing (theme 7) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("handled well (clarified / honest): %d / %d%n", good, asks.size()));
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }

    private static boolean cued(List<String> texts, List<String> cues) {
        return texts.stream().anyMatch(s -> cues.stream().anyMatch(c -> s.toLowerCase(Locale.ROOT).contains(c)));
    }
}
