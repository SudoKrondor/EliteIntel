package elite.intel.ai.brain.i18n.fr;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;
import elite.intel.session.Status;

import java.util.Map;
import java.util.Set;

import static elite.intel.ai.brain.actions.Commands.*;
import static elite.intel.ai.brain.actions.Queries.*;

public class FrenchAiActionAliases implements AiActionAliasProvider {

    @Override
    public Set<String> wakeBypassPhrases() {
        return Set.of("réveille-toi", "réveille toi", "reveille-toi", "reveille toi", "écoute", "ecoute", "écoute-moi", "écoute moi", "ecoute-moi", "ecoute moi");
    }

    @Override
    public Set<String> listenBypassPrefixes() {
        return Set.of("écoute-moi", "écoute moi", "ecoute-moi", "ecoute moi", "écoute", "ecoute");
    }

    @Override
    public void addAliases(Map<String, String> map, Status status, boolean isDryRun) {

        // always available
        map.put("réveille-toi, réveille toi, reveille-toi, reveille toi, écoute, ecoute, écoute-moi, écoute moi, ecoute-moi, ecoute moi", WAKEUP.getAction());
        map.put("dors, va dormir, endors-toi, ignore-moi, ignore moi, ne m'écoute plus, ne m'ecoute plus, ne surveille pas", SLEEP.getAction());
        map.put("interromps, interruption, arrête de parler, arrete de parler, coupe la voix, stop voix, silence", INTERRUPT_TTS.getAction());

        // navigation
        map.put("annule la route commerciale, arrête la route commerciale, arrete la route commerciale, supprime la route commerciale, abandonne la route commerciale, annule l'itinéraire commercial, annule l'itineraire commercial", CANCEL_TRADE_ROUTE.getAction());
        map.put("navigue vers les coordonnées {lat:X, lon:Y}, cap sur les coordonnées {lat:X, lon:Y}, va aux coordonnées {lat:X, lon:Y}", NAVIGATE_TO_TARGET.getAction());
        map.put("navigue vers la mission active, trace une route vers la mission active, trace une route vers la mission, amène-moi à la mission, amene-moi a la mission, va à la mission {key:X}, va a la mission {key:X}", NAVIGATE_TO_NEXT_MISSION.getAction());
        map.put("navigue vers le porte-vaisseaux, va au porte-vaisseaux, va au carrier, cap sur le carrier, retourne au carrier, retourne au porte-vaisseaux, ramène-nous au carrier, ramene-nous au carrier", NAVIGATE_TO_FLEET_CARRIER.getAction());
        map.put("navigue vers la zone d'atterrissage, cap vers la zone d'atterrissage, direction zone d'atterrissage, retour à la lz, retour a la lz, retour LZ", GET_HEADING_TO_LZ.getAction());
        map.put("navigue vers le prochain arrêt commercial, navigue vers le prochain arret commercial, va au prochain arrêt commercial, va au prochain arret commercial", NAVIGATE_TO_NEXT_TRADE_STOP.getAction());
        map.put("navigue depuis la mémoire, navigue depuis la memoire, colle depuis la mémoire, colle depuis la memoire, adresse depuis la mémoire, adresse depuis la memoire", NAVIGATE_TO_ADDRESS_FROM_MEMORY.getAction());
        map.put("annule la navigation, abandonne la navigation, arrête la navigation, arrete la navigation, annule la route, stop navigation", NAVIGATION_OFF.getAction());
        map.put("définis le système de base, definis le systeme de base, définis le système actuel comme base, definis le systeme actuel comme base, marque le système de base, marque le systeme de base, définis le système d'origine, definis le systeme d'origine", SET_HOME_SYSTEM.getAction());
        map.put("ramène-moi à la base, ramene-moi a la base, retour à la base, retour a la base, rentre à la base, rentre a la base, navigue vers la base, trace une route vers la base, rentrons à la maison, rentrons a la maison", TAKE_ME_HOME.getAction());
        map.put("réinitialise la vue tête, reinitialise la vue tete, réinitialise la vue tête par défaut, reinitialise la vue tete par defaut, réinitialise la vue tête au neutre, reinitialise la vue tete au neutre, reset head look, vue tête par défaut, vue tete par defaut", RESET_HEAD_LOOK.getAction());

        if (status.isInMainShip() || isDryRun) {
            // navigation
            map.put("sélectionne la destination fsd, selectionne la destination fsd, cible la destination, définis la destination, definis la destination, sélectionne la destination cible, selectionne la destination cible", TARGET_DESTINATION.getAction());
            map.put("saute en hyperespace, saut hyperespace, saut hyperspatial, jump, saut FSD, entre en hyperespace, allons-y, allons y, prochain waypoint, prochain point de route", JUMP_TO_HYPERSPACE.getAction());
            map.put("sors de supercruise, sors de la supercroisière, sors de la supercroisiere, quitte supercruise, quitte la supercroisière, quitte la supercroisiere, drop, drop maintenant, sortie FSD", DROP_FROM_SUPER_CRUISE.getAction());
            map.put("entre en supercruise, passe en supercruise, supercruise, active supercruise, entre en supercroisière, entre en supercroisiere, active la supercroisière, active la supercroisiere", ENTER_SUPER_CRUISE.getAction());
            map.put("lance le vaisseau, décollage, decollage, décolle, decolle, détache-toi de la station, detache-toi de la station, quitte le port, quitte la station", LAUNCH_SHIP.getAction());
            // speed / throttle
            map.put("arrête les moteurs, arrete les moteurs, stop moteurs, stop, arrêt complet, arret complet, halte, coupe les moteurs, coupe les gaz, zéro gaz, zero gaz, stop vaisseau", SET_SPEED_ZERO.getAction());
            map.put("taxi vers l'atterrissage, taxi atterrissage, taxi, atterrissage automatique, autopilote atterrissage, autopilote d'atterrissage", TAXI.getAction());
            map.put("quart de poussée, quart de poussee, vingt-cinq pour cent, 25 pour cent, vitesse lente, un quart", SET_SPEED25.getAction());
            map.put("demi poussée, demi poussee, cinquante pour cent, 50 pour cent, demi-vitesse, mi-vitesse", SET_SPEED50.getAction());
            map.put("trois quarts de poussée, trois quarts de poussee, soixante-quinze pour cent, 75 pour cent, trois quarts vitesse", SET_SPEED75.getAction());
            map.put("pleine poussée, pleine poussee, cent pour cent, 100 pour cent, vitesse maximale, plein gaz, poussée maximale, poussee maximale", SET_SPEED100.getAction());
            map.put("augmente la vitesse de {key:X}, accélère de {key:X}, accelere de {key:X}, plus vite de {key:X}", INCREASE_SPEED_BY.getAction());
            map.put("réduis la vitesse de {key:X}, reduis la vitesse de {key:X}, ralentis de {key:X}, diminue la vitesse de {key:X}", DECREASE_SPEED_BY.getAction());
            map.put("règle la vitesse optimale, regle la vitesse optimale, vitesse optimale, vitesse d'approche optimale, optimise la vitesse d'approche", SET_OPTIMAL_SPEED.getAction());
            map.put("train d'atterrissage, sors le train, descends le train, déploie le train d'atterrissage, deploie le train d'atterrissage, train sorti", DEPLOY_LANDING_GEAR.getAction());
            map.put("rentre le train d'atterrissage, remonte le train, relève le train, releve le train, train rentré, train rentre", RETRACT_LANDING_GEAR.getAction());
            map.put("demande l'autorisation d'appontage, autorisation d'appontage, demande l'autorisation d'amarrage, demande docking, demande l'atterrissage, demande une place, demande un pad, gare le vaisseau", REQUEST_DOCKING.getAction());
            // UI panels
            map.put("montre le panneau chasseur, ouvre le panneau chasseur, affiche le panneau chasseur, panneau chasseur, panneau fighter", SHOW_FIGHTER_PANEL.getAction());
            map.put("utilise une cellule de bouclier, active une cellule de bouclier, cellule de bouclier, shield cell, active shield cell, déploie une cellule de bouclier, deploie une cellule de bouclier", DEPLOY_SHIELD_CELL.getAction());
            map.put("lance les paillettes, déploie les paillettes, deploie les paillettes, utilise les paillettes, lance chaff, déploie chaff, deploie chaff, utilise chaff, lance les leurres, déploie les leurres, deploie les leurres", DEPLOY_CHAFF.getAction());
            // combat
            map.put("déploie les points d'emport, deploie les points d'emport, hardpoints, déploie hardpoints, deploie hardpoints, armes chaudes, armes prêtes, armes pretes, prépare les armes, prepare les armes, armes sorties", DEPLOY_HARDPOINTS.getAction());
            map.put("rentre les points d'emport, rentre hardpoints, armes froides, range les armes, armes rangées, armes rangees, sécurise les armes, securise les armes, armes rentrées, armes rentrees", RETRACT_HARDPOINTS.getAction());
            map.put("cible fsd {key:fsd}, cible moteurs {key:drive}, cible les moteurs {key:drive}, cible distributeur d'énergie {key:power distributor}, cible distributeur d'energie {key:power distributor}, cible centrale électrique {key:powerplant}, cible centrale electrique {key:powerplant}, cible power plant {key:powerplant}, cible système de survie {key:life support}, cible systeme de survie {key:life support}", TARGET_SUB_SYSTEM.getAction());
            map.put("cible ailier un, cible ailier 1, ailier alpha", TARGET_WINGMAN0.getAction());
            map.put("cible ailier deux, cible ailier 2, ailier bravo", TARGET_WINGMAN1.getAction());
            map.put("cible ailier trois, cible ailier 3, ailier charlie", TARGET_WINGMAN2.getAction());
            map.put("verrouillage navigation escadre, verrouillage nav escadre, lock navigation escadre, verrouille la navigation sur l'ailier, suis l'ailier, suis le wingman", WING_NAV_LOCK.getAction());
            map.put("cible prioritaire, cible la menace principale, cible la plus dangereuse, sélectionne l'hostile, selectionne l'hostile, prochain ennemi, sélectionne l'ennemi, selectionne l'ennemi", SELECT_HIGHEST_THREAT.getAction());
            // vehicle deployment
            map.put("déploie SRV, deploie SRV, lance SRV, déploie le véhicule, deploie le vehicule, lance le véhicule, lance le vehicule, sors le SRV", DEPLOY_SRV.getAction());
            map.put("lance dissipateur thermique, déploie dissipateur thermique, deploie dissipateur thermique, heat sink, lance heat sink, évacue la chaleur, evacue la chaleur", DEPLOY_HEAT_SINK.getAction());
            // fighter orders
            map.put("déploie chasseur, deploie chasseur, lance chasseur, envoie chasseur, sors le chasseur", DEPLOY_FIGHTER.getAction());
            map.put("chasseur défends le vaisseau, chasseur defends le vaisseau, chasseur en défense, chasseur en defense, chasseur défensif, chasseur defensif", FIGHTER_REQUEST_DEFENSIVE_BEHAVIOUR.getAction());
            map.put("chasseur attaque ma cible, chasseur attaque, chasseur cible ma cible, concentre-toi sur ma cible, concentre toi sur ma cible, focus ma cible, focus target, focus sur la cible", FIGHTER_REQUEST_FOCUS_TARGET.getAction());
            map.put("chasseur cesse le feu, chasseur retiens ton feu, chasseur ne tire pas, cessez-le-feu chasseur, hold fire chasseur", FIGHTER_REQUEST_HOLD_FIRE.getAction());
            map.put("chasseur rentre au vaisseau, chasseur dock, rappelle le chasseur, retour chasseur, chasseur reviens au vaisseau", FIGHTER_REQUEST_REQUEST_DOCK.getAction());
            map.put("chasseur feu à volonté, chasseur feu a volonte, feu à volonté, feu a volonte, attaque à volonté, attaque a volonte, fire at will", FIGHTER_OPEN_ORDERS.getAction());
            map.put("sélectionne le groupe de tir {key:X}, selectionne le groupe de tir {key:X}, groupe de tir {key:X}, bascule vers le groupe de tir {key:X}, basculez vers le groupe de tir {key:X}", SELECT_FIRE_GROUP_BY_NATO.getAction());
            map.put("calculer la route des étoiles à neutrons {efficiency:X}", CALCULATE_NEUTRON_STAR_ROUTE.getAction());
            map.put("prochain saut d'étoile à neutrons, tracer l'itinéraire vers le prochain point de cheminement de l'étoile à neutrons, prochaine étoile à neutrons", PLOT_ROUTE_TO_NEXT_NEUTRON_STAR.getAction());
            map.put("effacer/supprimer la route des étoiles à neutrons", CLEAR_NEUTRON_ROUTE.getAction());
        }

        if (status.isInMainShip() && !status.isDocked() || isDryRun) {
            map.put("stations dans le système, stations dans le systeme, quelles stations, stations proches, ports spatiaux, stations spatiales, amarrage disponible, appontage disponible", QUERY_STATIONS.getAction());
        }

        if (status.isInSrv() && status.isDocked() || isDryRun) {
            map.put("montre le panneau services, ouvre le panneau services, affiche le panneau services, ouvre services station, affiche services station, panneau services", SHOW_STATION_SERVICES.getAction());
        }

        if (status.isInMainShip() || status.isInSrv() || isDryRun) {
            // flight / ship systems
            map.put("passe en mode combat, active le mode combat, mode combat", ACTIVATE_COMBAT_MODE.getAction());
            map.put("passe en mode analyse, active le mode analyse, mode analyse", ACTIVATE_ANALYSIS_MODE.getAction());
            map.put("cargo scoop, ouvre cargo scoop, ferme cargo scoop, déploie cargo scoop, deploie cargo scoop, rentre cargo scoop, écoutille cargo, ecoutille cargo, ouvre l'écoutille cargo, ouvre l'ecoutille cargo, ferme l'écoutille cargo, ferme l'ecoutille cargo, trappe cargo, soute cargo", TOGGLE_CARGO_SCOOP.getAction());
            map.put("vision nocturne, allume vision nocturne, éteins vision nocturne, eteins vision nocturne, active vision nocturne, désactive vision nocturne, desactive vision nocturne {state:true/false}", NIGHT_VISION_ON_OFF.getAction());
            map.put("phares, lumières, lumieres, allume les lumières, allume les lumieres, éteins les lumières, eteins les lumieres, feux du vaisseau, lumières allumées, lumieres allumees, lumières éteintes, lumieres eteintes {state:true/false}", LIGHTS_ON_OFF.getAction());
            // UI panels
            map.put("montre le panneau commandant, ouvre le panneau commandant, affiche le panneau commandant, panneau central, panneau rôle, panneau role, panneau commander, ouvre la genouillère, ouvre la genouillere", SHOW_COMMANDER_PANEL.getAction());
            map.put("montre le panneau équipage, montre le panneau equipage, ouvre le panneau équipage, ouvre le panneau equipage, affiche équipage, affiche equipage, panneau équipage, panneau equipage", SHOW_CREW.getAction());
            map.put("montre le panneau accueil, ouvre le panneau accueil, affiche le panneau accueil, panneau interne, panneau home", SHOW_INTERNAL_PANEL.getAction());
            map.put("montre le panneau modules, ouvre le panneau modules, affiche modules, panneau modules", SHOW_MODULES_PANEL.getAction());
            map.put("montre les groupes de tir, ouvre les groupes de tir, affiche les groupes de tir, fire groups, groupes d'armes", SHOW_FIRE_GROUPS.getAction());
            map.put("montre l'inventaire, ouvre l'inventaire, affiche l'inventaire, panneau inventaire", SHOW_INVENTORY_PANEL.getAction());
            map.put("montre le stockage, ouvre le stockage, affiche le stockage, panneau stockage", SHOW_STORAGE_PANEL.getAction());
            // power
            map.put("puissance aux boucliers, maximum boucliers, boost boucliers, plus de puissance aux boucliers", INCREASE_SHIELDS_POWER.getAction());
            map.put("puissance aux moteurs, maximum moteurs, boost moteurs, plus de puissance aux moteurs", INCREASE_ENGINES_POWER.getAction());
            map.put("puissance aux armes, maximum armes, boost armes, plus de puissance aux armes", INCREASE_WEAPONS_POWER.getAction());
            // vehicle deployment
            map.put("débarque, debarque, quitte le vaisseau, sors du vaisseau, descends du vaisseau, sortir à pied, sortir a pied", DISEMBARK.getAction());
            map.put("équilibre la puissance, equilibre la puissance, balance la puissance, reset puissance, réinitialise la puissance, reinitialise la puissance, distribue la puissance également, distribue la puissance egalement", RESET_POWER.getAction());
        }

        if (status.isInSrv() || isDryRun) {
            map.put("assistance de conduite, aide à la conduite, aide a la conduite, drive assist, assistance SRV, assistance srv {state:true/false}", DRIVE_ASSIST.getAction());
            map.put("récupère SRV, recupere SRV, récupère le SRV, recupere le SRV, remonte à bord, remonte a bord, rentre le SRV, dock SRV, ramène le SRV, ramene le SRV", RECOVER_SRV.getAction());
        }

        if (status.isInSrv() || status.isOnFoot() || isDryRun) {
            map.put("renvoie le vaisseau, envoie le vaisseau en orbite, renvoie le ship, ship en orbite", DISMISS_SHIP.getAction());
            map.put("retour à la surface, retour a la surface, viens me chercher, récupère-moi, recupere-moi, récupère moi, recupere moi, rappelle le vaisseau", RETURN_TO_SURFACE.getAction());
        }

        // market / traders / brokers
        map.put("trouve marchand de matériaux bruts, trouve marchand de materiaux bruts, marchand matériaux bruts, marchand materiaux bruts, raw material trader, raw trader, où échanger les matériaux bruts {key:X}, ou echanger les materiaux bruts {key:X}", FIND_RAW_MATERIAL_TRADER.getAction());
        map.put("trouve marchand de données codées, trouve marchand de donnees codees, marchand données, marchand donnees, encoded material trader, encoded trader, data trader {key:X}", FIND_ENCODED_MATERIAL_TRADER.getAction());
        map.put("trouve marchand de matériaux manufacturés, trouve marchand de materiaux manufactures, marchand matériaux manufacturés, marchand materiaux manufactures, manufactured material trader, manufactured trader {key:X}", FIND_MANUFACTURED_MATERIAL_TRADER.getAction());
        map.put("trouve courtier de technologies humaines, courtier technologie humaine, human tech broker, human technology broker {key:X}", FIND_HUMAN_TECHNOLOGY_BROKER.getAction());
        map.put("trouve courtier de technologies gardiennes, courtier technologie gardienne, courtier guardian, guardian tech broker, guardian technology broker {key:X}", FIND_GUARDIAN_TECHNOLOGY_BROKER.getAction());
        map.put("trouve marchandise, trouve la marchandise la plus proche, achète marchandise, achete marchandise, où acheter, ou acheter, trouve marché, trouve marche, find market {key:X, max_distance:Y, state:true/false}", FIND_COMMODITY.getAction());
        map.put("trouve le porte-vaisseaux le plus proche, porte-vaisseaux le plus proche, carrier le plus proche, fleet carrier le plus proche", FIND_NEAREST_FLEET_CARRIER.getAction());

        // fleet carrier
        map.put("définis réserve carburant carrier, definis reserve carburant carrier, réserve carburant carrier, reserve carburant carrier, réserve tritium carrier, reserve tritium carrier {key:X}", SET_CARRIER_FUEL_RESERVE.getAction());
        map.put("calcule route carrier, calcule route fleet carrier, planifie route carrier, planifie route fleet carrier, route de saut carrier", CALCULATE_FLEET_CARRIER_ROUTE.getAction());
        map.put("entre destination carrier, définis destination carrier, definis destination carrier, destination carrier, destination porte-vaisseaux", ENTER_FLEET_CARRIER_DESTINATION.getAction());

        // squadron carrier
        map.put("route escadron carrier, navigation escadron carrier, route de saut escadron carrier, combien de sauts sur la route escadron carrier, sauts restants escadron carrier", SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put("où va l'escadron carrier, ou va l'escadron carrier, cap escadron carrier, destination finale escadron carrier, destination escadron carrier", SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION.getAction());
        map.put("tritium escadron carrier, carburant escadron carrier, réserve tritium escadron, reserve tritium escadron, niveau tritium escadron carrier, statut tritium escadron carrier", SQUADRON_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put("statut escadron carrier, finances escadron carrier, solde escadron carrier, aperçu escadron carrier, apercu escadron carrier, fonds escadron carrier, combien de temps peut fonctionner l'escadron carrier, statut carburant escadron carrier", SQUADRON_CARRIER_STATUS.getAction());
        map.put("ETA escadron carrier, eta escadron carrier, quand arrive l'escadron carrier, combien de temps avant l'escadron carrier, arrivée escadron carrier, arrivee escadron carrier, heure d'arrivée escadron carrier, heure d'arrivee escadron carrier", SQUADRON_CARRIER_ETA.getAction());

        // trade
        map.put("calcule route commerciale, calcule trade route, calcule itinéraire commercial, calcule itineraire commercial", CALCULATE_TRADE_ROUTE.getAction());
        map.put("liste paramètres route commerciale, liste parametres route commerciale, affiche paramètres trade route, affiche parametres trade route, paramètres commerciaux, parametres commerciaux", LIST_TRADE_ROUTE_PARAMETERS.getAction());
        map.put("calcule profit route, calcule le profit de la route, optimise profit route, monétise la route, monetise la route", MONETIZE_ROUTE.getAction());

        map.put("change profil commercial budget de départ {key:X}, change profil commercial budget de depart {key:X}, budget de départ profil commercial {key:X}, budget de depart profil commercial {key:X}", CHANGE_TRADE_PROFILE_SET_STARTING_BUDGET.getAction());
        map.put("change profil commercial nombre maximum d'arrêts {key:X}, change profil commercial nombre maximum d'arrets {key:X}, maximum arrêts profil commercial {key:X}, maximum arrets profil commercial {key:X}", CHANGE_TRADE_PROFILE_SET_MAX_NUMBER_OF_STOPS.getAction());
        map.put("change profil commercial distance maximum {key:X}, distance maximum profil commercial {key:X}, distance maximale depuis l'entrée {key:X}, distance maximale depuis l'entree {key:X}", CHANGE_TRADE_PROFILE_SET_MAX_DISTANCE_FROM_ENTRY.getAction());
        map.put("profil commercial autorise marchandises interdites, autorise marchandises interdites profil commercial, cargo interdit autorisé, cargo interdit autorise {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PROHIBITED_CARGO.getAction());
        map.put("profil commercial autorise ports planétaires, profil commercial autorise ports planetaires, autorise ports planétaires profil commercial, autorise ports planetaires profil commercial {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PLANETARY_PORT.getAction());
        map.put("profil commercial autorise systèmes à permis, profil commercial autorise systemes a permis, autorise systèmes à permis profil commercial, autorise systemes a permis profil commercial {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PERMIT_SYSTEMS.getAction());
        map.put("profil commercial autorise bastions, profil commercial autorise strongholds, autorise bastions profil commercial, autorise strongholds profil commercial {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_STRONGHOLDS.getAction());

        // announcements / app settings
        map.put("radio, trafic radio, transmissions radio, active radio, désactive radio, desactive radio {state:true/false}", SET_RADIO_TRANSMISSION_MODE.getAction());
        map.put("annonce contact radar, annonces contact radar, annonces contacts radar, radar contact {state:true/false}", SET_RADAR_CONTACT_ANNOUNCEMENT.getAction());
        map.put("annonces de découverte, annonces de decouverte, annonces découverte, annonces decouverte {state:true/false}", DISCOVERY_ON_OFF.getAction());
        map.put("annonces de route, annonces d'itinéraire, annonces d'itineraire, annonces route {state:true/false}", ROUTE_ON_OFF.getAction());
        map.put("basculer toutes les annonces {state:true/false}, toutes les annonces {state:true/false}", TOGGLE_ALL_ANNOUNCEMENTS.getAction());
        map.put("efface les rappels, supprime les rappels, clear reminders", CLEAR_REMINDERS.getAction());
        map.put("définis rappel {key:X}, definis rappel {key:X}, crée rappel {key:X}, cree rappel {key:X}, rappel {key:X}", SET_REMINDER.getAction());
        map.put("rappelle-moi dans {minutes:X} minutes {key:Y}, rappelle moi dans {minutes:X} minutes {key:Y}, minuterie {minutes:X} minutes {key:Y}, rappel minuté {minutes:X} minutes {key:Y}", SET_TIMED_REMINDER.getAction());
        map.put("effacer les missions actives, effacer toutes les missions actives", CLEAR_ALL_ACTIVE_MISSIONS.getAction());

        // UI panels
        map.put("active, activer, active ça, active ca", ACTIVATE.getAction());
        map.put("montre transactions, ouvre transactions, affiche transactions, panneau transactions", SHOW_TRANSACTIONS.getAction());
        map.put("montre contacts, ouvre contacts, affiche contacts, panneau contacts", SHOW_CONTACTS.getAction());
        map.put("montre navigation, ouvre navigation, affiche navigation, panneau navigation", SHOW_NAVIGATION.getAction());
        map.put("montre chat, ouvre chat, affiche chat, ouvre comms, panneau comms, communications", SHOW_CHAT_PANEL.getAction());
        map.put("montre boîte de réception, montre boite de reception, ouvre boîte de réception, ouvre boite de reception, ouvre inbox, email, messages", SHOW_INBOX_PANEL.getAction());
        map.put("montre social, ouvre social, affiche social, panneau social", SHOW_SOCIAL_PANEL.getAction());
        map.put("montre historique, ouvre historique, affiche historique, panneau historique", SHOW_HISTORY_PANEL.getAction());
        map.put("montre escadron, ouvre escadron, affiche escadron, squadron, panneau escadron", SHOW_SQUADRON.getAction());
        map.put("montre statut, ouvre statut, affiche statut, panneau statut", SHOW_STATUS_PANEL.getAction());
        map.put("montre gestion carrier, ouvre gestion carrier, affiche gestion carrier, gestion porte-vaisseaux, carrier management", DISPLAY_CARRIER_MANAGEMENT.getAction());
        map.put("ouvre carte galactique, affiche carte galactique, galaxy map, carte galaxie", OPEN_GALAXY_MAP.getAction());
        map.put("ouvre carte du système, ouvre carte du systeme, affiche carte du système, affiche carte du systeme, carte système, carte systeme, carte locale, system map", OPEN_SYSTEM_MAP.getAction());
        map.put("ferme panneau, ferme, quitte, quitte le panneau, close panel", EXIT_CLOSE.getAction());

        // pirate massacre missions
        map.put("navigue vers système fournisseur de missions, navigue vers systeme fournisseur de missions, système fournisseur de missions, systeme fournisseur de missions, système donneur de missions, systeme donneur de missions", RECON_PROVIDER_SYSTEM.getAction());
        map.put("navigue vers fournisseur de missions pirates, va au fournisseur de missions pirates, fournisseur missions pirates", NAVIGATE_TO_PIRATE_MISSION_PROVIDER.getAction());
        map.put("missions actives, missions courantes, journal missions, statut missions, quelles missions, quelles sont nos missions, missions en cours, tableau des missions", ANALYZE_MISSIONS.getAction());
        map.put("mission pirate, compteur de kills, combien de kills, progression massacre, progression mission massacre, pirates restants, kills pirates, progression chasse à prime, progression chasse a prime", PIRATE_MISSION_PROGRESS.getAction());
        map.put("trouve terrain de chasse {key:X}, trouve terrains de chasse {key:X}, trouve zone de chasse {key:X}, trouve hunting grounds {key:X}", FIND_HUNTING_GROUNDS.getAction());
        map.put("recon terrain de chasse, reconnaissance terrain de chasse, navigue vers système cible, navigue vers systeme cible, navigue vers terrain de chasse", RECON_TARGET_SYSTEM.getAction());
        map.put("ignore terrain de chasse, ignore ce terrain de chasse", IGNORE_HUNTING_GROUND.getAction());
        map.put("confirme terrain de chasse, confirme le terrain de chasse, confirme système cible, confirme systeme cible", CONFIRM_HUNTING_GROUND.getAction());

        // science / mining / biology
        map.put("ajoute cible minage {key:X}, ajoute cible de minage {key:X}, ajoute objectif minage {key:X}", ADD_MINING_TARGET.getAction());
        map.put("retire cible minage {key:X}, retire cible de minage {key:X}, supprime cible minage {key:X}", REMOVE_MINING_TARGET.getAction());
        map.put("efface cibles minage, supprime cibles minage, vide cibles minage", CLEAR_MINING_TARGETS.getAction());
        map.put("annonces minage, annonces de minage, mining announcements {state:true/false}", MINING_ON_OFF.getAction());
        map.put("trouve arbres cérébraux {key:X, max_distance:Y}, trouve arbres cerebraux {key:X, max_distance:Y}, trouve brain trees {key:X, max_distance:Y}", FIND_BRAIN_TREES.getAction());
        map.put("trouve site de minage, trouve lieu de minage, trouve hotspot de minage, où miner, ou miner, trouve champ d'astéroïdes, trouve champ d'asteroides {key:X, max_distance:Y}", FIND_MINING_SITE.getAction());
        map.put("trouve site de minage tritium, trouve champ de tritium, trouve hotspot tritium, où miner du tritium, ou miner du tritium {key:X, max_distance:Y}", FIND_FLEET_CARRIER_FUEL_MINING_SITE.getAction());
        map.put("navigue vers prochain échantillon bio, navigue vers prochain echantillon bio, va au prochain échantillon, va au prochain echantillon, prochain organique, entrée codex, entree codex, navigue vers entrée codex, navigue vers entree codex, navigue codex", NAVIGATE_TO_NEXT_BIO_SAMPLE.getAction());
        map.put("scanne le système, scanne le systeme, ouvre FSS, ouvre fss, scan complet, honk, discovery scan, scanner de découverte, scanner de decouverte, full spectrum scan, analyse complète du système, analyse complete du systeme", OPEN_FSS.getAction());
        map.put("trouve vista genomics le plus proche, trouve genomics, vista genomics, trouve Vista Genomics", FIND_VISTA_GENOMICS.getAction());
        map.put("supprime entrée codex, supprime entree codex, supprime cette entrée codex, supprime cette entree codex, supprime cet organique", DELETE_CODEX_ENTRY.getAction());
        map.put("Trouve/cherche moi le interstellar factor le plus proche, trouve/cherche où je peux payer mes amandes ", FIND_INTERSTELLAR_FACTOR.getAction());

        map.put("vérifie raccourcis, verifie raccourcis, vérifie les touches, verifie les touches, touches manquantes, raccourcis manquants, affectations manquantes, bindings manquants, key bindings, missing bindings", KEY_BINDINGS_ANALYSIS.getAction());
        map.put("signaux bio traités dans le système, signaux bio traites dans le systeme, organiques scannés dans le système, organiques scannes dans le systeme, combien d'échantillons bio dans le système, combien d'echantillons bio dans le systeme, signaux biologiques dans le système, signaux biologiques dans le systeme, quelles planètes ont des signaux bio, quelles planetes ont des signaux bio, quelles planètes restent à scanner, quelles planetes restent a scanner, progression bio système, progression bio systeme", BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
        map.put("échantillons exobiologie, echantillons exobiologie, échantillons biologiques, echantillons biologiques, organiques ici, quels organismes, que reste-t-il à scanner, que reste-t-il a scanner, organismes restants, progression exobiologie, organiques sur cette planète, organiques sur cette planete, que devons-nous encore scanner ici, organiques restants à scanner, organiques restants a scanner", EXOBIOLOGY_SAMPLES_ON_THIS_PLANET.getAction());
        map.put("distance au dernier échantillon bio, distance au dernier echantillon bio, distance dernier organisme, à quelle distance est l'échantillon, a quelle distance est l'echantillon, distance au précédent organisme, distance au precedent organisme, navigue vers échantillon bio, navigue vers echantillon bio", DISTANCE_TO_LAST_BIO_SAMPLE.getAction());
        map.put("analyse biome, quel biome {key:X}, biome planétaire, biome planetaire, analyse atmosphère, analyse atmosphere, quelle vie ici, type de biome", PLANET_BIOME_ANALYSIS.getAction());
        map.put("objets stellaires, planètes dans le système, planetes dans le systeme, planètes atterrissables, planetes atterrissables, est-ce que la planète est atterrissable, est-ce que la planete est atterrissable, corps du système, corps du systeme, corps stellaires, anneaux glacés, anneaux glaces, anneaux planétaires, anneaux planetaires, anneaux dans le système, anneaux dans le systeme", QUERY_STELLAR_OBJETS.getAction());
        map.put("signaux dans le système, signaux dans le systeme, quels signaux dans ce système, quels signaux dans ce systeme, qu'est-ce qui est détecté dans le système, qu'est-ce qui est detecte dans le systeme, quels signaux ici, signaux FSS, signaux fss, hotspots minage, zones de conflit, émissions, emissions, signaux non identifiés, signaux non identifies, signaux anormaux", QUERY_STELLAR_SIGNALS.getAction());
        map.put("signaux géologiques, signaux geologiques, activité géologique, activite geologique, signaux volcaniques, activité volcanique, activite volcanique, géologie dans le système, geologie dans le systeme", QUERY_GEO_SIGNALS.getAction());

        map.put("fleet carriers dans le système, fleet carriers dans le systeme, carriers dans le système, carriers dans le systeme, porte-vaisseaux dans le système, porte-vaisseaux dans le systeme, combien de carriers, combien de porte-vaisseaux, carriers ici, carriers proches", QUERY_CARRIERS.getAction());
        map.put("route carrier, navigation carrier, route de saut carrier, trajet carrier, voyage carrier, plan de voyage carrier, combien de sauts sur la route carrier, sauts restants carrier, nombre de sauts route carrier, sauts restants sur le carrier", FLEET_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put("route carrier, où va le carrier, ou va le carrier, prochain saut carrier, destination carrier, cap carrier, destination finale carrier, où se dirige le carrier, ou se dirige le carrier", FLEET_CARRIER_ROUTE.getAction());
        map.put("tritium carrier, carburant carrier, combien de tritium, réserve tritium, reserve tritium, niveau tritium, niveau carburant carrier, statut tritium carrier", FLEET_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put("statut carrier, finances carrier, solde carrier, aperçu carrier, apercu carrier, fonds carrier, combien de temps peut fonctionner le carrier, statut carburant carrier, portée de saut carrier, portee de saut carrier, portée carrier avec tritium actuel, portee carrier avec tritium actuel", FLEET_CARRIER_STATUS.getAction());
        map.put("ETA carrier, eta carrier, quand arrive le carrier, combien de temps avant le carrier, arrivée carrier, arrivee carrier, heure d'arrivée carrier, heure d'arrivee carrier, quand saute le carrier, heure saut carrier", FLEET_CARRIER_ETA.getAction());
        map.put("distance au carrier, où est notre carrier, ou est notre carrier, à quelle distance est le carrier, a quelle distance est le carrier, portée au carrier, portee au carrier, proximité carrier, proximite carrier", DISTANCE_TO_CARRIER.getAction());
        map.put("sécurité système, securite systeme, sécurité du système, securite du systeme, contrôle faction, controle faction, qui contrôle, qui controle, qui contrôle ce système, qui controle ce systeme, niveau sécurité, niveau securite, faction dominante, puissance dominante", SYSTEM_SECURITY_ANALYSIS.getAction());
        map.put("profil commercial, paramètres commerce, parametres commerce, paramètres commerciaux, parametres commerciaux, configuration commerce, critères commerciaux, criteres commerciaux", TRADE_PROFILE_ANALYSIS.getAction());
        map.put("distance à l'objet stellaire, distance a l'objet stellaire, distance au corps, distance planète {key:X}, distance planete {key:X}, portée planète, portee planete, distance lune, distance station, portée au corps, portee au corps. NOTE: Sol et la Terre signifient toujours la Bulle/civilisation - ne jamais utiliser cette action pour Sol ou la Terre", DISTANCE_TO_BODY.getAction());
        map.put("dernier scan, qu'avons-nous scanné, qu'avons-nous scanne, dernier objet scanné, dernier objet scanne, scan le plus récent, scan le plus recent, scan récent, scan recent", LAST_SCAN_ANALYSIS.getAction());
        map.put("inventaire matériaux {key:X}, inventaire materiaux {key:X}, combien d'objets {key:X}, combien de matériau {key:X}, combien de materiau {key:X}, avons-nous matériau {key:X}, avons-nous materiau {key:X}, combien de {key:X} avons-nous, avons-nous du {key:X}, matériau d'ingénierie {key:X}, materiau d'ingenierie {key:X}, matériau brut {key:X}, materiau brut {key:X}, matériau manufacturé {key:X}, materiau manufacture {key:X}, donnée codée {key:X}, donnee codee {key:X}, stock matériau {key:X}, stock materiau {key:X}", MATERIALS_INVENTORY.getAction());
        map.put("matériaux planète, materiaux planete, matériaux ici, materiaux ici, quels matériaux sur cette planète, quels materiaux sur cette planete, matériaux de surface, materiaux de surface, dépôts de matériaux, depots de materiaux, minéraux planète, mineraux planete", PLANET_MATERIALS.getAction());
        map.put("profits exploration, bénéfices exploration, benefices exploration, combien vaut l'exploration, valeur exploration, valeur scan, profits cartographie, valeur exobiologie, gains scan, que valent les scans", EXPLORATION_PROFITS.getAction());
        map.put("position actuelle, où sommes-nous, ou sommes-nous, où suis-je, ou suis-je, dans quel système sommes-nous, dans quel systeme sommes-nous, notre position, nos coordonnées, nos coordonnees, système actuel, systeme actuel, quelle planète actuelle, quelle planete actuelle, durée du jour, duree du jour", CURRENT_LOCATION.getAction());
        map.put("dans quel système suis-je, dans quel systeme suis-je, où est mon vaisseau, ou est mon vaisseau, où sommes-nous en ce moment, ou sommes-nous en ce moment, notre localisation actuelle, quelle est notre position actuelle", CURRENT_LOCATION.getAction());
        map.put("info cible FSD, info cible fsd, analyse destination, quelle étoile ciblons-nous, quelle etoile ciblons-nous, analyse cible FSD, analyse cible fsd, informations sur la cible FSD", FSD_TARGET_ANALYSIS.getAction());
        map.put("route tracée, route tracee, carburant au prochain arrêt, carburant au prochain arret, carburant disponible sur la route, analyse route, sommes-nous arrivés, sommes-nous arrives, route actuelle, itinéraire actuel, itineraire actuel, sauts restants, combien de sauts, prochaine étoile scoopable, prochaine etoile scoopable, arrêt carburant, arret carburant", PLOTTED_ROUTE_ANALYSIS.getAction());
        map.put("route commerciale, itinéraire commercial, itineraire commercial, plan commercial actuel, que transportons-nous pour le commerce, notre plan commercial, planning commercial, étapes commerciales, etapes commerciales", TRADE_ROUTE_ANALYSIS.getAction());
        map.put("outfitting, équipement station, equipement station, améliorations vaisseau, ameliorations vaisseau, modules disponibles, quels modules station, équipement disponible, equipement disponible, acheter modules, pièces de vaisseau, pieces de vaisseau", LOCAL_OUTFITTING.getAction());
        map.put("chantier naval, vaisseaux à vendre, vaisseaux a vendre, quels vaisseaux station, acheter vaisseau, vaisseaux disponibles, nouveaux vaisseaux, shipyard", LOCAL_SHIPYARD.getAction());
        map.put("contenu de la soute, que transportons-nous, cargaison à bord, cargaison a bord, marchandises à bord, marchandises a bord, que contient la soute, contenu cargo", CARGO_HOLD_CONTENTS.getAction());
        map.put("profil joueur, profil commander, profil commandant", PLAYER_PROFILE_ANALYSIS.getAction());
        map.put("configuration vaisseau, loadout vaisseau, rapport dégâts, rapport degats, modules vaisseau, rapport préparation combat, rapport preparation combat, équipement vaisseau, equipement vaisseau, spécifications vaisseau, specifications vaisseau, que pilotons-nous, avec quoi sommes-nous équipés, avec quoi sommes-nous equipes, shield generator, renfort de coque, capteurs, propulseurs, frameshift, fuel scoop, installé, installe", SHIP_LOADOUT.getAction());
        map.put("détails station, details station, quels services ici, services ici, quels services sont ici, quels services cette station offre, services station, que propose la station, info station, installations station, qu'y a-t-il à cette station, qu'y a-t-il a cette station, services disponibles", STATION_DETAILS.getAction());
        map.put("à quelle station suis-je, a quelle station suis-je, à quelle station sommes-nous, a quelle station sommes-nous, où suis-je amarré, ou suis-je amarre, où sommes-nous amarrés, ou sommes-nous amarres, quelle est ma station actuelle, ma station actuelle, notre station actuelle", STATION_DETAILS.getAction());
        map.put("primes, total primes, primes collectées, primes collectees, combien en primes, gains primes, crédits de primes, credits de primes", TOTAL_BOUNTIES.getAction());
        map.put("distance à la Bulle, distance a la Bulle, distance à Sol, distance a Sol, distance depuis Sol, distance à la Terre, distance a la Terre, distance depuis la Terre, distance espace habité, distance espace habite, distance civilisation, à quelle distance de la civilisation, a quelle distance de la civilisation", DISTANCE_TO_BUBBLE.getAction());
        map.put("heure actuelle, quelle heure est-il, heure UTC, heure utc, heure sur Terre, heure galactique, heure réelle, heure reelle", TIME_IN_ZONE.getAction());
        map.put("rappel, quel était le rappel, quel etait le rappel, rappel destination, destination rappel, y a-t-il des rappels, rappelle le rappel, quel rappel avons-nous défini, quel rappel avons-nous defini", REMINDER.getAction());
        map.put("marchés locaux, marches locaux, marchés aux stations et colonies, marches aux stations et colonies, marchés aux avant-postes du système, marches aux avant-postes du systeme", ANALYZE_MARKETS.getAction());
    }
}
