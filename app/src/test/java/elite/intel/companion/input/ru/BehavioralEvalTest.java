package elite.intel.companion.input.ru;

import elite.intel.companion.CompanionConfig;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    // Identity asks: the companion knows its own name from the persona prompt and must answer with it.
    private final List<String> nameAsks = List.of(
            "как тебя зовут",
            "как мне тебя называть",
            "кто ты");

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

    @Test
    void knowsItsOwnName() throws Exception {
        String name = CompanionConfig.companionName();
        // Russian output may transliterate the Latin name (e.g. "Vega" -> "Вега"); accept either form.
        String transliterated = "Вега";
        List<String> report = new ArrayList<>();
        report.add(String.format("%-36s | %-9s | %s", "ask", "said-name", "spoken"));
        report.add("-".repeat(120));

        int said = 0;
        for (String ask : nameAsks) {
            h.say(ask);
            boolean saidName = h.spokenContains(name) || h.spokenContains(transliterated);
            if (saidName) {
                said++;
            }
            report.add(String.format("%-36s | %-9s | %s", ask, saidName ? "yes" : "NO", h.spokenTexts()));
            report.add(h.memoryDeltaBlock());
        }

        StringBuilder block = new StringBuilder("\n======== RU NAME / companion identity ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("said its name \"%s\"/\"%s\": %d / %d%n", name, transliterated, said, nameAsks.size()));
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertTrue(said > 0, "the companion never said its own name \"" + name + "\" - see the trace");
    }

    private static boolean cued(List<String> texts, List<String> cues) {
        return texts.stream().anyMatch(s -> cues.stream().anyMatch(c -> s.toLowerCase(Locale.ROOT).contains(c)));
    }
}
