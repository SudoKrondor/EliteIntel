package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.dao.NeutronStarRouteDao;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.JetConeBoostEvent;
import elite.intel.session.Status;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supercharging at a neutron star plots the next waypoint - but only for a commander who asked for it.
 *
 * <p>Runs the subscriber with no wait and a plotter that types nothing, so the decisions are tested
 * without the suite sitting out the ten seconds the commander gets to fly clear of the cone, and without
 * a galaxy map sequence going anywhere near a keyboard.
 */
class JetConeBoostSubscriberTest {

    /**
     * In the main ship, the flag the plot is gated on.
     */
    private static final long FLAG_IN_MAIN_SHIP = 16_777_216L;

    private final List<String> plotted = new CopyOnWriteArrayList<>();
    private final SpeechRecorder speech = new SpeechRecorder();
    private final JetConeBoostSubscriber subscriber =
            new JetConeBoostSubscriber(0, destination -> {
                plotted.add(destination);
                return null; // what RoutePlotter answers when it really did plot
            }, Runnable::run);

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @BeforeEach
    void freshRoute() {
        GameEventBus.register(speech);
        clearRoute();
        setInMainShip(true);
        GlobalSettingsManager.getInstance().setAutoPlotNextNeutronJump(true);
    }

    @AfterEach
    void tidyUp() {
        GameEventBus.unregister(speech);
        GlobalSettingsManager.getInstance().setAutoPlotNextNeutronJump(false);
        clearRoute();
    }

    @Test
    void boostingOnANeutronRoutePlotsTheNextWaypoint() {
        saveRoute("Jackson's Lighthouse", "Colonia");

        subscriber.onJetConeBoost(boost());

        assertEquals(List.of("Jackson's Lighthouse"), plotted,
                "the first leg still to fly is the waypoint after the star being boosted at");
        assertTrue(speech.spoken.stream().anyMatch(text -> text.contains("Jackson's Lighthouse")),
                "the commander has to be told the map is about to open: " + speech.spoken);
    }

    @Test
    void theWarningNamesTheJetConeSoTheCommanderKnowsToFlyClear() {
        saveRoute("Colonia");

        subscriber.onJetConeBoost(boost());

        assertTrue(speech.spoken.getFirst().toLowerCase().contains("cone"),
                "a map opening with no warning leaves the commander cooking in the cone: " + speech.spoken);
    }

    @Test
    void theOptionOffMeansNothingHappens() {
        GlobalSettingsManager.getInstance().setAutoPlotNextNeutronJump(false);
        saveRoute("Colonia");

        subscriber.onJetConeBoost(boost());

        assertTrue(plotted.isEmpty(), "a galaxy map that opens unasked is alarming, not helpful");
        assertTrue(speech.spoken.isEmpty(), "and it should not be announced either: " + speech.spoken);
    }

    @Test
    void aBoostWithNoNeutronRouteIsLeftAlone() {
        // Neutron stars get boosted for their own sake all the time - a range extension towards
        // somewhere the commander picked themselves is not a route for us to continue.
        subscriber.onJetConeBoost(boost());

        assertTrue(plotted.isEmpty(), "there is no next waypoint to plot");
        assertTrue(speech.spoken.isEmpty(), speech.spoken.toString());
    }

    @Test
    void aBoostTakenOutsideTheMainShipIsLeftAlone() {
        // Plotting taps the ship-only galaxy map binding, so there is nothing this could do from a
        // fighter or on foot but type into whatever has focus.
        saveRoute("Colonia");
        setInMainShip(false);

        subscriber.onJetConeBoost(boost());

        assertTrue(plotted.isEmpty(), "the ship binding does nothing outside the ship");
    }

    @Test
    void aPlotterThatDeclinesToPlotStillAnswersTheCommander() {
        JetConeBoostSubscriber declining = new JetConeBoostSubscriber(
                0, destination -> "Already on course to Colonia.", Runnable::run);
        saveRoute("Colonia");

        declining.onJetConeBoost(boost());

        assertTrue(speech.spoken.contains("Already on course to Colonia."),
                "dropping that leaves the commander watching for a map that never opens: " + speech.spoken);
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private static JetConeBoostEvent boost() {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("event", "JetConeBoost");
        json.addProperty("BoostValue", 6.0);
        return new JetConeBoostEvent(json);
    }

    private static void saveRoute(String... systemNames) {
        Database.withDao(NeutronStarRouteDao.class, dao -> {
            for (int i = 0; i < systemNames.length; i++) {
                NeutronStarRouteDao.Route.Leg leg = new NeutronStarRouteDao.Route.Leg();
                leg.setLeg(i + 1);
                leg.setSystemAddress(1_000L + i);
                leg.setSystemName(systemNames[i]);
                leg.setNeutronStar(true);
                dao.save(leg);
            }
            return null;
        });
    }

    private static void clearRoute() {
        Database.withDao(NeutronStarRouteDao.class, dao -> {
            dao.clear();
            return null;
        });
    }

    private static void setInMainShip(boolean inShip) {
        GameEvents.StatusEvent snapshot = Status.getInstance().getStatus();
        snapshot.setFlags(inShip ? FLAG_IN_MAIN_SHIP : 0L);
        Status.getInstance().setStatus(snapshot);
    }

    private static class SpeechRecorder {
        private final List<String> spoken = new CopyOnWriteArrayList<>();

        @Subscribe
        public void onVox(AiVoxResponseEvent event) {
            spoken.add(event.getText());
        }
    }
}
