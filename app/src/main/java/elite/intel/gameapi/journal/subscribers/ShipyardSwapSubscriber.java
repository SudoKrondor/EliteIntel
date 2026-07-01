package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.ShipMakeManager;
import elite.intel.gameapi.journal.events.ShipyardSwapEvent;

public class ShipyardSwapSubscriber {

    @Subscribe
    public void onEvent(ShipyardSwapEvent event) {
        if (event.getShipTypeLocalised() == null) return;
        Thread.ofVirtual().start(() ->
                ShipMakeManager.getInstance().upsert(event.getShipType(), event.getShipTypeLocalised())
        );
    }
}
