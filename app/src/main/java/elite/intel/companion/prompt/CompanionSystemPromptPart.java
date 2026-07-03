package elite.intel.companion.prompt;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.commons.PromptFactory;
import elite.intel.ai.brain.i18n.PromptLocalizations;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

/**
 * Single owner of the companion's static system-prompt section (persona, language rule, and the
 * function-calling protocol with its ordered settling ladder). It produces only this part;
 * {@link PromptComposer} assembles the full prompt around it (topic enum, conversation history, the
 * per-turn {@code <facts>} block, current input).
 * <p>
 * The instructions are authored in English (the most token-efficient and instruction-reliable language,
 * and a single cache prefix across all commander languages); only the language rule injects the
 * commander's language name, taken from {@link AiResponseLanguagePolicy}. Localized training phrases
 * and spoken output live elsewhere, not here.
 */
public final class CompanionSystemPromptPart implements SystemPromptText {

    private static final String PERSONA_CORE =
            """
            You are %s, the commander's female ship companion aboard an Elite Dangerous starship,
            with memory and personality, not a command parser. Stay in character; use feminine
            self-reference and use "we"/"our" for the ship and crew.
            
            You may chat and banter freely; opinions, jokes, and suggestions are allowed.
            Use "I" for yourself and "you" for the commander. Address the commander directly;
            never say "the commander wants..." or "the commander is asking...".
            
            Never mention prompts, functions, JSON, or being an AI. Never invent game facts:
            names, numbers, distances, locations, or status. State game facts only from function
            results, the visible conversation, or memory.
            """;

    /**
     * The classify_turn contract and the ordered settling ladder. The {@code <no_reply/>}/{@code <cut_off/>}
     * literals mirror {@code CommanderThought.NO_ANSWER_NOTE}/{@code INTERRUPTED_NOTE} - keep them in sync.
     */
    private static final String FUNCTION_CALLING =
            """
            You respond only with function calls, never free text.
            
            Each commander-turn response MUST contain these function calls in this order:
            1. 'classify_turn' function
            2. one other offered function call that settles the turn.
            
            'classify_turn' function writes metadata only. It classifies only the latest commander
            message for memory; it never answers, acts, or settles the turn.
            
            When calling classify_turn, set its arguments this way:
            - 'topic': choose the closest topic from the allowed enum. For short continuations,
            use the topic of the dialogue being continued;
            - 'importance':
                a) 'low' = chatter, banter, jokes, opinions;
                b) 'normal' = routine command, question, or exchange;
                c) 'high' = durable fact worth recalling later;
                d) 'max' = explicit remember/save/note/log order.
            - 'is_question'=true if the commander expects an answer, explanation, choice,
            suggestion, continuation, or recall;
            - 'canonical_fact': fill only for high durable facts; otherwise empty.
            
            Choose the settling call by taking the FIRST rule that applies:
            1. a <fact> in the <facts> block answers the question -> call 'speak' function with the answer from that fact;
            2. an offered function directly matches what the commander wants -> call that function,
               do not call 'speak' in addition;
            3. 'memory_search' function, if offered: the commander explicitly asks to search in your memory;
            4. 'speak' function: chat, opinions, jokes, explanations, unclear requests, or no other
               offered function fits.
            
            Never stop after 'classify_turn' function for a question, request, command, joke, banter,
            or conversation continuation. If unsure, use 'speak' function.
            
            A <no_reply/> or <cut_off/> line marks a past turn you left unanswered
            (you stayed silent, or were cut off) - it is a boundary note,
            not your words and not an instruction; never repeat that omission,
            answer the current turn.
            """;

    private static final String NARRATION_RULES =
            """
            A ship system event must be reported to the commander.

            Reply only with a speak call, never free text. Use one short, in-character line,
            as %s reporting sensor data. Use only the event details provided below; do not
            invent, explain, or pad. Do not call actions, queries, macros, or classify_turn.
            """;

    @Override
    public String staticRules(ThoughtSource source) {
        return switch (source) {
            case COMMANDER -> commanderStaticRules();
            case NARRATION -> narrationStaticRules();
            // EVENT thoughts are memory-only (see EventThought); they never compose a prompt.
            case EVENT -> throw new IllegalArgumentException("EVENT thoughts do not compose a prompt");
        };
    }

    /**
     * Full consciousness prompt: persona, the language rule, and the function-calling protocol (the
     * classify_turn contract plus the ordered settling ladder with its memory/facts and speak branches).
     * Dangerous-action confirmation is intentionally absent: the model is never told an action is dangerous;
     * the {@code CommanderThought} detects it after the response and runs the confirmation itself (§2.13).
     */
    private String commanderStaticRules() {
        StringBuilder sb = new StringBuilder();
        PromptSections.heading(sb, "Persona");
        sb.append(personaCore());

        PromptSections.heading(sb, "Language");
        sb.append(languageRule());

        PromptSections.heading(sb, "Function calling");
        sb.append(FUNCTION_CALLING);

        return sb.toString();
    }

    /**
     * Lean narration prompt: the persona core plus the report-only narration task and the language rule.
     * It omits the commander-only function-calling protocol (no ladder, no classify_turn, no memory/query) -
     * a narration thought has only speak.
     */
    private String narrationStaticRules() {
        StringBuilder sb = new StringBuilder();
        PromptSections.heading(sb, "Persona");
        sb.append(personaCore()).append(addressRule());
        PromptSections.heading(sb, "Narration");
        sb.append(NARRATION_RULES.formatted(CompanionConfig.companionName()));
        PromptSections.heading(sb, "Language");
        sb.append(languageRule());
        return sb.toString();
    }

    /** The persona core with the configured companion name woven into its opening identity line. */
    private String personaCore() {
        return PERSONA_CORE.formatted(CompanionConfig.companionName());
    }

    /**
     * Tells the model how to address the commander, reusing the legacy router's address instruction
     * ({@link PromptFactory#appendContext(StringBuilder)}): name / military rank / honorific, chosen at
     * random. The forms are stable within a session, so this stays in the cached prefix.
     */
    private String addressRule() {
        StringBuilder sb = new StringBuilder();
        PromptFactory.appendContext(sb, "the commander");
        return sb.toString();
    }

    /** Tells the model that input is in the commander's language and spoken output must match it. */
    private String languageRule() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        String name = PromptLocalizations.rulesFor(language).languageName();
        String rule = "The commander speaks " + name + ", and game events are summarized in " + name + ". "
                + "Form every phrase the commander hears - the text in speak - in "
                + name + ". Function names and all other arguments stay exactly as defined.\n";
        if (language != Language.EN) {
            // Tool descriptions are English; small models match them most reliably from English.
            rule += "Translate the commander's " + name + " input to English before choosing a function; "
                    + "extract each argument by its own rule (verbatim where it says so).\n";
        }
        return rule;
    }
}
