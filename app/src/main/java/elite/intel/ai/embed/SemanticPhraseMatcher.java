package elite.intel.ai.embed;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scores how close a query phrase is to candidate phrases by <em>meaning</em>, using a {@link TextEmbedder}.
 * This is the shared core both reducers can sit on: it owns the embedder and a phrase&rarr;vector cache, so a
 * catalog phrase is embedded once and reused across utterances (per turn only the query is embedded fresh).
 * That keeps semantic selection cheap — the expensive part is the catalog, and it is paid once.
 * <p>
 * Survives the legacy pipeline: when only companion mode remains, this matcher backs the companion's semantic
 * reducer and memory recall unchanged. Inflection/synonym tolerance comes entirely from the embedding model,
 * so there are no per-language declension or synonym tables here.
 * <p>
 * Thread-safety: the underlying ONNX embedder is single-session, so embed calls are serialised. Cache reads
 * are lock-free; only a cache miss takes the embed lock.
 */
public final class SemanticPhraseMatcher {

    private final TextEmbedder embedder;
    private final Map<String, float[]> phraseCache = new ConcurrentHashMap<>();
    private final Object embedLock = new Object();

    public SemanticPhraseMatcher(TextEmbedder embedder) {
        this.embedder = embedder;
    }

    /**
     * Embeds a fresh query phrase (not cached — queries rarely repeat).
     */
    public float[] embedQuery(String text) {
        synchronized (embedLock) {
            return embedder.embed(text);
        }
    }

    /** The best-matching phrase: its index in the queried list and its cosine. {@code index == -1} when none. */
    public record Match(int index, double score) {}

    /**
     * Best similarity of a query to any of a candidate's phrases (e.g. comma-grouped aliases of one action).
     * Returns -1 if the candidate has no usable phrases.
     */
    public double bestSimilarity(float[] queryVector, List<String> phrases) {
        return bestMatch(queryVector, phrases).score();
    }

    /**
     * Like {@link #bestSimilarity}, but also reports <em>which</em> phrase won, by its index in {@code phrases}.
     * Lets a caller attribute a candidate's score to a specific source phrase (e.g. an alias vs a description).
     * Returns {@code index == -1, score == -1} when the candidate has no usable phrase.
     */
    public Match bestMatch(float[] queryVector, List<String> phrases) {
        int bestIndex = -1;
        double best = -1.0;
        for (int i = 0; i < phrases.size(); i++) {
            String phrase = phrases.get(i);
            if (phrase != null && !phrase.isBlank()) {
                double score = VectorMath.cosine(queryVector, vectorFor(phrase));
                if (score > best) {
                    best = score;
                    bestIndex = i;
                }
            }
        }
        return new Match(bestIndex, best);
    }

    /**
     * The cached (compute-once) embedding of a catalog phrase.
     */
    private float[] vectorFor(String phrase) {
        float[] cached = phraseCache.get(phrase);
        if (cached != null) {
            return cached;
        }
        synchronized (embedLock) {
            return phraseCache.computeIfAbsent(phrase, embedder::embed);
        }
    }
}
