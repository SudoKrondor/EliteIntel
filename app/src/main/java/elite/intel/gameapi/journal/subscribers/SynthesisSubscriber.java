package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.events.SynthesisEvent;

import java.util.List;

/**
 * Deducts what a synthesis recipe consumed — the same ledger role EngineerCraft plays for a roll.
 */
public class SynthesisSubscriber {

    private final MaterialManager materialManager = MaterialManager.getInstance();

    @Subscribe
    public void onSynthesis(SynthesisEvent event) {
        Thread.ofVirtual().start(() -> {
            List<SynthesisEvent.Material> materials = event.getMaterials();
            if (materials == null) return;
            for (SynthesisEvent.Material material : materials) {
                materialManager.subtract(material.getName(), material.getCount());
            }
        });
    }
}
