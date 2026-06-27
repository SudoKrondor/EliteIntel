package elite.intel.companion.input.en;

import elite.intel.companion.input.CompanionEvalHarness;
import elite.intel.companion.input.CompanionEvalHarness.Executed;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Theme 1 (English): does the companion understand and execute commander COMMANDS, including parameterized
 * ones, and does the reflex fast-path fire for verbatim, safe, parameterless phrases WITHOUT reaching the LLM?
 * For each phrase it scores, from the recorded tool-calls, whether the expected command tool was called and
 * (for parameterized commands) whether the extracted argument carries the requested value; for reflex cases it
 * additionally asserts the turn consumed zero LLM rounds (the {@code ReflexResolver} short-circuit) and for
 * LLM cases at least one. Each turn also dumps what it wrote to memory. Game commands are recorded, never
 * executed. Opt-in via the local-integration tag; LM Studio must be up.
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

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-commands-eval-trace.txt");

    private final List<Case> cases = List.of(
            // LLM path: paraphrases / digit-vs-word / parameters need the model to classify or extract.
            new Case("set speed to fifty percent", "set_speed_50", null, false),
            new Case("toggle the cargo scoop", "toggle_cargo_scoop", null, false),
            new Case("deploy the landing gear", "deploy_landing_gear", null, false),
            new Case("retract the hardpoints", "retract_hardpoints", null, false),
            new Case("target wingman two", "target_wingman_2", null, false),
            new Case("set the trade budget to ten million credits", "trade_profile_set_budget", "10", false),
            new Case("set the maximum number of trade stops to three", "trade_profile_set_max_stops", "3", false),
            // Subsystem targeting: parameterized, no synonym canonicalization, so it goes through the LLM, which
            // must execute target_subsystem with the verbatim subsystem rather than chatter (regression for the
            // companion action-bias prompt rules - observed answering with status/FSD-target offers instead).
            new Case("target drive", "target_subsystem", "drive", false),
            new Case("target power plant", "target_subsystem", "power", false),
            // "Find" commands: parameterized search (key + optional max_distance/state). Same regression class as
            // target_subsystem - the companion previously lost the param examples/hints and chattered instead of
            // executing. Verifies the command fires and carries the searched value.
            new Case("find where we can buy gold within 80 light years", "find_commodity", "gold", false),
            new Case("find a mining site for painite", "find_mining_site", "painite", false),
            new Case("find brain trees", "find_brain_trees", null, false),
            new Case("find nearest interstellar factor", "find_interstellar_factor", null, true),
            // Bare panel names: no synonym canonicalization, so they go through the LLM, which must execute the
            // matching command rather than chatter (regression for the companion action-bias prompt rules).
            new Case("navigation", "show_navigation_panel", null, false),
            new Case("contacts", "show_contacts_panel", null, false),
            new Case("inventory", "show_inventory_panel", null, false),
            // HUD-mode words: the input normalizer canonicalizes them to "switch to <x> mode", so they match a
            // training phrase verbatim and take the reflex fast-path (no LLM) - the legacy synonym map reused.
            new Case("combat mode", "switch_to_combat_mode", null, true),
            new Case("analysis mode", "switch_to_analysis_mode", null, true),
            // Reflex fast-path: a training phrase matched verbatim to one safe, parameterless command - no LLM.
            new Case("all stop", "set_speed_to_zero_0_stop_ship", null, true),
            new Case("cargo scoop", "toggle_cargo_scoop", null, true),
            new Case("gear down", "deploy_landing_gear", null, true),
            new Case("weapons cold", "retract_hardpoints", null, true));

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void executesCommandsIncludingParameterizedAndReflex() throws Exception {
        StringBuilder block = new StringBuilder("\n======== COMMAND EXECUTION (theme 1) ========\n");

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
            block.append(String.format("%n%-46s | call=%-3s arg=%-4s path=%-12s | %s%n",
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
