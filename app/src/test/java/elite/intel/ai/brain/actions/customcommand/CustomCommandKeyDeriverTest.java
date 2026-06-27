package elite.intel.ai.brain.actions.customcommand;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class CustomCommandKeyDeriverTest {

    private static final Pattern SAFE = CustomCommandValidator.SAFE_ID;

    // --- sanitize ---

    @Test
    void sanitizeFoldsLatinDiacriticsToAscii() {
        // French accents decompose and their marks are stripped; the apostrophe becomes a separator.
        assertEquals("l_etoile_polaire", CustomCommandKeyDeriver.sanitize("l'étoile polaire"));
        // Spanish: accented vowel folds, the rest is lowercased.
        assertEquals("navegar_a_la_estacion", CustomCommandKeyDeriver.sanitize("Navegar a la estación"));
    }

    @Test
    void sanitizePreservesCyrillicLetters() {
        // Russian has no Latin decomposition, so letters are preserved (spaces become separators).
        assertEquals("лететь_к_миссии", CustomCommandKeyDeriver.sanitize("лететь к миссии"));
    }

    @Test
    void sanitizeCollapsesPunctuationAndTrims() {
        assertEquals("go_now", CustomCommandKeyDeriver.sanitize("  go!!! now???  "));
        assertEquals("drop_cargo", CustomCommandKeyDeriver.sanitize("Drop-Cargo"));
    }

    @Test
    void sanitizeOfNonLatinScriptStillMatchesSafeId() {
        assertTrue(SAFE.matcher(CustomCommandKeyDeriver.sanitize("лететь к миссии")).matches());
        assertTrue(SAFE.matcher(CustomCommandKeyDeriver.sanitize("Navegar a la estación")).matches());
    }

    @Test
    void sanitizeOfEmptyOrNullIsEmpty() {
        assertEquals("", CustomCommandKeyDeriver.sanitize(""));
        assertEquals("", CustomCommandKeyDeriver.sanitize(null));
        assertEquals("", CustomCommandKeyDeriver.sanitize("!!! ??? ..."));
    }

    // --- deriveBaseKey ---

    @Test
    void deriveBaseKeyPicksLongestPhraseRegardlessOfPosition() {
        // The most descriptive phrasing wins even when it is listed last - no "best phrase first" rule.
        String phrases = "select tool, suite tool, select suite specific tool";
        assertEquals("select_suite_specific_tool", CustomCommandKeyDeriver.deriveBaseKey(phrases));
    }

    @Test
    void deriveBaseKeyDedupesRepeatedWordsFromRunOnInput() {
        // User ignored "one per line" and ran every phrasing together; repeated words collapse.
        String runOn = "select suite specific tool select tool suite tool";
        assertEquals("select_suite_specific_tool", CustomCommandKeyDeriver.deriveBaseKey(runOn));
    }

    @Test
    void deriveBaseKeyIgnoresTooShortPhrasesInFavorOfLonger() {
        assertEquals("navigate_to_mission", CustomCommandKeyDeriver.deriveBaseKey("go, navigate to mission"));
    }

    @Test
    void deriveBaseKeyFromCyrillicPhrasesPicksLongest() {
        assertEquals("навигация_к_миссии", CustomCommandKeyDeriver.deriveBaseKey("лететь к миссии, навигация к миссии"));
    }

    @Test
    void deriveBaseKeyOfBlankIsEmpty() {
        assertEquals("", CustomCommandKeyDeriver.deriveBaseKey("   "));
        assertEquals("", CustomCommandKeyDeriver.deriveBaseKey(""));
    }

    @Test
    void deriveBaseKeyTruncatesAtWordBoundary() {
        String longPhrase = "a".repeat(70);
        String key = CustomCommandKeyDeriver.deriveBaseKey(longPhrase);
        assertTrue(key.length() <= CustomCommandValidator.MAX_ACTION_KEY_LENGTH);
        assertTrue(SAFE.matcher(key).matches());
    }

    // --- deriveUniqueKey ---

    @Test
    void deriveUniqueKeyAppendsSuffixOnCollision() {
        String key = CustomCommandKeyDeriver.deriveUniqueKey("go to mission", List.of("go_to_mission"));
        assertEquals("go_to_mission_2", key);
    }

    @Test
    void deriveUniqueKeyIgnoresCaseWhenDeduping() {
        String key = CustomCommandKeyDeriver.deriveUniqueKey("go to mission", List.of("GO_TO_MISSION"));
        assertEquals("go_to_mission_2", key);
    }

    @Test
    void deriveUniqueKeyFallsBackWhenPhrasesUnusable() {
        String key = CustomCommandKeyDeriver.deriveUniqueKey("!!!", List.of());
        assertFalse(key.isBlank());
        assertTrue(SAFE.matcher(key).matches());
    }
}
