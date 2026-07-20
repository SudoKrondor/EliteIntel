package elite.intel.ai.brain.vega.input.ru;

import elite.intel.ai.brain.vega.input.CompanionEvalHarness;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;
import elite.intel.db.FuzzySearch;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Theme (Russian): the explicit memory enumeration. We dock at five stations, interleaved with chatter and a
 * command so the events arrive non-sequentially, then ask the companion to list which stations we docked at
 * and how many. The test asserts both the route and outcome: {@code memory_search} must be called, and the commander
 * must hear every docked station enumerated because durable memory is never injected as prompt facts. Station names are
 * matched tolerantly (one transliteration edit, e.g. "Уолш"/"Волш") since the model re-voices them in persona.
 * Opt-in; LM Studio must be up.
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
        // dedup) rather than replayed as live gameplay - in actual play dockings at five stations are minutes
        // apart and all land; here they are recorded back-to-back. This targets memory_search enumeration.
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

        // Ask for the full enumeration. The commander must route through memory_search and hear every station.
        h.beginTurn();
        h.say("перечисли, к каким станциям мы пристыковывались, и сколько их было");

        boolean searched = h.called("memory_search");
        // memory_search self-voices its analysis; see CompanionEvalHarness#spokenTexts.
        String answer = String.join(" ", h.spokenTexts());
        List<String> answerWords = words(answer);
        // The analysis model composes in-persona and may shorten "Джеймсон Мемориал" to "Джеймсон", so match
        // each station by its distinctive first word, tolerant to one transliteration edit (see stationHeard).
        long found = STATIONS.stream()
                .map(s -> s.split(" ")[0].toLowerCase(Locale.ROOT))
                .filter(firstWord -> stationHeard(answerWords, firstWord))
                .count();

        StringBuilder block = new StringBuilder("\n======== RU MEMORY SEARCH (docking enumeration) ========\n");
        block.append("tools this turn: ").append(h.turnToolNames()).append("\n");
        block.append("memory_search called: ").append(searched).append("\n");
        block.append(String.format("stations found in answer: %d / %d%n", found, STATIONS.size()));
        block.append("answer: ").append(answer).append("\n");
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
        assertTrue(searched, "durable recall must use memory_search");
        assertEquals(STATIONS.size(), found, "the commander must hear every docked station enumerated");
    }

    /**
     * A station is heard if any spoken word matches its first word exactly, or is one transliteration
     * substitution away: the same length with a single differing letter ("Уолш"/"Волш"). The same-length
     * guard keeps the tolerance to a letter swap and avoids a short word like "уолш" matching an unrelated
     * word an insertion/deletion away.
     */
    private static boolean stationHeard(List<String> spokenWords, String stationFirstWord) {
        return spokenWords.stream().anyMatch(w ->
                w.length() == stationFirstWord.length() && FuzzySearch.levenshteinDistance(w, stationFirstWord) <= 1);
    }

    /**
     * Lower-cased word tokens of the spoken answer (punctuation stripped) for station matching.
     */
    private static List<String> words(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(w -> !w.isEmpty())
                .toList();
    }

    /**
     * Records one docking as an EVENT memory entry via the real gateway write path (embedding + dedup).
     */
    private void dock(String station) {
        h.memory().write(MemoryRecord.event(
                Instant.now(), "Пристыковались к станции " + station + "."));
    }
}
