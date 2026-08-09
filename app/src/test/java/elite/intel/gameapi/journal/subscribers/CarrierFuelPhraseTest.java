package elite.intel.gameapi.journal.subscribers;

import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Whether the depot level may be spoken flatly is decided here, in Java, and not left to the model.
 *
 * <p>WHY: the announcement is written by an LLM from a payload, and a bare number in that payload is a bare
 * number in the commander's ear. Handing it the hedge already attached is the only way the distinction
 * survives the sentence being composed - and the same reason the credit figures are worded before they are
 * sent rather than after.
 */
class CarrierFuelPhraseTest {

    @Test
    void aMeasuredLevelIsQuotedFlatly() {
        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setMeasuredFuelLevel(880);

        assertEquals("880 tons", CarrierFuelPhrase.of(carrier));
    }

    @Test
    void anEstimatedLevelCarriesItsDoubtWithIt() {
        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setMeasuredFuelLevel(1000);
        carrier.chargeEstimatedFuel(100);

        assertEquals("approximately 900 tons", CarrierFuelPhrase.of(carrier));
    }

    /**
     * The reported case: a carrier that departed with a full depot. Announcing "1000 tons" after a jump was
     * what gave the bug away, because 1000 is the capacity of the depot and no jump ends at capacity.
     */
    @Test
    void theJumpThatStartedThisIsNoLongerQuotedAsCapacity() {
        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setMeasuredFuelLevel(1000);
        carrier.chargeEstimatedFuel(120);

        assertEquals("approximately 880 tons", CarrierFuelPhrase.of(carrier));
    }
}
