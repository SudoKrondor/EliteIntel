package elite.intel.ai.embed;

/**
 * Turns a piece of text into a dense vector ("embedding") whose direction encodes meaning: texts that
 * mean similar things produce vectors that point in similar directions, so closeness ({@link VectorMath#cosine})
 * measures semantic similarity rather than spelling overlap. This is the seam the companion's command
 * selection and memory recall can sit on instead of the word-overlap reducers, so that inflected forms
 * ({@code авианосец / авианосцу / авианосцем}) and synonyms ({@code курс на авианосец / лети к авианосцу})
 * match without per-language declension or synonym tables.
 * <p>
 * Implementations are expected to L2-normalise their output (unit length), so {@link VectorMath#cosine}
 * reduces to a dot product and vectors can be compared directly. The contract leaks no model detail
 * (no ONNX, no tokenizer), so the backing model can be swapped without touching callers.
 */
public interface TextEmbedder {

    /**
     * Embeds a single piece of text into a unit-length vector.
     *
     * @param text the text to embed; must not be null. Blank text yields a valid (if uninformative) vector.
     * @return a newly allocated, L2-normalised embedding vector
     */
    float[] embed(String text);

    /**
     * The dimensionality of the vectors this embedder produces (e.g. 384 for multilingual-e5-small).
     */
    int dimensions();
}
