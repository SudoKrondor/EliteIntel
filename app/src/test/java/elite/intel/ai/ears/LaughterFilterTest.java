package elite.intel.ai.ears;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Laughter is the engine's stand-in for noise it cannot map to words, so it must never reach the AI.
 */
class LaughterFilterTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "ha ha ha", "haha", "hahaha", "ha-ha-ha", "Ha Ha Ha!", "ha ha", "heh heh", "hehe",
            "huh huh", "hah hah", "ха-ха-ха", "хаха", "хе хе",
            "jajaja", "ja ja ja", "jeje"                       // Spanish
    })
    void discardsPureLaughter(String transcript) {
        assertTrue(LaughterFilter.isLaughter(transcript), transcript);
    }

    /**
     * The syllables never mix within one laugh, and requiring them not to is what keeps real words safe:
     * "haja" is Portuguese, and would match if an h-syllable and a j-syllable could sit side by side.
     */
    @ParameterizedTest
    @ValueSource(strings = {"haja", "hajaja", "jaha"})
    void doesNotMatchAcrossTwoSpellings(String transcript) {
        assertFalse(LaughterFilter.isLaughter(transcript), transcript);
    }

    /**
     * Laughter next to words is not noise - the commander spoke. Only whole-transcript laughter is
     * dropped, so nothing said around it can be lost.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "ha ha fire lasers", "deploy hardpoints", "what a hassle", "hallelujah", "hand me the map",
            "ha", "he", "plot a course to sol", "проложи курс"
    })
    void keepsRealSpeech(String transcript) {
        assertFalse(LaughterFilter.isLaughter(transcript), transcript);
    }

    @Test
    void treatsEmptyInputAsSpeechSoNothingIsDroppedTwice() {
        assertFalse(LaughterFilter.isLaughter(null));
        assertFalse(LaughterFilter.isLaughter("   "));
        assertFalse(LaughterFilter.isLaughter("..."));
    }
}
