package elite.intel.ai.mouth;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kokoro is the only engine that can fail to voice a language outright, and Cyrillic is the only language it
 * fails on. The rule is pinned here because three places lean on it - the stored setting the session hands
 * back, the mouth the factory builds, and the segment the settings panel withdraws - and all three would
 * degrade into silence rather than an error if it drifted.
 */
class TtsProviderLanguageTest {

    @Test
    void onlyKokoroIsBeatenByCyrillic() {
        for (Language language : Language.values()) {
            assertTrue(TtsProvider.EDGE.canVoice(language), "Edge carries every language: " + language);
            assertTrue(TtsProvider.GOOGLE.canVoice(language), "Google carries every language: " + language);
            assertEquals(!language.isCyrillicScript(), TtsProvider.KOKORO.canVoice(language),
                    "Kokoro against " + language);
        }
    }

    @Test
    void anEngineThatCannotSpeakTheLanguageIsReplacedByTheKeylessOne() {
        for (Language language : Language.values()) {
            for (TtsProvider selected : TtsProvider.values()) {
                TtsProvider resolved = TtsProvider.forLanguage(selected, language);
                assertTrue(resolved.canVoice(language), selected + " under " + language + " resolved to " + resolved);
                if (selected.canVoice(language)) {
                    assertEquals(selected, resolved, "a usable selection is never second-guessed: " + selected);
                } else {
                    assertEquals(TtsProvider.EDGE, resolved,
                            "the stand-in must be keyless, not a paid account: " + language);
                }
            }
        }
    }

    /**
     * Google speaks Cyrillic, so a Russian commander who pays for it keeps it: this rule withdraws Kokoro, not
     * the cloud.
     */
    @Test
    void googleSurvivesACyrillicLanguage() {
        assertEquals(TtsProvider.GOOGLE, TtsProvider.forLanguage(TtsProvider.GOOGLE, Language.RU));
        assertEquals(TtsProvider.GOOGLE, TtsProvider.forLanguage(TtsProvider.GOOGLE, Language.UK));
        assertFalse(TtsProvider.KOKORO.canVoice(Language.RU));
    }
}
