package elite.intel.ai.mouth;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every engine this app ships voices every language it ships. The rule is pinned here because three places
 * lean on it - the stored setting the session hands back, the mouth the factory builds, and the segment the
 * settings panel would otherwise withdraw - and all three would degrade into silence rather than an error if
 * an engine's language coverage ever regressed.
 */
class TtsProviderLanguageTest {

    @Test
    void everyEngineVoicesEveryLanguage() {
        for (Language language : Language.values()) {
            for (TtsProvider provider : TtsProvider.values()) {
                assertTrue(provider.canVoice(language), provider + " should carry every language: " + language);
            }
        }
    }

    @Test
    void aSelectionThatCanVoiceTheLanguageIsNeverSecondGuessed() {
        for (Language language : Language.values()) {
            for (TtsProvider selected : TtsProvider.values()) {
                assertEquals(selected, TtsProvider.forLanguage(selected, language),
                        "a usable selection is never second-guessed: " + selected + " under " + language);
            }
        }
    }
}

