package elite.intel.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The companion-mode toggle (DB-backed) and the confirmation code-word matcher.
 */
class CompanionConfigTest {

    @Test
    void companionModeForcedOn() {
        // Companion mode is hardcoded ON in SystemSession.companionModeOn() (which CompanionConfig
        // delegates to): testers are forced onto companion mode ahead of retiring the legacy LLM
        // pipeline. Restore the DB-backed read there to make this user-selectable again.
        assertTrue(CompanionConfig.companionModeOn());
    }

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
