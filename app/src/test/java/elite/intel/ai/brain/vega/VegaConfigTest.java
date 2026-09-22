package elite.intel.ai.brain.vega;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests fixed VEGA settings that affect all command turns.
 */
class VegaConfigTest {

    @Test
    void llmDeadlineLeavesHeadroomBelowThoughtWatchdog() {
        assertTrue(VegaConfig.llmLogicalDeadline().compareTo(VegaConfig.thoughtWatchdogTimeout()) < 0);
    }
}
