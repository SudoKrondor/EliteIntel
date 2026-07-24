package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The display name is what the commander hears. Two failure modes it guards against:
 * the constructor once dropped {@code Name_Localised} entirely (announcing "null"), and the journal omits that
 * field whenever it equals the raw name, which is the common case for raw elements.
 */
class MaterialCollectedEventTest {

    @Test
    void localisedNameIsSurvivedByTheConstructorAndSpoken() {
        MaterialCollectedEvent event = event("$iron_name;", "Raw", "Iron", 3);
        assertEquals("Iron", event.getNameLocalised());
        assertEquals("Iron", event.getDisplayName());
    }

    @Test
    void displayNameFallsBackToTheCapitalisedRawNameWhenLocalisedIsAbsent() {
        // Frontier omits Name_Localised when it would equal the raw value (typical for raw materials).
        MaterialCollectedEvent event = event("carbon", "Raw", null, 3);
        assertEquals("Carbon", event.getDisplayName());
    }

    private static MaterialCollectedEvent event(String name, String category, String nameLocalised, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("event", "MaterialCollected");
        json.addProperty("Name", name);
        json.addProperty("Category", category);
        json.addProperty("Count", count);
        if (nameLocalised != null) {
            json.addProperty("Name_Localised", nameLocalised);
        }
        return new MaterialCollectedEvent(json);
    }
}
