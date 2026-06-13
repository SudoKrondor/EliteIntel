package elite.intel.ai.brain.actions;

import elite.intel.ai.brain.actions.handlers.commands.*;

import static elite.intel.ai.hands.Bindings.GameCommand.*;


// @formatter:off
public enum Commands {


    LIGHTS_ON_OFF                                       ("toggle_lights_on_off", null, LightsOnOffHandler.class),
    ADD_MINING_TARGET                                   ("add_mining_target", null, AddMiningTargetHandler.class),
    REMOVE_MINING_TARGET                                ("remove_mining_target", null, RemoveMiningTargetHandler.class),
    CLEAR_MINING_TARGETS                                ("clear_mining_targets", null, ClearMiningTargetsHandler.class),
    INTERRUPT_TTS                                       ("interrupt", null, ShutUpHandler.class),

    FIND_RAW_MATERIAL_TRADER                            ("find_raw_material_trader", null, FindRawMaterialTraderHandler.class),
    FIND_ENCODED_MATERIAL_TRADER                        ("find_encoded_material_trader", null, FindEncodedMaterialTraderHandler.class),
    FIND_MANUFACTURED_MATERIAL_TRADER                   ("find_manufactured_material_trader", null, FindManufacturedMaterialTraderHandler.class),
    FIND_INTERSTELLAR_FACTOR                            ("find_interstellar_factor", null, PlotRouteToInterstellarFactorsHandler.class),

    FIND_HUNTING_GROUNDS                                ("find_hunting_grounds", null,  LocatePirateHuntingGrounds.class),
    RECON_TARGET_SYSTEM                                 ("recon_hunting_ground", null,  ReconPirateMissionTargetSystemHandler.class),
    RECON_PROVIDER_SYSTEM                               ("navigate_to_mission_provider", null,  ReconMissionProviderSystemHandler.class),
    IGNORE_HUNTING_GROUND                               ("ignore_hunting_ground", null, IgnoreHuntingGroundHandler.class),
    CONFIRM_HUNTING_GROUND                              ("confirm_hunting_ground", null, ConfirmHuntingGroundHandler.class),

    NAVIGATE_TO_NEXT_MISSION                            ("navigate_to_mission_target", null, NavigateToMissionDestination.class),
    NAVIGATE_TO_PIRATE_MISSION_TARGET_SYSTEM            ("navigate_to_pirate_mission_target", null,  NavigateToPirateMassacreMissionTargetHandler.class),
    NAVIGATE_TO_PIRATE_MISSION_PROVIDER                 ("navigate_to_pirate_mission_provider", null,  NavigateToToKnownPirateMassacreMissionProvider.class),
    NAVIGATE_TO_NEXT_TRADE_STOP                         ("navigate_to_trade_stop", null,  NavigateToNextTradeStopHandler.class),
    NAVIGATE_TO_ADDRESS_FROM_MEMORY                     ("navigate_from_memory", null, PasteFromMemoryHandler.class),
    CLEAR_ALL_ACTIVE_MISSIONS                           ("clear_active_missions", null,  ClearActiveMissionHandler.class),

    MONETIZE_ROUTE                                      ("monetize_route", null,  MonetizeRouteHandler.class),

    FIND_HUMAN_TECHNOLOGY_BROKER                        ("find_human_technology_broker", null,  FindHumanTechnologyBrokerHandler.class),
    FIND_GUARDIAN_TECHNOLOGY_BROKER                     ("find_guardian_technology_broker", null,  FindGuadrianTechnologyBroker.class),

    FIND_VISTA_GENOMICS                                 ("find_vista_genomics", null,  FindVistaGenomicsHandler.class),
    FIND_BRAIN_TREES                                    ("find_brain_trees", null,  FindBrainTreesHandler.class),
    FIND_FLEET_CARRIER_FUEL_MINING_SITE                 ("find_tritium_mining_site", null,  FindCarrierFuelMiningSiteHandler.class),
    FIND_MINING_SITE                                    ("find_mining_site", null, FindMiningSiteHandler.class),

    FIND_NEAREST_FLEET_CARRIER                          ("find_nearest_fleet_carrier", null,  FindNearestFleetCarrierHandler.class),

