package elite.intel.ai.brain.vega.input.ru;

import elite.intel.ai.brain.vega.input.CompanionEvalHarness;
import elite.intel.ai.brain.vega.memory.facts.MemoryFactContext;
import elite.intel.ai.brain.vega.memory.facts.MemoryFactSourceRegistry;
import elite.intel.ai.brain.vega.memory.facts.MergedFactCandidates;
import elite.intel.ai.brain.vega.memory.facts.sources.CurrentSystemFactSource;
import elite.intel.ai.brain.vega.prompt.Fact;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Theme (Russian): the current-system fact source. Seeds a known current system (Sol, Federation, high security) into
 * the live game state, then asks the companion about it. Recorder-style: hard-asserts only that the live model was
 * reached and the source produced a fact directly; traces whether relevance selection keeps the system fact while
 * dropping the unrelated commander profile, and whether the model answers from it or via a {@code query_*}. Opt-in;
 * LM Studio must be up, and (per localIntegrationTest) the app DB is the real one, so the seed is best-effort.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocationFactsEvalTest {

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-location-facts-trace.txt", Language.RU);

    @BeforeAll
    void boot() throws Exception {
        h.boot();
        seedSol();
        seedSituation(PlayerSituation.IN_SHIP_SUPERCRUISE);
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void tracesTheCurrentSystemFactAndTheModelsUseOfIt() throws Exception {
        StringBuilder block = new StringBuilder("\n======== RU LOCATION FACTS (current system) ========\n");

        int registered = MemoryFactSourceRegistry.getInstance().sources().size();
        List<String> sourceFacts = new CurrentSystemFactSource().factsFor(MemoryFactContext.forCommanderInput(""));
        // The fact reaches the prompt via the same registered-source path used by PromptComposer.
        List<Fact> blockFacts = MergedFactCandidates.forInput(MemoryFactContext.forCommanderInput("в какой мы системе"));
        boolean inBlock = blockFacts.stream().anyMatch(f -> f.text().toLowerCase(Locale.ROOT).contains("current system sol"));
        boolean profileInBlock = blockFacts.stream().anyMatch(f -> "commander".equals(f.source()));

        h.beginTurn();
        h.say("в какой мы сейчас системе и насколько тут безопасно?");

        List<String> injected = h.injectedFacts();
        List<String> tools = h.turnToolNames();
        boolean queried = tools.stream().anyMatch(t -> t.startsWith("query_"));

        block.append(String.format("registered sources=%d%n source output=%s%n reaches <facts> block=%s (%s)%n"
                        + "unrelated commander profile in block=%s%n"
                        + "injected <facts>(last body)=%s%n tools=%s | queried=%s%n -> %s%n",
                registered, sourceFacts, inBlock, blockFacts, profileInBlock,
                injected, tools, queried, h.spokenTexts()));
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
        assertTrue(inBlock, "the current-system fact did not reach the <facts> candidate block: " + blockFacts);
        assertFalse(profileInBlock, "an unrelated commander profile reached the system question: " + blockFacts);
    }

    private void seedSol() {
        PlayerSession.getInstance().setCurrentPrimaryStarName("Sol");
        LocationDto sol = new LocationDto(1L);
        sol.setStarName("Sol");
        sol.setPlanetName("Sol");
        sol.setLocationType(LocationDto.LocationType.PRIMARY_STAR);
        sol.setSystemAddress(9999L);
        sol.setAllegiance("Federation");
        sol.setSecurity("High Security");
        sol.setEconomy("Refinery");
        sol.setPopulation(22_700_000_000L);
        sol.setControllingPower("Zachary Hudson");
        LocationManager.getInstance().save(sol);
    }

    private void seedSituation(PlayerSituation situation) {
        long[] flags = StatusFlags.flagsForSituation(situation);
        GameEvents.StatusEvent event = new GameEvents.StatusEvent();
        event.setFlags(flags[0]);
        event.setFlags2(flags[1]);
        Status.getInstance().setStatus(event);
    }
}
