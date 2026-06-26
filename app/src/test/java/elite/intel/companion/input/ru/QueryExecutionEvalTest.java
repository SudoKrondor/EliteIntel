package elite.intel.companion.input.ru;

import elite.intel.companion.input.CompanionEvalHarness;
import elite.intel.companion.input.CompanionEvalHarness.Executed;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Theme 2 (Russian): verifies Russian commander QUERIES with the prompt/session language pinned to RU.
 * Expected query tool ids stay language-neutral.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryExecutionEvalTest {

    private record Case(String input, String expectedTool, boolean targeted) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-queries-eval-trace.txt", Language.RU);

    private final List<Case> cases = List.of(
            new Case("где мы сейчас находимся", "query_current_location", false),
            new Case("что у нас в трюме", "query_cargo_hold_contents", false),
            new Case("какие здесь рыночные цены", "query_markets", false),
            new Case("какая у нас комплектация корабля", "query_ship_loadout", false),
            new Case("сколько прыжков осталось по маршруту", "query_ship_route_remaining_jumps", false),
            new Case("какой уровень безопасности в этой системе", "query_system_security", false),
            new Case("какие у нас активные миссии", "query_missions_and_rewards", false),
            new Case("как далеко мы от пузыря цивилизации у Земли и Сола",
                    "query_distance_to_bubble_earth_sol_civilization", true));

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void executesRussianQueries() throws Exception {
        List<String> report = new ArrayList<>();
        report.add(String.format("%-58s | %-46s | %-5s | %-8s | %s",
                "input", "expected tool", "call", "targeted", "actually called"));
        report.add("-".repeat(145));

        int hits = 0;
        for (Case c : cases) {
            h.say(c.input());
            boolean called = h.called(c.expectedTool());
            if (called) {
                hits++;
            }
            report.add(String.format("%-58s | %-46s | %-5s | %-8s | %s",
                    c.input(), c.expectedTool(), called ? "yes" : "NO", c.targeted() ? "yes" : "-",
                    h.turnCalls().stream().map(Executed::tool).toList()));
        }

        StringBuilder block = new StringBuilder("\n======== RU QUERY EXECUTION (theme 2) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("score: %d / %d%n", hits, cases.size()));
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
