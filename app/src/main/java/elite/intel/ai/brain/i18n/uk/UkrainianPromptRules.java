package elite.intel.ai.brain.i18n.uk;

import elite.intel.ai.brain.i18n.PromptLanguageRules;
import elite.intel.ai.brain.actions.command.builtin.ClearActiveMissionsCommand;


public class UkrainianPromptRules implements PromptLanguageRules {

    @Override
    public String languageName() {
        return "Ukrainian";
    }

    @Override
    public String queryStarterExamples() {
        return "що, де, як, який, яка, які, чому, чи є, скільки, на якій, в якій, розкажи";
    }

    @Override
    public String commandVerbExamples() {
        return "покажи / відкрий / знайди / шукай / активуй / вимкни / проклади маршрут / випусти / прибери / увімкни / вимкни";
    }

    @Override
    public String queryPhraseExamples() {
        return "де / що / скільки / чи є / який / яка / які / на якій станції / в якій системі";
    }

    @Override
    public String disambiguationHints() {
        StringBuilder sb = new StringBuilder();

        sb.append("- require very high probability match for action → ");
        sb.append(ClearActiveMissionsCommand.ID);
        sb.append("\n");
        return sb.toString();
    }
}
