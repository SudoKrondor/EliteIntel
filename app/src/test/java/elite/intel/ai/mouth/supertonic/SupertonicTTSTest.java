package elite.intel.ai.mouth.supertonic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class SupertonicTTSTest {

    @Test
    void splitsOnSentenceEndingPunctuationButNotOnCommas() {
        assertArrayEquals(new String[]{"Hello, Commander.", "Ready?", "Go!", "Now"},
                "Hello, Commander. Ready? Go! Now".split(SupertonicTTS.SENTENCE_BOUNDARY));
    }

    @Test
    void keepsAnAbbreviatedMarkInOneSentence() {
        assertArrayEquals(new String[]{"Your Cobra Mk. III is ready.", "Launch."},
                "Your Cobra Mk. III is ready. Launch.".split(SupertonicTTS.SENTENCE_BOUNDARY));
        assertArrayEquals(new String[]{"Кобра Мк. III готова.", "Старт."},
                "Кобра Мк. III готова. Старт.".split(SupertonicTTS.SENTENCE_BOUNDARY));
    }
}
