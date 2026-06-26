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
 * Theme 1 (Russian): verifies that the companion understands Russian commander COMMANDS while the
 * session language is pinned to RU before prompt construction. Game commands are recorded, never executed.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandExecutionEvalTest {

    private record Case(String input, String expectedTool, String argContains) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-commands-eval-trace.txt", Language.RU);

    private final List<Case> cases = List.of(
            new Case("установи скорость пятьдесят процентов", "set_speed_50", null),
            new Case("полная остановка", "set_speed_to_zero_0_stop_ship", null),
            new Case("переключи грузовой ковш", "toggle_cargo_scoop", null),
            new Case("выпусти шасси", "deploy_landing_gear", null),
            new Case("убери оружие", "retract_hardpoints", null),
            new Case("выбери второго ведомого", "target_wingman_2", null),
            new Case("установи торговый бюджет десять миллионов кредитов", "trade_profile_set_budget", "10"),
            new Case("установи максимальное число торговых остановок три", "trade_profile_set_max_stops", "3"));

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void executesRussianCommandsIncludingParameterized() throws Exception {
        List<String> report = new ArrayList<>();
        report.add(String.format("%-56s | %-30s | %-5s | %-5s | %s",
                "input", "expected tool", "call", "arg", "actually called"));
        report.add("-".repeat(130));

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
            report.add(String.format("%-56s | %-30s | %-5s | %-5s | %s",
                    c.input(), c.expectedTool(),
                    called ? "yes" : "NO",
                    c.argContains() == null ? "-" : (argOk ? "ok" : "MISS"),
                    h.turnCalls().stream().map(Executed::tool).toList()
                            + (called && !args.isEmpty() ? " args=" + args : "")));
            report.add(h.memoryDeltaBlock()); // what this command wrote to memory, this turn
        }

        StringBuilder block = new StringBuilder("\n======== RU COMMAND EXECUTION (theme 1) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("score: %d / %d%n", hits, cases.size()));
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
