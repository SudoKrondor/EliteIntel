package elite.intel.companion.input.ru;

import elite.intel.companion.input.CompanionEvalHarness;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Theme (Russian): the comprehensive memory eval, mirrored to a Russian session and with twice the probes of
 * the English one. A believable Russian salvage-run conversation (terminology drawn from the RU Elite
 * Dangerous community) interleaves statements across many topics, explicit "запиши/запомни" instructions
 * (which the consciousness should rate {@code MAX}), routine chatter, and HIGH game events that land in
 * memory under their static topic. From one run it assesses filling &amp; recall (including after eviction),
 * importance distribution (explicit "запиши" -&gt; MAX and idle banter -&gt; LOW), topic distribution, events
 * in memory, coherence, and live-state query routing.
 * Mostly observational (the trace carries the scores and the full distribution); the hard assertions are only
 * that the model was reached and that recall works at all. Opt-in; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemoryEvalTest {

    private enum Kind { SAY, EVENT, ASK }

    /** SAY: {@code a}=commander line. EVENT: {@code a}=journal type, {@code b}=summary. ASK: {@code a}=question, {@code b}=expected keyword. */
    private record Turn(Kind kind, String a, String b) {}

    private static Turn say(String line) { return new Turn(Kind.SAY, line, null); }
    private static Turn event(String type, String summary) { return new Turn(Kind.EVENT, type, summary); }
    private static Turn ask(String question, String expect) { return new Turn(Kind.ASK, question, expect); }

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-memory-eval-trace.txt", Language.RU);

    private final List<Turn> script = List.of(
            say("значит так, в этот рейс у нас тихая работа по утилю за Дециатом, держим всё мимо журналов"),
            say("запиши: код стыковки на станции — Сьерра Девять Четыре, понадобится на подходе"),
            event("FSDJump", "прибыли в систему Вольф 359"),
            say("покупатель утиля — Халлоран, он наш контакт на местном рынке"),
            say("тихо тут, красота, как я люблю"),
            say("если зажмут пираты, кодовое слово на отход — Гранит, заруби на носу"),
            event("ShipTargeted", "просканирован разыскиваемый пират по имени Варгас"),
            ask("повтори, какое кодовое слово на отход?", "гранит"),  // ещё горячо
            say("пока мы тут, наша цель по добыче — низкотемпературные алмазы, остальное мимо"),
            say("поле астероидов, что отрабатываем, на картах зовётся Бедлам"),
            event("ProspectedAsteroid", "найден астероид, богатый платиной"),
            say("прикрывает нас старина Сорока, держись поближе к его крылу"),
            say("запомни: аварийная точка встречи — Хаттон Орбитал, если вдруг разделимся"),
            event("MissionAccepted", "принята боевая миссия против фракции Алый Картель"),
            say("работаем на синдикат Мокошь, это они платят за рейс"),
            say("и запиши: операция проходит как Отлив, в отчётах только так"),
            ask("напомни нашу цель по добыче", "алмазы"),  // ещё горячо
            say("засекли аммиачный мир, окрестили его Фонарь"),
            event("ScanOrganic", "взят образец организма Stratum Tectonicas"),
            say("как сам, держишься там?"),
            say("дозаправку делаем у нейтронной звезды, её зовут Веретено, на обратном пути"),
            event("MarketSell", "продали сорок тонн осмия на рынке"),
            say("наш пилот истребителя в малом корабле — Оконкво"),
            say("FSD везём к инженеру Фелисити Фарсир на настройку"),
            event("Docked", "пристыковались к станции Джеймсон Мемориал"),
            say("идём не главными трассами, а тихой дорогой, зовём её Объезд"),
            event("SAASignalsFound", "обнаружены биосигналы рода Светляк на планете"),
            say("и наконец окрестили корабль — Странствующий Альбатрос"),
            say("связь держим тихо до самой станции"));

    // 12 recall probes - by probe time the facts are out of the hot timeline, so a real recall is needed.
    private final List<Turn> recallProbes = List.of(
            ask("какой у нас код стыковки на станции?", "сьерра"),       // MAX
            ask("где аварийная точка встречи, если разделимся?", "хаттон"), // MAX
            ask("как называется наша операция?", "отлив"),               // MAX
            ask("кто покупатель утиля?", "халлоран"),
            ask("какая у нас цель по добыче в этот рейс?", "алмазы"),
            ask("кто нас прикрывает?", "сорока"),
            ask("как зовут пилота истребителя?", "оконкво"),
            ask("как мы назвали аммиачный мир?", "фонарь"),
            ask("у какого инженера настраиваем FSD?", "фарсир"),
            ask("как называется поле астероидов?", "бедлам"),
            ask("как мы окрестили корабль?", "альбатрос"),
            ask("на какой синдикат мы работаем?", "мокошь"));

    // 4 live-state probes: must route to a query function, not a memory recall.
    private final List<String> queryProbes = List.of(
            "сколько у нас топлива", "что сейчас в трюме", "где мы находимся", "какой уровень безопасности в системе");

    // 8 keywords planted only by events, to check each HIGH event landed in some memory tier.
    private final List<String> eventKeywords = List.of(
            "вольф", "варгас", "платин", "картель", "tectonicas", "осми", "джеймсон", "светляк");

    // 10 idle-banter probes carrying no fact, name or command - the consciousness should rate each LOW.
    private final List<String> lowProbes = List.of(
            "ну и тишина сегодня, аж в ушах звенит",
            "обожаю такие спокойные вылеты, душа отдыхает",
            "как настроение, не заскучал там у себя?",
            "красивая туманность за бортом, глаз не отвести",
            "да я просто болтаю, чтоб тишину разбавить",
            "ты вообще когда-нибудь отдыхаешь или всё на вахте?",
            "за такие минуты покоя и люблю эту работу",
            "кофе бы сейчас, да автомат опять чудит",
            "хех, вспомнил тут одну байку, да ладно, потом",
            "просто хотел услышать твой голос, всё нормально");

    /** System-function ids; any other executed tool is a real game query/action. */
    private static final Set<String> SYSTEM_TOOLS = Set.of(
            "speak", "nothing_to_do", "classify_turn", "change_verbosity", "clarify",
            "search_in_memory");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void remembersFillsDistributesAndRecallsAcrossARussianConversation() throws Exception {
        StringBuilder block = new StringBuilder("\n======== RU COMPREHENSIVE MEMORY (live conversation) ========\n");

        // Phase 1: play the session. ASK turns here are scored as in-conversation (hot) recall.
        int hotHits = 0;
        int hotAsks = 0;
        for (Turn turn : script) {
            switch (turn.kind()) {
                case SAY -> {
                    h.say(turn.a());
                    block.append("[CMDR] ").append(turn.a()).append("\n");
                    h.spokenTexts().forEach(s -> block.append("[VEGA] ").append(s).append("\n"));
                }
                case EVENT -> {
                    h.gameEvent(turn.a(), turn.b(), BaseEvent.Importance.HIGH);
                    block.append("[EVENT ").append(turn.a()).append("] ").append(turn.b()).append("\n");
                }
                case ASK -> {
                    hotAsks++;
                    h.say(turn.a());
                    boolean hit = h.spokenContains(turn.b());
                    if (hit) {
                        hotHits++;
                    }
                    block.append("[CMDR] ").append(turn.a()).append("\n");
                    block.append("    -> ждём '").append(turn.b()).append("' hot-hit=").append(hit).append(" | ").append(h.spokenTexts()).append("\n");
                }
            }
            block.append("    tools=").append(h.turnToolNames()).append("\n"); // shows classify_turn if called
            block.append(h.memoryDeltaBlock()); // what this turn wrote: [source][topic][importance] content
        }

        // Phase 2: recall probes after eviction.
        block.append("\n---- recall после вытеснения ----\n");
        int recallHits = 0;
        int recalledCount = 0;
        for (Turn probe : recallProbes) {
            String tier = h.locateTier(probe.b());
            h.beginTurn();
            h.say(probe.a());
            boolean hit = h.spokenContains(probe.b());
            boolean recalled = h.recalled();
            if (hit) {
                recallHits++;
            }
            if (recalled) {
                recalledCount++;
            }
            block.append(String.format("ждём '%s' | tier=%s | recalled=%s | hit=%s | %s%n",
                    probe.b(), tier, recalled, hit, h.spokenTexts()));
        }

        // Phase 3: two coherence probes, each weaving together two separately-stated facts.
        block.append("\n---- связность ----\n");
        int coherenceHits = 0;
        h.beginTurn();
        h.say("напомни кодовое слово на отход и где встречаемся при разделении");
        String c1 = String.join(" ", h.spokenTexts()).toLowerCase(Locale.ROOT);
        boolean c1ok = c1.contains("гранит") && c1.contains("хаттон");
        if (c1ok) {
            coherenceHits++;
        }
        block.append("ждём 'гранит'+'хаттон' | ok=").append(c1ok).append(" | ").append(h.spokenTexts()).append("\n");

        h.beginTurn();
        h.say("кто платит за работу и кто покупатель утиля");
        String c2 = String.join(" ", h.spokenTexts()).toLowerCase(Locale.ROOT);
        boolean c2ok = c2.contains("мокошь") && c2.contains("халлоран");
        if (c2ok) {
            coherenceHits++;
        }
        block.append("ждём 'мокошь'+'халлоран' | ok=").append(c2ok).append(" | ").append(h.spokenTexts()).append("\n");

        // Phase 4: events landed in memory.
        block.append("\n---- события в памяти ----\n");
        int eventsLanded = 0;
        for (String kw : eventKeywords) {
            String tier = h.locateTier(kw);
            boolean landed = !"LOST".equals(tier);
            if (landed) {
                eventsLanded++;
            }
            block.append(String.format("ключ события '%s' | tier=%s%n", kw, tier));
        }

        // Phase 5: live-state routing - must use a query, not memory.
        block.append("\n---- маршрутизация live-state (не из памяти) ----\n");
        int routedOk = 0;
        for (String q : queryProbes) {
            h.beginTurn();
            h.say(q);
            List<String> tools = h.turnToolNames();
            boolean usedQuery = tools.stream().anyMatch(t -> !SYSTEM_TOOLS.contains(t));
            boolean ok = usedQuery && !h.recalled();
            if (ok) {
                routedOk++;
            }
            block.append(String.format("%-44s | tools=%s | routed-ok=%s%n", q, tools, ok));
        }

        // Phase 6: idle small talk - the consciousness should rate banter LOW (no fact to keep).
        block.append("\n---- болтовня -> LOW ----\n");
        int lowHits = 0;
        for (String line : lowProbes) {
            h.beginTurn();
            h.say(line);
            String imp = h.assignedImportance();
            if ("low".equalsIgnoreCase(imp)) {
                lowHits++;
            }
            block.append(String.format("'%s' | importance=%s%n", line, imp.isEmpty() ? "(none)" : imp));
        }

        // Did the explicit "запиши/запомни" facts get MAX importance from the AI?
        long maxAssigned = h.allEntries().stream()
                .filter(e -> e.importance() == MemoryImportance.MAX)
                .filter(e -> e.content().contains("сьерра") || e.content().contains("хаттон") || e.content().contains("отлив"))
                .count();

        block.append("\n---- итоги ----\n");
        block.append(String.format("горячий recall:        %d / %d%n", hotHits, hotAsks));
        block.append(String.format("recall после вытеснения: %d / %d (search_in_memory вызван %d)%n", recallHits, recallProbes.size(), recalledCount));
        block.append(String.format("связность (2 факта):   %d / 2%n", coherenceHits));
        block.append(String.format("события записаны:      %d / %d%n", eventsLanded, eventKeywords.size()));
        block.append(String.format("маршрутизация:         %d / %d%n", routedOk, queryProbes.size()));
        block.append(String.format("явное «запиши/запомни» -> MAX: %d (из 3)%n", maxAssigned));
        block.append(String.format("болтовня -> LOW:       %d / %d%n", lowHits, lowProbes.size()));
        block.append(h.memoryDistributionBlock());
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
        assertTrue(recallHits > 0, "no fact was recalled after eviction - see the trace");
    }
}
