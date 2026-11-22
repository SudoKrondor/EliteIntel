package elite.intel.junit.util;

import elite.intel.i18n.Language;
import elite.intel.util.NumberWords;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The cases the hand-rolled composition could not express, one per language family.
 *
 * <p>These are what the old templates got wrong: German heard "vierzig fünf" where it wanted the inverted
 * compound, Russian heard "три сотен двадцать" where the hundreds are a word of their own, and Spanish,
 * Portuguese and Italian each contract or join in ways a "{0} {1}" pattern cannot reach.
 */
class NumberWordsTest {

    @Test
    void germanInvertsAndRunsTogether() {
        assertEquals("fünfundvierzig", NumberWords.of(45, Language.DE));
        assertEquals("eintausenddreihundertzwanzig", NumberWords.of(1320, Language.DE));
        assertEquals("fünfundvierzig Komma eins", NumberWords.of(45.1, Language.DE));
    }

    @Test
    void frenchCountsInTwenties() {
        assertEquals("quatre-vingt-dix-sept", NumberWords.of(97, Language.FR));
        assertEquals("mille trois cent vingt", NumberWords.of(1320, Language.FR));
    }

    @Test
    void spanishContractsTheTwentiesAndOwnsItsHundreds() {
        assertEquals("veintidós", NumberWords.of(22, Language.ES));
        assertEquals("trescientos veintidós", NumberWords.of(322, Language.ES));
        assertEquals("cuarenta y cinco", NumberWords.of(45, Language.ES));
    }

    @Test
    void italianElides() {
        assertEquals("ventotto", NumberWords.of(28, Language.IT));
        assertEquals("quarantacinque", NumberWords.of(45, Language.IT));
    }

    /**
     * The two Portuguese variants are separate languages here, and their teens are where they part company.
     */
    @Test
    void portugueseVariantsSpellTheirOwnTeens() {
        assertEquals("mil trezentos e dezasseis", NumberWords.of(1316, Language.PT));
        assertEquals("mil trezentos e dezesseis", NumberWords.of(1316, Language.PTBZ));
    }

    @Test
    void slavicHundredsAreWordsOfTheirOwn() {
        assertEquals("триста сорок пять", NumberWords.of(345, Language.RU));
        assertEquals("триста сорок три", NumberWords.of(343, Language.UK));
    }

    @Test
    void englishHyphenatesTheTens() {
        assertEquals("forty-five", NumberWords.of(45, Language.EN));
        assertEquals("one thousand three hundred twenty", NumberWords.of(1320, Language.EN));
        assertEquals("one point zero two", NumberWords.of(1.02, Language.EN));
    }

    /**
     * ICU marks compound seams with a soft hyphen, which is invisible on screen and meaningless to a voice.
     * It must never survive into the spoken string - a German amount would otherwise reach the engine as
     * "fünf­und­vierzig".
     */
    @Test
    void noSoftHyphenSurvives() {
        for (Language language : Language.values()) {
            for (long value : new long[]{28, 45, 322, 680, 1320, 25_000, 999_999}) {
                String spelled = NumberWords.of(value, language);
                assertFalse(spelled.indexOf('­') >= 0,
                        language + " left a soft hyphen in " + value + ": " + spelled);
                assertFalse(spelled.matches(".*\\d.*"),
                        language + " left digits in " + value + ": " + spelled);
            }
        }
    }
}
