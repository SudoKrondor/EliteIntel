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

/**
 * Theme 6 (Russian): long single-topic Russian conversation that should consolidate into long-term memory.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LongTermMemoryEvalTest {

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-longterm-eval-trace.txt", Language.RU);

    private static final String EARLY_FACT_TOKEN = "deciat";

    private static final List<String> NAVIGATION = List.of(
            "проложи маршрут к Sol", "как далеко следующая звездная система", "какой у нас текущий уровень топлива",
            "найди ближайшую звезду для заправки", "сколько прыжков до цели", "какая дистанция осталась",
            "начни следующий прыжок когда будем готовы", "есть ли нейтронные звезды на нашем пути",
            "какая у нас максимальная дальность прыжка", "пересчитай маршрут в экономичном режиме",
            "сколько времени до прибытия", "цель находится на планетарной базе",
            "какой уровень безопасности в следующей системе", "проверь ближайшие точки интереса",
            "какой у нас текущий курс", "выровняй нас на следующую цель прыжка",
            "сколько топлива стоит следующий прыжок", "мы в суперкруизе или в обычном пространстве",
            "какая ближайшая обитаемая система", "проходит ли маршрут через анархические системы",
            "какие координаты у нашей цели", "выведи нас из суперкруиза рядом со станцией",
            "сколько световых лет мы уже прошли", "поставь следующую точку маршрута на карте галактики",
            "какое у нас среднее время прыжка", "есть ли топливная звезда по пути",
            "сколько систем осталось неотсканированными", "какое население у системы назначения",
            "есть ли поблизости туристические маяки", "какой размер посадочной площадки у следующей станции");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void consolidatesAndUsesTheRussianLongTermSummary() throws Exception {
        h.say("давай поговорим о навигации");
        h.say("запомни для сессии: наше путешествие началось в системе Deciat");
        for (String turn : NAVIGATION) {
            h.say(turn);
        }
        Thread.sleep(6000);

        String summary = h.memory().longTermSummary();
        boolean consolidated = summary != null && !summary.isBlank();
        boolean earlyFactSurvived = consolidated && summary.toLowerCase(java.util.Locale.ROOT).contains(EARLY_FACT_TOKEN);

        h.say("над чем мы работали в этой сессии");
        boolean answered = !h.spokenTexts().isEmpty();

        h.say("где началось наше путешествие");
        boolean originRecalled = h.spokenContains(EARLY_FACT_TOKEN);

        StringBuilder block = new StringBuilder("\n======== RU LONG-TERM MEMORY (theme 6) ========\n");
        block.append("consolidation fired (long-term summary present): ").append(consolidated).append("\n");
        block.append("early fact '").append(EARLY_FACT_TOKEN).append("' survived into summary: ")
                .append(earlyFactSurvived).append("\n");
        block.append("answered a session-summary question: ").append(answered).append("\n");
        block.append("origin recalled at probe ('").append(EARLY_FACT_TOKEN).append("'): ")
                .append(originRecalled).append("\n");
        block.append("long-term summary:\n")
                .append(consolidated ? summary : "(none - the run was not long enough to consolidate)").append("\n");
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
