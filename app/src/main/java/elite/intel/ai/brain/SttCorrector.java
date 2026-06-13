package elite.intel.ai.brain;

import elite.intel.db.FuzzySearch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Vocabulary-driven STT post-processor.
 * <p>
 * Corrects individual tokens in an STT transcript that are phonetically close to
 * words in the action-map vocabulary but were mis-transcribed. Only replaces a token
 * when there is a single unambiguous best match within the length-proportional
 * edit-distance threshold - if two vocabulary words tie, the original is preserved.
 * <p>
 * This runs before {@link InputNormalizer} so the normalizer's synonym rules see
 * corrected text and the LLM receives the best available input.
 */
public class SttCorrector {

    private static final Logger log = LogManager.getLogger(SttCorrector.class);

    // Strips {key:X}, {state:true/false}, {lat:X, lon:Y}, {key:X, max_distance:Y} etc.
    private static final Pattern TEMPLATE = Pattern.compile("\\{[^}]*\\}");

    // Mirrors Reducer.STOP_WORDS plus annotation words that appear in map-key notes.
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "to", "of", "in", "on", "at", "by", "for",
            "with", "and", "or", "is", "are", "am", "be", "do", "does",
            "what", "where", "how", "which", "any", "our", "my", "me",
            "we", "us", "it", "this", "that", "get", "have",
            "has", "can", "could", "would", "should", "not", "no", "up",
            "here", "there", "some", "much", "many",
            // large numbers - never game commands; "millions" → "missions" edit-distance collision
            "million", "millions", "billion", "billions", "trillion", "trillions",
            // annotation noise from map-key inline notes ("NOTE: Sol and Earth...")
            "note", "never", "mean", "always", "only", "true", "false", "also", "use", "about"
    );

    private SttCorrector() {
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Extracts all meaningful vocabulary tokens from the action-map keys.
     * Template placeholders are stripped first, then each key is split on
     * delimiters (spaces, commas, slashes, dashes, dots).  Tokens shorter than
     * 4 characters or matching stop words are excluded.
     */
    public static Set<String> extractVocabulary(Map<String, String> actionMap) {
        Set<String> vocab = new HashSet<>();
        for (String key : actionMap.keySet()) {
            String stripped = TEMPLATE.matcher(key).replaceAll(" ");
            for (String token : stripped.split("[\\s,/.\\-]+")) {
                String t = token.toLowerCase().replaceAll("[^a-z]", "");
                if (t.length() >= 4 && !STOP_WORDS.contains(t)) {
                    vocab.add(t);
                }
            }
        }
        return Collections.unmodifiableSet(vocab);
    }

    /**
     * Corrects each whitespace-delimited token in {@code input} against
     * {@code vocabulary}.  A token is replaced only when:
     * <ol>
     *   <li>it is not already an exact vocabulary match,</li>
     *   <li>its length meets the minimum threshold (≥ 5 chars),</li>
     *   <li>there is exactly one best match within the length-proportional
     *       edit-distance limit (second-best must be strictly farther).</li>
     * </ol>
     * Tokens that do not meet all three conditions are passed through unchanged.
     */
    public static String correct(String input, Set<String> vocabulary) {
        if (input == null || input.isBlank() || vocabulary.isEmpty()) return input;

        String[] tokens = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (!sb.isEmpty()) sb.append(' ');
            String corrected = correctToken(token, vocabulary);
            if (!corrected.equals(token)) {
                log.info("STT correction: '{}' → '{}'", token, corrected);
            }
            sb.append(corrected);
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private static String correctToken(String token, Set<String> vocabulary) {
        if (token.contains("-")) return token;     // hyphenated phrases are intentional constructs, not mishearings
        String lower = token.toLowerCase().replaceAll("[^a-z]", "");
        int threshold = maxDistance(lower);
        if (threshold == 0) return token;          // too short - collision risk too high
        if (STOP_WORDS.contains(lower)) return token; // common word - never correct away
        if (vocabulary.contains(lower)) return token; // already correct

        String bestMatch = null;
        int bestDist = Integer.MAX_VALUE;
        int secondBest = Integer.MAX_VALUE;

        for (String candidate : vocabulary) {
            int dist = FuzzySearch.levenshteinDistance(lower, candidate);
            if (dist < bestDist) {
                secondBest = bestDist;
                bestDist = dist;
                bestMatch = candidate;
            } else if (dist < secondBest) {
                secondBest = dist;
            }
        }

        // Require: within threshold AND uniquely better than second-best
        if (bestMatch != null && bestDist <= threshold && bestDist < secondBest) {
            return bestMatch;
        }
        return token;
    }

    /**
     * Length-proportional edit-distance ceiling.
     *
     * <pre>
     *  &lt; 5 chars  →  0  (no correction - short tokens collide too easily)
     *  5–6 chars  →  1
     *  7–9 chars  →  2
     *  10+ chars  →  3
     * </pre>
     */
    static int maxDistance(String token) {
        int len = token.length();
        if (len < 5) return 0;
        if (len < 7) return 1;
        if (len < 10) return 2;
        return 3;
    }
}
