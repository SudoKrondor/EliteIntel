package elite.intel.gameapi.population;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PopulationEstimateTest {

    @Test
    void aLoneStationHoldsTheWholeSystemWhenItHasRoom() {
        PopulationEstimate estimate = new PopulationEstimate(962_494, List.of(PortSize.LARGE));

        assertEquals(960_000, estimate.of(PortSize.LARGE));
    }

    @Test
    void portsShareThePopulationBySize() {
        PopulationEstimate estimate = new PopulationEstimate(1_020_000,
                List.of(PortSize.HUGE, PortSize.MEDIUM));

        assertEquals(1_000_000, estimate.of(PortSize.HUGE));
        assertEquals(20_000, estimate.of(PortSize.MEDIUM));
    }

    @Test
    void aPortNeverHoldsMoreThanItsCapacity() {
        PopulationEstimate estimate = new PopulationEstimate(8_000_000_000L, List.of(PortSize.MEDIUM));

        assertEquals(PortSize.MEDIUM.capacity(), estimate.of(PortSize.MEDIUM));
    }

    @Test
    void aPlanetsSettlementsAreSummedAndRoundedOnce() {
        PopulationEstimate estimate = new PopulationEstimate(1_000_000,
                List.of(PortSize.LARGE, PortSize.SMALL, PortSize.SMALL, PortSize.SMALL));

        long planet = estimate.ofAll(List.of(PortSize.SMALL, PortSize.SMALL, PortSize.SMALL));

        assertEquals(15_000, planet);
    }

    @Test
    void anUninhabitedSystemHasNobodyInItsPorts() {
        PopulationEstimate estimate = new PopulationEstimate(0, List.of(PortSize.SMALL));

        assertEquals(0, estimate.of(PortSize.SMALL));
    }

    @Test
    void aSystemWithNoKnownPortsStillAnswersForTheAskedOneAsNobody() {
        assertEquals(0, new PopulationEstimate(1_000, List.of()).of(PortSize.HUGE));
    }

    @Test
    void figuresAreRoundedToTwoSignificantDigits() {
        assertEquals(12_000_000, PopulationEstimate.roundToTwoDigits(11_985_700));
        assertEquals(990, PopulationEstimate.roundToTwoDigits(987));
        assertEquals(42, PopulationEstimate.roundToTwoDigits(42));
    }

    @Test
    void aNegativePopulationIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new PopulationEstimate(-1, List.of()));
        assertTrue(e.getMessage().contains("-1"));
    }
}
