package elite.intel.ai.brain.i18n.fr;

import elite.intel.ai.brain.i18n.PromptLanguageRules;

import static elite.intel.ai.brain.actions.Commands.ACTIVATE;
import static elite.intel.ai.brain.actions.Commands.CLEAR_ALL_ACTIVE_MISSIONS;
import static elite.intel.ai.brain.actions.Commands.DEPLOY_FIGHTER;
import static elite.intel.ai.brain.actions.Commands.DEPLOY_LANDING_GEAR;
import static elite.intel.ai.brain.actions.Commands.DISCOVERY_ON_OFF;
import static elite.intel.ai.brain.actions.Commands.FIGHTER_REQUEST_FOCUS_TARGET;
import static elite.intel.ai.brain.actions.Commands.FIND_NEAREST_FLEET_CARRIER;
import static elite.intel.ai.brain.actions.Commands.INCREASE_ENGINES_POWER;
import static elite.intel.ai.brain.actions.Commands.INCREASE_SHIELDS_POWER;
import static elite.intel.ai.brain.actions.Commands.INCREASE_WEAPONS_POWER;
import static elite.intel.ai.brain.actions.Commands.INTERRUPT_TTS;
import static elite.intel.ai.brain.actions.Commands.JUMP_TO_HYPERSPACE;
import static elite.intel.ai.brain.actions.Commands.RESET_POWER;
import static elite.intel.ai.brain.actions.Commands.RETRACT_LANDING_GEAR;
import static elite.intel.ai.brain.actions.Commands.SELECT_FIRE_GROUP_BY_NATO;
import static elite.intel.ai.brain.actions.Commands.SET_SPEED_ZERO;
import static elite.intel.ai.brain.actions.Commands.SHOW_INVENTORY_PANEL;
import static elite.intel.ai.brain.actions.Commands.SHOW_FIGHTER_PANEL;
import static elite.intel.ai.brain.actions.Commands.SHOW_STATUS_PANEL;
import static elite.intel.ai.brain.actions.Commands.TARGET_DESTINATION;
import static elite.intel.ai.brain.actions.Commands.TARGET_SUB_SYSTEM;
import static elite.intel.ai.brain.actions.Commands.TAXI;
import static elite.intel.ai.brain.actions.Commands.TOGGLE_ALL_ANNOUNCEMENTS;
import static elite.intel.ai.brain.actions.Commands.TOGGLE_CARGO_SCOOP;
import static elite.intel.ai.brain.actions.Queries.ANALYZE_MARKETS;
import static elite.intel.ai.brain.actions.Queries.BIO_SAMPLE_IN_STAR_SYSTEM;
import static elite.intel.ai.brain.actions.Queries.CARGO_HOLD_CONTENTS;
import static elite.intel.ai.brain.actions.Queries.EXOBIOLOGY_SAMPLES_ON_THIS_PLANET;
import static elite.intel.ai.brain.actions.Queries.FSD_TARGET_ANALYSIS;
import static elite.intel.ai.brain.actions.Queries.FLEET_CARRIER_STATUS;
import static elite.intel.ai.brain.actions.Queries.MATERIALS_INVENTORY;
import static elite.intel.ai.brain.actions.Queries.PLOTTED_ROUTE_ANALYSIS;
import static elite.intel.ai.brain.actions.Queries.QUERY_GEO_SIGNALS;
import static elite.intel.ai.brain.actions.Queries.QUERY_STATIONS;
import static elite.intel.ai.brain.actions.Queries.SHIP_LOADOUT;
import static elite.intel.ai.brain.actions.Queries.SQUADRON_CARRIER_ROUTE_ANALYSIS;
import static elite.intel.ai.brain.actions.Queries.SQUADRON_CARRIER_STATUS;

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
        sb.append("- Require explicit destructive intent for ").append(CLEAR_ALL_ACTIVE_MISSIONS.getAction()).append(".\n");
        sb.append("- COPY ACTION NAMES EXACTLY. Never invent synonyms or rename actions. Fleet status is exactly ");
        sb.append(FLEET_CARRIER_STATUS.getAction());
        sb.append(" (never query_fleet_carrier_status_fuel_credit_balance). Fighter focus is exactly ");
        sb.append(FIGHTER_REQUEST_FOCUS_TARGET.getAction());
        sb.append(" (never fighter_focus_on_target).\n");

        sb.append("- sors/déploie + train d'atterrissage → ");
        sb.append(DEPLOY_LANDING_GEAR.getAction());
        sb.append("; rentre/relève/remonte + train → ");
        sb.append(RETRACT_LANDING_GEAR.getAction());
        sb.append(". \"sors\" never retracts.\n");

        sb.append("- puissance/énergie/priorité + systèmes or boucliers → ");
        sb.append(INCREASE_SHIELDS_POWER.getAction());
        sb.append("; moteurs → ").append(INCREASE_ENGINES_POWER.getAction());
        sb.append("; armes → ").append(INCREASE_WEAPONS_POWER.getAction());
        sb.append("; équilibre/réinitialise → ").append(RESET_POWER.getAction()).append(".\n");
        sb.append("- With power words, systèmes=ship SYS. With route/map/FSD/scan/station/planet, système=star system. \"cible le système de survie\" → ").append(TARGET_SUB_SYSTEM.getAction()).append(".\n");

        sb.append("- coupe/désactive + toutes les annonces/notifications vocales → ");
        sb.append(TOGGLE_ALL_ANNOUNCEMENTS.getAction());
        sb.append("; never ");
        sb.append(DISCOVERY_ON_OFF.getAction());
        sb.append(".\n");
        sb.append("- ouvre/affiche/montre + panneau/commandes + chasseur(s) → ");
        sb.append(SHOW_FIGHTER_PANEL.getAction());
        sb.append("; déploie/lance/sors le chasseur → ");
        sb.append(DEPLOY_FIGHTER.getAction());
        sb.append(".\n");

        sb.append("- Carrier: \"d'escadron\" means squadron carrier; without \"escadron\", porte-vaisseau(x)=player fleet carrier.\n");
        sb.append("- fleet carrier + statut/rapport/état/finances/autonomie/portée → ");
        sb.append(FLEET_CARRIER_STATUS.getAction());
        sb.append(". squadron carrier + statut/rapport/carburant/tritium → ");
        sb.append(SQUADRON_CARRIER_STATUS.getAction());
        sb.append("; route/itinéraire required for ");
        sb.append(SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction());
        sb.append(".\n");
        sb.append("- trouve/cherche porte-vaisseau le plus proche → ").append(FIND_NEAREST_FLEET_CARRIER.getAction()).append(".\n");

        sb.append("- active alone → ").append(ACTIVATE.getAction());
        sb.append("; active pilotage automatique/taxi → ").append(TAXI.getAction());
        sb.append("; active/lance saut or réacteur FSD → ").append(JUMP_TO_HYPERSPACE.getAction()).append(".\n");
        sb.append("- stop alone → ").append(SET_SPEED_ZERO.getAction());
        sb.append("; stop voix/silence/arrête de parler → ").append(INTERRUPT_TTS.getAction()).append(".\n");
        sb.append("- INFO + itinéraire/sauts → ").append(PLOTTED_ROUTE_ANALYSIS.getAction());
        sb.append("; ACTION trace/navigue/établis/définis uses the matching navigation action.\n");

        sb.append("- cible/sélectionne/verrouille + destination/prochaine étape → ");
        sb.append(TARGET_DESTINATION.getAction());
        sb.append("; INFO about cible FSD/prochaine destination → ");
        sb.append(FSD_TARGET_ANALYSIS.getAction());
        sb.append(". \"cible\" alone has no action.\n");
        sb.append("- ");
        sb.append(SELECT_FIRE_GROUP_BY_NATO.getAction());
        sb.append(" requires explicit groupe de tir/groupe d'armes/fire group + NATO identifier; never for destination/route.\n");
        sb.append("- cible + ailier → wingman action; cible + ship component → ").append(TARGET_SUB_SYSTEM.getAction());
        sb.append("; chasseur attaque ma cible → fighter-focus action.\n");

        sb.append("- bio dans le système/quelles planètes → ");
        sb.append(BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
        sb.append("; bio ici/sur cette planète → ");
        sb.append(EXOBIOLOGY_SAMPLES_ON_THIS_PLANET.getAction());
        sb.append("; géologie/signaux géologiques → ").append(QUERY_GEO_SIGNALS.getAction()).append(".\n");
        sb.append("- où se poser/stations → ").append(QUERY_STATIONS.getAction());
        sb.append("; marchés/commerce without commodity → ").append(ANALYZE_MARKETS.getAction()).append(".\n");

        sb.append("- Any \"trappe\" → ").append(TOGGLE_CARGO_SCOOP.getAction());
        sb.append("; panneau/affichage/inventaire cargo → ").append(SHOW_INVENTORY_PANEL.getAction()).append(".\n");
        sb.append("- ouvre/montre/affiche + état/statut du vaisseau → ");
        sb.append(SHOW_STATUS_PANEL.getAction());
        sb.append("; INFO modules/equipment/damage/specifications → ").append(SHIP_LOADOUT.getAction()).append(".\n");
        sb.append("- full cargo list → ").append(CARGO_HOLD_CONTENTS.getAction());
        sb.append("; specific cargo commodity or engineering material → ").append(MATERIALS_INVENTORY.getAction()).append(".\n");
        sb.append("- avons-nous/as-tu/est-ce équipé + installed ship module (collecteur de carburant, bouclier, capteurs, propulseurs, etc.) → ");
        sb.append(SHIP_LOADOUT.getAction());
        sb.append("; never ").append(MATERIALS_INVENTORY.getAction()).append(".\n");
        sb.append("- concentre-toi sur ma cible/focus sur la cible/chasseur attaque ma cible → ");
        sb.append(FIGHTER_REQUEST_FOCUS_TARGET.getAction());
        sb.append(".\n");
        return sb.toString();
    }
}
