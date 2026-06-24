package elite.intel.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The centralized hardcoded settings and the confirmation code-word matcher. */
class CompanionConfigTest {

    @Test
    void companionModeIsHardcodedOn() {
        assertTrue(CompanionConfig.companionModeOn());
    }

    @Test
    void confirmationCodeWordMatchesTrimmedAndCaseInsensitive() {
        assertTrue(CompanionConfig.isConfirmationCodeWord(CompanionConfig.confirmationCodeWord()));
        assertTrue(CompanionConfig.isConfirmationCodeWord("  Password "));
        assertFalse(CompanionConfig.isConfirmationCodeWord("not the word"));
        assertFalse(CompanionConfig.isConfirmationCodeWord(null));
    }
}
