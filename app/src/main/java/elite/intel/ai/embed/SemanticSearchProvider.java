package elite.intel.ai.embed;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Process-wide owner of the single embedding model used for meaning-based matching. Loads the model lazily on
 * first use and hands out one shared {@link SemanticPhraseMatcher}, so every consumer - command selection (the
 * reducer) and memory recall today, retrieval (RAG) later - shares one ONNX session instead of each loading its
 * own ~130 MB model.
 * <p>
 * A load failure is not fatal: it is logged once and {@link #matcher()} returns {@code null} thereafter, so
 * callers degrade to word-based matching for the rest of the session - the same way the project degrades other
 * optional backends (STT/TTS/LLM).
 * <p>
 * Thread-safety: the returned matcher serialises its own embed calls (one ONNX session); this holder only
 * guards the one-time load.
 */
public final class SemanticSearchProvider {

    private static final Logger log = LogManager.getLogger(SemanticSearchProvider.class);

    /** Built once on first use; stays null while the model has not been loaded yet. */
    private static volatile SemanticPhraseMatcher matcher;
    /** Set once if the embedding model cannot be loaded; the session then degrades to word matching. */
    private static volatile boolean unavailable;

    private SemanticSearchProvider() {
    }

    /**
     * The shared semantic matcher, loaded on first call, or {@code null} when the embedding model could not be
     * loaded (logged once; the session then degrades to word-based matching).
     */
    public static SemanticPhraseMatcher matcher() {
        if (unavailable) {
            return null;
        }
        SemanticPhraseMatcher m = matcher;
        if (m != null) {
            return m;
        }
        synchronized (SemanticSearchProvider.class) {
            if (unavailable) {
                return null;
            }
            if (matcher == null) {
                try {
                    log.info("Loading embedding model for semantic search");
                    matcher = new SemanticPhraseMatcher(new OnnxTextEmbedder());
                } catch (RuntimeException e) {
                    // WHY: an absent or broken embedding model is a degraded optional backend, not a reason to
                    // break recall/routing; degrade to word matching for the rest of the session.
                    unavailable = true;
                    log.warn("Embedding model unavailable; semantic search disabled for this session", e);
                    return null;
                }
            }
            return matcher;
        }
    }
}
