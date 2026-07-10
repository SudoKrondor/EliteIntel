package elite.intel.ai.embed;

import java.util.Objects;

/**
 * A per-turn semantic query embedding. It is valid only for the exact text and matcher instance that produced it,
 * which lets independent consumers reuse one ONNX inference without accidentally mixing model sessions or inputs.
 */
public final class SemanticQuery {

    private final String text;
    private final SemanticPhraseMatcher matcher;
    private final float[] vector;

    /** Captures a vector freshly produced by {@link SemanticPhraseMatcher#embedQueryContext(String)}. */
    SemanticQuery(String text, SemanticPhraseMatcher matcher, float[] vector) {
        this.text = text;
        this.matcher = matcher;
        this.vector = vector;
    }

    /**
     * Returns this query's vector only when the caller has the exact input and matcher that produced it; callers
     * must treat the returned vector as read-only.
     */
    public float[] vectorFor(String text, SemanticPhraseMatcher matcher) {
        return this.matcher == matcher && Objects.equals(this.text, text) ? vector : null;
    }
}
