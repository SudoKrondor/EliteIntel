package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.events.MaterialTradeEvent;
import elite.intel.gameapi.search.edsm.dto.MaterialsType;

/**
 * Keeps the material inventory honest across a trader visit.
 * <p>
 * Without this, only the pickup half of the ledger was ever recorded: MaterialCollected added what was
 * gathered and nothing removed what was spent at a trader, so a material traded down to zero was still
 * reported at whatever total it had reached (the reported figure only ever grew).
 */
public class MaterialTradeSubscriber {

    private final MaterialManager materialManager = MaterialManager.getInstance();

    @Subscribe
    public void onMaterialTrade(MaterialTradeEvent event) {
        Thread.ofVirtual().start(() -> {
            MaterialTradeEvent.TradedMaterial paid = event.getPaid();
            if (paid != null && paid.getMaterial() != null) {
                materialManager.subtract(paid.getMaterial(), paid.getQuantity());
            }

            MaterialTradeEvent.TradedMaterial received = event.getReceived();
            if (received != null && received.getMaterial() != null) {
                // collect() rather than a bare add: the material received may be one the commander has
                // never held, and collect() registers it before crediting the amount.
                materialManager.collect(
                        received.getMaterial(),
                        MaterialsType.fromJournalCategory(received.getCategory()),
                        received.getQuantity(),
                        received.getMaterialLocalised());
            }
        });
    }
}
