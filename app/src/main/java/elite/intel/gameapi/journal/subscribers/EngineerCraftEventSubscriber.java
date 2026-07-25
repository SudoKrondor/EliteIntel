package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.events.EngineerCraftEvent;

import java.util.List;

public class EngineerCraftEventSubscriber {

    private final MaterialManager materialManager = MaterialManager.getInstance();

    @Subscribe public void onEngineerCraftEvent(EngineerCraftEvent event) {
        Thread.ofVirtual().start(() -> {
            List<EngineerCraftEvent.Ingredient> ingredients = event.getIngredients();
            if (ingredients == null) return;
            for (EngineerCraftEvent.Ingredient material : ingredients) {
                // Deduct against the journal's Name (the FDev symbol). This previously used
                // capitalizeWords(Name_Localised) -- "Focus Crystals" -- while the inventory row was
                // written from the symbol as "Focuscrystals", so the deduction silently matched
                // nothing and spent material was never subtracted.
                materialManager.subtract(material.getName(), material.getCount());
            }
        });
    }
}