    FIND_COMMODITY                                      ("find_commodity", null, FindCommodityHandler.class),
    SET_HOME_SYSTEM                                     ("set_home_system", null,  SetCurrentStarAsHomeSystem.class),
    SET_RADIO_TRANSMISSION_MODE                         ("toggle_radio", null,  SetRadioTransmissionOnOff.class),
    SET_RADAR_CONTACT_ANNOUNCEMENT                      ("toggle_radar_announcements", null, RadarAnnouncementOnOffHandler.class),

    WAKEUP                                              ("wakeup", null, StartListeningHandler.class),
    SLEEP                                               ("sleep", null, IgnoreMeHandler.class),

    NAVIGATE_TO_TARGET                                  ("navigate_to_coordinates", null,  NavigateToCoordinatesHandler.class),
    NAVIGATION_OFF                                      ("cancel_navigation", null,  NavigationOnOffHandler.class),
    CANCEL_TRADE_ROUTE                                  ("cancel_trade_route", null,  CancelTradeRouteHandler.class),
    DISCOVERY_ON_OFF                                    ("toggle_discovery_announcements", null, DiscoveryOnOffHandler.class),
    MINING_ON_OFF                                       ("toggle_mining_announcements", null, MiningOnOffHandler.class),
    ROUTE_ON_OFF                                        ("toggle_route_announcements", null, RouteAnnouncementsOnOffHandler.class),
    TOGGLE_ALL_ANNOUNCEMENTS                            ("toggle_all_announcements", null, ToggleAllAnnouncementsHandler.class),
    HONK_THE_SYSTEM                                     ("honk", null, HonkTheSystemHandler.class),

    INCREASE_SPEED_BY                                   ("increase_speed", BINDING_INCREASE_SPEED.getGameBinding(),  SpeedPlusControlHandler.class),
    DECREASE_SPEED_BY                                   ("decrease_speed", BINDING_DECREASE_SPEED.getGameBinding(),  SpeedMinusControlHandler.class),

    /// Commands that have a specific handler impl (which uses N bindings inside)
    INCREASE_ENGINES_POWER                              ("transfer_power_to_engines", null,  SetPowerToEnginesHandler.class),
    NAVIGATE_TO_NEXT_BIO_SAMPLE                         ("navigate_to_bio_sample_codex_entry", null,  NavigateToNextCodexEntry.class),
    INCREASE_SHIELDS_POWER                              ("transfer_power_to_shields", null,  SetPowerToSystemsHandler.class),
    INCREASE_SYSTEMS_POWER                              ("transfer_power_to_ship_systems", null,  SetPowerToSystemsHandler.class),
    INCREASE_WEAPONS_POWER                              ("transfer_power_to_weapons", null,  SetPowerToWeaponsHandler.class),
    RESET_POWER                                         ("reset_power", null,  ResetPowerSettings.class),

    OPEN_GALAXY_MAP                                     ("display_open_galaxy_map", null,  OpenGalaxyMapHandler.class),
    OPEN_SYSTEM_MAP                                     ("display_open_system_map", null,  OpenLocalMapHandler.class),


    DISPLAY_RADAR_PANEL                                 ("display_radar_panel", null,  DisplayRadarPanelHandler.class),
    ///
    DISPLAY_CARRIER_MANAGEMENT                          ("display_fleet_carrier_management_panel", null,  OpenFleetCarrierManagementHandler.class    ),
    TOGGLE_CARGO_SCOOP                                  ("toggle_cargo_scoop", null,  CargoScoopHandler.class),
    RETRACT_HARDPOINTS                                  ("retract_hardpoints", null,  RetractHardpointsHandler.class),
    DEPLOY_HARDPOINTS                                   ("deploy_hardpoints", null,  DeployHardpointsHandler.class),
    DEPLOY_LANDING_GEAR                                 ("deploy_landing_gear", null,  DeployLandingGearHandler.class),
    RETRACT_LANDING_GEAR                                ("retract_landing_gear", null,  RetractLandingGearHandler.class),

    JUMP_TO_HYPERSPACE                                  ("jump_to_hyperspace", null,  JumpToHyperspaceHandler.class),
    ENTER_SUPER_CRUISE                                  ("enter_super_cruise", null,  SuperCruiseHandler.class),

