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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Theme 8 (Russian): real-time responsiveness under Russian commander phrases and mixed game events.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RealTimeResponsivenessEvalTest {

    private static final long REALTIME_BUDGET_MS = 12_000;
    private static final long STREAM_INTERVAL_MS = 2_500;

    private record Item(boolean event, String a, String b) {
        static Item say(String phrase) {
            return new Item(false, phrase, null);
        }
        static Item event(String type, String summary) {
            return new Item(true, type, summary);
        }
    }

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-realtime-eval-trace.txt", Language.RU);

    private final List<Item> conversation = List.of(
            Item.say("где мы сейчас находимся"),
            Item.event("FSDJump", "jumped to the next system"),
            Item.say("установи скорость пятьдесят процентов"),
            Item.say("что у нас в трюме"),
            Item.event("MarketSell", "sold forty tons of gold for profit"),
            Item.say("сколько прыжков осталось по маршруту"),
            Item.event("FSDJump", "jumped to the next system"),
            Item.say("какой уровень безопасности в этой системе"));

    private final List<String> stream = List.of(
            "который час", "какой у нас курс", "есть ли контакты на радаре",
            "сколько у нас свободного места в трюме", "какой у нас запас топлива", "где мы сейчас находимся");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void respondsToRussianAtConversationalSpeed() throws Exception {
        List<String> report = new ArrayList<>();
        report.add(String.format("%-54s | %-8s | %s", "turn", "ms", "spoken"));
        report.add("-".repeat(120));
        List<Long> turnMs = new ArrayList<>();
        for (Item item : conversation) {
            long t0 = System.nanoTime();
            if (item.event()) {
                h.gameEvent(item.a(), item.b());
            } else {
                h.say(item.a());
            }
            long ms = (System.nanoTime() - t0) / 1_000_000;
            turnMs.add(ms);
            report.add(String.format("%-54s | %-8d | %s",
                    (item.event() ? "EVENT " + item.a() : "\"" + item.a() + "\""), ms, h.spokenTexts()));
        }
        long avg = turnMs.stream().mapToLong(Long::longValue).sum() / turnMs.size();
        long max = turnMs.stream().mapToLong(Long::longValue).max().orElse(0);
        long median = median(turnMs);

        long streamStart = System.nanoTime();
        for (int i = 0; i < stream.size(); i++) {
            h.publishInput(stream.get(i));
            if (i < stream.size() - 1) {
                Thread.sleep(STREAM_INTERVAL_MS);
            }
        }
        h.awaitIdle();
        long drainMs = (System.nanoTime() - streamStart) / 1_000_000;
        long arrivalSpanMs = (stream.size() - 1) * STREAM_INTERVAL_MS;
        long backlogMs = drainMs - arrivalSpanMs;

        StringBuilder block = new StringBuilder("\n======== RU REAL-TIME RESPONSIVENESS (theme 8) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("per-turn wall-clock: median %d ms, avg %d ms, max %d ms (budget %d ms)%n",
                median, avg, max, REALTIME_BUDGET_MS));
        block.append(String.format("keep-up stream: %d phrases at %d ms cadence -> drained in %d ms "
                        + "(arrival span %d ms, processing backlog %d ms)%n",
                stream.size(), STREAM_INTERVAL_MS, drainMs, arrivalSpanMs, backlogMs));
        block.append(backlogMs <= arrivalSpanMs ? "kept up with the stream\n" : "FELL BEHIND the stream\n");
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(turnMs.isEmpty(), "no turns were timed");
        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
        assertTrue(median < REALTIME_BUDGET_MS,
                "median per-turn latency " + median + " ms exceeds the real-time budget " + REALTIME_BUDGET_MS + " ms");
    }

    private static long median(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        int n = sorted.size();
        return n == 0 ? 0 : n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
    }
}
