package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.ShipMakeManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;

public class ShipyardSubscriber {

    @Subscribe
    public void onShipyardEvent(GameEvents.ShipyardEvent event) {
        if (event.getPriceList() == null) return;
        Thread.ofVirtual().start(() -> {
            ShipMakeManager manager = ShipMakeManager.getInstance();
            for (GameEvents.ShipyardEvent.ShipPrice ship : event.getPriceList()) {
                if (ship.getShipTypeLocalised() != null) {
                    manager.upsert(ship.getShipType(), ship.getShipTypeLocalised());
                }
            }
        });
    }
}
