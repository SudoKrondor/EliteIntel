package elite.intel.ai.brain.i18n.it;

import elite.intel.ai.brain.actions.command.builtin.ClearActiveMissionsCommand;
import elite.intel.ai.brain.actions.command.builtin.FighterAttackTargetCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyzeFleetCarrierDataQueryCommand;
import elite.intel.ai.brain.i18n.PromptLanguageRules;


public class ItalianPromptRules implements PromptLanguageRules {

    @Override
    public String languageName() {
        return "Italian";
    }

    @Override
    public String queryStarterExamples() {
        return "cosa, dove, come, qual, quali, perché, c'è, ci sono, quanti, quanta, dimmi, abbiamo, siamo, trova, cerca, proponi, stato, distanza, posizione, livello, informazioni, dettagli, rapporto, analisi, studia";
    }

    @Override
    public String commandVerbExamples() {
        return "apri / mostra / visualizza / attiva / disattiva / naviga / traccia / punta / mira / seleziona / blocca / imposta / definisci / regola / schiera / ritira / accendi / spegni / entra / esci / configura";
    }

    @Override
    public String queryPhraseExamples() {
        return "dove / cosa / quanto / quanti / ci sono / qual è lo stato / dammi le informazioni / dammi i dettagli / rapporto / quanti salti / che distanza / che rotta / ci sono segnali / cosa trasportiamo / siamo arrivati / quale stazione";
    }

    @Override
    public String disambiguationHints() {
        StringBuilder sb = new StringBuilder();

        sb.append("- Classify Italian as INTENT + SUBJECT + COMPLEMENT; never use one keyword alone.\n");
        sb.append("- INFO: rapporto/stato/informazioni/quanti/dove/trova/cerca. ACTION: apri/attiva/disattiva/naviga/traccia/punta/seleziona/blocca/regola/schiera.\n");
        sb.append("- Require explicit destructive intent for ").append(ClearActiveMissionsCommand.ID).append(".\n");
        sb.append("- COPY ACTION NAMES EXACTLY. Never invent synonyms or rename actions. Fleet carrier status is exactly ");
        sb.append(AnalyzeFleetCarrierDataQueryCommand.ID);
        sb.append(". Fighter focus is exactly ");
        sb.append(FighterAttackTargetCommand.ID);
        sb.append(".\n");
        return sb.toString();
    }
}
