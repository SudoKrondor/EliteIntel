package elite.intel.ai.brain.i18n;

import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.Map;
import java.util.Set;

import static elite.intel.ai.brain.actions.Commands.*;
import static elite.intel.ai.brain.actions.Queries.*;

public abstract class AiActionAliasProvider {

    public abstract Set<String> wakeBypassPhrases();

    /**
     * Listen-type prefixes that can be stripped before forwarding to the AI.
     * E.g. "listen open galaxy map" → "open galaxy map".
     * Pure wake phrases (no content follows) are NOT in this set.
     */
    public abstract Set<String> listenBypassPrefixes();

    public void addAliases(Map<String, String> map, Status status, boolean isDryRun) {

        // always available
        map.put(StringUtls.localizedAiActionKeys(WAKEUP.getAction()), WAKEUP.getAction());
        map.put(StringUtls.localizedAiActionKeys(SLEEP.getAction()), SLEEP.getAction());
        map.put(StringUtls.localizedAiActionKeys(INTERRUPT_TTS.getAction()), INTERRUPT_TTS.getAction());

        // Declared early so these win before power-management "balance power" and carrier-route entries can interfere
        map.put(StringUtls.localizedAiActionKeys(FIND_NEAREST_FLEET_CARRIER.getAction()), FIND_NEAREST_FLEET_CARRIER.getAction());
        map.put(StringUtls.localizedAiActionKeys(FLEET_CARRIER_STATUS.getAction()), FLEET_CARRIER_STATUS.getAction());
        map.put(StringUtls.localizedAiActionKeys(SQUADRON_CARRIER_STATUS.getAction()), SQUADRON_CARRIER_STATUS.getAction());

        // navigation
        map.put(StringUtls.localizedAiActionKeys(CANCEL_TRADE_ROUTE.getAction()), CANCEL_TRADE_ROUTE.getAction());
        map.put(StringUtls.localizedAiActionKeys(NAVIGATE_TO_TARGET.getAction()), NAVIGATE_TO_TARGET.getAction());
        map.put(StringUtls.localizedAiActionKeys(NAVIGATE_TO_NEXT_MISSION.getAction()), NAVIGATE_TO_NEXT_MISSION.getAction());
        map.put(StringUtls.localizedAiActionKeys(NAVIGATE_TO_FLEET_CARRIER.getAction()), NAVIGATE_TO_FLEET_CARRIER.getAction());
        map.put(StringUtls.localizedAiActionKeys(GET_HEADING_TO_LZ.getAction()), GET_HEADING_TO_LZ.getAction());
        map.put(StringUtls.localizedAiActionKeys(NAVIGATE_TO_NEXT_TRADE_STOP.getAction()), NAVIGATE_TO_NEXT_TRADE_STOP.getAction());
        map.put(StringUtls.localizedAiActionKeys(NAVIGATE_TO_ADDRESS_FROM_MEMORY.getAction()), NAVIGATE_TO_ADDRESS_FROM_MEMORY.getAction());
        map.put(StringUtls.localizedAiActionKeys(NAVIGATION_OFF.getAction()), NAVIGATION_OFF.getAction());
        map.put(StringUtls.localizedAiActionKeys(SET_HOME_SYSTEM.getAction()), SET_HOME_SYSTEM.getAction());
        map.put(StringUtls.localizedAiActionKeys(TAKE_ME_HOME.getAction()), TAKE_ME_HOME.getAction());
        map.put(StringUtls.localizedAiActionKeys(RESET_HEAD_LOOK.getAction()), RESET_HEAD_LOOK.getAction());

        if (status.isInMainShip() || isDryRun) {
            // navigation
            map.put(StringUtls.localizedAiActionKeys(TARGET_DESTINATION.getAction()), TARGET_DESTINATION.getAction());
            map.put(StringUtls.localizedAiActionKeys(JUMP_TO_HYPERSPACE.getAction()), JUMP_TO_HYPERSPACE.getAction());
            map.put(StringUtls.localizedAiActionKeys(DROP_FROM_SUPER_CRUISE.getAction()), DROP_FROM_SUPER_CRUISE.getAction());
            map.put(StringUtls.localizedAiActionKeys(ENTER_SUPER_CRUISE.getAction()), ENTER_SUPER_CRUISE.getAction());
            map.put(StringUtls.localizedAiActionKeys(LAUNCH_SHIP.getAction()), LAUNCH_SHIP.getAction());
            // speed / throttle
            map.put(StringUtls.localizedAiActionKeys(SET_SPEED_ZERO.getAction()), SET_SPEED_ZERO.getAction());
            map.put(StringUtls.localizedAiActionKeys(TAXI.getAction()), TAXI.getAction());
            map.put(StringUtls.localizedAiActionKeys(SET_SPEED25.getAction()), SET_SPEED25.getAction());
            map.put(StringUtls.localizedAiActionKeys(SET_SPEED50.getAction()), SET_SPEED50.getAction());
            map.put(StringUtls.localizedAiActionKeys(SET_SPEED75.getAction()), SET_SPEED75.getAction());
            map.put(StringUtls.localizedAiActionKeys(SET_SPEED100.getAction()), SET_SPEED100.getAction());
            map.put(StringUtls.localizedAiActionKeys(INCREASE_SPEED_BY.getAction()), INCREASE_SPEED_BY.getAction());
            map.put(StringUtls.localizedAiActionKeys(DECREASE_SPEED_BY.getAction()), DECREASE_SPEED_BY.getAction());
            map.put(StringUtls.localizedAiActionKeys(SET_OPTIMAL_SPEED.getAction()), SET_OPTIMAL_SPEED.getAction());
            map.put(StringUtls.localizedAiActionKeys(DEPLOY_LANDING_GEAR.getAction()), DEPLOY_LANDING_GEAR.getAction());
            map.put(StringUtls.localizedAiActionKeys(RETRACT_LANDING_GEAR.getAction()), RETRACT_LANDING_GEAR.getAction());
            map.put(StringUtls.localizedAiActionKeys(REQUEST_DOCKING.getAction()), REQUEST_DOCKING.getAction());
            // UI panels
            map.put(StringUtls.localizedAiActionKeys(SHOW_FIGHTER_PANEL.getAction()), SHOW_FIGHTER_PANEL.getAction());
            map.put(StringUtls.localizedAiActionKeys(DEPLOY_SHIELD_CELL.getAction()), DEPLOY_SHIELD_CELL.getAction());
            map.put(StringUtls.localizedAiActionKeys(DEPLOY_CHAFF.getAction()), DEPLOY_CHAFF.getAction());
            // combat
            map.put(StringUtls.localizedAiActionKeys(DEPLOY_HARDPOINTS.getAction()), DEPLOY_HARDPOINTS.getAction());
            map.put(StringUtls.localizedAiActionKeys(RETRACT_HARDPOINTS.getAction()), RETRACT_HARDPOINTS.getAction());
            map.put(StringUtls.localizedAiActionKeys(TARGET_SUB_SYSTEM.getAction()), TARGET_SUB_SYSTEM.getAction());
            map.put(StringUtls.localizedAiActionKeys(TARGET_WINGMAN0.getAction()), TARGET_WINGMAN0.getAction());
            map.put(StringUtls.localizedAiActionKeys(TARGET_WINGMAN1.getAction()), TARGET_WINGMAN1.getAction());
            map.put(StringUtls.localizedAiActionKeys(TARGET_WINGMAN2.getAction()), TARGET_WINGMAN2.getAction());
            map.put(StringUtls.localizedAiActionKeys(WING_NAV_LOCK.getAction()), WING_NAV_LOCK.getAction());
            map.put(StringUtls.localizedAiActionKeys(SELECT_HIGHEST_THREAT.getAction()), SELECT_HIGHEST_THREAT.getAction());
            // vehicle deployment
            map.put(StringUtls.localizedAiActionKeys(DEPLOY_SRV.getAction()), DEPLOY_SRV.getAction());
            map.put(StringUtls.localizedAiActionKeys(DEPLOY_HEAT_SINK.getAction()), DEPLOY_HEAT_SINK.getAction());
            // fighter orders
            map.put(StringUtls.localizedAiActionKeys(DEPLOY_FIGHTER.getAction()), DEPLOY_FIGHTER.getAction());
            map.put(StringUtls.localizedAiActionKeys(FIGHTER_REQUEST_DEFENSIVE_BEHAVIOUR.getAction()), FIGHTER_REQUEST_DEFENSIVE_BEHAVIOUR.getAction());
            map.put(StringUtls.localizedAiActionKeys(FIGHTER_REQUEST_FOCUS_TARGET.getAction()), FIGHTER_REQUEST_FOCUS_TARGET.getAction());
            map.put(StringUtls.localizedAiActionKeys(FIGHTER_REQUEST_HOLD_FIRE.getAction()), FIGHTER_REQUEST_HOLD_FIRE.getAction());
            map.put(StringUtls.localizedAiActionKeys(FIGHTER_REQUEST_REQUEST_DOCK.getAction()), FIGHTER_REQUEST_REQUEST_DOCK.getAction());
            map.put(StringUtls.localizedAiActionKeys(FIGHTER_OPEN_ORDERS.getAction()), FIGHTER_OPEN_ORDERS.getAction());
            map.put(StringUtls.localizedAiActionKeys(SELECT_FIRE_GROUP_BY_NATO.getAction()), SELECT_FIRE_GROUP_BY_NATO.getAction());
            // power (RU-only action; other locales get a dead entry that no user will trigger)
            map.put(StringUtls.localizedAiActionKeys(INCREASE_SYSTEMS_POWER.getAction()), INCREASE_SYSTEMS_POWER.getAction());
            // neutron star routes
            map.put(StringUtls.localizedAiActionKeys(CALCULATE_NEUTRON_STAR_ROUTE.getAction()), CALCULATE_NEUTRON_STAR_ROUTE.getAction());
            map.put(StringUtls.localizedAiActionKeys(PLOT_ROUTE_TO_NEXT_NEUTRON_STAR.getAction()), PLOT_ROUTE_TO_NEXT_NEUTRON_STAR.getAction());
            map.put(StringUtls.localizedAiActionKeys(CLEAR_NEUTRON_ROUTE.getAction()), CLEAR_NEUTRON_ROUTE.getAction());
        }

        if (status.isInMainShip() && !status.isDocked() || isDryRun) {
            map.put(StringUtls.localizedAiActionKeys(QUERY_STATIONS.getAction()), QUERY_STATIONS.getAction());
        }

        if (status.isInSrv() && status.isDocked() || isDryRun) {
            map.put(StringUtls.localizedAiActionKeys(SHOW_STATION_SERVICES.getAction()), SHOW_STATION_SERVICES.getAction());
        }

        if (status.isInMainShip() || status.isInSrv() || isDryRun) {
            // flight / ship systems
            map.put(StringUtls.localizedAiActionKeys(ACTIVATE_COMBAT_MODE.getAction()), ACTIVATE_COMBAT_MODE.getAction());
            map.put(StringUtls.localizedAiActionKeys(ACTIVATE_ANALYSIS_MODE.getAction()), ACTIVATE_ANALYSIS_MODE.getAction());
            map.put(StringUtls.localizedAiActionKeys(TOGGLE_CARGO_SCOOP.getAction()), TOGGLE_CARGO_SCOOP.getAction());
            map.put(StringUtls.localizedAiActionKeys(NIGHT_VISION_ON_OFF.getAction()), NIGHT_VISION_ON_OFF.getAction());
            map.put(StringUtls.localizedAiActionKeys(LIGHTS_ON_OFF.getAction()), LIGHTS_ON_OFF.getAction());
            // UI panels
            map.put(StringUtls.localizedAiActionKeys(SHOW_COMMANDER_PANEL.getAction()), SHOW_COMMANDER_PANEL.getAction());
            map.put(StringUtls.localizedAiActionKeys(SHOW_CREW.getAction()), SHOW_CREW.getAction());
            map.put(StringUtls.localizedAiActionKeys(SHOW_INTERNAL_PANEL.getAction()), SHOW_INTERNAL_PANEL.getAction());
            map.put(StringUtls.localizedAiActionKeys(SHOW_MODULES_PANEL.getAction()), SHOW_MODULES_PANEL.getAction());
            map.put(StringUtls.localizedAiActionKeys(SHOW_FIRE_GROUPS.getAction()), SHOW_FIRE_GROUPS.getAction());
            map.put(StringUtls.localizedAiActionKeys(SHOW_INVENTORY_PANEL.getAction()), SHOW_INVENTORY_PANEL.getAction());
            map.put(StringUtls.localizedAiActionKeys(SHOW_STORAGE_PANEL.getAction()), SHOW_STORAGE_PANEL.getAction());
            // power
            map.put(StringUtls.localizedAiActionKeys(INCREASE_SHIELDS_POWER.getAction()), INCREASE_SHIELDS_POWER.getAction());
            map.put(StringUtls.localizedAiActionKeys(INCREASE_ENGINES_POWER.getAction()), INCREASE_ENGINES_POWER.getAction());
            map.put(StringUtls.localizedAiActionKeys(INCREASE_WEAPONS_POWER.getAction()), INCREASE_WEAPONS_POWER.getAction());
            // vehicle deployment
            map.put(StringUtls.localizedAiActionKeys(DISEMBARK.getAction()), DISEMBARK.getAction());
            map.put(StringUtls.localizedAiActionKeys(RESET_POWER.getAction()), RESET_POWER.getAction());
        }

        if (status.isInSrv() || isDryRun) {
            map.put(StringUtls.localizedAiActionKeys(DRIVE_ASSIST.getAction()), DRIVE_ASSIST.getAction());
            map.put(StringUtls.localizedAiActionKeys(RECOVER_SRV.getAction()), RECOVER_SRV.getAction());
        }

        if (status.isInSrv() || status.isOnFoot() || isDryRun) {
            map.put(StringUtls.localizedAiActionKeys(DISMISS_SHIP.getAction()), DISMISS_SHIP.getAction());
            map.put(StringUtls.localizedAiActionKeys(RETURN_TO_SURFACE.getAction()), RETURN_TO_SURFACE.getAction());
        }

        // market / traders / brokers
        map.put(StringUtls.localizedAiActionKeys(FIND_RAW_MATERIAL_TRADER.getAction()), FIND_RAW_MATERIAL_TRADER.getAction());
        map.put(StringUtls.localizedAiActionKeys(FIND_ENCODED_MATERIAL_TRADER.getAction()), FIND_ENCODED_MATERIAL_TRADER.getAction());
        map.put(StringUtls.localizedAiActionKeys(FIND_MANUFACTURED_MATERIAL_TRADER.getAction()), FIND_MANUFACTURED_MATERIAL_TRADER.getAction());
        map.put(StringUtls.localizedAiActionKeys(FIND_HUMAN_TECHNOLOGY_BROKER.getAction()), FIND_HUMAN_TECHNOLOGY_BROKER.getAction());
        map.put(StringUtls.localizedAiActionKeys(FIND_GUARDIAN_TECHNOLOGY_BROKER.getAction()), FIND_GUARDIAN_TECHNOLOGY_BROKER.getAction());
        map.put(StringUtls.localizedAiActionKeys(FIND_COMMODITY.getAction()), FIND_COMMODITY.getAction());
        map.put(StringUtls.localizedAiActionKeys(FIND_INTERSTELLAR_FACTOR.getAction()), FIND_INTERSTELLAR_FACTOR.getAction());

        // fleet carrier  bare "carrier" without "squadron" always means fleet/my carrier
        map.put(StringUtls.localizedAiActionKeys(SET_CARRIER_FUEL_RESERVE.getAction()), SET_CARRIER_FUEL_RESERVE.getAction());
        map.put(StringUtls.localizedAiActionKeys(CALCULATE_FLEET_CARRIER_ROUTE.getAction()), CALCULATE_FLEET_CARRIER_ROUTE.getAction());
        map.put(StringUtls.localizedAiActionKeys(ENTER_FLEET_CARRIER_DESTINATION.getAction()), ENTER_FLEET_CARRIER_DESTINATION.getAction());
        map.put(StringUtls.localizedAiActionKeys(QUERY_CARRIERS.getAction()), QUERY_CARRIERS.getAction());
        map.put(StringUtls.localizedAiActionKeys(FLEET_CARRIER_ROUTE_ANALYSIS.getAction()), FLEET_CARRIER_ROUTE_ANALYSIS.getAction());
        //map.put(StringUtls.localizedAiActionKeys(FLEET_CARRIER_TRITIUM_SUPPLY.getAction()), FLEET_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put(StringUtls.localizedAiActionKeys(FLEET_CARRIER_ETA.getAction()), FLEET_CARRIER_ETA.getAction());
        map.put(StringUtls.localizedAiActionKeys(DISTANCE_TO_CARRIER.getAction()), DISTANCE_TO_CARRIER.getAction());

        // squadron carrier  must explicitly say "squadron carrier"
        map.put(StringUtls.localizedAiActionKeys(SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction()), SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put(StringUtls.localizedAiActionKeys(SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION.getAction()), SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION.getAction());
        map.put(StringUtls.localizedAiActionKeys(SQUADRON_CARRIER_TRITIUM_SUPPLY.getAction()), SQUADRON_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put(StringUtls.localizedAiActionKeys(SQUADRON_CARRIER_ETA.getAction()), SQUADRON_CARRIER_ETA.getAction());

        // trade
        map.put(StringUtls.localizedAiActionKeys(CALCULATE_TRADE_ROUTE.getAction()), CALCULATE_TRADE_ROUTE.getAction());
        map.put(StringUtls.localizedAiActionKeys(LIST_TRADE_ROUTE_PARAMETERS.getAction()), LIST_TRADE_ROUTE_PARAMETERS.getAction());
        map.put(StringUtls.localizedAiActionKeys(MONETIZE_ROUTE.getAction()), MONETIZE_ROUTE.getAction());

        map.put(StringUtls.localizedAiActionKeys(CHANGE_TRADE_PROFILE_SET_STARTING_BUDGET.getAction()), CHANGE_TRADE_PROFILE_SET_STARTING_BUDGET.getAction());
        map.put(StringUtls.localizedAiActionKeys(CHANGE_TRADE_PROFILE_SET_MAX_NUMBER_OF_STOPS.getAction()), CHANGE_TRADE_PROFILE_SET_MAX_NUMBER_OF_STOPS.getAction());
        map.put(StringUtls.localizedAiActionKeys(CHANGE_TRADE_PROFILE_SET_MAX_DISTANCE_FROM_ENTRY.getAction()), CHANGE_TRADE_PROFILE_SET_MAX_DISTANCE_FROM_ENTRY.getAction());
        map.put(StringUtls.localizedAiActionKeys(CHANGE_TRADE_PROFILE_SET_ALLOW_PROHIBITED_CARGO.getAction()), CHANGE_TRADE_PROFILE_SET_ALLOW_PROHIBITED_CARGO.getAction());
        map.put(StringUtls.localizedAiActionKeys(CHANGE_TRADE_PROFILE_SET_ALLOW_PLANETARY_PORT.getAction()), CHANGE_TRADE_PROFILE_SET_ALLOW_PLANETARY_PORT.getAction());
        map.put(StringUtls.localizedAiActionKeys(CHANGE_TRADE_PROFILE_SET_ALLOW_PERMIT_SYSTEMS.getAction()), CHANGE_TRADE_PROFILE_SET_ALLOW_PERMIT_SYSTEMS.getAction());
        map.put(StringUtls.localizedAiActionKeys(CHANGE_TRADE_PROFILE_SET_ALLOW_STRONGHOLDS.getAction()), CHANGE_TRADE_PROFILE_SET_ALLOW_STRONGHOLDS.getAction());

        // announcements / app settings
        map.put(StringUtls.localizedAiActionKeys(SET_RADIO_TRANSMISSION_MODE.getAction()), SET_RADIO_TRANSMISSION_MODE.getAction());
        map.put(StringUtls.localizedAiActionKeys(SET_RADAR_CONTACT_ANNOUNCEMENT.getAction()), SET_RADAR_CONTACT_ANNOUNCEMENT.getAction());
        map.put(StringUtls.localizedAiActionKeys(DISCOVERY_ON_OFF.getAction()), DISCOVERY_ON_OFF.getAction());
        map.put(StringUtls.localizedAiActionKeys(ROUTE_ON_OFF.getAction()), ROUTE_ON_OFF.getAction());
        map.put(StringUtls.localizedAiActionKeys(TOGGLE_ALL_ANNOUNCEMENTS.getAction()), TOGGLE_ALL_ANNOUNCEMENTS.getAction());
        map.put(StringUtls.localizedAiActionKeys(HONK_THE_SYSTEM.getAction()), HONK_THE_SYSTEM.getAction());
        map.put(StringUtls.localizedAiActionKeys(CLEAR_REMINDERS.getAction()), CLEAR_REMINDERS.getAction());
        map.put(StringUtls.localizedAiActionKeys(SET_REMINDER.getAction()), SET_REMINDER.getAction());
        map.put(StringUtls.localizedAiActionKeys(SET_TIMED_REMINDER.getAction()), SET_TIMED_REMINDER.getAction());

        // UI panels
        map.put(StringUtls.localizedAiActionKeys(ACTIVATE.getAction()), ACTIVATE.getAction());
        map.put(StringUtls.localizedAiActionKeys(SHOW_TRANSACTIONS.getAction()), SHOW_TRANSACTIONS.getAction());
        map.put(StringUtls.localizedAiActionKeys(SHOW_CONTACTS.getAction()), SHOW_CONTACTS.getAction());
        map.put(StringUtls.localizedAiActionKeys(SHOW_NAVIGATION.getAction()), SHOW_NAVIGATION.getAction());
        map.put(StringUtls.localizedAiActionKeys(SHOW_CHAT_PANEL.getAction()), SHOW_CHAT_PANEL.getAction());
        map.put(StringUtls.localizedAiActionKeys(SHOW_INBOX_PANEL.getAction()), SHOW_INBOX_PANEL.getAction());
        map.put(StringUtls.localizedAiActionKeys(SHOW_SOCIAL_PANEL.getAction()), SHOW_SOCIAL_PANEL.getAction());
        map.put(StringUtls.localizedAiActionKeys(SHOW_HISTORY_PANEL.getAction()), SHOW_HISTORY_PANEL.getAction());
        map.put(StringUtls.localizedAiActionKeys(SHOW_SQUADRON.getAction()), SHOW_SQUADRON.getAction());
        map.put(StringUtls.localizedAiActionKeys(SHOW_STATUS_PANEL.getAction()), SHOW_STATUS_PANEL.getAction());
        map.put(StringUtls.localizedAiActionKeys(DISPLAY_CARRIER_MANAGEMENT.getAction()), DISPLAY_CARRIER_MANAGEMENT.getAction());
        map.put(StringUtls.localizedAiActionKeys(OPEN_GALAXY_MAP.getAction()), OPEN_GALAXY_MAP.getAction());
        map.put(StringUtls.localizedAiActionKeys(OPEN_SYSTEM_MAP.getAction()), OPEN_SYSTEM_MAP.getAction());
        map.put(StringUtls.localizedAiActionKeys(EXIT_CLOSE.getAction()), EXIT_CLOSE.getAction());

        // pirate massacre missions
        map.put(StringUtls.localizedAiActionKeys(RECON_PROVIDER_SYSTEM.getAction()), RECON_PROVIDER_SYSTEM.getAction());
        map.put(StringUtls.localizedAiActionKeys(NAVIGATE_TO_PIRATE_MISSION_PROVIDER.getAction()), NAVIGATE_TO_PIRATE_MISSION_PROVIDER.getAction());
        map.put(StringUtls.localizedAiActionKeys(ANALYZE_MISSIONS.getAction()), ANALYZE_MISSIONS.getAction());
        map.put(StringUtls.localizedAiActionKeys(PIRATE_MISSION_PROGRESS.getAction()), PIRATE_MISSION_PROGRESS.getAction());
        map.put(StringUtls.localizedAiActionKeys(FIND_HUNTING_GROUNDS.getAction()), FIND_HUNTING_GROUNDS.getAction());
        map.put(StringUtls.localizedAiActionKeys(RECON_TARGET_SYSTEM.getAction()), RECON_TARGET_SYSTEM.getAction());
        map.put(StringUtls.localizedAiActionKeys(IGNORE_HUNTING_GROUND.getAction()), IGNORE_HUNTING_GROUND.getAction());
        map.put(StringUtls.localizedAiActionKeys(CONFIRM_HUNTING_GROUND.getAction()), CONFIRM_HUNTING_GROUND.getAction());
        map.put(StringUtls.localizedAiActionKeys(CLEAR_ALL_ACTIVE_MISSIONS.getAction()), CLEAR_ALL_ACTIVE_MISSIONS.getAction());

        // science / mining / biology
        map.put(StringUtls.localizedAiActionKeys(ADD_MINING_TARGET.getAction()), ADD_MINING_TARGET.getAction());
        map.put(StringUtls.localizedAiActionKeys(REMOVE_MINING_TARGET.getAction()), REMOVE_MINING_TARGET.getAction());
        map.put(StringUtls.localizedAiActionKeys(CLEAR_MINING_TARGETS.getAction()), CLEAR_MINING_TARGETS.getAction());
        map.put(StringUtls.localizedAiActionKeys(MINING_ON_OFF.getAction()), MINING_ON_OFF.getAction());
        map.put(StringUtls.localizedAiActionKeys(FIND_BRAIN_TREES.getAction()), FIND_BRAIN_TREES.getAction());
        map.put(StringUtls.localizedAiActionKeys(FIND_MINING_SITE.getAction()), FIND_MINING_SITE.getAction());
        map.put(StringUtls.localizedAiActionKeys(FIND_FLEET_CARRIER_FUEL_MINING_SITE.getAction()), FIND_FLEET_CARRIER_FUEL_MINING_SITE.getAction());
        map.put(StringUtls.localizedAiActionKeys(NAVIGATE_TO_NEXT_BIO_SAMPLE.getAction()), NAVIGATE_TO_NEXT_BIO_SAMPLE.getAction());
        map.put(StringUtls.localizedAiActionKeys(OPEN_FSS.getAction()), OPEN_FSS.getAction());
        map.put(StringUtls.localizedAiActionKeys(FIND_VISTA_GENOMICS.getAction()), FIND_VISTA_GENOMICS.getAction());
        map.put(StringUtls.localizedAiActionKeys(DELETE_CODEX_ENTRY.getAction()), DELETE_CODEX_ENTRY.getAction());
        // FR-only action; other locales get a dead entry that no user will trigger
        map.put(StringUtls.localizedAiActionKeys(NAVIGATE_TO_SQUADRON_CARRIER.getAction()), NAVIGATE_TO_SQUADRON_CARRIER.getAction());

        map.put(StringUtls.localizedAiActionKeys(KEY_BINDINGS_ANALYSIS.getAction()), KEY_BINDINGS_ANALYSIS.getAction());
        map.put(StringUtls.localizedAiActionKeys(BIO_SAMPLE_IN_STAR_SYSTEM.getAction()), BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
        map.put(StringUtls.localizedAiActionKeys(EXOBIOLOGY_SAMPLES_ON_THIS_PLANET.getAction()), EXOBIOLOGY_SAMPLES_ON_THIS_PLANET.getAction());
        map.put(StringUtls.localizedAiActionKeys(DISTANCE_TO_LAST_BIO_SAMPLE.getAction()), DISTANCE_TO_LAST_BIO_SAMPLE.getAction());
        map.put(StringUtls.localizedAiActionKeys(PLANET_BIOME_ANALYSIS.getAction()), PLANET_BIOME_ANALYSIS.getAction());
        map.put(StringUtls.localizedAiActionKeys(QUERY_STELLAR_OBJETS.getAction()), QUERY_STELLAR_OBJETS.getAction());
        map.put(StringUtls.localizedAiActionKeys(QUERY_STELLAR_SIGNALS.getAction()), QUERY_STELLAR_SIGNALS.getAction());
        map.put(StringUtls.localizedAiActionKeys(QUERY_GEO_SIGNALS.getAction()), QUERY_GEO_SIGNALS.getAction());

        map.put(StringUtls.localizedAiActionKeys(SYSTEM_SECURITY_ANALYSIS.getAction()), SYSTEM_SECURITY_ANALYSIS.getAction());
        map.put(StringUtls.localizedAiActionKeys(TRADE_PROFILE_ANALYSIS.getAction()), TRADE_PROFILE_ANALYSIS.getAction());
        map.put(StringUtls.localizedAiActionKeys(DISTANCE_TO_BODY.getAction()), DISTANCE_TO_BODY.getAction());
        map.put(StringUtls.localizedAiActionKeys(LAST_SCAN_ANALYSIS.getAction()), LAST_SCAN_ANALYSIS.getAction());
        map.put(StringUtls.localizedAiActionKeys(MATERIALS_INVENTORY.getAction()), MATERIALS_INVENTORY.getAction());
        map.put(StringUtls.localizedAiActionKeys(PLANET_MATERIALS.getAction()), PLANET_MATERIALS.getAction());
        map.put(StringUtls.localizedAiActionKeys(EXPLORATION_PROFITS.getAction()), EXPLORATION_PROFITS.getAction());
        map.put(StringUtls.localizedAiActionKeys(CURRENT_LOCATION.getAction()), CURRENT_LOCATION.getAction());
        map.put(StringUtls.localizedAiActionKeys(FSD_TARGET_ANALYSIS.getAction()), FSD_TARGET_ANALYSIS.getAction());
        map.put(StringUtls.localizedAiActionKeys(PLOTTED_ROUTE_ANALYSIS.getAction()), PLOTTED_ROUTE_ANALYSIS.getAction());
        map.put(StringUtls.localizedAiActionKeys(TRADE_ROUTE_ANALYSIS.getAction()), TRADE_ROUTE_ANALYSIS.getAction());
        map.put(StringUtls.localizedAiActionKeys(LOCAL_OUTFITTING.getAction()), LOCAL_OUTFITTING.getAction());
        map.put(StringUtls.localizedAiActionKeys(LOCAL_SHIPYARD.getAction()), LOCAL_SHIPYARD.getAction());
        map.put(StringUtls.localizedAiActionKeys(CARGO_HOLD_CONTENTS.getAction()), CARGO_HOLD_CONTENTS.getAction());
        map.put(StringUtls.localizedAiActionKeys(PLAYER_PROFILE_ANALYSIS.getAction()), PLAYER_PROFILE_ANALYSIS.getAction());
        map.put(StringUtls.localizedAiActionKeys(SHIP_LOADOUT.getAction()), SHIP_LOADOUT.getAction());
        map.put(StringUtls.localizedAiActionKeys(STATION_DETAILS.getAction()), STATION_DETAILS.getAction());
        map.put(StringUtls.localizedAiActionKeys(TOTAL_BOUNTIES.getAction()), TOTAL_BOUNTIES.getAction());
        map.put(StringUtls.localizedAiActionKeys(DISTANCE_TO_BUBBLE.getAction()), DISTANCE_TO_BUBBLE.getAction());
        map.put(StringUtls.localizedAiActionKeys(TIME_IN_ZONE.getAction()), TIME_IN_ZONE.getAction());
        map.put(StringUtls.localizedAiActionKeys(REMINDER.getAction()), REMINDER.getAction());
        map.put(StringUtls.localizedAiActionKeys(ANALYZE_MARKETS.getAction()), ANALYZE_MARKETS.getAction());
    }
}
