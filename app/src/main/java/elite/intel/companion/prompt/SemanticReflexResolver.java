package elite.intel.companion.prompt;

import com.google.gson.JsonObject;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticSearchProvider;
import elite.intel.companion.confirm.CommandFlagDangerousActionPolicy;
import elite.intel.companion.confirm.DangerousActionPolicy;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolInvocation;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The companion's <em>semantic</em> reflex gate: it runs after {@link ReflexResolver} (verbatim exact-alias) and
 * before the LLM. Where the exact-alias reflex needs a training phrase matched word-for-word, this one matches by
 * <em>meaning</em> - it embeds the commander phrase and, when one visible action is an unambiguously close match,
 * dispatches it directly (no LLM), exactly as if the model had chosen it.
 * <p>
 * WHY: on a small local command model, asking the LLM to pick the tool the embedder already identified is the
 * weak link - it fumbles even a single offered tool and chats instead ("total bounties" -> speak). The embedder
 * is deterministic and already ranks the right action, so for a confident, clear, single winner we trust it and
 * skip the model. Unlike {@link ReflexResolver} this covers QUERIES too (a query reflex runs the query's own
 * data-grounded analysis path to voice the answer - the answer is delivered from data, never the model's whim).
 * <p>
 * Deliberately strict, so it never hijacks conversation or a parameterized/ambiguous request: it fires only when
 * the top match is above {@link #FLOOR}, clears the runner-up by {@link #GAP} (a genuine ambiguity - fleet vs
 * squadron carrier - has a close second and falls through to the LLM), is parameterless (the LLM extracts
 * arguments), and is not dangerous (a dangerous command keeps its confirmation flow). Degrades to "no reflex"
 * (the LLM path) when the embedding model is unavailable.
 */
public final class SemanticReflexResolver {

    /**
     * Below this best cosine, nothing is close enough in meaning to auto-dispatch; defer to the LLM/conversation.
     */
    private static final double FLOOR = parseDouble("elite.intel.companion.semreflex.floor", 0.88);
    /**
     * The top match must clear the runner-up by this margin to count as an unambiguous single winner.
     */
    private static final double GAP = parseDouble("elite.intel.companion.semreflex.gap", 0.05);

    private final Function<Set<IntelActionCategory>, List<GameToolCandidates.Candidate>> candidateSource;
    private final Supplier<SemanticPhraseMatcher> matcherSupplier;
    private final DangerousActionPolicy dangerousActionPolicy;

    /**
     * Production: visible candidates from the live registries/status, the shared embedder, the command danger flag.
     */
    public SemanticReflexResolver() {
        this(new GameToolCandidates()::collect, SemanticSearchProvider::matcher, new CommandFlagDangerousActionPolicy());
    }

    /**
     * Test seam: inject the candidate source, the matcher supplier (may return {@code null}), and the danger policy.
     */
    SemanticReflexResolver(Function<Set<IntelActionCategory>, List<GameToolCandidates.Candidate>> candidateSource,
                           Supplier<SemanticPhraseMatcher> matcherSupplier,
                           DangerousActionPolicy dangerousActionPolicy) {
        this.candidateSource = candidateSource;
        this.matcherSupplier = matcherSupplier;
        this.dangerousActionPolicy = dangerousActionPolicy;
    }

    /**
     * A resolver that never fires ({@link #resolve} always empty). For tests that exercise the LLM/preemption
     * path and must not have an input turned into a reflex by the live embedder.
     */
    public static SemanticReflexResolver disabled() {
        return new SemanticReflexResolver(categories -> List.of(), () -> null, null);
    }

    /**
     * The id of the single visible action whose meaning the input matches confidently and unambiguously - a
     * parameterless, non-dangerous command or query to dispatch without the LLM - or empty when the input is not a
     * clear semantic reflex (no embedder, nothing above the floor, a close runner-up, or the winner is
     * parameterized/dangerous), in which case it takes the normal LLM path.
     */
    public Optional<String> resolve(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        SemanticPhraseMatcher matcher = matcherSupplier.get();
        if (matcher == null) {
            return Optional.empty(); // no embedder: defer to the LLM path (documented degradation)
        }
        List<GameToolCandidates.Candidate> candidates =
                candidateSource.apply(Set.of(IntelActionCategory.ACTION, IntelActionCategory.QUERY));
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        float[] query = matcher.embedQuery(input);
        GameToolCandidates.Candidate best = null;
        double bestScore = -1.0;
        double secondScore = -1.0;
        for (GameToolCandidates.Candidate candidate : candidates) {
            double score = matcher.bestSimilarity(query, AliasMatchSurface.phrases(
                    candidate.phraseKey(), candidate.tool().parameters()));
            if (score > bestScore) {
                secondScore = bestScore;
                bestScore = score;
                best = candidate;
            } else if (score > secondScore) {
                secondScore = score;
            }
        }
        if (best == null || bestScore < FLOOR || (bestScore - secondScore) < GAP) {
            return Optional.empty(); // nothing confident, or a close second => ambiguous => let the LLM decide
        }
        if (!best.tool().parameters().isEmpty() || isDangerous(best.id())) {
            return Optional.empty(); // parameters need the LLM; dangerous keeps its confirmation flow
        }
        return Optional.of(best.id());
    }

    private boolean isDangerous(String actionId) {
        return dangerousActionPolicy.isDangerous(
                new LlmToolInvocation(UUID.randomUUID().toString(), actionId, new JsonObject()));
    }

    private static double parseDouble(String key, double def) {
        try {
            String value = System.getProperty(key);
            return value == null ? def : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
