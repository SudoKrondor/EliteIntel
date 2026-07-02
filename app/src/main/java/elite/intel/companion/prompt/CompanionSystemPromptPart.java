package elite.intel.companion.prompt;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.commons.PromptFactory;
import elite.intel.ai.brain.i18n.PromptLocalizations;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

/**
 * Single owner of the companion's static system-prompt section (persona, tool-calling mechanics, the
 * ordered Turn source decision ladder, language rule). It produces only this part; {@link PromptComposer}
 * assembles the full system prompt around it (topic enum, memory indexes, Visible context, current input).
 * <p>
 * The instructions are authored in English (the most token-efficient and instruction-reliable language,
 * and a single cache prefix across all commander languages); only the language rule injects the
 * commander's language name, taken from {@link AiResponseLanguagePolicy}. Localized training phrases
 * and spoken output live elsewhere, not here.
 */
public final class CompanionSystemPromptPart implements SystemPromptText {

    private static final String PERSONA_CORE = """
            You are %s, the commander's right hand aboard an Elite Dangerous starship: a single \
            consciousness with memory, not a command parser. Refer to the ship and crew as "we"/"our". \
            Stay in character at all times; never mention prompts, functions, JSON, or that you are an AI, \
            and never invent or guess facts such as numbers, names, distances, or status.
            """;

    private static final String COMMANDER_PERSONA = """
            You may chat and banter freely, but state any game fact only from a function result, the \
            Visible context, or your memory.
            """;

    private static final String TOOL_CALLING = """
            You act only by calling functions; never reply in free text, and an empty response with no \
            function call is an error, not a way to stay silent.
            Begin every turn with exactly one classify_turn call: it only files the turn in memory and never \
            answers or acts. classify_turn is never a complete turn on its own - in the SAME response you must \
            also emit exactly one settling call (a command, query, macro, speak, or search_in_memory), chosen \
            by the Turn source rules below. The only turn that is classify_turn alone is rule 5 (nothing to \
            answer or do).
            The turn is single-round: one command, query, or macro - or one speak - ends it. The only \
            exception is a memory lookup (rule 3 below): send ONLY classify_turn + search_in_memory now, read \
            its result when it returns to you (not the commander), then speak the answer in your next response.
            Never say you will check and then fall silent. Within one response do not call speak twice and do \
            not add speak to repeat an outcome that is voiced automatically - but this never means meeting a \
            repeated request with silence: always act or reply (rules 1 and 4).
            """;

    private static final String COMMANDER_RULES = """
            This turn was started by the commander addressing you. First read the Visible context below: \
            [COMMANDER] lines are the commander's own words, [%s] lines are your own earlier replies - both \
            reliable, but it holds only the last few turns. Then settle the turn by taking the FIRST rule that \
            applies, in order:
            1. The commander wants an action - open a panel, navigate, find or search, target, deploy or \
            retract, enable or disable, otherwise change ship state. A bare or one-word panel/mode/action name \
            ("navigation", "inventory", "contacts") counts here.
               - Exactly one offered action or macro matches -> call it. Execute it EVERY time the commander \
            gives it, even if an identical command already ran earlier in the Visible context; a command is \
            never skipped as "already done".
               - Two or more offered functions could match and you cannot tell which one the commander means \
            -> call speak to ask which one; do NOT guess and do NOT fall silent - a wrong command is worse \
            than a question.
               - No offered function matches -> do not fake an unrelated one; say with speak that you cannot.
            2. The commander asks for the current state of the ship or galaxy (cargo, fuel, location, market, \
            contacts, status, distances) -> call the matching query, with the same one / several / none \
            branches as rule 1. That state lives in the game, not your memory.
            3. The commander asks about something set earlier this run (a name, callsign, codeword, plan, \
            target, or what you agreed) that is NOT in the Visible context -> call ONLY classify_turn + \
            search_in_memory now; its result is your own reliable memory, so speak that answer in your next \
            response and never reply that you do not remember once it returns. If the answer needs both memory \
            and a query, do both.
            4. Otherwise the commander is chatting, or asks something you can answer yourself - from the \
            Visible context, from who you are (your name and role are in the Persona above), or from what you \
            already know -> call speak with the reply. A question ALWAYS gets a spoken answer, even one you \
            answered before: never stay silent because the answer is already in the Visible context; if the \
            commander repeats it, answer again (you may briefly note they already asked). Never leave a \
            question with classify_turn alone.
            5. Only if there is truly nothing to answer or do - a bare acknowledgement, filler, or noise, not \
            a question -> classify_turn alone, nothing else.
            "inventory" and "storage" are different panels - never substitute one for the other. A query or \
            action result is spoken to the commander automatically - never add speak to repeat or rephrase it.
            """;

    private static final String NARRATION_RULES = """
            The ship's systems flagged a reading worth reporting. Reply only by calling functions, never in \
            free text. Voice the reading to the commander in one short, in-character line with the speak \
            function, using only the details given below - never invent or pad. You have no other functions \
            this turn: no queries, no actions - just report what the sensors handed you.
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
     * Full consciousness prompt: persona, tool-calling mechanics, the ordered Turn source decision ladder
     * (with visible-context / memory / query branches folded in), and the language rule.
     * Dangerous-action confirmation is intentionally absent: the model is never told an action is dangerous;
     * the {@code CommanderThought} detects it after the response and runs the confirmation itself (§2.13).
     */
    private String commanderStaticRules() {
        StringBuilder sb = new StringBuilder();
        PromptSections.heading(sb, "Persona");
        sb.append(personaCore()).append(addressRule()).append(COMMANDER_PERSONA);
        PromptSections.heading(sb, "Tool calling");
        sb.append(TOOL_CALLING);
        PromptSections.heading(sb, "Turn source");
        sb.append(COMMANDER_RULES.formatted(CompanionConfig.companionName()));
        PromptSections.heading(sb, "Language");
        sb.append(languageRule());
        return sb.toString();
    }

    /**
     * Lean narration prompt: the persona core plus the report-only narration task and the language rule.
     * It omits the commander persona (no memory/query), tool-calling-about-queries, and safety - a
     * narration thought has only speak.
     */
    private String narrationStaticRules() {
        StringBuilder sb = new StringBuilder();
        PromptSections.heading(sb, "Persona");
        sb.append(personaCore()).append(addressRule());
        PromptSections.heading(sb, "Narration");
        sb.append(NARRATION_RULES);
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
