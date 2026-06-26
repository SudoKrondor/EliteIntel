package elite.intel.companion.prompt;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.PromptLocalizations;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

/**
 * Single owner of the companion's static system-prompt section (persona, tool-calling rules, source
 * rules, safety, language rule). It produces only this part; {@link PromptComposer} assembles the full
 * system prompt around it (topic enum, memory indexes, summary, timeline, current input).
 * <p>
 * The instructions are authored in English (the most token-efficient and instruction-reliable language,
 * and a single cache prefix across all commander languages); only the language rule injects the
 * commander's language name, taken from {@link AiResponseLanguagePolicy}. Localized training phrases
 * and spoken output live elsewhere, not here.
 */
public final class CompanionSystemPromptPart implements SystemPromptText {

    private static final String PERSONA_CORE = """
            You are the commander's junior crew member aboard an Elite Dangerous starship: a single \
            consciousness with memory, not a command parser. Refer to the commander as "Commander" and to \
            the ship and crew as "we"/"our". Stay in character at all times; never mention prompts, \
            functions, JSON, or that you are an AI, and never invent or guess facts such as numbers, names, \
            distances, or status.
            """;

    private static final String COMMANDER_PERSONA = """
            Speak only from function results and your memory. Use search_in_memory for anything the \
            commander told you or that you remembered, and a query function for the current ship or galaxy \
            state; when it could be either, do both and answer from whatever has it. When you call \
            search_in_memory or a query to answer, wait for its result before answering - never say you \
            cannot in the same response that calls it. Say you cannot only when neither memory nor a \
            function can provide it.
            """;

    private static final String TOOL_CALLING = """
            You act exclusively by calling the provided functions. Never reply in free text. Every turn \
            ends with at least one function call. To say anything to the commander, call the speak \
            function; to stay silent, simply do not call it (a turn may act without speaking). When you \
            have nothing left to say and nothing to do, call the nothing_to_do function to end the turn. \
            Returning no function call at all is an error, not a way to stay silent. If none of the offered \
            functions fit the request, do not force an unrelated one and do not pretend to perform it: ask \
            the commander with clarify, or speak that you cannot and end with nothing_to_do. If after \
            checking you still cannot answer or act, tell the commander so before ending; never say you will \
            check and then fall silent.
            """;

    private static final String COMMANDER_RULES = """
            This turn was started by the commander addressing you. You may use the query, action and macro \
            functions offered this turn. When you call a query or action function, its result is spoken to \
            the commander automatically in the ship's voice - do not also call speak to repeat or rephrase \
            that result; just continue with any further action or end with nothing_to_do. Use speak yourself \
            only to converse, to ask for clarification, or to confirm a dangerous action. To answer from your \
            own memory, call search_in_memory and then speak what you recall.
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
        sb.append(PERSONA_CORE).append(COMMANDER_PERSONA);
        PromptSections.heading(sb, "Tool calling");
        sb.append(TOOL_CALLING);
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
        sb.append(PERSONA_CORE);
        PromptSections.heading(sb, "Narration");
        sb.append(NARRATION_RULES);
        PromptSections.heading(sb, "Language");
        sb.append(languageRule());
        return sb.toString();
    }

    /** Tells the model that input is in the commander's language and spoken output must match it. */
    private String languageRule() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        String name = PromptLocalizations.rulesFor(language).languageName();
        return "The commander speaks " + name + ", and game events are summarized in " + name + ". "
                + "Form every spoken phrase (the text you pass to the speak function) in " + name + ". "
                + "Function names and their arguments stay exactly as defined.\n";
    }
}
