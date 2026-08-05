package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.CompanionIdentity;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.vega.model.ThoughtSource;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the source-specific static prompt contracts and resolved language settings.
 */
class CompanionSystemPromptTest {

    /**
     * Budget for the prompt text we author, excluding the interpolated personality clause. It is a bloat tripwire,
     * not a spending limit: exceeding it means an older rule has to pay for the new one, because every rule added
     * dilutes the rest for a small local model. Raise this number only with a reason that is not "the new rule
     * did not fit".
     *
     * <p>It last fired when the identity clause landed (623 words). Two passages paid for it rather than raising
     * the number: "never describe the request in the third person", already covered by <em>Address the commander
     * as "you"</em> and by <em>never echo or restate the input</em>; and "Weigh the commander's words against each
     * function's triggers and description, and commit to the best", which repeats the {@code <language>} block's
     * instruction to match the commander's wording against the offered triggers.
     *
     * <p>It fired before that when the state-gating rule landed and the prompt was at 598 - one word under. Two passages
     * were cut to make room rather than raising it: the biography's career sentences (which licensed "when to
     * challenge risky decisions", working directly against <em>Obey without argument</em> and against the new
     * rule) and the grounding block's gloss on the {@code source} XML attribute, which explained a label the
     * model reads fine unaided. Headroom is deliberate: translated prompts run longer than the English original.
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

    /**
     * Both spoken sources must open by saying what she is: an AI, named, and not hedging about it.
     *
     * <p>WHY: the previous persona line ("the AI serving the commander aboard an Elite Dangerous starship")
     * named the role but never the nature, and the analysis path opened with "You are {shipName}, a ship" - so
     * the same session could answer as a crew member in one turn and as the hull in the next. One clause,
     * {@code CompanionIdentity.identityClause()}, now opens every prompt whose output is spoken.
     */
    @Test
    void bothSpokenPromptsOpenWithTheSameAiIdentity() {
        String identity = CompanionIdentity.identityClause();
        assertTrue(identity.contains("artificial intelligence"), "she must be named as an AI, not merely 'the AI'");
        assertTrue(identity.contains(CompanionIdentity.name()));
        assertTrue(identity.contains("never pretend otherwise"));

        for (ThoughtSource source : ThoughtSource.values()) {
            String normalized = prompt.staticRules(source).replaceAll("\\s+", " ");
            assertTrue(normalized.contains(identity.replaceAll("\\s+", " ")),
                    () -> source + " prompt must carry the shared identity clause verbatim");
        }
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
        assertTrue(normalized.contains("Use only offered functions and their declared parameters"));
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
        assertTrue(normalized.contains("several plausible candidates are not a reason to ask"),
                "several plausible functions must resolve to the best one, not a request to restate");
        assertFalse(normalized.contains("call speak and briefly ask for a restatement"),
                "the ambiguity branch must never route to conversation");
    }

    /**
     * The offered tool set is the authority on what is currently possible: {@link GameToolCandidates} gates every
     * candidate through {@code IntelAction.isVisibleForLLM(status)} against the live game status, so a function the
     * model can see is by construction runnable right now.
     *
     * <p>WHY: asked to "run biome analysis" in supercruise, Mistral was offered {@code query_biome_analysis} (the
     * reducer kept it alone at 0.942) and still answered "we're in supercruise, can't do that yet" - and repeated
     * the refusal when the commander corrected it. Biome analysis is a DB read with no state requirement at all;
     * the precondition came from the model's own Elite lore about surface scanning, applied over a
     * {@code situation} fact saying "In ship - supercruise". Without this rule, every always-available query is one
     * plausible-sounding game rule away from being refused, and the {@code situation} fact makes that guess easy.
     */
    @Test
    void neverRefusesAnOfferedFunctionOnGameStateGrounds() {
        String normalized = prompt.staticRules(ThoughtSource.COMMANDER).replaceAll("\\s+", " ");

        assertTrue(normalized.contains("already filtered by live game state"),
                "the model must be told the offered set is state-gated by the host");
        assertTrue(normalized.contains("every one can run now"));
        assertTrue(normalized.contains("Never refuse or defer one on situational grounds"),
                "an offered function must never be declined over the game situation");
        assertTrue(normalized.contains("an action needing another state is not offered"),
                "the model needs the reason, not just the prohibition");
    }

    /**
     * A value the commander already spoke ("find a market to buy tritium") fills its parameter, and is not a prompt
     * to ask "what type of tritium?". request_input exists for a parameter the commander never filled, never to
     * refine or subcategorize a value already spoken - otherwise required-string commands (find_commodity,
     * find_mining_site) stall on a needless clarification instead of executing.
     */
    @Test
    void requestInputNeverRefinesAnAlreadySpokenValue() {
        String normalized = prompt.staticRules(ThoughtSource.COMMANDER).replaceAll("\\s+", " ");

        assertTrue(normalized.contains("A value the commander already spoke fills its parameter"),
                "a spoken value must be defined as filling its parameter so the model extracts it instead of asking");
        assertTrue(normalized.contains("never request_input to refine or subcategorize it"),
                "request_input must be forbidden from re-clarifying a value already given");
    }

    /**
     * Two failure modes seen on Mistral, both fed by the same thing: recent history is replayed as
     * USER/ASSISTANT pairs, so whatever the companion said last turn is in context as an example of what to
     * say next. With nothing new to add, the model re-emitted its previous reply; and having once answered
     * "Sorry, Commander", it had a template it kept reaching for.
     *
     * <p>The old wording - "Do not repeat answers verbatim unless asked" - was too weak on both counts: it
     * forbade only verbatim repeats (a reworded echo passed) and said nothing about apologising at all.
     */
    @Test
    void forbidsEchoingEarlierRepliesAndOpeningWithApology() {
        String normalized = prompt.staticRules(ThoughtSource.COMMANDER).replaceAll("\\s+", " ");

        assertTrue(normalized.contains("Never reuse an earlier reply's wording"),
                "the rule must cover reworded echoes, not just verbatim ones");
        assertTrue(normalized.contains("if you already said it, say only what is new"),
                "the model needs the alternative behaviour, not just the prohibition");
        assertTrue(normalized.contains("Never apologise or open with regret"));
        assertTrue(normalized.contains("name what is unavailable, then what you can do instead"),
                "an unavailable request still owes the commander a useful sentence");
        assertFalse(normalized.contains("Do not repeat answers verbatim unless asked"),
                "superseded by the stronger rule; keeping both dilutes it");
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

    /**
     * The narration branch must forbid substituting proper nouns, not merely forbid inventing facts.
     *
     * <p>WHY: asked to report a carrier jump whose payload named no destination, the model answered "we're
     * heading to Sol" for a route running to Colonia. The rule against inventing was already present; what
     * was missing was an explicit instruction to reproduce names exactly and to name nothing when the data
     * names nothing. Elite Dangerous system names are long procedural codes, so a small model paraphrasing
     * one into something familiar is a standing risk on every narrated event, not just this one.
     */
    @Test
    void narrationBranchForbidsSubstitutingNamesFromEventData() {
        String normalized = prompt.staticRules(ThoughtSource.EVENT).replaceAll("\\s+", " ");
        assertTrue(normalized.contains("Copy every name, place and number from event_data exactly as written"),
                "narration must pin proper nouns to the payload");
        assertTrue(normalized.contains("never replace one with a shorter, more familiar or more pronounceable name"),
                "narration must forbid swapping a procedural name for a familiar one");
        assertTrue(normalized.contains("If event_data names no destination, do not name one"),
                "narration must forbid filling a gap the payload leaves");
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
