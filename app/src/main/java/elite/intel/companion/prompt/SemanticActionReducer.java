package elite.intel.companion.prompt;

import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticSearchProvider;
import elite.intel.companion.diag.CompanionDiagnostics;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.diagnostics.DiagnosticsLog;
import elite.intel.diagnostics.DiagnosticsMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Selects the game tools for a thought turn by <em>meaning</em>: it embeds the commander phrase and keeps the
 * candidates whose training phrases (localized aliases) are closest in the embedding space, so an inflected or
 * paraphrased input reaches the right tool without sharing a literal word with it. This replaces
 * {@link WordOverlapActionReducer} on the companion path; the word-overlap reducer is retained as the
 * graceful-degradation fallback and is delegated to whenever meaning-based selection cannot run.
 * <p>
 * The selection model is ported from the proven legacy {@code elite.intel.ai.brain.Reducer} semantic mode:
 * e5 cosines sit in a high, compressed band, so selection is <em>relative</em> to the best match (keep
 * everything within {@link #SEM_MARGIN} of it), gated by an absolute {@link #SEM_FLOOR} below which nothing is
 * relevant (the turn is conversation or memory recall, not a command), and capped at {@link #SEM_MAX} so a
 * vague input cannot flood the prompt.
 * <p>
 * Degrades to word-overlap (never errors out a turn) when the input is blank, the embedding model is
 * unavailable ({@link SemanticSearchProvider#matcher()} returns {@code null}), or an embed call fails.
 */
public final class SemanticActionReducer implements CompanionActionReducer {

    private static final Logger log = LogManager.getLogger(SemanticActionReducer.class);

    /**
     * Below this best cosine, nothing is close enough in meaning: offer no game tools. Calibrated for
     * multilingual-e5-small against the live-LLM natural-speech routing suite (EN + RU), the recall authority:
     * 0.85 matches 0.80's routing exactly for EN/RU (no real commands dropped) while rejecting more sub-0.85
     * junk, whereas 0.87+ starts dropping EN commands. Conversational noise embeds ~0.87-0.92, so this floor
     * does not reject it without also costing recall - a known limit of this model's compressed band, not fixed
     * by any single global floor. IT aliases embed lower and regress even at 0.85 (deprioritized for now); a
     * per-language floor or stronger IT aliases would be the real fix. Recalibrate after any model change (see
     * {@code SemanticReducerProbe#dumpsFloorCalibration}).
     */
    private static final double SEM_FLOOR = 0.85;
    /**
     * Keep candidates scoring within this margin of the best match (legacy semantic margin). Deliberately
     * inclusive: the correct tool is not always the single top match (a noun like "двигатели" can pull a
     * sibling query above "target subsystem"), so a band wide enough to admit the near-miss correct tool lets
     * the LLM make the final pick. Narrowing is the cap's job, not the margin's.
     */
    private static final double SEM_MARGIN = 0.04;
    /**
     * Hard cap on surviving candidates, and the actual narrowing lever. Far below the legacy 25: a broad verb
     * ("переключи", "найди") sits near a whole command family, so the band alone would admit the family and
     * widen the prompt; the cap bounds that tail to the closest few while the top-ranked correct tool always
     * survives.
     */
    private static final int SEM_MAX = 8;

    private final Function<Set<IntelActionCategory>, List<GameToolCandidates.Candidate>> candidateSource;
    private final Supplier<SemanticPhraseMatcher> matcherSupplier;
    private final CompanionActionReducer wordOverlapFallback;

    /** Production: live candidates, the shared process-wide embedder, and a word-overlap reducer for degradation. */
    public SemanticActionReducer() {
        // Build the candidates fresh every turn (not once) so the reducer always reflects the CURRENT command
        // language, game status and custom-command set. GameToolCandidates freezes those in its constructor, so
        // capturing one instance would pin the language to companion-start time - a later language change (e.g.
        // in Settings) would never reach the reducer and it would keep matching the old language's aliases.
        this(cats -> new GameToolCandidates().collect(cats), SemanticSearchProvider::matcher, new WordOverlapActionReducer());
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
        this.candidateSource = candidateSource;
        this.matcherSupplier = matcherSupplier;
        this.wordOverlapFallback = wordOverlapFallback;
    }

    @Override
    public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories, String currentInput) {
        List<LlmToolDefinition> selected = doSelect(allowedCategories, currentInput);
        // Diagnostics harness: surface the reducer's shortlist as a structured marker so the skill can tell a
        // recall miss (expected id ABSENT here - fix the aliases) from a selection miss (expected id present but
        // the LLM dispatched another - fix the llmDescription), without scraping the prose DBG/LOG lines. The
        // names are the same runtime ids DIAG dispatch uses, in the reducer's ranking order. Only for a real
        // game-tool reduction (non-empty categories); a NARRATION turn offers no game tools by intent, so no line.
        if (DiagnosticsMode.isEnabled() && !allowedCategories.isEmpty()) {
            DiagnosticsLog.write("DIAG reducer-candidates=" + CompanionDiagnostics.names(selected));
        }
        return selected;
    }

    private List<LlmToolDefinition> doSelect(Set<IntelActionCategory> allowedCategories, String currentInput) {
        List<GameToolCandidates.Candidate> candidates = candidateSource.apply(allowedCategories);
        if (candidates.isEmpty()) {
            // An empty allowed-category set is intent (e.g. a NARRATION thought offers no game tools), not a
            // selection outcome, so it needs no line; a non-empty set that yielded nothing is worth surfacing.
            if (!allowedCategories.isEmpty()) {
                CompanionDiagnostics.debugAmbient("reduce", "semantic: no candidates for " + allowedCategories);
            }
            return List.of();
        }
        // A blank input has no meaning to embed; the word-overlap fallback owns the "offer all" blank-input rule.
        if (currentInput == null || currentInput.isBlank()) {
            CompanionDiagnostics.debugAmbient("reduce", "semantic: blank input -> word-overlap fallback");
            return wordOverlapFallback.selectTools(allowedCategories, currentInput);
        }
        SemanticPhraseMatcher matcher = matcherSupplier.get();
        if (matcher == null) {
            // Embedding model unavailable: degrade to word-overlap for the rest of the session (documented contract).
            CompanionDiagnostics.debugAmbient("reduce", "semantic: embedding model unavailable -> word-overlap fallback");
            return wordOverlapFallback.selectTools(allowedCategories, currentInput);
        }
        try {
            return semanticSelect(matcher, candidates, currentInput);
        } catch (RuntimeException embedFailure) {
            // WHY: a transient embed failure must not drop the turn's tools; degrade to word-overlap for this turn.
            log.warn("Semantic reduction failed; falling back to word-overlap for this turn", embedFailure);
            CompanionDiagnostics.debugAmbient("reduce", "semantic: embed failed -> word-overlap fallback");
            return wordOverlapFallback.selectTools(allowedCategories, currentInput);
        }
    }

    /**
     * Scores every candidate by the best cosine of the input against its localized alias phrases, then keeps
     * the relative band above the floor, ranked and capped. Returns empty when the best match is below the floor.
     */
    private List<LlmToolDefinition> semanticSelect(SemanticPhraseMatcher matcher,
                                                   List<GameToolCandidates.Candidate> candidates, String input) {
        float[] queryVector = matcher.embedQuery(input);
        double[] scores = new double[candidates.size()];
        double best = -1.0;
        for (int i = 0; i < candidates.size(); i++) {
            GameToolCandidates.Candidate candidate = candidates.get(i);
            List<String> aliases = AliasMatchSurface.phrases(candidate.phraseKey(), candidate.tool().parameters());
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
