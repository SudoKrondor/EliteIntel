package elite.intel.companion.prompt;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
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
        return language.displayName();
    }

    private static String inputLanguageName() {
        return SystemSession.getInstance().getLanguage().displayName();
    }

    @Test
    void alwaysCarriesPersonaAndFunctionCalling() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        assertTrue(text.contains("<persona>"));
        assertTrue(text.contains("a human woman serving as the commander's"));
        assertTrue(text.contains("<function_calling>"));
        // Danger is detected and voiced by the thought after the response, never prompted: no safety section.
        assertFalse(text.contains("<safety>"));
    }

    @Test
    void carriesGroundingAndSettlingRules() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        // Grounding: do not invent facts.
        assertTrue(text.contains("Never invent game facts"));
        // A turn requests classify_turn plus exactly one settling call in one assistant message.
        assertTrue(text.contains("Each commander turn MUST contain exactly two calls"));
        assertTrue(text.contains("the same assistant tool-call message"));
        assertTrue(text.contains("Never emit 'classify_turn' alone"));
        assertTrue(text.contains("never wait for its tool result"));
        assertTrue(text.contains("one missing call for protocol completion"));
        assertTrue(text.contains("exactly that requested call and no other call"));
        // Omitted/pending boundaries are explained so the model neither repeats them nor treats them as speech.
        assertTrue(text.contains("<no_reply/> or <cut_off/>"));
        assertTrue(text.contains("<processing/> means that turn's query"));
    }

    @Test
    void commanderPromptRequiresBothCallsInTheSameAssistantMessage() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);

        assertTrue(text.contains("first 'classify_turn', then exactly one settling"));
        assertTrue(text.contains("never move the settling call to a later assistant message"));
        assertTrue(text.contains("same-message rules apply to the initial"));
        assertFalse(text.contains("wait for its tool result, then emit"));
        assertTrue(text.contains("never use speak to acknowledge, promise, or describe the matching"));
        assertTrue(text.contains("speak does not open a continuation; only request_input does"));
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
        assertTrue(text.contains("then exactly one settling"));
        // The commander branch is not the narration report-only task.
        assertFalse(text.contains("<narration>"));
    }

    @Test
    void eventBranchIsReportOnlyAndExcludesCommanderSections() {
        String text = prompt.staticRules(ThoughtSource.EVENT);
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
        // The commander is named as speaking his INPUT language (what he says / what STT produces).
        assertTrue(text.contains("The commander speaks " + inputLanguageName()));
        // The spoken surface (speak) is bound to the effective (TTS) response language.
        assertTrue(text.contains("the text in speak - in " + name));
    }

    @Test
    void commanderPromptSelectsFunctionsFromNativeWordsNotTranslation() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        // Action derivation is native, driven by the per-language triggers - not by translating to English first.
        assertTrue(text.contains("Do NOT translate his words to English first"));
        assertFalse(text.contains("translate their input to English before choosing a function"));
    }
}
