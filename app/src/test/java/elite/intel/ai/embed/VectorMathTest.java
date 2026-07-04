package elite.intel.ai.embed;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure maths; runs in the default suite (no model needed).
 */
class VectorMathTest {

    @Test
    void identicalDirectionScoresOne() {
        float[] a = {1, 2, 3};
        float[] b = {2, 4, 6}; // same direction, different length
        assertEquals(1.0, VectorMath.cosine(a, b), 1e-6);
    }

    @Test
    void orthogonalScoresZero() {
        assertEquals(0.0, VectorMath.cosine(new float[]{1, 0}, new float[]{0, 1}), 1e-6);
    }

    @Test
    void oppositeScoresMinusOne() {
        assertEquals(-1.0, VectorMath.cosine(new float[]{1, 1}, new float[]{-1, -1}), 1e-6);
    }

    @Test
    void zeroVectorScoresZeroNotNaN() {
        assertEquals(0.0, VectorMath.cosine(new float[]{0, 0}, new float[]{1, 1}), 1e-9);
    }

    @Test
    void lengthMismatchThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> VectorMath.cosine(new float[]{1, 2}, new float[]{1, 2, 3}));
    }

    @Test
    void normalizeProducesUnitLength() {
        float[] v = VectorMath.normalizeInPlace(new float[]{3, 4});
        double len = Math.sqrt((double) v[0] * v[0] + (double) v[1] * v[1]);
        assertEquals(1.0, len, 1e-6);
    }

    @Test
    void normalizeLeavesZeroVectorUnchanged() {
        float[] v = VectorMath.normalizeInPlace(new float[]{0, 0});
        assertTrue(v[0] == 0 && v[1] == 0);
    }
}
