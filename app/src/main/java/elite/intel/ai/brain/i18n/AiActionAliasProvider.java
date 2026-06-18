package elite.intel.ai.brain.i18n;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.Map;
import java.util.Set;

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
        map.put(StringUtls.localizedAiActionKeys(CommandIds.WAKEUP), CommandIds.WAKEUP);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.SLEEP), CommandIds.SLEEP);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.INTERRUPT), CommandIds.INTERRUPT);

        // Declared early so these win before power-management "balance power" and carrier-route entries can interfere
        map.put(StringUtls.localizedAiActionKeys(CommandIds.FIND_NEAREST_FLEET_CARRIER), CommandIds.FIND_NEAREST_FLEET_CARRIER);
        map.put(StringUtls.localizedAiActionKeys(FLEET_CARRIER_STATUS.getAction()), FLEET_CARRIER_STATUS.getAction());
        map.put(StringUtls.localizedAiActionKeys(SQUADRON_CARRIER_STATUS.getAction()), SQUADRON_CARRIER_STATUS.getAction());

        // navigation
        map.put(StringUtls.localizedAiActionKeys(CommandIds.CANCEL_TRADE_ROUTE), CommandIds.CANCEL_TRADE_ROUTE);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.NAVIGATE_TO_COORDINATES), CommandIds.NAVIGATE_TO_COORDINATES);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.NAVIGATE_TO_MISSION_TARGET), CommandIds.NAVIGATE_TO_MISSION_TARGET);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.NAVIGATE_TO_FLEET_CARRIER), CommandIds.NAVIGATE_TO_FLEET_CARRIER);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.NAVIGATE_TO_LANDING_ZONE), CommandIds.NAVIGATE_TO_LANDING_ZONE);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.NAVIGATE_TO_TRADE_STOP), CommandIds.NAVIGATE_TO_TRADE_STOP);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.NAVIGATE_FROM_MEMORY), CommandIds.NAVIGATE_FROM_MEMORY);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.CANCEL_NAVIGATION), CommandIds.CANCEL_NAVIGATION);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.SET_HOME_SYSTEM), CommandIds.SET_HOME_SYSTEM);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.NAVIGATE_TO_HOME_SYSTEM), CommandIds.NAVIGATE_TO_HOME_SYSTEM);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.RESET_HEAD_LOOK_AHEAD), CommandIds.RESET_HEAD_LOOK_AHEAD);

        if (status.isInMainShip() || isDryRun) {
            // navigation
            map.put(StringUtls.localizedAiActionKeys(CommandIds.TARGET_DESTINATION), CommandIds.TARGET_DESTINATION);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.JUMP_TO_HYPERSPACE), CommandIds.JUMP_TO_HYPERSPACE);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.DROP_FROM_SUPER_CRUISE), CommandIds.DROP_FROM_SUPER_CRUISE);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.ENTER_SUPER_CRUISE), CommandIds.ENTER_SUPER_CRUISE);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.LAUNCH_SHIP_DETACH_FROM_STATION), CommandIds.LAUNCH_SHIP_DETACH_FROM_STATION);
            // speed / throttle
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SET_SPEED_TO_ZERO_0_STOP_SHIP), CommandIds.SET_SPEED_TO_ZERO_0_STOP_SHIP);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.TAXI_TO_LANDING_PAD), CommandIds.TAXI_TO_LANDING_PAD);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SET_SPEED_25), CommandIds.SET_SPEED_25);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SET_SPEED_50), CommandIds.SET_SPEED_50);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SET_SPEED_75), CommandIds.SET_SPEED_75);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SET_SPEED_100), CommandIds.SET_SPEED_100);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.INCREASE_SPEED), CommandIds.INCREASE_SPEED);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.DECREASE_SPEED), CommandIds.DECREASE_SPEED);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SET_OPTIMAL_SPEED), CommandIds.SET_OPTIMAL_SPEED);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.DEPLOY_LANDING_GEAR), CommandIds.DEPLOY_LANDING_GEAR);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.RETRACT_LANDING_GEAR), CommandIds.RETRACT_LANDING_GEAR);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.REQUEST_DOCKING), CommandIds.REQUEST_DOCKING);
            // UI panels
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_FIGHTER_PANEL), CommandIds.SHOW_FIGHTER_PANEL);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.DEPLOY_SHIELD_CELL), CommandIds.DEPLOY_SHIELD_CELL);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.DEPLOY_CHAFF), CommandIds.DEPLOY_CHAFF);
            // combat
            map.put(StringUtls.localizedAiActionKeys(CommandIds.DEPLOY_HARDPOINTS), CommandIds.DEPLOY_HARDPOINTS);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.RETRACT_HARDPOINTS), CommandIds.RETRACT_HARDPOINTS);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.TARGET_SUBSYSTEM), CommandIds.TARGET_SUBSYSTEM);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.TARGET_WINGMAN_1), CommandIds.TARGET_WINGMAN_1);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.TARGET_WINGMAN_2), CommandIds.TARGET_WINGMAN_2);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.TARGET_WINGMAN_3), CommandIds.TARGET_WINGMAN_3);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.WING_NAV_LOCK), CommandIds.WING_NAV_LOCK);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.TARGET_HOSTILE_HIGHEST_THREAT), CommandIds.TARGET_HOSTILE_HIGHEST_THREAT);
            // vehicle deployment
            map.put(StringUtls.localizedAiActionKeys(CommandIds.DEPLOY_VEHICLE_SRV), CommandIds.DEPLOY_VEHICLE_SRV);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.DEPLOY_HEAT_SINK), CommandIds.DEPLOY_HEAT_SINK);
            // fighter orders
            map.put(StringUtls.localizedAiActionKeys(CommandIds.DEPLOY_FIGHTER), CommandIds.DEPLOY_FIGHTER);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.FIGHTER_DEFEND), CommandIds.FIGHTER_DEFEND);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.FIGHTER_ATTACK_TARGET), CommandIds.FIGHTER_ATTACK_TARGET);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.FIGHTER_HOLD_FIRE), CommandIds.FIGHTER_HOLD_FIRE);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.FIGHTER_RETURN_TO_SHIP), CommandIds.FIGHTER_RETURN_TO_SHIP);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.FIGHTER_FIRE_AT_WILL), CommandIds.FIGHTER_FIRE_AT_WILL);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SELECT_FIRE_GROUP_BY_NATO), CommandIds.SELECT_FIRE_GROUP_BY_NATO);
            // power (RU-only action; other locales get a dead entry that no user will trigger)
            map.put(StringUtls.localizedAiActionKeys(CommandIds.TRANSFER_POWER_TO_SHIP_SYSTEMS), CommandIds.TRANSFER_POWER_TO_SHIP_SYSTEMS);
            // neutron star routes
            map.put(StringUtls.localizedAiActionKeys(CommandIds.CALCULATE_NEUTRON_STAR_ROUTE), CommandIds.CALCULATE_NEUTRON_STAR_ROUTE);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.PLOT_ROUTE_NEXT_NEUTRON_STAR_WAYPOINT), CommandIds.PLOT_ROUTE_NEXT_NEUTRON_STAR_WAYPOINT);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.CLEAR_NEUTRON_ROUTE), CommandIds.CLEAR_NEUTRON_ROUTE);
        }

        if (status.isInMainShip() && !status.isDocked() || isDryRun) {
            map.put(StringUtls.localizedAiActionKeys(QUERY_STATIONS.getAction()), QUERY_STATIONS.getAction());
        }

        if (status.isInSrv() && status.isDocked() || isDryRun) {
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_STATION_SERVICES_PANEL), CommandIds.SHOW_STATION_SERVICES_PANEL);
        }

        if (status.isInMainShip() || status.isInSrv() || isDryRun) {
            // flight / ship systems
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SWITCH_TO_COMBAT_MODE), CommandIds.SWITCH_TO_COMBAT_MODE);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SWITCH_TO_ANALYSIS_MODE), CommandIds.SWITCH_TO_ANALYSIS_MODE);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.TOGGLE_CARGO_SCOOP), CommandIds.TOGGLE_CARGO_SCOOP);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.TOGGLE_NIGHT_VISION_ON_OFF), CommandIds.TOGGLE_NIGHT_VISION_ON_OFF);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.TOGGLE_LIGHTS_ON_OFF), CommandIds.TOGGLE_LIGHTS_ON_OFF);
            // UI panels
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_COMMANDER_PANEL), CommandIds.SHOW_COMMANDER_PANEL);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_CREW_PANEL), CommandIds.SHOW_CREW_PANEL);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_INTERNAL_PANEL), CommandIds.SHOW_INTERNAL_PANEL);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_MODULES_PANEL), CommandIds.SHOW_MODULES_PANEL);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_FIRE_GROUPS_PANEL), CommandIds.SHOW_FIRE_GROUPS_PANEL);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_INVENTORY_PANEL), CommandIds.SHOW_INVENTORY_PANEL);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_STORAGE_PANEL), CommandIds.SHOW_STORAGE_PANEL);
            // power
            map.put(StringUtls.localizedAiActionKeys(CommandIds.TRANSFER_POWER_TO_SHIELDS), CommandIds.TRANSFER_POWER_TO_SHIELDS);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.TRANSFER_POWER_TO_ENGINES), CommandIds.TRANSFER_POWER_TO_ENGINES);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.TRANSFER_POWER_TO_WEAPONS), CommandIds.TRANSFER_POWER_TO_WEAPONS);
            // vehicle deployment
            map.put(StringUtls.localizedAiActionKeys(CommandIds.DISEMBARK), CommandIds.DISEMBARK);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.EQUALIZE_POWER), CommandIds.EQUALIZE_POWER);
        }

        if (status.isInSrv() || isDryRun) {
            map.put(StringUtls.localizedAiActionKeys(CommandIds.DRIVE_ASSIST), CommandIds.DRIVE_ASSIST);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.RECOVER_SRV_VEHICLE_GET_ON_BOARD_SHIP), CommandIds.RECOVER_SRV_VEHICLE_GET_ON_BOARD_SHIP);
        }

        if (status.isInSrv() || status.isOnFoot() || isDryRun) {
            map.put(StringUtls.localizedAiActionKeys(CommandIds.DISMISS_SHIP_TO_ORBIT), CommandIds.DISMISS_SHIP_TO_ORBIT);
            map.put(StringUtls.localizedAiActionKeys(CommandIds.RETURN_TO_SURFACE), CommandIds.RETURN_TO_SURFACE);
        }

        // market / traders / brokers
        map.put(StringUtls.localizedAiActionKeys(CommandIds.FIND_RAW_MATERIAL_TRADER), CommandIds.FIND_RAW_MATERIAL_TRADER);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.FIND_ENCODED_MATERIAL_TRADER), CommandIds.FIND_ENCODED_MATERIAL_TRADER);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.FIND_MANUFACTURED_MATERIAL_TRADER), CommandIds.FIND_MANUFACTURED_MATERIAL_TRADER);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.FIND_HUMAN_TECHNOLOGY_BROKER), CommandIds.FIND_HUMAN_TECHNOLOGY_BROKER);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.FIND_GUARDIAN_TECHNOLOGY_BROKER), CommandIds.FIND_GUARDIAN_TECHNOLOGY_BROKER);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.FIND_COMMODITY), CommandIds.FIND_COMMODITY);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.FIND_INTERSTELLAR_FACTOR), CommandIds.FIND_INTERSTELLAR_FACTOR);

        // fleet carrier  bare "carrier" without "squadron" always means fleet/my carrier
        map.put(StringUtls.localizedAiActionKeys(CommandIds.SET_CARRIER_FUEL_RESERVE), CommandIds.SET_CARRIER_FUEL_RESERVE);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.CALCULATE_FLEET_CARRIER_ROUTE), CommandIds.CALCULATE_FLEET_CARRIER_ROUTE);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.ENTER_FLEET_CARRIER_DESTINATION), CommandIds.ENTER_FLEET_CARRIER_DESTINATION);
        map.put(StringUtls.localizedAiActionKeys(QUERY_CARRIERS.getAction()), QUERY_CARRIERS.getAction());
        map.put(StringUtls.localizedAiActionKeys(FLEET_CARRIER_ROUTE_ANALYSIS.getAction()), FLEET_CARRIER_ROUTE_ANALYSIS.getAction());
        //map.put(StringUtls.localizedAiActionKeys(FLEET_CARRIER_TRITIUM_SUPPLY.getAction()), FLEET_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put(StringUtls.localizedAiActionKeys(FLEET_CARRIER_ETA.getAction()), FLEET_CARRIER_ETA.getAction());
        map.put(StringUtls.localizedAiActionKeys(DISTANCE_TO_CARRIER.getAction()), DISTANCE_TO_CARRIER.getAction());

        // squadron carrier  must explicitly say "squadron carrier"
        map.put(StringUtls.localizedAiActionKeys(SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction()), SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put(StringUtls.localizedAiActionKeys(SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION.getAction()), SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION.getAction());
        map.put(StringUtls.localizedAiActionKeys(SQUADRON_CARRIER_ETA.getAction()), SQUADRON_CARRIER_ETA.getAction());

        // trade
        map.put(StringUtls.localizedAiActionKeys(CommandIds.CALCULATE_TRADE_ROUTE), CommandIds.CALCULATE_TRADE_ROUTE);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.LIST_TRADE_PARAMETERS), CommandIds.LIST_TRADE_PARAMETERS);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.MONETIZE_ROUTE), CommandIds.MONETIZE_ROUTE);

        map.put(StringUtls.localizedAiActionKeys(CommandIds.TRADE_PROFILE_SET_BUDGET), CommandIds.TRADE_PROFILE_SET_BUDGET);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.TRADE_PROFILE_SET_MAX_STOPS), CommandIds.TRADE_PROFILE_SET_MAX_STOPS);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.TRADE_PROFILE_SET_MAX_DISTANCE), CommandIds.TRADE_PROFILE_SET_MAX_DISTANCE);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.TRADE_PROFILE_TOGGLE_PROHIBITED_CARGO), CommandIds.TRADE_PROFILE_TOGGLE_PROHIBITED_CARGO);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.TRADE_PROFILE_TOGGLE_PLANETARY_PORTS), CommandIds.TRADE_PROFILE_TOGGLE_PLANETARY_PORTS);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.TRADE_PROFILE_TOGGLE_PERMIT_SYSTEMS), CommandIds.TRADE_PROFILE_TOGGLE_PERMIT_SYSTEMS);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.TRADE_PROFILE_TOGGLE_STRONGHOLDS), CommandIds.TRADE_PROFILE_TOGGLE_STRONGHOLDS);

        // announcements / app settings
        map.put(StringUtls.localizedAiActionKeys(CommandIds.TOGGLE_RADIO), CommandIds.TOGGLE_RADIO);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.TOGGLE_RADAR_ANNOUNCEMENTS), CommandIds.TOGGLE_RADAR_ANNOUNCEMENTS);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.TOGGLE_DISCOVERY_ANNOUNCEMENTS), CommandIds.TOGGLE_DISCOVERY_ANNOUNCEMENTS);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.TOGGLE_ROUTE_ANNOUNCEMENTS), CommandIds.TOGGLE_ROUTE_ANNOUNCEMENTS);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.TOGGLE_ALL_ANNOUNCEMENTS), CommandIds.TOGGLE_ALL_ANNOUNCEMENTS);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.HONK), CommandIds.HONK);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.CLEAR_REMINDERS), CommandIds.CLEAR_REMINDERS);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.SET_REMINDER), CommandIds.SET_REMINDER);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.SET_TIMED_REMINDER), CommandIds.SET_TIMED_REMINDER);

        // UI panels
        map.put(StringUtls.localizedAiActionKeys(CommandIds.ACTIVATE_UI_CONTROL), CommandIds.ACTIVATE_UI_CONTROL);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_TRANSACTIONS_PANEL), CommandIds.SHOW_TRANSACTIONS_PANEL);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_CONTACTS_PANEL), CommandIds.SHOW_CONTACTS_PANEL);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_NAVIGATION_PANEL), CommandIds.SHOW_NAVIGATION_PANEL);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_CHAT_COMMS_PANEL), CommandIds.SHOW_CHAT_COMMS_PANEL);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_EMAIL_INBOX_PANEL), CommandIds.SHOW_EMAIL_INBOX_PANEL);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_SOCIAL_PANEL), CommandIds.SHOW_SOCIAL_PANEL);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_HISTORY_PANEL), CommandIds.SHOW_HISTORY_PANEL);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_SQUADRON_PANEL), CommandIds.SHOW_SQUADRON_PANEL);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.SHOW_STATUS_PANEL), CommandIds.SHOW_STATUS_PANEL);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.DISPLAY_FLEET_CARRIER_MANAGEMENT_PANEL), CommandIds.DISPLAY_FLEET_CARRIER_MANAGEMENT_PANEL);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.DISPLAY_OPEN_GALAXY_MAP), CommandIds.DISPLAY_OPEN_GALAXY_MAP);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.DISPLAY_OPEN_SYSTEM_MAP), CommandIds.DISPLAY_OPEN_SYSTEM_MAP);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.EXIT_CLOSE), CommandIds.EXIT_CLOSE);

        // pirate massacre missions
        map.put(StringUtls.localizedAiActionKeys(CommandIds.NAVIGATE_TO_MISSION_PROVIDER), CommandIds.NAVIGATE_TO_MISSION_PROVIDER);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.NAVIGATE_TO_PIRATE_MISSION_PROVIDER), CommandIds.NAVIGATE_TO_PIRATE_MISSION_PROVIDER);
        map.put(StringUtls.localizedAiActionKeys(ANALYZE_MISSIONS.getAction()), ANALYZE_MISSIONS.getAction());
        map.put(StringUtls.localizedAiActionKeys(PIRATE_MISSION_PROGRESS.getAction()), PIRATE_MISSION_PROGRESS.getAction());
        map.put(StringUtls.localizedAiActionKeys(CommandIds.FIND_HUNTING_GROUNDS), CommandIds.FIND_HUNTING_GROUNDS);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.RECON_HUNTING_GROUND), CommandIds.RECON_HUNTING_GROUND);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.IGNORE_HUNTING_GROUND), CommandIds.IGNORE_HUNTING_GROUND);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.CONFIRM_HUNTING_GROUND), CommandIds.CONFIRM_HUNTING_GROUND);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.CLEAR_ACTIVE_MISSIONS), CommandIds.CLEAR_ACTIVE_MISSIONS);

        // science / mining / biology
        map.put(StringUtls.localizedAiActionKeys(CommandIds.ADD_MINING_TARGET), CommandIds.ADD_MINING_TARGET);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.REMOVE_MINING_TARGET), CommandIds.REMOVE_MINING_TARGET);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.CLEAR_MINING_TARGETS), CommandIds.CLEAR_MINING_TARGETS);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.TOGGLE_MINING_ANNOUNCEMENTS), CommandIds.TOGGLE_MINING_ANNOUNCEMENTS);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.FIND_BRAIN_TREES), CommandIds.FIND_BRAIN_TREES);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.FIND_MINING_SITE), CommandIds.FIND_MINING_SITE);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.FIND_TRITIUM_MINING_SITE), CommandIds.FIND_TRITIUM_MINING_SITE);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.NAVIGATE_TO_BIO_SAMPLE_CODEX_ENTRY), CommandIds.NAVIGATE_TO_BIO_SAMPLE_CODEX_ENTRY);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.OPEN_FSS_SCAN_SYSTEM), CommandIds.OPEN_FSS_SCAN_SYSTEM);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.FIND_VISTA_GENOMICS), CommandIds.FIND_VISTA_GENOMICS);
        map.put(StringUtls.localizedAiActionKeys(CommandIds.DELETE_CODEX_ENTRY), CommandIds.DELETE_CODEX_ENTRY);
        // FR-only action; other locales get a dead entry that no user will trigger
        map.put(StringUtls.localizedAiActionKeys(CommandIds.NAVIGATE_TO_SQUADRON_CARRIER), CommandIds.NAVIGATE_TO_SQUADRON_CARRIER);

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
