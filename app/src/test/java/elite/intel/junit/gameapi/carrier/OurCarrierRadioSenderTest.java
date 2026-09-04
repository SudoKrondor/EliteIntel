package elite.intel.junit.gameapi.carrier;

import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.db.managers.SquadronCarrierManager;
import elite.intel.gameapi.carrier.OurCarriers;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Recognising the commander's own carrier on the comms channel, so its traffic control can speak with the
 * voice they gave it instead of a stranger drawn per transmission.
 * <p>
 * {@code ReceiveText} signs a carrier with its NAME and callsign together - "LONE WOLF GHY-L8X", 2002 times
 * across two months of journals - while {@code DockingGranted} names the same carrier "GHY-L8X".
 */
class OurCarrierRadioSenderTest {

    private static final String CALL_SIGN = "GHY-L8X";
    private static final String CARRIER_NAME = "LONE WOLF";

    @BeforeEach
    void ourCarrierIsOnFile() {
        CarrierDataDto fleet = new CarrierDataDto();
        fleet.setCallSign(CALL_SIGN);
        fleet.setCarrierName(CARRIER_NAME);
        fleet.setVoice("BELLA");
        FleetCarrierManager.getInstance().save(fleet);

        CarrierDataDto squadron = new CarrierDataDto();
        squadron.setCallSign("QQQ-11Z");
        squadron.setCarrierName("SQUADRON HOME");
        SquadronCarrierManager.getInstance().save(squadron);
    }

    @Test
    void aCarrierIsRecognisedByEitherFormTheJournalUses() {
        assertTrue(OurCarriers.byRadioSender(CARRIER_NAME + " " + CALL_SIGN).isPresent(), "ReceiveText form");
        assertTrue(OurCarriers.byRadioSender(CALL_SIGN).isPresent(), "DockingGranted form");
        assertTrue(OurCarriers.byRadioSender("lone wolf ghy-l8x").isPresent(), "case is the journal's business");
    }

    @Test
    void anotherCommandersCarrierIsAStranger() {
        // Every carrier in the galaxy signs its transmissions this way; only the callsign says whose it is.
        assertTrue(OurCarriers.byRadioSender("SOME OTHER RIG ABC-99X").isEmpty());
        assertTrue(OurCarriers.byRadioSender("Abasheli City").isEmpty());
        assertTrue(OurCarriers.byRadioSender(null).isEmpty());
        assertTrue(OurCarriers.byRadioSender("   ").isEmpty());
    }

    @Test
    void aCallSignThatIsMerelyContainedIsNotOurs() {
        // The callsign is the last word of the sender, not a substring of it.
        assertTrue(OurCarriers.byRadioSender(CALL_SIGN + " STATION").isEmpty());
        assertTrue(OurCarriers.byRadioSender("PREFIX" + CALL_SIGN).isEmpty());
    }

    @Test
    void theAssignedVoiceIsTheOneTheCarrierWasGiven() {
        assertEquals("BELLA", OurCarriers.radioVoiceOf(CARRIER_NAME + " " + CALL_SIGN));
        // A carrier with no voice picked, and anyone else, stay strangers drawn at random.
        assertNull(OurCarriers.radioVoiceOf("SQUADRON HOME QQQ-11Z"));
        assertNull(OurCarriers.radioVoiceOf("Abasheli City"));
    }

    @Test
    void bothCarriersAreListedForTheFleetGrid() {
        assertEquals(2, OurCarriers.known().size());
        assertEquals(OurCarriers.Kind.FLEET, OurCarriers.known().getFirst().kind());
        assertEquals(OurCarriers.Kind.SQUADRON, OurCarriers.known().get(1).kind());
    }
}
