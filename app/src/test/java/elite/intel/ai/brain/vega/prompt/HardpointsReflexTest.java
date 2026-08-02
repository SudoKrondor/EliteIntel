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
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards "hardpoints", which answered with the ship's loadout instead of deploying them.
 *
 * <p>No action owned the bare phrase, so {@link ReflexResolver} matched nothing and the turn fell through
 * to the local model. The reducer then kept three candidates inside its margin
 * ({@code query_ship_loadout}, {@code retract_hardpoints}, {@code deploy_hardpoints}) because
 * {@code query_ship_loadout} carried "any hardpoints" and "hardpoints installed", and the model picked the
 * query three times running - the commander was mining in a ring at the time, and the command described
 * itself as arming weapons for combat.
 *
 * <p>Deploying is an order, not a question: a hardpoint carries mining lasers and limpet controllers as
 * readily as guns. The bare noun now belongs to the command in every locale, so it resolves here with no
 * model in the loop at all.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HardpointsReflexTest {

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
     * The reported utterance, in the situation it was reported from.
     */
    @Test
    void aBareHardpointsDeploysRatherThanDescribingTheShip() {
        Optional<ReflexResolver.Reflex> reflex = resolve("hardpoints");
        assertTrue(reflex.isPresent(), "\"hardpoints\" must never depend on the local model");
        assertEquals("deploy_hardpoints", reflex.get().actionId());
    }

    /**
     * What the commander actually says. STT hears two words and the normalizer joins them, so the reflex
     * only ever sees "hardpoints" - this pins that hand-off, which is where the reported log started.
     */
    @Test
    void theSpokenTwoWordFormNormalizesIntoTheSameReflex() {
        String normalized = PhoneticInputNormalizer.normalize("hard points");
        assertEquals("hardpoints", normalized);
        assertEquals("deploy_hardpoints", resolve(normalized).orElseThrow().actionId());
    }

    @ParameterizedTest(name = "{0} \"{1}\" deploys")
    @CsvSource({
            "EN, hardpoints",
            "DE, hardpoints",
            "ES, hardpoints",
            "ES, anclajes",
            "IT, hardpoints",
            "PT, hardpoints",
            "PTBZ, hardpoints",
            "FR, points d'emport",
            "RU, хардпойнты",
            "UK, хардпойнти",
    })
    void everyLocaleDeploysOnItsOwnBareNoun(Language language, String utterance) {
        SystemSession.getInstance().setLanguage(language);
        try {
            Optional<ReflexResolver.Reflex> reflex = resolve(utterance);
            assertTrue(reflex.isPresent(),
                    () -> language + " \"" + utterance + "\" must resolve without the LLM");
            assertEquals("deploy_hardpoints", reflex.get().actionId());
        } finally {
            SystemSession.getInstance().setLanguage(Language.EN);
        }
    }

    /**
     * Giving the bare noun to the command must not swallow the opposite order.
     */
    @Test
    void retractingStillResolvesToItsOwnCommand() {
        assertEquals("retract_hardpoints", resolve("retract hardpoints").orElseThrow().actionId());
    }

    /**
     * Hardpoints are locked in a station's no-fire zone, so the command is not visible there and the bare
     * noun must fall back to the normal path rather than firing a binding the game will refuse.
     */
    @Test
    void theReflexRespectsTheNoFireZone() {
        assertTrue(resolver.resolve("hardpoints", GameStateSnapshot.capture(
                Status.detached(PlayerSituation.IN_SHIP_DOCKED))).isEmpty());
    }

    /**
     * The commander was in a ring when this was reported - a mining context, which is exactly where the
     * old combat-only wording sent the turn to the loadout query.
     */
    private Optional<ReflexResolver.Reflex> resolve(String utterance) {
        return resolver.resolve(utterance, GameStateSnapshot.capture(
                Status.detached(PlayerSituation.IN_SHIP_RING)));
    }
}
