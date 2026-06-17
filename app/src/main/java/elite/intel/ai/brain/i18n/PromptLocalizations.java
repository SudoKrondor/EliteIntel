package elite.intel.ai.brain.i18n;

import elite.intel.ai.brain.i18n.de.GermanPromptRules;
import elite.intel.ai.brain.i18n.en.EnglishPromptRules;
import elite.intel.ai.brain.i18n.es.SpanishPromptRules;
import elite.intel.ai.brain.i18n.fr.FrenchPromptRules;
import elite.intel.ai.brain.i18n.pt.PortuguesePromptRules;
import elite.intel.ai.brain.i18n.ru.RussianPromptRules;
import elite.intel.ai.brain.i18n.uk.UkrainianPromptRules;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

/**
 * Factory that supplies the correct {@link PromptLanguageRules} for a given language.
 * <p>
 * Mirrors {@link AiActionLocalizations} and {@link InputNormalizerLocalizations}:
 * one file per language, no shared mutable state, parallel localizer-friendly.
 */
public final class PromptLocalizations {

    private PromptLocalizations() {
    }

    /**
     * Rules for the current session language.
     */
    public static PromptLanguageRules rules() {
        return rulesFor(SystemSession.getInstance().getLanguage());
    }

    /**
     * Rules for an explicit language (e.g. when response language differs from input language).
     */
    public static PromptLanguageRules rulesFor(Language lang) {
        return switch (lang) {
            case EN -> new EnglishPromptRules();
            case RU -> new RussianPromptRules();
            case UK -> new UkrainianPromptRules();
            case DE -> new GermanPromptRules();
            case FR -> new FrenchPromptRules();
            case ES -> new SpanishPromptRules();
            case PT -> new PortuguesePromptRules();
        };
    }
}
