package elite.intel.ai.brain.i18n.es;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.i18n.PromptLanguageRules;


public class SpanishPromptRules implements PromptLanguageRules {

    @Override
    public String languageName() {
        return "Spanish";
    }

    @Override
    public String queryStarterExamples() {
        return "qué, dónde, cómo, cuál, cuáles, por qué, hay, cuánto, cuántos, dime";
    }

    @Override
    public String commandVerbExamples() {
        return "muestra / abre / encuentra / busca / activa / desactiva / navega / traza / despliega / retrae / enciende / apaga";
    }

    @Override
    public String queryPhraseExamples() {
        return "dónde / qué / cuánto / cuántos / hay / cuál / cuáles / en qué estación / en qué sistema";
    }


    @Override
    public String disambiguationHints() {
        StringBuilder sb = new StringBuilder();

        sb.append("- require very high probability match for action → ");
        sb.append(CommandIds.CLEAR_ACTIVE_MISSIONS);
        sb.append("\n");
        return sb.toString();
    }
}
