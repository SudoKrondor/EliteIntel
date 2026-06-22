package elite.intel.companion.prompt;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.PromptLocalizations;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the real static narrative owner that {@link PromptComposerTest} stubs out: the source-aware
 * COMMANDER/EVENT branch and the language rule that names the commander's resolved language. The
 * expected language name is computed the same way production does, so the test holds in any environment.
 */
class CompanionSystemPromptPartTest {

    private final CompanionSystemPromptPart prompt = new CompanionSystemPromptPart();

    private static String resolvedLanguageName() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        return PromptLocalizations.rulesFor(language).languageName();
    }

    @Test
    void alwaysCarriesPersonaToolCallingAndSafety() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        assertTrue(text.contains("junior crew member"));
        assertTrue(text.contains("TOOL-CALLING ONLY"));
        assertTrue(text.contains("SAFETY"));
    }

    @Test
    void commanderBranchAllowsActionsAndExcludesEventRule() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        assertTrue(text.contains("TURN SOURCE - COMMANDER"));
        assertFalse(text.contains("TURN SOURCE - EVENT"));
    }

    @Test
    void eventBranchIsReadOnlyAndExcludesCommanderRule() {
        String text = prompt.staticRules(ThoughtSource.EVENT);
        assertTrue(text.contains("TURN SOURCE - EVENT"));
        assertTrue(text.contains("read-only"));
        assertFalse(text.contains("TURN SOURCE - COMMANDER"));
    }

    @Test
    void languageRuleNamesResolvedLanguageForInputAndSpokenOutput() {
        String name = resolvedLanguageName();
        String text = prompt.staticRules(ThoughtSource.COMMANDER);

        assertTrue(text.contains("LANGUAGE:"));
        // The commander's language is named, and spoken output is bound to that same language.
        assertTrue(text.contains("The commander speaks " + name));
        assertTrue(text.contains("Form every spoken phrase (the text you pass to the speak function) in " + name));
    }
}
