package elite.intel.ai.mouth.supertonic;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SupertonicTextNormalizerTest {

    @Test
    void keepsLongUppercaseWordsTogether() {
        assertEquals("RIATTIVA il elle elle emme",
                SupertonicTextNormalizer.normalize("RIATTIVA il LLM", Language.IT));
    }

    @Test
    void spellsEnglishAcronymsUsingEnglishLetterNames() {
        assertEquals("Connected to ell ell em",
                SupertonicTextNormalizer.normalize("Connected to LLM", Language.EN));
    }

    @Test
    void spellsAcronymsUsingLetterNamesForEverySupportedLanguage() {
        assertEquals("ell ell em", SupertonicTextNormalizer.normalize("LLM", Language.EN));
        assertEquals("ell ell emm", SupertonicTextNormalizer.normalize("LLM", Language.DE));
        assertEquals("elle elle emme", SupertonicTextNormalizer.normalize("LLM", Language.FR));
        assertEquals("ele ele eme", SupertonicTextNormalizer.normalize("LLM", Language.ES));
        assertEquals("elle elle emme", SupertonicTextNormalizer.normalize("LLM", Language.IT));
        assertEquals("ele ele eme", SupertonicTextNormalizer.normalize("LLM", Language.PT));
        assertEquals("ele ele eme", SupertonicTextNormalizer.normalize("LLM", Language.PTBZ));
    }

    @Test
    void spellsNumbersInHyphenatedSystemCodes() {
        assertEquals("see twelve dash thirty-four",
                SupertonicTextNormalizer.normalize("c12-34", Language.EN));
        assertEquals("ci dodici trattino trentaquattro",
                SupertonicTextNormalizer.normalize("c12-34", Language.IT));
    }

    @Test
    void spellsItalianDecimalValuesAttachedToSingleLetterUnits() {
        assertEquals("gravità zero virgola tre cinque gi",
                SupertonicTextNormalizer.normalize("gravità 0.35G", Language.IT));
    }

    @Test
    void spellsDecimalValuesAttachedToSingleLetterUnitsInEnglish() {
        assertEquals("gravity zero point three five gee",
                SupertonicTextNormalizer.normalize("gravity 0.35G", Language.EN));
    }

    @Test
    void acceptsCommaAsItalianDecimalSeparator() {
        assertEquals("gravità tre virgola tre otto gi",
                SupertonicTextNormalizer.normalize("gravità 3,38G", Language.IT));
    }

    @Test
    void preservesPunctuationAfterDecimalUnits() {
        assertEquals("gravity zero point three five gee, next",
                SupertonicTextNormalizer.normalize("gravity 0.35G, next", Language.EN));
    }

    @Test
    void spellsItalianCommaDecimalCoordinatesAsWholeDecimals() {
        assertEquals("Coordinate del Sole Flickiano: centotrentacinque virgola due tre quattro, "
                        + "centoventitré virgola quattro cinque sei, zero virgola zero zero uno.",
                SupertonicTextNormalizer.normalize(
                        "Coordinate del Sole Flickiano: 135,234, 123,456, 0.001.", Language.IT));
    }

    @Test
    void spellsItalianDecimalsWithGroupedThousands() {
        assertEquals("valore milleduecentotrentaquattro virgola cinque sei",
                SupertonicTextNormalizer.normalize("valore 1.234,56", Language.IT));
    }

    @Test
    void keepsSingleUppercaseLettersUnchanged() {
        assertEquals("A new ship",
                SupertonicTextNormalizer.normalize("A new ship", Language.EN));
    }

    @Test
    void spellsItalianClassLettersWithItalianNames() {
        assertEquals("classe ipsilon",
                SupertonicTextNormalizer.normalize("classe Y", Language.IT));
    }
}
