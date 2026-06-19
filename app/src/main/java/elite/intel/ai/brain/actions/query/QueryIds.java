package elite.intel.ai.brain.actions.query;

import elite.intel.ai.brain.commons.AiEndPoint;

/**
 * Single source of truth for built-in query id strings. Each constant's name mirrors the
 * matching {@code Queries} enum element; the value is the exact action id that element
 * carries. Leaf class: parallel to CommandIds, referenced by the self-describing query
 * wrappers so ids are no longer scattered literals.
 */
public final class QueryIds {

    private QueryIds() {}

    public static final String CONNECTION_CHECK = AiEndPoint.CONNECTION_CHECK_COMMAND;
    public static final String BIO_SAMPLE_IN_STAR_SYSTEM = "query_bio_scans_and_samples_in_star_system";
    public static final String EXOBIOLOGY_SAMPLES_ON_THIS_PLANET = "query_exobiology_samples";
    public static final String QUERY_STELLAR_OBJETS = "query_stellar_objects";
    public static final String QUERY_STELLAR_SIGNALS = "query_signals_in_star_system";
    public static final String QUERY_GEO_SIGNALS = "query_geo_signals";
    public static final String QUERY_STATIONS = "query_stations";
    public static final String ANALYZE_MARKETS = "query_markets";
    public static final String QUERY_CARRIERS = "query_carriers";
    public static final String KEY_BINDINGS_ANALYSIS = "check_missing_key_bindings";
    public static final String SYSTEM_SECURITY_ANALYSIS = "query_system_security";
    public static final String TRADE_PROFILE_ANALYSIS = "query_trade_profile";
    public static final String DISTANCE_TO_BODY = "query_distance_to_body";
    public static final String LAST_SCAN_ANALYSIS = "query_last_scan";
    public static final String MATERIALS_INVENTORY = "query_material_inventory";
    public static final String PLANET_MATERIALS = "query_planet_materials";
    public static final String EXPLORATION_PROFITS = "query_exploration_profits";
    public static final String CURRENT_LOCATION = "query_current_location";
    public static final String FSD_TARGET_ANALYSIS = "query_fsd_target";
    public static final String TRADE_ROUTE_ANALYSIS = "query_trade_route";
    public static final String LOCAL_OUTFITTING = "query_local_outfitting";
    public static final String LOCAL_SHIPYARD = "query_local_shipyard";
    public static final String CARGO_HOLD_CONTENTS = "query_cargo_hold_contents";
    public static final String PLOTTED_ROUTE_ANALYSIS = "query_ship_route_remaining_jumps";
    public static final String FLEET_CARRIER_ROUTE_ANALYSIS = "query_fleet_carrier_route";
    public static final String FLEET_CARRIER_FINAL_DESTINATION = "query_fleet_carrier_final_destination";
    public static final String FLEET_CARRIER_STATUS = "query_fleet_carrier_status_fuel_credit_finance";
    public static final String FLEET_CARRIER_ETA = "query_fleet_carrier_eta";
    public static final String SQUADRON_CARRIER_ROUTE_ANALYSIS = "query_squadron_carrier_route";
    public static final String SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION = "query_squadron_carrier_final_destination";
    public static final String SQUADRON_CARRIER_STATUS = "query_squadron_carrier_status_fuel_credit_finance";
    public static final String SQUADRON_CARRIER_ETA = "query_squadron_carrier_eta";
    public static final String DISTANCE_TO_CARRIER = "query_distance_to_carrier";
    public static final String PIRATE_MISSION_PROGRESS = "query_pirate_mission";
    public static final String PLAYER_PROFILE_ANALYSIS = "query_player_profile_rank_progress";
    public static final String SHIP_LOADOUT = "query_ship_loadout";
    public static final String STATION_DETAILS = "query_station_details";
    public static final String TOTAL_BOUNTIES = "query_total_bounties";
    public static final String DISTANCE_TO_BUBBLE = "query_distance_to_bubble_earth_sol_civilization";
    public static final String DISTANCE_TO_LAST_BIO_SAMPLE = "query_distance_to_bio_sample";
    public static final String TIME_IN_ZONE = "query_time";
    public static final String PLANET_BIOME_ANALYSIS = "query_biome_analysis";
    public static final String REMINDER = "query_reminder";
    public static final String ANALYZE_MISSIONS = "query_missions_and_rewards";
    public static final String GENERAL_CONVERSATION = "query_general_conversation";
}
