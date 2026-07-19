package elite.intel.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests fixed companion settings that affect all command turns. */
class CompanionConfigTest {

    @Test
    void confirmationCodeWordMatchesTrimmedAndCaseInsensitive() {
        assertTrue(CompanionConfig.isConfirmationCodeWord(CompanionConfig.confirmationCodeWord()));
        assertTrue(CompanionConfig.isConfirmationCodeWord("  Password "));
        assertFalse(CompanionConfig.isConfirmationCodeWord("not the word"));
        assertFalse(CompanionConfig.isConfirmationCodeWord(null));
    }

    @Test
    void llmDeadlineLeavesHeadroomBelowThoughtWatchdog() {
        assertTrue(CompanionConfig.llmLogicalDeadline().compareTo(CompanionConfig.thoughtWatchdogTimeout()) < 0);
    }
}
