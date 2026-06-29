package elite.intel.junit.gameapi.journal.events;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.LoadGameEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoadGameEventTest {

    @Test
    void parsesShipLocalisedWhenPresent() {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", "2025-01-01T12:00:00Z");
        json.addProperty("Ship", "type9_military");
        json.addProperty("Ship_Localised", "Type-10 Defender");
        LoadGameEvent event = new LoadGameEvent(json);
        assertEquals("Type-10 Defender", event.getShipLocalised());
    }

    @Test
    void shipLocalisedIsNullWhenAbsent() {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", "2025-01-01T12:00:00Z");
        json.addProperty("Ship", "anaconda");
        LoadGameEvent event = new LoadGameEvent(json);
        assertNull(event.getShipLocalised());
    }
}
