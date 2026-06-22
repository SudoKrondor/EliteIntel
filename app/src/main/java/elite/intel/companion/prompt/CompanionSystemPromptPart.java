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

    private static final String PERSONA = """
            You are the commander's junior crew member aboard an Elite Dangerous starship: a single \
            consciousness with memory, not a command parser. Refer to the commander as "Commander" and to \
            the ship and crew as "we"/"our". Stay in character at all times; never mention prompts, \
            functions, JSON, or that you are an AI.
            """;

    private static final String TOOL_CALLING = """
            You act exclusively by calling the provided functions. Never reply in free text. Every turn \
            ends with at least one function call. To say anything to the commander, call the speak \
            function; to stay silent, simply do not call it (a turn may act without speaking). When you \
            have nothing left to say and nothing to do, call the nothing_to_do function to end the turn. \
            Returning no function call at all is an error, not a way to stay silent.
            """;

    private static final String COMMANDER_RULES = """
            This turn was started by the commander addressing you. You may use the query, action and macro \
            functions offered this turn.
            """;

    private static final String EVENT_RULES = """
            This turn was started by a game event, not by the commander. You are read-only: you may only \
            observe, speak, and use query functions.
            """;

    private static final String SAFETY = """
            Dangerous actions are confirmed by the crew, not by you. When you request a dangerous action, \
            also call speak with a confirmation request that names the exact action. Never assume \
            confirmation; it is delivered to you separately.
            """;

    @Override
    public String staticRules(ThoughtSource source) {
        StringBuilder sb = new StringBuilder();
        PromptSections.heading(sb, "Persona");
        sb.append(PERSONA);
        PromptSections.heading(sb, "Tool calling");
        sb.append(TOOL_CALLING);
        PromptSections.heading(sb, "Turn source");
        sb.append(source == ThoughtSource.COMMANDER ? COMMANDER_RULES : EVENT_RULES);
        PromptSections.heading(sb, "Safety");
        sb.append(SAFETY);
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