    DROP_FROM_SUPER_CRUISE                              ("drop_from_super_cruise", null,  DropFromFtlHandler.class),
    ACTIVATE_ANALYSIS_MODE                              ("switch_to_analysis_mode", null,  ActivateAnalysisModeHandler.class),
    ACTIVATE_COMBAT_MODE                                ("switch_to_combat_mode", null,  ActivateCombatModeHandler.class),
    NAVIGATE_TO_FLEET_CARRIER                           ("navigate_to_fleet_carrier", null,  NavigateToMyFleetCarrier.class),
    NAVIGATE_TO_SQUADRON_CARRIER                        ("navigate_to_squadron_carrier", null,  NavigateToMySquadronCarrier.class),
    SET_OPTIMAL_SPEED                                   ("set_optimal_speed", null,  SetOptimalSpeedHandler.class),
    TAKE_ME_HOME                                        ("navigate_to_home_system", null,  NavigateToHomeHandler.class),
    OPEN_FSS                                            ("open_fss_scan_system", null,  DisplayFssAndScanHandler.class),
    CALCULATE_NEUTRON_STAR_ROUTE                        ("calculate_neutron_star_route", null,  CalculateNeutronStarRouteHandler.class),
    PLOT_ROUTE_TO_NEXT_NEUTRON_STAR                     ("plot_route_next_neutron_star_waypoint", null,PlotRouteToNextNeutronStarHandler.class),
    CLEAR_NEUTRON_ROUTE                                ("clear_neutron_route", null, ClearNeutronRouteHandler.class),

    GET_HEADING_TO_LZ                                   ("navigate_to_landing_zone", null,  NavigateToLandingZone.class),
    DEPLOY_SRV                                          ("deploy_vehicle_srv", null,  DeploySrvHandler.class),
    DISEMBARK                                           ("disembark", null,  DisembarkHandler.class),
    RESET_HEAD_LOOK                                     ("reset_head_look_ahead",  null, LookAheadHandler.class),

    DEPLOY_FIGHTER                                      ("deploy_fighter", null,  DeployFighterHandler.class),
    RECOVER_SRV                                         ("recover_srv_vehicle_get_on_board_ship", null,  RecoverSrvHandler.class),
    CALCULATE_FLEET_CARRIER_ROUTE                       ("calculate_fleet_carrier_route", null,  CalculateFleetCarrierRouteHandler.class),
    ENTER_FLEET_CARRIER_DESTINATION                     ("enter_fleet_carrier_destination", null,  EnterNextCarrierDestinationHandler.class),

    CALCULATE_TRADE_ROUTE                               ("calculate_trade_route", null,  CalculateTradeRouteHandler.class),
    CHANGE_TRADE_PROFILE_SET_STARTING_BUDGET            ("trade_profile_set_budget", null,  ChangeTradeProfileSetStartingBudgetHandler.class),
    CHANGE_TRADE_PROFILE_SET_MAX_NUMBER_OF_STOPS        ("trade_profile_set_max_stops", null,  ChangeTradeProfileSetMaxStopsHandler.class),
    CHANGE_TRADE_PROFILE_SET_MAX_DISTANCE_FROM_ENTRY    ("trade_profile_set_max_distance", null,  ChangeTradeProfileSetMaxDistanceFromEntryHandler.class),
    CHANGE_TRADE_PROFILE_SET_ALLOW_PROHIBITED_CARGO     ("trade_profile_toggle_prohibited_cargo", null, ChangeTradeProfileSetAllowProhibitedCargoHandler.class),
    CHANGE_TRADE_PROFILE_SET_ALLOW_PLANETARY_PORT       ("trade_profile_toggle_planetary_ports", null, ChangeTradeProfileSetIncluidePlanetaryPortsHandler.class),
    CHANGE_TRADE_PROFILE_SET_ALLOW_PERMIT_SYSTEMS       ("trade_profile_toggle_permit_systems", null, ChangeTradeProfileAllowPermitSystemsHandler.class),
    CHANGE_TRADE_PROFILE_SET_ALLOW_STRONGHOLDS          ("trade_profile_toggle_strongholds", null, ChangeTradeProfileSetAllowEnemyStrongHoldsHandler.class),
    LIST_TRADE_ROUTE_PARAMETERS                         ("list_trade_parameters", null,  ListAvailableTradeRouteProfilesHandler.class),

