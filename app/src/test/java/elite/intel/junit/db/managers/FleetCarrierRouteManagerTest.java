package elite.intel.junit.db.managers;

import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FleetCarrierRouteManagerTest {

    private final FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
    private final PlayerSession session = PlayerSession.getInstance();

    @BeforeEach
    void clearRoute() {
        route.clear();
    }

    @AfterEach
    void resetSession() {
        route.clear();
        session.setLastKnownCarrierLocation(null);
    }

    @Test
    void aPlottedRouteNeverStoresTheSystemTheCarrierIsIn() {
        session.setLastKnownCarrierLocation("Deciat");

        route.setFleetCarrierRoute(plot("Deciat", "Maia", "Merope"));

        assertEquals(List.of("Maia", "Merope"), storedNames());
    }

    /**
     * The route table outlives the process. A run that ended mid-voyage leaves legs the carrier has
     * since flown, and "enter next carrier destination" reads leg 1 straight out of that table.
     */
    @Test
    void aRouteLeftByAPreviousRunIsTruncatedAtTheCarriersCurrentSystem() {
        session.setLastKnownCarrierLocation("Colonia");
        route.setFleetCarrierRoute(plot("Maia", "Merope", "Colonia", "Sol"));
        assertEquals(List.of("Sol"), storedNames(), "the plot itself is truncated");

        // The carrier moved on while nothing was watching, and the table still holds the old legs.
        session.setLastKnownCarrierLocation("Sol");

        assertTrue(route.getFleetCarrierRoute().isEmpty(), "no leg may name the system we are sitting in");
        assertNull(route.getFinalDestination());
        assertEquals(0, route.getTotalFuelRequired());
    }

    @Test
    void nextLegIsAlwaysLegOneAndIsNeverWhereWeAre() {
        session.setLastKnownCarrierLocation("Maia");
        route.setFleetCarrierRoute(plot("Maia", "Merope", "Colonia"));

        Map<Integer, CarrierJump> remaining = route.getFleetCarrierRoute();

        assertEquals("Merope", remaining.get(1).getSystemName());
        assertEquals("Colonia", route.getFinalDestination());
    }

    /**
     * Arrival consumes the leg reached and every leg before it, so a missed arrival event cannot
     * leave a stale leg ahead of the carrier.
     */
    @Test
    void arrivalConsumesTheLegReachedAndEveryLegBeforeIt() {
        session.setLastKnownCarrierLocation("Sol");
        route.setFleetCarrierRoute(plot("Maia", "Merope", "Colonia"));

        route.removeLeg("Merope");

        assertEquals(List.of("Colonia"), storedNames());
    }

    @Test
    void arrivalOffRouteConsumesNothing() {
        session.setLastKnownCarrierLocation("Sol");
        route.setFleetCarrierRoute(plot("Maia", "Merope"));

        route.removeLeg("Shinrarta Dezhra");

        assertEquals(List.of("Maia", "Merope"), storedNames());
    }

    /**
     * The arrival handler needs the completed leg's tritium cost and coordinates, which is exactly the
     * leg the route has stopped reporting.
     */
    @Test
    void theCompletedLegIsStillReadableForItsFuelAndCoordinates() {
        session.setLastKnownCarrierLocation("Sol");
        route.setFleetCarrierRoute(plot("Maia", "Merope"));
        session.setLastKnownCarrierLocation("Maia");

        assertTrue(route.getFleetCarrierRoute().values().stream()
                .noneMatch(leg -> "Maia".equals(leg.getSystemName())));

        CarrierJump completed = route.findByPrimaryStar("maia");
        assertNotNull(completed, "case must not decide whether an arrival is on-route");
        assertEquals(10, completed.getFuelUsed());
    }

    @Test
    void aFailedPlotLeavesTheStandingRouteAlone() {
        session.setLastKnownCarrierLocation("Sol");
        route.setFleetCarrierRoute(plot("Maia", "Merope"));

        route.setFleetCarrierRoute(Map.of());

        assertEquals(List.of("Maia", "Merope"), storedNames());
    }

    @Test
    void fuelRequiredCoversOnlyTheLegsStillToFly() {
        session.setLastKnownCarrierLocation("Sol");
        route.setFleetCarrierRoute(plot("Maia", "Merope", "Colonia"));
        session.setLastKnownCarrierLocation("Maia");

        assertEquals(20, route.getTotalFuelRequired(), "the tritium for the leg already flown is not still owed");
    }

    // -- a re-plot that was overtaken while Spansh was answering -------------------

    /**
     * The re-plot that follows an off-route arrival runs detached, because it calls Spansh and must not
     * hold up the arrival announcement. That announcement ("N jumps left") is exactly what prompts a
     * commander to abandon a route he stopped following, so the clear and the plot genuinely overlap.
     * Storing the plot afterwards would put the route back, seconds after he was told it was cleared.
     */
    @Test
    void aPlotIsDroppedWhenTheRouteWasAbandonedWhileItWasBeingCalculated() {
        session.setLastKnownCarrierLocation("Deciat");
        route.setFleetCarrierRoute(plot("Maia", "Merope"));

        long generation = route.generation();   // the re-plot reads this, then calls Spansh
        route.clear();                          // ... and the commander abandons the route meanwhile

        assertFalse(route.setFleetCarrierRouteIfUnchanged(plot("Sol", "Colonia"), generation),
                "a plot must not outlive the route it was repairing");
        assertTrue(route.getFleetCarrierRoute().isEmpty(), "an abandoned route must stay abandoned");
    }

    @Test
    void aPlotIsStoredWhenNothingTouchedTheRouteWhileItWasBeingCalculated() {
        session.setLastKnownCarrierLocation("Deciat");
        route.setFleetCarrierRoute(plot("Maia", "Merope"));

        long generation = route.generation();

        assertTrue(route.setFleetCarrierRouteIfUnchanged(plot("Sol", "Colonia"), generation),
                "an uncontested repair must still land");
        assertEquals(List.of("Sol", "Colonia"), storedNames());
    }

    /**
     * A jump that lands on a plotted leg while the re-plot is out moves the route on, so the plot was
     * made from a system the carrier has already left.
     */
    @Test
    void aPlotIsDroppedWhenAnArrivalConsumedALegWhileItWasBeingCalculated() {
        session.setLastKnownCarrierLocation("Deciat");
        route.setFleetCarrierRoute(plot("Maia", "Merope", "Colonia"));

        long generation = route.generation();
        route.removeLeg("Maia");

        assertFalse(route.setFleetCarrierRouteIfUnchanged(plot("Sol"), generation));
        assertEquals(List.of("Merope", "Colonia"), storedNames(), "the consumed route stands as it is");
    }

    /**
     * The counterpart: an off-route arrival consumes nothing, so it must not invalidate the very
     * re-plot it triggered.
     */
    @Test
    void anArrivalThatConsumedNothingLeavesAnInFlightPlotValid() {
        session.setLastKnownCarrierLocation("Deciat");
        route.setFleetCarrierRoute(plot("Maia", "Merope"));

        long generation = route.generation();
        route.removeLeg("Shinrarta Dezhra");

        assertTrue(route.setFleetCarrierRouteIfUnchanged(plot("Sol"), generation));
        assertEquals(List.of("Sol"), storedNames());
    }

    @Test
    void aFailedPlotIsNotStoredAndDoesNotCountAsAChange() {
        session.setLastKnownCarrierLocation("Deciat");
        route.setFleetCarrierRoute(plot("Maia", "Merope"));

        long generation = route.generation();

        assertFalse(route.setFleetCarrierRouteIfUnchanged(Map.of(), generation));
        assertEquals(List.of("Maia", "Merope"), storedNames(), "the standing route is left alone");
        assertEquals(generation, route.generation(), "a plot that stored nothing changed nothing");
    }

    private static Map<Integer, CarrierJump> plot(String... systemNames) {
        Map<Integer, CarrierJump> plotted = new LinkedHashMap<>();
        for (int i = 0; i < systemNames.length; i++) {
            CarrierJump jump = new CarrierJump();
            jump.setLeg(i + 1);
            jump.setSystemName(systemNames[i]);
            jump.setFuelUsed(10);
            plotted.put(i + 1, jump);
        }
        return plotted;
    }

    private List<String> storedNames() {
        return route.getFleetCarrierRoute().values().stream().map(CarrierJump::getSystemName).toList();
    }
}
