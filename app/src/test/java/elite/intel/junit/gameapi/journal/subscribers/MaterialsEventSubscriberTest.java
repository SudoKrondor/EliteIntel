package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.events.MaterialsEvent;
import elite.intel.gameapi.journal.subscribers.MaterialsEventSubscriber;
import elite.intel.search.edsm.dto.MaterialsType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class MaterialsEventSubscriberTest {

    private final MaterialsEventSubscriber subscriber = new MaterialsEventSubscriber();

    @BeforeEach
    void clearMaterials() {
        MaterialManager.getInstance().clear();
    }

    @Test
    void rawMaterialsAreStoredWithCorrectTypeAndCount() throws InterruptedException {
        subscriber.onMaterialsEvent(event(material("Carbon", 10), null, null));

        awaitTrue(() -> MaterialManager.getInstance().find("Carbon") != null);
        var result = MaterialManager.getInstance().find("Carbon");
        assertEquals(10, result.getAmount());
        assertEquals(MaterialsType.GAME_RAW.getType(), result.getMaterialType());
    }

    @Test
    void manufacturedMaterialsAreStoredWithCorrectType() throws InterruptedException {
        subscriber.onMaterialsEvent(event(null, material("Focus Crystals", 5), null));

        awaitTrue(() -> MaterialManager.getInstance().find("Focus Crystals") != null);
        assertEquals(MaterialsType.GAME_MANUFACTURED.getType(),
                MaterialManager.getInstance().find("Focus Crystals").getMaterialType());
    }

    @Test
    void encodedMaterialsAreStoredWithCorrectType() throws InterruptedException {
        subscriber.onMaterialsEvent(event(null, null, material("Unusual Encrypted Files", 8)));

        awaitTrue(() -> MaterialManager.getInstance().find("Unusual Encrypted Files") != null);
        assertEquals(MaterialsType.GAME_ENCODED.getType(),
                MaterialManager.getInstance().find("Unusual Encrypted Files").getMaterialType());
    }

    @Test
    void materialsEventReplacesExistingCountNotAccumulates() throws InterruptedException {
        MaterialManager.getInstance().save("Carbon", MaterialsType.GAME_RAW, 50);

        subscriber.onMaterialsEvent(event(material("Carbon", 12), null, null));

        awaitTrue(() -> MaterialManager.getInstance().find("Carbon").getAmount() == 12);
        assertEquals(12, MaterialManager.getInstance().find("Carbon").getAmount());
    }

    @Test
    void allThreeCategoriesStoredInOneEvent() throws InterruptedException {
        subscriber.onMaterialsEvent(event(
                material("Carbon", 3),
                material("Focus Crystals", 7),
                material("Unusual Encrypted Files", 2)
        ));

        awaitTrue(() -> MaterialManager.getInstance().find("Focus Crystals") != null);
        assertNotNull(MaterialManager.getInstance().find("Carbon"));
        assertNotNull(MaterialManager.getInstance().find("Focus Crystals"));
        assertNotNull(MaterialManager.getInstance().find("Unusual Encrypted Files"));
    }

    private static MaterialsEvent event(JsonObject raw, JsonObject manufactured, JsonObject encoded) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("event", "Materials");
        json.add("Raw", arrayOf(raw));
        json.add("Manufactured", arrayOf(manufactured));
        json.add("Encoded", arrayOf(encoded));
        return new MaterialsEvent(json);
    }

    private static JsonObject material(String name, int count) {
        JsonObject m = new JsonObject();
        m.addProperty("Name", name);
        m.addProperty("Count", count);
        return m;
    }

    private static JsonArray arrayOf(JsonObject item) {
        JsonArray arr = new JsonArray();
        if (item != null) arr.add(item);
        return arr;
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
