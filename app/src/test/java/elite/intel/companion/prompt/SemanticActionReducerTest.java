package elite.intel.companion.prompt;

import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.TextEmbedder;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolDefinition;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies meaning-based selection in isolation (candidates injected, a synthetic embedder, no model/singletons):
 * narrows to the closest-by-meaning candidate, keeps the relative band, returns nothing below the floor, and
 * degrades to the injected word-overlap fallback on a blank input, an unavailable matcher, or an embed failure.
 */
class SemanticActionReducerTest {

    private static final Set<IntelActionCategory> ALL = EnumSet.allOf(IntelActionCategory.class);

    private static GameToolCandidates.Candidate candidate(String id, String phraseKey) {
        return new GameToolCandidates.Candidate(id, phraseKey,
                new LlmToolDefinition(id, "desc", phraseKey, List.of()));
    }

    private final List<GameToolCandidates.Candidate> catalog = List.of(
            candidate("navigate", "navigate, plot course"),
            candidate("trade", "open market, sell cargo"),
            candidate("ship_status", "ship status report"));

    /** Synthetic embedder: known phrases map to fixed axis vectors; anything else maps far from all of them. */
    private static TextEmbedder embedder(Map<String, float[]> table) {
        return new TextEmbedder() {
            @Override public float[] embed(String text) {
                float[] v = table.get(text);
                return v != null ? v : new float[]{1, 1, 1};
            }
            @Override public int dimensions() {
                return 3;
            }
        };
    }

    private static final Map<String, float[]> VECTORS = Map.of(
            "navigate", new float[]{1, 0, 0},
            "plot course", new float[]{1, 0, 0},
            "open market", new float[]{0, 1, 0},
            "sell cargo", new float[]{0, 1, 0},
            "ship status report", new float[]{0, 0, 1},
            "GO_NAV", new float[]{1, 0, 0});

    private SemanticActionReducer reducerWith(Supplier<SemanticPhraseMatcher> matcherSupplier,
                                              CompanionActionReducer fallback) {
        return new SemanticActionReducer(allowed -> catalog, matcherSupplier, fallback);
    }

    private SemanticActionReducer reducer(CompanionActionReducer fallback) {
        SemanticPhraseMatcher matcher = new SemanticPhraseMatcher(embedder(VECTORS));
        return reducerWith(() -> matcher, fallback);
    }

    private static List<String> ids(List<LlmToolDefinition> tools) {
        return tools.stream().map(LlmToolDefinition::name).toList();
    }

    /** A fallback that must never be reached when semantic selection runs; flips a flag if it is. */
    private static CompanionActionReducer unusedFallback(AtomicBoolean used) {
        return (categories, input) -> {
            used.set(true);
            return List.of();
        };
    }

    @Test
    void narrowsToClosestByMeaning() {
        AtomicBoolean usedFallback = new AtomicBoolean();
        List<LlmToolDefinition> tools = reducer(unusedFallback(usedFallback)).selectTools(ALL, "GO_NAV");
        assertEquals(List.of("navigate"), ids(tools));
        assertTrue(!usedFallback.get(), "semantic path must not fall back when the matcher is available");
    }

    @Test
    void belowFloorOffersNoTools() {
        // An input far from every candidate (best cosine under the floor) yields no game tools.
        List<LlmToolDefinition> tools = reducer(unusedFallback(new AtomicBoolean())).selectTools(ALL, "unrelated");
        assertTrue(tools.isEmpty(), "nothing close enough in meaning -> no game tools");
    }

    @Test
    void keepsTiedBestMatchesInCandidateOrder() {
        // Two candidates share the closest alias (cosine 1.0); both sit in the band and survive, in order.
        List<GameToolCandidates.Candidate> twoNav = List.of(
                candidate("navigate", "navigate"),
                candidate("navigate_alt", "plot course"),
                candidate("ship_status", "ship status report"));
        SemanticPhraseMatcher matcher = new SemanticPhraseMatcher(embedder(VECTORS));
        SemanticActionReducer r = new SemanticActionReducer(allowed -> twoNav, () -> matcher,
                unusedFallback(new AtomicBoolean()));

        List<LlmToolDefinition> tools = r.selectTools(ALL, "GO_NAV");
        assertEquals(List.of("navigate", "navigate_alt"), ids(tools));
    }

    @Test
    void blankInputDelegatesToFallback() {
        List<LlmToolDefinition> sentinel = List.of(catalog.get(0).tool());
        List<LlmToolDefinition> tools = reducer((categories, input) -> sentinel).selectTools(ALL, "  ");
        assertSame(sentinel, tools, "a blank input is handed to the word-overlap fallback");
    }

    @Test
    void unavailableMatcherDegradesToFallback() {
        List<LlmToolDefinition> sentinel = List.of(catalog.get(1).tool());
        SemanticActionReducer r = reducerWith(() -> null, (categories, input) -> sentinel);
        assertSame(sentinel, r.selectTools(ALL, "GO_NAV"), "a null matcher degrades to word-overlap");
    }

    @Test
    void embedFailureDegradesToFallback() {
        List<LlmToolDefinition> sentinel = List.of(catalog.get(2).tool());
        TextEmbedder throwing = new TextEmbedder() {
            @Override public float[] embed(String text) {
                throw new IllegalStateException("embed boom");
            }
            @Override public int dimensions() {
                return 3;
            }
        };
        SemanticPhraseMatcher matcher = new SemanticPhraseMatcher(throwing);
        SemanticActionReducer r = reducerWith(() -> matcher, (categories, input) -> sentinel);
        assertSame(sentinel, r.selectTools(ALL, "GO_NAV"), "an embed failure degrades to word-overlap for the turn");
    }
}
