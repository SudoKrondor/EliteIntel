package elite.intel.companion.prompt;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.commons.PromptFactory;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

/**
 * The companion's static system-prompt owner: a thin dispatcher over the per-thought-type templates, plus the
 * few dynamic values those templates interpolate.
 * <p>
 * Dispatch: each {@link ThoughtSource} that composes a prompt owns its full text in a single-constant template
 * ({@link CommanderPrompt}, {@link NarrationPrompt}); {@link #staticRules} routes to the right one. This class
 * stays the {@link SystemPromptText} entry point so {@link PromptComposer} and tests bind to one stable type.
 * <p>
 * Fragments: the templates are otherwise literal text; the only insertions are the genuinely dynamic values
 * provided here - the companion name ({@code {name}}), the resolved commander-language name ({@code {language}}),
 * the address rule ({@code {address}}), and the AI personality roleplay clause ({@code {personalityClause}}) -
 * because the name comes from config, the language name is resolved from the session, the address form is chosen
 * at random, and the personality is the commander's session setting (shared with the legacy router).
 */
public final class CompanionSystemPromptPart implements SystemPromptText {

    @Override
    public String staticRules(ThoughtSource source) {
        return switch (source) {
            case COMMANDER -> CommanderPrompt.render();
            case NARRATION -> NarrationPrompt.render();
            // EVENT thoughts are memory-only (see EventThought); they never compose a prompt.
            case EVENT -> throw new IllegalArgumentException("EVENT thoughts do not compose a prompt");
        };
    }

    /** The configured companion name, woven into the persona and narration templates as {@code {name}}. */
    static String companionName() {
        return CompanionConfig.companionName();
    }

    /**
     * The language the companion SPEAKS, named in the templates' language rule as {@code {language}}. This is the
     * TTS-bound effective response language, which can differ from what the commander speaks (e.g. local Kokoro
     * cannot voice ru/uk/de, so those downgrade to English output) - see {@link AiResponseLanguagePolicy}.
     */
    static String languageName() {
        return effectiveLanguage().displayName();
    }

    /**
     * The language the commander gives ORDERS in, named in the template as {@code {inputLanguage}}. This is the
     * session/STT language ({@link SystemSession#getLanguage()}) - the language of the text the model must match
     * to a function - NOT the effective response language, which can downgrade to English for TTS reasons while
     * the commander still speaks his own language.
     */
    static String inputLanguageName() {
        return SystemSession.getInstance().getLanguage().displayName();
    }


    /**
     * The commander-chosen AI personality's roleplay clause, woven into the persona as {@code {personalityClause}}.
     * Reuses the same source as the legacy router ({@link SystemSession#getAIPersonality()}), so the companion and
     * the legacy brain share one personality setting. The clause is already self-labeled ("Your Personality
     * Roleplay: ..."), so no extra label is added.
     */
    static String personalityClause() {
        return SystemSession.getInstance().getAIPersonality().getPersonalityClause();
    }

    /**
     * Tells the model how to address the commander, reusing the legacy router's address instruction
     * ({@link PromptFactory#appendContext(StringBuilder, String)}): name / military rank / honorific, chosen at
     * random. The forms are stable within a session, so this stays in the cached prefix. Ends with a newline.
     */
    static String addressRule() {
        StringBuilder sb = new StringBuilder();
        PromptFactory.appendContext(sb, "the commander");
        return sb.toString();
    }

    private static Language effectiveLanguage() {
        return AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
    }
}
