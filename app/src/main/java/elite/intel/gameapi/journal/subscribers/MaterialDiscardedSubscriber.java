package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.events.MaterialDiscardedEvent;

/**
 * Removes material thrown away from the inventory panel.
 */
public class MaterialDiscardedSubscriber {

    private final MaterialManager materialManager = MaterialManager.getInstance();

    @Subscribe
    public void onMaterialDiscarded(MaterialDiscardedEvent event) {
        Thread.ofVirtual().start(() -> materialManager.subtract(event.getName(), event.getCount()));
    }
}
