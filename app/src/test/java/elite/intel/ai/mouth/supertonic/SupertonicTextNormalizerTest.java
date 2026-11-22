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
    void readsMarkNumbersInEverySupportedLanguage() {
        assertEquals("mark one, mark two", SupertonicTextNormalizer.normalize("MK I, MK II", Language.EN));
        assertEquals("Mark eins, Mark zwei", SupertonicTextNormalizer.normalize("MK I, MK II", Language.DE));
        assertEquals("mark un, mark deux", SupertonicTextNormalizer.normalize("MK I, MK II", Language.FR));
        assertEquals("mark uno, mark dos", SupertonicTextNormalizer.normalize("MK I, MK II", Language.ES));
        assertEquals("mark uno, mark due", SupertonicTextNormalizer.normalize("MK I, MK II", Language.IT));
        assertEquals("mark um, mark dois", SupertonicTextNormalizer.normalize("MK I, MK II", Language.PT));
        assertEquals("mark um, mark dois", SupertonicTextNormalizer.normalize("MK I, MK II", Language.PTBZ));
        assertEquals("марк один, марк два", SupertonicTextNormalizer.normalize("MK I, MK II", Language.RU));
        assertEquals("марк один, марк два", SupertonicTextNormalizer.normalize("MK I, MK II", Language.UK));
    }

    @Test
    void readsShipMarkNumbersWithOrWithoutSpacesAndRegardlessOfCase() {
        assertEquals("Kestrel mark two, Krait mark two, Cobra mark three, Viper mark four, Cobra mark five.",
                SupertonicTextNormalizer.normalize(
                        "Kestrel Mk II, Krait MkII, Cobra MkIII, Viper MKIV, Cobra mkv.", Language.EN));
    }

    @Test
    void readsMarkAbbreviatedWithADot() {
        assertEquals("Cobra mark three, Krait mark two",
                SupertonicTextNormalizer.normalize("Cobra Mk. III, Krait Mk.II", Language.EN));
    }

    @Test
    void readsMarkWrittenOut() {
        assertEquals("Cobra mark three, Viper mark four, Krait mark two",
                SupertonicTextNormalizer.normalize("Cobra Mark III, Viper mark IV, Krait MARK II", Language.EN));
    }

    @Test
    void leavesTheWordMarkAloneWhenNoHullNumeralFollows() {
        assertEquals("Mark I think, mark v here",
                SupertonicTextNormalizer.normalize("Mark I think, mark v here", Language.EN));
    }

    @Test
    void readsMarkWrittenInCyrillic() {
        assertEquals("Кобра марк три, Кобра марк четыре, Кобра марк пять, Кобра марк два",
                SupertonicTextNormalizer.normalize("Кобра Мк III, Кобра МК IV, Кобра мк. V, Кобра Марк II", Language.RU));
        assertEquals("Кобра марк три",
                SupertonicTextNormalizer.normalize("Кобра Мк III", Language.UK));
    }

    @Test
    void doesNotMatchPartsOfLongerWordsOrUnsupportedRomanNumerals() {
        assertEquals("remark MkIIx MkII2 _MkII MkII_ MkVI",
                SupertonicTextNormalizer.normalize("remark MkIIx MkII2 _MkII MkII_ MkVI", Language.EN));
    }

    @Test
    void spellsEliteRanksInEverySupportedLanguage() {
        assertEquals("Elite one, Elite two", SupertonicTextNormalizer.normalize("Elite I, Elite II", Language.EN));
        assertEquals("Elite eins, Elite zwei", SupertonicTextNormalizer.normalize("Elite I, Elite II", Language.DE));
        assertEquals("Élite un, Élite deux", SupertonicTextNormalizer.normalize("Élite I, Élite II", Language.FR));
        assertEquals("Élite uno, Élite dos", SupertonicTextNormalizer.normalize("Élite I, Élite II", Language.ES));
        assertEquals("Elite uno, Elite due", SupertonicTextNormalizer.normalize("Elite I, Elite II", Language.IT));
        assertEquals("Elite um, Elite dois", SupertonicTextNormalizer.normalize("Elite I, Elite II", Language.PT));
        assertEquals("Elite um, Elite dois", SupertonicTextNormalizer.normalize("Elite I, Elite II", Language.PTBZ));
        assertEquals("Элита один, Элита два", SupertonicTextNormalizer.normalize("Элита I, Элита II", Language.RU));
        assertEquals("Еліта один, Еліта два", SupertonicTextNormalizer.normalize("Еліта I, Еліта II", Language.UK));
    }

    @Test
    void spellsAllEliteRanksAndKeepsTheUnnumberedRank() {
        assertEquals("Elite, Elite one, Elite two, Elite three, Elite four, ELITE five.",
                SupertonicTextNormalizer.normalize(
                        "Elite, Elite I, Elite II, Elite III, Elite IV, ELITE V.", Language.EN));
        assertEquals("élite три, ÉLITE четыре, ЭЛИТА пять",
                SupertonicTextNormalizer.normalize("élite III, ÉLITE IV, ЭЛИТА V", Language.RU));
    }

    @Test
    void doesNotTreatOtherWordsOrIncompleteEliteRankMatchesAsRanks() {
        assertEquals("Superelite IIx, Elite i piloti, Elite IIx",
                SupertonicTextNormalizer.normalize("Superelite IIx, Elite i piloti, Elite IIx", Language.IT));
    }

    @Test
    void doesNotReadThePronounAfterEliteAsARank() {
        assertEquals("Now that you are Elite I can open the door.",
                SupertonicTextNormalizer.normalize("Now that you are Elite I can open the door.", Language.EN));
        assertEquals("Since you reached Elite I think you are ready.",
                SupertonicTextNormalizer.normalize("Since you reached Elite I think you are ready.", Language.EN));
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
    void readsSeveralThousandsGroupsAsOneNumberWhicheverSeparatorTheModelUsed() {
        assertEquals("Hai un milione duecentotrentaquattromilacinquecentosessantasette crediti.",
                SupertonicTextNormalizer.normalize("Hai 1,234,567 crediti.", Language.IT));
        assertEquals("Hai un milione duecentotrentaquattromilacinquecentosessantasette crediti.",
                SupertonicTextNormalizer.normalize("Hai 1.234.567 crediti.", Language.IT));
        assertEquals("You have one million two hundred thirty-four thousand five hundred sixty-seven credits.",
                SupertonicTextNormalizer.normalize("You have 1,234,567 credits.", Language.EN));
    }

    @Test
    void readsAFractionAfterTheOtherSeparatorAsGrouping() {
        assertEquals("one thousand two hundred thirty-four point five",
                SupertonicTextNormalizer.normalize("1,234.5", Language.EN));
        assertEquals("milleduecentotrentaquattro virgola cinque",
                SupertonicTextNormalizer.normalize("1,234.5", Language.IT));
    }

    @Test
    void readsASingleCommaGroupAsThousandsOnlyInEnglish() {
        assertEquals("one thousand two hundred thirty-four tons",
                SupertonicTextNormalizer.normalize("1,234 tons", Language.EN));
        assertEquals("zero virgola zero zero uno",
                SupertonicTextNormalizer.normalize("0.001", Language.IT));
    }

    @Test
    void spellsWholeSectorCodes() {
        assertEquals("Synuefe ex oh dash ar bee nineteen dash three",
                SupertonicTextNormalizer.normalize("Synuefe XO-R b19-3", Language.EN));
        assertEquals("Swoilz ay ay dash ay aitch zero",
                SupertonicTextNormalizer.normalize("Swoilz AA-A h0", Language.EN));
        assertEquals("Col two hundred eighty-five Sector ex why dash zee see twelve dash thirty-four",
                SupertonicTextNormalizer.normalize("Col 285 Sector XY-Z c12-34", Language.EN));
        assertEquals("Synuefe iks oh Bindestrich err beh neunzehn Bindestrich drei",
                SupertonicTextNormalizer.normalize("Synuefe XO-R b19-3", Language.DE));
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
