package elite.intel.ai.brain;

import elite.intel.ai.brain.actions.command.builtin.IgnoreNonsensicalInputCommand;
import elite.intel.ai.brain.actions.handlers.query.GeneralConversationQueryCommand;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.TextEmbedder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the semantic selection math (floor / margin / cap) and the degradation dispatch in the default
 * suite, with no model. A stand-in {@link TextEmbedder} maps each phrase to a 2-D unit vector at a chosen
 * angle, so cosine(query, phrase) equals the cosine of that angle and thresholds can be hit exactly.
 */
class SemanticReducerLogicTest {

    private static final String QUERY = "запрос"; // arbitrary token; not equal to any trigger key

    /**
     * Deterministic embedder: a phrase's vector points at its configured angle (query points at 0 degrees).
     */
    private static final class AngleEmbedder implements TextEmbedder {
        private final Map<String, Double> degrees;

        AngleEmbedder(Map<String, Double> degrees) {
            this.degrees = degrees;
        }

        @Override
        public float[] embed(String text) {
            double radians = Math.toRadians(degrees.getOrDefault(text, 90.0));
            return new float[]{(float) Math.cos(radians), (float) Math.sin(radians)};
        }

        @Override
        public int dimensions() {
            return 2;
        }
    }

    private static SemanticPhraseMatcher matcher(Map<String, Double> phraseAngles) {
        Map<String, Double> angles = new HashMap<>(phraseAngles);
        angles.put(QUERY, 0.0); // the query points along the reference axis
        return new SemanticPhraseMatcher(new AngleEmbedder(angles));
    }

    @Test
    void belowFloorFallsBackToNonsensical() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("alpha", "act_alpha");
        map.put("bravo", "act_bravo");
        // cos 40deg ~ 0.766, below the 0.80 floor: nothing is relevant.
        Map<String, String> result = Reducer.semanticReduce(
                matcher(Map.of("alpha", 40.0, "bravo", 40.0)), QUERY, map, false);
        assertTrue(result.containsKey(IgnoreNonsensicalInputCommand.ID));
        assertFalse(result.containsValue("act_alpha"));
    }

    @Test
    void belowFloorFallsBackToConversationWhenConversational() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("alpha", "act_alpha");
        Map<String, String> result = Reducer.semanticReduce(
                matcher(Map.of("alpha", 40.0)), QUERY, map, true);
        assertTrue(result.containsKey(GeneralConversationQueryCommand.ID));
    }

    @Test
    void keepsOnlyCandidatesWithinMarginOfTheBest() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("near", "act_near"); // 0deg  -> 1.000 (best)
        map.put("mid", "act_mid");   // 10deg -> 0.985 (within the 0.04 margin, cutoff 0.96)
        map.put("far", "act_far");   // 20deg -> 0.940 (outside the margin)
        Map<String, String> result = Reducer.semanticReduce(
                matcher(Map.of("near", 0.0, "mid", 10.0, "far", 20.0)), QUERY, map, false);
        assertTrue(result.containsValue("act_near"));
        assertTrue(result.containsValue("act_mid"));
        assertFalse(result.containsValue("act_far"));
    }

    @Test
    void capsTheNumberOfSurvivingCandidates() {
        Map<String, String> map = new LinkedHashMap<>();
        Map<String, Double> angles = new HashMap<>();
        for (int i = 0; i < 30; i++) {
            map.put("phrase" + i, "act_" + i);
            angles.put("phrase" + i, 0.0); // all identical to the query, so all score 1.0
        }
        Map<String, String> result = Reducer.semanticReduce(matcher(angles), QUERY, map, false);
        assertEquals(25, result.size()); // SEM_MAX default
    }

    @Test
    void nullMatcherDegradesToWordOverlap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("carrier status", "carrier_status");
        map.put("deploy chaff", "deploy_chaff");
        // A null matcher is what reduce() passes when the model is unavailable: it must fall through to
        // word-overlap rather than fail. "carrier" overlaps the first trigger only.
        Map<String, String> result = Reducer.reduceWith(null, "show carrier status", map, false);
        assertTrue(result.containsValue("carrier_status"));
        assertFalse(result.containsValue("deploy_chaff"));
    }
}
