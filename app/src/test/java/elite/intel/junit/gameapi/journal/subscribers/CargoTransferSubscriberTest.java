package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.gameapi.journal.events.CargoTransferEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.subscribers.CargoTransferSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class CargoTransferSubscriberTest {

    private final CargoTransferSubscriber subscriber = new CargoTransferSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

    @BeforeEach
    void resetCarrierData() throws InterruptedException {
        Thread.sleep(100);
        CarrierDataDto fresh = new CarrierDataDto();
        FleetCarrierManager.getInstance().save(fresh);
    }

    @Test
    void toCarrierTransferAddsCommodity() throws InterruptedException {
        subscriber.onCargoTransfer(cargoTransferEvent("gold", 5, "tocarrier"));

        awaitTrue(() -> session.getFleetCarrierData().getCommodity().containsKey("gold"));
        assertEquals(5, session.getFleetCarrierData().getCommodity().get("gold"));
    }

    @Test
    void toShipTransferRemovesCommodity() throws InterruptedException {
        CarrierDataDto seeded = new CarrierDataDto();
        seeded.addCommodity("gold", 10);
        FleetCarrierManager.getInstance().save(seeded);

        subscriber.onCargoTransfer(cargoTransferEvent("gold", 4, "toship"));

        awaitTrue(() -> {
            Integer count = session.getFleetCarrierData().getCommodity().get("gold");
            return count != null && count == 6;
        });
        assertEquals(6, session.getFleetCarrierData().getCommodity().get("gold"));
    }

    @Test
    void toShipTransferRemovesEntryWhenCountReachesZero() throws InterruptedException {
        CarrierDataDto seeded = new CarrierDataDto();
        seeded.addCommodity("silver", 3);
        FleetCarrierManager.getInstance().save(seeded);

        subscriber.onCargoTransfer(cargoTransferEvent("silver", 3, "toship"));

        awaitTrue(() -> !session.getFleetCarrierData().getCommodity().containsKey("silver"));
        assertFalse(session.getFleetCarrierData().getCommodity().containsKey("silver"));
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
