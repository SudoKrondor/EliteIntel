package elite.intel.junit.db.managers;

import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@code getDestination()} answers "where is this ship headed", and several callers act on it: the
 * HUD mission card features the mission waiting at the far end, and {@code RoutePlotter} skips
 * plotting a route it believes is already set. Both misbehave quietly if it returns the wrong leg,
 * so the last leg is pinned here rather than left to the DAO's ordering.
 */
class ShipRouteManagerTest {

    private final ShipRouteManager route = ShipRouteManager.getInstance();

    @BeforeEach
    void clearRoute() {
        route.clearRoute();
    }

    @AfterEach
    void tidy() {
        route.clearRoute();
    }

    @Test
    void theDestinationIsTheLastLegWhateverOrderTheLegsArriveIn() {
        // Written back to front: the answer must come from the leg number, not from insertion order.
        plot(3, "Deciat");
        plot(1, "Sol");
        plot(2, "Alpha Centauri");

        assertEquals("Deciat", route.getDestination());
    }

    /**
     * Legs are consumed as they are flown, so the destination is what is still ahead rather than
     * what the route was first plotted to.
     */
    @Test
    void flyingALegDoesNotMoveTheDestination() {
        plot(1, "Sol");
        plot(2, "Alpha Centauri");
        plot(3, "Deciat");

        route.removeLeg("Sol");

        assertEquals("Deciat", route.getDestination());
        assertEquals(List.of("Alpha Centauri", "Deciat"),
                route.getOrderedRoute().stream().map(NavRouteDto::getName).toList());
    }

    @Test
    void theLastLegFlownLeavesNoDestination() {
        plot(1, "Sol");

        route.removeLeg("Sol");

        assertNull(route.getDestination());
    }

    /**
     * The game writes NavRoute.json at plot time only, so after a reload the stored route still starts at the
     * plot's origin. Arriving mid-route must drop every leg up to the arrival, not just the arrival's own.
     * Seen in a support bundle: arriving in the second system still announced the first as "next", 5 jumps.
     */
    @Test
    void arrivingConsumesTheArrivalLegAndEveryLegBeforeIt() {
        plot(1, "Condovichs");
        plot(2, "ICZ FW-V b2-1");
        plot(3, "Vequess");
        plot(4, "Coastakal");
        plot(5, "Miphifa");

        route.arrivedAt("icz fw-v b2-1");

        assertEquals(List.of("Vequess", "Coastakal", "Miphifa"),
                route.getOrderedRoute().stream().map(NavRouteDto::getName).toList());
    }

    @Test
    void arrivingOffTheRouteLeavesItAlone() {
        plot(1, "Sol");
        plot(2, "Deciat");

        route.arrivedAt("Shinrarta Dezhra");

        assertEquals(List.of("Sol", "Deciat"),
                route.getOrderedRoute().stream().map(NavRouteDto::getName).toList());
    }

    @Test
    void noRouteMeansNoDestination() {
        assertNull(route.getDestination());
    }

    private void plot(int leg, String system) {
        NavRouteDto dto = new NavRouteDto();
        dto.setLeg(leg);
        dto.setName(system);
        dto.setStarClass("G");
        dto.setScoopable(true);
        route.updateRouteNode(dto);
    }
}
