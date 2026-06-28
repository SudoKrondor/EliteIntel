package elite.intel.junit.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.ShipyardBuyEvent;
import elite.intel.gameapi.journal.subscribers.NewShipPurchasedHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NewShipPurchasedHandlerTest {

    private final NewShipPurchasedHandler handler = new NewShipPurchasedHandler();
    private SensorDataEvent capturedEvent;

    @BeforeEach
    void registerOnBus() {
        GameEventBus.register(this);
    }

    @AfterEach
    void unregisterFromBus() {
        GameEventBus.unregister(this);
    }

    @Subscribe
    public void onSensorData(SensorDataEvent event) {
        capturedEvent = event;
    }

    private ShipyardBuyEvent buyEvent(String shipType) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", "2025-01-01T12:00:00Z");
        json.addProperty("ShipType", shipType);
        json.addProperty("ShipPrice", 100000);
        return new ShipyardBuyEvent(json);
    }

    @Test
    void resolvesSeededShipTypeToDisplayName() {
        handler.onNewShipPurchased(buyEvent("type9_military"));
        assertNotNull(capturedEvent);
        assertTrue(capturedEvent.getSensorData().contains("Type-10 Defender"),
                "Expected display name in message but got: " + capturedEvent.getSensorData());
        assertFalse(capturedEvent.getSensorData().contains("type9_military"));
    }

    @Test
    void titleCasesUnknownShipType() {
        handler.onNewShipPurchased(buyEvent("unknownship"));
        assertNotNull(capturedEvent);
        assertTrue(capturedEvent.getSensorData().contains("Unknownship"),
                "Expected title-cased name but got: " + capturedEvent.getSensorData());
        assertFalse(capturedEvent.getSensorData().contains("unknownship"),
                "Raw lowercase codename should not appear in message");
    }
}
