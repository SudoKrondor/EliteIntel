package elite.intel.companion.input.ru;

import elite.intel.companion.input.CompanionEvalHarness;
import elite.intel.companion.input.CompanionEvalHarness.Executed;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Theme 1 (Russian): does the companion understand and execute commander COMMANDS, including parameterized
 * ones, while the session language is pinned to RU before prompt construction, and does the reflex fast-path
 * fire for verbatim, safe, parameterless phrases WITHOUT reaching the LLM? Mirrors the English theme-1 test:
 * for each phrase it scores, from the recorded tool-calls, whether the expected command tool was called and
 * (for parameterized commands) whether the extracted argument carries the requested value; for reflex cases it
 * additionally asserts the turn consumed zero LLM rounds (the {@code ReflexResolver} short-circuit) and for
 * LLM cases at least one. Game commands are recorded, never executed. Opt-in via the local-integration tag;
 * LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandExecutionEvalTest {

    /**
     * input -> expected command tool; {@code argContains} non-null marks a parameterized command;
     * {@code reflex} true marks a verbatim, safe, parameterless phrase that must take the reflex fast-path
     * (executed directly, no LLM round), false marks a phrase that must go through the LLM.
     */
    private record Case(String input, String expectedTool, String argContains, boolean reflex) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-commands-eval-trace.txt", Language.RU);

    private final List<Case> cases = List.of(
            // LLM path: paraphrases / digit-vs-word / parameters need the model to classify or extract.
            new Case("установи скорость пятьдесят процентов", "set_speed_50", null, false),
            new Case("переключи грузовой ковш", "toggle_cargo_scoop", null, false),
            new Case("установи торговый бюджет десять миллионов кредитов", "trade_profile_set_budget", "10", false),
            new Case("установи максимальное число торговых остановок три", "trade_profile_set_max_stops", "3", false),
            // Subsystem targeting: parameterized, so it goes through the LLM. The model normalizes the spoken
            // Russian subsystem to its English key (двигатели -> drive, силовая установка -> powerplant).
            new Case("наведись на двигатели", "target_subsystem", "drive", false),
            new Case("целься в двигатели", "target_subsystem", "drive", false),
            new Case("цель силовая установка", "target_subsystem", "powerplant", false),
            // "Find" commands: parameterized search. The model normalizes the commodity to its English key.
            new Case("найди где купить золото в радиусе 80 световых лет", "find_commodity", "gold", false),
            new Case("найди место добычи платины", "find_mining_site", "platinum", false),
            new Case("найди мозговые деревья", "find_brain_trees", null, false),
            new Case("найди охотничьи угодья в радиусе 50 световых лет", "find_hunting_grounds", "50", false),
            new Case("навигация к активной миссии", "navigate_to_mission_target", null, false),
            new Case("рассчитать нейтронный маршрут с эффективностью 60", "calculate_neutron_star_route", "60", false),
            // Toggle commands: state:boolean param. Must execute with the correct on/off state.
            new Case("выключи объявления добычи", "toggle_mining_announcements", "false", false),
            new Case("включи объявления маршрута", "toggle_route_announcements", "true", false),
            new Case("выключи все объявления", "toggle_all_announcements", "false", false),
            new Case("включи объявления открытий", "toggle_discovery_announcements", "true", false),
            new Case("выключи объявления радарных контактов", "toggle_radar_announcements", "false", false),
            new Case("выключи радио", "toggle_radio", "false", false),
            // HUD-mode words: the RU input normalizer canonicalizes them to the English "switch to <x> mode",
            // which is not a verbatim RU alias, so they take the LLM path and must execute the matching command.
            new Case("включи боевой режим", "switch_to_combat_mode", null, false),
            new Case("включи режим анализа", "switch_to_analysis_mode", null, false),
            // Bare panel names: no verbatim alias, so they go through the LLM, which must execute the matching
            // command rather than chatter.
            new Case("контакты", "show_contacts_panel", null, false),
            new Case("инвентарь", "show_inventory_panel", null, false),
            // Bare noun vs full imperative for the same command: the bare word goes through the LLM (may
            // chatter), the verbatim alias phrase takes the deterministic reflex fast-path.
            new Case("навигация", "show_navigation_panel", null, false),
            new Case("открой навигацию", "show_navigation_panel", null, true),
            // Reflex fast-path: a training phrase matched verbatim to one safe, parameterless command - no LLM.
            new Case("цель ведомый два", "target_wingman_2", null, true),
            new Case("открой управление авианосцем", "display_fleet_carrier_management_panel", null, true),
            new Case("полный стоп", "set_speed_to_zero_0_stop_ship", null, true),
            new Case("грузозаборник", "toggle_cargo_scoop", null, true),
            new Case("выпусти шасси", "deploy_landing_gear", null, true),
            new Case("убери оружие", "retract_hardpoints", null, true));

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void executesRussianCommandsIncludingParameterizedAndReflex() throws Exception {
        StringBuilder block = new StringBuilder("\n======== RU COMMAND EXECUTION (theme 1) ========\n");

        int hits = 0;
        for (Case c : cases) {
            long roundsBefore = h.roundCount();
            h.say(c.input());
            long llmRounds = h.roundCount() - roundsBefore; // 0 == reflex fast-path, >=1 == LLM path

            List<Executed> calls = h.callsNamed(c.expectedTool());
            boolean called = !calls.isEmpty();
            String args = called ? calls.get(0).args().toString() : "";
            boolean argOk = c.argContains() == null || (called && args.contains(c.argContains()));
            boolean tookReflex = llmRounds == 0;
            boolean pathOk = tookReflex == c.reflex();
            boolean pass = called && argOk && pathOk;
            if (pass) {
                hits++;
            }
            String pathCell = String.format("%s%s",
                    tookReflex ? "reflex" : "llm(" + llmRounds + ")",
                    pathOk ? "" : (c.reflex() ? " WANT-REFLEX" : " WANT-LLM"));
            block.append(String.format("%n%-56s | call=%-3s arg=%-4s path=%-12s | %s%n",
                    c.input(),
                    called ? "yes" : "NO",
                    c.argContains() == null ? "-" : (argOk ? "ok" : "MISS"),
                    pathCell,
                    actualTools() + (called && !args.isEmpty() ? " args=" + args : "")));
            block.append(h.memoryDeltaBlock()); // what this command wrote to memory, this turn
        }

        block.append(String.format("%nscore: %d / %d%n", hits, cases.size()));
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }

    /** The tool names the model actually called this turn (for diagnosing a miss). */
    private List<String> actualTools() {
        return h.turnCalls().stream().map(Executed::tool).toList();
    }
}
