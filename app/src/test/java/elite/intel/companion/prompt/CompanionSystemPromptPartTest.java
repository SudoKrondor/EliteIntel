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
    void alwaysCarriesPersonaAndFunctionCalling() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        assertTrue(text.contains("<persona>"));
        assertTrue(text.contains("the commander's female ship companion"));
        assertTrue(text.contains("<function_calling>"));
        // Danger is detected and voiced by the thought after the response, never prompted: no safety section.
        assertFalse(text.contains("<safety>"));
    }

    @Test
    void carriesGroundingAndSettlingRules() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        // Grounding: do not invent facts.
        assertTrue(text.contains("Never invent game facts"));
        // Every turn must settle - the metadata-only classify_turn never ends the turn.
        assertTrue(text.contains("it never ends the turn"));
        // The no-reply / cut-off boundary markers are explained so the model does not repeat the omission.
        assertTrue(text.contains("<no_reply/> or <cut_off/>"));
    }

    @Test
    void addressesTheCommanderDirectlyInSecondPerson() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        // Address the commander directly as "you", never narrate them in the third person.
        assertTrue(text.contains("Use \"I\" for yourself and \"you\" for the commander"));
        assertTrue(text.contains("never say \"the commander wants...\""));
    }

    @Test
    void commanderBranchCarriesFunctionCallingAndExcludesNarration() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        assertTrue(text.contains("<function_calling>"));
        assertTrue(text.contains("that settles the turn"));
        // The commander branch is not the narration report-only task.
        assertFalse(text.contains("<narration>"));
    }

    @Test
    void narrationBranchIsReportOnlyAndExcludesCommanderSections() {
        String text = prompt.staticRules(ThoughtSource.NARRATION);
        assertTrue(text.contains("<persona>"));
        assertTrue(text.contains("<narration>"));
        assertTrue(text.contains("must be reported to the commander"));
        // The lean narration prompt drops the commander-only function-calling section and memory/query guidance.
        assertFalse(text.contains("<function_calling>"));
        assertFalse(text.contains("<safety>"));
        assertFalse(text.contains("memory_search"));
        assertTrue(text.contains("<language>"));
    }

    @Test
    void languageRuleNamesResolvedLanguageForInputAndSpokenOutput() {
        String name = resolvedLanguageName();
        String text = prompt.staticRules(ThoughtSource.COMMANDER);

        assertTrue(text.contains("<language>"));
        // The commander's language is named, and spoken output is bound to that same language.
        assertTrue(text.contains("The commander speaks " + name));
        // The spoken surface (speak) is bound to the commander's language.
        assertTrue(text.contains("the text in speak - in " + name));
    }
}
