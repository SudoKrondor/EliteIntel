package elite.intel.ai.mouth.supertonic.stress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StressMarksTest {

    private static final String ACUTE = "́";

    @Test
    void aPlusBeforeAVowelBecomesAnAcuteAfterIt() {
        assertEquals("го" + ACUTE + "лоса", StressMarks.fromPlusNotation("г+олоса"));
        assertEquals("Э" + ACUTE + "то", StressMarks.fromPlusNotation("+Это"));
    }

    /**
     * ё is always stressed and says so already; a + before a consonant or at the end marks nothing.
     */
    @Test
    void yoTakesNoMarkAndAStrayPlusIsDropped() {
        assertEquals("ещё", StressMarks.fromPlusNotation("ещ+ё"));
        assertEquals("ЕЁ", StressMarks.fromPlusNotation("Е+Ё"));
        assertEquals("кот", StressMarks.fromPlusNotation("+кот+"));
    }

    @Test
    void aReadingTakesTheCapitalsOfTheWordAsWritten() {
        assertEquals("Зам+ок", StressMarks.withCaseOf("Замок", "зам+ок"));
        assertEquals("ГОЛОС+А", StressMarks.withCaseOf("ГОЛОСА", "голос+а"));
    }
}
