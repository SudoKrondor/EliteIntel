package elite.intel.junit.db.managers;

import elite.intel.db.managers.CarrierRouteLegs;
import elite.intel.search.spansh.carrierroute.CarrierJump;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rule "enter next carrier destination" depends on: a route is what is still to fly, so it can
 * never name the system the carrier is sitting in.
 */
class CarrierRouteLegsTest {

    @Test
    void carrierMidRouteDropsTheCurrentSystemAndEverythingBehindIt() {
        List<CarrierJump> plotted = route("A", "B", "C", "D", "E");

        List<CarrierJump> remaining = CarrierRouteLegs.stillToFly(plotted, "B");

        assertEquals(List.of("C", "D", "E"), names(remaining));
    }

    @Test
    void carrierAtTheHeadOfTheRouteDropsOnlyTheHead() {
        List<CarrierJump> plotted = route("A", "B", "C", "D", "E");

        List<CarrierJump> remaining = CarrierRouteLegs.stillToFly(plotted, "A");

        assertEquals(List.of("B", "C", "D", "E"), names(remaining));
    }

    @Test
    void legsAreRenumberedSoLegOneIsAlwaysTheNextJump() {
        List<CarrierJump> remaining = CarrierRouteLegs.stillToFly(route("A", "B", "C", "D"), "B");

        assertEquals(List.of(1, 2), remaining.stream().map(CarrierJump::getLeg).toList());
        assertEquals("C", remaining.get(0).getSystemName(), "leg 1 is the system the carrier flies to next");
    }

    @Test
    void carrierAtTheDestinationLeavesNothingToFly() {
        assertTrue(CarrierRouteLegs.stillToFly(route("A", "B", "C"), "C").isEmpty());
    }

    /**
     * A carrier that jumped somewhere the route never mentioned has flown off-route. Nothing is
     * consumed; the caller re-plots from where it now is.
     */
    @Test
    void carrierOffRouteConsumesNothing() {
        List<CarrierJump> remaining = CarrierRouteLegs.stillToFly(route("A", "B", "C"), "Sol");

        assertEquals(List.of("A", "B", "C"), names(remaining));
    }

    /**
     * Spansh and the journal are the two sources of a system name and need not agree on case or
     * padding. An exact match that misses would leave the carrier's own system as leg 1.
     */
    @Test
    void currentSystemMatchesRegardlessOfCaseAndPadding() {
        List<CarrierJump> remaining = CarrierRouteLegs.stillToFly(route("Deciat", "Shinrarta Dezhra"), "  deciat ");

        assertEquals(List.of("Shinrarta Dezhra"), names(remaining));
    }

    /**
     * Truncating at the last occurrence, not the first, is what makes the guarantee absolute: a route
     * that doubles back through the current system must not hand that system out again.
     */
    @Test
    void routeDoublingBackThroughTheCurrentSystemNeverYieldsIt() {
        List<CarrierJump> remaining = CarrierRouteLegs.stillToFly(route("A", "B", "A", "C"), "A");

        assertEquals(List.of("C"), names(remaining));
    }

    @Test
    void unknownCarrierSystemTruncatesNothing() {
        assertEquals(List.of("A", "B"), names(CarrierRouteLegs.stillToFly(route("A", "B"), null)));
        assertEquals(List.of("A", "B"), names(CarrierRouteLegs.stillToFly(route("A", "B"), "  ")));
    }

    private static List<CarrierJump> route(String... systemNames) {
        return java.util.stream.IntStream.range(0, systemNames.length)
                .mapToObj(i -> {
                    CarrierJump jump = new CarrierJump();
                    jump.setLeg(i + 1);
                    jump.setSystemName(systemNames[i]);
                    return jump;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private static List<String> names(List<CarrierJump> legs) {
        return legs.stream().map(CarrierJump::getSystemName).toList();
    }
}
