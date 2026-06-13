package elite.intel.ai.brain.i18n.de;

import elite.intel.ai.brain.i18n.PromptLanguageRules;

import static elite.intel.ai.brain.actions.Commands.CLEAR_ALL_ACTIVE_MISSIONS;

public class GermanPromptRules implements PromptLanguageRules {

    @Override
    public String languageName() {
        return "German";
    }

    @Override
    public String queryStarterExamples() {
        return "was, wo, wie, welcher, welche, welches, warum, gibt es, wie viel, wie viele, erzähl mir";
    }

    @Override
    public String commandVerbExamples() {
        return "zeig / öffne / finde / suche / aktiviere / deaktiviere / navigiere / plotte / fahre aus / einziehen / einschalten / ausschalten";
    }

    @Override
    public String queryPhraseExamples() {
        return "wo / was / wie viel / wie viele / gibt es / welcher / welche / auf welcher Station / in welchem System";
    }

    @Override
    public String disambiguationHints() {
        StringBuilder sb = new StringBuilder();

        sb.append("- require very high probability match for action →");
        sb.append(CLEAR_ALL_ACTIVE_MISSIONS.getAction());
        sb.append("\n");
        return sb.toString();
    }
}
