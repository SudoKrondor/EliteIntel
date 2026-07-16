package elite.intel.companion.prompt;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.commons.PromptFactory;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.model.ThoughtSource;
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

    static String languageName() {
        return effectiveLanguage().displayName();
    }

    static String inputLanguageName() {
        return SystemSession.getInstance().getLanguage().displayName();
    }

    static String personalityClause() {
        return SystemSession.getInstance().getAIPersonality().getPersonalityClause();
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
