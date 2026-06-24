package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.dao.MaterialsDao;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.events.MaterialCollectedEvent;
import elite.intel.gameapi.journal.subscribers.MaterialCollectedSubscriber;
import elite.intel.search.edsm.dto.MaterialsType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MaterialCollectedSubscriberTest {

    private final MaterialCollectedSubscriber subscriber = new MaterialCollectedSubscriber();

    @BeforeEach
    void clearMaterials() {
        MaterialManager.getInstance().clear();
    }

    @Test
    void firstCollectionCreatesMaterialRecord() {
        subscriber.onMaterialCollected(event("Carbon", "Raw", 3));

        MaterialsDao.Material result = MaterialManager.getInstance().find("Carbon");
        assertNotNull(result);
        assertEquals(3, result.getAmount());
    }

    @Test
    void repeatedCollectionAccumulatesAmount() {
        subscriber.onMaterialCollected(event("Carbon", "Raw", 3));
        subscriber.onMaterialCollected(event("Carbon", "Raw", 5));

        assertEquals(8, MaterialManager.getInstance().find("Carbon").getAmount());
    }

    @Test
    void rawCategoryMapsToGameRaw() {
        subscriber.onMaterialCollected(event("Carbon", "Raw", 1));

        assertEquals(MaterialsType.GAME_RAW.getType(),
                MaterialManager.getInstance().find("Carbon").getMaterialType());
    }

    @Test
    void manufacturedCategoryMapsToGameManufactured() {
        subscriber.onMaterialCollected(event("Focus Crystals", "Manufactured", 1));

        assertEquals(MaterialsType.GAME_MANUFACTURED.getType(),
                MaterialManager.getInstance().find("Focus Crystals").getMaterialType());
    }

    @Test
    void encodedCategoryMapsToGameEncoded() {
        subscriber.onMaterialCollected(event("Unusual Encrypted Files", "Encoded", 1));

        assertEquals(MaterialsType.GAME_ENCODED.getType(),
                MaterialManager.getInstance().find("Unusual Encrypted Files").getMaterialType());
    }

    @Test
    void unrecognisedCategoryMapsToGameUnknown() {
        subscriber.onMaterialCollected(event("Sensor Fragment", "Thargoid", 1));

        assertEquals(MaterialsType.GAME_UNKNOWN.getType(),
                MaterialManager.getInstance().find("Sensor Fragment").getMaterialType());
    }

    @Test
    void lowercaseNameFromJournalIsStoredCapitalized() {
        subscriber.onMaterialCollected(event("carbon", "Raw", 3));

        MaterialsDao.Material result = MaterialManager.getInstance().find("Carbon");
        assertNotNull(result, "material stored with lowercase journal name should be findable as 'Carbon'");
        assertEquals("Carbon", result.getMaterialName());
        assertEquals(3, result.getAmount());
    }

    @Test
    void maxCapIsPopulatedFromEDMaterialCaps() {
        subscriber.onMaterialCollected(event("Focus Crystals", "Manufactured", 1));

        // Focus Crystals is G4 manufactured → cap 150
        assertEquals(150, MaterialManager.getInstance().find("Focus Crystals").getMaxCap());
    }

    @Test
    void differentMaterialsAccumulateIndependently() {
        subscriber.onMaterialCollected(event("Carbon", "Raw", 5));
        subscriber.onMaterialCollected(event("Iron", "Raw", 10));

        assertEquals(5, MaterialManager.getInstance().find("Carbon").getAmount());
        assertEquals(10, MaterialManager.getInstance().find("Iron").getAmount());
    }

    private static MaterialCollectedEvent event(String name, String category, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("event", "MaterialCollected");
        json.addProperty("Name", name);
        json.addProperty("Category", category);
        json.addProperty("Count", count);
        return new MaterialCollectedEvent(json);
    }
}
