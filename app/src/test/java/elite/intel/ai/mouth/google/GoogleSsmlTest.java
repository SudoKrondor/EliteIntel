package elite.intel.ai.mouth.google;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards {@link GoogleSsml#wrap(String)}: it must produce well-formed SSML, escape XML metacharacters from the
 * source text, flatten "!" to a declarative ".", and insert explicit {@code <break>} pauses after sentence ends,
 * ellipses, and commas while leaving decimals and abbreviations intact.
 */
class GoogleSsmlTest {

    @Test
    void wrapsPlainTextInSpeak() {
        assertEquals("<speak>Hello</speak>", GoogleSsml.wrap("Hello"));
    }

    @Test
    void blankInputYieldsEmptySpeak() {
        assertEquals("<speak></speak>", GoogleSsml.wrap(""));
        assertEquals("<speak></speak>", GoogleSsml.wrap("   "));
        assertEquals("<speak></speak>", GoogleSsml.wrap(null));
    }

    @Test
    void escapesXmlMetacharacters() {
        String result = GoogleSsml.wrap("a & b < c > d");

        assertTrue(result.contains("&amp;"), "ampersand escaped, got: " + result);
        assertTrue(result.contains("&lt;"), "less-than escaped, got: " + result);
        assertTrue(result.contains("&gt;"), "greater-than escaped, got: " + result);
        assertFalse(result.contains("< c"), "raw '<' from text must not survive, got: " + result);
    }

    @Test
    void insertsSentenceBreakAfterPeriod() {
        String result = GoogleSsml.wrap("Go. Now");

        assertTrue(result.contains("Go.<break time=\"300ms\"/>"), "sentence break after period, got: " + result);
    }

    @Test
    void exclamationIsSpokenAsADeclarativeSentence() {
        // Chirp3-HD gives "!" an unnatural excited intonation, so it is flattened to "." (still a sentence break).
        String result = GoogleSsml.wrap("Go! Now");

        assertFalse(result.contains("!"), "'!' should be flattened, got: " + result);
        assertTrue(result.contains("Go.<break time=\"300ms\"/>"), "flattened '!' still gets a sentence break, got: " + result);
    }

    @Test
    void commaGetsAShortClauseBreak() {
        assertEquals("<speak>Yes<break time=\"120ms\"/> ready</speak>", GoogleSsml.wrap("Yes, ready"));
    }

    @Test
    void ellipsisGetsOneEllipsisPauseNotThreeSentencePauses() {
        String result = GoogleSsml.wrap("Wait... go");

        assertTrue(result.contains("...<break time=\"300ms\"/>"), "one ellipsis pause, got: " + result);
        int breaks = result.split("<break", -1).length - 1;
        assertEquals(1, breaks, "ellipsis must not cascade into extra breaks, got: " + result);
    }

    @Test
    void decimalPointIsNotTreatedAsSentenceEnd() {
        // "3.5" has no whitespace after the dot, so no break is inserted and the number stays intact.
        assertEquals("<speak>It is 3.5 units</speak>", GoogleSsml.wrap("It is 3.5 units"));
    }

    @Test
    void decimalCommaIsNotTreatedAsClauseEnd() {
        assertEquals("<speak>It is 3,5 units</speak>", GoogleSsml.wrap("It is 3,5 units"));
    }
}
