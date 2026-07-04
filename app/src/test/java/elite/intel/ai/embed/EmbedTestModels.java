package elite.intel.ai.embed;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared test helper: locates the bundled embedding model so model-gated tests stay location-independent.
 */
public final class EmbedTestModels {

    private EmbedTestModels() {
    }

    /**
     * Walks up from the test working directory to find {@code distribution/embed/multilingual-e5-small},
     * returning its path or {@code null} if the model is not present (so callers can skip).
     */
    public static Path locateModelDir() {
        Path dir = Path.of("").toAbsolutePath();
        for (int up = 0; up < 4 && dir != null; up++, dir = dir.getParent()) {
            Path candidate = dir.resolve("distribution/embed/multilingual-e5-small");
            if (Files.exists(candidate.resolve("model_quantized.onnx"))) {
                return candidate;
            }
        }
        return null;
    }
}
