package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.events.EngineerCraftEvent;
import elite.intel.gameapi.journal.events.MaterialCollectedEvent;
import elite.intel.gameapi.journal.subscribers.EngineerCraftEventSubscriber;
import elite.intel.gameapi.journal.subscribers.MaterialCollectedSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class EngineerCraftEventSubscriberTest {

    private final EngineerCraftEventSubscriber subscriber = new EngineerCraftEventSubscriber();
    private final MaterialCollectedSubscriber materialCollectedSubscriber = new MaterialCollectedSubscriber();
    private final MaterialManager materialManager = MaterialManager.getInstance();

    @BeforeEach
    void clearAmounts() throws InterruptedException {
        Thread.sleep(100);
        materialManager.clear();
    }

    @Test
    void engineerCraftSubtractsBySymbol() throws InterruptedException {
        materialCollectedSubscriber.onMaterialCollected(collected("carbon", "Raw", 10));

        subscriber.onEngineerCraftEvent(craft(Map.of("carbon", 3), Map.of("carbon", "Carbon")));

        awaitAmount("carbon", 7);
    }

    @Test
    void engineerCraftSubtractsWhenJournalOmitsLocalisedName() throws InterruptedException {
        // Frontier omits Name_Localised whenever it would equal the raw name.
        materialCollectedSubscriber.onMaterialCollected(collected("iron", "Raw", 5));

        subscriber.onEngineerCraftEvent(craft(Map.of("iron", 2), Map.of()));

        awaitAmount("iron", 3);
    }

    @Test
    void engineerCraftSubtractsMultiWordMaterial() throws InterruptedException {
        // Regression guard. This previously deducted using capitalizeWords(Name_Localised) —
        // "Focus Crystals" — while the inventory row was keyed "Focuscrystals" from the symbol, so
        // nothing matched and the spend was silently lost. Single-word elements like carbon hid the
        // bug because there the two spellings coincide.
        materialCollectedSubscriber.onMaterialCollected(collected("focuscrystals", "Manufactured", 20, "Focus Crystals"));

        subscriber.onEngineerCraftEvent(craft(Map.of("focuscrystals", 5), Map.of("focuscrystals", "Focus Crystals")));

        awaitAmount("focuscrystals", 15);
    }

    @Test
    void engineerCraftSubtractsUnderscoredGuardianSymbol() throws InterruptedException {
        materialCollectedSubscriber.onMaterialCollected(collected("guardian_powercell", "Manufactured", 12, "Guardian Power Cell"));

        subscriber.onEngineerCraftEvent(craft(Map.of("guardian_powercell", 4), Map.of("guardian_powercell", "Guardian Power Cell")));

        awaitAmount("guardian_powercell", 8);
    }

    @Test
    void engineerCraftSubtractsMultipleIngredients() throws InterruptedException {
        materialCollectedSubscriber.onMaterialCollected(collected("carbon", "Raw", 10));
        materialCollectedSubscriber.onMaterialCollected(collected("iron", "Raw", 8));

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("carbon", 3);
        counts.put("iron", 3);
        subscriber.onEngineerCraftEvent(craft(counts, Map.of("carbon", "Carbon", "iron", "Iron")));

        awaitAmount("carbon", 7);
        awaitAmount("iron", 5);
    }

    @Test
    void spendingMoreThanHeldFloorsAtZero() throws InterruptedException {
        materialCollectedSubscriber.onMaterialCollected(collected("carbon", "Raw", 2));

        subscriber.onEngineerCraftEvent(craft(Map.of("carbon", 5), Map.of("carbon", "Carbon")));

        awaitAmount("carbon", 0);
    }

    private static MaterialCollectedEvent collected(String symbol, String category, int count) {
        return collected(symbol, category, count, null);
    }

    private static MaterialCollectedEvent collected(String symbol, String category, int count, String localised) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "MaterialCollected");
        j.addProperty("Name", symbol);
        j.addProperty("Category", category);
        j.addProperty("Count", count);
        if (localised != null) j.addProperty("Name_Localised", localised);
        return new MaterialCollectedEvent(j);
    }

    /**
     * @param counts    ingredient symbol → units consumed
     * @param localised ingredient symbol → Name_Localised, omitted where the journal would omit it
     */
    private static EngineerCraftEvent craft(Map<String, Integer> counts, Map<String, String> localised) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "EngineerCraft");
        j.addProperty("Slot", "MainEngines");
        j.addProperty("Module", "int_engine_size3_class5");
        j.addProperty("Engineer", "Elvira Martuuk");
        j.addProperty("EngineerID", 300160L);
        j.addProperty("BlueprintID", 128673638L);
        j.addProperty("BlueprintName", "Engine_Dirty");
        j.addProperty("Level", 1);
        j.addProperty("Quality", 1.0);
        JsonArray ingredients = new JsonArray();
        counts.forEach((symbol, count) -> {
            JsonObject ing = new JsonObject();
            ing.addProperty("Name", symbol);
            if (localised.containsKey(symbol)) ing.addProperty("Name_Localised", localised.get(symbol));
            ing.addProperty("Count", count);
            ingredients.add(ing);
        });
        j.add("Ingredients", ingredients);
        j.add("Modifiers", new JsonArray());
        return new EngineerCraftEvent(j);
    }

    private void awaitAmount(String symbol, int expected) throws InterruptedException {
        awaitTrue(() -> {
            var mat = materialManager.find(symbol);
            return mat != null && mat.getAmount() == expected;
        });
        assertEquals(expected, materialManager.find(symbol).getAmount(), symbol);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
