package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
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
 * Guards "analyze fleet carrier route", which plotted a new route instead of describing the existing one.
 *
 * <p>No action owned any phrase with "analyse" in it, so {@link ReflexResolver} matched nothing. The reducer
 * then scored {@code calculate_fleet_carrier_route} highest (0.949) because its own trigger is the near-identical
 * "calculate fleet carrier route", kept {@code query_carrier_voyage} inside the margin, and the model picked the
 * command twice running.
 *
 * <p>That mis-pick is worse than answering the wrong question: the command reads its destination from the system
 * clipboard, so it plotted a route to whatever text the commander happened to have copied. RU and UK already led
 * their phrase group with "анализ маршрута авианосца"; English simply never got the vocabulary.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CarrierRouteAnalysisReflexTest {

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
     * The reported utterance, verbatim from the log.
     */
    @Test
    void analyzingTheRouteReportsItRatherThanPlottingANewOne() {
        Optional<ReflexResolver.Reflex> reflex = resolver.resolve("analyze fleet carrier route");
        assertTrue(reflex.isPresent(), "\"analyze fleet carrier route\" must never depend on the local model");
        assertEquals("query_carrier_voyage", reflex.get().actionId());
    }

    /**
     * The forms the commander reaches for around it. Each must resolve on its own: the reflex matches verbatim,
     * so a phrase that is merely close to one of these still falls through to the model.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "analyse fleet carrier route",
            "analyze carrier route",
            "carrier route analysis",
            "fleet carrier route",
            "carrier refuel stops",
            "where can the carrier refuel"
    })
    void everyAnalysisPhrasingResolvesToTheQuery(String utterance) {
        Optional<ReflexResolver.Reflex> reflex = resolver.resolve(utterance);
        assertTrue(reflex.isPresent(), () -> utterance + " must resolve without the model");
        assertEquals("query_carrier_voyage", reflex.get().actionId(), utterance);
    }

    /**
     * The other half of the pair has to keep working: plotting is still an order, and taking these phrases for
     * the query would trade one misroute for its mirror image.
     */
    @ParameterizedTest
    @ValueSource(strings = {"calculate fleet carrier route", "plan carrier route", "carrier jump route"})
    void plottingPhrasesStillReachTheCommand(String utterance) {
        Optional<ReflexResolver.Reflex> reflex = resolver.resolve(utterance);
        assertTrue(reflex.isPresent(), () -> utterance + " must resolve without the model");
        assertEquals("calculate_fleet_carrier_route", reflex.get().actionId(), utterance);
    }
}
