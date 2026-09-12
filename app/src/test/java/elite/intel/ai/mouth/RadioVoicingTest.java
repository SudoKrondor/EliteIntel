package elite.intel.ai.mouth;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins which engine voices radio. A local engine always does: Kokoro wherever it can pronounce the language,
 * Supertonic for the Cyrillic locales Kokoro's phonemizer cannot read - and whichever local engine is already
 * the main mouth, so two ONNX models are never resident for one channel.
 */
class RadioVoicingTest {

    @Test
    void underACloudMainMouthCyrillicIsVoicedBySupertonicAndEveryOtherLanguageByKokoro() {
        for (TtsProvider cloud : new TtsProvider[]{TtsProvider.GOOGLE, TtsProvider.EDGE}) {
            for (Language language : Language.values()) {
                assertEquals(
                        language.isCyrillicScript() ? TtsProvider.SUPERTONIC : TtsProvider.KOKORO,
                        RadioVoicing.engineFor(language, cloud),
                        "radio engine for " + language + " under " + cloud);
            }
        }
    }

    @Test
    void aLocalMainMouthVoicesItsOwnRadioWhateverTheLanguage() {
        for (TtsProvider local : new TtsProvider[]{TtsProvider.KOKORO, TtsProvider.SUPERTONIC}) {
            for (Language language : Language.values()) {
                if (!local.canVoice(language)) continue; // never the main mouth there - see TtsProvider.forLanguage
                assertEquals(local, RadioVoicing.engineFor(language, local),
                        "a commander on " + local + " must not get a second local engine loaded for radio: " + language);
            }
        }
    }

    @Test
    void radioIsAlwaysLocalAndNeverBillsGoogle() {
        for (TtsProvider main : TtsProvider.values()) {
            for (Language language : Language.values()) {
                TtsProvider radio = RadioVoicing.engineFor(language, main);
                assertTrue(radio.isLocal(), "radio is chatter, not narration: " + language + " under " + main);
                assertNotEquals(TtsProvider.GOOGLE, radio);
            }
        }
    }
}
