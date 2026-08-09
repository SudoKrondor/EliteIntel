package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.vega.model.GameStateSnapshot;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fuzzy second pass against the real alias catalog: what it must catch, and what it must refuse to touch.
 *
 * <p>The transcript is the only text in this pipeline nobody authored, so it is the only one worth repairing.
 * Everything the repair could reach - the aliases, the vocabulary, the visibility rules - is ours, which is
 * what makes the guards checkable rather than hopeful.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FuzzyReflexTest {

    private ReflexResolver resolver;

    @BeforeAll
    void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        SystemSession.getInstance().setLanguage(Language.EN);
        resolver = new ReflexResolver();
    }

    /**
     * The reported shape of the problem: one word is unrecoverable on its own ("near" is as close to hear,
     * year and wear as it is to gear), and the rest of the phrase decides it.
     */
    @Test
    void aGarbledPhraseResolvesWhenTheRestOfItAgrees() {
        Optional<ReflexResolver.Reflex> reflex = resolve("deploy blanding near");
        assertTrue(reflex.isPresent(), "the surrounding words identify this phrase without a model");
        assertEquals("deploy_landing_gear", reflex.get().actionId());
        assertEquals(ReflexResolver.MatchKind.FUZZY, reflex.get().matchKind());
    }

    @Test
    void aCleanPhraseStillResolvesAsExact() {
        Optional<ReflexResolver.Reflex> reflex = resolve("deploy landing gear");
        assertEquals("deploy_landing_gear", reflex.orElseThrow().actionId());
        assertEquals(ReflexResolver.MatchKind.EXACT, reflex.get().matchKind(),
                "an authored alias must never be reported as a repair");
    }

    /**
     * "dump heat" is an authored alias of the heat sink, and "dump" is one edit from "jump". If the repair
     * worked word by word against the vocabulary rather than phrase by phrase, this is where it would fire a
     * hyperspace jump instead.
     */
    @Test
    void anAuthoredWordIsNeverRepairedIntoAnotherCommand() {
        assertEquals("deploy_heat_sink", resolve("dump heat").orElseThrow().actionId());
    }

    /**
     * Free speech must stay free speech: the model handles conversation, and a reflex that fires on it would
     * be both wrong and silent.
     */
    @Test
    void ordinarySpeechDoesNotFireAnything() {
        assertTrue(resolve("what do you think about that").isEmpty());
        assertTrue(resolve("how are we doing on time").isEmpty());
    }

    /**
     * Repairs are bounded by the same visibility rule as every other reflex - the gear is unavailable in
     * supercruise, so the phrase falls through to the normal path instead of firing a refused binding.
     */
    @Test
    void aRepairRespectsVisibility() {
        assertTrue(resolver.resolve("deploy blanding near", GameStateSnapshot.capture(
                Status.detached(PlayerSituation.IN_SHIP_SUPERCRUISE))).isEmpty());
    }

    /**
     * The reported docking phrase, garbled a different way than the run that started this - the authored alias
     * now covers the clean form, and the repair covers what the engine does to it.
     */
    @Test
    void theDockingPhraseSurvivesItsOwnMishearing() {
        assertEquals("request_docking", resolve("request blanding permission").orElseThrow().actionId());
    }

    private Optional<ReflexResolver.Reflex> resolve(String utterance) {
        return resolver.resolve(utterance, GameStateSnapshot.capture(
                Status.detached(PlayerSituation.IN_SHIP_ORBIT)));
    }
}
