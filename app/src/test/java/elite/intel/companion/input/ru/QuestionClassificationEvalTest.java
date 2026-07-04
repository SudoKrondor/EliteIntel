package elite.intel.companion.input.ru;

import elite.intel.companion.input.CompanionEvalHarness;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Theme (Russian): the {@code is_question} classification eval. classify_turn now carries a third piece of
 * metadata - whether the commander's current phrase is a question. This live eval drives 30 Russian probes (15
 * genuine questions, 15 statements/commands/banter) through the real companion loop and checks the boolean the
 * model set on classify_turn against the expected value. Mostly observational (the trace shows every probe's
 * expected vs. assigned flag and the score); the hard assertions are only that the model was reached and that
 * classify_turn actually carried the flag. Opt-in; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuestionClassificationEvalTest {

    /** One probe: a commander line and whether it should be flagged as a question. */
    private record Probe(String line, boolean expectQuestion) {}

    private static Probe question(String line) { return new Probe(line, true); }
    private static Probe statement(String line) { return new Probe(line, false); }

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-is-question-eval-trace.txt", Language.RU);

    // 30 probes: 15 questions expecting is_question=true, 15 non-questions (command/instruction/banter) expecting false.
    private final List<Probe> probes = List.of(
            question("сколько у нас топлива осталось?"),
            question("где ближайшая станция с ремонтом?"),
            question("ты помнишь, какой у нас код стыковки?"),
            question("напомни, как зовут нашего покупателя утиля"),
            question("какой уровень безопасности в этой системе?"),
            question("что у нас сейчас в трюме?"),
            question("далеко ещё до точки назначения?"),
            question("какой следующий прыжок по маршруту?"),
            question("ты уверен, что это правильный курс?"),
            question("повтори, где у нас аварийная точка встречи"),
            question("кто командует на той станции?"),
            question("почему двигатель так греется?"),
            question("какие цены на палладий на местном рынке?"),
            question("сколько мы заработаем за этот рейс?"),
            question("когда нам ждать перехвата?"),
            statement("проложи курс до системы Соль"),
            statement("запиши: кодовое слово на отход — Гранит"),
            statement("красивая туманность за бортом, аж дух захватывает"),
            statement("опусти шасси, идём на стыковку"),
            statement("да я просто болтаю, чтоб тишину разбавить"),
            statement("включи форсаж, уходим отсюда"),
            statement("запомни: ниже двадцати процентов топлива не падаем"),
            statement("ну и денёк сегодня выдался, аж вымотался"),
            statement("сбрось скорость перед стыковочным коридором"),
            statement("наш связной на станции — Дельгадо"),
            statement("разверни корабль к выходу из поля астероидов"),
            statement("люблю смотреть, как звёзды проносятся мимо"),
            statement("отметь этот сектор как зачищенный"),
            statement("заряжай ССД, готовимся к прыжку"),
            statement("просто проверяю связь, всё чисто"));

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void classifiesQuestionsAcrossRussianProbes() throws Exception {
        StringBuilder block = new StringBuilder(
                String.format("%n======== RU IS_QUESTION CLASSIFICATION (%d probes) ========%n", probes.size()));

        int classified = 0; // classify_turn actually carried the flag
        int correct = 0;    // flag matched the expected value
        for (Probe probe : probes) {
            h.beginTurn();
            h.say(probe.line());
            String assigned = h.assignedIsQuestion();
            boolean called = !assigned.isEmpty();
            boolean ok = called && Boolean.parseBoolean(assigned) == probe.expectQuestion();
            if (called) {
                classified++;
            }
            if (ok) {
                correct++;
            }
            block.append(String.format("ждём is_question=%s | получено=%s | ok=%s | '%s'%n",
                    probe.expectQuestion(), assigned.isEmpty() ? "(none)" : assigned, ok, probe.line()));
        }

        block.append("\n---- итоги ----\n");
        block.append(String.format("classify_turn проставил флаг: %d / %d%n", classified, probes.size()));
        block.append(String.format("флаг угадан верно:           %d / %d%n", correct, probes.size()));
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
        assertTrue(classified > 0, "classify_turn never carried is_question - see the trace");
    }
}
