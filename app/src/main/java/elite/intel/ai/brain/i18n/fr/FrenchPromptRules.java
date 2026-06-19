package elite.intel.ai.brain.i18n.fr;

import elite.intel.ai.brain.i18n.PromptLanguageRules;
import elite.intel.ai.brain.actions.command.builtin.FindNearestFleetCarrierCommand;
import elite.intel.ai.brain.actions.command.builtin.DeployLandingGearCommand;
import elite.intel.ai.brain.actions.command.builtin.ToggleDiscoveryAnnouncementsCommand;
import elite.intel.ai.brain.actions.command.builtin.TargetDestinationCommand;
import elite.intel.ai.brain.actions.command.builtin.TaxiToLandingPadCommand;
import elite.intel.ai.brain.actions.command.builtin.TargetSubsystemCommand;
import elite.intel.ai.brain.actions.command.builtin.RetractLandingGearCommand;
import elite.intel.ai.brain.actions.command.builtin.SetSpeedZeroCommand;
import elite.intel.ai.brain.actions.command.builtin.EqualizePowerCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyzeSquadronCarrierRouteQueryCommand;
import elite.intel.ai.brain.actions.command.builtin.ShowStatusPanelCommand;
import elite.intel.ai.brain.actions.command.builtin.FighterAttackTargetCommand;
import elite.intel.ai.brain.actions.command.builtin.TransferPowerToWeaponsCommand;
import elite.intel.ai.brain.actions.command.builtin.ToggleAllAnnouncementsCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyzeMarketsQueryCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyzeCargoHoldQueryCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyseMaterialsQueryCommand;
import elite.intel.ai.brain.actions.command.builtin.ClearActiveMissionsCommand;
import elite.intel.ai.brain.actions.command.builtin.TransferPowerToShieldsCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyzeSquadronCarrierDataQueryCommand;
import elite.intel.ai.brain.actions.command.builtin.JumpToHyperspaceCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyzeShipLoadoutQueryCommand;
import elite.intel.ai.brain.actions.command.builtin.TransferPowerToEnginesCommand;
import elite.intel.ai.brain.actions.command.builtin.SelectFireGroupByNatoCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyzeBioScansStarSystemQueryCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyzeStationsQueryCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyzeGeologyInStarSystemQueryCommand;
import elite.intel.ai.brain.actions.command.builtin.DeployFighterCommand;
import elite.intel.ai.brain.actions.command.builtin.ActivateUiControlCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyzeFsdTargetQueryCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyzeBioSamplesPlanetSurfaceQueryCommand;
import elite.intel.ai.brain.actions.command.builtin.ShowInventoryPanelCommand;
import elite.intel.ai.brain.actions.command.builtin.InterruptCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyzeFleetCarrierDataQueryCommand;
import elite.intel.ai.brain.actions.command.builtin.ShowFighterPanelCommand;
import elite.intel.ai.brain.actions.handlers.query.AnalyzeRouterQueryCommand;
import elite.intel.ai.brain.actions.command.builtin.ToggleCargoScoopCommand;


public class FrenchPromptRules implements PromptLanguageRules {

    @Override
    public String languageName() {
        return "French";
    }

    @Override
    public String queryStarterExamples() {
        return "quoi, qui, où, comment, quel, quelle, quels, quelles, pourquoi, combien, y a-t-il, que, qu'est-ce que, avons-nous, sommes-nous, dis-moi, trouve, cherche, propose, statut, état, rapport, information, détails, compte rendu, analyse, étudie, distance, position, niveau";
    }

    @Override
    public String commandVerbExamples() {
        return "ouvre / active / désactive / navigue / trace / cible / sélectionne / verrouille / établis / fais / définis / règle / déploie / rentre / replie / allume / éteins / monte / relève / entre / sors / configure";
    }

    @Override
    public String queryPhraseExamples() {
        return "trouve / cherche / propose / quel est le statut / donne les informations / donne les détails / compte rendu / rapport / étudie / combien de sauts / quelle distance / quel cap / y a-t-il des signaux / que transportons-nous / sommes-nous arrivés / quelle station";
    }

