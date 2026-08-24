package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.db.managers.SquadronCarrierManager;
import elite.intel.gameapi.journal.events.CargoTransferEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.subscribers.CargoTransferSubscriber;
import elite.intel.session.DockedMarket;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Keeping the carrier's hold level as the commander loads and unloads it.
 * <p>
 * The move IS journalled, so the only reason the app ever believed steel was still aboard a carrier it had
 * just been emptied out of was that nothing applied the event.
 */
class CargoTransferSubscriberTest {

    private static final String OUR_CALLSIGN = "GHY-L8X";
    private static final long OUR_PAD = 3712500736L;

    private final CargoTransferSubscriber subscriber = new CargoTransferSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

    @BeforeEach
    @AfterEach
    void resetCarrierData() throws InterruptedException {
        Thread.sleep(100);
        FleetCarrierManager.getInstance().save(carrier(OUR_CALLSIGN));
        SquadronCarrierManager.getInstance().save(new CarrierDataDto());
        DockedMarket.getInstance().departed();
    }

    @Test
    void toCarrierTransferAddsCommodity() throws InterruptedException {
        onOurPad();

        subscriber.onCargoTransfer(cargoTransferEvent("gold", 5, "tocarrier"));

        awaitTrue(() -> session.getFleetCarrierData().getCommodity().containsKey("gold"));
        assertEquals(5, session.getFleetCarrierData().getCommodity().get("gold"));
    }

    @Test
    void toShipTransferRemovesCommodity() throws InterruptedException {
        onOurPad();
        seedHold("gold", 10);

        subscriber.onCargoTransfer(cargoTransferEvent("gold", 4, "toship"));

        awaitTrue(() -> {
            Integer count = session.getFleetCarrierData().getCommodity().get("gold");
            return count != null && count == 6;
        });
        assertEquals(6, session.getFleetCarrierData().getCommodity().get("gold"));
    }

    @Test
    void toShipTransferRemovesEntryWhenCountReachesZero() throws InterruptedException {
        onOurPad();
        seedHold("silver", 3);

        subscriber.onCargoTransfer(cargoTransferEvent("silver", 3, "toship"));

        awaitTrue(() -> !session.getFleetCarrierData().getCommodity().containsKey("silver"));
        assertFalse(session.getFleetCarrierData().getCommodity().containsKey("silver"));
    }

    /**
     * The manifest spells the good {@code $Steel_name;} and the transfer spells it {@code steel}. A ledger
     * keyed on the raw string would never be joined against a construction manifest at all.
     */
    @Test
    void ledgerIsKeyedByBareSymbol() throws InterruptedException {
        onOurPad();

        subscriber.onCargoTransfer(cargoTransferEvent("$Steel_name;", 640, "tocarrier"));

        awaitTrue(() -> session.getFleetCarrierData().getCommodity().containsKey("steel"));
        assertEquals(640, session.getFleetCarrierData().getCommodity().get("steel"));
    }

    /**
     * An SRV is loaded planetside, on no pad at all. Deducting that from a carrier in another system is how
     * a hold we never touched comes to read empty.
     */
    @Test
    void transferOffACarrierPadIsNotOurs() throws InterruptedException {
        seedHold("gold", 10);
        DockedMarket.getInstance().departed();

        subscriber.onCargoTransfer(cargoTransferEvent("gold", 4, "toship"));

        Thread.sleep(300);
        assertEquals(10, session.getFleetCarrierData().getCommodity().get("gold"),
                "cargo moved somewhere else does not come off the carrier's books");
    }

    /**
     * A commander can own a fleet carrier AND a squadron carrier, and {@code CargoTransfer} names neither.
     * Guessing means unloading one carrier's books because the ship was standing on the other.
     */
    @Test
    void transferIsAttributedToTheCarrierUnderTheShip() throws InterruptedException {
        CarrierDataDto squadron = carrier("SQD-001");
        SquadronCarrierManager.getInstance().save(squadron);
        seedHold("gold", 10);
        DockedMarket.getInstance().arrived(999L, "SQD-001");

        subscriber.onCargoTransfer(cargoTransferEvent("gold", 7, "tocarrier"));

        awaitTrue(() -> SquadronCarrierManager.getInstance().get().getCommodity().containsKey("gold"));
        assertEquals(7, SquadronCarrierManager.getInstance().get().getCommodity().get("gold"));
        assertEquals(10, session.getFleetCarrierData().getCommodity().get("gold"),
                "the fleet carrier was not the pad we were standing on, so its books do not move");
    }

    private void onOurPad() {
        DockedMarket.getInstance().arrived(OUR_PAD, OUR_CALLSIGN);
    }

    /**
     * Puts the hold in a known, TRACKED state - the same state a market read leaves it in - so a test about
     * transfers is not also a test about adopting a snapshot.
     */
    private void seedHold(String commodity, int units) {
        CarrierDataDto seeded = carrier(OUR_CALLSIGN);
        seeded.replaceCommodities(java.util.Map.of(commodity, units));
        FleetCarrierManager.getInstance().save(seeded);
    }

    private static CarrierDataDto carrier(String callSign) {
        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setCallSign(callSign);
        return carrier;
    }

    private static CargoTransferEvent cargoTransferEvent(String commodity, int count, String direction) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CargoTransfer");
        JsonArray transfers = new JsonArray();
        JsonObject transfer = new JsonObject();
        transfer.addProperty("Type", commodity);
        transfer.addProperty("Count", count);
        transfer.addProperty("Direction", direction);
        transfers.add(transfer);
        j.add("Transfers", transfers);
        return new CargoTransferEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
