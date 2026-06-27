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
 * Theme 3 (Russian): filling and using SHORT-TERM memory across a natural conversation. One continuous
 * smuggling-run dialogue - the commander states facts and, interleaved, asks the companion to recall
 * earlier ones while they are still in the hot session timeline. Two questions ask about what the COMPANION
 * itself said ("что ты подтвердил…", "как ты повторил…"), exercising recall of its own [COMPANION] lines. Each
 * fact is a pure session detail (codeword / name) with no game-query twin and a keyword that survives Russian
 * speech and declension (a consonant-ending base form that stays a substring of its declined forms). Opt-in
 * via the local-integration tag; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShortTermMemoryEvalTest {

    /** One conversation turn: {@code expect == null} is a commander statement, otherwise a question whose
     *  spoken answer must contain {@code expect}. */
    private record Step(String text, String expect) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-shortterm-eval-trace.txt", Language.RU);

    private final List<Step> script = List.of(
            new Step("так, экипаж, этот контрабандный рейс через границу назовём Сквозняк, проскочим тихо", null),
            new Step("в прикрытии с нами идёт старый Филин, держись его крыла", null),
            new Step("груз заберёт перекупщик на станции, зовут Назаров, с ним и торгуемся", null),
            new Step("если дело завоняет, кодовое слово на отмену сделки — Гранит, заруби на носу", null),
            new Step("напомни-ка, как мы этот рейс окрестили?", "сквозняк"),
            new Step("сядут на хвост — уходим в астероиды, точку отрыва помечай как Омут", null),
            new Step("что ты там подтвердил насчёт кодового слова на отмену?", "гранит"),
            new Step("к Назарову нас сведёт местный барыга по кличке Грач", null),
            new Step("слушай, а перекупщика-то как звать, из головы вылетело?", "назаров"),
            new Step("запасной точкой сбора держим старый форт Редут", null),
            new Step("и хватит сопеть в эфире, идём молча до самой станции", null),
            new Step("кто у нас в прикрытии, позывной напомни?", "филин"),
            new Step("маяк на входе в систему помечай как Беркут", null),
            new Step("куда уходим, если оторвёмся от хвоста?", "омут"),
            new Step("наше крыло по сводке проходит как отряд Вереск", null),
            new Step("запасная точка сбора у нас какая, напомни?", "редут"),
            new Step("как ты повторил, наш отряд по сводке называется?", "вереск"),
            new Step("через кого выходим на перекупщика, кличку напомни?", "грач"),
            new Step("куда помечен маяк на входе?", "беркут"),
            new Step("и кодовое слово на отмену ещё раз повтори", "гранит"));

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void recallsFactsAcrossANaturalConversation() throws Exception {
        List<String> report = new ArrayList<>();
        report.add(String.format("%-48s | %-22s | %-6s | %s", "question -> keyword", "located tier", "hit", "spoken"));
        report.add("-".repeat(120));

        int hits = 0;
        int questions = 0;
        for (Step step : script) {
            h.say(step.text());
            report.add("[COMMANDER] " + step.text());
            h.spokenTexts().forEach(s -> report.add("[COMPANION] " + s));
            if (step.expect() == null) {
                continue; // a commander statement: just fills the hot timeline
            }
            questions++;
            String tier = h.locateTier(step.expect());
            boolean hit = h.spokenContains(step.expect());
            if (hit) {
                hits++;
            }
            report.add(String.format("    -> expect '%s' | tier=%s | hit=%s", step.expect(), tier, hit ? "yes" : "no"));
        }

        StringBuilder block = new StringBuilder("\n======== RU SHORT-TERM MEMORY (theme 3) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("score: %d / %d%n", hits, questions));
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
