package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.i18n.TrailingStringAliasMatcher;
import elite.intel.ai.brain.vega.diag.CompanionDiagnostics;
import elite.intel.ai.brain.vega.model.GameStateSnapshot;
import elite.intel.ai.brain.vega.model.IntelActionCategory;
import elite.intel.ai.brain.vega.model.llm.LlmToolDefinition;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticQuery;
import elite.intel.ai.embed.SemanticSearchProvider;
import elite.intel.diagnostics.DiagnosticsLog;
import elite.intel.diagnostics.DiagnosticsMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Selects game tools by semantic similarity. Exact prefixes for trailing string parameters are retained in
 * addition to semantic candidates, and word overlap is the fallback when embedding is unavailable.
 */
public final class SemanticActionReducer implements CompanionActionReducer {

    private static final Logger log = LogManager.getLogger(SemanticActionReducer.class);

    /** Minimum best cosine accepted as an actionable match. */
    private static final double SEM_FLOOR = 0.85;
    /** Relative band kept below the best score. */
    private static final double SEM_MARGIN = 0.04;
    /** Maximum number of game tools exposed to one turn. */
    private static final int SEM_MAX = 8;

    private final BiFunction<Set<IntelActionCategory>, GameStateSnapshot,
            List<GameToolCandidates.Candidate>> candidateSource;
    private final Supplier<SemanticPhraseMatcher> matcherSupplier;
    private final CompanionActionReducer wordOverlapFallback;

    /** Production: live candidates, the shared process-wide embedder, and a word-overlap reducer for degradation. */
    public SemanticActionReducer() {
        // Rebuild the catalog each turn so language and custom-command changes become visible immediately.
        this((categories, snapshot) -> new GameToolCandidates(snapshot).collect(categories),
                SemanticSearchProvider::matcher, new WordOverlapActionReducer());
    }

    /**
     * A reducer that scores with the shared embedder but takes candidate visibility from the given source
     * instead of the live game. Used by the Help tab to preview routing for a picked what-if situation and the
     * current language, rather than the running companion reducer's frozen live-game context.
     */
    public static SemanticActionReducer withCandidateSource(
            Function<Set<IntelActionCategory>, List<GameToolCandidates.Candidate>> candidateSource) {
        return new SemanticActionReducer(candidateSource, SemanticSearchProvider::matcher, new WordOverlapActionReducer());
    }

    /** Test seam: inject a fixed candidate source, a matcher supplier (may return {@code null}), and the fallback. */
    SemanticActionReducer(Function<Set<IntelActionCategory>, List<GameToolCandidates.Candidate>> candidateSource,
                          Supplier<SemanticPhraseMatcher> matcherSupplier,
                          CompanionActionReducer wordOverlapFallback) {
        this((categories, snapshot) -> candidateSource.apply(categories), matcherSupplier, wordOverlapFallback);
    }

    /** Test seam whose candidate catalog can observe the immutable commander-turn state. */
    SemanticActionReducer(BiFunction<Set<IntelActionCategory>, GameStateSnapshot,
            List<GameToolCandidates.Candidate>> candidateSource,
                          Supplier<SemanticPhraseMatcher> matcherSupplier,
                          CompanionActionReducer wordOverlapFallback) {
        this.candidateSource = candidateSource;
        this.matcherSupplier = matcherSupplier;
        this.wordOverlapFallback = wordOverlapFallback;
    }

