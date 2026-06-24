package elite.intel.companion.input.en;

import elite.intel.companion.input.CompanionEvalHarness;
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
 * Theme 8 (English): real-time responsiveness. For speech (TTS) to feel live, commander phrases and game
 * events must be handled at a real, adequate conversational speed. Part A drives a realistic mixed
 * conversation and times each turn end-to-end (input published -> turn spoken and settled), reporting the
 * per-turn wall-clock latency. Part B fires a stream of inputs at a realistic cadence without waiting between
 * them and measures whether the subsystem keeps up or builds a backlog. Opt-in; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RealTimeResponsivenessEvalTest {

    /** Adequate per-turn ceiling for a live conversation; a gross regression (or an unreachable model) fails. */
    private static final long REALTIME_BUDGET_MS = 12_000;
    /** Realistic spacing between commander phrases / events in the keep-up stream. */
    private static final long STREAM_INTERVAL_MS = 2_500;

    /** A conversation item: a commander phrase, or a game event ({@code event=true}, {@code arg} = summary). */
    private record Item(boolean event, String a, String b) {
        static Item say(String phrase) {
            return new Item(false, phrase, null);
        }
        static Item event(String type, String summary) {
            return new Item(true, type, summary);
        }
    }

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-realtime-eval-trace.txt");

    private final List<Item> conversation = List.of(
            Item.say("what is our current location"),
            Item.event("FSDJump", "jumped to the next system"),
            Item.say("set speed to fifty percent"),
            Item.say("what's in our cargo hold"),
            Item.event("MarketSell", "sold forty tons of gold for profit"),
            Item.say("how many jumps are left on the route"),
            Item.event("FSDJump", "jumped to the next system"),
            Item.say("what's the security level of this system"));

    private final List<String> stream = List.of(
            "what time is it", "what's our heading", "are there contacts on radar",
            "how much cargo space do we have", "what's our fuel status", "what is our current location");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void respondsAtConversationalSpeed() throws Exception {
        // --- Part A: per-turn end-to-end latency on a realistic mixed conversation ---
        List<String> report = new ArrayList<>();
        report.add(String.format("%-44s | %-8s | %s", "turn", "ms", "spoken"));
        report.add("-".repeat(110));
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
            report.add(String.format("%-44s | %-8d | %s",
                    (item.event() ? "EVENT " + item.a() : "\"" + item.a() + "\""), ms, h.spokenTexts()));
        }
        long avg = turnMs.stream().mapToLong(Long::longValue).sum() / turnMs.size();
        long max = turnMs.stream().mapToLong(Long::longValue).max().orElse(0);
        long median = median(turnMs);

        // --- Part B: keep-up under a real-time stream (publish at a fixed cadence, then drain) ---
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
        long backlogMs = drainMs - arrivalSpanMs; // time spent processing beyond the arrival window

        StringBuilder block = new StringBuilder("\n======== REAL-TIME RESPONSIVENESS (theme 8) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("per-turn wall-clock: median %d ms, avg %d ms, max %d ms (budget %d ms)%n",
                median, avg, max, REALTIME_BUDGET_MS));
        block.append(String.format("keep-up stream: %d phrases at %d ms cadence -> drained in %d ms "
                        + "(arrival span %d ms, processing backlog %d ms)%n",
                stream.size(), STREAM_INTERVAL_MS, drainMs, arrivalSpanMs, backlogMs));
        block.append(backlogMs <= arrivalSpanMs ? "kept up with the stream\n" : "FELL BEHIND the stream\n");
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
