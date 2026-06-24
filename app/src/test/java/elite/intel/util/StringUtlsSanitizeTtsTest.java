package elite.intel.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the unicode handling in {@link StringUtls#sanitizeTts(String)}.
 * <p>
 * The Kokoro/espeak phonemizer skips combining marks it has no phoneme for
 * (e.g. the IPA syllabic marker U+0329), which produces audio artifacts. The
 * sanitizer must drop such stray marks while preserving the precomposed
 * accented letters used by the supported non-English languages.
 * <p>
 * Combining-mark inputs are built with explicit \\u escapes so the source file
 * encoding cannot silently pre-compose them.
 */
class StringUtlsSanitizeTtsTest {

    @Test
    void stripsStrayCombiningMarkThatPhonemizerCannotPronounce() {
        // "test" + U+0329 (COMBINING VERTICAL LINE BELOW) + "ing"
        String input = "test̩ing";

        String result = StringUtls.sanitizeTts(input);

        assertFalse(result.contains("̩"), "stray combining mark should be removed");
        // sanitizer also title-cases the leading char; that is not what this test guards.
        assertTrue(result.equalsIgnoreCase("testing"), "word should be intact, got: " + result);
    }

    @Test
    void preservesPrecomposedAccentedLettersForSupportedLanguages() {
        // German, French, Spanish precomposed letters (category L, not M).
        String input = "Grün Système Niño"; // Grün Système Niño

        String result = StringUtls.sanitizeTts(input);

        assertTrue(result.contains("Grün"), "German umlaut should survive");
        assertTrue(result.contains("Système"), "French accents should survive");
        assertTrue(result.contains("Niño"), "Spanish tilde-n should survive");
    }

    @Test
    void preservesCyrillicLettersForRussianAndUkrainian() {
        // й (Russian U+0439) and ї/і (Ukrainian) are precomposed letters.
        String input = "Привет Київ"; // Привет Київ

        String result = StringUtls.sanitizeTts(input);

        assertTrue(result.contains("Привет"), "Russian letters should survive");
        assertTrue(result.contains("Київ"), "Ukrainian letters should survive");
    }

    @Test
    void composesDecomposedAccentsSoTheySurviveTheMarkStrip() {
        // "cafe" + U+0301 (COMBINING ACUTE ACCENT) on the e must fold into the single
        // precomposed letter é, not be stripped as a combining mark.
        String decomposed = "café";

        String result = StringUtls.sanitizeTts(decomposed);

        assertTrue(result.toLowerCase().contains("café"), "decomposed accent should compose to precomposed é, got: " + result);
        assertFalse(result.contains("́"), "no bare combining accent should remain");
    }
}