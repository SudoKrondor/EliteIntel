package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.events.MaterialsEvent;
import elite.intel.gameapi.journal.subscribers.MaterialsEventSubscriber;
import elite.intel.gameapi.search.edsm.dto.MaterialsType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The Materials event is a full inventory snapshot keyed by FDev symbol, so its counts are absolute
 * and replace whatever was held.
 */
class MaterialsEventSubscriberTest {

    private final MaterialsEventSubscriber subscriber = new MaterialsEventSubscriber();

    @BeforeEach
    void clearAmounts() {
        MaterialManager.getInstance().clear();
    }

    @Test
    void rawMaterialsAreStoredWithCorrectTypeAndCount() throws InterruptedException {
        subscriber.onMaterialsEvent(event(material("carbon", 10), null, null));

        awaitTrue(() -> MaterialManager.getInstance().find("carbon").getAmount() == 10);
        var result = MaterialManager.getInstance().find("carbon");
        assertEquals(10, result.getAmount());
        assertEquals(MaterialsType.GAME_RAW.getType(), result.getMaterialType());
    }

    @Test
    void manufacturedMaterialsAreStoredWithCorrectType() throws InterruptedException {
        subscriber.onMaterialsEvent(event(null, material("focuscrystals", 5, "Focus Crystals"), null));

        awaitTrue(() -> MaterialManager.getInstance().find("focuscrystals").getAmount() == 5);
        assertEquals(MaterialsType.GAME_MANUFACTURED.getType(),
                MaterialManager.getInstance().find("focuscrystals").getMaterialType());
    }

    @Test
    void encodedMaterialsAreStoredWithCorrectType() throws InterruptedException {
        subscriber.onMaterialsEvent(event(null, null, material("encryptedfiles", 8, "Unusual Encrypted Files")));

        awaitTrue(() -> MaterialManager.getInstance().find("encryptedfiles").getAmount() == 8);
        assertEquals(MaterialsType.GAME_ENCODED.getType(),
                MaterialManager.getInstance().find("encryptedfiles").getMaterialType());
    }

    @Test
    void materialsEventReplacesExistingCountNotAccumulates() throws InterruptedException {
        MaterialManager.getInstance().snapshot("carbon", MaterialsType.GAME_RAW, 50, null);

        subscriber.onMaterialsEvent(event(material("carbon", 12), null, null));

        awaitTrue(() -> MaterialManager.getInstance().find("carbon").getAmount() == 12);
        assertEquals(12, MaterialManager.getInstance().find("carbon").getAmount());
    }

    @Test
    void snapshotIsStoredAgainstTheSymbolNotTheLocalisedName() throws InterruptedException {
        // A German client sends Name=salvagedalloys with Name_Localised="Geborgene Legierungen".
        // Both must land on the same row an English client writes.
        subscriber.onMaterialsEvent(event(null, material("salvagedalloys", 42, "Geborgene Legierungen"), null));

        awaitTrue(() -> MaterialManager.getInstance().find("salvagedalloys").getAmount() == 42);
        var result = MaterialManager.getInstance().find("salvagedalloys");
        assertEquals("Salvaged Alloys", result.getName(),
                "the catalogue's own English name must win over the client's localized string");
        assertEquals(300, result.getMaxCapacity());
    }

    @Test
    void allThreeCategoriesStoredInOneEvent() throws InterruptedException {
        subscriber.onMaterialsEvent(event(
                material("carbon", 3),
                material("focuscrystals", 7, "Focus Crystals"),
                material("encryptedfiles", 2, "Unusual Encrypted Files")
        ));

        awaitTrue(() -> MaterialManager.getInstance().find("encryptedfiles").getAmount() == 2);
        assertEquals(3, MaterialManager.getInstance().find("carbon").getAmount());
        assertEquals(7, MaterialManager.getInstance().find("focuscrystals").getAmount());
        assertEquals(2, MaterialManager.getInstance().find("encryptedfiles").getAmount());
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

    private static JsonObject material(String symbol, int count) {
        return material(symbol, count, null);
    }

    private static JsonObject material(String symbol, int count, String localised) {
        JsonObject m = new JsonObject();
        m.addProperty("Name", symbol);
        if (localised != null) m.addProperty("Name_Localised", localised);
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
