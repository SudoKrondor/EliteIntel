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
        assertTrue(text.contains("## Persona"));
        assertTrue(text.contains("junior crew member"));
        assertTrue(text.contains("## Tool calling"));
        assertTrue(text.contains("## Safety"));
    }

    @Test
    void carriesGroundingNoFitAndPoliteClosingRules() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        // Grounding: do not invent facts.
        assertTrue(text.contains("never invent or guess facts"));
        // No-fit: use find_action / clarify / decline instead of forcing an unrelated function.
        assertTrue(text.contains("find_action"));
        assertTrue(text.contains("clarify"));
        // Polite closing: do not promise to check and then go silent.
        assertTrue(text.contains("fall silent"));
    }

    @Test
    void commanderBranchAllowsActionsAndExcludesEventRule() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        assertTrue(text.contains("## Turn source"));
        assertTrue(text.contains("started by the commander"));
        assertFalse(text.contains("started by a game event"));
    }

    @Test
    void eventBranchIsReadOnlyAndExcludesCommanderRule() {
        String text = prompt.staticRules(ThoughtSource.EVENT);
        assertTrue(text.contains("## Turn source"));
        assertTrue(text.contains("read-only"));
        assertTrue(text.contains("started by a game event"));
        assertFalse(text.contains("started by the commander"));
    }

    @Test
    void languageRuleNamesResolvedLanguageForInputAndSpokenOutput() {
        String name = resolvedLanguageName();
        String text = prompt.staticRules(ThoughtSource.COMMANDER);

        assertTrue(text.contains("## Language"));
        // The commander's language is named, and spoken output is bound to that same language.
        assertTrue(text.contains("The commander speaks " + name));
        assertTrue(text.contains("Form every spoken phrase (the text you pass to the speak function) in " + name));
    }
}
