package elite.intel.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A search radius arrives as whatever the model made of what the commander said. When it converted, these
 * are digits; when it echoed, they are the words speech-to-text wrote. Reading only the first meant the
 * second silently became the default radius - the failure this class exists to prevent.
 */
class SpokenNumbersTest {

    @Test
    void digitsAreReadAsThemselves() {
        assertEquals(200, SpokenNumbers.parse("200"));
        assertEquals(80, SpokenNumbers.parse("80 ly"));
        assertEquals(150, SpokenNumbers.parse("150 light years"));
    }

    @Test
    void theRadiusFromTheLoggedTurnIsRead() {
        // "find where we can buy neo fabric insulation within two hundred light years" - the turn that
        // searched 40 ly and reported the commodity as nowhere to be found.
        assertEquals(200, SpokenNumbers.parse("two hundred"));
    }

    @Test
    void englishWordsAreRead() {
        assertEquals(100, SpokenNumbers.parse("one hundred"));
        assertEquals(100, SpokenNumbers.parse("hundred"), "a bare hundred is one hundred, not zero");
        assertEquals(50, SpokenNumbers.parse("fifty"));
        assertEquals(1000, SpokenNumbers.parse("one thousand"));
        assertEquals(1200, SpokenNumbers.parse("one thousand two hundred"));
    }

    @Test
    void theOtherShippedLanguagesAreRead() {
        assertEquals(200, SpokenNumbers.parse("zweihundert"), "German writes it as one word");
        assertEquals(200, SpokenNumbers.parse("deux cents"));
        assertEquals(200, SpokenNumbers.parse("doscientos"));
        assertEquals(200, SpokenNumbers.parse("duzentos"));
        assertEquals(200, SpokenNumbers.parse("duecento"));
        assertEquals(200, SpokenNumbers.parse("двести"));
        assertEquals(200, SpokenNumbers.parse("двісті"));
    }

    @Test
    void accentsDoNotHideANumber() {
        assertEquals(50, SpokenNumbers.parse("cinquenta"));
        assertEquals(100, SpokenNumbers.parse("cem"));
    }

    @Test
    void surroundingWordsAreIgnored() {
        assertEquals(200, SpokenNumbers.parse("within two hundred light years"));
        assertEquals(200, SpokenNumbers.parse("dans un rayon de deux cents années-lumière"));
    }

    @Test
    void textWithNoNumberIsAbsentRatherThanZero() {
        // The caller falls back to its own default on null; a 0 would search nothing at all.
        assertNull(SpokenNumbers.parse("light years"));
        assertNull(SpokenNumbers.parse(""));
        assertNull(SpokenNumbers.parse(null));
        assertNull(SpokenNumbers.parse("as far as you can"));
    }
}
