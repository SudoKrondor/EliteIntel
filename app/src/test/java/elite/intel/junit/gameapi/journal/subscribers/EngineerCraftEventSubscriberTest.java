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
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class EngineerCraftEventSubscriberTest {

    private final EngineerCraftEventSubscriber subscriber = new EngineerCraftEventSubscriber();
    private final MaterialCollectedSubscriber materialCollectedSubscriber = new MaterialCollectedSubscriber();
    private final MaterialManager materialManager = MaterialManager.getInstance();

    @BeforeEach
    void clearMaterials() throws InterruptedException {
        Thread.sleep(100);
        materialManager.clear();
    }

    @Test
    void engineerCraftSubtractsMaterialsByLocalizedName() throws InterruptedException {
        materialCollectedSubscriber.onMaterialCollected(collectedEvent("Carbon", "Raw", 10));

        subscriber.onEngineerCraftEvent(craftEvent("carbon", "Carbon", 3));

        awaitTrue(() -> {
            var mat = materialManager.find("Carbon");
            return mat != null && mat.getAmount() == 7;
        });
        assertEquals(7, materialManager.find("Carbon").getAmount());
    }

    @Test
    void engineerCraftFallsBackToNameWhenNoLocalizedName() throws InterruptedException {
        materialCollectedSubscriber.onMaterialCollected(collectedEvent("Iron", "Raw", 5));

        subscriber.onEngineerCraftEvent(craftEventNoLocalized("iron", 2));

        awaitTrue(() -> {
            var mat = materialManager.find("Iron");
            return mat != null && mat.getAmount() == 3;
        });
        assertEquals(3, materialManager.find("Iron").getAmount());
    }

    @Test
    void engineerCraftSubtractsMultipleIngredients() throws InterruptedException {
        materialCollectedSubscriber.onMaterialCollected(collectedEvent("Carbon", "Raw", 10));
        materialCollectedSubscriber.onMaterialCollected(collectedEvent("Iron", "Raw", 8));

        subscriber.onEngineerCraftEvent(craftEventMultiple());

        awaitTrue(() -> {
            var carbon = materialManager.find("Carbon");
            return carbon != null && carbon.getAmount() == 7;
        });
        assertEquals(7, materialManager.find("Carbon").getAmount());
        assertEquals(5, materialManager.find("Iron").getAmount());
    }

    private static MaterialCollectedEvent collectedEvent(String name, String category, int count) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "MaterialCollected");
        j.addProperty("Name", name);
        j.addProperty("Category", category);
        j.addProperty("Count", count);
        return new MaterialCollectedEvent(j);
    }

    private static EngineerCraftEvent craftEvent(String name, String localizedName, int count) {
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
        JsonObject ing = new JsonObject();
        ing.addProperty("Name", name);
        ing.addProperty("Name_Localised", localizedName);
        ing.addProperty("Count", count);
        ingredients.add(ing);
        j.add("Ingredients", ingredients);
        j.add("Modifiers", new JsonArray());
        return new EngineerCraftEvent(j);
    }

    private static EngineerCraftEvent craftEventNoLocalized(String name, int count) {
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
        JsonObject ing = new JsonObject();
        ing.addProperty("Name", name);
        ing.addProperty("Count", count);
        ingredients.add(ing);
        j.add("Ingredients", ingredients);
        j.add("Modifiers", new JsonArray());
        return new EngineerCraftEvent(j);
    }

    private static EngineerCraftEvent craftEventMultiple() {
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
        JsonObject ing1 = new JsonObject();
        ing1.addProperty("Name", "carbon");
        ing1.addProperty("Name_Localised", "Carbon");
        ing1.addProperty("Count", 3);
        JsonObject ing2 = new JsonObject();
        ing2.addProperty("Name", "iron");
        ing2.addProperty("Name_Localised", "Iron");
        ing2.addProperty("Count", 3);
        ingredients.add(ing1);
        ingredients.add(ing2);
        j.add("Ingredients", ingredients);
        j.add("Modifiers", new JsonArray());
        return new EngineerCraftEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
