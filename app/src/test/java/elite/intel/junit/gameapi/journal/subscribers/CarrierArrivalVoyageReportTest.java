package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.vega.CompanionNarrator;
import elite.intel.ai.brain.vega.CompanionRuntimeGraph;
import elite.intel.ai.brain.vega.CompanionRuntimeTestSupport;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.gameapi.journal.events.CarrierJumpEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.subscribers.CarrierJumpCompleteSubscriber;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a carrier arrival is allowed to say about the plotted voyage.
 *
 * <p>A commander scheduled a single jump of his own - carriers reach anything inside 500 ly in one hop, so
 * most jumps are single and unplotted - and was told at the end of it that one more leg remained. There was
 * no such leg. The announcement counted whatever the route table happened to hold, and it held a route to a
 * system he had stopped following weeks earlier: nothing consumed it, because he never arrived at any of its
 * legs, and nothing cleared it, because an off-route arrival used to re-plot the route rather than void it.
 * That plot was made from wherever he had landed, to the destination read out of the route being replaced,
 * so the leg count was not merely stale - it was regenerated, over Spansh, after every jump and every app
 * start.
 *
 * <p>The rule these tests hold to: a leg count is only ever about a route the carrier is actually on.
 */
class CarrierArrivalVoyageReportTest {

    private static final String SYSTEM = "Hyades Sector MH-V c2-8";

    private final CarrierJumpCompleteSubscriber subscriber = new CarrierJumpCompleteSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();
    private final FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
    private final CapturingNarrator narrator = new CapturingNarrator();
    private CompanionRuntimeGraph runtimeGraph;

    @BeforeEach
    void installNarrator() {
        runtimeGraph = CompanionRuntimeTestSupport.installNarrator(narrator);
        route.clear();
        session.setFleetCarrierData(new CarrierDataDto());
        session.setLastKnownCarrierLocation("Struve 2398");
    }

    @AfterEach
    void clearNarrator() {
        route.clear();
        CompanionRuntimeTestSupport.uninstall(runtimeGraph);
    }

    /**
     * The reported bug, in the shape it was reported: one manual jump, one stale route, "1 jump remaining".
     */
    @Test
    void aJumpOffAStaleRouteReportsTheRouteDroppedRatherThanALegCount() throws Exception {
        route.setFleetCarrierRoute(Map.of(1, leg("Zeus")));

        String spoken = narrateArrival();

        assertFalse(spoken.contains("Remaining"),
                () -> "a leg the carrier never flew towards is not a leg remaining: " + spoken);
        assertTrue(spoken.contains("Route cleared"), spoken);
        assertFalse(route.hasStoredLegs(), "the route the commander stopped following is gone for good");
    }

    /**
     * The ordinary case, and the one the leg count used to get wrong in the other direction: with no route
     * at all, an arrival used to announce "Final destination reached!" because zero legs remained.
     */
    @Test
    void aJumpWithNoRoutePlottedSaysNothingAboutLegs() throws Exception {
        String spoken = narrateArrival();

        assertFalse(spoken.contains("Remaining"), spoken);
        assertFalse(spoken.contains("Final destination"), spoken);
        assertFalse(spoken.contains("Route cleared"),
                () -> "there was no route to clear, so there is nothing to report: " + spoken);
    }

    @Test
    void aJumpAlongThePlottedRouteCountsTheLegsStillToFly() throws Exception {
        route.setFleetCarrierRoute(Map.of(1, leg(SYSTEM), 2, leg("Deciat"), 3, leg("Colonia")));

        String spoken = narrateArrival();

        assertTrue(spoken.contains("Remaining 2 jumps"), spoken);
    }

    @Test
    void theLastLegOfTheRouteEndsTheVoyage() throws Exception {
        route.setFleetCarrierRoute(Map.of(1, leg(SYSTEM)));

        String spoken = narrateArrival();

        assertTrue(spoken.contains("Final destination"), spoken);
    }

    /**
     * Runs the subscriber and returns the payload it handed the model.
     */
    private String narrateArrival() throws InterruptedException {
        subscriber.onCarrierJumpCompleteEvent(carrierJump());
        assertTrue(narrator.awaitNarration(5, TimeUnit.SECONDS), "the arrival was never announced");
        return narrator.data;
    }

    private static CarrierJump leg(String systemName) {
        CarrierJump jump = new CarrierJump();
        jump.setSystemName(systemName);
        jump.setFuelUsed(50);
        jump.setX(119.90625);
        jump.setY(-87.6875);
        jump.setZ(-184.625);
        return jump;
    }

    private static CarrierJumpEvent carrierJump() {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CarrierJump");
        j.addProperty("Docked", false);
        j.addProperty("OnFoot", true);
        j.addProperty("StarSystem", SYSTEM);
        j.addProperty("SystemAddress", 2283077046962L);
        j.addProperty("Body", SYSTEM + " 5");
        j.addProperty("BodyID", 20L);
        j.addProperty("BodyType", "Planet");
        JsonArray starPos = new JsonArray();
        starPos.add(119.90625);
        starPos.add(-87.6875);
        starPos.add(-184.625);
        j.add("StarPos", starPos);
        return new CarrierJumpEvent(j);
    }

    private static final class CapturingNarrator implements CompanionNarrator {
        private final CountDownLatch latch = new CountDownLatch(1);
        volatile String data;

        @Override
        public void filler(String text, boolean urgent) {
        }

        @Override
        public void narrate(String data, String instructions) {
            this.data = data;
            latch.countDown();
        }

        @Override
        public void announce(String phrase, boolean urgent) {
        }

        boolean awaitNarration(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }
}
