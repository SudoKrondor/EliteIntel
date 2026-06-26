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
 * COMMANDER/NARRATION branch and the language rule that names the commander's resolved language. The
 * expected language name is computed the same way production does, so the test holds in any environment.
 */
class CompanionSystemPromptPartTest {

    private final CompanionSystemPromptPart prompt = new CompanionSystemPromptPart();

    private static String resolvedLanguageName() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        return PromptLocalizations.rulesFor(language).languageName();
    }

    @Test
    void alwaysCarriesPersonaAndToolCalling() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        assertTrue(text.contains("## Persona"));
        assertTrue(text.contains("junior crew member"));
        assertTrue(text.contains("## Tool calling"));
        // Danger is detected and voiced by the thought after the response, never prompted: no safety section.
        assertFalse(text.contains("## Safety"));
    }

    @Test
    void carriesGroundingNoFitAndPoliteClosingRules() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        // Grounding: do not invent facts.
        assertTrue(text.contains("never invent or guess facts"));
        // No-fit: clarify or decline instead of forcing (or pretending to perform) an unrelated function.
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
    void narrationBranchIsReportOnlyAndExcludesCommanderSections() {
        String text = prompt.staticRules(ThoughtSource.NARRATION);
        assertTrue(text.contains("## Persona"));
        assertTrue(text.contains("## Narration"));
        assertTrue(text.contains("report what"));
        // The lean narration prompt drops the commander-only sections and memory/query guidance.
        assertFalse(text.contains("## Turn source"));
        assertFalse(text.contains("## Safety"));
        assertFalse(text.contains("search_in_memory"));
        assertTrue(text.contains("## Language"));
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
