package elite.intel.ai.brain.i18n;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The words the fuzzy reflex takes as heard rather than damaged. Two of its behaviours are worth pinning
 * because a reader would not guess either from the name.
 */
class AliasVocabularyTest {

    @Test
    void itHoldsTheWordsOfTheAuthoredAliases() {
        Set<String> english = AliasVocabulary.forLanguage(Language.EN);

        assertTrue(english.contains("landing"));
        assertTrue(english.contains("docking"));
        assertFalse(english.contains("blanding"), "a word we never wrote must stay repairable");
    }

    /**
     * Parameter annotations are not words the commander says, and a placeholder name leaking in would make
     * that name unrepairable for no reason.
     */
    @Test
    void parameterAnnotationsContributeNoWords() {
        Set<String> english = AliasVocabulary.forLanguage(Language.EN);

        assertFalse(english.contains("key"), "{key:fsd} names a parameter, it is not spoken");
        assertTrue(english.contains("fsd"), "but the spoken part of that alias is");
    }

    /**
     * A language inherits the English base phrases for any key it has not translated, and those are the words
     * its commander is actually offered, so they belong to its vocabulary too.
     */
    @Test
    void aTranslatedLanguageKeepsItsOwnWordsAndTheEnglishItFallsBackTo() {
        Set<String> russian = AliasVocabulary.forLanguage(Language.RU);

        assertTrue(russian.contains("стыковку"));
        assertTrue(russian.stream().anyMatch(word -> word.chars().allMatch(c -> c < 128)),
                "the untranslated keys resolve to English, so English words are part of this vocabulary");
    }

    @Test
    void tokenizationLowerCasesAndDropsPunctuation() {
        assertEquals(List.of("open", "the", "map"), AliasVocabulary.tokenize("Open, the MAP!"));
        assertEquals(List.of(), AliasVocabulary.tokenize("   "));
    }
}
