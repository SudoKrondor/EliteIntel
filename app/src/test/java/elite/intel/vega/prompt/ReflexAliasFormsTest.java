package elite.intel.vega.prompt;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data guard for the companion reflex fast-path ({@link ReflexResolver}). The short ship/panel/map commands the
 * commander blurts must each carry a bare spoken form ("landing gear", "supercruise", "optimal speed", "galaxy
 * map", ...) that belongs to exactly one command, so the reflex gate executes them deterministically instead of
 * dropping to the probabilistic LLM round. Reverting a value to the old "show, open or display X" phrasing (which
 * drops the bare form) or duplicating a phrase across commands breaks reflex eligibility - this test catches that.
 * It checks the alias data only; the resolver's matching rules are covered by {@link ReflexResolverTest}.
 */
class ReflexAliasFormsTest {

    /**
     * Bare spoken form -> the single command it must resolve to (verbatim, so the reflex gate fires).
     */
    private static final Map<String, String> EXPECTED_UNIQUE_BARE_FORM = Map.ofEntries(
            // Panels / maps
            Map.entry("galaxy map", "display_open_galaxy_map"),
            Map.entry("system map", "display_open_system_map"),
            Map.entry("radar", "display_radar_panel"),
            Map.entry("status panel", "show_status_panel"),
            Map.entry("contacts panel", "show_contacts_panel"),
            Map.entry("navigation panel", "show_navigation_panel"),
            // Flight / speed / docking
            Map.entry("jump", "jump_to_hyperspace"),
            Map.entry("supercruise", "enter_super_cruise"),
            Map.entry("taxi", "taxi_to_landing_pad"),
            Map.entry("full stop", "set_speed_to_zero_0_stop_ship"),
            Map.entry("full throttle", "set_speed_100"),
            Map.entry("half throttle", "set_speed_50"),
            Map.entry("quarter throttle", "set_speed_25"),
            Map.entry("optimal speed", "set_optimal_speed"),
            Map.entry("landing gear", "deploy_landing_gear"),
            Map.entry("gear down", "deploy_landing_gear"),
            Map.entry("gear up", "retract_landing_gear"),
            // Combat / consumables / modes
            Map.entry("chaff", "deploy_chaff"),
            Map.entry("heat sink", "deploy_heat_sink"),
            Map.entry("shield cell", "deploy_shield_cell"),
            Map.entry("priority target", "target_hostile_highest_threat"),
            Map.entry("combat mode", "switch_to_combat_mode"),
            Map.entry("analysis mode", "switch_to_analysis_mode"),
            // Systems / SRV / scan
            Map.entry("cargo scoop", "toggle_cargo_scoop"),
            Map.entry("night vision", "toggle_night_vision_on_off"),
            Map.entry("drive assist", "drive_assist"),
            Map.entry("disembark", "disembark"),
            Map.entry("honk", "run_discovery_scan"));

    @Test
    void shortShipCommandsCarryAUniqueBareReflexForm() throws Exception {
        Map<String, List<String>> phrasesById = loadPhrasesById();
        for (Map.Entry<String, String> expected : EXPECTED_UNIQUE_BARE_FORM.entrySet()) {
            String bare = expected.getKey();
            String owner = expected.getValue();

            List<String> ownerPhrases = phrasesById.get(owner);
            assertNotNull(ownerPhrases, owner + " is missing from the English alias bundle");
            assertTrue(ownerPhrases.contains(bare),
                    owner + " must list the bare form \"" + bare + "\" so it can reflex; found: " + ownerPhrases);

            List<String> alsoContain = new ArrayList<>();
            for (Map.Entry<String, List<String>> command : phrasesById.entrySet()) {
                if (!command.getKey().equals(owner) && command.getValue().contains(bare)) {
                    alsoContain.add(command.getKey());
                }
            }
            assertTrue(alsoContain.isEmpty(),
                    "bare form \"" + bare + "\" must be unique for the reflex gate, but also appears in: " + alsoContain);
        }
    }

    /**
     * Command id -> its comma-split alias phrases (trimmed, lower-cased) from the English alias bundle.
     */
    private static Map<String, List<String>> loadPhrasesById() throws Exception {
        Properties props = new Properties();
        try (InputStream in = ReflexAliasFormsTest.class.getResourceAsStream("/i18n/ai_action_aliases.properties")) {
            assertNotNull(in, "English alias bundle must be on the test classpath");
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        Map<String, List<String>> byId = new TreeMap<>();
        for (String id : props.stringPropertyNames()) {
            List<String> phrases = new ArrayList<>();
            for (String phrase : props.getProperty(id).split(",")) {
                String trimmed = phrase.trim().toLowerCase(Locale.ROOT);
                if (!trimmed.isEmpty()) {
                    phrases.add(trimmed);
                }
            }
            byId.put(id, phrases);
        }
        return byId;
    }
}
