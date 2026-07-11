package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.ActionParameterSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculateNeutronStarRouteCommandTest {

    @Test
    void efficiencyIsOptionalSoTheHandlerCanRequestAMissingValue() {
        ActionParameterSpec efficiency = new CalculateNeutronStarRouteCommand().parameters().getFirst();

        assertEquals("efficiency", efficiency.getName());
        assertFalse(efficiency.isRequired());
        assertTrue(efficiency.getDescription().contains("Omit it when the commander did not state"));
        assertTrue(efficiency.getExtractionHint().contains("never infer or choose a default"));
    }
}
