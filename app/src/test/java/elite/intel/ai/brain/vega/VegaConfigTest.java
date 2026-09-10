package elite.intel.ai.brain.vega;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests fixed VEGA settings that affect all command turns.
 */
class VegaConfigTest {

    @Test
    void confirmationCodeWordMatchesTrimmedAndCaseInsensitive() {
        assertTrue(VegaConfig.isConfirmationCodeWord(VegaConfig.confirmationCodeWord()));
        assertTrue(VegaConfig.isConfirmationCodeWord("  Password "));
        assertFalse(VegaConfig.isConfirmationCodeWord("not the word"));
        assertFalse(VegaConfig.isConfirmationCodeWord(null));
    }

    @Test
    void llmDeadlineLeavesHeadroomBelowThoughtWatchdog() {
        assertTrue(VegaConfig.llmLogicalDeadline().compareTo(VegaConfig.thoughtWatchdogTimeout()) < 0);
    }
}
