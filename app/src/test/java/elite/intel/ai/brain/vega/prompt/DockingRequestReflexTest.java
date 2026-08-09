package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.i18n.PhoneticInputNormalizer;
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
 * Guards "request landing permission", which docked on one run and was refused on the next.
 *
 * <p>The alias group owned "request landing" but not the permission form, so {@link ReflexResolver} matched
 * nothing and both turns fell through to the local model. The reducer was never in doubt - it kept
 * {@code request_docking} alone, at 0.965 for the clean transcript and 0.908 for the misheard one - but the
 * model still answered the misheard turn with speak "Unavailable. Try 'request docking'.", handing the
 * commander a script instead of contacting traffic control. A phrase the reducer resolves that confidently has
 * no business being decided by a model at all.
 *
 * <p>The mishearing is the second half of it: Parakeet returns "lending" for "landing" often enough that the
 * pair sits beside the existing {@code lensing -> landing} correction, so the acoustic form reaches the same
 * reflex rather than a coin flip.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DockingRequestReflexTest {

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
     * The reported utterance, in the situation it was reported from (orbital flight near a planet).
     */
    @Test
    void askingForLandingPermissionRequestsDockingWithoutTheModel() {
        Optional<ReflexResolver.Reflex> reflex = resolve("request landing permission");
        assertTrue(reflex.isPresent(), "\"request landing permission\" must never depend on the local model");
        assertEquals("request_docking", reflex.get().actionId());
    }

    /**
     * What the STT engine actually delivered on the failing run. The normalizer has to land it on the authored
     * alias, because that hand-off is the whole reason the turn reached the model in the first place.
     */
    @Test
    void theMisheardLendingFormNormalizesIntoTheSameReflex() {
        String normalized = PhoneticInputNormalizer.normalize("request lending permission");
        assertEquals("request landing permission", normalized);
        assertEquals("request_docking", resolve(normalized).orElseThrow().actionId());
    }

    /**
     * The phrasings either side of the reported one, so the fix is not one lucky string.
     */
    @Test
    void theOtherPermissionPhrasingsResolveToo() {
        assertEquals("request_docking", resolve("request permission to land").orElseThrow().actionId());
        assertEquals("request_docking", resolve("request docking permission").orElseThrow().actionId());
        assertEquals("request_docking", resolve("request docking").orElseThrow().actionId());
    }

    /**
     * "landing" is shared with the gear commands, which are the near neighbours the new aliases could have
     * swallowed. They keep their own phrases.
     */
    @Test
    void theLandingGearCommandsKeepTheirOwnPhrases() {
        assertEquals("deploy_landing_gear", resolve("lower landing gear").orElseThrow().actionId());
        assertEquals("retract_landing_gear", resolve("retract landing gear").orElseThrow().actionId());
    }

    private Optional<ReflexResolver.Reflex> resolve(String utterance) {
        return resolver.resolve(utterance, GameStateSnapshot.capture(
                Status.detached(PlayerSituation.IN_SHIP_ORBIT)));
    }
}
