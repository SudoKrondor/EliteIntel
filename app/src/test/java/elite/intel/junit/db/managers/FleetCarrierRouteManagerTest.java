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
