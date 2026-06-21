package elite.intel.ai.brain.i18n.pt;

import elite.intel.ai.brain.i18n.PromptLanguageRules;

public class PortuguesePromptRules implements PromptLanguageRules {

    @Override
    public String languageName() {
        return "Portuguese";
    }

    @Override
    public String queryStarterExamples() {
        return "o que, onde, como, qual, quais, por que, há, quanto, quantos, me diga";
    }

    @Override
    public String commandVerbExamples() {
        return "mostrar / abrir / encontrar / navegar / ativar / desativar / traçar / implantar / retrair / ligar / desligar";
    }

    @Override
    public String queryPhraseExamples() {
        return "onde / o que / quanto / quantos / há / qual / quais / em que estação / em que sistema";
    }

    @Override
    public String localeSpecificFormattingRules() {
        return "";
    }
}
