package elite.intel.ai.brain.actions;

import elite.intel.ai.brain.actions.handlers.query.*;// @formatter:off
import static elite.intel.ai.brain.commons.AiEndPoint.CONNECTION_CHECK_COMMAND;
public enum Queries {

    /// ########################################################################################################################################
    CONNECTION_CHECK                    (CONNECTION_CHECK_COMMAND, ConnectionCheckHandler.class),
    BIO_SAMPLE_IN_STAR_SYSTEM           ("query_bio_scans_and_samples_in_star_system",  AnalyzeBioScansStarSystemHandler.class),
    EXOBIOLOGY_SAMPLES_ON_THIS_PLANET   ("query_exobiology_samples",                    AnalyzeBioSamplesPlanetSurfaceHandler.class),
    QUERY_STELLAR_OBJETS                ("query_stellar_objects",                       AnalyzeStellarObjectsHandler.class),
    QUERY_STELLAR_SIGNALS               ("query_signals_in_star_system",                AnalyzeStellarSignalsHandler.class),
    QUERY_GEO_SIGNALS                   ("query_geo_signals",                           AnalyzeGeologyInStarSystemHandler.class),
    QUERY_STATIONS                      ("query_stations",                              AnalyzeStationsHandler.class),
    ANALYZE_MARKETS                     ("query_markets",                               AnalyzeMarkets.class),
    QUERY_CARRIERS                      ("query_carriers",                              AnalyzeCarriersHandler.class),
    KEY_BINDINGS_ANALYSIS               ("check_missing_key_bindings",                  AnalyzeMisingKeyBindingHandler.class),
    SYSTEM_SECURITY_ANALYSIS            ("query_system_security",                       AnalyzeSystemSecurityHandler.class),
    TRADE_PROFILE_ANALYSIS              ("query_trade_profile",                         AnalyzeTradeProfileHandler.class),
    DISTANCE_TO_BODY                    ("query_distance_to_body",                      AnalyzeDistanceToStellarObject.class),
    LAST_SCAN_ANALYSIS                  ("query_last_scan",                             AnalyzeLastScanHandler.class),
    MATERIALS_INVENTORY                 ("query_material_inventory",                    AnalyseMaterialsHandler.class),
    PLANET_MATERIALS                    ("query_planet_materials",                      AnalyzeMaterialsOnPlanetHandler.class),
    EXPLORATION_PROFITS                 ("query_exploration_profits",                   AnalyzeExplorationProfitsHandler.class),
    CURRENT_LOCATION                    ("query_current_location",                      AnalyzeCurrentLocationHandler.class),
    FSD_TARGET_ANALYSIS                 ("query_fsd_target",                            AnalyzeFsdTargetHandler.class),
    TRADE_ROUTE_ANALYSIS                ("query_trade_route",                           AnalyzeTradeScheduleHandler.class),
    LOCAL_OUTFITTING                    ("query_local_outfitting",                      AnalyzeLocalOutfittingHandler.class),
    LOCAL_SHIPYARD                      ("query_local_shipyard",                        AnalyzeShipyardHandler.class),
    CARGO_HOLD_CONTENTS                 ("query_cargo_hold_contents",                   AnalyzeCargoHoldHandler.class),
    PLOTTED_ROUTE_ANALYSIS              ("query_ship_route_remaining_jumps",            AnalyzeRouterHandler.class),

    FLEET_CARRIER_ROUTE_ANALYSIS        ("query_fleet_carrier_route",                         AnalyzeFleetCarrierRouteHandler.class),
    FLEET_CARRIER_TRITIUM_SUPPLY        ("query_fleet_carrier_fuel",                          AnalyzeFleetCarrierFuelSupplyHandler.class),
    FLEET_CARRIER_ROUTE                 ("query_fleet_carrier_route",                         AnalyzeFleetCarrierFinalDestinationHandler.class),
    FLEET_CARRIER_STATUS                ("query_fleet_carrier_status_fuel_credit_finance",    AnalyzeFleetCarrierDataHandler.class),
    FLEET_CARRIER_ETA                   ("query_fleet_carrier_eta",                           AnalyzeFleetCarrierETAHandler.class),

    SQUADRON_CARRIER_ROUTE_ANALYSIS     ("query_squadron_carrier_route",                      AnalyzeSquadronCarrierRouteHandler.class),
    SQUADRON_CARRIER_TRITIUM_SUPPLY     ("query_squadron_carrier_fuel",                       AnalyzeSquadronCarrierFuelSupplyHandler.class),
    SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION ("query_squadron_carrier_final_destination",                      AnalyzeSquadronCarrierFinalDestinationHandler.class),
    SQUADRON_CARRIER_STATUS             ("query_squadron_carrier_status_fuel_credit_finance", AnalyzeSquadronCarrierDataHandler.class),
    SQUADRON_CARRIER_ETA                ("query_squadron_carrier_eta",                        AnalyzeSquadronCarrierETAHandler.class),

    DISTANCE_TO_CARRIER                 ("query_distance_to_carrier",                   AnalyzeDistanceFromFleetCarrierHandler.class),
    PIRATE_MISSION_PROGRESS             ("query_pirate_mission",                        AnalyzePirateMissionHandler.class),
    PLAYER_PROFILE_ANALYSIS             ("query_player_profile_rank_progress",          AnalyzePlayerProfile.class),
    SHIP_LOADOUT                        ("query_ship_loadout",                          AnalyzeShipLoadoutHandler.class),
    STATION_DETAILS                     ("query_station_details",                       StationDataHandler.class),
    TOTAL_BOUNTIES                      ("query_total_bounties",                        AnalyzeBountiesCollectedHandler.class),
    DISTANCE_TO_BUBBLE                  ("query_distance_to_bubble_earth_sol_civilization", AnalyzeDistanceFromTheBubble.class),
    DISTANCE_TO_LAST_BIO_SAMPLE         ("query_distance_to_bio_sample",                AnalyzeDistanceFromLastBioSample.class),
    TIME_IN_ZONE                        ("query_time",                                  TimeQueryHandler.class),
    PLANET_BIOME_ANALYSIS               ("query_biome_analysis",                        BiomeAnalyzerHandler.class),
    REMINDER                            ("query_reminder",                              RemindTargetDestinationHandler.class),
    ANALYZE_MISSIONS                    ("query_missions_and_rewards",                  AnalyzeMissionHandler.class),
    GENERAL_CONVERSATION                ("query_general_conversation",                  GeneralConversationHandler.class)
    ;
    /// ########################################################################################################################################


    private final String action;
    private final Class<? extends QueryHandler> handlerClass;

    Queries(String action, Class<? extends QueryHandler> handlerClass) {
        this.action = action;
        this.handlerClass = handlerClass;
    }

    public String getAction() {
        return action;
    }

    public Class<? extends QueryHandler> getHandlerClass() {
        return handlerClass;
    }
}