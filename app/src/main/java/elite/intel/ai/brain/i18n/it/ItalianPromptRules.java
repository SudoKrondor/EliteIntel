package elite.intel.ai.brain.i18n.it;

import elite.intel.ai.brain.actions.command.builtin.*;
import elite.intel.ai.brain.actions.handlers.query.*;
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

        sb.append("- estrai/abbassa/schiera + carrello → ");
        sb.append(DeployLandingGearCommand.ID);
        sb.append("; ritira/alza/rientra + carrello → ");
        sb.append(RetractLandingGearCommand.ID);
        sb.append(". \"estrai\" never retracts.\n");

        sb.append("- potenza/energia/priorità + scudi or sistemi → ");
        sb.append(TransferPowerToShieldsCommand.ID);
        sb.append("; motori → ").append(TransferPowerToEnginesCommand.ID);
        sb.append("; armi → ").append(TransferPowerToWeaponsCommand.ID);
        sb.append("; bilancia/reset/distribuisci → ").append(EqualizePowerCommand.ID).append(".\n");
        sb.append("- With power words, sistemi=ship SYS. With rotta/mappa/FSD/scansione/stazione/pianeta, sistema=star system. \"mira al sistema di supporto vitale\" → ").append(TargetSubsystemCommand.ID).append(".\n");

        sb.append("- disattiva/silenzia + tutte le notifiche/annunci vocali → ");
        sb.append(ToggleAllAnnouncementsCommand.ID);
        sb.append("; never ");
        sb.append(ToggleDiscoveryAnnouncementsCommand.ID);
        sb.append(".\n");
        sb.append("- apri/mostra/visualizza + pannello/comandi + caccia → ");
        sb.append(ShowFighterPanelCommand.ID);
        sb.append("; schiera/lancia/fai uscire il caccia → ");
        sb.append(DeployFighterCommand.ID);
        sb.append(".\n");

        sb.append("- Carrier: \"dello squadrone\" means squadron carrier; without \"squadrone\", portanavi/fleet carrier = player fleet carrier.\n");
        sb.append("- fleet carrier + stato/rapporto/finanze/autonomia/portata → ");
        sb.append(AnalyzeFleetCarrierDataQueryCommand.ID);
        sb.append(". squadron carrier + stato/rapporto/carburante/tritio → ");
        sb.append(AnalyzeSquadronCarrierDataQueryCommand.ID);
        sb.append("; rotta/itinerario required for ");
        sb.append(AnalyzeSquadronCarrierRouteQueryCommand.ID);
        sb.append(".\n");
        sb.append("- trova/cerca portanavi più vicina → ").append(FindNearestFleetCarrierCommand.ID).append(".\n");

        sb.append("- attiva alone → ").append(ActivateUiControlCommand.ID);
        sb.append("; attiva pilota automatico/taxi → ").append(TaxiToLandingPadCommand.ID);
        sb.append("; attiva/avvia salto or reattore FSD → ").append(JumpToHyperspaceCommand.ID).append(".\n");
        sb.append("- ferma/fermati/alt alone → ").append(SetSpeedZeroCommand.ID);
        sb.append("; zitto/silenzio/smettila di parlare → ").append(InterruptCommand.ID).append(".\n");
        sb.append("- INFO + itinerario/salti → ").append(AnalyzeRouterQueryCommand.ID);
        sb.append("; ACTION traccia/naviga/imposta/definisci uses the matching navigation action.\n");

        sb.append("- mira/seleziona/blocca + destinazione/prossima tappa → ");
        sb.append(TargetDestinationCommand.ID);
        sb.append("; INFO about destinazione FSD/prossima destinazione → ");
        sb.append(AnalyzeFsdTargetQueryCommand.ID);
        sb.append(". \"mira\" alone has no action.\n");
        sb.append("- ");
        sb.append(SelectFireGroupByNatoCommand.ID);
        sb.append(" requires explicit gruppo di fuoco/gruppo armi/fire group + NATO identifier; never for destination/route.\n");
        sb.append("- mira + gregario/compagno d'ala → wingman action; mira + ship component → ").append(TargetSubsystemCommand.ID);
        sb.append("; il caccia attacca il mio bersaglio → fighter-focus action.\n");

        sb.append("- bio nel sistema/quali pianeti → ");
        sb.append(AnalyzeBioScansStarSystemQueryCommand.ID);
        sb.append("; bio qui/su questo pianeta → ");
        sb.append(AnalyzeBioSamplesPlanetSurfaceQueryCommand.ID);
        sb.append("; geologia/segnali geologici → ").append(AnalyzeGeologyInStarSystemQueryCommand.ID).append(".\n");
        sb.append("- dove attraccare/stazioni → ").append(AnalyzeStationsQueryCommand.ID);
        sb.append("; mercati/commercio without commodity → ").append(AnalyzeMarketsQueryCommand.ID).append(".\n");

        sb.append("- Any \"scoop\"/\"vano di carico\" → ").append(ToggleCargoScoopCommand.ID);
        sb.append("; pannello/inventario/cargo → ").append(ShowInventoryPanelCommand.ID).append(".\n");
        sb.append("- INFO noun phrase \"inventario/lista/scorta dei materiali\" without apri/mostra/visualizza and without pannello → ");
        sb.append(AnalyseMaterialsQueryCommand.ID);
        sb.append(". Only ACTION apri/mostra/visualizza + pannello/inventario della nave/cargo → ");
        sb.append(ShowInventoryPanelCommand.ID);
        sb.append(". Never open the inventory panel for \"inventario dei materiali\".\n");
        sb.append("- apri/mostra/visualizza + stato/condizione della nave → ");
        sb.append(ShowStatusPanelCommand.ID);
        sb.append("; INFO moduli/equipaggiamento/danni/specifiche → ").append(AnalyzeShipLoadoutQueryCommand.ID).append(".\n");
        sb.append("- full cargo list → ").append(AnalyzeCargoHoldQueryCommand.ID);
        sb.append("; specific cargo commodity or engineering material → ").append(AnalyseMaterialsQueryCommand.ID).append(".\n");
        sb.append("- abbiamo/hai/è equipaggiata + installed ship module (raccoglitore di carburante, scudo, sensori, propulsori, ecc.) → ");
        sb.append(AnalyzeShipLoadoutQueryCommand.ID);
        sb.append("; never ").append(AnalyseMaterialsQueryCommand.ID).append(".\n");
        sb.append("- concentrati sul mio bersaglio/il caccia attacca il mio bersaglio → ");
        sb.append(FighterAttackTargetCommand.ID);
        sb.append(".\n");
        return sb.toString();
    }
}
