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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the countermeasure shout: "chaff!", "flare!", "countermeasures!".
 *
 * <p>Reported as never firing. "chaff" itself always resolved here - it is a bare alias with a single owner -
 * but "flare" and "flares" only existed inside the longer "launch flares" / "deploy flares", and the reflex
 * matches whole phrases, so the one word a commander actually shouts under a missile lock owned nothing. The
 * bare colloquial forms belong to the command now, as they already did in ES/PT ("contramedidas").
 *
 * <p>This is the one command where the phrasing budget is worth spending: it is shouted mid-combat, once, and
 * a turn that reaches the local model has already cost the missile lock it was meant to break.
 *
 * <p>Not covered here, because it is downstream of routing: chaff is frequently bound to a HOTAS button
 * rather than a key, and the executable binding map is keyboard-only. That case is not silent - it announces
 * {@code speech.keyBindingNotFound} - but it looks identical to the commander.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChaffReflexTest {

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
     * The reported utterances, shouted the way they are shouted - the exclamation mark included, since the
     * reflex canonicalizes trailing punctuation away rather than failing on it.
     */
    @ParameterizedTest(name = "\"{0}\" launches chaff")
    @ValueSource(strings = {
            "chaff!", "chaff", "flare!", "flare", "flares", "countermeasures",
            "deploy chaff", "launch chaff", "fire chaff", "drop chaff", "chaff out", "chaff now",
            "launch flares", "deploy flares", "deploy countermeasures", "launch countermeasures",
    })
    void theCountermeasureShoutNeverDependsOnTheModel(String utterance) {
        assertEquals("deploy_chaff", resolve(utterance));
    }

    /**
     * The other countermeasure is a different command; sharing a phrase would condemn both to the model.
     */
    @Test
    void theHeatSinkKeepsItsOwnCommand() {
        assertEquals("deploy_heat_sink", resolve("heat sink"));
        assertEquals("deploy_heat_sink", resolve("dump heat"));
    }

    /**
     * Chaff is a normal-space countermeasure, so the phrase must fall back to the normal path in supercruise
     * rather than firing a binding the game will refuse.
     */
    @Test
    void theReflexRespectsSupercruise() {
        assertTrue(resolver.resolve(PhoneticInputNormalizer.normalize("chaff"), GameStateSnapshot.capture(
                Status.detached(PlayerSituation.IN_SHIP_SUPERCRUISE))).isEmpty());
    }

    /**
     * Resolves the way a live turn does: normalizer first, then the reflex, in normal space.
     */
    private String resolve(String utterance) {
        String normalized = PhoneticInputNormalizer.normalize(utterance);
        Optional<ReflexResolver.Reflex> reflex = resolver.resolve(normalized, GameStateSnapshot.capture(
                Status.detached(PlayerSituation.IN_SHIP_DEEP_SPACE)));
        assertTrue(reflex.isPresent(),
                () -> "\"" + utterance + "\" (normalized: \"" + normalized + "\") must not depend on the local model");
        return reflex.get().actionId();
    }
}
