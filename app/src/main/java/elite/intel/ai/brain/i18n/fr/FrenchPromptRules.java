package elite.intel.ai.brain.i18n.fr;

import elite.intel.ai.brain.i18n.PromptLanguageRules;

import static elite.intel.ai.brain.actions.Commands.CLEAR_ALL_ACTIVE_MISSIONS;
import static elite.intel.ai.brain.actions.Commands.FIND_NEAREST_FLEET_CARRIER;
import static elite.intel.ai.brain.actions.Commands.INCREASE_ENGINES_POWER;
import static elite.intel.ai.brain.actions.Commands.INCREASE_SHIELDS_POWER;
import static elite.intel.ai.brain.actions.Commands.INCREASE_WEAPONS_POWER;
import static elite.intel.ai.brain.actions.Commands.INTERRUPT_TTS;
import static elite.intel.ai.brain.actions.Commands.RESET_POWER;
import static elite.intel.ai.brain.actions.Commands.SET_SPEED_ZERO;
import static elite.intel.ai.brain.actions.Commands.SHOW_INVENTORY_PANEL;
import static elite.intel.ai.brain.actions.Commands.TARGET_DESTINATION;
import static elite.intel.ai.brain.actions.Commands.TOGGLE_ALL_ANNOUNCEMENTS;
import static elite.intel.ai.brain.actions.Queries.ANALYZE_MARKETS;
import static elite.intel.ai.brain.actions.Queries.BIO_SAMPLE_IN_STAR_SYSTEM;
import static elite.intel.ai.brain.actions.Queries.EXOBIOLOGY_SAMPLES_ON_THIS_PLANET;
import static elite.intel.ai.brain.actions.Queries.FSD_TARGET_ANALYSIS;
import static elite.intel.ai.brain.actions.Queries.MATERIALS_INVENTORY;
import static elite.intel.ai.brain.actions.Queries.PLOTTED_ROUTE_ANALYSIS;
import static elite.intel.ai.brain.actions.Queries.QUERY_GEO_SIGNALS;
import static elite.intel.ai.brain.actions.Queries.QUERY_STATIONS;

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

        sb.append("- require very high probability match for action → ");
        sb.append(CLEAR_ALL_ACTIVE_MISSIONS.getAction());
        sb.append("\n");
        sb.append("- French lexical intent split: INFO/RESEARCH words are \"trouve\", \"cherche\", \"rapport\", \"statut\", \"information\", \"détails\", \"compte rendu\", \"propose\", \"étudie\". Treat them as query/research unless an explicit game-action verb is also present.\n");
        sb.append("- French ACTION words are imperative verbs such as \"ouvre\", \"active\", \"désactive\", \"navigue\", \"trace\", \"cible\", \"sélectionne\", \"verrouille\", \"établis\", \"fais\", \"définis\", \"règle\". These can trigger bindings, panels, navigation, route entry, or app state changes.\n");
        sb.append("- The noun \"itinéraire\" is neutral. With INFO words like \"rapport\", \"statut\", \"combien\", \"où\", it is a query. With ACTION verbs like \"trace\", \"navigue\", \"établis\", \"définis\", it is an action.\n");
        sb.append("- Fleet carrier terms: \"porte-vaisseau\" or bare English \"fleet carrier/carrier\" means the player's Drake-Class fleet carrier by default. \"porte-vaisseau d'escadron\", \"carrier d'escadron\", \"squadron carrier\", or any phrase containing \"escadron\" means the squadron/Javelin carrier.\n");
        sb.append("- French exact standalone \"stop\" means stop the ship, not voice interruption → ");
        sb.append(SET_SPEED_ZERO.getAction());
        sb.append("; \"stop voix\", \"silence\", \"arrête de parler\" mean → ");
        sb.append(INTERRUPT_TTS.getAction());
        sb.append("\n");
        sb.append("- \"équilibre le distributeur\" / \"réinitialise la puissance\" mean power distribution reset → ");
        sb.append(RESET_POWER.getAction());
        sb.append("\n");
        sb.append("- French elliptical power commands: \"puissance dans/aux moteurs\" → ");
        sb.append(INCREASE_ENGINES_POWER.getAction());
        sb.append("; \"puissance dans/aux armes\" → ");
        sb.append(INCREASE_WEAPONS_POWER.getAction());
        sb.append("; \"puissance dans/aux boucliers/systèmes\" → ");
        sb.append(INCREASE_SHIELDS_POWER.getAction());
        sb.append("\n");
        sb.append("- \"porte-vaisseau(x) le plus proche\" with trouve/cherche/où est means nearest fleet carrier search → ");
        sb.append(FIND_NEAREST_FLEET_CARRIER.getAction());
        sb.append("\n");
        sb.append("- \"plus d'annonces\" means disable all announcements → ");
        sb.append(TOGGLE_ALL_ANNOUNCEMENTS.getAction());
        sb.append("\n");
        sb.append("- \"rapport sur notre itinéraire\", \"rapport d'itinéraire\", or remaining jumps without carrier means ship plotted route → ");
        sb.append(PLOTTED_ROUTE_ANALYSIS.getAction());
        sb.append("\n");
        sb.append("- \"rapport sur la prochaine étape de l'itinéraire\" is an information query about the current FSD target → ");
        sb.append(FSD_TARGET_ANALYSIS.getAction());
        sb.append("; only verbs like \"cible\", \"sélectionne\", \"verrouille\" make it an action → ");
        sb.append(TARGET_DESTINATION.getAction());
        sb.append("\n");
        sb.append("- \"où puis-je me poser ici\" asks for stations/ports in the current system → ");
        sb.append(QUERY_STATIONS.getAction());
        sb.append("\n");
        sb.append("- French biology scope: \"dans le système\", \"quelles planètes\", or \"progression biologique système\" means system-wide bio signals/scans → ");
        sb.append(BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
        sb.append("; \"ici\", \"sur cette planète\", \"à scanner ici\", or sampling still needed at current location means surface exobiology → ");
        sb.append(EXOBIOLOGY_SAMPLES_ON_THIS_PLANET.getAction());
        sb.append("\n");
        sb.append("- \"signaux géologiques\", \"sites géologiques\", or \"activité volcanique\" means geological signals → ");
        sb.append(QUERY_GEO_SIGNALS.getAction());
        sb.append("\n");
        sb.append("- \"inventaire des matériaux\" / \"liste des matériaux\" is an information query → ");
        sb.append(MATERIALS_INVENTORY.getAction());
        sb.append("; opening the UI requires \"montre/ouvre/affiche le panneau inventaire\" → ");
        sb.append(SHOW_INVENTORY_PANEL.getAction());
        sb.append("\n");
        sb.append("- \"stations avec commerce\", \"marchés locaux\", \"marché le plus proche\" without a named commodity means market list → ");
        sb.append(ANALYZE_MARKETS.getAction());
        sb.append("; generic landing/ports/stations means → ");
        sb.append(QUERY_STATIONS.getAction());
        sb.append("\n");
        return sb.toString();
    }
}
