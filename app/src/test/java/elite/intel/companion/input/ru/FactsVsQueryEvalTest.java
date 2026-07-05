package elite.intel.companion.input.ru;

import elite.intel.companion.input.CompanionEvalHarness;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.gameapi.journal.events.BaseEvent;
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
 * Theme (Russian): the settling-ladder arbitration between remembered facts and offered game functions -
 * a focused version of the failing MemoryEvalTest "recall событий" cases. Seeds ten facts (five HIGH game
 * events and five commander statements), then EVICTS them from the hot short-term timeline with filler
 * banter before probing - verified by a hard assertion - so each probe can be answered only via the
 * injected {@code <facts>} candidates, never from the replayed conversation. Ten memory probes follow
 * (the eval DB has no tracked missions/scans, so a query would answer "nothing"), plus one live-state
 * control question with no fact behind it: ladder rule 1 should settle the probes with speak-from-fact,
 * and the control must still route to a game function (rule 2 not over-suppressed).
 * <p>
 * Recorder-style: per probe it traces the executed tools, the injected fact candidates, and the spoken
 * reply; hard assertions are only that eviction succeeded and the live model was reached. Opt-in;
 * LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FactsVsQueryEvalTest {

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-facts-vs-query-trace.txt", Language.RU);

    /** One memory probe: the question and the keyword its spoken answer must carry. */
    private record Probe(String question, String keyword) {}

    /** One seeded game event: journal type and the Russian summary that lands in memory. */
    private record Seed(String type, String summary) {}

    private final List<Seed> events = List.of(
            new Seed("MissionAccepted", "принята боевая миссия против фракции алый картель"),
            new Seed("ShipTargeted", "просканирован разыскиваемый пират по имени варгас"),
            new Seed("Bounty", "получена награда за уничтожение пирата по кличке шакал"),
            new Seed("Docked", "пристыковались к станции джеймсон мемориал"),
            new Seed("SAASignalsFound", "обнаружены биосигналы рода светляк на планете"));

    private final List<String> commanderFacts = List.of(
            "запиши: код стыковки на станции — сьерра девять четыре",
            "если зажмут пираты, кодовое слово на отход — гранит",
            "покупатель утиля — халлоран, наш контакт на рынке",
            "аварийная точка встречи — хаттон орбитал, если разделимся",
            "наш пилот истребителя — оконкво");

    private final List<Probe> probes = List.of(
            new Probe("против какой фракции у нас боевая миссия?", "картель"),
            new Probe("как звали просканированного пирата?", "варгас"),
            new Probe("за уничтожение какого пирата мы получили награду?", "шакал"),
            new Probe("к какой станции мы пристыковались?", "джеймсон"),
            new Probe("биосигналы какого рода мы обнаружили?", "светляк"),
            new Probe("какой у нас код стыковки на станции?", "сьерра"),
            new Probe("какое кодовое слово на отход?", "гранит"),
            new Probe("кто покупатель утиля?", "халлоран"),
            new Probe("где аварийная точка встречи?", "хаттон"),
            new Probe("как зовут пилота истребителя?", "оконкво"));

    // Idle banter with no fact keywords: pushes the seeded facts (and Vega's echoes of them) out of the
    // 30-entry hot window, so by probe time the facts live only in mid-term and reach the prompt solely
    // as <facts> candidates. Sized so its writes exceed the window even with silent turns.
    private final List<String> filler = List.of(
            "ну и тишина сегодня, аж в ушах звенит",
            "обожаю такие спокойные вылеты",
            "как настроение, не заскучала там?",
            "красивая туманность за бортом",
            "да я просто болтаю, чтоб тишину разбавить",
            "ты вообще когда-нибудь отдыхаешь?",
            "за такие минуты покоя и люблю эту работу",
            "кофе бы сейчас, да автомат опять чудит",
            "хех, вспомнил тут одну байку, да ладно, потом",
            "просто хотел услышать твой голос",
            "денёк сегодня ленивый, даже приборы дремлют",
            "звёзды тут всё-таки красивые, не устаю смотреть",
            "иногда кажется, что космос нас слушает",
            "ладно, не буду отвлекать, просто скучно",
            "интересно, о чём думают навигаторы в такие часы",
            "тишина на канале, даже помех нет",
            "надо будет потом кресло починить, скрипит",
            "вечером посмотрю старые записи полётов");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void answersEvictedFactsFromCandidatesAndStillQueriesLiveState() throws Exception {
        StringBuilder block = new StringBuilder("\n======== RU FACTS VS QUERY (ladder rule 1 vs rule 2, facts evicted) ========\n");

        // Phase 1: seed the facts.
        for (Seed seed : events) {
            h.gameEvent(seed.type(), seed.summary(), BaseEvent.Importance.HIGH);
        }
        for (String fact : commanderFacts) {
            h.say(fact);
        }

        // Phase 2: evict - filler banter until no fact keyword remains in the hot timeline (Vega's echoes
        // of the facts count too). Guarantees the probes can only be answered via <facts> candidates.
        int fillerUsed = 0;
        for (String line : filler) {
            if (keywordsInShortTerm().isEmpty()) {
                break;
            }
            h.say(line);
            fillerUsed++;
        }
        List<String> leftover = keywordsInShortTerm();
        block.append(String.format("filler turns used: %d | fact keywords still in short-term: %s%n", fillerUsed, leftover));
        for (Probe probe : probes) {
            block.append(String.format("  '%s' tier=%s%n", probe.keyword(), h.locateTier(probe.keyword())));
        }
        assertTrue(leftover.isEmpty(),
                "facts must be evicted from short-term before probing, still hot: " + leftover);

        // Phase 3: ten memory probes - the answers now exist only in mid-term, reachable via <facts>.
        int hits = 0;
        int settledByQuery = 0;
        for (Probe probe : probes) {
            h.beginTurn();
            h.say(probe.question());
            boolean hit = h.spokenContains(probe.keyword());
            List<String> tools = h.turnToolNames();
            // A query_* call on a memory question means rule 2 outran rule 1 (the failing mode under test).
            boolean queried = tools.stream().anyMatch(t -> t.startsWith("query_"));
            if (hit) {
                hits++;
            }
            if (queried) {
                settledByQuery++;
            }
            block.append(String.format("[CMDR] %s%n   ждём '%s' | hit=%s | queried=%s | tools=%s%n   facts=%s%n   -> %s%n",
                    probe.question(), probe.keyword(), hit, queried, tools, h.recallResult(), h.spokenTexts()));
        }

        // Phase 4: control - live state with no fact behind it; the ladder must still reach rule 2.
        h.beginTurn();
        h.say("что у нас сейчас в трюме");
        boolean controlQueried = h.turnToolNames().stream().anyMatch(t -> t.startsWith("query_"));
        block.append(String.format("[CMDR] что у нас сейчас в трюме%n   control-query=%s | tools=%s%n   -> %s%n",
                controlQueried, h.turnToolNames(), h.spokenTexts()));

        block.append(String.format("%nитого: ответы из памяти %d / %d | ушло в query %d / %d | контрольный query: %s%n",
                hits, probes.size(), settledByQuery, probes.size(), controlQueried));
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }

    /** Fact keywords still present anywhere in the hot short-term timeline (any source, echoes included). */
    private List<String> keywordsInShortTerm() {
        List<String> hot = new ArrayList<>();
        List<MemoryEntry> timeline = h.memory().readShortTermTimeline();
        for (Probe probe : probes) {
            String kw = probe.keyword().toLowerCase(Locale.ROOT);
            if (timeline.stream().anyMatch(e -> e.content() != null
                    && e.content().toLowerCase(Locale.ROOT).contains(kw))) {
                hot.add(probe.keyword());
            }
        }
        return hot;
    }
}
