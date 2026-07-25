package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.dao.MaterialNameDao;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.events.MaterialCollectedEvent;
import elite.intel.gameapi.journal.subscribers.MaterialCollectedSubscriber;
import elite.intel.gameapi.search.edsm.dto.MaterialsType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * MaterialCollected carries a delta, not a total, and identifies the material by the journal's
 * non-localized {@code Name} — the FDev symbol, e.g. {@code focuscrystals}. The fixtures here use
 * real symbols taken from live journal files, not display names.
 */
class MaterialCollectedSubscriberTest {

    private final MaterialCollectedSubscriber subscriber = new MaterialCollectedSubscriber();

    @BeforeEach
    void clearAmounts() {
        MaterialManager.getInstance().clear();
    }

    @Test
    void firstCollectionRecordsAmount() {
        subscriber.onMaterialCollected(event("carbon", "Raw", 3));

        MaterialNameDao.Material result = MaterialManager.getInstance().find("carbon");
        assertNotNull(result);
        assertEquals(3, result.getAmount());
    }

    @Test
    void repeatedCollectionAccumulatesAmount() {
        subscriber.onMaterialCollected(event("carbon", "Raw", 3));
        subscriber.onMaterialCollected(event("carbon", "Raw", 5));

        assertEquals(8, MaterialManager.getInstance().find("carbon").getAmount());
    }

    @Test
    void multiWordMaterialIsFoundByItsSymbol() {
        // The journal writes Name=focuscrystals with Name_Localised="Focus Crystals". Keying on the
        // display name is what previously split this material across two rows.
        subscriber.onMaterialCollected(event("focuscrystals", "Manufactured", 4, "Focus Crystals"));

        MaterialNameDao.Material result = MaterialManager.getInstance().find("focuscrystals");
        assertNotNull(result);
        assertEquals(4, result.getAmount());
        assertEquals("Focus Crystals", result.getName());
    }

    @Test
    void underscoredGuardianSymbolIsFound() {
        subscriber.onMaterialCollected(event("guardian_powercell", "Manufactured", 6, "Guardian Power Cell"));

        assertEquals(6, MaterialManager.getInstance().find("guardian_powercell").getAmount());
    }

    @Test
    void collectionIsCappedAtTheMaterialsStorageLimit() {
        // Imperial Shielding is G5 manufactured, cap 100. The game cannot hand out more than the cap.
        subscriber.onMaterialCollected(event("imperialshielding", "Manufactured", 250, "Imperial Shielding"));

        MaterialNameDao.Material result = MaterialManager.getInstance().find("imperialshielding");
        assertEquals(100, result.getMaxCapacity());
        assertEquals(100, result.getAmount());
    }

    @Test
    void seededMaterialTypeSurvivesAMisreportedCategory() {
        // The catalogue's own category is authoritative; a stray Category on the event must not
        // overwrite it. Sensor Fragment is Manufactured regardless of what the event claims.
        subscriber.onMaterialCollected(event("unknownenergysource", "Thargoid", 1, "Sensor Fragment"));

        assertEquals(MaterialsType.GAME_MANUFACTURED.getType(),
                MaterialManager.getInstance().find("unknownenergysource").getMaterialType());
    }

    @Test
    void unrecognisedSymbolIsRegisteredRatherThanDropped() {
        // Guards against a future game update adding a material this build has never seen.
        subscriber.onMaterialCollected(event("someunreleasedmaterial", "Raw", 7, "Some Unreleased Material"));

        MaterialNameDao.Material result = MaterialManager.getInstance().find("someunreleasedmaterial");
        assertNotNull(result, "an unknown symbol should be registered, not silently discarded");
        assertEquals(7, result.getAmount());
        assertEquals("Some Unreleased Material", result.getName());
        assertEquals(MaterialsType.GAME_RAW.getType(), result.getMaterialType());
    }

    @Test
    void differentMaterialsAccumulateIndependently() {
        subscriber.onMaterialCollected(event("carbon", "Raw", 5));
        subscriber.onMaterialCollected(event("iron", "Raw", 10));

        assertEquals(5, MaterialManager.getInstance().find("carbon").getAmount());
        assertEquals(10, MaterialManager.getInstance().find("iron").getAmount());
    }

    private static MaterialCollectedEvent event(String symbol, String category, int count) {
        return event(symbol, category, count, null);
    }

    private static MaterialCollectedEvent event(String symbol, String category, int count, String localised) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("event", "MaterialCollected");
        json.addProperty("Name", symbol);
        json.addProperty("Category", category);
        json.addProperty("Count", count);
        // Frontier omits Name_Localised whenever it would equal the raw name, which is the norm
        // for raw elements.
        if (localised != null) json.addProperty("Name_Localised", localised);
        return new MaterialCollectedEvent(json);
    }
}
