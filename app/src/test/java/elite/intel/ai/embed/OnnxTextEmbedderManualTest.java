package elite.intel.ai.embed;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manual proof that the in-process embedder makes inflected Slavic forms cluster by meaning — the thing
 * the word-overlap reducers cannot do. Loads the real 118 MB model, so it is tagged {@code embedding-manual}
 * and excluded from the default {@code test} run.
 * <p>
 * Run it: {@code ./gradlew embeddingTest}  (the model must be in distribution/embed/multilingual-e5-small/).
 * It prints a cosine matrix and asserts that inflected forms of "carrier" sit closer to each other and to a
 * synonym phrase than to an unrelated word.
 */
@Tag("embedding-manual")
class OnnxTextEmbedderManualTest {

    /**
     * Russian declensions of "авианосец" (carrier) — the exact case where exact word-overlap fails.
     */
    private static final List<String> CARRIER_FORMS = List.of(
            "авианосец",   // nominative
            "авианосца",   // genitive
            "авианосцу",   // dative
            "авианосцем"); // instrumental

    private static final String SYNONYM_PHRASE = "курс на авианосец"; // "set course to the carrier"
    private static final String UNRELATED = "выбросить помехи";       // "deploy chaff" — different intent

    @Test
    void inflectedFormsClusterByMeaning() {
        Path modelDir = EmbedTestModels.locateModelDir();
        org.junit.jupiter.api.Assumptions.assumeTrue(modelDir != null,
                "distribution/embed/multilingual-e5-small not found — skipping (run download first)");

        try (OnnxTextEmbedder embedder = new OnnxTextEmbedder(modelDir, 2)) {
            System.out.println("\nEmbedding dimensions: " + embedder.dimensions());

            // Embed every probe once.
            List<String> all = new java.util.ArrayList<>(CARRIER_FORMS);
            all.add(SYNONYM_PHRASE);
            all.add(UNRELATED);
            float[][] vectors = new float[all.size()][];
            for (int i = 0; i < all.size(); i++) {
                vectors[i] = embedder.embed(all.get(i));
            }

            printMatrix(all, vectors);

            // Average similarity among the four inflected carrier forms (the "same word" cluster).
            double inflectionAvg = averagePairwise(vectors, 0, CARRIER_FORMS.size());
            // Similarity of the nominative to the unrelated phrase (the "different meaning" baseline).
            int unrelatedIdx = all.indexOf(UNRELATED);
            double unrelatedSim = VectorMath.cosine(vectors[0], vectors[unrelatedIdx]);
            // Similarity of the nominative to a synonym phrase (different words, same intent).
            int synonymIdx = all.indexOf(SYNONYM_PHRASE);
            double synonymSim = VectorMath.cosine(vectors[0], vectors[synonymIdx]);

            System.out.printf(Locale.ROOT,
                    "%navg(inflected forms)=%.3f   синоним('%s')=%.3f   unrelated('%s')=%.3f%n",
                    inflectionAvg, SYNONYM_PHRASE, synonymSim, UNRELATED, unrelatedSim);

            // The whole point: inflections of the same word, and a synonym phrase, are both meaningfully
            // closer than an unrelated command. This is what frees us from per-language declension tables.
            assertTrue(inflectionAvg > unrelatedSim + 0.05,
                    "Inflected forms should cluster well above the unrelated baseline");
            assertTrue(synonymSim > unrelatedSim,
                    "A synonym phrase should beat an unrelated command");
        }
    }

    private static double averagePairwise(float[][] vectors, int from, int toExclusive) {
        double sum = 0;
        int count = 0;
        for (int i = from; i < toExclusive; i++) {
            for (int j = i + 1; j < toExclusive; j++) {
                sum += VectorMath.cosine(vectors[i], vectors[j]);
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    private static void printMatrix(List<String> labels, float[][] vectors) {
        System.out.println("\nCosine similarity matrix (1.0 = identical meaning):");
        for (int i = 0; i < labels.size(); i++) {
            StringBuilder row = new StringBuilder(String.format(Locale.ROOT, "%-22s", labels.get(i)));
            for (int j = 0; j < labels.size(); j++) {
                row.append(String.format(Locale.ROOT, " %5.2f", VectorMath.cosine(vectors[i], vectors[j])));
            }
            System.out.println(row);
        }
    }

}
