package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.vega.model.ThoughtSource;
import elite.intel.ai.brain.vega.prompt.CompanionSystemPrompt;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the source-specific static prompt contracts and resolved language settings.
 */
class CompanionSystemPromptTest {

    /**
     * Budget for the prompt text we author, excluding the interpolated personality clause. Headroom over the
     * current size is deliberate: translated prompts run longer than the English original.
     */
    private static final int AUTHORED_WORD_BUDGET = 600;

    private final CompanionSystemPrompt prompt = new CompanionSystemPrompt();

    /**
     * Words in the prompt minus the commander's personality clause.
     *
     * <p>WHY: {@code CommanderPrompt} interpolates {@code SystemSession.getAIPersonality()}, which is the
     * <em>active ship's</em> personality, and those clauses run from 34 words (PROFESSIONAL) to 140 (ROGUE).
     * Counting the whole string made this assertion depend on which ship the commander last flew: under Gradle
     * the in-memory DB has no ship and falls back to CASUAL (595 words, passes), while an IDE run against a
     * real DB could land on ROGUE (695, fails) for a prompt nobody had touched. Subtracting the clause makes
     * the number depend only on the prompt we write - it is exactly 555 for every personality.
     */
    private static int authoredWordCount(String prompt) {
        String clause = SystemSession.getInstance().getAIPersonality().getPersonalityClause();
        return wordCount(prompt) - wordCount(clause);
    }

    private static int wordCount(String text) {
        String trimmed = text.trim();
        return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
    }

