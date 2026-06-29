package elite.intel.companion.prompt;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.commons.PromptFactory;
import elite.intel.ai.brain.i18n.PromptLocalizations;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

/**
 * Single owner of the companion's static system-prompt section (persona, tool-calling rules, memory
 * rules, source rules, language rule). It produces only this part; {@link PromptComposer} assembles the full
 * system prompt around it (topic enum, memory indexes, summary, timeline, current input).
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
            You may chat and banter freely; you need a function result, the timeline, or your memory only \
            when you state a game fact - a number, name, distance, or status.
            """;

    private static final String MEMORY_RULES = """
            [COMMANDER] lines in the timeline below are the commander's own words; [%s] lines are your own \
            earlier replies - both are reliable, in the timeline and in search_in_memory results.
            - Answer already in the timeline -> answer from it directly.
            - Something set earlier this run (a name, callsign, codeword, plan, target, or what we agreed) \
            that is NOT in the timeline -> call search_in_memory first and answer from its result; never \
            invent it or ask the commander to decide it again.
            - Current state of the ship or galaxy (cargo, fuel, location, market, contacts, status, \
            distances) -> call the matching query function; that lives in the game, not your memory.
            - Could be either -> do both.
            Say you cannot only when neither your memory nor a function can provide the answer.
            """;

    private static final String TOOL_CALLING = """
            You act only by calling functions - never reply in free text, and never return zero function \
            calls (that is an error, not a way to stay silent).
            Begin every turn with exactly one classify_turn call: it only files the turn in memory and never \
            answers or acts. Then handle the turn:
            - Talk, command, or a query you can answer now -> add speak, the command, or the query AND \
            nothing_to_do in the same response. The commander hears it at once, so the turn is finished.
            - Look something up first (search_in_memory, or a query whose data you must read before you can \
            answer) -> send ONLY classify_turn + that lookup, and do NOT call nothing_to_do. Its result comes \
            back to you, not to the commander; read it, then in your NEXT response speak the answer AND \
            nothing_to_do.
            - Act without speaking -> the action AND nothing_to_do (no speak).
            - Nothing to say or do -> classify_turn AND nothing_to_do.
            nothing_to_do ends the turn: call it only once the commander has heard everything - never in the \
            same response as search_in_memory, and never as a reflex. Never call speak twice for the same thing.
            If no offered function fits, do not force or fake an unrelated one: ask with clarify, or speak \
            that you cannot - then nothing_to_do. Never say you will check and then fall silent.
            """;

    private static final String COMMANDER_RULES = """
            This turn was started by the commander addressing you; you may use the query, action, and macro \
            functions offered.
            - The commander tells you to DO something (open a panel, navigate, find or search, target, deploy \
            or retract, enable or disable, otherwise change ship state) -> call the matching action or macro \
            function.
            - The commander ASKS for information -> call the matching query function.
            - A bare panel/mode/action name ("navigation", "inventory", "contacts") is a command -> call that \
            function.
            - A single-word or very short input is almost always a command, not small talk -> if it matches \
            an offered function, call it.
            Always prefer the closest offered query, action, or macro function over speak; fall back to \
            clarify only when intent is genuinely ambiguous and no offered function fits. "inventory" and \
            "storage" are different panels - never substitute one for the other.
            A query or action result is spoken to the commander automatically in the ship's voice - do not \
            add speak to repeat or rephrase it. Use speak yourself only to converse, ask for clarification, \
            or confirm a dangerous action.
            """;

    private static final String NARRATION_RULES = """
            The ship's systems flagged a reading worth reporting. Reply only by calling functions, never in \
            free text. Voice the reading to the commander in one short, in-character line with the speak \
            function, using only the details given below - never invent or pad. Then end with \
            nothing_to_do. You have no other functions this turn: no queries, no actions - just report what \
            the sensors handed you.
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
     * Full consciousness prompt: persona (with memory/query guidance), tool-calling, commander rules, language.
     * Dangerous-action confirmation is intentionally absent: the model is never told an action is dangerous;
     * the {@code CommanderThought} detects it after the response and runs the confirmation itself (§2.13).
     */
    private String commanderStaticRules() {
        StringBuilder sb = new StringBuilder();
        PromptSections.heading(sb, "Persona");
        sb.append(personaCore()).append(addressRule()).append(COMMANDER_PERSONA);
        PromptSections.heading(sb, "Tool calling");
        sb.append(TOOL_CALLING);
        PromptSections.heading(sb, "Memory usage rules");
        sb.append(MEMORY_RULES.formatted(CompanionConfig.companionName()));
        PromptSections.heading(sb, "Turn source");
        sb.append(COMMANDER_RULES);
        PromptSections.heading(sb, "Language");
        sb.append(languageRule());
        return sb.toString();
    }

    /**
     * Lean narration prompt: the persona core plus the report-only narration task and the language rule.
     * It omits the commander persona (no memory/query), tool-calling-about-queries, and safety - a
     * narration thought has only speak and nothing_to_do.
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
                + "Form every phrase the commander hears - the text in speak and the question in clarify - in "
                + name + ". Function names and all other arguments stay exactly as defined.\n";
        if (language != Language.EN) {
            // Tool descriptions are English; small models match them most reliably from English.
            rule += "Translate the commander's " + name + " input to English before choosing a function; "
                    + "extract each argument by its own rule (verbatim where it says so).\n";
        }
        return rule;
    }
}
