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
 * Theme 2 (English): does the companion understand and execute commander QUERIES? For each phrase it scores,
 * from the recorded tool-calls, whether the expected query tool was called. Most built-in queries read the
 * current ship/galaxy state and take no formal parameters (the spoken target is resolved later by the
 * query's own analysis stage), so a "query with parameters" is mostly N/A here - the {@code targeted} flag
 * marks the cases that name a specific target, where we still expect the right query to be chosen.
 * Opt-in via the local-integration tag; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryExecutionEvalTest {

    /**
     * input -> expected query tool; {@code targeted} marks a query that names a specific target;
     * {@code argContains} non-null marks a parameterized query whose extracted argument must carry the value.
     */
    private record Case(String input, String expectedTool, boolean targeted, String argContains) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-queries-eval-trace.txt");

    private final List<Case> cases = List.of(
            new Case("what is our current location", "query_current_location", false, null),
            new Case("what's in our cargo hold", "query_cargo_hold_contents", false, null),
            new Case("what are the market prices here", "query_markets", false, null),
            new Case("what's our ship loadout", "query_ship_loadout", false, null),
            new Case("how many jumps are left on the route", "query_ship_route_remaining_jumps", false, null),
            new Case("what's the security level of this system", "query_system_security", false, null),
            new Case("what are our active missions", "query_missions_and_rewards", false, null),
            new Case("how far are we from the bubble", "query_distance_to_bubble_earth_sol_civilization", true, null),
            // Parameterized queries: the query layer must extract the named target into its key param.
            new Case("check our material inventory for iron", "query_material_inventory", true, "iron"),
            new Case("analyze the biome", "query_biome_analysis", false, null));

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void executesQueries() throws Exception {
        List<String> report = new ArrayList<>();
        report.add(String.format("%-46s | %-46s | %-5s | %-4s | %-8s | %s", "input", "expected tool", "call", "arg", "targeted", "actually called"));
        report.add("-".repeat(140));

        int hits = 0;
        for (Case c : cases) {
            h.say(c.input());
            List<Executed> calls = h.callsNamed(c.expectedTool());
            boolean called = !calls.isEmpty();
            String args = called ? calls.get(0).args().toString() : "";
            boolean argOk = c.argContains() == null || (called && args.contains(c.argContains()));
            if (called && argOk) {
                hits++;
            }
            report.add(String.format("%-46s | %-46s | %-5s | %-4s | %-8s | %s",
                    c.input(), c.expectedTool(), called ? "yes" : "NO",
                    c.argContains() == null ? "-" : (argOk ? "ok" : "MISS"),
                    c.targeted() ? "yes" : "-",
                    h.turnCalls().stream().map(Executed::tool).toList() + (called && !args.isEmpty() ? " args=" + args : "")));
            report.add(h.memoryDeltaBlock()); // what this query wrote to memory, this turn
        }

        StringBuilder block = new StringBuilder("\n======== QUERY EXECUTION (theme 2) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("score: %d / %d%n", hits, cases.size()));
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
