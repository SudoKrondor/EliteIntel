package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.ShipManager;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.journal.events.ShipyardSellEvent;
import elite.intel.ui.event.ShipProfileChangedEvent;

/**
 * Takes a sold ship off the fleet. {@code SellShipID} is the same ShipID the Loadout event carries, which is
 * what the ship table is keyed on.
 */
public class ShipyardSellSubscriber {

    @Subscribe
    public void onShipyardSell(ShipyardSellEvent event) {
        Thread.ofVirtual().start(() -> {
            if (ShipManager.getInstance().forgetSoldShip(event.getSellShipID())) {
                UiBus.publish(new ShipProfileChangedEvent());
            }
        });
    }
}
