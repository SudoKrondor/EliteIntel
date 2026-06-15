package elite.intel.ai.brain.i18n.fr;

import elite.intel.ai.brain.i18n.PromptLanguageRules;

import static elite.intel.ai.brain.actions.Commands.CLEAR_ALL_ACTIVE_MISSIONS;

public class FrenchPromptRules implements PromptLanguageRules {

    @Override
    public String languageName() {
        return "French";
    }

    @Override
    public String queryStarterExamples() {
        return "quoi, qui, où, comment, quel, quelle, quels, quelles, pourquoi, combien, y a-t-il, que, qu'est-ce que, avons-nous, sommes-nous, dis-moi, statut, état, rapport, analyse, distance, position, niveau";
    }

    @Override
    public String commandVerbExamples() {
        return "montre / ouvre / trouve / cherche / active / désactive / navigue / trace / déploie / rentre / replie / allume / éteins / monte / relève / entre / sors /configure";
    }

    @Override
    public String queryPhraseExamples() {
        return "quel est le statut / combien de sauts / quelle distance / quel cap / y a-t-il des signaux / que transportons-nous / sommes-nous arrivés / quelle station /rapport";
    }

    @Override
    public String disambiguationHints() {
        StringBuilder sb = new StringBuilder();

        sb.append("- require very high probability match for action → ");
        sb.append(CLEAR_ALL_ACTIVE_MISSIONS.getAction());
        sb.append("\n");
        return sb.toString();
    }
}
