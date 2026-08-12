package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.events.ScientificResearchEvent;

/**
 * Removes material donated to a research station.
 */
public class ScientificResearchSubscriber {

    private final MaterialManager materialManager = MaterialManager.getInstance();

    @Subscribe
    public void onScientificResearch(ScientificResearchEvent event) {
        Thread.ofVirtual().start(() -> materialManager.subtract(event.getName(), event.getCount()));
    }
}
