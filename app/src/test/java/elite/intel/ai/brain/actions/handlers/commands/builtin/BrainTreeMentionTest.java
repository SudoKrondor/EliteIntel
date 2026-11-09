package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A commander who names brain trees must never be sent to a bare planet surface, so the mention has to be
 * heard in every language, and in English whatever language is set: commanders on several locales say the
 * game's English name untranslated.
 */
class BrainTreeMentionTest {

    @Test
    void namingBrainTreesInEachLanguageIsHeard() {
        assertHeard(Language.EN, "find brain trees with tellurium");
        assertHeard(Language.DE, "Gehirnbäume mit Tellur finden");
        assertHeard(Language.FR, "trouve des céphalobores avec du tellure");
        assertHeard(Language.ES, "buscar árboles cerebrales con telurio");
        assertHeard(Language.IT, "trova alberi cerebrali con tellurio");
        assertHeard(Language.PT, "encontrar árvores cerebrais com telúrio");
        assertHeard(Language.PTBZ, "encontrar árvores cerebrais com telúrio");
        assertHeard(Language.RU, "найди мозговые деревья с теллуром");
        assertHeard(Language.UK, "знайди мозкові дерева з телуром");
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void theEnglishNameIsHeardInEveryLanguage(Language language) {
        assertTrue(BrainTreeMention.in("Brain Trees mit Tellur", BrainTreeMention.stems(language)));
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void askingOnlyForTheMaterialIsNotAMention(Language language) {
        assertFalse(BrainTreeMention.in("where can I find tellurium", BrainTreeMention.stems(language)));
    }

    @Test
    void noWordsAreNoMention() {
        assertFalse(BrainTreeMention.in(null, BrainTreeMention.stems(Language.EN)));
        assertFalse(BrainTreeMention.in("  ", BrainTreeMention.stems(Language.EN)));
    }

    private static void assertHeard(Language language, String utterance) {
        assertTrue(BrainTreeMention.in(utterance, BrainTreeMention.stems(language)), language + ": " + utterance);
    }
}
