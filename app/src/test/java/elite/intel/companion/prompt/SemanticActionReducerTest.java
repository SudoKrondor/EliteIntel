package elite.intel.companion.prompt;

import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticQuery;
import elite.intel.ai.embed.TextEmbedder;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.GameStateSnapshot;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

    private static GameToolCandidates.Candidate candidate(String id, String localizedAliasGroup) {
        return candidate(id, localizedAliasGroup, List.of());
    }

    private static GameToolCandidates.Candidate candidate(String id, String localizedAliasGroup,
                                                          List<ActionParameterSpec> parameters) {
        return new GameToolCandidates.Candidate(id, localizedAliasGroup,
                new LlmToolDefinition(id, "desc", localizedAliasGroup, parameters));
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
    void clarificationTargetLookupUsesFreshSnapshotWithoutEmbedding() {
        GameStateSnapshot turnState = GameStateSnapshot.capture(Status.detached(PlayerSituation.IN_SHIP_DEEP_SPACE));
        AtomicReference<GameStateSnapshot> observed = new AtomicReference<>();
        AtomicBoolean matcherRequested = new AtomicBoolean();
        SemanticActionReducer snapshotReducer = new SemanticActionReducer(
                (allowed, snapshot) -> {
                    observed.set(snapshot);
                    return snapshot == turnState ? catalog : List.of();
                },
                () -> {
                    matcherRequested.set(true);
                    return null;
                },
                (categories, input) -> List.of());

        LlmToolDefinition target = snapshotReducer.findToolById(ALL, "navigate", turnState).orElseThrow();

        assertEquals("navigate", target.name());
        assertSame(turnState, observed.get());
        assertTrue(!matcherRequested.get(), "id rehydration must not embed the terse continuation reply");
    }

    @Test
    void reusesPreparedSemanticQueryForTheSameTurn() {
        AtomicInteger queryEmbeds = new AtomicInteger();
        TextEmbedder counting = new TextEmbedder() {
            @Override public float[] embed(String text) {
                if ("GO_NAV".equals(text)) {
                    queryEmbeds.incrementAndGet();
                }
                return VECTORS.getOrDefault(text, new float[]{1, 1, 1});
            }
            @Override public int dimensions() {
                return 3;
            }
        };
        SemanticPhraseMatcher matcher = new SemanticPhraseMatcher(counting);
        List<GameToolCandidates.Candidate> parameterizedCatalog = List.of(
                candidate("navigate", "navigate, plot course", List.of(
                        new ActionParameterSpec("destination", "string", true, "Destination", List.of(), null))),
                catalog.get(1), catalog.get(2));
        GameStateSnapshot turnState = GameStateSnapshot.capture(Status.detached(PlayerSituation.IN_SHIP_DEEP_SPACE));
        AtomicReference<GameStateSnapshot> reducerState = new AtomicReference<>();
        SemanticActionReducer reducer = new SemanticActionReducer(
                (allowed, snapshot) -> {
                    reducerState.set(snapshot);
                    return parameterizedCatalog;
                }, () -> matcher, unusedFallback(new AtomicBoolean()));

        SemanticQuery prepared = matcher.embedQueryContext("GO_NAV");
        List<LlmToolDefinition> tools = reducer.selectTools(
                ALL, "GO_NAV", prepared, turnState);

        assertEquals(List.of("navigate"), ids(tools));
        assertEquals(1, queryEmbeds.get(), "the reducer must reuse the prepared query vector");
        assertSame(turnState, reducerState.get(), "the reducer must receive that exact same state instance");
    }

    @Test
    void belowFloorOffersNoTools() {
        // An input far from every candidate (best cosine under the floor) yields no game tools.
        List<LlmToolDefinition> tools = reducer(unusedFallback(new AtomicBoolean())).selectTools(ALL, "unrelated");
        assertTrue(tools.isEmpty(), "nothing close enough in meaning -> no game tools");
    }

    @Test
    void exactFreeFormTriggerSurvivesWhenSemanticMatchingIsUnavailable() {
        ActionParameterSpec text = new ActionParameterSpec(
                "text", "string", true, "Text to remember", List.of(), "Extract verbatim");
        List<GameToolCandidates.Candidate> parameterized = List.of(
                candidate("broad_reminder", "do not forget {text:X}", List.of(text)),
                candidate("remember", "remember {text:X}, remember that {text:X}, do not forget that {text:X}",
                        List.of(text)),
                catalog.get(2));
        AtomicBoolean matcherRequested = new AtomicBoolean();
        SemanticActionReducer reducer = new SemanticActionReducer(
                allowed -> parameterized,
                () -> {
                    matcherRequested.set(true);
                    return null;
                },
                unusedFallback(new AtomicBoolean()));

        List<LlmToolDefinition> tools = reducer.selectTools(
                ALL, "do not forget that the docking code is Sierra Nine Four");

        assertEquals(List.of("remember"), ids(tools));
        assertTrue(matcherRequested.get(), "semantic competitors must still be considered");

        assertEquals(List.of("remember"), ids(reducer.selectTools(ALL, "remember that")),
                "an incomplete exact trigger must still expose the command for request_input");
    }

    @Test
    void exactFreeFormTriggerDoesNotSuppressSemanticCompetitor() {
        ActionParameterSpec text = new ActionParameterSpec(
                "text", "string", true, "Text to remember", List.of(), "Extract verbatim");
        List<GameToolCandidates.Candidate> candidates = List.of(
                candidate("remember", "remember {text:X}, remember that {text:X}", List.of(text)),
                candidate("set_reminder", "remember to request docking"));
        String input = "remember to request docking";
        SemanticPhraseMatcher matcher = new SemanticPhraseMatcher(embedder(Map.of(
                input, new float[]{1, 0, 0},
                "remember", new float[]{0, 1, 0},
                "remember that", new float[]{0, 1, 0})));
        SemanticActionReducer reducer = new SemanticActionReducer(
                allowed -> candidates, () -> matcher, unusedFallback(new AtomicBoolean()));

        List<LlmToolDefinition> tools = reducer.selectTools(ALL, input);

        assertEquals(List.of("remember", "set_reminder"), ids(tools));
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
    void unavailableMatcherKeepsTheTurnSnapshotForFallback() {
        GameStateSnapshot turnState = GameStateSnapshot.capture(Status.detached(PlayerSituation.IN_SHIP_DEEP_SPACE));
        AtomicReference<GameStateSnapshot> observed = new AtomicReference<>();
        List<LlmToolDefinition> sentinel = List.of(catalog.get(1).tool());
        CompanionActionReducer fallback = new CompanionActionReducer() {
            @Override
            public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> categories, String input) {
                return sentinel;
            }

            @Override
            public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> categories, String input,
                                                       SemanticQuery semanticQuery,
                                                       GameStateSnapshot gameStateSnapshot) {
                observed.set(gameStateSnapshot);
                return sentinel;
            }
        };
        SemanticActionReducer reducer = reducerWith(() -> null, fallback);

        assertSame(sentinel, reducer.selectTools(ALL, "GO_NAV", null, turnState));
        assertSame(turnState, observed.get(), "degradation must not return to live visibility state");
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
