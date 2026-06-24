package elite.intel.companion.input.en;

import elite.intel.companion.input.CompanionEvalHarness;
import elite.intel.companion.input.CompanionEvalHarness.Executed;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Theme 1 (English): does the companion understand and execute commander COMMANDS, including parameterized
 * ones? For each phrase it scores, from the recorded tool-calls, whether the expected command tool was
 * called and (for parameterized commands) whether the extracted argument carries the requested value.
 * Game commands are recorded, never executed. Opt-in via the local-integration tag; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandExecutionEvalTest {

    /** input -> expected command tool; {@code argContains} non-null marks a parameterized command. */
    private record Case(String input, String expectedTool, String argContains) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-commands-eval-trace.txt");

    private final List<Case> cases = List.of(
            new Case("set speed to fifty percent", "set_speed_50", null),
            new Case("all stop", "set_speed_to_zero_0_stop_ship", null),
            new Case("toggle the cargo scoop", "toggle_cargo_scoop", null),
            new Case("deploy the landing gear", "deploy_landing_gear", null),
            new Case("retract the hardpoints", "retract_hardpoints", null),
            new Case("target wingman two", "target_wingman_2", null),
            new Case("set the trade budget to ten million credits", "trade_profile_set_budget", "10"),
            new Case("set the maximum number of trade stops to three", "trade_profile_set_max_stops", "3"));

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void executesCommandsIncludingParameterized() throws Exception {
        List<String> report = new ArrayList<>();
        report.add(String.format("%-46s | %-30s | %-5s | %-5s | %s", "input", "expected tool", "call", "arg", "actually called"));
        report.add("-".repeat(120));

        int hits = 0;
        for (Case c : cases) {
            h.say(c.input());
            List<Executed> calls = h.callsNamed(c.expectedTool());
            boolean called = !calls.isEmpty();
            String args = called ? calls.get(0).args().toString() : "";
            boolean argOk = c.argContains() == null || (called && args.contains(c.argContains()));
            boolean pass = called && argOk;
            if (pass) {
                hits++;
            }
            report.add(String.format("%-46s | %-30s | %-5s | %-5s | %s",
                    c.input(), c.expectedTool(),
                    called ? "yes" : "NO",
                    c.argContains() == null ? "-" : (argOk ? "ok" : "MISS"),
                    actualTools(c) + (called && !args.isEmpty() ? " args=" + args : "")));
        }

        StringBuilder block = new StringBuilder("\n======== COMMAND EXECUTION (theme 1) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("score: %d / %d%n", hits, cases.size()));
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }

    /** The tool names the model actually called this turn (for diagnosing a miss). */
    private List<String> actualTools(Case c) {
        return h.turnCalls().stream().map(Executed::tool).toList();
    }
}
