package elite.intel.companion.prompt;

import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolDefinition;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the selection/narrowing logic in isolation (candidates injected, no registries/singletons):
 * word-overlap narrowing, "blank input offers all", legacy-fallback stripping, and ordered, de-duplicated
 * output. Category gating lives in {@link GameToolCandidates} and is tested there.
 */
class WordOverlapActionReducerTest {

    private static final Set<IntelActionCategory> ALL = EnumSet.allOf(IntelActionCategory.class);

    private static GameToolCandidates.Candidate candidate(String id, String phraseKey) {
        return new GameToolCandidates.Candidate(id, phraseKey,
                new LlmToolDefinition(id, "desc", phraseKey, List.of()));
    }

    /** Fixed three-tool catalog; the source ignores categories (gating is tested separately). */
    private final List<GameToolCandidates.Candidate> catalog = List.of(
            candidate("navigate", "navigate, plot course"),
            candidate("trade", "open market, sell cargo"),
            candidate("ship_status", "ship status report"));

    private final WordOverlapActionReducer reducer = new WordOverlapActionReducer(allowed -> catalog);

    private static List<String> ids(List<LlmToolDefinition> tools) {
        return tools.stream().map(LlmToolDefinition::name).toList();
    }

    @Test
    void narrowsToWordOverlapMatch() {
        List<LlmToolDefinition> tools = reducer.selectTools(ALL, "open the market please");
        assertEquals(List.of("trade"), ids(tools));
    }

    @Test
    void blankInputOffersAllInOrder() {
        List<LlmToolDefinition> tools = reducer.selectTools(ALL, "");
        assertEquals(List.of("navigate", "trade", "ship_status"), ids(tools));
    }

    @Test
    void noMatchStripsFallbackToEmpty() {
        // No overlap -> legacy reducer injects its ignore-nonsensical fallback, which must be stripped.
        List<LlmToolDefinition> tools = reducer.selectTools(ALL, "qwerty zxcvb");
        assertTrue(tools.isEmpty(), "fallback id must not surface as a game tool");
    }

    @Test
    void exactPhraseMatchReturnsSingleToolWithoutDuplicates() {
        // Exact alias match plus word overlap both point at "trade"; result must contain it exactly once.
        List<LlmToolDefinition> tools = reducer.selectTools(ALL, "open market");
        assertEquals(List.of("trade"), ids(tools));
    }

    @Test
    void selectionFollowsPhrasesNotTheDescription() {
        // Phrases and the LLM description deliberately share no words, isolating what drives selection: the
        // reducer only ever sees the phrase key, never the description sent to the provider.
        GameToolCandidates.Candidate widget = new GameToolCandidates.Candidate("widget_op", "frobnicate widget",
                new LlmToolDefinition("widget_op", "Adjust the ship lights.", "frobnicate widget", List.of()));
        WordOverlapActionReducer onlyWidget = new WordOverlapActionReducer(allowed -> List.of(widget));

        // A word from the PHRASES selects the tool, and the offered description stays the purpose, not the phrases.
        List<LlmToolDefinition> byPhrase = onlyWidget.selectTools(ALL, "please frobnicate it");
        assertEquals(List.of("widget_op"), ids(byPhrase));
        assertEquals("Adjust the ship lights.", byPhrase.get(0).description());

        // Words from the DESCRIPTION only (not the phrases) select nothing - the description never drives selection.
        List<LlmToolDefinition> byDescription = onlyWidget.selectTools(ALL, "adjust the ship lights");
        assertTrue(byDescription.isEmpty());
    }
}