    @Override
    public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories, String currentInput) {
        return selectTools(allowedCategories, currentInput, null, null);
    }

    @Override
    public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories, String currentInput,
                                               SemanticQuery semanticQuery) {
        return selectTools(allowedCategories, currentInput, semanticQuery, null);
    }

    @Override
    public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories, String currentInput,
                                               SemanticQuery semanticQuery,
                                               GameStateSnapshot gameStateSnapshot) {
        List<LlmToolDefinition> selected = doSelect(
                allowedCategories, currentInput, semanticQuery, gameStateSnapshot);
        // A structured shortlist distinguishes reducer misses from later model-selection misses.
        if (DiagnosticsMode.isEnabled() && !allowedCategories.isEmpty()) {
            DiagnosticsLog.write("DIAG reducer-candidates=" + CompanionDiagnostics.names(selected));
        }
        return selected;
    }

    /** Rehydrates a clarification target directly from the current visible catalog, without embedding it again. */
    @Override
    public Optional<LlmToolDefinition> findToolById(Set<IntelActionCategory> allowedCategories, String actionId,
                                                    GameStateSnapshot gameStateSnapshot) {
        if (actionId == null || actionId.isBlank()) {
            return Optional.empty();
        }
        return candidateSource.apply(allowedCategories, gameStateSnapshot).stream()
                .filter(candidate -> actionId.equals(candidate.id()))
                .map(GameToolCandidates.Candidate::tool)
                .findFirst();
    }

    private List<LlmToolDefinition> doSelect(Set<IntelActionCategory> allowedCategories, String currentInput,
                                             SemanticQuery semanticQuery,
                                             GameStateSnapshot gameStateSnapshot) {
        List<GameToolCandidates.Candidate> candidates = candidateSource.apply(allowedCategories, gameStateSnapshot);
        if (candidates.isEmpty()) {
            if (!allowedCategories.isEmpty()) {
                CompanionDiagnostics.debugAmbient("reduce", "semantic: no candidates for " + allowedCategories);
            }
            return List.of();
        }
        List<LlmToolDefinition> exactParameterized = exactParameterizedMatches(candidates, currentInput);
        // A blank input has no meaning to embed; the word-overlap fallback owns the "offer all" blank-input rule.
        if (currentInput == null || currentInput.isBlank()) {
            CompanionDiagnostics.debugAmbient("reduce", "semantic: blank input -> word-overlap fallback");
            return mergeExactMatches(exactParameterized,
                    wordOverlapFallback.selectTools(allowedCategories, currentInput, null, gameStateSnapshot));
        }
        SemanticPhraseMatcher matcher = matcherSupplier.get();
        if (matcher == null) {
            // Embedding model unavailable: degrade to word-overlap for the rest of the session (documented contract).
            CompanionDiagnostics.debugAmbient("reduce", "semantic: embedding model unavailable -> word-overlap fallback");
            return mergeExactMatches(exactParameterized,
                    wordOverlapFallback.selectTools(allowedCategories, currentInput, null, gameStateSnapshot));
        }
        try {
            return mergeExactMatches(exactParameterized,
                    semanticSelect(matcher, candidates, currentInput, semanticQuery));
        } catch (RuntimeException embedFailure) {
            // A transient embedding failure must not make every game command unavailable.
            log.warn("Semantic reduction failed; falling back to word-overlap for this turn", embedFailure);
            CompanionDiagnostics.debugAmbient("reduce", "semantic: embed failed -> word-overlap fallback");
            return mergeExactMatches(exactParameterized,
                    wordOverlapFallback.selectTools(allowedCategories, currentInput, null, gameStateSnapshot));
        }
    }

    /** Keeps only matches with the longest exact literal prefix, resolving overlapping broad/specific aliases. */
    private static List<LlmToolDefinition> exactParameterizedMatches(
            List<GameToolCandidates.Candidate> candidates,
            String input
    ) {
        int[] specificity = new int[candidates.size()];
        int best = 0;
        for (int i = 0; i < candidates.size(); i++) {
            GameToolCandidates.Candidate candidate = candidates.get(i);
            specificity[i] = TrailingStringAliasMatcher.findBestMatch(
                            candidate.localizedAliasGroup(), candidate.tool().parameters(), input)
                    .map(TrailingStringAliasMatcher.Match::prefixWordCount)
                    .orElse(0);
            best = Math.max(best, specificity[i]);
        }
        if (best == 0) {
            return List.of();
        }
        List<LlmToolDefinition> result = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();
        for (int i = 0; i < candidates.size() && result.size() < SEM_MAX; i++) {
            GameToolCandidates.Candidate candidate = candidates.get(i);
            if (specificity[i] == best && added.add(candidate.id())) {
                result.add(candidate.tool());
            }
        }
        return result;
    }

    /** Keeps deterministic prefix matches first without suppressing semantic competitors. */
    private static List<LlmToolDefinition> mergeExactMatches(
            List<LlmToolDefinition> exactMatches,
            List<LlmToolDefinition> selected
    ) {
        if (exactMatches.isEmpty()) {
            return selected;
        }
        List<LlmToolDefinition> merged = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();
        for (LlmToolDefinition tool : exactMatches) {
            if (added.add(tool.name())) {
                merged.add(tool);
            }
        }
        for (LlmToolDefinition tool : selected) {
            if (merged.size() >= SEM_MAX) {
                break;
            }
            if (added.add(tool.name())) {
                merged.add(tool);
            }
        }
        CompanionDiagnostics.debugAmbient("reduce", "parameterized trigger retained -> "
                + CompanionDiagnostics.names(merged));
        return List.copyOf(merged);
    }

    /**
     * Scores every candidate by the best cosine of the input against its localized alias phrases, then keeps
     * the relative band above the floor, ranked and capped. Returns empty when the best match is below the floor.
     */
    private List<LlmToolDefinition> semanticSelect(SemanticPhraseMatcher matcher,
                                                   List<GameToolCandidates.Candidate> candidates, String input,
                                                   SemanticQuery semanticQuery) {
        float[] queryVector = semanticQuery == null ? null : semanticQuery.vectorFor(input, matcher);
        if (queryVector == null) {
            queryVector = matcher.embedQuery(input);
        }
        double[] scores = new double[candidates.size()];
        double best = -1.0;
        for (int i = 0; i < candidates.size(); i++) {
            GameToolCandidates.Candidate candidate = candidates.get(i);
            List<String> aliases = AliasEmbeddingText.phrases(
                    candidate.localizedAliasGroup(), candidate.tool().parameters());
            scores[i] = matcher.bestSimilarity(queryVector, aliases);
            best = Math.max(best, scores[i]);
        }
        if (best < SEM_FLOOR) {
            CompanionDiagnostics.debugAmbient("reduce", String.format(Locale.ROOT,
                    "semantic: candidates=%d best=%.3f < floor %.2f -> no game tools (conversation/recall)",
                    candidates.size(), best, SEM_FLOOR));
            return List.of();
        }

        double cutoff = best - SEM_MARGIN;
        List<Integer> kept = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            if (scores[i] >= cutoff) {
                kept.add(i);
            }
        }
        // Strongest first so the cap keeps the closest matches; ties keep candidate order (stable sort).
        kept.sort((a, b) -> Double.compare(scores[b], scores[a]));

        List<LlmToolDefinition> result = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();
        for (int idx : kept) {
            if (result.size() >= SEM_MAX) {
                break;
            }
            GameToolCandidates.Candidate candidate = candidates.get(idx);
            if (added.add(candidate.id())) {
                result.add(candidate.tool());
            }
        }
        CompanionDiagnostics.debugAmbient("reduce", String.format(Locale.ROOT,
                "semantic: candidates=%d best=%.3f cutoff=%.3f kept=%d -> %s",
                candidates.size(), best, cutoff, result.size(), CompanionDiagnostics.names(result)));
        return result;
    }
}
