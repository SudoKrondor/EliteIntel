package elite.intel.companion.input.ru;

import elite.intel.companion.CompanionConfig;
import elite.intel.companion.input.CompanionEvalHarness;
import elite.intel.companion.memory.CompanionMemoryLimits;
import elite.intel.companion.memory.HeuristicTokenEstimator;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.gameapi.SensorDataEvent;
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
 * Theme (Russian): the {@code NarrationThought} volume / prompt-bloat eval, mirrored to a Russian session
 * (pinned to RU before boot) with 10 payloads shaped like the real subscribers emit them in Russian (route
 * arrivals, trade reminders, SAA exobiology signals, a full system body dump, organic samples, mission,
 * codex, carrier jump). It assesses the same two things as the English test: a large incoming payload must be
 * phrased into one short spoken line (narration compresses, not echoes), and short-term memory must stay
 * within its entry cap and token budget so it never bloats the commander prompt. Per turn it also re-checks
 * the contract: one LLM round, only the narration system tools. Opt-in via the local-integration tag;
 * LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NarrationEvalTest {

    /** A narration payload shaped like its real subscriber: pre-digested {@code data} + a narration {@code instruction}. */
    private record Item(String name, String data, String instruction, String topic) {}

    /** Same character heuristic the memory gateway uses, so the footprint numbers match the real eviction. */
    private static final HeuristicTokenEstimator EST = new HeuristicTokenEstimator();

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-narration-eval-trace.txt", Language.RU);

    private final List<Item> items = List.of(
            new Item("route-arrival",
                    "Прибыли в систему Шинрарта Дежра. Следующая точка маршрута Соль, класс звезды G, можно заправиться. Осталось 6 прыжков.",
                    "Объяви эту информацию о маршруте.", SensorDataEvent.TOPIC_NAVIGATION),
            new Item("trade-buy",
                    "Направляйтесь к станции Jameson Memorial, купите тритий.",
                    "Напомни командиру об активном торговом маршруте: назови станцию и товар для покупки.",
                    SensorDataEvent.TOPIC_TRADE),
            new Item("trade-sell",
                    "Направляйтесь к станции Ray Gateway, продайте золото.",
                    "Напомни командиру об активном торговом маршруте: назови станцию и товар для продажи.",
                    SensorDataEvent.TOPIC_TRADE),
            new Item("saa-signals",
                    "Обнаружены сигналы. Тип сигнала: геологический. Тип сигнала: биологический. Экзобиология: три формы. "
                            + "Bacterium Aurasus, Stratum Tectonicas, Tussock Pennata, средняя ожидаемая выплата "
                            + "18 500 000 кредитов. Бонус за первое открытие 92 000 000 кредитов.",
                    """
                            Сообщи о сигналах, обнаруженных на этом теле. Кратко перечисли типы сигналов.
                            Если есть биологические сигналы, назови каждый род и среднюю ожидаемую выплату.
                            Если это наше первое открытие, добавь бонус за первооткрытие.
                            """, SensorDataEvent.TOPIC_SYSTEM),
            new Item("system-scan-dump",
                    // A deliberately large, realistic body dump - the kind of full-volume data an arrival can carry.
                    "Сканирование системы завершено, восемь тел. "
                            + "Тело A 1: планета с высоким содержанием металлов, посадка возможна, гравитация 0.31g, "
                            + "1280 кельвинов, пригодна для терраформирования, материалы железо, никель, сера, углерод, фосфор, марганец. "
                            + "Тело A 2: водный мир, посадка невозможна, гравитация 1.12g, 290 кельвинов, атмосфера азот кислород. "
                            + "Тело A 3: газовый гигант с жизнью на водной основе, два кольца, запасы нетронутые. "
                            + "Тело A 4: каменистое тело, посадка возможна, гравитация 0.08g, 180 кельвинов, материалы сурьма, полоний. "
                            + "Тело A 5: аммиачный мир, посадка невозможна, гравитация 1.4g, 240 кельвинов. "
                            + "Тело B 1: тело, богатое металлами, посадка возможна, гравитация 0.22g, 720 кельвинов, материалы рутений, теллур. "
                            + "Тело B 2: ледяное тело с биологическими и геологическими сигналами, посадка возможна, гравитация 0.05g. "
                            + "Два тела остались не отсканированы.",
                    "Кратко изложи командиру результат сканирования системы одной строкой; не перечисляй каждое тело.",
                    SensorDataEvent.TOPIC_SYSTEM),
            new Item("organic-sample",
                    "Образец взят. Stratum Tectonicas, образец два из трёх. Для следующего образца отойдите минимум на 500 метров.",
                    "Сообщи командиру о прогрессе взятия образцов.", SensorDataEvent.TOPIC_SYSTEM),
            new Item("mission-accepted",
                    "Принята миссия: уничтожить 8 пиратов фракции Code в системе LP 132-9. Награда 4 200 000 кредитов.",
                    "Кратко подтверди принятую миссию: цель и награду.", SensorDataEvent.TOPIC_SYSTEM),
            new Item("codex-entry",
                    "Запись в кодекс: новый вид Tussock Caputus, обнаружен на планете A 2. Это наше первое открытие.",
                    "Объяви запись в кодекс одной короткой фразой.", SensorDataEvent.TOPIC_SYSTEM),
            new Item("carrier-jump",
                    "Носитель совершил прыжок в сектор Coalsack. Прибытие завершено.",
                    "Объяви о завершении прыжка носителя.", SensorDataEvent.TOPIC_NAVIGATION),
            new Item("route-arrival-2",
                    "Прибыли в систему LHS 20. Следующая точка Wolf 359, класс M, можно заправиться. Осталось 3 прыжка.",
                    "Объяви эту информацию о маршруте.", SensorDataEvent.TOPIC_NAVIGATION));

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void narratesRussianRealEventsCompactlyWithoutBloatingShortTermMemory() throws Exception {
        StringBuilder block = new StringBuilder("\n======== RU NARRATION volume / bloat (real events) ========\n");
        block.append(String.format("%-18s | in(ch/tok) | out(ch/tok) | round | tools | spoken%n", "event"));
        block.append("-".repeat(130)).append("\n");

        int spokeCount = 0;
        int contractOk = 0;
        for (Item it : items) {
            long roundsBefore = h.roundCount();
            h.narrate(it.data(), it.instruction(), it.topic());
            long rounds = h.roundCount() - roundsBefore; // narration is a single short round

            int inChars = it.data().length() + it.instruction().length();
            String spoken = String.join(" ", h.spokenTexts());
            boolean spoke = !spoken.isBlank();
            boolean oneRound = rounds == 1;
            boolean onlyNarrationTools = h.turnToolNames().stream()
                    .allMatch(t -> t.equals("speak") || t.equals("nothing_to_do"));
            if (spoke) {
                spokeCount++;
            }
            if (spoke && oneRound && onlyNarrationTools) {
                contractOk++;
            }
            block.append(String.format("%-18s | %4d/%-4d | %4d/%-4d | %-5s | %-5s | %s%n",
                    it.name(),
                    inChars, EST.estimate(it.data()) + EST.estimate(it.instruction()),
                    spoken.length(), EST.estimate(spoken),
                    oneRound ? "1" : String.valueOf(rounds),
                    onlyNarrationTools ? "ok" : "BAD",
                    spoken));
            block.append(h.memoryDeltaBlock()); // narration records only the spoken [companion] line
        }

        // Final short-term footprint: what narration actually leaves in the prompt's timeline block.
        List<MemoryEntry> timeline = h.memory().readShortTermTimeline();
        int entries = timeline.size();
        int footprintTokens = timeline.stream()
                .mapToInt(e -> EST.estimate(e.content()) + CompanionMemoryLimits.SHORT_TERM_ENTRY_FRAMING_OVERHEAD_TOKENS)
                .sum();
        int maxEntryTokens = timeline.stream().mapToInt(e -> EST.estimate(e.content())).max().orElse(0);

        block.append(String.format("%ncontract ok (spoke, 1 round, narration tools): %d / %d%n", contractOk, items.size()));
        block.append(String.format("short-term: %d / %d entries, footprint ~%d tok (budget %d), largest entry ~%d tok%n",
                entries, CompanionConfig.shortTermMemorySize(), footprintTokens,
                CompanionMemoryLimits.SHORT_TERM_TOKEN_BUDGET, maxEntryTokens));
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
        assertTrue(spokeCount > 0, "narration never produced a spoken line - see the trace");
        // No prompt bloat: short-term honors its entry cap, and its token footprint stays within budget
        // (the sole-newest-entry exception aside) - a large incoming payload does not bloat the timeline.
        assertTrue(entries <= CompanionConfig.shortTermMemorySize(),
                "short-term exceeded its entry cap: " + entries + " > " + CompanionConfig.shortTermMemorySize());
        assertTrue(footprintTokens <= CompanionMemoryLimits.SHORT_TERM_TOKEN_BUDGET || entries <= 1,
                "short-term footprint ~" + footprintTokens + " tok exceeded budget "
                        + CompanionMemoryLimits.SHORT_TERM_TOKEN_BUDGET + " with " + entries + " entries");
    }
}
