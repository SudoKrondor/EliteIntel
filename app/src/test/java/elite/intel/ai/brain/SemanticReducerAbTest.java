package elite.intel.ai.brain;

import elite.intel.ai.embed.EmbedTestModels;
import elite.intel.ai.embed.OnnxTextEmbedder;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The point of the whole exercise, as an A/B: a Russian input in a different case than the trigger word.
 * Word-overlap drops the action (the LLM never sees it); semantic keeps it. Loads the real model, so it is
 * tagged {@code embedding-manual} and excluded from the default run. Run: {@code ./gradlew embeddingTest}.
 */
@Tag("embedding-manual")
class SemanticReducerAbTest {

    private static Map<String, String> map() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("авианосец", "carrier_status"); // trigger is the nominative ("carrier")
        m.put("выбросить помехи", "deploy_chaff");
        m.put("грузовой люк", "cargo_hatch");
        return m;
    }

    /**
     * Dative case of "carrier" — the commander says it inflected; the trigger word is the nominative.
     */
    private static final String INFLECTED_INPUT = "авианосцу";

    @Test
    void wordOverlapDropsInflectedFormButSemanticKeepsIt() {
        Path modelDir = EmbedTestModels.locateModelDir();
        Assumptions.assumeTrue(modelDir != null,
                "distribution/embed/multilingual-e5-small not found — skipping");

        // Word-overlap: "авианосцу" != "авианосец", no shared word → action is dropped (bug reproduced).
        Map<String, String> wordOverlap = Reducer.wordOverlapReduce(INFLECTED_INPUT, map(), false);
        assertFalse(wordOverlap.containsValue("carrier_status"),
                "word-overlap should drop the inflected form (this is the bug)");

        // Semantic: cosine sees them as the same word → action survives for the LLM to pick.
        try (OnnxTextEmbedder embedder = new OnnxTextEmbedder(modelDir, 2)) {
            SemanticPhraseMatcher matcher = new SemanticPhraseMatcher(embedder);
            Map<String, String> semantic = Reducer.semanticReduce(matcher, INFLECTED_INPUT, map(), false);
            System.out.println("Semantic reduced set for '" + INFLECTED_INPUT + "': " + semantic);
            assertTrue(semantic.containsValue("carrier_status"),
                    "semantic should keep the inflected form (the fix)");
            assertFalse(semantic.containsValue("deploy_chaff"),
                    "unrelated commands should still be filtered out");
        }
    }

}
