package elite.intel.gameapi.journal.events.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A depot level is exact only while the game is the one who reported it.
 *
 * <p>WHY this matters enough to track: the game writes the depot level when the commander opens carrier
 * management and at no other time, so between those moments the figure is our own arithmetic on top of an
 * older reading. Announcing that as a flat number is the same error as announcing the pre-jump level was:
 * it tells the commander something we do not know. It drifts for reasons we never see either - tritium sold
 * off the market, a squadron mate topping the depot up.
 */
class CarrierFuelConfidenceTest {

    @Test
    void aLevelTheGameReportedIsExact() {
        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setMeasuredFuelLevel(1000);

        assertTrue(carrier.isFuelLevelMeasured());
        assertEquals(1000, carrier.getFuelLevel());
    }

    @Test
    void chargingAJumpMakesTheLevelAnEstimate() {
        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setMeasuredFuelLevel(1000);

        carrier.chargeEstimatedFuel(100);

        assertEquals(900, carrier.getFuelLevel());
        assertFalse(carrier.isFuelLevelMeasured(), "the leg's planned burn is not a reading");
    }

    @Test
    void afreshReadingRestoresConfidence() {
        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setMeasuredFuelLevel(1000);
        carrier.chargeEstimatedFuel(100);

        carrier.setMeasuredFuelLevel(880);

        assertEquals(880, carrier.getFuelLevel());
        assertTrue(carrier.isFuelLevelMeasured(),
                "opening carrier management settles it, including whatever drifted while we were guessing");
    }

    /**
     * A carrier we have never had a reading for starts at zero, and zero is not a measurement.
     */
    @Test
    void anUnreadCarrierIsNotTreatedAsMeasured() {
        assertFalse(new CarrierDataDto().isFuelLevelMeasured());
    }
}
