package elite.intel.companion.input.ru;

import elite.intel.companion.input.CompanionEvalHarness;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Theme (Russian): the explicit memory query. We dock at five stations, interleaved with chatter and a
 * command so the events arrive non-sequentially, then ask the companion to list which stations we docked at
 * and how many. This is exactly the case the capped auto-injected candidates (top few) cannot answer: it must
 * route to {@code memory_search}, which deterministically recalls EVERY match; the analysis model then voices
 * the full set. Opt-in; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemorySearchEvalTest {

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-memory-search-trace.txt", Language.RU);

    private static final List<String> STATIONS = List.of(
            "Джеймсон Мемориал", "Дейзи Дюваль", "Абрахам Линкольн", "Хаттон Орбитал", "Уолш Хаб");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void enumeratesEveryDockedStationFromMemory() throws Exception {
        // Record five docking events into memory, interleaved with chatter and a command so they are scattered
        // through the timeline. The events are written straight to the gateway (the real write path: embedding +
        // dedup) rather than published, to skip GameEventFilter's per-type real-time cooldown - in actual play
        // dockings at five stations are minutes apart and all land; here they would fire within one cooldown
        // window. This targets memory_search enumeration, not the event filter.
        dock(STATIONS.get(0));
        h.say("красиво тут, полюбуемся немного");
        dock(STATIONS.get(1));
        h.say("наша цель по добыче — низкотемпературные алмазы");
        dock(STATIONS.get(2));
        h.say("открой карту галактики");
        dock(STATIONS.get(3));
        h.say("как настроение, дружище?");
        dock(STATIONS.get(4));
        h.say("тихо сегодня, поболтаем ещё немного");

        // The explicit enumeration request must route to memory_search (not answer from the few auto-candidates).
        h.beginTurn();
        h.say("перечисли, к каким станциям мы пристыковывались, и сколько их было");

        boolean called = h.called("memory_search");
        String answer = h.callsNamed("memory_search").stream()
                .findFirst()
                .map(e -> e.result().has("text_to_speech_response")
                        ? e.result().get("text_to_speech_response").getAsString() : "")
                .orElse("");
        String lower = answer.toLowerCase(Locale.ROOT);
        // The analysis model composes the answer in-persona and may shorten "Джеймсон Мемориал" to "Джеймсон",
        // so match each station by its distinctive first word rather than the full name.
        long found = STATIONS.stream()
                .map(s -> s.split(" ")[0].toLowerCase(Locale.ROOT))
                .filter(lower::contains)
                .count();

        StringBuilder block = new StringBuilder("\n======== RU MEMORY SEARCH (docking enumeration) ========\n");
        block.append("tools this turn: ").append(h.turnToolNames()).append("\n");
        block.append("memory_search called: ").append(called).append("\n");
        block.append(String.format("stations found in answer: %d / %d%n", found, STATIONS.size()));
        block.append("answer: ").append(answer).append("\n");
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
        assertTrue(called, "the explicit memory enumeration must route to memory_search");
        assertEquals(STATIONS.size(), found, "every docked station must appear in the memory_search answer");
    }

    /** Records one docking as an EVENT memory entry via the real gateway write path (embedding + dedup). */
    private void dock(String station) {
        h.memory().write(new MemoryEntry(
                Instant.now(), ConversationTopic.NAVIGATION, MemorySource.EVENT, "пристыковались к станции " + station));
    }
}
