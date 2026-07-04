package elite.intel.ai.embed;

import java.util.Map;

/**
 * Deterministic stand-in {@link TextEmbedder} for tests: maps each known text to a 2-D unit vector at a chosen
 * angle (in degrees), so {@link VectorMath#cosine} of two texts equals the cosine of their angle difference and
 * thresholds can be hit exactly (cos 0 = 1, cos 90 = 0). An unknown text maps to the zero vector, which scores
 * cosine 0 against anything, so only the texts a test registers can match by meaning.
 */
public final class AngleEmbedder implements TextEmbedder {

    private final Map<String, Double> degrees;

    public AngleEmbedder(Map<String, Double> degrees) {
        this.degrees = degrees;
    }

    @Override
    public float[] embed(String text) {
        Double deg = degrees.get(text);
        if (deg == null) {
            return new float[]{0f, 0f};
        }
        double radians = Math.toRadians(deg);
        return new float[]{(float) Math.cos(radians), (float) Math.sin(radians)};
    }

    @Override
    public int dimensions() {
        return 2;
    }
}
