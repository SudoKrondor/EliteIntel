package elite.intel.ai.brain.i18n.fr;

import elite.intel.ai.brain.i18n.PromptLanguageRules;

import static elite.intel.ai.brain.actions.Commands.ACTIVATE;
import static elite.intel.ai.brain.actions.Commands.CLEAR_ALL_ACTIVE_MISSIONS;
import static elite.intel.ai.brain.actions.Commands.FIND_NEAREST_FLEET_CARRIER;
import static elite.intel.ai.brain.actions.Commands.INCREASE_ENGINES_POWER;
import static elite.intel.ai.brain.actions.Commands.INCREASE_SHIELDS_POWER;
import static elite.intel.ai.brain.actions.Commands.INCREASE_WEAPONS_POWER;
import static elite.intel.ai.brain.actions.Commands.INTERRUPT_TTS;
import static elite.intel.ai.brain.actions.Commands.JUMP_TO_HYPERSPACE;
import static elite.intel.ai.brain.actions.Commands.RESET_POWER;
import static elite.intel.ai.brain.actions.Commands.SELECT_FIRE_GROUP_BY_NATO;
import static elite.intel.ai.brain.actions.Commands.SET_SPEED_ZERO;
import static elite.intel.ai.brain.actions.Commands.SHOW_INVENTORY_PANEL;
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

        sb.append("- require very high probability match for action → ");
        sb.append(CLEAR_ALL_ACTIVE_MISSIONS.getAction());
        sb.append("\n");
        sb.append("- French lexical intent split: INFO/RESEARCH words are \"trouve\", \"cherche\", \"rapport\", \"statut\", \"information\", \"détails\", \"compte rendu\", \"propose\", \"étudie\". Treat them as query/research unless an explicit game-action verb is also present.\n");
        sb.append("- French ACTION words are imperative verbs such as \"ouvre\", \"active\", \"désactive\", \"navigue\", \"trace\", \"cible\", \"sélectionne\", \"verrouille\", \"établis\", \"fais\", \"définis\", \"règle\". These can trigger bindings, panels, navigation, route entry, or app state changes.\n");
        sb.append("- Exact standalone \"active\" or \"active ça\" means generic UI activation → ");
        sb.append(ACTIVATE.getAction());
        sb.append(". When \"active\" is followed by a named system or function, select that specific action instead. \"active le pilotage automatique\", \"mode pilote automatique\", \"mode taxi\", or \"taxi\" mean → ");
        sb.append(TAXI.getAction());
        sb.append(". Never classify these phrases as generic UI activation.\n");
        sb.append("- \"active le réacteur FSD\", \"active le saut FSD\", \"lance le saut FSD\", or any instruction that activates/engages the FSD means → ");
        sb.append(JUMP_TO_HYPERSPACE.getAction());
        sb.append(". Only explicit targeting verbs such as \"cible\", \"vise\", or \"verrouille\" with FSD mean → ");
        sb.append(TARGET_SUB_SYSTEM.getAction());
        sb.append(". Never classify \"active le réacteur FSD\" as subsystem targeting.\n");
        sb.append("- The noun \"itinéraire\" is neutral. With INFO words like \"rapport\", \"statut\", \"combien\", \"où\", it is a query. With ACTION verbs like \"trace\", \"navigue\", \"établis\", \"définis\", it is an action.\n");
        sb.append("- Fleet carrier terms: \"porte-vaisseaux\" or bare English \"fleet carrier/carrier\" means the player's Drake-Class fleet carrier by default. \"porte-vaisseau d'escadron\", \"carrier d'escadron\", \"squadron carrier\", or any phrase containing \"escadron\" means the squadron/Javelin carrier.\n");
        sb.append("- A general \"rapport\", \"statut\", or \"état\" about the squadron carrier, without a route-specific noun, means full squadron-carrier status → ");
        sb.append(SQUADRON_CARRIER_STATUS.getAction());
        sb.append(". Squadron-carrier route analysis → ");
        sb.append(SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction());
        sb.append(" requires an explicit navigation noun such as \"route\", \"itinéraire\", \"sauts\", or \"trajet\". The word \"rapport\" alone never implies a route.\n");
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
        sb.append("; \"puissance dans/aux boucliers\" or \"puissance dans/aux systèmes\" both refer to the ship's SYS distributor, which powers the shields, and mean → ");
        sb.append(INCREASE_SHIELDS_POWER.getAction());
        sb.append("\n");
        sb.append("- French \"système/systèmes\" is context-sensitive. With ship-energy words such as \"puissance\", \"énergie\", \"distributeur\", \"pip\", \"priorité\", \"boost\", or \"SYS\", it means the ship's onboard SYS power channel, not a star system.\n");
        sb.append("- With navigation/astronomy words such as \"navigation\", \"route\", \"itinéraire\", \"carte\", \"FSD\", \"destination\", \"scan\", \"analyseur\", \"station\", \"corps\", \"planète\", \"sécurité\", \"position\", \"local\", \"actuel\", or \"ciblé\", French \"système\" means a stellar/star system. Never route those phrases to power distribution.\n");
        sb.append("- \"système de survie\" is a ship subsystem. With targeting verbs such as \"cible\", \"vise\", or \"verrouille\", route it to ");
        sb.append(TARGET_SUB_SYSTEM.getAction());
        sb.append(", never to SYS power distribution or a stellar-system query.\n");
        sb.append("- \"porte-vaisseau(x) le plus proche\" with trouve/cherche/où est means nearest fleet carrier search → ");
        sb.append(FIND_NEAREST_FLEET_CARRIER.getAction());
        sb.append("\n");
        sb.append("- \"plus d'annonces\" means disable all announcements → ");
        sb.append(TOGGLE_ALL_ANNOUNCEMENTS.getAction());
        sb.append("\n");
        sb.append("- \"rapport sur notre itinéraire\", \"rapport d'itinéraire\", or remaining jumps without carrier means ship plotted route → ");
        sb.append(PLOTTED_ROUTE_ANALYSIS.getAction());
        sb.append("\n");
        sb.append("- HARD RULE: imperative \"cible\", \"sélectionne\", or \"verrouille\" with \"destination\", \"prochaine étape\", or route/itinerary context always selects the destination of the next plotted FSD jump → ");
        sb.append(TARGET_DESTINATION.getAction());
        sb.append(". Never route these imperative phrases to ");
        sb.append(FSD_TARGET_ANALYSIS.getAction());
        sb.append(". Standalone \"cible\" has no action and must not be guessed.\n");
        sb.append("- HARD EXCLUSION: ");
        sb.append(SELECT_FIRE_GROUP_BY_NATO.getAction());
        sb.append(" is valid only when the input explicitly contains \"groupe de tir\", \"groupe d'armes\", or English \"fire group\", together with a NATO group identifier. The verbs \"cible\", \"sélectionne\", or \"verrouille\" alone never imply a fire group. Any phrase containing navigation words \"destination\", \"itinéraire\", \"route\", \"étape\", or \"FSD jump\" must never use the fire-group action.\n");
        sb.append("- \"rapport sur la prochaine étape de l'itinéraire\", \"informations sur la prochaine destination\", \"quelle est la cible FSD\", or \"quel système est ciblé\" is an information query about the already-selected next FSD jump → ");
        sb.append(FSD_TARGET_ANALYSIS.getAction());
        sb.append(". These query phrases must not change the selected destination.\n");
        sb.append("- French combat targeting requires a specific subject/complement: \"cible l'ailier un/deux/trois\" selects that wingman; \"cible les moteurs/le FSD/le système de survie\" targets a ship subsystem; \"chasseur attaque ma cible\" or an explicit fighter-focus order controls the fighter. None of these mean ");
        sb.append(TARGET_DESTINATION.getAction());
        sb.append(". Conversely, navigation \"destination/prochaine étape\" is never a fighter, wingman, or subsystem target.\n");
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
        sb.append("- HARD RULE: if French input contains the noun \"trappe\", with or without \"cargo\" or \"soute\", the only valid action is → ");
        sb.append(TOGGLE_CARGO_SCOOP.getAction());
        sb.append(". Never route an input containing \"trappe\" to an inventory panel or cargo-content query.\n");
        sb.append("- ACTION verbs \"ouvre\", \"montre\", or \"affiche\" with \"état du vaisseau\", \"état général du vaisseau\", \"statut du vaisseau\", or \"panneau de statut\" mean → ");
        sb.append(SHOW_STATUS_PANEL.getAction());
        sb.append(". This is a UI-panel command, never ");
        sb.append(SHIP_LOADOUT.getAction());
        sb.append(". Ship loadout queries require information wording about modules, equipment, damage, specifications, or combat readiness.\n");
        sb.append("- ACTION + \"panneau de cargaison\", \"panneau d'affichage du cargo\", \"inventaire des marchandises du vaisseau\", or \"inventaire du vaisseau\" means → ");
        sb.append(SHOW_INVENTORY_PANEL.getAction());
        sb.append(". ACTION + \"trappe de cargaison\", \"trappe du cargo\", \"trappe de la soute\", or \"récupérateur de cargaison\" means → ");
        sb.append(TOGGLE_CARGO_SCOOP.getAction());
        sb.append(". Bare \"ouvre le cargo\" is ambiguous and must not be used as an alias. The words \"trappe\" or \"récupérateur\" always identify the scoop action; the words \"panneau\", \"affichage\", or \"inventaire\" always identify the UI action.\n");
        sb.append("- Ship \"cargaison\" / \"soute\" contains transported commodities (\"marchandises\"). A request for the full cargo list means → ");
        sb.append(CARGO_HOLD_CONTENTS.getAction());
        sb.append(". A request for one explicitly named commodity in the hold means → ");
        sb.append(MATERIALS_INVENTORY.getAction());
        sb.append(" with key set to that commodity. Never treat a commodity in the cargo hold as an engineering material.\n");
        sb.append("- Ship \"matériaux d'ingénierie\" covers raw, manufactured, and encoded materials. French \"matériaux bruts\", \"matériaux manufacturés\", \"matériaux encodés\", and \"données encodées\" mean → ");
        sb.append(MATERIALS_INVENTORY.getAction());
        sb.append(". These materials are stored separately from cargo commodities and are not measured in tonnes.\n");
        sb.append("- \"inventaire des matériaux d'ingénierie\" / \"liste des matériaux d'ingénierie\" is an information query → ");
        sb.append(MATERIALS_INVENTORY.getAction());
        sb.append("; opening the ship UI requires \"montre/ouvre/affiche le panneau inventaire du vaisseau\" → ");
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
