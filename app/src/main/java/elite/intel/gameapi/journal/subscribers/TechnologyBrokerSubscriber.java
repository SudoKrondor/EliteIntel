package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.events.TechnologyBrokerEvent;

import java.util.List;

/**
 * Deducts the engineering materials a tech-broker unlock cost. The commodities half of the price is
 * cargo and is not this ledger's business.
 */
public class TechnologyBrokerSubscriber {

    private final MaterialManager materialManager = MaterialManager.getInstance();

    @Subscribe
    public void onTechnologyBroker(TechnologyBrokerEvent event) {
        Thread.ofVirtual().start(() -> {
            List<TechnologyBrokerEvent.Material> materials = event.getMaterials();
            if (materials == null) return;
            for (TechnologyBrokerEvent.Material material : materials) {
                materialManager.subtract(material.getName(), material.getCount());
            }
        });
    }
}
