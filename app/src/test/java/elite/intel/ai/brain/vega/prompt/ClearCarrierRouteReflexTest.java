package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.i18n.AliasPhrase;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards "cancel carrier route", which used to clear everything except the carrier route.
 *
 * <p>No action could clear it - {@code FleetCarrierRouteManager.clear()} had no caller anywhere in the app -
 * so {@link ReflexResolver} matched nothing and the reducer offered the nearest siblings it had. On
 * 2026-08-27 a commander asked four times and got {@code dismiss_construction_site}, then
 * {@code cancel_trade_route}, then {@code clear_neutron_route}, then {@code cancel_trade_route} again:
 * four unrelated things destroyed, and the carrier route still on file.
 *
 * <p>The route is not merely inert while it sits there. An arrival in a system the route never mentions
 * consumes no leg and re-plots from there to the same final destination, so an abandoned route follows the
 * carrier around and is quoted back on every jump - which is how a single manually scheduled hop was
 * announced as "only 1 jump left, about 20 minutes out".
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearCarrierRouteReflexTest {

    private static final String CLEAR = "clear_fleet_carrier_route";

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
    void theReportedUtteranceReachesTheCarrierRoute() {
        Optional<ReflexResolver.Reflex> reflex = resolver.resolve("cancel carrier route");
        assertTrue(reflex.isPresent(), "\"cancel carrier route\" must never depend on the local model");
        assertEquals(CLEAR, reflex.get().actionId());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "clear carrier route",
            "clear fleet carrier route",
            "cancel fleet carrier route",
            "abort carrier route",
            "stop carrier route"
    })
    void everyAbandonPhrasingResolvesToTheCommand(String utterance) {
        Optional<ReflexResolver.Reflex> reflex = resolver.resolve(utterance);
        assertTrue(reflex.isPresent(), () -> utterance + " must resolve without the model");
        assertEquals(CLEAR, reflex.get().actionId(), utterance);
    }

    /**
     * The siblings it was mistaken for. Clearing the carrier route must not become the new catch-all: these
     * destroy different state, and the commander who says "trade route" means the trade route.
     */
    @ParameterizedTest
    @ValueSource(strings = {"cancel trade route", "clear trade route", "abort trade route"})
    void tradeRoutePhrasesStillReachTheTradeRoute(String utterance) {
        Optional<ReflexResolver.Reflex> reflex = resolver.resolve(utterance);
        assertTrue(reflex.isPresent(), () -> utterance + " must resolve without the model");
        assertEquals("cancel_trade_route", reflex.get().actionId(), utterance);
    }

    @Test
    void neutronRoutePhraseStillReachesTheNeutronRoute() {
        Optional<ReflexResolver.Reflex> reflex = resolver.resolve("clear neutron route");
        assertTrue(reflex.isPresent());
        assertEquals("clear_neutron_route", reflex.get().actionId());
    }

    /**
     * Plotting stays an order of its own: abandoning must not swallow the phrases that create a route.
     */
    @ParameterizedTest
    @ValueSource(strings = {"calculate fleet carrier route", "plan carrier route"})
    void plottingPhrasesStillReachThePlotter(String utterance) {
        Optional<ReflexResolver.Reflex> reflex = resolver.resolve(utterance);
        assertTrue(reflex.isPresent(), () -> utterance + " must resolve without the model");
        assertEquals("calculate_fleet_carrier_route", reflex.get().actionId(), utterance);
    }

    /**
     * Every locale needs its own way of saying it. A commander cannot abandon a route in a language whose
     * bundle never got the phrases, and the sibling that WOULD be offered instead destroys something else.
     */
    @ParameterizedTest(name = "{0} can abandon the carrier route without the model")
    @EnumSource(Language.class)
    void everyLocaleCanAbandonTheRoute(Language language) {
        SystemSession.getInstance().setLanguage(language);
        try {
            List<String> phrases = AiActionLocalizations.phrasesForAction(CLEAR);
            assertFalse(phrases.isEmpty(), language + " has no phrases for " + CLEAR);

            ReflexResolver localised = new ReflexResolver();
            for (String phrase : phrases) {
                String utterance = AliasPhrase.parse(phrase).spokenText();
                Optional<ReflexResolver.Reflex> reflex = localised.resolve(utterance);
                assertTrue(reflex.isPresent(), () -> language + ": \"" + utterance + "\" needs the model");
                assertEquals(CLEAR, reflex.get().actionId(), language + ": " + utterance);
            }
        } finally {
            SystemSession.getInstance().setLanguage(Language.EN);
        }
    }
}
