package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.CompanionIdentity;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.commons.PromptFactory;
import elite.intel.ai.brain.vega.CompanionConfig;
import elite.intel.ai.brain.vega.model.ThoughtSource;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

/** Resolves source-specific system prompts and their shared dynamic values. */
public final class CompanionSystemPrompt implements SystemPromptText {

    @Override
    public String staticRules(ThoughtSource source) {
        return switch (source) {
            case COMMANDER -> CommanderPrompt.render();
            case EVENT -> NarrationPrompt.render();
        };
    }

    static String companionName() {
        return CompanionConfig.companionName();
    }

    /**
     * Who the companion is - the same clause the analysis prompt opens with.
     */
    static String identityClause() {
        return CompanionIdentity.identityClause();
    }

    static String languageName() {
        return effectiveLanguage().displayName();
    }

    static String inputLanguageName() {
        return SystemSession.getInstance().getLanguage().displayName();
    }

    static String personalityClause() {
        return SystemSession.getInstance().getAIPersonality().getPersonalityClause();
    }

    /**
     * How the companion speaks of itself: the grammatical gender of the voice the active ship carries.
     * <p>
     * This was the word "feminine", written into both prompts as a constant, back when every ship voice was
     * forced female. The fleet grid now offers each engine's male voices too, and a masculine voice narrating
     * itself in feminine forms is immediately audible - in the languages with grammatical gender, on every
     * sentence. One word either way, so the prompt budget is unchanged.
     */
    static String selfGender() {
        return SystemSession.getInstance().getVoiceGender().isMale() ? "masculine" : "feminine";
    }

    static String addressRule() {
        StringBuilder rule = new StringBuilder();
        PromptFactory.appendContext(rule, "the commander");
        return rule.toString();
    }

    private static Language effectiveLanguage() {
        return AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
    }
}
