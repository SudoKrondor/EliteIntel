package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.ShipyardSwapEvent;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;
import elite.intel.gameapi.journal.subscribers.ShipyardSwapSubscriber;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShipyardSwapSubscriberTest {

    private final ShipyardSwapSubscriber subscriber = new ShipyardSwapSubscriber();

    private ShipyardSwapEvent swapEvent(String shipType, String localised) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", "2025-01-01T12:00:00Z");
        json.addProperty("event", "ShipyardSwap");
        json.addProperty("ShipType", shipType);
        if (localised != null) json.addProperty("ShipType_Localised", localised);
        json.addProperty("ShipID", 42);
        return new ShipyardSwapEvent(json);
    }

    @Test
    void skipsUpsertWhenLocalisedAbsent() throws InterruptedException {
        subscriber.onEvent(swapEvent("swaptest_nolocal", null));
        Thread.sleep(100);
        // Title-case fallback confirms no upsert occurred
        assertEquals("Swaptest_nolocal", LoadoutConverter.toDisplayShipName(null, "swaptest_nolocal"));
    }

    @Test
    void upsertsDisplayNameWhenLocalisedPresent() throws InterruptedException {
        subscriber.onEvent(swapEvent("swaptest_ship1", "Swap Test Ship 1"));
        Thread.sleep(100);
        assertEquals("Swap Test Ship 1", LoadoutConverter.toDisplayShipName(null, "swaptest_ship1"));
    }
}
