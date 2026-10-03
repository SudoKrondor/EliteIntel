package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.vega.model.IntelActionCategory;
import elite.intel.ai.brain.vega.model.llm.LlmToolDefinition;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.TextEmbedder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The first command after start paid ~4 s embedding the catalog before the LLM was asked. Once warmed, a turn
 * must embed nothing but the commander's own words.
 */
class SemanticCatalogWarmerTest {

    private final List<String> embedded = new ArrayList<>();
    private final SemanticPhraseMatcher matcher = new SemanticPhraseMatcher(new TextEmbedder() {
        @Override
        public float[] embed(String text) {
            embedded.add(text);
            return new float[]{text.length(), 1f};
        }

        @Override
        public int dimensions() {
            return 2;
        }
    });
    private final List<GameToolCandidates.Candidate> catalog = List.of(
            candidate("show_navigation_panel", "display navigation panel, open nav panel"),
            candidate("drop_from_super_cruise", "drop right here, drop out"));

    @Test
    void aWarmedCatalogLeavesTheFirstTurnOnlyTheQueryToEmbed() {
        SemanticCatalogWarmer warmer = new SemanticCatalogWarmer(() -> catalog, () -> matcher);
        assertEquals(4, warmer.warm(), "every alias phrase is embedded up front");
        embedded.clear();

        SemanticActionReducer reducer = new SemanticActionReducer(
                allowed -> catalog, () -> matcher, new WordOverlapActionReducer(allowed -> catalog));
        reducer.selectTools(Set.of(IntelActionCategory.ACTION), "display navigation panel");

        assertEquals(List.of("display navigation panel"), embedded,
                "the turn embeds the commander's words and nothing from the catalog");
    }

    @Test
    void aSecondWarmCostsNothing() {
        SemanticCatalogWarmer warmer = new SemanticCatalogWarmer(() -> catalog, () -> matcher);
        warmer.warm();

        assertEquals(0, warmer.warm(), "a language change re-warms only what is new");
    }

    @Test
    void anUnavailableModelIsNotAFailure() {
        assertEquals(-1, new SemanticCatalogWarmer(() -> catalog, () -> null).warm());
    }

    private static GameToolCandidates.Candidate candidate(String id, String aliases) {
        return new GameToolCandidates.Candidate(id, aliases, new LlmToolDefinition(id, "desc", aliases, List.of()));
    }
}