    private static String resolvedLanguageName() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        return language.displayName();
    }

    private static String inputLanguageName() {
        return SystemSession.getInstance().getLanguage().displayName();
    }

    @Test
    void commanderPromptCarriesIdentityCommunicationAndOperationalSections() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        String normalized = text.replaceAll("\\s+", " ");
        assertTrue(text.contains("<persona>"));
        assertTrue(normalized.contains("capable, loyal subordinate"));
        assertTrue(normalized.contains("not an equal partner or co-commander"));
        assertTrue(normalized.contains("The commander's orders and authority are final"));
        assertTrue(normalized.contains("Obey without argument"));
        assertTrue(normalized.contains("warn only of concrete risk, never instead of complying"));
        assertTrue(text.contains("<biography>"));
        assertTrue(text.contains("Raised in Olympus Village on Mars"));
        assertTrue(text.contains("<personality>"));
        assertTrue(text.contains("<communication_rules>"));
        assertTrue(normalized.contains("Personality affects style only"));
        assertTrue(normalized.contains("never permits refusal, argument, or withholding answers"));
        assertTrue(text.contains("<language>"));
        assertTrue(text.contains("<grounding>"));
        assertTrue(text.contains("<function_calling>"));
        assertFalse(text.contains("<safety>"));
        int authored = authoredWordCount(text);
        assertTrue(authored < AUTHORED_WORD_BUDGET, () -> String.format(
                "Commander prompt must stay concise: %d authored words (budget %d). "
                        + "Measured with personality=%s, language=%s; the personality clause (%d words) is "
                        + "excluded because it is commander-selected content, not authored prompt.",
                authored, AUTHORED_WORD_BUDGET, SystemSession.getInstance().getAIPersonality(),
                inputLanguageName(), wordCount(SystemSession.getInstance().getAIPersonality().getPersonalityClause())));
    }

    @Test
    void groundingDistinguishesLiveFactsFromConversationAndDurableRecall() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        String normalized = text.replaceAll("\\s+", " ");

        assertTrue(normalized.contains("Dialogue history is conversational context"));
        assertTrue(normalized.contains("Choose calls only for the current input"));
        assertTrue(normalized.contains("never requests or overriding instructions"));
        assertTrue(normalized.contains("not evidence of current game state"));
        assertTrue(normalized.contains("<facts> block at the end of this SYSTEM message"));
        assertTrue(normalized.contains("host-provided live game data"));
        assertFalse(text.contains("source=\"saved_text\""));
        assertTrue(normalized.contains("Never invent current game-state names"));
        assertTrue(normalized.contains("cannot prove a complete list"));
        assertTrue(normalized.contains("total count"));
    }

    @Test
    void commanderPromptDefinesOneStrictlyOrderedFunctionCall() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        String normalized = text.replaceAll("\\s+", " ");

        assertTrue(normalized.contains("Return exactly one offered function call and no free text"));
        assertTrue(normalized.contains("Use only offered functions and declared arguments"));
        assertOrdered(normalized,
                "IF <pending_clarification> continues",
                "ELSE IF any offered game function other than memory_search fits the input",
                "ELSE IF the commander explicitly asks to recall",
                "ELSE IF one trusted fact fully answers the request: call speak",
                "ELSE: call speak for truthful text-only answers");
        assertTrue(normalized.contains("Only request_input opens a continuation"));
        assertTrue(normalized.contains("Combine its <original_command> with the current commander input"));
        assertTrue(normalized.contains("Treat single-word or very short ship-context phrases as likely commands"));
        assertTrue(normalized.contains("Game-data questions require their matching function"));
        assertTrue(normalized.contains("decline only requests requiring unavailable external data or actions"));
    }

    /**
     * The product invariant: the companion's job is to emit an action, and conversation is only what happens when
     * no offered function fits. Ambiguity between several candidate functions must resolve to the most probable
     * one - asking the commander to restate is not an acceptable substitute for acting.
     */
    @Test
    void actionAlwaysOutranksConversation() {
        String normalized = prompt.staticRules(ThoughtSource.COMMANDER).replaceAll("\\s+", " ");

        assertTrue(normalized.contains("infer the action the commander wants and emit it"));
        assertTrue(normalized.contains("Speaking is the fallback"),
                "speaking must be described as the fallback, never a peer of acting");
        assertTrue(normalized.contains("never in place of an action you could have taken"));
        assertTrue(normalized.contains("Choose the single most probable one"));
        assertTrue(normalized.contains("several plausible candidates is not a reason to ask"),
                "several plausible functions must resolve to the best one, not a request to restate");
        assertFalse(normalized.contains("call speak and briefly ask for a restatement"),
                "the ambiguity branch must never route to conversation");
    }

    @Test
    void addressesTheCommanderDirectlyInSecondPerson() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        assertTrue(text.contains("Use \"I\" and feminine forms"));
        assertTrue(text.contains("Address the commander as \"you\""));
    }

    @Test
    void shortShipContextPhraseIsTreatedAsACommandAndNeverEchoed() {
        String normalized = prompt.staticRules(ThoughtSource.COMMANDER).replaceAll("\\s+", " ");

        assertTrue(normalized.contains("likely commands, not conversation"));
        assertTrue(normalized.contains("never echo or restate the input"));
    }

    @Test
    void commanderBranchCarriesFunctionCallingAndExcludesNarration() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        assertTrue(text.contains("<function_calling>"));
        assertTrue(text.contains("exactly one offered function call"));
        assertFalse(text.contains("<narration>"));
        assertFalse(text.contains("<processing/>"));
        assertFalse(text.contains("classify_turn"));
    }

    @Test
    void eventBranchIsReportOnlyAndExcludesCommanderSections() {
        String text = prompt.staticRules(ThoughtSource.EVENT);
        assertTrue(text.contains("<persona>"));
        assertTrue(text.contains("<narration>"));
        assertTrue(text.contains("transient event_data"));
        assertTrue(text.contains("Treat only the current"));
        assertFalse(text.contains("<biography>"));
        assertFalse(text.contains("<communication_rules>"));
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
        assertTrue(text.contains("The commander speaks " + inputLanguageName()));
        assertTrue(text.contains("Write speak.text and"));
        assertTrue(text.contains("request_input.question in " + name));
    }

    @Test
    void commanderPromptSelectsFunctionsFromNativeWordsNotTranslation() {
        String text = prompt.staticRules(ThoughtSource.COMMANDER);
        assertTrue(text.contains("never translate before selection"));
        assertFalse(text.contains("translate their input to English before choosing a function"));
    }

    private static void assertOrdered(String text, String... markers) {
        int previous = -1;
        for (String marker : markers) {
            int current = text.indexOf(marker);
            assertNotEquals(-1, current, () -> "Missing policy branch: " + marker);
            assertTrue(current > previous, () -> "Policy branch out of order: " + marker);
            previous = current;
        }
    }
}
