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
 * Hard RU routing probe (not a calibrated pass/fail gate): deliberately terse, grammatically ambiguous, or
 * colloquial Russian phrasings of commands whose matching tool IS surfaced to the model, but which a small
 * model on Russian frequently mis-routes (reads a terse noun-phrase as a question, picks a near-miss tool, or
 * just chatters). Scores only whether the EXPECTED command tool was called - parameter extraction is not
 * gated, so the routing signal is not muddied by English-vs-Russian argument forms. Used to measure the
 * effect of the "reason in English to choose the function" language rule (before/after). Opt-in via the
 * local-integration tag; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoutingHardProbeEvalTest {

    private record Case(String input, String expectedTool) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-routing-probe-trace.txt", Language.RU);

    private final List<Case> cases = List.of(
            // Terse subsystem noun-phrases: "цель" is a Russian noun, not an imperative verb, so the model
            // often reads these as a question ("что с двигателями?") instead of targeting the subsystem.
            new Case("цель двигатели", "target_subsystem"),
            new Case("цель распределитель", "target_subsystem"),
            new Case("цель жизнеобеспечение", "target_subsystem"),
            new Case("цель силовая установка", "target_subsystem"),
            // Bare / paraphrased panel & wingman requests that misrouted (clarify / speak / fire-group).
            new Case("навигация", "show_navigation_panel"),
            new Case("управление авианосцем", "display_fleet_carrier_management_panel"),
            new Case("выбери второго ведомого", "target_wingman_2"),
            // Colloquial imperatives that are not verbatim aliases but share a meaningful word with one.
            new Case("глуши двигатели", "set_speed_to_zero_0_stop_ship"),
            new Case("вырубай радио", "toggle_radio"),
            new Case("вырубай все объявления", "toggle_all_announcements"),
            new Case("где купить платину", "find_commodity"),
            new Case("проложи маршрут к миссии срочно", "navigate_to_mission_target"));

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void probesRussianRouting() throws Exception {
        StringBuilder block = new StringBuilder("\n======== RU ROUTING HARD PROBE ========\n");

        int hits = 0;
        for (Case c : cases) {
            long roundsBefore = h.roundCount();
            h.say(c.input());
            long llmRounds = h.roundCount() - roundsBefore;

            boolean called = h.called(c.expectedTool());
            if (called) {
                hits++;
            }
            block.append(String.format("%n%-40s | want=%-44s | call=%-3s path=llm(%d) | %s%n",
                    c.input(), c.expectedTool(), called ? "yes" : "NO", llmRounds,
                    h.turnCalls().stream().map(Executed::tool).toList()));
            block.append(h.memoryDeltaBlock());
        }

        block.append(String.format("%nrouting score: %d / %d%n", hits, cases.size()));
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
