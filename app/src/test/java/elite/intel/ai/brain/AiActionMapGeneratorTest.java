package elite.intel.ai.brain;

import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.command.builtin.IgnoreNonsensicalInputCommand;
import elite.intel.ai.brain.actions.customcommand.CustomCommandDefinition;
import elite.intel.ai.brain.actions.customcommand.CustomCommandRegistry;
import elite.intel.ai.brain.actions.handlers.query.ConnectionCheckQueryCommand;
import elite.intel.ai.brain.actions.handlers.query.GeneralConversationQueryCommand;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Composition + ordering tests for the parallel {@link AiActionMapGenerator}.
 * <p>
 * Composition is checked against a frozen SNAPSHOT of the built-in action id set (language pinned
 * to EN in {@link #bootstrap()} for determinism). Only the built-in ids are compared: the floating
 * additions (mode fallback, connection-check) and custom-command ids are excluded, since they vary
 * by session mode and by the local custom_commands.json. Comparison is by id SET only - not phrases
 * (language-dependent) and not order (covered by {@link #carrierClusterOrderInvariant()}).
 */
class AiActionMapGeneratorTest {

    @BeforeAll
    static void bootstrap() {
        // Lightweight headless bootstrap WITHOUT HeadlessBootstrap.start() (no LLM endpoint, no sleep).
        Cypher.initializeKey();
        Database.init().close();
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        CustomCommandRegistry.getInstance().load();
        // Pin language so the built-in id snapshot is deterministic regardless of persisted aiLanguage.
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    /**
     * Frozen snapshot of the built-in action ids the generator must produce on EN (181 ids).
     * Excludes floating additions (general_conversation / ignore_nonsensical_input / connection_check)
     * and custom-command ids. Note: transfer_power_to_ship_systems is intentionally absent on EN
     * (RU-only alias, no EN bundle key). Regenerate via the dump diagnostic if the built-in set
     * legitimately changes.
     */
    private static final List<String> SNAPSHOT_BUILTIN_IDS = List.of(
            "activate_ui_control",
            "add_mining_target",
            "calculate_fleet_carrier_route",
            "calculate_neutron_star_route",
            "calculate_trade_route",
            "cancel_navigation",
            "cancel_trade_route",
            "clear_active_missions",
            "clear_mining_targets",
            "clear_neutron_route",
            "clear_reminders",
            "confirm_hunting_ground",
            "decrease_speed",
            "delete_codex_entry",
            "deploy_chaff",
            "deploy_fighter",
            "deploy_hardpoints",
            "deploy_heat_sink",
            "deploy_landing_gear",
            "deploy_shield_cell",
            "deploy_vehicle_srv",
            "discovery_scan_honk",
            "disembark",
            "dismiss_ship_to_orbit",
            "display_fleet_carrier_management_panel",
            "display_open_galaxy_map",
            "display_open_system_map",
            "drive_assist",
            "drop_from_super_cruise",
            "enter_fleet_carrier_destination",
            "enter_super_cruise",
            "equalize_power",
            "exit_close",
            "fighter_attack_target",
            "fighter_defend",
            "fighter_fire_at_will",
            "fighter_hold_fire",
            "fighter_return_to_ship",
            "find_brain_trees",
            "find_commodity",
            "find_encoded_material_trader",
            "find_guardian_technology_broker",
            "find_human_technology_broker",
            "find_hunting_grounds",
            "find_interstellar_factor",
            "find_manufactured_material_trader",
            "find_mining_site",
            "find_nearest_fleet_carrier",
            "find_raw_material_trader",
            "find_tritium_mining_site",
            "find_vista_genomics",
            "ignore_hunting_ground",
            "increase_speed",
            "interrupt",
            "jump_to_hyperspace",
            "launch_ship_detach_from_station",
            "monetize_route",
            "navigate_from_memory",
            "navigate_to_bio_sample_codex_entry",
            "navigate_to_coordinates",
            "navigate_to_fleet_carrier",
            "navigate_to_home_system",
            "navigate_to_landing_zone",
            "navigate_to_mission_provider",
            "navigate_to_mission_target",
            "navigate_to_next_trade_stop",
            "navigate_to_pirate_mission_provider",
            "navigate_to_squadron_carrier",
            "open_fss_scan_system",
            "plot_route_next_neutron_star_waypoint",
            "query_bio_scans_and_samples_in_star_system",
            "query_biome_analysis",
            "query_cargo_hold_contents",
            "query_carriers",
            "query_current_location",
            "query_distance_to_bio_sample",
            "query_distance_to_body",
            "query_distance_to_bubble_earth_sol_civilization",
            "query_distance_to_carrier",
            "query_exobiology_samples",
            "query_exploration_profits",
            "query_fleet_carrier_eta",
            "query_fleet_carrier_final_destination",
            "query_fleet_carrier_route",
            "query_fleet_carrier_status_fuel_credit_finance",
            "query_fsd_target",
            "query_geo_signals",
            "query_last_scan",
            "query_local_outfitting",
            "query_local_shipyard",
            "query_markets",
            "query_material_inventory",
            "query_missions_and_rewards",
            "query_pirate_mission",
            "query_planet_materials",
            "query_player_profile_rank_progress",
            "query_reminder",
            "query_ship_loadout",
            "query_ship_route_remaining_jumps",
            "query_signals_in_star_system",
            "query_squadron_carrier_eta",
            "query_squadron_carrier_final_destination",
            "query_squadron_carrier_route",
            "query_squadron_carrier_status_fuel_credit_finance",
            "query_station_details",
            "query_stations",
            "query_stellar_objects",
            "query_system_security",
            "query_time",
            "query_total_bounties",
            "query_trade_profile",
            "query_trade_route",
            "recon_hunting_ground",
            "recover_srv_vehicle_get_on_board_ship",
            "lauch_deploy_nomad",
            "remove_mining_target",
            "request_docking",
            "reset_head_look_ahead",
            "retract_hardpoints",
            "retract_landing_gear",
            "return_to_surface",
            "select_fire_group_by_nato",
            "set_carrier_fuel_reserve",
            "set_home_system",
            "set_optimal_speed",
            "set_reminder",
            "set_speed_100",
            "set_speed_25",
            "set_speed_50",
            "set_speed_75",
            "set_speed_to_zero_0_stop_ship",
            "set_timed_reminder",
            "show_chat_comms_panel",
            "show_commander_panel",
            "show_contacts_panel",
            "show_crew_panel",
            "show_email_inbox_panel",
            "show_fighter_panel",
            "show_fire_groups_panel",
            "show_history_panel",
            "show_internal_panel",
            "show_inventory_panel",
            "show_modules_panel",
            "show_navigation_panel",
            "show_social_panel",
            "show_squadron_panel",
            "show_station_services_panel",
            "show_status_panel",
            "show_storage_panel",
            "show_transactions_panel",
            "sleep",
            "switch_to_analysis_mode",
            "switch_to_combat_mode",
            "target_destination",
            "target_hostile_highest_threat",
            "target_subsystem",
            "target_wingman_1",
            "target_wingman_2",
            "target_wingman_3",
            "taxi_to_landing_pad",
            "toggle_all_announcements",
            "toggle_cargo_scoop",
            "toggle_discovery_announcements",
            "toggle_lights_on_off",
            "toggle_mining_announcements",
            "toggle_night_vision_on_off",
            "toggle_radar_announcements",
            "toggle_radio",
            "toggle_route_announcements",
            "trade_profile_set_budget",
            "trade_profile_set_max_distance",
            "trade_profile_set_max_stops",
            "trade_profile_toggle_permit_systems",
            "trade_profile_toggle_planetary_ports",
            "trade_profile_toggle_prohibited_cargo",
            "trade_profile_toggle_strongholds",
            "transfer_power_to_engines",
            "transfer_power_to_shields",
            "transfer_power_to_weapons",
            "wakeup",
            "wing_nav_lock"
    );

    @Test
    void snapshotBuiltinComposition() {
        Map<String, String> m = new AiActionMapGenerator().generate(
                Status.getInstance(), true,
                SystemSession.getInstance().conversationalModeOn());

        // Floating additions vary by session mode / are machine-only - excluded from the built-in snapshot.
        Set<String> floating = new HashSet<>(Arrays.asList(
                GeneralConversationQueryCommand.ID,
                IgnoreNonsensicalInputCommand.ID,
                ConnectionCheckQueryCommand.ID));
        // Custom-command ids come from the local custom_commands.json - excluded for portability.
        Set<String> custom =
                CustomCommandRegistry.getInstance().getCustomCommands().stream()
                        .map(CustomCommandDefinition::getActionKey)
                        .collect(Collectors.toSet());

        Set<String> actualBuiltin = m.values().stream()
                .filter(v -> !floating.contains(v) && !custom.contains(v))
                .collect(Collectors.toSet());

        Set<String> expected = new HashSet<>(SNAPSHOT_BUILTIN_IDS);
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(actualBuiltin);
        Set<String> unexpected = new TreeSet<>(actualBuiltin);
        unexpected.removeAll(expected);
        assertTrue(missing.isEmpty() && unexpected.isEmpty(),
                "Built-in composition drift. Missing (snapshot, not generated): "
                        + missing + " ; Unexpected (generated, not in snapshot): " + unexpected);
    }

    /**
     * Verifies the 'before' ordering hints on the carrier-cluster annotations produce the
     * expected relative order in the generated map (mirrors the legacy "declared early" wins).
     * Checks RELATIVE positions only, not absolute indices nor the whole map. Sources must be
     * present; targets are guarded (a target may be filtered out of the EN composition).
     */
    @Test
    void carrierClusterOrderInvariant() {
        Map<String, String> actual = new AiActionMapGenerator()
                .generate(Status.getInstance(), true,
                        SystemSession.getInstance().conversationalModeOn());

        List<String> order = new ArrayList<>(actual.values());

        requireBefore(order, "find_nearest_fleet_carrier", "navigate_to_fleet_carrier");

        String fleet = "query_fleet_carrier_status_fuel_credit_finance";
        requireBefore(order, fleet, "query_fleet_carrier_route");
        requireBefore(order, fleet, "query_fleet_carrier_final_destination");
        requireBefore(order, fleet, "query_fleet_carrier_eta");
        requireBefore(order, fleet, "query_distance_to_carrier");
        requireBefore(order, fleet, "calculate_fleet_carrier_route");
        requireBefore(order, fleet, "enter_fleet_carrier_destination");
        requireBefore(order, fleet, "set_carrier_fuel_reserve");
        requireBefore(order, fleet, "query_squadron_carrier_status_fuel_credit_finance");

        String squad = "query_squadron_carrier_status_fuel_credit_finance";
        requireBefore(order, squad, "query_squadron_carrier_route");
        requireBefore(order, squad, "query_squadron_carrier_final_destination");
        requireBefore(order, squad, "query_squadron_carrier_eta");
    }

    /** Source must exist; target is guarded (skip if filtered out of the composition). */
    private static void requireBefore(List<String> order, String src, String dst) {
        int s = order.indexOf(src);
        assertTrue(s >= 0, "source id missing from generated map: " + src);
        int d = order.indexOf(dst);
        if (d < 0) return; // target absent (filtered by composition) - edge not checked
        assertTrue(s < d, "expected '" + src + "' before '" + dst + "' but got positions " + s + " >= " + d);
    }
}
