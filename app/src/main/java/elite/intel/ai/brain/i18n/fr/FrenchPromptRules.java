package elite.intel.ai.brain.i18n.fr;

import elite.intel.ai.brain.i18n.PromptLanguageRules;

import static elite.intel.ai.brain.actions.command.CommandIds.*;
import static elite.intel.ai.brain.actions.query.QueryIds.*;

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
        sb.append("- Require explicit destructive intent for ").append(CLEAR_ACTIVE_MISSIONS).append(".\n");
        sb.append("- COPY ACTION NAMES EXACTLY. Never invent synonyms or rename actions. Fleet status is exactly ");
        sb.append(FLEET_CARRIER_STATUS);
        sb.append(" (never query_fleet_carrier_status_fuel_credit_balance). Fighter focus is exactly ");
        sb.append(FIGHTER_ATTACK_TARGET);
        sb.append(" (never fighter_focus_on_target).\n");

        sb.append("- sors/déploie + train d'atterrissage → ");
        sb.append(DEPLOY_LANDING_GEAR);
        sb.append("; rentre/relève/remonte + train → ");
        sb.append(RETRACT_LANDING_GEAR);
        sb.append(". \"sors\" never retracts.\n");

        sb.append("- puissance/énergie/priorité + systèmes or boucliers → ");
        sb.append(TRANSFER_POWER_TO_SHIELDS);
        sb.append("; moteurs → ").append(TRANSFER_POWER_TO_ENGINES);
        sb.append("; armes → ").append(TRANSFER_POWER_TO_WEAPONS);
        sb.append("; équilibre/réinitialise → ").append(EQUALIZE_POWER).append(".\n");
        sb.append("- With power words, systèmes=ship SYS. With route/map/FSD/scan/station/planet, système=star system. \"cible le système de survie\" → ").append(TARGET_SUBSYSTEM).append(".\n");

        sb.append("- coupe/désactive + toutes les annonces/notifications vocales → ");
        sb.append(TOGGLE_ALL_ANNOUNCEMENTS);
        sb.append("; never ");
        sb.append(TOGGLE_DISCOVERY_ANNOUNCEMENTS);
        sb.append(".\n");
        sb.append("- ouvre/affiche/montre + panneau/commandes + chasseur(s) → ");
        sb.append(SHOW_FIGHTER_PANEL);
        sb.append("; déploie/lance/sors le chasseur → ");
        sb.append(DEPLOY_FIGHTER);
        sb.append(".\n");

        sb.append("- Carrier: \"d'escadron\" means squadron carrier; without \"escadron\", porte-vaisseau(x)=player fleet carrier.\n");
        sb.append("- fleet carrier + statut/rapport/état/finances/autonomie/portée → ");
        sb.append(FLEET_CARRIER_STATUS);
        sb.append(". squadron carrier + statut/rapport/carburant/tritium → ");
        sb.append(SQUADRON_CARRIER_STATUS);
        sb.append("; route/itinéraire required for ");
        sb.append(SQUADRON_CARRIER_ROUTE_ANALYSIS);
        sb.append(".\n");
        sb.append("- trouve/cherche porte-vaisseau le plus proche → ").append(FIND_NEAREST_FLEET_CARRIER).append(".\n");

        sb.append("- active alone → ").append(ACTIVATE_UI_CONTROL);
        sb.append("; active pilotage automatique/taxi → ").append(TAXI_TO_LANDING_PAD);
        sb.append("; active/lance saut or réacteur FSD → ").append(JUMP_TO_HYPERSPACE).append(".\n");
        sb.append("- stop alone → ").append(SET_SPEED_TO_ZERO_0_STOP_SHIP);
        sb.append("; stop voix/silence/arrête de parler → ").append(INTERRUPT).append(".\n");
        sb.append("- INFO + itinéraire/sauts → ").append(PLOTTED_ROUTE_ANALYSIS);
        sb.append("; ACTION trace/navigue/établis/définis uses the matching navigation action.\n");

        sb.append("- cible/sélectionne/verrouille + destination/prochaine étape → ");
        sb.append(TARGET_DESTINATION);
        sb.append("; INFO about cible FSD/prochaine destination → ");
        sb.append(FSD_TARGET_ANALYSIS);
        sb.append(". \"cible\" alone has no action.\n");
        sb.append("- ");
        sb.append(SELECT_FIRE_GROUP_BY_NATO);
        sb.append(" requires explicit groupe de tir/groupe d'armes/fire group + NATO identifier; never for destination/route.\n");
        sb.append("- cible + ailier → wingman action; cible + ship component → ").append(TARGET_SUBSYSTEM);
        sb.append("; chasseur attaque ma cible → fighter-focus action.\n");

        sb.append("- bio dans le système/quelles planètes → ");
        sb.append(BIO_SAMPLE_IN_STAR_SYSTEM);
        sb.append("; bio ici/sur cette planète → ");
        sb.append(EXOBIOLOGY_SAMPLES_ON_THIS_PLANET);
        sb.append("; géologie/signaux géologiques → ").append(QUERY_GEO_SIGNALS).append(".\n");
        sb.append("- où se poser/stations → ").append(QUERY_STATIONS);
        sb.append("; marchés/commerce without commodity → ").append(ANALYZE_MARKETS).append(".\n");

        sb.append("- Any \"trappe\" → ").append(TOGGLE_CARGO_SCOOP);
        sb.append("; panneau/affichage/inventaire cargo → ").append(SHOW_INVENTORY_PANEL).append(".\n");
        sb.append("- INFO noun phrase \"inventaire/liste/stock des matériaux\" without ouvre/montre/affiche and without panneau → ");
        sb.append(MATERIALS_INVENTORY);
        sb.append(". Only ACTION ouvre/montre/affiche + panneau/inventaire du vaisseau/cargo → ");
        sb.append(SHOW_INVENTORY_PANEL);
        sb.append(". Never open the inventory panel for \"inventaire des matériaux\".\n");
        sb.append("- ouvre/montre/affiche + état/statut du vaisseau → ");
        sb.append(SHOW_STATUS_PANEL);
        sb.append("; INFO modules/equipment/damage/specifications → ").append(SHIP_LOADOUT).append(".\n");
        sb.append("- full cargo list → ").append(CARGO_HOLD_CONTENTS);
        sb.append("; specific cargo commodity or engineering material → ").append(MATERIALS_INVENTORY).append(".\n");
        sb.append("- avons-nous/as-tu/est-ce équipé + installed ship module (collecteur de carburant, bouclier, capteurs, propulseurs, etc.) → ");
        sb.append(SHIP_LOADOUT);
        sb.append("; never ").append(MATERIALS_INVENTORY).append(".\n");
        sb.append("- concentre-toi sur ma cible/focus sur la cible/chasseur attaque ma cible → ");
        sb.append(FIGHTER_ATTACK_TARGET);
        sb.append(".\n");
        return sb.toString();
    }
}