    /// Generic simple commands. no parameters, but require binding
    ACTIVATE                                            ("activate_ui_control", BINDING_ACTIVATE.getGameBinding(), SimpleCommandActionHandler.class),
    SHOW_TRANSACTIONS                                   ("show_transactions_panel", null, OpenTransactionHandler.class),
    SHOW_CONTACTS                                       ("show_contacts_panel", null, OpenContactsHandler.class),
    SHOW_NAVIGATION                                     ("show_navigation_panel", null, OpenNavigationHandler.class),
    SHOW_CHAT_PANEL                                     ("show_chat_comms_panel", null, OpenCommsPanelHandler.class   ),
    SHOW_INBOX_PANEL                                    ("show_email_inbox_panel", null, OpenInboxPanelHandler.class),
    SHOW_SOCIAL_PANEL                                   ("show_social_panel", null, OpenSocialPanelHandler.class),
    SHOW_HISTORY_PANEL                                  ("show_history_panel", null, OpenHistoryPanelHandler.class),
    SHOW_SQUADRON                                       ("show_squadron_panel", null, OpenSquadronHandler.class),
    SHOW_COMMANDER_PANEL                                ("show_commander_panel", null, OpenCommanderPanel.class),
    SHOW_FIGHTER_PANEL                                  ("show_fighter_panel", null, OpenFighterPanelHandler.class),
    SHOW_CREW                                           ("show_crew_panel", null, OpenCrewPanelHandler.class),
    SHOW_INTERNAL_PANEL                                 ("show_internal_panel", null, OpenInternalPanelHandler.class),
    SHOW_MODULES_PANEL                                  ("show_modules_panel", null, OpenModulesPanelHandler.class),
    SHOW_FIRE_GROUPS                                    ("show_fire_groups_panel", null, OpenFireGroupsPanelHandler.class),
    SHOW_INVENTORY_PANEL                                ("show_inventory_panel", null, OpenInventoryHandler.class),
    SHOW_STORAGE_PANEL                                  ("show_storage_panel", null, OpenStoragePanelHandler.class),
    SHOW_STATUS_PANEL                                   ("show_status_panel", null, OpenStatusPanelHandler.class),
    SHOW_STATION_SERVICES                               ("show_station_services_panel", null, OpenStationServicesHandler.class),
    LAUNCH_SHIP                                         ("launch_ship_detach_from_station", null, LaunchShipHandler.class),
    EXIT_CLOSE                                          ("exit_close", null, ClosePanelHandler.class),


    REQUEST_DOCKING                                     ("request_docking", null,  RequestDockingHandler.class),
    NIGHT_VISION_ON_OFF                                 ("toggle_night_vision_on_off", BINDING_NIGHT_VISION_TOGGLE.getGameBinding(), ToggleNightVision.class),

    CYCLE_NEXT_PAGE                                     ("cycle_next_page", BINDING_CYCLE_NEXT_PAGE.getGameBinding(), SimpleCommandActionHandler.class),
    CYCLE_NEXT_PANEL                                    ("cycle_next_panel", BINDING_CYCLE_NEXT_PANEL.getGameBinding(), SimpleCommandActionHandler.class),
    CYCLE_PREVIOUS_PAGE                                 ("cycle_previous_page", BINDING_CYCLE_PREVIOUS_PAGE.getGameBinding(), SimpleCommandActionHandler.class),
    CYCLE_PREVIOUS_PANEL                                ("cycle_previous_panel", BINDING_CYCLE_PREVIOUS_PANEL.getGameBinding(), SimpleCommandActionHandler.class),

    DEPLOY_HEAT_SINK                                    ("deploy_heat_sink", BINDING_DEPLOY_HEAT_SINK.getGameBinding(), SimpleCommandActionHandler.class),
    DEPLOY_SHIELD_CELL                                  ("deploy_shield_cell", BINDING_USE_SHIELD_CELL.getGameBinding(), SimpleCommandActionHandler.class),
    DEPLOY_CHAFF                                        ("deploy_chaff", BINDING_FIRE_CHAFF_LAUNCHER.getGameBinding(), SimpleCommandActionHandler.class),
    DRIVE_ASSIST                                        ("drive_assist", BINDING_DRIVE_ASSIST.getGameBinding(), SimpleCommandActionHandler.class),

    TARGET_SUB_SYSTEM                                   ("target_subsystem", null, TargetSubSystemHandler.class),
    DISMISS_SHIP                                        ("dismiss_ship_to_orbit", null, DismissRecallShip.class),
    RETURN_TO_SURFACE                                   ("return_to_surface", null, DismissRecallShip.class),

