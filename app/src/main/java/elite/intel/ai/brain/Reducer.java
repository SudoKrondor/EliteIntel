package elite.intel.ai.brain;

import elite.intel.ai.brain.actions.command.builtin.IgnoreNonsensicalInputCommand;
import elite.intel.ai.brain.actions.handlers.query.GeneralConversationQueryCommand;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.i18n.InputNormalizerLocalizations;
import elite.intel.ai.embed.OnnxTextEmbedder;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Narrows the full action catalog to the handful relevant to one user phrase, before the LLM makes the final
 * pick. Two interchangeable strategies, chosen once by the {@code elite.intel.reducer} system property:
 * <ul>
 *   <li>{@code wordoverlap} (default) — the original exact word-overlap match. Fast, no model, but blind to
 *       inflection: {@code авианосцу} does not match the trigger word {@code авианосец}.</li>
 *   <li>{@code semantic} — embeds the input and scores it against the catalog by meaning
 *       ({@link SemanticPhraseMatcher}), so inflected forms and synonyms match without per-language tables.</li>
 * </ul>
 * Both share the same exact-alias preservation and conversation/nonsensical fallback, so only the overlap test
 * differs. The embedding model is loaded lazily and only when semantic mode is active, so word-overlap mode
 * keeps its exact original cost.
 * <p>
 * This whole class is part of the temporary legacy LLM pipeline; once companion mode is the only mode it goes
 * away. The durable piece is {@link SemanticPhraseMatcher}, which the companion path reuses. The toggle is a
 * deliberately lightweight in-file switch (not a full strategy framework) so it is trivial to delete with the
 * pipeline.
 */
public class Reducer {

    private static final Logger log = LogManager.getLogger(Reducer.class);

    /**
     * Selection strategy, fixed at class load from {@code -Delite.intel.reducer=semantic|wordoverlap}.
     */
    private static final boolean SEMANTIC =
            "semantic".equalsIgnoreCase(System.getProperty("elite.intel.reducer", "wordoverlap"));

    // Semantic tuning. e5 cosines sit in a high, compressed band, so selection is relative to the best match,
    // with an absolute floor below which the input is treated as unrelated (so the fallback fires). All three
    // are overridable via -D for live A/B tuning without a recompile.
    /**
     * Below this best similarity, nothing is relevant — trigger the fallback.
     */
    private static final double SEM_FLOOR = parseDouble("elite.intel.reducer.semantic.floor", 0.80);
    /**
     * Keep candidates scoring within this margin of the best match.
     */
    private static final double SEM_MARGIN = parseDouble("elite.intel.reducer.semantic.margin", 0.04);
    /**
     * Hard cap on how many candidates survive, so a vague input cannot flood the prompt.
     */
    private static final int SEM_MAX = parseInt("elite.intel.reducer.semantic.max", 25);

    /**
     * Lazily built only when semantic mode actually runs, so word-overlap mode never loads the model.
     */
    private static volatile SemanticPhraseMatcher matcher;
    /**
     * Set once if the embedding model cannot be loaded; the session then degrades to word-overlap.
     */
    private static volatile boolean semanticUnavailable;

    /**
     * The semantic matcher, lazily loaded on first use, or {@code null} when semantic mode is off or the model
     * could not be loaded. A load failure is not fatal: it is logged once and the session falls back to
     * word-overlap, matching how the project degrades other optional backends (STT/TTS/LLM).
     */
    private static SemanticPhraseMatcher semanticMatcherOrNull() {
        if (semanticUnavailable) {
            return null;
        }
        SemanticPhraseMatcher m = matcher;
        if (m != null) {
            return m;
        }
        synchronized (Reducer.class) {
            if (semanticUnavailable) {
                return null;
            }
            if (matcher == null) {
                try {
                    log.info("Semantic reducer active — loading embedding model");
                    matcher = new SemanticPhraseMatcher(new OnnxTextEmbedder());
                } catch (RuntimeException e) {
                    // WHY: an absent or broken embedding model is a degraded optional backend, not a reason to
                    // break all routing; degrade to word-overlap for the rest of the session.
                    semanticUnavailable = true;
                    log.warn("Embedding model unavailable; falling back to word-overlap reduction for this session", e);
                    return null;
                }
            }
            return matcher;
        }
    }

    /**
     * Reduces the action map to entries relevant to {@code normalizedInput}. Dispatches to the configured
     * strategy; both preserve an exact-alias match and apply the same empty-result fallback.
     *
     * @param normalizedInput  normalized user input; null/blank returns the full map unchanged
     * @param full             the complete trigger-phrase &rarr; action-name map
     * @param isConversationMode selects the fallback action when nothing matches
     * @return the reduced candidate map (never empty: falls back to conversation/nonsensical)
     */
    public static Map<String, String> reduce(
            String normalizedInput,
            Map<String, String> full,
            boolean isConversationMode
    ) {
        if (normalizedInput == null || normalizedInput.isBlank()) {
            return full;
        }
        SemanticPhraseMatcher activeMatcher = SEMANTIC ? semanticMatcherOrNull() : null;
        return reduceWith(activeMatcher, normalizedInput, full, isConversationMode);
    }

