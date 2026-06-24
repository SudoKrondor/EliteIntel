package elite.intel.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The companion-mode toggle (DB-backed) and the confirmation code-word matcher.
 */
class CompanionConfigTest {

    @Test
    void companionModeDefaultsOff() {
        // DB-backed flag (parallel to conversation mode); the seeded game_session row defaults it off.
        assertFalse(CompanionConfig.companionModeOn());
    }

    @Test
    void confirmationCodeWordMatchesTrimmedAndCaseInsensitive() {
        assertTrue(CompanionConfig.isConfirmationCodeWord(CompanionConfig.confirmationCodeWord()));
        assertTrue(CompanionConfig.isConfirmationCodeWord("  Password "));
        assertFalse(CompanionConfig.isConfirmationCodeWord("not the word"));
        assertFalse(CompanionConfig.isConfirmationCodeWord(null));
    }
}
