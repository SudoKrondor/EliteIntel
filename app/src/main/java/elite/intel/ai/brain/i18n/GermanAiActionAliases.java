package elite.intel.ai.brain.i18n;

import elite.intel.session.Status;

import java.util.Map;
import java.util.Set;

import static elite.intel.ai.brain.actions.Commands.*;
import static elite.intel.ai.brain.actions.Queries.*;

public class GermanAiActionAliases implements AiActionAliasProvider {

    @Override
    public Set<String> wakeBypassPhrases() {
        return Set.of("wach auf", "hör zu", "hör mir zu", "aktiviere dich");
    }

    @Override
    public Set<String> listenBypassPrefixes() {
        return Set.of("hör mir zu", "hör zu");
    }

    @Override
    public void addAliases(Map<String, String> map, Status status, boolean isDryRun) {
        // always available
        map.put("wach auf, hör zu, hör mir zu, aktiviere dich", WAKEUP.getAction());
        map.put("schlaf, geh schlafen, ignoriere mich, nicht zuhören, nicht überwachen", SLEEP.getAction());
        map.put("unterbrich, unterbrechen, sprache stoppen, hör auf zu reden", INTERRUPT_TTS.getAction());

        // navigation
        map.put("handelsroute abbrechen, handelsroute stoppen, handelsroute löschen, handelsroute stornieren", CANCEL_TRADE_ROUTE.getAction());
        map.put("navigiere zu koordinaten {lat:X, lon:Y}, kurs auf koordinaten {lat:X, lon:Y}, fliege zu koordinaten {lat:X, lon:Y}", NAVIGATE_TO_TARGET.getAction());
        map.put("navigiere zur aktiven mission, route zur aktiven mission planen, route zur mission planen, bring mich zur mission, fliege zur mission {key:X}", NAVIGATE_TO_NEXT_MISSION.getAction());
        map.put("navigiere zum fleet carrier, fliege zum carrier, kurs zum carrier, zurück zum carrier, bring uns zum carrier, zurück zum träger", NAVIGATE_TO_FLEET_CARRIER.getAction());
        map.put("navigiere zur landezone, kurs zur landezone, peilung zur landezone, zurück zur landezone, zurück zur lz", GET_HEADING_TO_LZ.getAction());
        map.put("navigiere zum nächsten handelsstopp, fliege zum nächsten handelsstopp, nächster handelsstopp", NAVIGATE_TO_NEXT_TRADE_STOP.getAction());
        map.put("navigiere aus dem speicher, aus speicher einfügen, adresse aus speicher verwenden", NAVIGATE_TO_ADDRESS_FROM_MEMORY.getAction());
        map.put("navigation abbrechen, route abbrechen, navigation stoppen, route löschen", NAVIGATION_OFF.getAction());
        map.put("heimatsystem setzen, aktuelles system als heimatsystem setzen, heimatsystem markieren", SET_HOME_SYSTEM.getAction());
        map.put("bring mich nach hause, fliege nach hause, navigiere nach hause, zurück nach hause, route nach hause planen, kurs nach hause", TAKE_ME_HOME.getAction());
        map.put("head look zurücksetzen, kopfansicht zurücksetzen, head look auf standard, head look auf neutral, blickrichtung zurücksetzen", RESET_HEAD_LOOK.getAction());

        if (status.isInMainShip() || isDryRun) {
            // navigation
            map.put("fsd ziel auswählen, ziel auswählen, ziel setzen, sprungziel auswählen, routenziel auswählen", TARGET_DESTINATION.getAction());
            map.put("sprung in den hyperraum, spring, hypersprung, hyperraumsprung, in den hyperraum, los gehts, nächster wegpunkt", JUMP_TO_HYPERSPACE.getAction());
            map.put("aus dem supercruise fallen, hier rausfallen, supercruise verlassen, supercruise beenden, raus hier", DROP_FROM_SUPER_CRUISE.getAction());
            map.put("in supercruise gehen, supercruise, supercruise aktivieren", ENTER_SUPER_CRUISE.getAction());
            map.put("schiff starten, starten, abdocken, station verlassen, hafen verlassen", LAUNCH_SHIP.getAction());
            map.put("schildzelle einsetzen, schildzelle verwenden, schildzelle aktivieren, schildzellenbank, energiezelle einsetzen, schildzelle auslösen", DEPLOY_SHIELD_CELL.getAction());
            map.put("täuschkörper einsetzen, täuschkörper abwerfen, täuschkörper verwenden, täuschkörper auslösen, leuchtraketen abwerfen, leuchtraketen einsetzen", DEPLOY_CHAFF.getAction());
            // speed / throttle
            map.put("triebwerke stoppen, stopp, voller stopp, halt, triebwerke aus, schub auf null, null schub, schiff stoppen", SET_SPEED_ZERO.getAction());
            map.put("taxi zur landung, taxi, automatisch landen, autopilot landung, autolandung", TAXI.getAction());
            map.put("viertel schub, fünfundzwanzig prozent, langsame geschwindigkeit, ein viertel", SET_SPEED25.getAction());
            map.put("halber schub, fünfzig prozent, halbe geschwindigkeit", SET_SPEED50.getAction());
            map.put("drei viertel schub, fünfundsiebzig prozent, drei viertel geschwindigkeit", SET_SPEED75.getAction());
            map.put("voller schub, hundert prozent, volle geschwindigkeit, maximale geschwindigkeit, maximum schub", SET_SPEED100.getAction());
            map.put("geschwindigkeit erhöhen um {key:X}, erhöhe geschwindigkeit um {key:X}, schneller um {key:X}", INCREASE_SPEED_BY.getAction());
            map.put("geschwindigkeit verringern um {key:X}, reduziere geschwindigkeit um {key:X}, langsamer um {key:X}", DECREASE_SPEED_BY.getAction());
            map.put("optimale geschwindigkeit setzen, optimale anfluggeschwindigkeit, anfluggeschwindigkeit optimieren", SET_OPTIMAL_SPEED.getAction());
            map.put("fahrwerk, fahrwerk runter, fahrwerk ausfahren, landegear ausfahren, landefahrwerk ausfahren", DEPLOY_LANDING_GEAR.getAction());
            map.put("fahrwerk einfahren, fahrwerk hoch, fahrwerk rein, landefahrwerk einfahren", RETRACT_LANDING_GEAR.getAction());
            map.put("landeerlaubnis anfragen, andocken anfragen, docking anfragen, landung anfragen, parkplatz anfragen, landeplatz anfragen", REQUEST_DOCKING.getAction());

            // UI panels
            map.put("jäger panel anzeigen, jäger panel öffnen, fighter panel öffnen", SHOW_FIGHTER_PANEL.getAction());

            // combat
            map.put("waffen ausfahren, hardpoints ausfahren, waffen heiß, kampfbereit, waffen bereit, bewaffnen", DEPLOY_HARDPOINTS.getAction());
            map.put("waffen einfahren, hardpoints einfahren, waffen kalt, waffen weg, zurücktreten, waffen sichern", RETRACT_HARDPOINTS.getAction());
            map.put("ziel fsd {key:fsd}, ziel triebwerke {key:drive}, ziel energieverteiler {key:power distributor}, ziel kraftwerk {key:powerplant}, ziel lebenserhaltung {key:life support}", TARGET_SUB_SYSTEM.getAction());
            map.put("wingman eins anvisieren, wingman alpha", TARGET_WINGMAN0.getAction());
            map.put("wingman zwei anvisieren, wingman bravo", TARGET_WINGMAN1.getAction());
            map.put("wingman drei anvisieren, wingman charlie", TARGET_WINGMAN2.getAction());
            map.put("wing nav lock, navigationsbindung zum wingman, wingman folgen", WING_NAV_LOCK.getAction());
            map.put("höchste bedrohung anvisieren, gefährlichstes ziel, feind auswählen, nächster feind, prioritätsziel", SELECT_HIGHEST_THREAT.getAction());

            // vehicle deployment
            map.put("srv ausfahren, srv starten, fahrzeug ausfahren, fahrzeug deployen, srv deployen", DEPLOY_SRV.getAction());
            map.put("heat sink abwerfen, kühlkörper auslösen, wärmesenke starten, hitze abwerfen", DEPLOY_HEAT_SINK.getAction());

            // fighter orders
            map.put("jäger starten, jäger aussetzen, fighter starten, fighter deployen", DEPLOY_FIGHTER.getAction());
            map.put("jäger verteidige das schiff, fighter defensiv, jäger defensiv", FIGHTER_REQUEST_DEFENSIVE_BEHAVIOUR.getAction());
            map.put("jäger greife mein ziel an, fighter greif mein ziel an, fokus auf mein ziel, ziel fokussieren", FIGHTER_REQUEST_FOCUS_TARGET.getAction());
            map.put("jäger feuer einstellen, fighter feuer halten, feuer einstellen", FIGHTER_REQUEST_HOLD_FIRE.getAction());
            map.put("jäger zurück zum schiff, fighter andocken, jäger zurückrufen", FIGHTER_REQUEST_REQUEST_DOCK.getAction());
            map.put("jäger feuer frei, fighter feuer frei, nach eigenem ermessen angreifen", FIGHTER_OPEN_ORDERS.getAction());
            map.put("wähle feuergruppe {key:X}, feuergruppe {key:X}, wechsle zu feuergruppe {key:X}", SELECT_FIRE_GROUP_BY_NATO.getAction());
        }

        if (status.isInMainShip() && !status.isDocked() || isDryRun) {
            map.put("stationen im system, welche stationen, nahe stationen, raumhäfen, space stations, andocken möglich", QUERY_STATIONS.getAction());
        }

        if (status.isInSrv() && status.isDocked() || isDryRun) {
            map.put("services panel anzeigen, service panel öffnen, stationsdienste anzeigen, stationsservice öffnen", SHOW_STATION_SERVICES.getAction());
        }

        if (status.isInMainShip() || status.isInSrv() || isDryRun) {
            // flight / ship systems
            map.put("in den kampfmodus wechseln, kampfmodus aktivieren", ACTIVATE_COMBAT_MODE.getAction());
            map.put("in den analysemodus wechseln, analysemodus aktivieren", ACTIVATE_ANALYSIS_MODE.getAction());
            map.put("frachtschaufel, frachtschaufel öffnen, frachtschaufel schließen, cargo scoop öffnen, cargo scoop schließen, frachtluke öffnen, frachtluke schließen", TOGGLE_CARGO_SCOOP.getAction());
            map.put("nachtsicht, nachsicht an, nachsicht aus, nachtsicht einschalten, nachtsicht ausschalten {state:true/false}", NIGHT_VISION_ON_OFF.getAction());
            map.put("scheinwerfer, licht, lichter, licht einschalten, licht ausschalten, schiffslicht, lichter an, lichter aus {state:true/false}", LIGHTS_ON_OFF.getAction());

            // UI panels
            map.put("commander panel anzeigen, commander panel öffnen, zentrales panel, rollen panel, kneeboard öffnen", SHOW_COMMANDER_PANEL.getAction());
            map.put("crew panel anzeigen, crew panel öffnen, besatzung anzeigen", SHOW_CREW.getAction());
            map.put("home panel anzeigen, internes panel öffnen, heim panel öffnen", SHOW_INTERNAL_PANEL.getAction());
            map.put("module panel anzeigen, module öffnen, module anzeigen", SHOW_MODULES_PANEL.getAction());
            map.put("feuergruppen anzeigen, feuergruppen öffnen, fire groups öffnen", SHOW_FIRE_GROUPS.getAction());
            map.put("inventar anzeigen, inventar öffnen", SHOW_INVENTORY_PANEL.getAction());
            map.put("lager anzeigen, storage panel öffnen, speicher öffnen", SHOW_STORAGE_PANEL.getAction());

            // power
            map.put("energie auf schilde, maximale schilde, schilde verstärken", INCREASE_SHIELDS_POWER.getAction());
            map.put("energie auf triebwerke, maximale triebwerke, triebwerke verstärken", INCREASE_ENGINES_POWER.getAction());
            map.put("energie auf waffen, maximale waffen, waffen verstärken", INCREASE_WEAPONS_POWER.getAction());

            // vehicle deployment
            map.put("aussteigen, schiff verlassen, von bord gehen", DISEMBARK.getAction());
            map.put("energie ausgleichen, energie balancieren, energie zurücksetzen, energie gleichmäßig verteilen", RESET_POWER.getAction());
        }

        if (status.isInSrv() || isDryRun) {
            map.put("fahrassistenz, drive assist, srv assist {state:true/false}", DRIVE_ASSIST.getAction());
            map.put("srv bergen, zurück ins schiff, srv zurückholen, srv andocken", RECOVER_SRV.getAction());
        }

        if (status.isInSrv() || status.isOnFoot() || isDryRun) {
            map.put("schiff wegschicken, schiff in den orbit, schiff fortschicken", DISMISS_SHIP.getAction());
            map.put("zur oberfläche zurückkehren, hol mich ab, schiff zurückrufen", RETURN_TO_SURFACE.getAction());
        }

        // market / traders / brokers
        map.put("rohmaterialhändler finden, raw material trader finden, wo rohmaterialien tauschen {key:X}", FIND_RAW_MATERIAL_TRADER.getAction());
        map.put("datenhändler finden, encoded material trader finden, encoded trader, wo daten tauschen {key:X}", FIND_ENCODED_MATERIAL_TRADER.getAction());
        map.put("hergestellte materialien händler finden, manufactured material trader finden, manufactured trader {key:X}", FIND_MANUFACTURED_MATERIAL_TRADER.getAction());
        map.put("human tech broker finden, menschlichen technologiebroker finden {key:X}", FIND_HUMAN_TECHNOLOGY_BROKER.getAction());
        map.put("guardian tech broker finden, guardian technologiebroker finden, wächter tech broker {key:X}", FIND_GUARDIAN_TECHNOLOGY_BROKER.getAction());
        map.put("ware finden, nächste ware finden, ware kaufen, wo kaufen, markt finden {key:X, max_distance:Y, state:true/false}", FIND_COMMODITY.getAction());
        map.put("nächsten fleet carrier finden, nächsten carrier finden, nächsten träger finden", FIND_NEAREST_FLEET_CARRIER.getAction());

        // fleet carrier
        map.put("carrier treibstoffreserve setzen, carrier tritium reserve setzen {key:X}", SET_CARRIER_FUEL_RESERVE.getAction());
        map.put("fleet carrier route berechnen, carrier route planen, carrier sprungroute planen", CALCULATE_FLEET_CARRIER_ROUTE.getAction());
        map.put("carrier ziel eingeben, carrier ziel setzen, carrier ziel", ENTER_FLEET_CARRIER_DESTINATION.getAction());

        // squadron carrier
        map.put("squadron carrier route, squadron carrier navigation, squadron carrier sprungroute, wie viele sprünge auf der squadron carrier route, verbleibende squadron carrier sprünge", SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put("wohin fliegt der squadron carrier, wohin ist der squadron carrier unterwegs, squadron carrier ziel, endziel des squadron carriers", SQUADRON_CARRIER_ROUTE.getAction());
        map.put("squadron carrier tritium, squadron carrier treibstoff, squadron tritium vorrat, squadron carrier treibstoffstand, squadron carrier tritium status", SQUADRON_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put("squadron carrier status, squadron carrier finanzen, squadron carrier balance, squadron carrier übersicht, squadron carrier kontostand, wie lange kann der squadron carrier betrieben werden, squadron carrier treibstoffstatus", SQUADRON_CARRIER_STATUS.getAction());
        map.put("squadron carrier eta, wann kommt der squadron carrier an, wie lange bis squadron carrier ankunft, squadron carrier ankunft, squadron carrier ankunftszeit", SQUADRON_CARRIER_ETA.getAction());

        // trade
        map.put("handelsroute berechnen, trade route berechnen", CALCULATE_TRADE_ROUTE.getAction());
        map.put("handelsroutenparameter auflisten, handelsparameter anzeigen", LIST_TRADE_ROUTE_PARAMETERS.getAction());
        map.put("route monetarisieren, route gewinn berechnen, route profit berechnen", MONETIZE_ROUTE.getAction());

        map.put("handelsprofil startbudget ändern {key:X}, startbudget handelsprofil ändern {key:X}", CHANGE_TRADE_PROFILE_SET_STARTING_BUDGET.getAction());
        map.put("handelsprofil maximale stopps ändern {key:X}, max stopps handelsprofil ändern {key:X}", CHANGE_TRADE_PROFILE_SET_MAX_NUMBER_OF_STOPS.getAction());
        map.put("handelsprofil maximale entfernung ändern {key:X}, max entfernung handelsprofil ändern {key:X}", CHANGE_TRADE_PROFILE_SET_MAX_DISTANCE_FROM_ENTRY.getAction());
        map.put("verbotene waren im handelsprofil erlauben {state:true/false}, illegale ware erlauben {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PROHIBITED_CARGO.getAction());
        map.put("planetare häfen im handelsprofil erlauben {state:true/false}, planetenhäfen erlauben {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PLANETARY_PORT.getAction());
        map.put("permit systeme im handelsprofil erlauben {state:true/false}, erlaubnissysteme erlauben {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PERMIT_SYSTEMS.getAction());
        map.put("strongholds im handelsprofil erlauben {state:true/false}, festungen erlauben {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_STRONGHOLDS.getAction());

        // announcements / app settings
        map.put("radio umschalten, funktrafik, funkübertragungen {state:true/false}", SET_RADIO_TRANSMISSION_MODE.getAction());
        map.put("radarkontakt ansagen {state:true/false}, radar kontakt ansage {state:true/false}", SET_RADAR_CONTACT_ANNOUNCEMENT.getAction());
        map.put("entdeckungsansagen {state:true/false}, discovery ansagen {state:true/false}", DISCOVERY_ON_OFF.getAction());
        map.put("routenansagen {state:true/false}, route ansagen {state:true/false}", ROUTE_ON_OFF.getAction());
        map.put("alle ansagen umschalten {state:true/false}, alle ankündigungen {state:true/false}", TOGGLE_ALL_ANNOUNCEMENTS.getAction());
        map.put("erinnerungen löschen, erinnerungen zurücksetzen", CLEAR_REMINDERS.getAction());
        map.put("erinnerung setzen {key:X}, erinnere mich {key:X}", SET_REMINDER.getAction());
        map.put("erinnere mich in {minutes:X} minuten {key:Y}, timer für {minutes:X} minuten {key:Y}, zeitgesteuerte erinnerung {minutes:X} minuten {key:Y}", SET_TIMED_REMINDER.getAction());
        map.put("Erkunde das System, scanne das System, scanne das System", HONK_THE_SYSTEM.getAction());
        // UI panels
        map.put("aktivieren, aktiviere", ACTIVATE.getAction());
        map.put("transaktionen anzeigen, transaktionspanel öffnen", SHOW_TRANSACTIONS.getAction());
        map.put("kontakte anzeigen, kontakte öffnen, kontaktpanel öffnen", SHOW_CONTACTS.getAction());
        map.put("navigation anzeigen, navigationspanel öffnen", SHOW_NAVIGATION.getAction());
        map.put("chat anzeigen, chat öffnen, comms panel öffnen, kommunikation öffnen", SHOW_CHAT_PANEL.getAction());
        map.put("posteingang anzeigen, inbox öffnen, e-mail öffnen, nachrichten öffnen", SHOW_INBOX_PANEL.getAction());
        map.put("social panel anzeigen, sozial panel öffnen", SHOW_SOCIAL_PANEL.getAction());
        map.put("verlauf anzeigen, history panel öffnen, historie anzeigen", SHOW_HISTORY_PANEL.getAction());
        map.put("staffel anzeigen, squadron panel öffnen, squadron anzeigen", SHOW_SQUADRON.getAction());
        map.put("status anzeigen, status panel öffnen", SHOW_STATUS_PANEL.getAction());
        map.put("carrier management anzeigen, trägerverwaltung öffnen, carrier verwaltung öffnen", DISPLAY_CARRIER_MANAGEMENT.getAction());
        map.put("galaxiekarte öffnen, galaxiekarte anzeigen, galaxy map öffnen", OPEN_GALAXY_MAP.getAction());
        map.put("systemkarte öffnen, lokale karte öffnen, system map öffnen", OPEN_SYSTEM_MAP.getAction());
        map.put("panel schließen, schließen, raus, verlassen", EXIT_CLOSE.getAction());

        // pirate massacre missions
        map.put("navigiere zum missionsgeber system, kurs zum system des missionsgebers", RECON_PROVIDER_SYSTEM.getAction());
        map.put("navigiere zum piraten missionsgeber, fliege zum piraten missionsgeber", NAVIGATE_TO_PIRATE_MISSION_PROVIDER.getAction());
        map.put("aktive missionen, aktuelle missionen, missionslog, welche missionen, missionsstatus, unsere missionen, laufende missionen", ANALYZE_MISSIONS.getAction());
        map.put("piratenmission, kill count, wie viele kills, massacre mission fortschritt, verbleibende kills, verbleibende piraten, piraten kills", PIRATE_MISSION_PROGRESS.getAction());
        map.put("jagdgebiet finden {key:X}, hunting grounds finden {key:X}, ort zum jagen finden {key:X}", FIND_HUNTING_GROUNDS.getAction());
        map.put("jagdgebiet erkunden, zum zielsystem navigieren, zum jagdgebiet navigieren", RECON_TARGET_SYSTEM.getAction());
        map.put("jagdgebiet ignorieren, hunting ground ignorieren", IGNORE_HUNTING_GROUND.getAction());
        map.put("jagdgebiet bestätigen, zielsystem bestätigen", CONFIRM_HUNTING_GROUND.getAction());

        // science / mining / biology
        map.put("mining ziel hinzufügen {key:X}, abbauziel hinzufügen {key:X}", ADD_MINING_TARGET.getAction());
        map.put("mining ziel entfernen {key:X}, abbauziel entfernen {key:X}", REMOVE_MINING_TARGET.getAction());
        map.put("mining ziele löschen, abbauziele löschen", CLEAR_MINING_TARGETS.getAction());
        map.put("mining ansagen {state:true/false}, abbau ansagen {state:true/false}", MINING_ON_OFF.getAction());
        map.put("brain trees finden {key:X, max_distance:Y}, gehirnbäume finden {key:X, max_distance:Y}", FIND_BRAIN_TREES.getAction());
        map.put("mining site finden, mining ort finden, hotspot finden, wo abbauen, asteroidenfeld finden {key:X, max_distance:Y}", FIND_MINING_SITE.getAction());
        map.put("tritium mining site finden, tritium feld finden, tritium hotspot finden {key:X, max_distance:Y}", FIND_FLEET_CARRIER_FUEL_MINING_SITE.getAction());
        map.put("zum nächsten bio sample navigieren, zur nächsten probe, zum nächsten organismus, codex eintrag, zum codex eintrag navigieren", NAVIGATE_TO_NEXT_BIO_SAMPLE.getAction());
        map.put("system scannen, fss öffnen, vollständiger scan, honk, discovery scan, full spectrum scan, systemscan", OPEN_FSS_AND_SCAN.getAction());
        map.put("nächste vista genomics finden, vista genomics finden, genomics finden", FIND_VISTA_GENOMICS.getAction());
        map.put("codex eintrag löschen, diesen codex löschen, diesen eintrag löschen, diesen organismus löschen", DELETE_CODEX_ENTRY.getAction());

        map.put("key bindings prüfen, fehlende tastenbelegung, unbelegte tasten, tastaturbelegung, bindings prüfen, fehlende bindings", KEY_BINDINGS_ANALYSIS.getAction());
        map.put("bio signale im system erledigt, organik im system gescannt, wie viele bio samples im system, bio signale im system, welche planeten haben biosignale, welche planeten müssen noch gescannt werden, bio scan fortschritt", BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
        map.put("exobiologie proben, biologische proben, organische proben am ort, welche organismen, was bleibt zu scannen, verbleibende organismen, exobiologie fortschritt, was wurde hier gescannt, organik auf diesem planeten", EXOBIOLOGY_SAMPLES.getAction());
        map.put("entfernung zur letzten bio probe, wie weit zur probe, entfernung zum letzten organismus, reichweite zur bio probe, zur bio probe navigieren", DISTANCE_TO_LAST_BIO_SAMPLE.getAction());
        map.put("biom analysieren, biom analyse, welches biom {key:X}, planetenbiom, atmosphärenanalyse, welches leben ist hier, biom typ", PLANET_BIOME_ANALYSIS.getAction());
        map.put("stellare objekte, planeten im system, landbare planeten, ist planet oder mond landbar, körper im system, welche planeten, wie viele planeten, systemkörper, ringe, eisringe", QUERY_STELLAR_OBJETS.getAction());
        map.put("signale im system, welche signale sind im system, was ist im system, was wurde erkannt, fss signale, mining hotspots, resource extraction sites, konfliktzonen, emissionen, unbekannte signale", QUERY_STELLAR_SIGNALS.getAction());
        map.put("geosignale, geologische signale, vulkanische signale, geologische aktivität, vulkanische aktivität", QUERY_GEO_SIGNALS.getAction());

        map.put("fleet carrier im system, carrier im system, wie viele carrier, träger im system, carrier in der nähe", QUERY_CARRIERS.getAction());
        map.put("carrier route, carrier navigation, carrier sprungroute, carrier reise, wie viele sprünge auf der carrier route, verbleibende carrier sprünge", FLEET_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put("carrier route, wohin fliegt der carrier, nächster carrier sprung, wohin ist der carrier unterwegs, carrier ziel, endziel des carriers", FLEET_CARRIER_ROUTE.getAction());
        map.put("carrier tritium, carrier treibstoff, wie viel tritium, tritium vorrat, tritium stand, carrier fuel level", FLEET_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put("carrier status, carrier finanzen, carrier balance, carrier übersicht, carrier kontostand, wie lange kann der carrier betrieben werden, carrier reichweite", FLEET_CARRIER_STATUS.getAction());
        map.put("carrier eta, wann kommt der carrier an, wie lange bis carrier ankunft, carrier ankunft, wann springt der carrier, carrier sprungzeit", FLEET_CARRIER_ETA.getAction());
        map.put("entfernung zum carrier, wo ist unser carrier, wie weit ist der carrier weg, distanz zum carrier, carrier nähe", DISTANCE_TO_CARRIER.getAction());
        map.put("systemsicherheit, fraktionskontrolle, wer kontrolliert das system, sicherheitsstufe, wem gehört das system, dominante fraktion, kontrollierende macht", SYSTEM_SECURITY_ANALYSIS.getAction());
        map.put("handelsprofil, handelseinstellungen, handelsparameter, handelskonfiguration, handelskriterien", TRADE_PROFILE_ANALYSIS.getAction());
        map.put("entfernung zum stellaren objekt, wie weit zum körper, entfernung zum planeten {key:X}, entfernung zum mond, wie weit zur station, distanz zum körper", DISTANCE_TO_BODY.getAction());
        map.put("letzter scan, was haben wir gescannt, zuletzt gescanntes objekt, jüngster scan, aktueller scan", LAST_SCAN_ANALYSIS.getAction());
        map.put("material inventar {key:X}, wie viele items {key:X}, wie viel material {key:X}, haben wir material {key:X}, wie viel {key:X} haben wir, engineering material {key:X}, rohmaterial {key:X}, manufactured material {key:X}, encoded material {key:X}", MATERIALS_INVENTORY.getAction());
        map.put("planetenmaterialien, materialien hier, welche materialien auf diesem planeten, oberflächenmaterialien, materialvorkommen, mineralien auf dem planeten", PLANET_MATERIALS.getAction());
        map.put("explorationsgewinn, entdeckungsgewinn, wie viel ist exploration wert, scan wert, kartierungsgewinn, exobiologie wert", EXPLORATION_PROFITS.getAction());
        map.put("aktueller standort, wo sind wir, unsere position, in welchem system sind wir, wo bin ich, unsere koordinaten, aktuelles system, auf welchem planeten sind wir, aktuelle position, tageslänge", CURRENT_LOCATION.getAction());
        map.put("in welchem system bin ich, wo ist mein schiff, wo sind wir momentan, unser aktueller ort, was ist mein aktueller aufenthaltsort", CURRENT_LOCATION.getAction());
        map.put("fsd ziel info, ziel analysieren, welchen stern visieren wir an, fsd ziel analysieren, information zum fsd ziel", FSD_TARGET_ANALYSIS.getAction());
        map.put("geplante route, treibstoff am nächsten stopp, treibstoffverfügbarkeit auf der route, routenanalyse, sind wir schon da, aktuelle route, wie viele sprünge übrig, nächste tankbare sonne", PLOTTED_ROUTE_ANALYSIS.getAction());
        map.put("handelsroute, aktuelle handelsroute, aktueller handelsplan, womit handeln wir, unser handelsplan, handelsabschnitte", TRADE_ROUTE_ANALYSIS.getAction());
        map.put("outfitting, schiffsupgrades, verfügbare module, welche module auf station, verfügbare ausrüstung, module kaufen, schiffsteile", LOCAL_OUTFITTING.getAction());
        map.put("werft, schiffe zum verkauf, welche schiffe auf station, schiff kaufen, verfügbare schiffe, neues schiff", LOCAL_SHIPYARD.getAction());
        map.put("was ist im frachtraum, was tragen wir, frachtinhalt, waren an bord, was transportieren wir, laderaum inhalt", CARGO_HOLD_CONTENTS.getAction());
        map.put("spielerprofil, commander profil, kommandantenprofil", PLAYER_PROFILE_ANALYSIS.getAction());
        map.put("schiff loadout, schadensbericht, schiffsmodule, kampfbereitschaft, schiffsausrüstung, schiffsdaten, was fliege ich, was ist ausgerüstet, schildgenerator, hüllenverstärkung, sensoren, triebwerke, frameshift, fuelscoop", SHIP_LOADOUT.getAction());
        map.put("stationsdetails, welche services hier, welche dienste hier, services auf station, was bietet die station, stationsinfo, stationseinrichtungen, was gibt es auf dieser station, verfügbare services", STATION_DETAILS.getAction());
        map.put("an welcher station bin ich, an welcher station sind wir, wo bin ich angedockt, wo sind wir angedockt, was ist meine aktuelle station, welche station ist das, aktuelle station", STATION_DETAILS.getAction());
        map.put("kopfgelder, gesamte kopfgelder, bounty gesammelt, wie viel kopfgeld, bounty ertrag, credits aus kopfgeldern", TOTAL_BOUNTIES.getAction());
        map.put("entfernung zur bubble, entfernung zu sol, entfernung von sol, entfernung zur erde, wie weit von sol, wie weit von der bubble, wie weit von zivilisation, entfernung vom bewohnten raum", DISTANCE_TO_BUBBLE.getAction());
        map.put("aktuelle zeit, wie spät ist es, zeit auf der erde, galaktische zeit, utc zeit, echte zeit", TIME_IN_ZONE.getAction());
        map.put("erinnerung, was war die erinnerung, ziel erinnerung, gibt es erinnerungen, welche erinnerung haben wir gesetzt", REMINDER.getAction());
        map.put("lokale märkte, märkte auf stationen und siedlungen, märkte auf außenposten im system", ANALYZE_MARKETS.getAction());
    }
}
