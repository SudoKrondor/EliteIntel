package elite.intel.ai.embed;

/**
 * Small vector helpers for comparing embeddings. Kept separate from any embedder so the maths can be
 * unit-tested and reused by both command selection and memory recall.
 */
public final class VectorMath {

    private VectorMath() {
    }

    /**
     * Cosine similarity of two vectors: 1.0 means same direction (most similar), 0.0 unrelated, -1.0
     * opposite. Robust to vectors that are not unit length, though {@link TextEmbedder} returns
     * normalised vectors so this is effectively a dot product for embeddings.
     *
     * @throws IllegalArgumentException if the vectors differ in length
     */
    public static double cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vector length mismatch: " + a.length + " vs " + b.length);
        }
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * Scales {@code v} in place to unit (L2) length; a zero vector is left unchanged.
     */
    public static float[] normalizeInPlace(float[] v) {
        double sum = 0;
        for (float x : v) {
            sum += (double) x * x;
        }
        if (sum == 0) {
            return v;
        }
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < v.length; i++) {
            v[i] /= norm;
        }
        return v;
    }
}
