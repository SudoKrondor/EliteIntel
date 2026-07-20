package elite.intel.ai.brain.actions.handlers.queries.carrier;

import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The seam that hides the fleet/squadron manager split. The managers are injected here, so these cases pin the
 * translation this class performs rather than the database beneath it.
 */
class CarrierViewTest {

    private static CarrierView view(CarrierDataDto data,
                                    Map<Integer, CarrierJump> route,
                                    Supplier<Integer> totalFuelRequired) {
        return new CarrierView(CarrierOwnership.FLEET, () -> data, () -> route, totalFuelRequired, () -> "Colonia");
    }

    private static CarrierJump jump(int leg, String systemName) {
        CarrierJump carrierJump = new CarrierJump();
        carrierJump.setLeg(leg);
        carrierJump.setSystemName(systemName);
        return carrierJump;
    }

    /**
     * The route DAO sums nothing when no route is plotted, so it hands back a null Integer. Unboxing it is what
     * used to throw out of the fleet-carrier destination query.
     */
    @Test
    void anAbsentFuelRequirementReadsAsZeroRatherThanThrowing() {
        CarrierView carrier = view(new CarrierDataDto(), Map.of(), () -> null);

        assertEquals(0, carrier.totalFuelRequired());
    }

    @Test
    void aPresentFuelRequirementIsPassedThrough() {
        CarrierView carrier = view(new CarrierDataDto(), Map.of(), () -> 480);

        assertEquals(480, carrier.totalFuelRequired());
    }

    /**
     * The store returns a HashMap, so ordering is the view's job, not the caller's.
     */
    @Test
    void theRouteIsOrderedByLegWhateverOrderTheStoreReturns() {
        Map<Integer, CarrierJump> unordered = new LinkedHashMap<>();
        unordered.put(9, jump(9, "Gamma"));
        unordered.put(4, jump(4, "Alpha"));
        unordered.put(7, jump(7, "Beta"));

        CarrierView carrier = view(new CarrierDataDto(), unordered, () -> 0);

        assertEquals(List.of(4, 7, 9), List.copyOf(carrier.route().keySet()));
        assertEquals(List.of("Alpha", "Beta", "Gamma"),
                carrier.route().values().stream().map(CarrierJump::getSystemName).toList());
    }

    @Test
    void anEmptyRouteStaysEmpty() {
        CarrierView carrier = view(new CarrierDataDto(), Map.of(), () -> null);

        assertTrue(carrier.route().isEmpty());
    }

    /**
     * A carrier the commander does not own reads as all-zero rather than as an absence.
     */
    @Test
    void aCarrierWithNoBalanceAndNoFuelCountsAsHavingNoData() {
        assertFalse(view(new CarrierDataDto(), Map.of(), () -> 0).hasData());
    }

    @Test
    void fuelAloneIsEnoughToCountAsData() {
        CarrierDataDto data = new CarrierDataDto();
        data.setFuelLevel(120);

        assertTrue(view(data, Map.of(), () -> 0).hasData());
    }

    @Test
    void balanceAloneIsEnoughToCountAsData() {
        CarrierDataDto data = new CarrierDataDto();
        data.setTotalBalance(5_000_000L);

        assertTrue(view(data, Map.of(), () -> 0).hasData());
    }

    @Test
    void theViewRemembersWhichCarrierItSpeaksFor() {
        assertEquals(CarrierOwnership.FLEET, view(new CarrierDataDto(), Map.of(), () -> 0).ownership());
        assertEquals(CarrierOwnership.SQUADRON, CarrierView.of(CarrierOwnership.SQUADRON).ownership());
    }
}
