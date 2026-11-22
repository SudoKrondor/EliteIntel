package elite.intel.ai.mouth.supertonic.stress;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SentenceWordsTest {

    @Test
    void punctuationRunsAreWordsAndSpacesAreNot() {
        assertEquals(List.of("Груз", "-", "сорок", "тонн", "?!"), SentenceWords.of("Груз - сорок тонн?!").words());
    }

    /**
     * RUAccent deletes what it does not know - % / + … here - and the commander must still hear it, so only the
     * models lose it.
     */
    @Test
    void charactersRuAccentDropsStayInTheRebuiltSentence() {
        SentenceWords words = SentenceWords.of("Щиты 50% / +5 к броне…");
        assertEquals(List.of("Щиты", "50", "5", "к", "броне"), words.words());
        assertEquals("Щиты 50% / +5 к броне…", words.rebuild(words.words()));
    }

    /**
     * Expected strings are what RUAccent's own delete_spaces_before_punc returns, odd double space included.
     */
    @Test
    void theHomographContextFollowsRuAccentsSpacing() {
        SentenceWords sentence = SentenceWords.of("Ключ не подходит к замку, замок сломан.");
        assertEquals("Ключ не подходит к замку, <w>замок</w>  сломан.",
                SentenceWords.homographContext(sentence.contextWords(sentence.words()), 6));
    }

    @Test
    void aFreeStandingDashKeepsItsSpacesInTheContextAndAHyphenDoesNot() {
        SentenceWords sentence = SentenceWords.of("Груз - что-то тяжёлое.");
        assertEquals("Груз - что-то <w>тяжёлое</w> .",
                SentenceWords.homographContext(sentence.contextWords(sentence.words()), 5));
    }
}
