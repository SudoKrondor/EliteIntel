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
    /** The Russian companion transliterates the Latin system name when speaking, so accept either form. */
    private static final String EARLY_FACT_CYRILLIC = "дециат";

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
        // Two passes of the single-topic navigation script: enough volume to overflow mid-term into the
        // long-term consolidation buffer and fire consolidation. The lone early MAX fact does NOT reach the
        // archive (mid-term eviction keeps MAX longest, so it stays searchable in mid-term); the archive-fill
        // path is covered deterministically by the unit tests, not this run.
        for (int pass = 0; pass < 2; pass++) {
            for (String turn : NAVIGATION) {
                h.say(turn);
            }
        }
        Thread.sleep(6000);

        String summary = h.memory().longTermSummary();
        boolean consolidated = summary != null && !summary.isBlank();
        // A lone MAX fact stays protected in mid-term (importance-aware eviction keeps MAX longest), so the
        // archive - the overflow path that only fills when MAX facts exceed a topic's mid-term cap - is normally
        // empty here. Report its size as an observation, not a pass/fail.
        int archiveSize = h.memory().longTermPinnedFacts().size();

        h.say("над чем мы работали в этой сессии");
        boolean answered = !h.spokenTexts().isEmpty();

        h.say("где началось наше путешествие");
        // Robust recall signal: the search RESULT carries the fact verbatim (the stored commander input keeps the
        // Latin "deciat"), independent of how the companion transliterates it aloud. The spoken form varies
        // (deciat / дициат / декиат), so the spoken check is softer and accepts any of them.
        boolean recallReturnedFact = h.recallResult().stream()
                .anyMatch(s -> s.toLowerCase(java.util.Locale.ROOT).contains(EARLY_FACT_TOKEN));
        boolean originSpoken = h.spokenContains(EARLY_FACT_TOKEN)
                || h.spokenContains(EARLY_FACT_CYRILLIC) || h.spokenContains("декиат");

        StringBuilder block = new StringBuilder("\n======== RU LONG-TERM MEMORY (theme 6) ========\n");
        block.append("consolidation fired (long-term summary present): ").append(consolidated).append("\n");
        block.append("MAX archive size (overflow path; a lone MAX stays protected in mid-term): ").append(archiveSize).append("\n");
        block.append("answered a session-summary question: ").append(answered).append("\n");
        block.append("recall returned the early fact (search result): ").append(recallReturnedFact).append("\n");
        block.append("origin spoken aloud (deciat/дициат/декиат): ").append(originSpoken).append("\n");
        if (archiveSize > 0) {
            block.append("pinned MAX archive:\n");
            h.memory().longTermPinnedFacts().forEach(e -> block.append("  ").append(e.content()).append("\n"));
        }
        block.append("long-term summary:\n")
                .append(consolidated ? summary : "(none - the run was not long enough to consolidate)").append("\n");
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
