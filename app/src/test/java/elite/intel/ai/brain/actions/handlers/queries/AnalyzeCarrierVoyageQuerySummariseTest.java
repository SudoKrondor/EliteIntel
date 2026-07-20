package elite.intel.ai.brain.actions.handlers.queries;

import elite.intel.ai.brain.actions.handlers.queries.AnalyzeCarrierVoyageQuery.DataDto;
import elite.intel.ai.brain.actions.handlers.queries.carrier.CarrierOwnership;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The pure route summary behind {@code query_carrier_voyage}.
 * <p>
 * The cases that matter are the ones about a route the carrier is already partway along: the store deletes each
 * leg as the carrier arrives, so the surviving map keys are the ORIGINAL leg numbers. Everything the commander is
 * told has to be counted from where the carrier is now, not read out of those keys.
 */
class AnalyzeCarrierVoyageQuerySummariseTest {

    private static CarrierJump jump(int leg, String systemName, boolean hasIcyRing) {
        CarrierJump carrierJump = new CarrierJump();
        carrierJump.setLeg(leg);
        carrierJump.setSystemName(systemName);
        carrierJump.setHasIcyRing(hasIcyRing);
        carrierJump.setPristine(false);
        return carrierJump;
    }

    private static SortedMap<Integer, CarrierJump> route(CarrierJump... jumps) {
        SortedMap<Integer, CarrierJump> route = new TreeMap<>();
        for (CarrierJump jump : jumps) {
            route.put(jump.getLeg(), jump);
        }
        return route;
    }

    private static DataDto summarise(SortedMap<Integer, CarrierJump> route) {
        return AnalyzeCarrierVoyageQuery.summarise(CarrierOwnership.FLEET, "Colonia", route, 900, 500);
    }

    /**
     * The regression this method exists for: legs 4 and 5 remain, so the refuel stop is 2 jumps away, not 5.
     */
    @Test
    void refuelDistanceIsCountedFromHereNotFromTheOriginalLegNumber() {
        DataDto voyage = summarise(route(
                jump(4, "Eol Prou", false),
                jump(5, "Colonia", true)));

        assertEquals(2, voyage.totalJumps());
        assertEquals("Colonia", voyage.nearestRefuelSystem());
        assertEquals(2, voyage.jumpsToNearestRefuelStop());
    }

    /**
     * A stop's reported leg is its position in what remains, so it can never exceed totalJumps.
     */
    @Test
    void routeStopsAreRenumberedFromOne() {
        DataDto voyage = summarise(route(
                jump(7, "Alpha", false),
                jump(8, "Beta", false),
                jump(9, "Gamma", false)));

        assertEquals(List.of(1, 2, 3), voyage.route().stream().map(AnalyzeCarrierVoyageQuery.RouteStop::leg).toList());
        assertEquals(List.of("Alpha", "Beta", "Gamma"),
                voyage.route().stream().map(AnalyzeCarrierVoyageQuery.RouteStop::systemName).toList());
        assertTrue(voyage.route().stream().allMatch(stop -> stop.leg() <= voyage.totalJumps()));
    }

    @Test
    void everyIcyRingSystemIsListedInRouteOrderAndTheNearestIsTheFirst() {
        DataDto voyage = summarise(route(
                jump(1, "Alpha", false),
                jump(2, "Beta", true),
                jump(3, "Gamma", false),
                jump(4, "Delta", true)));

        assertEquals(List.of("Beta", "Delta"), voyage.refuelSystems());
        assertEquals("Beta", voyage.nearestRefuelSystem());
        assertEquals(2, voyage.jumpsToNearestRefuelStop());
    }

    /**
     * With no icy ring anywhere, all three refuel fields stay null/empty so the serializer omits them together.
     * A zero here would read to the model as "refuel at the next jump".
     */
    @Test
    void aRouteWithNoIcyRingReportsNoRefuelStopAtAll() {
        DataDto voyage = summarise(route(
                jump(1, "Alpha", false),
                jump(2, "Beta", false)));

        assertTrue(voyage.refuelSystems().isEmpty());
        assertNull(voyage.nearestRefuelSystem());
        assertNull(voyage.jumpsToNearestRefuelStop());
    }

    @Test
    void travelTimeIsTwentyMinutesPerRemainingJumpSplitIntoHoursAndMinutes() {
        DataDto voyage = summarise(route(
                jump(1, "Alpha", false),
                jump(2, "Beta", false),
                jump(3, "Gamma", false),
                jump(4, "Delta", false)));

        assertEquals(80, voyage.timeToFinalDestinationInMinutes());
        assertEquals(1, voyage.travelTimeHours());
        assertEquals(20, voyage.travelTimeMinutes());
    }

    @Test
    void fuelBalanceIsTheSurplusOverWhatTheRouteNeeds() {
        DataDto voyage = summarise(route(jump(1, "Alpha", false)));

        assertEquals(900, voyage.currentFuelSupply());
        assertEquals(500, voyage.fuelRequired());
        assertEquals(400, voyage.fuelBalance());
    }

    @Test
    void aShortfallIsReportedAsANegativeBalance() {
        DataDto voyage = AnalyzeCarrierVoyageQuery.summarise(
                CarrierOwnership.SQUADRON, "Sol", route(jump(1, "Alpha", false)), 100, 500);

        assertEquals(-400, voyage.fuelBalance());
        assertEquals("squadron carrier", voyage.carrier());
    }
}
