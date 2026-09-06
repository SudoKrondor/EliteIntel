package elite.intel.ui.i18n;

import elite.intel.i18n.Language;
import elite.intel.util.NumberWords;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The commander hears a figure spelled out, because that is the only way a TTS engine reads one reliably,
 * and then reads the same sentence in the chat log and on the overlay - where "one thousand two hundred
 * twenty-four tonnes" is what they asked us to stop showing them.
 * <p>
 * The lines below are the shape the companion actually produces.
 */
class DisplayNumeralsTest {

    @Test
    void aSpelledFigureIsShownAsDigitsInTheCommandersOwnFormat() {
        String spoken = "We still need to buy one thousand two hundred twenty-four tonnes of aluminium here.";

        assertEquals("We still need to buy 1,224 tonnes of aluminium here.",
                DisplayNumerals.digits(spoken, Language.EN));
    }

    /**
     * The property the whole thing rests on: whatever the speller says a figure sounds like, this reads back
     * as that figure, in every language and at every magnitude the app speaks. Round-tripping through
     * {@link NumberWords} - the class that spells the numbers in the first place - is what proves it, and it
     * keeps proving it if the spelling rules ever change under us.
     */
    @Test
    void everythingTheAppSpellsOutCanBeReadBack() {
        for (Language language : Language.values()) {
            for (long value : new long[]{100, 999, 1224, 8450, 343_000, 45_132_120, 1_020_000_000L}) {
                String spoken = NumberWords.of(value, language);
                assertEquals(LocalizedNumbers.grouped(value, language), DisplayNumerals.digits(spoken, language),
                        language + " could not read back \"" + spoken + "\"");
            }
            String fraction = NumberWords.of(1.02, language);
            assertEquals(LocalizedNumbers.decimal(1.02, language), DisplayNumerals.digits(fraction, language),
                    language + " could not read back \"" + fraction + "\"");
        }
    }

    /**
     * The model is handed the figure spelled one way and may write it another - the payload says "one
     * thousand two hundred twenty-four" and the sentence comes back "twelve hundred twenty-four". Reading the
     * words is what survives that; matching the string we generated would not.
     */
    @Test
    void aFigureTheModelRephrasedIsStillRead() {
        assertEquals("Twelve hundred twenty-four tonnes remain.".replace("Twelve hundred twenty-four", "1,224"),
                DisplayNumerals.digits("Twelve hundred twenty-four tonnes remain.", Language.EN));
    }

    /**
     * Large credit amounts are spoken rounded and hedged, and the fraction is part of the figure.
     */
    @Test
    void aFractionalFigureIsShownWhateverItsSize() {
        assertEquals("You have about 1.02 billion credits.",
                DisplayNumerals.digits("You have about one point zero two billion credits.", Language.EN));
    }

    /**
     * Spelled numbers are ordinary words too. Every line here parses as a number and none of them is one.
     */
    @Test
    void ordinaryProseIsLeftAlone() {
        for (String prose : new String[]{
                "One of the systems is hostile.",
                "No one answered the hail.",
                "An onerous contract, that one.",   // "onerous" begins with "one"
                "Often the tenders arrive late.",   // "often" and "tenders" both contain "ten"
                "Docking at the second station.",   // "second" parses as 2
                "Four hours ago.",
                "Nothing numeric here."}) {
            assertEquals(prose, DisplayNumerals.digits(prose, Language.EN));
        }
    }

    /**
     * Elite's system names are half numbers, and grouping the digits inside one would corrupt it.
     */
    @Test
    void namesThatCarryDigitsKeepTheirShape() {
        for (String name : new String[]{
                "Route plotted to Col 285 Sector XY-Z c12-34.",
                "Arriving at HIP 22460 in 3 jumps.",
                "Entered channel: Prua Phoe IY-B b58-2."}) {
            assertEquals(name, DisplayNumerals.digits(name, Language.EN));
        }
    }

    /**
     * The spelling differs far more than a hand-written table would capture - German and Italian run the
     * parts together, French counts in twenties, Portuguese joins them with a conjunction - and the same CLDR
     * rules that produced each form read it back. The expected digits come from the formatter rather than
     * being typed out, because the group separator is the locale's business (several of these use a
     * non-breaking space) and is not what this test is pinning.
     */
    @Test
    void everyLanguageReadsItsOwnSpelling() {
        assertEquals(expected(Language.DE, "Tonnen"),
                DisplayNumerals.digits("eintausendzweihundertvierundzwanzig Tonnen", Language.DE));
        assertEquals(expected(Language.RU, "тонны"),
                DisplayNumerals.digits("одна тысяча двести двадцать четыре тонны", Language.RU));
        assertEquals(expected(Language.UK, "тонни"),
                DisplayNumerals.digits("одна тисяча двісті двадцять чотири тонни", Language.UK));
        assertEquals(expected(Language.FR, "tonnes"),
                DisplayNumerals.digits("mille deux cent vingt-quatre tonnes", Language.FR));
        assertEquals(expected(Language.IT, "tonnellate"),
                DisplayNumerals.digits("milleduecentoventiquattro tonnellate", Language.IT));
        assertEquals(expected(Language.ES, "toneladas"),
                DisplayNumerals.digits("mil doscientos veinticuatro toneladas", Language.ES));
        assertEquals(expected(Language.PT, "toneladas"),
                DisplayNumerals.digits("mil duzentos e vinte e quatro toneladas", Language.PT));
        assertEquals(expected(Language.PTBZ, "toneladas"),
                DisplayNumerals.digits("mil duzentos e vinte e quatro toneladas", Language.PTBZ));
    }

    private static String expected(Language language, String unit) {
        return LocalizedNumbers.grouped(1224, language) + " " + unit;
    }

    /**
     * Reading a word back is expensive - about a millisecond, because the parse matches through a collator -
     * so a line is only handed words that could begin a number. Without that filter a long line cost eighty
     * seconds, on the thread that is about to speak it.
     * <p>
     * The bound is deliberately loose: the gap being guarded is between milliseconds and a minute, so two
     * seconds catches the regression without failing on a loaded machine.
     */
    @Test
    void aLongLineOfOrdinaryWordsIsNotParsedWordByWord() {
        String longLine = "present ".repeat(600); // what the Edge splitter test speaks

        long start = System.nanoTime();
        String shown = DisplayNumerals.digits(longLine, Language.EN);
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertEquals(longLine, shown);
        assertTrue(millis < 2_000, "reading a 4,800 character line took " + millis + " ms");
    }

    @Test
    void nothingToReadIsReturnedUntouched() {
        assertEquals(null, DisplayNumerals.digits(null, Language.EN));
        assertEquals("", DisplayNumerals.digits("", Language.EN));
        assertEquals("ok", DisplayNumerals.digits("ok", Language.EN));
    }
}
