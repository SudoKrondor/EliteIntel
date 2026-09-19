package elite.intel.ai.brain.actions.handlers.queries;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The narration instruction asks for the capacity only when the payload carries one, so the payload must
 * carry none for a material learned from the journal: a cap of 0 sent to the model reads as a full hold of
 * nothing.
 */
class AnalyseMaterialsQueryPayloadTest {

    @Test
    void anUnknownCapAndGradeAreLeftOutOfThePayload() {
        String yaml = new AnalyseMaterialsQuery.MaterialDataDto("Rhodiumstaub", "Raw", null, 3, null).toYaml();

        assertTrue(yaml.contains("amount: 3"));
        assertFalse(yaml.contains("maxCap"), yaml);
        assertFalse(yaml.contains("grade"), yaml);
    }

    @Test
    void aKnownCapIsStated() {
        String yaml = new AnalyseMaterialsQuery.MaterialDataDto("Iron", "Raw", 1, 3, 300).toYaml();

        assertTrue(yaml.contains("maxCap: 300"), yaml);
        assertTrue(yaml.contains("grade: 1"), yaml);
    }
}