    @Override
    public String disambiguationHints() {
        StringBuilder sb = new StringBuilder();

        sb.append("- Classify French as INTENT + SUBJECT + COMPLEMENT; never use one keyword alone.\n");
        sb.append("- INFO: rapport/statut/état/informations/combien/où/trouve/cherche. ACTION: ouvre/active/désactive/navigue/trace/cible/sélectionne/verrouille/règle/déploie.\n");
        sb.append("- Require explicit destructive intent for ").append(ClearActiveMissionsCommand.ID).append(".\n");
        sb.append("- COPY ACTION NAMES EXACTLY. Never invent synonyms or rename actions. Fleet status is exactly ");
        sb.append(AnalyzeFleetCarrierDataQueryCommand.ID);
        sb.append(" (never query_fleet_carrier_status_fuel_credit_balance). Fighter focus is exactly ");
        sb.append(FighterAttackTargetCommand.ID);
        sb.append(" (never fighter_focus_on_target).\n");

        sb.append("- sors/déploie + train d'atterrissage → ");
        sb.append(DeployLandingGearCommand.ID);
        sb.append("; rentre/relève/remonte + train → ");
        sb.append(RetractLandingGearCommand.ID);
        sb.append(". \"sors\" never retracts.\n");

        sb.append("- puissance/énergie/priorité + systèmes or boucliers → ");
        sb.append(TransferPowerToShieldsCommand.ID);
        sb.append("; moteurs → ").append(TransferPowerToEnginesCommand.ID);
        sb.append("; armes → ").append(TransferPowerToWeaponsCommand.ID);
        sb.append("; équilibre/réinitialise → ").append(EqualizePowerCommand.ID).append(".\n");
        sb.append("- With power words, systèmes=ship SYS. With route/map/FSD/scan/station/planet, système=star system. \"cible le système de survie\" → ").append(TargetSubsystemCommand.ID).append(".\n");

        sb.append("- coupe/désactive + toutes les annonces/notifications vocales → ");
        sb.append(ToggleAllAnnouncementsCommand.ID);
        sb.append("; never ");
        sb.append(ToggleDiscoveryAnnouncementsCommand.ID);
        sb.append(".\n");
        sb.append("- ouvre/affiche/montre + panneau/commandes + chasseur(s) → ");
        sb.append(ShowFighterPanelCommand.ID);
        sb.append("; déploie/lance/sors le chasseur → ");
        sb.append(DeployFighterCommand.ID);
        sb.append(".\n");

        sb.append("- Carrier: \"d'escadron\" means squadron carrier; without \"escadron\", porte-vaisseau(x)=player fleet carrier.\n");
        sb.append("- fleet carrier + statut/rapport/état/finances/autonomie/portée → ");
        sb.append(AnalyzeFleetCarrierDataQueryCommand.ID);
        sb.append(". squadron carrier + statut/rapport/carburant/tritium → ");
        sb.append(AnalyzeSquadronCarrierDataQueryCommand.ID);
        sb.append("; route/itinéraire required for ");
        sb.append(AnalyzeSquadronCarrierRouteQueryCommand.ID);
        sb.append(".\n");
        sb.append("- trouve/cherche porte-vaisseau le plus proche → ").append(FindNearestFleetCarrierCommand.ID).append(".\n");

        sb.append("- active alone → ").append(ActivateUiControlCommand.ID);
        sb.append("; active pilotage automatique/taxi → ").append(TaxiToLandingPadCommand.ID);
        sb.append("; active/lance saut or réacteur FSD → ").append(JumpToHyperspaceCommand.ID).append(".\n");
        sb.append("- stop alone → ").append(SetSpeedZeroCommand.ID);
        sb.append("; stop voix/silence/arrête de parler → ").append(InterruptCommand.ID).append(".\n");
        sb.append("- INFO + itinéraire/sauts → ").append(AnalyzeRouterQueryCommand.ID);
        sb.append("; ACTION trace/navigue/établis/définis uses the matching navigation action.\n");

        sb.append("- cible/sélectionne/verrouille + destination/prochaine étape → ");
        sb.append(TargetDestinationCommand.ID);
        sb.append("; INFO about cible FSD/prochaine destination → ");
        sb.append(AnalyzeFsdTargetQueryCommand.ID);
        sb.append(". \"cible\" alone has no action.\n");
        sb.append("- ");
        sb.append(SelectFireGroupByNatoCommand.ID);
        sb.append(" requires explicit groupe de tir/groupe d'armes/fire group + NATO identifier; never for destination/route.\n");
        sb.append("- cible + ailier → wingman action; cible + ship component → ").append(TargetSubsystemCommand.ID);
        sb.append("; chasseur attaque ma cible → fighter-focus action.\n");

        sb.append("- bio dans le système/quelles planètes → ");
        sb.append(AnalyzeBioScansStarSystemQueryCommand.ID);
        sb.append("; bio ici/sur cette planète → ");
        sb.append(AnalyzeBioSamplesPlanetSurfaceQueryCommand.ID);
        sb.append("; géologie/signaux géologiques → ").append(AnalyzeGeologyInStarSystemQueryCommand.ID).append(".\n");
        sb.append("- où se poser/stations → ").append(AnalyzeStationsQueryCommand.ID);
        sb.append("; marchés/commerce without commodity → ").append(AnalyzeMarketsQueryCommand.ID).append(".\n");

        sb.append("- Any \"trappe\" → ").append(ToggleCargoScoopCommand.ID);
        sb.append("; panneau/affichage/inventaire cargo → ").append(ShowInventoryPanelCommand.ID).append(".\n");
        sb.append("- INFO noun phrase \"inventaire/liste/stock des matériaux\" without ouvre/montre/affiche and without panneau → ");
        sb.append(AnalyseMaterialsQueryCommand.ID);
        sb.append(". Only ACTION ouvre/montre/affiche + panneau/inventaire du vaisseau/cargo → ");
        sb.append(ShowInventoryPanelCommand.ID);
        sb.append(". Never open the inventory panel for \"inventaire des matériaux\".\n");
        sb.append("- ouvre/montre/affiche + état/statut du vaisseau → ");
        sb.append(ShowStatusPanelCommand.ID);
        sb.append("; INFO modules/equipment/damage/specifications → ").append(AnalyzeShipLoadoutQueryCommand.ID).append(".\n");
        sb.append("- full cargo list → ").append(AnalyzeCargoHoldQueryCommand.ID);
        sb.append("; specific cargo commodity or engineering material → ").append(AnalyseMaterialsQueryCommand.ID).append(".\n");
        sb.append("- avons-nous/as-tu/est-ce équipé + installed ship module (collecteur de carburant, bouclier, capteurs, propulseurs, etc.) → ");
        sb.append(AnalyzeShipLoadoutQueryCommand.ID);
        sb.append("; never ").append(AnalyseMaterialsQueryCommand.ID).append(".\n");
        sb.append("- concentre-toi sur ma cible/focus sur la cible/chasseur attaque ma cible → ");
        sb.append(FighterAttackTargetCommand.ID);
        sb.append(".\n");
        return sb.toString();
    }
}
