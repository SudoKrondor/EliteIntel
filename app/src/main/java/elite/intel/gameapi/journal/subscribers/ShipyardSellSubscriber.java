package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.ShipManager;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.journal.events.ShipyardSellEvent;
import elite.intel.ui.event.ShipProfileChangedEvent;

/**
 * Takes a sold ship off the fleet. {@code SellShipID} is the same ShipID the Loadout event carries, which is
 * what the ship table is keyed on. The row is marked sold rather than deleted - see {@link ShipManager#markSold}.
 */
public class ShipyardSellSubscriber {

    @Subscribe
    public void onShipyardSell(ShipyardSellEvent event) {
        Thread.ofVirtual().start(() -> {
            if (ShipManager.getInstance().markSold(event.getSellShipID(), event.timestamp)) {
                UiBus.publish(new ShipProfileChangedEvent());
            }
        });
    }
}
