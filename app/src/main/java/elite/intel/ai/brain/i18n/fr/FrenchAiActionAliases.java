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
        return Set.of("réveille-toi", "réveille toi", "reveille-toi", "reveille toi", "écoute", "ecoute", "ecoute commande vocale", "ecoute les commandes vocales");
    }

    @Override
    public Set<String> listenBypassPrefixes() {
        return Set.of("écoute-moi", "écoute moi", "ecoute-moi", "ecoute moi", "écoute", "ecoute");
    }

    @Override
    public void addAliases(Map<String, String> map, Status status, boolean isDryRun) {

        // -----------------------------------------------------------------
        // Aliases toujours disponibles – placés tôt pour priorité
        // -----------------------------------------------------------------
        map.put("réveil, réveille-toi, réveille toi, reveille-toi, reveille toi, activation commande vocale, active les commandes vocales, écoute, ecoute", WAKEUP.getAction());
        map.put("désactive les commandes vocales, passe en mode veille, mise en veille, mode veille, tu peux disposer, désactivation commande vocale, desactivation commande vocale, desactive les commandes vocales, dors, dormir, endors-toi", SLEEP.getAction());
        map.put("interromps, interruption, arrête de parler, arrete de parler, coupe la voix, stop voix, silence, tais-toi", INTERRUPT_TTS.getAction());

        // -----------------------------------------------------------------
        // Désambiguïsation carriers : "porte-vaisseau" seul = fleet
        // Ces alias doivent venir AVANT les requêtes génériques "statut porte-vaisseau"
        // -----------------------------------------------------------------
        map.put("trouve le porte-vaisseau le plus proche, cherche le porte-vaisseau le plus proche, porte-vaisseau le plus proche", FIND_NEAREST_FLEET_CARRIER.getAction());
        map.put("statut du porte-vaisseau, rapport du porte-vaisseau, état du porte-vaisseau, combien de temps peut fonctionner le porte-vaisseau, portée du porte-vaisseau avec tritium actuel, portée de saut du porte-vaisseau", FLEET_CARRIER_STATUS.getAction());
        map.put("statut du porte-vaisseau d'escadron, finances du porte-vaisseau d'escadron, combien de temps peut fonctionner le porte-vaisseau d'escadron", SQUADRON_CARRIER_STATUS.getAction());

        // -----------------------------------------------------------------
        // Navigation (commandes)
        // -----------------------------------------------------------------
        map.put("annule la route commerciale, arrête la route commerciale, supprime la route commerciale, annule l'itinéraire commercial", CANCEL_TRADE_ROUTE.getAction());
        map.put("navigue vers les coordonnées {lat:X, lon:Y}, va aux coordonnées {lat:X, lon:Y}", NAVIGATE_TO_TARGET.getAction());
        map.put("navigue vers la mission active, trace l'itinéraire vers la mission active, va à la mission {key:X}", NAVIGATE_TO_NEXT_MISSION.getAction());
        map.put("navigue vers le porte-vaisseau, trace l'itinéraire vers le porte-vaisseau, retourne au porte-vaisseaux, ramène-nous au porte-vaisseaux, direction le porte-vaisseaux, cap sur le porte-vaisseaux, retourne au porte vaisseaux, ", NAVIGATE_TO_FLEET_CARRIER.getAction());
        map.put("navigue vers la zone d'atterrissage, direction la zone d'atterrissage, va à la ZA", GET_HEADING_TO_LZ.getAction());
        map.put("navigue vers le prochain arrêt commercial, va au prochain point de commerce", NAVIGATE_TO_NEXT_TRADE_STOP.getAction());
        map.put("navigue depuis la mémoire, colle depuis la mémoire, adresse depuis la mémoire", NAVIGATE_TO_ADDRESS_FROM_MEMORY.getAction());
        map.put("annule la navigation, arrête la navigation, annule l'itinéraire, stop navigation", NAVIGATION_OFF.getAction());
        map.put("définis le système de base, marque le système actuel comme base, définis le système d'origine", SET_HOME_SYSTEM.getAction());
        map.put("navigue vers la base, trace une route vers la base, ramène-moi à la base, retour à la base", TAKE_ME_HOME.getAction());
        map.put("réinitialise la vue tête, reset head look, vue tête par défaut", RESET_HEAD_LOOK.getAction());

        if (status.isInMainShip() || isDryRun) {
            // Navigation
            map.put("cible la prochaine destination, sélectionne la prochaine étape d'itinéraire, verrouille la destination suivante, sélectionne la destination FSD", TARGET_DESTINATION.getAction());
            map.put("saute en hyperespace, lance le saut FSD, active le saut, saut, allons-y, fs d, f sd, f s d", JUMP_TO_HYPERSPACE.getAction());
            map.put("sors de la super navigation, quitte la super navigation, drop, drop maintenant", DROP_FROM_SUPER_CRUISE.getAction());
            map.put("entre en super navigation, active la super navigation, super navigation", ENTER_SUPER_CRUISE.getAction());
            map.put("lance le vaisseau, décolle, quitte le port, quitte la station", LAUNCH_SHIP.getAction());

            // Vitesse / propulseurs
            map.put("arrête le vaisseau, coupe les gaz, stop, arrêt complet, halte", SET_SPEED_ZERO.getAction());
            map.put("active le pilotage automatique, prends les commandes, mode taxi, taxi", TAXI.getAction());
            map.put("règle la vitesse à 25 pour cent, quart de poussée, vitesse lente, quart de poussée, quart de poussee, vingt-cinq pour cent, 25 pour cent, vitesse lente, un quart", SET_SPEED25.getAction());
            map.put("règle la vitesse à 50 pour cent, demi poussée, vitesse moyenne, demi poussée, demi poussee, cinquante pour cent, 50 pour cent, demi-vitesse, mi-vitesse", SET_SPEED50.getAction());
            map.put("règle la vitesse à 75 pour cent, trois quarts de poussée, vitesse rapide, trois quarts de poussée, trois quarts de poussee, soixante-quinze pour cent, 75 pour cent, trois quarts vitesse, propulsion à 75 %, propulsion à soixante quinze pourcent", SET_SPEED75.getAction());
            map.put("règle la vitesse à 100 pour cent, pleine poussée, plein gaz, vitesse maximum, pleine poussée, pleine poussee, propulseurs à 100 pour cent, propulseurs à fond, poussée à fond, vitesse maximale, poussée maximale, poussee, mets les gaz, plein gaz\"", SET_SPEED100.getAction());
            map.put("augmente la vitesse de {key:X}, accélère de {key:X}", INCREASE_SPEED_BY.getAction());
            map.put("réduis la vitesse de {key:X}, ralentis de {key:X}", DECREASE_SPEED_BY.getAction());
            map.put("règle la vitesse optimale, vitesse d'approche optimale, optimise la vitesse d'approche", SET_OPTIMAL_SPEED.getAction());

            // Train d'atterrissage
            map.put("déploie le train d'atterrissage, sors le train, trains d'atterrissage", DEPLOY_LANDING_GEAR.getAction());
            map.put("rentre le train d'atterrissage, relève le train, remonte les trains", RETRACT_LANDING_GEAR.getAction());
            map.put("demande l'autorisation d'appontage, demande d'appontage, demande l'atterrissage, demande un quai, demande un pad", REQUEST_DOCKING.getAction());

            // Panneaux UI (commandes)
            map.put("montre le panneau chasseur, ouvre le panneau chasseur, affiche le panneau fighter", SHOW_FIGHTER_PANEL.getAction());
            map.put("utilise une cellule de bouclier, active une cellule de bouclier, déploie une cellule de bouclier, cellule de bouclier", DEPLOY_SHIELD_CELL.getAction());
            map.put("lance les paillettes, déploie un leurre, active le chaff, lance chaff", DEPLOY_CHAFF.getAction());

            // Combat
            map.put("déploie les points d'emport, sors les armes, armes au clair", DEPLOY_HARDPOINTS.getAction());
            map.put("rentre les points d'emport, range les armes, replie les points d'emport, armes rangées", RETRACT_HARDPOINTS.getAction());
            map.put("cible le fsd {key:fsd}, cible les moteurs {key:drive}, cible la centrale électrique {key:powerplant}, cible le distributeur d'énergie {key:power distributor}, cible le système de survie {key:life support}", TARGET_SUB_SYSTEM.getAction());
            map.put("cible l'ailier un, cible l'ailier 1, ailier alpha", TARGET_WINGMAN0.getAction());
            map.put("cible l'ailier deux, cible l'ailier 2, ailier bravo", TARGET_WINGMAN1.getAction());
            map.put("cible l'ailier trois, cible l'ailier 3, ailier charlie", TARGET_WINGMAN2.getAction());
            map.put("verrouille la navigation sur l'ailier, suis l'ailier, lock navigation escadre", WING_NAV_LOCK.getAction());
            map.put("cible la menace prioritaire, sélectionne l'ennemi le plus dangereux, verrouille la plus grande menace, cible prioritaire, prochain ennemi", SELECT_HIGHEST_THREAT.getAction());

            // Déploiement véhicules
            map.put("déploie le VRS, lance le véhicule, sors le VRS", DEPLOY_SRV.getAction());
            map.put("lance un dissipateur thermique, déploie un heat sink, évacue la chaleur, heat sink", DEPLOY_HEAT_SINK.getAction());

            // Ordres chasseur
            map.put("déploie le chasseur, lance le chasseur, sors le chasseur", DEPLOY_FIGHTER.getAction());
            map.put("chasseur défends le vaisseau, chasseur en défense, chasseur défensif", FIGHTER_REQUEST_DEFENSIVE_BEHAVIOUR.getAction());
            map.put("chasseur attaque ma cible, concentre-toi sur ma cible, focus sur la cible, chasseur attaque", FIGHTER_REQUEST_FOCUS_TARGET.getAction());
            map.put("chasseur cesse le feu, hold fire chasseur, chasseur ne tire pas", FIGHTER_REQUEST_HOLD_FIRE.getAction());
            map.put("chasseur rentre au vaisseau, rappelle le chasseur, chasseur dock", FIGHTER_REQUEST_REQUEST_DOCK.getAction());
            map.put("ordonne feu à volonté, chasseur feu à volonté, active le tir à volonté, feu à volonté, fire at will", FIGHTER_OPEN_ORDERS.getAction());
            map.put("sélectionne le groupe de tir {key:X}, bascule vers le groupe de tir {key:X}, groupe de tir {key:X}", SELECT_FIRE_GROUP_BY_NATO.getAction());

            // Neutrons (commandes)
            map.put("calcule la route des étoiles à neutrons avec efficacité {efficiency:X}, trace l'itinéraire par étoiles à neutrons, calcule l'itinéraire des étoiles à neutrons", CALCULATE_NEUTRON_STAR_ROUTE.getAction());
            map.put("trace la route vers la prochaine étoile à neutrons, planifie le prochain saut neutron, va à la prochaine étoile à neutrons, prochain saut d'étoile à neutrons", PLOT_ROUTE_TO_NEXT_NEUTRON_STAR.getAction());
            map.put("efface la route des étoiles à neutrons, supprime l'itinéraire neutron, annule le voyage par neutrons, effacer la route des étoiles à neutrons", CLEAR_NEUTRON_ROUTE.getAction());
        }

        if (status.isInMainShip() && !status.isDocked() || isDryRun) {
            map.put("quelles stations dans le système, liste les ports spatiaux, où puis-je me poser ici, stations spatiales", QUERY_STATIONS.getAction());
        }

        if (status.isInSrv() && status.isDocked() || isDryRun) {
            map.put("montre le panneau services, ouvre les services de la station, affiche services station", SHOW_STATION_SERVICES.getAction());
        }

        if (status.isInMainShip() || status.isInSrv() || isDryRun) {
            // Vol / systèmes
            map.put("passe en mode combat, active le mode combat", ACTIVATE_COMBAT_MODE.getAction());
            map.put("passe en mode analyse, active le mode analyse", ACTIVATE_ANALYSIS_MODE.getAction());
            map.put("déploie le récupérateur de cargaison, rentre le récupérateur de cargaison, active le récupérateur de cargaison, désactive le récupérateur de cargaison, ouvre la trappe , ferme la trappe", TOGGLE_CARGO_SCOOP.getAction());
            map.put("active la vision nocturne, désactive la vision nocturne, vision nocturne", NIGHT_VISION_ON_OFF.getAction());
            map.put("allume les phares, éteins les phares, allume les lumières, éteins les lumières, lumières, phares", LIGHTS_ON_OFF.getAction());

            // Panneaux UI
            map.put("montre le panneau commandant, ouvre le panneau central, affiche le panneau du commandant, panneau central", SHOW_COMMANDER_PANEL.getAction());
            map.put("montre le panneau équipage, ouvre l'équipage", SHOW_CREW.getAction());
            map.put("montre le panneau accueil, ouvre le panneau interne", SHOW_INTERNAL_PANEL.getAction());
            map.put("montre le panneau modules, ouvre les modules, affiche le panneau d'équipement du vaisseau, affiche le panneau modules du vaisseau, affiche le panneau equipement du vaisseau", SHOW_MODULES_PANEL.getAction());
            map.put("montre les groupes de tir, ouvre les groupes d'armes, fire groups", SHOW_FIRE_GROUPS.getAction());
            map.put("montre l'inventaire, ouvre le cargo, affiche la liste des matériaux", SHOW_INVENTORY_PANEL.getAction());
            map.put("montre le stockage, ouvre le stockage", SHOW_STORAGE_PANEL.getAction());

            // Distribution d'énergie
            map.put("mets la puissance dans les boucliers, concentre l'energie vers les boucliers, priorité aux boucliers, puissance aux boucliers, energie dans les boucliers, boost boucliers, rediriger vers bouclier", INCREASE_SHIELDS_POWER.getAction());
            map.put("redirige la puissance vers les moteurs, priorité aux moteurs, puissance dans les moteurs, mets la puissance dans les moteurs", INCREASE_ENGINES_POWER.getAction());
            map.put("redirige la puissance vers les armes, priorité aux armes, puissance dans les armes, mets la puissance dans les armes", INCREASE_WEAPONS_POWER.getAction());
            map.put("réinitialise la puissance, équilibre le distributeur, remet la puissance par défaut, puissance à l'équilibre", RESET_POWER.getAction());

            // Débarquement
            map.put("je débarque, sors du vaisseau, quitte le vaisseau, débarquer", DISEMBARK.getAction());
        }

        if (status.isInSrv() || isDryRun) {
            map.put("active l'assistance de conduite, désactive l'assistance de conduite, drive assist", DRIVE_ASSIST.getAction());
            map.put("récupère le VRS, remonte à bord, dock le VRS, récupère VRS", RECOVER_SRV.getAction());
        }

        if (status.isInSrv() || status.isOnFoot() || isDryRun) {
            map.put("renvoie le vaisseau en orbite, envoie le vaisseau se mettre en orbite, renvoie le vaisseau", DISMISS_SHIP.getAction());
            map.put("retour à la surface, rappelle le vaisseau, viens me chercher, retourne à la surface", RETURN_TO_SURFACE.getAction());
        }

        // -----------------------------------------------------------------
        // Marchés / courtiers (commandes)
        // -----------------------------------------------------------------
        map.put("trouve un marchand de matériaux bruts, où échanger les matériaux bruts {key:X}, raw material trader", FIND_RAW_MATERIAL_TRADER.getAction());
        map.put("trouve un marchand de données codées, data trader {key:X}, encoded material trader", FIND_ENCODED_MATERIAL_TRADER.getAction());
        map.put("trouve un marchand de matériaux manufacturés, manufactured trader {key:X}", FIND_MANUFACTURED_MATERIAL_TRADER.getAction());
        map.put("trouve un courtier de technologies humaines, human tech broker {key:X}", FIND_HUMAN_TECHNOLOGY_BROKER.getAction());
        map.put("trouve un courtier de technologies gardiennes, guardian tech broker {key:X}", FIND_GUARDIAN_TECHNOLOGY_BROKER.getAction());
        map.put("trouve une marchandise {key:X, max_distance:Y}, où acheter {key:X, max_distance:Y}, je cherche {key:X}, cherche {key:X}, cherche {key:X} à un distance de {max_distance:Y}, où acheter {key:X, max_distance:Y},", FIND_COMMODITY.getAction());
        map.put("trouve l'Interstellar Factor le plus proche, où payer mes amendes", FIND_INTERSTELLAR_FACTOR.getAction());

        // -----------------------------------------------------------------
        // Porte-vaisseau personnel (commandes)
        // -----------------------------------------------------------------
        map.put("définis la réserve de tritium à {key:X}, règle la réserve de carburant du porte-vaisseau, réserve tritium porte-vaisseau {key:X}", SET_CARRIER_FUEL_RESERVE.getAction());
        map.put("calcule la route du porte-vaisseau, planifie l'itinéraire du porte-vaisseau, calcule route porte-vaisseau", CALCULATE_FLEET_CARRIER_ROUTE.getAction());
        map.put("entre la destination du porte-vaisseau, définis la destination du porte-vaisseau, programme la prochaine destination, destination porte-vaisseau", ENTER_FLEET_CARRIER_DESTINATION.getAction());

        // -----------------------------------------------------------------
        // Porte-vaisseau d'escadron (commandes)
        // -----------------------------------------------------------------
        map.put("affiche la route du porte-vaisseau d'escadron, analyse l'itinéraire du porte-vaisseau d'escadron, route escadron porte-vaisseau", SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put("où va le porte-vaisseau d'escadron, destination finale du porte-vaisseau d'escadron, cap escadron porte-vaisseau", SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION.getAction());
        map.put("quel est le niveau de tritium du porte-vaisseau d'escadron, carburant du porte-vaisseau d'escadron, tritium escadron porte-vaisseau", SQUADRON_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put("statut du porte-vaisseau d'escadron, finances du porte-vaisseau d'escadron, solde escadron porte-vaisseau", SQUADRON_CARRIER_STATUS.getAction());
        map.put("ETA du porte-vaisseau d'escadron, quand arrive le porte-vaisseau d'escadron, heure d'arrivée porte-vaisseau d'escadron", SQUADRON_CARRIER_ETA.getAction());

        // -----------------------------------------------------------------
        // Commerce (commandes)
        // -----------------------------------------------------------------
        map.put("calcule une route commerciale, calcule un itinéraire commercial, calcule trade route", CALCULATE_TRADE_ROUTE.getAction());
        map.put("liste les paramètres de la route commerciale, affiche les paramètres trade, paramètres commerciaux", LIST_TRADE_ROUTE_PARAMETERS.getAction());
        map.put("calcule le profit de la route, monétise la route, optimise profit route", MONETIZE_ROUTE.getAction());

        map.put("change le budget de départ du profil commercial à {key:X}", CHANGE_TRADE_PROFILE_SET_STARTING_BUDGET.getAction());
        map.put("change le nombre maximum d'arrêts du profil commercial à {key:X}", CHANGE_TRADE_PROFILE_SET_MAX_NUMBER_OF_STOPS.getAction());
        map.put("change la distance maximum du profil commercial à {key:X}", CHANGE_TRADE_PROFILE_SET_MAX_DISTANCE_FROM_ENTRY.getAction());
        map.put("autorise les marchandises interdites dans le profil commercial {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PROHIBITED_CARGO.getAction());
        map.put("autorise les ports planétaires dans le profil commercial {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PLANETARY_PORT.getAction());
        map.put("autorise les systèmes à permis dans le profil commercial {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PERMIT_SYSTEMS.getAction());
        map.put("autorise les bastions dans le profil commercial {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_STRONGHOLDS.getAction());

        // -----------------------------------------------------------------
        // Annonces / réglages (commandes)
        // -----------------------------------------------------------------
        map.put("active la radio, désactive la radio {state:true/false}, radio, trafic radio", SET_RADIO_TRANSMISSION_MODE.getAction());
        map.put("active les annonces de contact radar, désactive les annonces radar {state:true/false}, annonce contact radar", SET_RADAR_CONTACT_ANNOUNCEMENT.getAction());
        map.put("active les annonces de découverte, désactive les annonces de découverte {state:true/false}", DISCOVERY_ON_OFF.getAction());
        map.put("active les annonces de route, désactive les annonces d'itinéraire {state:true/false}", ROUTE_ON_OFF.getAction());
        map.put("désactive toutes les annonces, coupe toutes les notifications vocales {state:true/false}, basculer toutes les annonces", TOGGLE_ALL_ANNOUNCEMENTS.getAction());
        map.put("efface les rappels, supprime tous les rappels, clear reminders", CLEAR_REMINDERS.getAction());
        map.put("définis un rappel {key:X}, crée un rappel {key:X}, nouveau rappel {key:X}, rappelle-moi {key:X}, ajoute un rappel {key:X}", SET_REMINDER.getAction());
        map.put("rappelle-moi dans {minutes:X} minutes {key:Y}, minuterie {minutes:X} minutes {key:Y}, rappel minuté {minutes:X} minutes {key:Y}", SET_TIMED_REMINDER.getAction());
        map.put("efface toutes les missions actives, supprime les missions en cours", CLEAR_ALL_ACTIVE_MISSIONS.getAction());

        // -----------------------------------------------------------------
        // Panneaux UI génériques (commandes)
        // -----------------------------------------------------------------
        map.put("active, active ça", ACTIVATE.getAction());
        map.put("montre les transactions, ouvre le panneau transactions", SHOW_TRANSACTIONS.getAction());
        map.put("montre les contacts, ouvre le panneau contacts", SHOW_CONTACTS.getAction());
        map.put("montre la navigation, ouvre le panneau de navigation", SHOW_NAVIGATION.getAction());
        map.put("montre le chat, ouvre les communications", SHOW_CHAT_PANEL.getAction());
        map.put("montre la boîte de réception, ouvre les messages", SHOW_INBOX_PANEL.getAction());
        map.put("montre le panneau social, ouvre le social", SHOW_SOCIAL_PANEL.getAction());
        map.put("montre l'historique, ouvre l'historique", SHOW_HISTORY_PANEL.getAction());
        map.put("montre l'escadron, ouvre le panneau escadron", SHOW_SQUADRON.getAction());
        map.put("montre le statut, ouvre le panneau de statut, montre statut, ouvre statut, affiche statut, panneau statut", SHOW_STATUS_PANEL.getAction());
        map.put("montre la gestion du porte-vaisseau, ouvre le porte-vaisseau management, gestion porte-vaisseau", DISPLAY_CARRIER_MANAGEMENT.getAction());
        map.put("ouvre la carte galactique, affiche la galaxy map, carte galaxie", OPEN_GALAXY_MAP.getAction());
        map.put("ouvre la carte du système, affiche la carte locale, carte système", OPEN_SYSTEM_MAP.getAction());
        map.put("ferme le panneau, quitte l'écran actuel, close panel", EXIT_CLOSE.getAction());

        // -----------------------------------------------------------------
        // Missions pirates (commandes)
        // -----------------------------------------------------------------
        map.put("navigue vers le système fournisseur de missions, va au donneur de missions", RECON_PROVIDER_SYSTEM.getAction());
        map.put("navigue vers le fournisseur de missions pirates, va au contact pirate", NAVIGATE_TO_PIRATE_MISSION_PROVIDER.getAction());
        map.put("analyse les missions actives, liste les missions en cours, missions actives", ANALYZE_MISSIONS.getAction());
        map.put("progression de la mission pirate, combien de kills restants, compteur de kills", PIRATE_MISSION_PROGRESS.getAction());
        map.put("trouve un terrain de chasse pour {key:X}, cherche une zone de massacre, trouve terrains de chasse {key:X}", FIND_HUNTING_GROUNDS.getAction());
        map.put("reconnais le terrain de chasse, navigue vers le système cible", RECON_TARGET_SYSTEM.getAction());
        map.put("ignore ce terrain de chasse, ne considère pas cette zone", IGNORE_HUNTING_GROUND.getAction());
        map.put("confirme ce terrain de chasse, valide le système cible", CONFIRM_HUNTING_GROUND.getAction());

        // -----------------------------------------------------------------
        // Minage / biologie (commandes)
        // -----------------------------------------------------------------
        map.put("ajoute une cible de minage {key:X}, ajoute un objectif minage", ADD_MINING_TARGET.getAction());
        map.put("retire une cible de minage {key:X}, supprime un objectif minage", REMOVE_MINING_TARGET.getAction());
        map.put("efface toutes les cibles de minage, vide la liste de minage", CLEAR_MINING_TARGETS.getAction());
        map.put("active les annonces de minage, désactive les annonces mining {state:true/false}", MINING_ON_OFF.getAction());
        map.put("trouve des arbres cérébraux {key:X, max_distance:Y}, cherche des brain trees", FIND_BRAIN_TREES.getAction());
        map.put("trouve un site de minage pour {key:X} dans {max_distance:Y}, où miner, trouve champ d'astéroïdes", FIND_MINING_SITE.getAction());
        map.put("trouve un site de minage de tritium, où miner du tritium {key:X, max_distance:Y}, trouve site de minage tritium", FIND_FLEET_CARRIER_FUEL_MINING_SITE.getAction());
        map.put("navigue vers le prochain échantillon biologique, va au prochain organique, trace l'itinéraire vers l'entrée codex suivante, navigue vers prochain échantillon biologique", NAVIGATE_TO_NEXT_BIO_SAMPLE.getAction());
        map.put("ouvre l'analyseur de système, lance l'analyse complète du système, affiche l'analyseur spectral, ouvre ACS, scanner de système", OPEN_FSS.getAction());
        map.put("trouve Vista Genomics le plus proche, cherche un centre Vista Genomics, vista genomics", FIND_VISTA_GENOMICS.getAction());
        map.put("supprime cette entrée codex, efface cet organique, supprime entrée codex", DELETE_CODEX_ENTRY.getAction());

        // -----------------------------------------------------------------
        // REQUÊTES (commencent par un starter)
        // -----------------------------------------------------------------
        map.put("vérifie les raccourcis clavier, quelles touches sont manquantes, analyse les bindings, commandes manquantes", KEY_BINDINGS_ANALYSIS.getAction());
        map.put("redirige la puissance vers les boucliers, priorité aux boucliers, puissance dans les boucliers, redirige la puissance vers les systèmes, priorité aux systèmes, puissance dans les systèmes", INCREASE_SHIELDS_POWER.getAction());
        map.put("quels organismes reste-t-il à scanner ici, liste les échantillons exobiologiques, que devons-nous encore prélever, échantillons exobiologie", EXOBIOLOGY_SAMPLES_ON_THIS_PLANET.getAction());
        map.put("distance depuis le dernier échantillon biologique, à quelle distance se trouve le dernier organique scanné, combien de mètres jusqu'au prochain, distance au dernier échantillon biologique", DISTANCE_TO_LAST_BIO_SAMPLE.getAction());
        map.put("signaux biologique traités dans le système, signaux biologique traites dans le systeme, organiques scannés dans le système, organiques scannes dans le systeme, combien d'échantillons biologique dans le système, combien d'echantillons biologique dans le systeme, signaux biologiques dans le système, signaux biologiques dans le systeme, quelles planètes ont des signaux biologique, quelles planetes ont des signaux biologique, quelles planètes restent à scanner, quelles planetes restent a scanner, progression biologique système, progression biologique systeme", BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
        map.put("quel est le biome de cette planète, analyse de l'environnement, rapport sur le type de terrain, analyse biome", PLANET_BIOME_ANALYSIS.getAction());
        map.put("quels sont les corps du système, y a-t-il des planètes atterrissables, montre les anneaux planétaires, objets stellaires", QUERY_STELLAR_OBJETS.getAction());
        map.put("quels signaux sont détectés, y a-t-il des hotspots de minage, analyse les sources inconnues, signaux dans le système", QUERY_STELLAR_SIGNALS.getAction());
        map.put("y a-t-il des signaux géologiques, activité volcanique détectée, où sont les sites géologiques, signaux géologiques", QUERY_GEO_SIGNALS.getAction());
        map.put("combien de porte-vaisseaux dans le système, y a-t-il des carriers ici, fleet porte-vaisseaux dans le système", QUERY_CARRIERS.getAction());
        map.put("quel est l'itinéraire de mon porte-vaisseau, combien de sauts pour le porte-vaisseau, rapport de l'itinéraire du porte-vaisseau, route porte-vaisseau", FLEET_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put("quel est le statut de mon porte-vaisseau, rapport du porte-vaisseau, état du porte-vaisseau (déjà partiellement traité plus haut mais on garde pour la requête explicite)", FLEET_CARRIER_STATUS.getAction());
        map.put("quand arrive mon porte-vaisseau, ETA du porte-vaisseau, heure d'arrivée du porte-vaisseau, quand saute le porte-vaisseau", FLEET_CARRIER_ETA.getAction());
        map.put("à quelle distance se trouve mon porte-vaisseau, distance jusqu'au porte-vaisseau, sommes-nous loin de notre porte-vaisseau, proximité du porte-vaisseau", DISTANCE_TO_CARRIER.getAction());
        map.put("quel est le niveau de sécurité du système, qui contrôle ce système, faction dominante ici, sécurité système", SYSTEM_SECURITY_ANALYSIS.getAction());
        map.put("quels sont les paramètres commerciaux actuels, configuration du profil de commerce, profil commercial", TRADE_PROFILE_ANALYSIS.getAction());
        map.put("distance à l'objet stellaire, quelle est la distance jusqu'à cette planète, distance au corps", DISTANCE_TO_BODY.getAction());
        map.put("quel a été le dernier scan, affiche le résultat du dernier analyse, qu'avons-nous scanné récemment, dernier scan", LAST_SCAN_ANALYSIS.getAction());
        map.put("combien de fer avons-nous, inventaire des matériaux, avons-nous du manganèse, inventaire matériaux {key:X}", MATERIALS_INVENTORY.getAction());
        map.put("quels matériaux trouve-t-on sur cette planète, ressources de surface, minéraux disponibles ici, matériaux planète", PLANET_MATERIALS.getAction());
        map.put("combien rapportent les scans d'exploration, valeur des données exobiologiques, estimation des profits, profits exploration", EXPLORATION_PROFITS.getAction());
        map.put("où sommes-nous, quelle est notre position actuelle, dans quel système sommes-nous, position actuelle", CURRENT_LOCATION.getAction());
        map.put("quelle est la cible FSD actuelle, informations sur la prochaine destination, quel système est ciblé, info cible FSD", FSD_TARGET_ANALYSIS.getAction());
        map.put("route tracée, route tracee, carburant au prochain arrêt, carburant au prochain arret, carburant disponible sur la route, analyse route, sommes-nous arrivés, sommes-nous arrives, route actuelle, itinéraire actuel, itineraire actuel, sauts restants, combien de sauts, prochaine étoile scoopable, prochaine etoile scoopable, arrêt carburant, arret carburant, rapport d'itinéraire, rapport sur l'itinérairer, quand est-ce qu'on arrive, quand arrive-t-on, on arrive quand, il reste combien de saut", PLOTTED_ROUTE_ANALYSIS.getAction());
        map.put("quel est notre plan commercial actuel, que transportons-nous pour le commerce, route commerciale", TRADE_ROUTE_ANALYSIS.getAction());
        map.put("quels équipements sont disponibles ici, améliorations vaisseau, modules en vente, outfitting", LOCAL_OUTFITTING.getAction());
        map.put("quels vaisseaux sont à vendre, chantier naval, shipyard", LOCAL_SHIPYARD.getAction());
        map.put("que contient la soute, quel est le contenu du cargo, liste la cargaison, contenu de la soute", CARGO_HOLD_CONTENTS.getAction());
        map.put("quel est mon profil de commandant, affiche mes statistiques, rapport sur le commandant, profil joueur", PLAYER_PROFILE_ANALYSIS.getAction());
        map.put("configuration du vaisseau, rapport des dégâts, modules du vaisseau, rapport de préparation au combat, spécifications du vaisseau, que pilotons-nous, avec quoi sommes-nous équipés, as-tu, est-ce équipé, générateur de bouclier, renfort de coque, capteurs, propulseurs, frameshift, collecteur de carburant, installé", SHIP_LOADOUT.getAction());
        map.put("quels services propose cette station, détails de la station, que puis-je faire ici, services station", STATION_DETAILS.getAction());
        map.put("combien de primes avons-nous accumulées, total des primes, rapport sur le montant des récompenses, primes", TOTAL_BOUNTIES.getAction());
        map.put("à quelle distance de la Bulle sommes-nous, distance depuis Sol, combien d'années-lumière de la civilisation, distance de la Bulle", DISTANCE_TO_BUBBLE.getAction());
        map.put("quelle heure est-il, donne-moi l'heure UTC, heure actuelle, heure UTC", TIME_IN_ZONE.getAction());
        map.put("quel est le rappel actif, rappelle-moi ce que j'ai programmé, liste les rappels, rappel", REMINDER.getAction());
        map.put("quels sont les marchés locaux, liste les stations avec commerce, où puis-je acheter des marchandises, marchés locaux, trouve moi le marché le plus proche, cherche une station avec commerce", ANALYZE_MARKETS.getAction());

        // -----------------------------------------------------------------
        // Alias pour NAVIGATE_TO_SQUADRON_CARRIER (commandes)
        // -----------------------------------------------------------------
        map.put("navigue vers le porte-vaisseau d'escadron, trace l'itinéraire vers le porte-vaisseau d'escadron", NAVIGATE_TO_SQUADRON_CARRIER.getAction());

        // -----------------------------------------------------------------
        // Formes courtes / nominales conservées pour compatibilité (mais placées en fin pour ne pas interférer)
        // -----------------------------------------------------------------
        map.put("arrête de parler", INTERRUPT_TTS.getAction());
        map.put("tais-toi", INTERRUPT_TTS.getAction());
        map.put("stop voix", INTERRUPT_TTS.getAction());
        map.put("silence", INTERRUPT_TTS.getAction());
        map.put("mode combat", ACTIVATE_COMBAT_MODE.getAction());
        map.put("mode analyse", ACTIVATE_ANALYSIS_MODE.getAction());
        map.put("vision nocturne", NIGHT_VISION_ON_OFF.getAction());
        map.put("lumières", LIGHTS_ON_OFF.getAction());
        map.put("soute à cargo", TOGGLE_CARGO_SCOOP.getAction());
        map.put("carte galaxie", OPEN_GALAXY_MAP.getAction());
        map.put("carte système locale", OPEN_SYSTEM_MAP.getAction());
        map.put("feu à volonté", FIGHTER_OPEN_ORDERS.getAction());
        map.put("prochain ennemi", SELECT_HIGHEST_THREAT.getAction());
        map.put("pleine poussée", SET_SPEED100.getAction());
        map.put("vitesse moyenne", SET_SPEED50.getAction());
        map.put("vitesse lente", SET_SPEED25.getAction());
        map.put("vitesse optimale", SET_OPTIMAL_SPEED.getAction());
    }
}
