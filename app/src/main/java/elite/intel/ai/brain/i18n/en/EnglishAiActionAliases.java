package elite.intel.ai.brain.i18n.en;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;
import elite.intel.session.Status;

import java.util.Map;
import java.util.Set;

import static elite.intel.ai.brain.actions.Commands.*;
import static elite.intel.ai.brain.actions.Queries.*;

public class EnglishAiActionAliases implements AiActionAliasProvider {

    @Override
    public Set<String> wakeBypassPhrases() {
        return Set.of("wake", "wake up", "listen", "listen up");
    }

    @Override
    public Set<String> listenBypassPrefixes() {
        return Set.of("listen up", "listen");
    }

    @Override
    public void addAliases(Map<String, String> map, Status status, boolean isDryRun) {

        // always available
        map.put("wake up", WAKEUP.getAction());
        map.put("sleep, go to sleep, ignore me, do not monitor", SLEEP.getAction());
        map.put("interrupt", INTERRUPT_TTS.getAction());

        // Declared early so these win before power-management "balance power" and carrier-route entries can interfere
        map.put("find nearest fleet carrier, nearest fleet carrier, find nearest carrier", FIND_NEAREST_FLEET_CARRIER.getAction());
        map.put("carrier status, how long can we operate carrier, how far can carrier jump, carrier jump range with current tritium, carrier jump range", FLEET_CARRIER_STATUS.getAction());
        map.put("squadron carrier status, how long can we operate squadron carrier", SQUADRON_CARRIER_STATUS.getAction());

        // navigation
        map.put("cancel trade route, stop trade route, clear trade route, abort trade route", CANCEL_TRADE_ROUTE.getAction());
        map.put("navigate to coordinates {lat:X, lon:Y}", NAVIGATE_TO_TARGET.getAction());
        map.put("navigate to active mission, plot route to active mission, plot route to mission, take me to mission, go to mission {key:X}", NAVIGATE_TO_NEXT_MISSION.getAction());
        map.put("navigate to fleet carrier, go to carrier, head to carrier, return to carrier, take us to carrier", NAVIGATE_TO_FLEET_CARRIER.getAction());
        map.put("navigate to landing zone, bearing to landing zone, heading to landing zone, back to LZ", GET_HEADING_TO_LZ.getAction());
        map.put("navigate to next trade stop, go to next trade stop", NAVIGATE_TO_NEXT_TRADE_STOP.getAction());
        map.put("navigate from memory, paste from memory", NAVIGATE_TO_ADDRESS_FROM_MEMORY.getAction());
        map.put("cancel navigation, abort navigation, stop navigation", NAVIGATION_OFF.getAction());
        map.put("set home system, set current system as home, mark home system", SET_HOME_SYSTEM.getAction());
        map.put("take me home, go home, navigate home, return home, plot route home, head home, take us home", TAKE_ME_HOME.getAction());
        map.put("reset, reset head look, reset head look to default, reset head look to neutral", RESET_HEAD_LOOK.getAction());
        if (status.isInMainShip() || isDryRun) {
            // navigation
            map.put("select fds destination, target destination, set destination, select target destination", TARGET_DESTINATION.getAction());
            map.put("jump to hyperspace,  jump, hyperspace jump, enter hyperspace, lets go, next way point, let's go", JUMP_TO_HYPERSPACE.getAction());
            map.put("drop out, drop here, drop ftl, drop from supercruise, leave supercruise, drop in", DROP_FROM_SUPER_CRUISE.getAction());
            map.put("enter supercruise, supercruise, go supercruise", ENTER_SUPER_CRUISE.getAction());
            map.put("launch ship, launch, detach from station, leave port, leave station", LAUNCH_SHIP.getAction());
            // speed / throttle
            map.put("stop engines, stop here, full stop, all stop, halt, kill engines, cut throttle, zero throttle, stop ship", SET_SPEED_ZERO.getAction());
            map.put("taxi to landing, taxi, auto land, autopilot landing", TAXI.getAction());
            map.put("quarter throttle, 25 percent, slow speed, one quarter", SET_SPEED25.getAction());
            map.put("half throttle, 50 percent, half speed", SET_SPEED50.getAction());
            map.put("three quarters throttle, 75 percent, three quarter speed", SET_SPEED75.getAction());
            map.put("full throttle, 100 percent, full speed, maximum speed, max throttle", SET_SPEED100.getAction());
            map.put("increase speed by {key:X}", INCREASE_SPEED_BY.getAction());
            map.put("decrease speed by {key:X}", DECREASE_SPEED_BY.getAction());
            map.put("set optimal speed, optimal approach speed, optimize approach speed", SET_OPTIMAL_SPEED.getAction());
            map.put("landing gear, gear down, lower landing gear, extend landing gear", DEPLOY_LANDING_GEAR.getAction());
            map.put("retract landing gear, gear up, raise landing gear, stow landing gear", RETRACT_LANDING_GEAR.getAction());
            map.put("request docking, dock at station, request landing, docking request, ask for docking, request parking, parking spot, request pad, park ship", REQUEST_DOCKING.getAction());
            // UI panels
            map.put("show, open or display fighter panel", SHOW_FIGHTER_PANEL.getAction());
            map.put("deploy shield cell, use shield cell, activate shield cell, shield cell bank, deploy power cell, fire shield cell", DEPLOY_SHIELD_CELL.getAction());
            map.put("deploy chaff, launch chaff, use chaff, fire chaff, launch flares, deploy flares", DEPLOY_CHAFF.getAction());
            // combat
            map.put("deploy hardpoints, weapons hot, combat ready, weapons free, weapons out, arm weapons, weapons ready", DEPLOY_HARDPOINTS.getAction());
            map.put("retract hardpoints, weapons cold, weapons away, stand down, holster weapons, weapons down, safe weapons", RETRACT_HARDPOINTS.getAction());
            map.put("target fsd {key:fsd}, target engines {key:drive}, target Power Distributor {key:power distributor} target power plant {key:powerplant}, target powerplant {key:powerplant}, target life support {key:life support}, ", TARGET_SUB_SYSTEM.getAction());
            map.put("target wingman 1, wingman alpha", TARGET_WINGMAN0.getAction());
            map.put("target wingman 2, wingman bravo", TARGET_WINGMAN1.getAction());
            map.put("target wingman 3, wingman charlie", TARGET_WINGMAN2.getAction());
            map.put("wing nav lock, lock wingman nav, follow wingman", WING_NAV_LOCK.getAction());
            map.put("priority target, target highest threat, target most dangerous, select hostile, next enemy, select enemy", SELECT_HIGHEST_THREAT.getAction());
            // vehicle deployment
            map.put("deploy SRV, deploy vehicle, launch SRV, send out SRV, drop SRV", DEPLOY_SRV.getAction());
            map.put("deploy heat sink, launch heat sink, dump heat", DEPLOY_HEAT_SINK.getAction());
            // fighter orders
            map.put("deploy fighter, launch fighter, send out fighter", DEPLOY_FIGHTER.getAction());
            map.put("order fighter defend ship, fighter defend, fighter defensive", FIGHTER_REQUEST_DEFENSIVE_BEHAVIOUR.getAction());
            map.put("order fighter attack my target, fighter attack, sic fighter on target, focus my target, focus target, focus on target, fighter focus target", FIGHTER_REQUEST_FOCUS_TARGET.getAction());
            map.put("order fighter hold fire, fighter cease fire, fighter stand down", FIGHTER_REQUEST_HOLD_FIRE.getAction());
            map.put("order fighter return to ship, fighter dock, recall fighter", FIGHTER_REQUEST_REQUEST_DOCK.getAction());
            map.put("fighter open orders, fire at will, attack at will", FIGHTER_OPEN_ORDERS.getAction());
            map.put("select fire group {key:X}, fire group {key:X}, switch to fire group {key:X}", SELECT_FIRE_GROUP_BY_NATO.getAction());
        }

        if (status.isInMainShip() && !status.isDocked() || isDryRun) {
            map.put("stations in system, what stations, nearby stations, star ports, space stations, docking available", QUERY_STATIONS.getAction());
        }

        if (status.isInSrv() && status.isDocked() || isDryRun) {
            map.put("show services panel, open services panel, display services panel, show station services panel", SHOW_STATION_SERVICES.getAction());
        }

        if (status.isInMainShip() || status.isInSrv() || isDryRun) {
            // flight / ship systems
            map.put("switch to combat mode", ACTIVATE_COMBAT_MODE.getAction());
            map.put("switch to analysis mode", ACTIVATE_ANALYSIS_MODE.getAction());
            map.put("cargo scoop, open cargo scoop, close cargo scoop, deploy cargo scoop, retract cargo scoop, open cargo bay, close cargo bay", TOGGLE_CARGO_SCOOP.getAction());
            map.put("night vision, nightvision, turn on night vision, turn off night vision {state:true/false}", NIGHT_VISION_ON_OFF.getAction());
            map.put("headlights, lights, turn off lights, turn on lights, ship lights, lights on, lights off {state:true/false}", LIGHTS_ON_OFF.getAction());
            // UI panels
            map.put("show, open or display commander, central, role panel, open knee board", SHOW_COMMANDER_PANEL.getAction());
            map.put("show, open or display crew panel", SHOW_CREW.getAction());
            map.put("show, open or display home panel", SHOW_INTERNAL_PANEL.getAction());
            map.put("show, open or display modules panel", SHOW_MODULES_PANEL.getAction());
            map.put("show, open or display fire groups", SHOW_FIRE_GROUPS.getAction());
            map.put("show, open or display inventory panel", SHOW_INVENTORY_PANEL.getAction());
            map.put("show, open or display storage panel", SHOW_STORAGE_PANEL.getAction());
            // power
            map.put("power to shields, max shields, boost shields", INCREASE_SHIELDS_POWER.getAction());
            map.put("power to engines, max engines, boost engines", INCREASE_ENGINES_POWER.getAction());
            map.put("power to weapons, max weapons, boost weapons", INCREASE_WEAPONS_POWER.getAction());
            // vehicle deployment
            map.put("disembark", DISEMBARK.getAction());
            map.put("equalize power, reset power, distribute power equally", RESET_POWER.getAction());
        }

        if (status.isInSrv() || isDryRun) {
            map.put("drive assist, driving assist, SRV assist {state:true/false}", DRIVE_ASSIST.getAction());
            map.put("recover SRV, board ship, return SRV, retrieve SRV, SRV dock", RECOVER_SRV.getAction());
        }

        if (status.isInSrv() || status.isOnFoot() || isDryRun) {
            map.put("dismiss ship, send ship away, ship to orbit", DISMISS_SHIP.getAction());
            map.put("return to surface, pick me up", RETURN_TO_SURFACE.getAction());
        }

        // market / traders / brokers
        map.put("find raw material trader, raw trader, where to trade raw materials {key:X}", FIND_RAW_MATERIAL_TRADER.getAction());
        map.put("find encoded material trader, encoded trader, data trader {key:X}", FIND_ENCODED_MATERIAL_TRADER.getAction());
        map.put("find manufactured material trader, manufactured trader {key:X}", FIND_MANUFACTURED_MATERIAL_TRADER.getAction());
        map.put("find human tech broker, human technology broker {key:X}", FIND_HUMAN_TECHNOLOGY_BROKER.getAction());
        map.put("find guardian tech broker, guardian technology broker {key:X}", FIND_GUARDIAN_TECHNOLOGY_BROKER.getAction());
        map.put("find commodity, find nearest commodity, buy commodity, where to buy, find market{key:X, max_distance:Y, state:true/false}", FIND_COMMODITY.getAction());
        map.put("find nearest fleet carrier, nearest carrier", FIND_NEAREST_FLEET_CARRIER.getAction());
        map.put("find nearest interstellar factor, find where I can payoff my bounty ", FIND_INTERSTELLAR_FACTOR.getAction());

        // fleet carrier  bare "carrier" without "squadron" always means fleet/my carrier
        map.put("set carrier fuel reserve, carrier tritium reserve {key:X}", SET_CARRIER_FUEL_RESERVE.getAction());
        map.put("calculate fleet carrier route, plan carrier route, carrier jump route", CALCULATE_FLEET_CARRIER_ROUTE.getAction());
        map.put("enter carrier destination, set carrier destination, carrier destination", ENTER_FLEET_CARRIER_DESTINATION.getAction());
        map.put("fleet carriers in system, carriers in system, how many carriers, fleet carriers here, any carriers nearby", QUERY_CARRIERS.getAction());
        map.put("carrier route, carrier navigation, how many jumps on carrier route, jumps remaining on carrier, carrier route jump count, jumps left on carrier, carrier trip, carrier journey, carrier travel plan", FLEET_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put("where is carrier going, carrier next jump, where is carrier headed, carrier heading, carrier endpoint, carrier final destination", FLEET_CARRIER_ROUTE.getAction());
        map.put("carrier tritium, carrier fuel, how much tritium, tritium supply, tritium level, carrier fuel level, tritium reserve", FLEET_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put("carrier status, carrier finances, carrier overview, fleet carrier status, fleet carrier fuel status, how far can carrier jump, carrier jump range, carrier jump range with current tritium", FLEET_CARRIER_STATUS.getAction());
        map.put("carrier ETA, when does carrier arrive, how long until carrier, carrier arrival time, carrier arrival, carrier jump time, carrier next jump time", FLEET_CARRIER_ETA.getAction());
        map.put("distance to carrier, where is our carrier, how far is carrier, range to carrier, carrier proximity, how far away is carrier", DISTANCE_TO_CARRIER.getAction());

        // squadron carrier  must explicitly say "squadron carrier"
        map.put("squadron carrier route, squadron carrier navigation, squadron carrier jump route, how many jumps on squadron carrier route, jumps remaining on squadron carrier", SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put("where is squadron carrier going, squadron carrier heading, squadron carrier endpoint, squadron carrier final destination", SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION.getAction());
        map.put("squadron carrier tritium, squadron carrier fuel, squadron tritium supply, squadron carrier fuel level, squadron carrier tritium status", SQUADRON_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put("squadron carrier status, squadron carrier finances, squadron carrier balance, squadron carrier overview, squadron carrier funds, how long can we operate squadron carrier, squadron carrier fuel status", SQUADRON_CARRIER_STATUS.getAction());
        map.put("squadron carrier ETA, when does squadron carrier arrive, how long until squadron carrier, squadron carrier arrival time, squadron carrier arrival", SQUADRON_CARRIER_ETA.getAction());

        // trade
        map.put("calculate trade route", CALCULATE_TRADE_ROUTE.getAction());
        map.put("list trade route parameters", LIST_TRADE_ROUTE_PARAMETERS.getAction());
        map.put("monetize route", MONETIZE_ROUTE.getAction());

        map.put("change trade profile starting budget {key:X}", CHANGE_TRADE_PROFILE_SET_STARTING_BUDGET.getAction());
        map.put("change trade profile max stops {key:X}", CHANGE_TRADE_PROFILE_SET_MAX_NUMBER_OF_STOPS.getAction());
        map.put("change trade profile max distance {key:X}", CHANGE_TRADE_PROFILE_SET_MAX_DISTANCE_FROM_ENTRY.getAction());
        map.put("change trade profile allow prohibited cargo {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PROHIBITED_CARGO.getAction());
        map.put("change trade profile allow planetary port {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PLANETARY_PORT.getAction());
        map.put("change trade profile allow permit systems {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PERMIT_SYSTEMS.getAction());
        map.put("change trade profile allow strongholds {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_STRONGHOLDS.getAction());

        // announcements / app settings
        map.put("toggle radio, radio traffic, radio transmissions {state:true/false}", SET_RADIO_TRANSMISSION_MODE.getAction());
        map.put("radar contact announcement {state:true/false}", SET_RADAR_CONTACT_ANNOUNCEMENT.getAction());
        map.put("discovery announcements {state:true/false}", DISCOVERY_ON_OFF.getAction());
        map.put("route announcements {state:true/false}", ROUTE_ON_OFF.getAction());
        map.put("toggle all announcements {state:true/false}", TOGGLE_ALL_ANNOUNCEMENTS.getAction());
        map.put("honk, honk the system", HONK_THE_SYSTEM.getAction());
        map.put("clear reminders", CLEAR_REMINDERS.getAction());
        map.put("set reminder {key:X}", SET_REMINDER.getAction());
        map.put("remind me in {minutes:X} minutes to {key:Y}, set a {minutes:X} minute timer {key:Y}, set timer for {minutes:X} minutes {key:Y}, timed reminder {minutes:X} minutes {key:Y}", SET_TIMED_REMINDER.getAction());

        // UI panels
        map.put("activate", ACTIVATE.getAction());
        map.put("show, open or display transactions panel", SHOW_TRANSACTIONS.getAction());
        map.put("show, open or display contacts panel", SHOW_CONTACTS.getAction());
        map.put("show, open or display navigation panel", SHOW_NAVIGATION.getAction());
        map.put("show, open or display chat panel, comms panel", SHOW_CHAT_PANEL.getAction());
        map.put("show, open or display email inbox panel, email", SHOW_INBOX_PANEL.getAction());
        map.put("show, open or display social panel", SHOW_SOCIAL_PANEL.getAction());
        map.put("show, open or display history panel", SHOW_HISTORY_PANEL.getAction());
        map.put("show, open or display squadron panel", SHOW_SQUADRON.getAction());
        map.put("show, open or display status panel", SHOW_STATUS_PANEL.getAction());
        map.put("show, open or display carrier management panel", DISPLAY_CARRIER_MANAGEMENT.getAction());
        map.put("show, open or display galaxy map", OPEN_GALAXY_MAP.getAction());
        map.put("show, open or display local / stat system map", OPEN_SYSTEM_MAP.getAction());
        map.put("exit close panel", EXIT_CLOSE.getAction());

        // pirate massacre missions
        map.put("navigate to mission provider system", RECON_PROVIDER_SYSTEM.getAction());
        map.put("navigate to pirate mission provider", NAVIGATE_TO_PIRATE_MISSION_PROVIDER.getAction());
        map.put("active missions, current missions, mission log, what missions, mission status, what are our missions, ongoing missions, mission board", ANALYZE_MISSIONS.getAction());
        map.put("pirate mission, kill count, how many kills, massacre mission progress, kills remaining, massacre progress, pirates remaining, pirate kills, bounty hunt progress", PIRATE_MISSION_PROGRESS.getAction());
        map.put("find hunting grounds {key:X}", FIND_HUNTING_GROUNDS.getAction());
        map.put("recon hunting ground, navigate to target system, navigate to hunting ground", RECON_TARGET_SYSTEM.getAction());
        map.put("ignore hunting ground", IGNORE_HUNTING_GROUND.getAction());
        map.put("confirm hunting ground, confirm target star system", CONFIRM_HUNTING_GROUND.getAction());
        map.put("clear active missions, clear all active missions", CLEAR_ALL_ACTIVE_MISSIONS.getAction());

        // science / mining / biology
        map.put("add mining target {key:X}", ADD_MINING_TARGET.getAction());
        map.put("remove mining target {key:X}", REMOVE_MINING_TARGET.getAction());
        map.put("clear mining targets", CLEAR_MINING_TARGETS.getAction());
        map.put("mining announcements {state:true/false}", MINING_ON_OFF.getAction());
        map.put("find brain trees {key:X, max_distance:Y}", FIND_BRAIN_TREES.getAction());
        map.put("find mining site, find mining location, find mining hotspot, find where to mine, find asteroid field {key:X, max_distance:Y}", FIND_MINING_SITE.getAction());
        map.put("find tritium mining site, find tritium field {key:X, max_distance:Y}", FIND_FLEET_CARRIER_FUEL_MINING_SITE.getAction());
        map.put("navigate to codex entry, navigate to next bio sample, navigate to bio sample, go to next bio sample, go to next sample, navigate to next organic, go to codex entry", NAVIGATE_TO_NEXT_BIO_SAMPLE.getAction());
        map.put("open fss, full scan, full spectrum scan, discovery scan, FSS", OPEN_FSS.getAction());
        map.put("find nearest vista genomics, find genomics, vista genomics", FIND_VISTA_GENOMICS.getAction());
        map.put("delete codex entry, delete this codex, delete this entry, delete this organic", DELETE_CODEX_ENTRY.getAction());
        map.put("calculate neutron star route {efficiency:X}", CALCULATE_NEUTRON_STAR_ROUTE.getAction());
        map.put("next neutron star jump, plot route to next neutron star waypoint, next neutron star", PLOT_ROUTE_TO_NEXT_NEUTRON_STAR.getAction());
        map.put("clear neutron route", CLEAR_NEUTRON_ROUTE.getAction());

        map.put("check key bindings, missing key bindings, unbound keys, keyboard bindings, keybind check, missing bindings", KEY_BINDINGS_ANALYSIS.getAction());
        map.put("bio signals done within the star system, organics scanned in star system, how many bio samples in star system, bio signals in system, bio samples in star system, biological signals in star system, organics in system, which planets have bio signals, which planets still need bio scans, which planets need organic scans, which planets still need scanning, bio scan needed on which planets, planets with unscanned bio signals, bio scan progress", BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
        map.put("exobiology samples, biology samples, organics at location, what organisms, what's left to scan, remaining organisms, samples left, organisms remaining, exobiology progress, scan remaining, what bio scans completed, what bio scans have we completed, bio scans done, bio scans completed, organics on this planet, biology on this planet, what organisms are here, what organisms are on this planet, bio sample progress on planet, what's been scanned here, what organics do we still have to scan, what organics still to scan, organics still to scan, organics remaining to scan, organics left to scan, what organics remain, biology still to scan, what biology remains, what do we still need to scan here", EXOBIOLOGY_SAMPLES_ON_THIS_PLANET.getAction());
        map.put("distance to last bio sample, how far to sample, how far to last organism, range to bio sample, how far to previous organism, navigate to bio sample", DISTANCE_TO_LAST_BIO_SAMPLE.getAction());
        map.put("analyze biome, biome analysis, what biome {key:X}, planetary biome, atmosphere analysis, what life is here, biome type", PLANET_BIOME_ANALYSIS.getAction());
        map.put("stellar objects, planets in system, landable planets, is planet or moon landable, bodies in system, what planets, how many planets, system bodies, stellar bodies, ice rings, planetary rings, rings in system, has rings, ring system", QUERY_STELLAR_OBJETS.getAction());
        map.put("signals in system, what signals are in this system, what's in this system, what's detected in system, what signals here, what signals do you see, what signals do you detect, what signals can you see, FSS signals, mining hot spots, resource extraction sites, what signals, conflict zones, emissions, unidentified signals, system signals, detected signals, anomalous signals", QUERY_STELLAR_SIGNALS.getAction());
        map.put("geo signals, geological signals, volcanic signals, geological activity, volcanic activity, geology in system", QUERY_GEO_SIGNALS.getAction());

        map.put("system security, faction control, who controls, power struggle, security level, who owns this system, dominant faction, controlling power", SYSTEM_SECURITY_ANALYSIS.getAction());
        map.put("trade profile, trade settings, trading parameters, trade configuration, trading criteria", TRADE_PROFILE_ANALYSIS.getAction());
        map.put("distance to stellar object, how far to body, distance to planet {key:X}, range to planet, how far to moon, how far to station, range to body. NOTE: Sol and Earth always mean the bubble/civilization - never use this action for Sol or Earth", DISTANCE_TO_BODY.getAction());
        map.put("last scan, what did we scan, last scanned object, most recent scan, latest scan, recent body scan, what did i last scan", LAST_SCAN_ANALYSIS.getAction());
        map.put("material inventory {key:X}, how many items {key:X}, how many guardian item {key:X}, how much material {key:X}, do we have material {key:X}, how much {key:X} do we have, do we have any {key:X}, engineering material {key:X}, raw material {key:X}, manufactured material {key:X}, encoded material {key:X}, material stock {key:X}", MATERIALS_INVENTORY.getAction());
        map.put("planet materials, materials here, what materials on this planet, surface materials, what materials are here, material deposits, minerals on planet", PLANET_MATERIALS.getAction());
        map.put("exploration profits, discovery profits, how much exploration worth, exploration value, scan value, mapping profit, exobiology value, scan earnings, what are scans worth", EXPLORATION_PROFITS.getAction());
        map.put("query current location, where are we, our position, what system are we in, where am i, our coordinates, current system, what planet are we at, our current position, day length", CURRENT_LOCATION.getAction());
        map.put("где я нахожусь, где мы находимся, в какой системе я нахожусь, в какой системе мы находимся, какая текущая система, где мой корабль, где мы сейчас, наше местоположение, текущая позиция", CURRENT_LOCATION.getAction());
        map.put("FSD target info, analyze destination, what star are we targeting, analyze fsd target, information on fsd target", FSD_TARGET_ANALYSIS.getAction());
        map.put("plotted route, fuel at next stop, fuel availability on route, route analysis, are we there yet, current route, navigation route, jumps remaining, jumps left, how many jumps, next star scoopable, fuel stop", PLOTTED_ROUTE_ANALYSIS.getAction());
        map.put("trade route, trading route, current trade plan, what are we trading, our trading plan, trading schedule, trade legs", TRADE_ROUTE_ANALYSIS.getAction());
        map.put("outfitting, ship upgrades, modules available, what modules at station, available modules, available equipment, buy modules, ship parts, station equipment", LOCAL_OUTFITTING.getAction());
        map.put("shipyard, ships for sale, what ships at station, buy a ship, available ships, ships to buy, new ship", LOCAL_SHIPYARD.getAction());
        map.put("what is in our cargo hold, what are we carrying, cargo contents, commodities on board, what are we hauling, hold contents", CARGO_HOLD_CONTENTS.getAction());
        map.put("player profile", PLAYER_PROFILE_ANALYSIS.getAction());
        map.put("ship loadout, damage report, ship modules, combat readiness report, ship equipment, ship specs, what am I flying, what are we equipped with, do you have, is it equipped, shield generator, hull reinforcement, sensors, thrusters, frameshift, fuel scoop, installed", SHIP_LOADOUT.getAction());
        map.put("station details, what services here, what services are here, services here, what services does this station have, services at this station, what does station offer, station info, station facilities, what's at this station, services available", STATION_DETAILS.getAction());
        map.put("на какой станции я нахожусь, на какой станции мы находимся, где я пристыкован, где мы пристыкованы, какая моя текущая станция, какая у нас текущая станция, информация о станции, какие сервисы есть на станции, что есть на этой станции", STATION_DETAILS.getAction());
        map.put("bounties, total bounties, bounty collected, how much in bounties, bounty earnings, credits from bounties, bounty credits", TOTAL_BOUNTIES.getAction());
        map.put("distance to bubble, distance to sol, distance from sol, distance to Earth, distance from Earth, how far from sol, how far from bubble, how far from inhabited space, distance from inhabited space, how far from civilization, range from human space", DISTANCE_TO_BUBBLE.getAction());
        map.put("current time, what time is it, time on earth, galactic time, utc time, what's the time, real time", TIME_IN_ZONE.getAction());
        map.put("reminder, what was the reminder, destination reminder, any reminders, recall reminder, what did we set as reminder", REMINDER.getAction());
        map.put("local markets, markets at stations and settlements, markets at outposts in system", ANALYZE_MARKETS.getAction());
    }
}