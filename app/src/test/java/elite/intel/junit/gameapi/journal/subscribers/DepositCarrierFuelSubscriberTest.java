package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.gameapi.journal.events.CarrierDepositFuelEvent;
import elite.intel.gameapi.journal.subscribers.DepositCarrierFuelSubscriber;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class DepositCarrierFuelSubscriberTest {

    private final DepositCarrierFuelSubscriber subscriber = new DepositCarrierFuelSubscriber();
    private final FleetCarrierManager carrierManager = FleetCarrierManager.getInstance();

    @Test
    void fuelLevelIsUpdatedFromDepositEvent() throws InterruptedException {
        subscriber.onCarrierDepositFuelEvent(depositFuelEvent(500));

        awaitTrue(() -> carrierManager.get().getFuelLevel() == 500);
        assertEquals(500, carrierManager.get().getFuelLevel());
    }

    @Test
    void fuelLevelIsOverwrittenBySubsequentDeposit() throws InterruptedException {
        subscriber.onCarrierDepositFuelEvent(depositFuelEvent(200));
        awaitTrue(() -> carrierManager.get().getFuelLevel() == 200);

        subscriber.onCarrierDepositFuelEvent(depositFuelEvent(750));
        awaitTrue(() -> carrierManager.get().getFuelLevel() == 750);

        assertEquals(750, carrierManager.get().getFuelLevel());
    }

    private static CarrierDepositFuelEvent depositFuelEvent(int total) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CarrierDepositFuel");
        j.addProperty("CarrierID", 3700000001L);
        j.addProperty("Amount", 50);
        j.addProperty("Total", total);
        return new CarrierDepositFuelEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