    /**
     * Dispatch point, package-private for testing: a non-null matcher selects semantic reduction; a null
     * matcher (semantic mode off, or model unavailable) selects word-overlap. This is where the graceful
     * degradation from a failed model load lands.
     */
    static Map<String, String> reduceWith(
            SemanticPhraseMatcher matcher,
            String normalizedInput,
            Map<String, String> full,
            boolean isConversationMode
    ) {
        return matcher != null
                ? semanticReduce(matcher, normalizedInput, full, isConversationMode)
                : wordOverlapReduce(normalizedInput, full, isConversationMode);
    }

    /**
     * Original exact word-overlap reduction: keep an action when its trigger phrase shares a meaningful
     * (long enough, non-stop) word with the input. Package-private so it can be tested directly.
     */
    static Map<String, String> wordOverlapReduce(
            String normalizedInput,
            Map<String, String> full,
            boolean isConversationMode
    ) {
        String directAction = resolveDirectAlias(normalizedInput, full);

        // Unicode-aware tokenization: "\\W+" is too ASCII-centric for Cyrillic, umlauts, etc.
        Set<String> inputWords = significantWords(normalizedInput);

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : full.entrySet()) {
            Set<String> triggerWords = significantWords(entry.getKey());
            if (triggerWords.stream().anyMatch(inputWords::contains)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return finish(result, directAction, normalizedInput, isConversationMode);
    }

    /**
     * Semantic reduction: keep actions whose trigger phrases are closest in meaning to the input. Scores each
     * entry by the best cosine of the input against its (comma-grouped) alias phrases, keeps those within
     * {@link #SEM_MARGIN} of the best and above {@link #SEM_FLOOR}, capped at {@link #SEM_MAX}. Package-private
     * and matcher-injected so a test can drive it with a real model without touching the static singleton.
     */
    static Map<String, String> semanticReduce(
            SemanticPhraseMatcher matcher,
            String normalizedInput,
            Map<String, String> full,
            boolean isConversationMode
    ) {
        String directAction = resolveDirectAlias(normalizedInput, full);

        float[] queryVector = matcher.embedQuery(normalizedInput);
        List<Map.Entry<String, String>> entries = new ArrayList<>(full.entrySet());
        double[] scores = new double[entries.size()];
        double best = -1.0;
        for (int i = 0; i < entries.size(); i++) {
            List<String> aliases = AiActionLocalizations.splitPhraseGroup(entries.get(i).getKey());
            scores[i] = matcher.bestSimilarity(queryVector, aliases);
            best = Math.max(best, scores[i]);
        }

        Map<String, String> result = new LinkedHashMap<>();
        if (best >= SEM_FLOOR) {
            double cutoff = best - SEM_MARGIN;
            // Rank by score so the cap keeps the strongest matches.
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) {
                if (scores[i] >= cutoff) {
                    order.add(i);
                }
            }
            order.sort((a, b) -> Double.compare(scores[b], scores[a]));
            for (int idx : order) {
                if (result.size() >= SEM_MAX) {
                    break;
                }
                result.put(entries.get(idx).getKey(), entries.get(idx).getValue());
            }
        }

        return finish(result, directAction, normalizedInput, isConversationMode);
    }

    /**
     * Shared tail: re-add an exact alias match, then fall back if nothing survived.
     */
    private static Map<String, String> finish(
            Map<String, String> result,
            String directAction,
            String normalizedInput,
            boolean isConversationMode
    ) {
        // An exact alias is a high-confidence candidate; keep it regardless of the overlap/score test.
        // The LLM still makes the final intent selection.
        if (directAction != null) {
            result.put(normalizedInput, directAction);
        }
        if (result.isEmpty()) {
            String fallback = isConversationMode
                    ? GeneralConversationQueryCommand.ID
                    : IgnoreNonsensicalInputCommand.ID;
            result.put(fallback, fallback);
        }
        return result;
    }

    /**
     * The action whose key is, or contains as an alias, an exact case-insensitive match of the input.
     */
    private static String resolveDirectAlias(String normalizedInput, Map<String, String> full) {
        String directAction = full.get(normalizedInput);
        if (directAction != null) {
            return directAction;
        }
        String lowerInput = normalizedInput.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : full.entrySet()) {
            List<String> phrases = AiActionLocalizations.splitPhraseGroup(entry.getKey());
            if (phrases.stream().anyMatch(p -> p.toLowerCase(Locale.ROOT).equals(lowerInput))) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Lowercased, long-enough, non-stop-word tokens — Unicode-aware (Cyrillic, umlauts, etc.).
     */
    private static Set<String> significantWords(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_]+"))
                .filter(w -> w.length() > 2)
                .filter(w -> !InputNormalizerLocalizations.stopWords().contains(w))
                .collect(Collectors.toSet());
    }

    public static String formatActions(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("ACTIONS (use ONLY these exact action names):\n\n");
        map.forEach((key, action) ->
                sb.append("  ").append(action).append(" ← ").append(key).append("\n"));
        return sb.toString();
    }

    private static double parseDouble(String key, double def) {
        try {
            String v = System.getProperty(key);
            return v == null ? def : Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int parseInt(String key, int def) {
        try {
            String v = System.getProperty(key);
            return v == null ? def : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
