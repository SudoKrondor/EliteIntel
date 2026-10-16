package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which plugged-in controllers onboarding still owes an offer: each once per run (Alan, 2026-10-15, A7).
 */
class HotPlugOffersTest {

    private static final HardwareId LEFT = new HardwareId("3344", "83F4");
    private static final HardwareId RIGHT = new HardwareId("3344", "03F5");

    @Test
    void aControllerPluggedInWaitsForAnOffer() {
        HotPlugOffers offers = new HotPlugOffers();
        offers.connected(1, LEFT);

        assertTrue(offers.anyWaiting());
        assertEquals(Set.of(LEFT), offers.take());
        assertFalse(offers.anyWaiting());
    }

    /** Two sticks flashed or plugged in together are one dialog, in the order they came. */
    @Test
    void severalPluggedInTogetherAreTakenTogetherInOrder() {
        HotPlugOffers offers = new HotPlugOffers();
        offers.connected(2, RIGHT);
        offers.connected(1, LEFT);

        assertEquals(List.of(RIGHT, LEFT), List.copyOf(offers.take()));
    }

    /** Alan's answer: a loose cable must not put the dialog up again. */
    @Test
    void aControllerOfferedOnceIsNotOfferedAgainWhenPluggedBackIn() {
        HotPlugOffers offers = new HotPlugOffers();
        offers.connected(1, LEFT);
        offers.take();

        offers.disconnected(1);
        offers.connected(3, LEFT);

        assertFalse(offers.anyWaiting());
    }

    /** The automatic offer after activation counts, so a late connect from the startup listing is not asked twice. */
    @Test
    void aControllerOfferedAnotherWayIsNotOfferedByHotPlug() {
        HotPlugOffers offers = new HotPlugOffers();
        offers.connected(1, LEFT);
        offers.offered(List.of(LEFT));

        assertFalse(offers.anyWaiting());
        offers.connected(4, LEFT);
        assertFalse(offers.anyWaiting());
    }

    @Test
    void aControllerUnpluggedBeforeItsOfferStopsWaitingAndWaitsAgainWhenBack() {
        HotPlugOffers offers = new HotPlugOffers();
        offers.connected(1, LEFT);
        offers.disconnected(1);

        assertFalse(offers.anyWaiting());

        offers.connected(5, LEFT);
        assertEquals(Set.of(LEFT), offers.take());
    }

    /** Two identical sticks share a VID/PID; one leaving does not take the other's offer away. */
    @Test
    void aSecondControllerOnTheSameHardwareKeepsItWaiting() {
        HotPlugOffers offers = new HotPlugOffers();
        offers.connected(1, LEFT);
        offers.connected(2, LEFT);
        offers.disconnected(1);

        assertTrue(offers.anyWaiting());
    }

    @Test
    void aDisconnectNeverSeenConnectingChangesNothing() {
        HotPlugOffers offers = new HotPlugOffers();
        offers.connected(1, LEFT);
        offers.disconnected(9);

        assertEquals(Set.of(LEFT), offers.take());
    }

    /** A candidate of an open identity question keeps waiting, and is offered once the question lets it go (A12). */
    @Test
    void aHeldBackControllerKeepsWaiting() {
        HotPlugOffers offers = new HotPlugOffers();
        offers.connected(1, LEFT);
        offers.connected(2, RIGHT);

        assertTrue(offers.anyWaitingBesides(Set.of(LEFT)));
        assertEquals(Set.of(RIGHT), offers.take(Set.of(LEFT)));
        assertFalse(offers.anyWaitingBesides(Set.of(LEFT)));
        assertTrue(offers.anyWaiting());

        assertEquals(Set.of(LEFT), offers.take(Set.of()));
    }
}