    FIGHTER_REQUEST_DEFENSIVE_BEHAVIOUR                 ("fighter_defend", BINDING_REQUEST_DEFENSIVE_BEHAVIOUR.getGameBinding(), SimpleCommandActionHandler.class),
    FIGHTER_REQUEST_FOCUS_TARGET                        ("fighter_attack_target", BINDING_REQUEST_FOCUS_TARGET.getGameBinding(), SimpleCommandActionHandler.class),
    FIGHTER_REQUEST_HOLD_FIRE                           ("fighter_hold_fire", BINDING_REQUEST_HOLD_FIRE.getGameBinding(), SimpleCommandActionHandler.class),
    FIGHTER_REQUEST_REQUEST_DOCK                        ("fighter_return_to_ship", BINDING_REQUEST_REQUEST_DOCK.getGameBinding(), SimpleCommandActionHandler.class),
    FIGHTER_OPEN_ORDERS                                 ("fighter_fire_at_will", OPEN_ORDERS.getGameBinding(), SimpleCommandActionHandler.class),
    SET_SPEED_ZERO                                      ("set_speed_to_zero_0_stop_ship", BINDING_SET_SPEED_ZERO.getGameBinding(), SimpleCommandActionHandler.class),
    TAXI                                                ("taxi_to_landing_pad", BINDING_SET_SPEED_ZERO.getGameBinding(), SimpleCommandActionHandler.class),
    SET_SPEED25                                         ("set_speed_25", BINDING_SET_SPEED25.getGameBinding(), SimpleCommandActionHandler.class),
    SET_SPEED50                                         ("set_speed_50", BINDING_SET_SPEED50.getGameBinding(), SimpleCommandActionHandler.class),
    SET_SPEED75                                         ("set_speed_75", BINDING_SET_SPEED75.getGameBinding(), SimpleCommandActionHandler.class),
    SET_SPEED100                                        ("set_speed_100", BINDING_SET_SPEED100.getGameBinding(), SimpleCommandActionHandler.class),

    SET_CARRIER_FUEL_RESERVE                            ("set_carrier_fuel_reserve", null,  SetFleetCarrierFuelReserveHandler.class),

    SELECT_HIGHEST_THREAT                               ("target_hostile_highest_threat", BINDING_SELECT_HIGHEST_THREAT.getGameBinding(), SimpleCommandActionHandler.class),
    TARGET_DESTINATION                                  ("target_destination", BINDING_TARGET_NEXT_ROUTE_SYSTEM.getGameBinding(), SimpleCommandActionHandler.class),

    TARGET_WINGMAN0                                     ("target_wingman_1", BINDING_TARGET_WINGMAN0.getGameBinding(), SimpleCommandActionHandler.class),
    TARGET_WINGMAN1                                     ("target_wingman_2", BINDING_TARGET_WINGMAN1.getGameBinding(), SimpleCommandActionHandler.class),
    TARGET_WINGMAN2                                     ("target_wingman_3", BINDING_TARGET_WINGMAN2.getGameBinding(), SimpleCommandActionHandler.class),
    WING_NAV_LOCK                                       ("wing_nav_lock", BINDING_WING_NAV_LOCK.getGameBinding(), SimpleCommandActionHandler.class),
    CLEAR_REMINDERS                                     ("clear_reminders", null, ClearReminderHandler.class),
    SET_REMINDER                                        ("set_reminder", null, SetReminderHandler.class),
    SET_TIMED_REMINDER                                  ("set_timed_reminder", null, SetTimedReminderHandler.class),
    DELETE_CODEX_ENTRY                                  ("delete_codex_entry", null, DeleteCodexEntryHandler.class),
    SELECT_FIRE_GROUP_BY_NATO                           ("select_fire_group_by_nato", null, SelectFireGroupByNatoHandler.class),
    IGNORE_NONSENSE                                     ("ignore_nonsensical_input", null, IgnoreNonSensicalInputHandler.class),
    ;

    ///
    private final String action;
    private final String binding;
    private final Class<? extends CommandHandler> handlerClass;

    Commands(String action, String binding, Class<? extends CommandHandler> handlerClass) {
        this.action = action;
        this.binding = binding;
        this.handlerClass = handlerClass;
    }

    public static String getGameBinding(String action) {
        Commands[] values = values();
        for (Commands command : values) {
            if (action.equalsIgnoreCase(command.getAction())) {
                return command.getBinding();
            }
        }
        return null;
    }

    public String getAction() {
        return action;
    }

    public String getBinding() {
        return binding;
    }

    public Class<? extends CommandHandler> getHandlerClass() {
        return handlerClass;
    }
}